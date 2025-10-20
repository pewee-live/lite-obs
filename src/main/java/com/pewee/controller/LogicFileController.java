package com.pewee.controller;

import com.alibaba.fastjson.JSON;
import com.pewee.bean.LogicFile;
import com.pewee.bean.dto.DownloadLogicFileRespDto;
import com.pewee.bean.dto.PreUploadDto;
import com.pewee.bean.dto.SingleStreamDto;
import com.pewee.bean.dto.UploadFileRespDto;
import com.pewee.service.IFileService;
import com.pewee.service.ILogicFileService;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.RespEntity;
import com.pewee.util.resp.ServiceException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;
import javax.annotation.Nullable;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 逻辑文件Controller
 * 由于分片上传后再合并方式我们无法掌握每个分片对于每个引擎再分片的文件MD5信息,
 * 如果对于某些引擎,需要在一开始预上传就需要掌握每个分片的MD5信息,我们只能先预缓存在本地,然后再本地merge,在切片上传到各个引擎,这样做的成本过于巨大,且可能会消耗大量磁盘,
 * 而且上传因为写2次流导致上传会很慢(客户端-> obsLocal -> merge -> 再分片 -> 上传engine )
 * 考虑分片文件上传,下载以controller来模拟读写多个流的方式搞定
 *
 * @author pewee
 * @date 2022-04-13
 */
@RestController
@RequestMapping("/obs/logicfile")
public class LogicFileController {

    private static final Logger log = LoggerFactory.getLogger(LogicFileController.class);

    @Autowired
    private ILogicFileService logicFileService;
    @Autowired
    private IFileService fileService;
    @Autowired
    private JedisCluster jedisCluster;

    private static final String UPLOAD_PARTS_KEY = "com.pewee:obs:uploadPartKey:%s;%d";
    /**
     * 限制分片最小为4KB
     */
    private static final Long FOUR_KB = 4 * 1024L;
    /**
     * 限制分片最大为16MB
     */
    private static final Long SIXTEEN_MB = 16 * 1024 * 1024L;

    /**
     * 查询逻辑文件列表
     */
    @PostMapping("/listFile")
    public RespEntity<List<LogicFile>> list(@RequestHeader(value = "sysAlias") String sysAlias,  @Nullable LogicFile file) {
        log.info("查询文件列表-开始 ,sysAlias : {} ,file : {} ", sysAlias, JSON.toJSONString(file));
        Assert.notNull(sysAlias, "系统别名不能为空!");
        LogicFile req = new LogicFile();
        if (null != file){
            BeanUtils.copyProperties(file,req);
        }
        req.setSysAlisa(sysAlias);
        if (null == req.getStatus()) {
        	req.setStatus(2);
        }
        List<LogicFile> list = logicFileService.selectLogicFileList(req);
        log.info("查询文件列表-完成 : {}", JSON.toJSONString(list));
        return new RespEntity<List<LogicFile>>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData(list);
    }


    /**
     * 查询下载文件详细信息,获取下载文件链接
     * forceStaticLink : 是否强行生成合并文件静态链接 没有传默认为obs配置的策略
     * 分片的下载链接永远按照OBS的配置决定!!
     */
    @GetMapping(value = "/queryDownloadInfo/{code}")
    public RespEntity<DownloadLogicFileRespDto> getInfo(@RequestHeader(value = "sysAlias") String sysAlias, @RequestHeader(value = "forceStaticLink") @Nullable Boolean forceStaticLink,
    		@PathVariable("code") String code) {
        log.info("获取文件下载信息-开始 ,sysAlias : {} ,code : {} ", sysAlias, JSON.toJSONString(code));
        Assert.notNull(sysAlias, "系统别名不能为空!");
        LogicFile logicFile = logicFileService.selectLogicFileByCode(code);
        Assert.notNull(logicFile, "該文件不存在");
        Assert.isTrue(logicFile.getStatus() == 2, "文件尚未上傳完成或已刪除");
        Assert.isTrue(System.currentTimeMillis() < logicFile.getExpire().getTime(), "文件已過期");
        DownloadLogicFileRespDto result = logicFileService.getDownloadInfo(logicFile,forceStaticLink);
        log.info("获取文件下载信息-完成: {}",JSON.toJSONString(result));
        return new RespEntity<DownloadLogicFileRespDto>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData(result);
    }

    /**
     * 下载文件
     * 20250818 测试发现Synology中nginx配置连接超时时长为60S,当一次性获取数十个inputStream做merge时,会导致排在后面所有的流超时无法读取
     * 改造方案1 : 修改Synology中nginx配置sudo vim /etc/nginx/nginx.conf   keepalive_timeout 600s;    send_timeout 600s; 
     * 改造方案2 : 修改本方法,在需要流的时候去获取
     * 
     * 采用了方案2以避免多个nginx配置的修改
     * 
     * 
     * @param code
     */
    @GetMapping(value = "/download/merge/{code}")
    public void getMerge(@PathVariable("code") String code,@RequestParam(name = "dynamicLink") @Nullable Boolean dynamicLink,
    		HttpServletRequest request,HttpServletResponse resp) {
        log.info("文件合并下载-开始 code : {} ,dynamicLink : {}",  code, dynamicLink);
        ServletOutputStream outputStream = null;
        SingleStreamDto singleStreamDto =  logicFileService.getMergeFileInfo(code,dynamicLink);
        try {
        	LogicFile file = singleStreamDto.getFile();
        	outputStream = resp.getOutputStream();
        	resp.reset();
            resp.setCharacterEncoding("utf-8");
            resp.setContentType(file.getMimeType());
            resp.addHeader("Content-Disposition", "attachment;filename=" + new String(file.getFileName().getBytes("gb2312"), "ISO8859-1"));
            String origin = request.getHeader("Origin");    // 获得客户端domain
            if(origin == null) {
                origin = request.getHeader("Referer");
            }
            resp.setHeader("Access-Control-Allow-Origin", origin);            // 允许指定域访问跨域资源
            resp.setHeader("Access-Control-Allow-Credentials", "true");       // 允许客户端携带跨域cookie，此时origin值不能为“*”，只能为指定单一域名
            resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
            resp.setHeader("Access-Control-Max-Age", "3600");
            resp.setHeader("Access-Control-Allow-Headers", "x-requested-with,satoken");	// 允许的header参数
            InputStream inputStream = null;
            while ( singleStreamDto.hasNextStream()) {
            	logicFileService.getSingleStream(singleStreamDto);
            	inputStream = singleStreamDto.getStream();
            	try {
            		IOUtils.copy(inputStream, outputStream);
            	} finally {
        			if (null != inputStream) {
        				try {
        					IOUtils.closeQuietly(inputStream);
        				} catch (Exception e) {
        					log.error("文件分片下载-关闭输入流失败!!",e);
        				}
        			}
            	}
            }
        }catch (Exception e) {
			log.error("文件分片下载-错误!",e);
			throw new ServiceException(CommonRespInfo.SYS_ERROR);
		} finally {
			if (null != outputStream) {
				try {
					outputStream.flush();
				} catch (IOException e) {
					log.error("文件分片下载-错误!",e);
				}
			}
			
			IOUtils.closeQuietly(outputStream);
		}
        logicFileService.getDynamicLink().countDown(code);
        log.info("文件合并下载-完成: {}",code);
    }

    /**
     * 下载文件分片,当文件只有一个分片时,会写入文件信息
     * @param code
     */
    @GetMapping(value = "/download/part/{code}")
    public void getPart(@PathVariable("code") String code,
    		@RequestParam(name = "length") long length,
    		@RequestParam(name = "sequence") int sequence,
    		@RequestParam(name = "dynamicLink") boolean dynamicLink,
    		@RequestParam(name = "storageType") int storageType,
    		@RequestParam(name = "info",required = false) String info,
    		 HttpServletRequest request,
    		HttpServletResponse resp) {
        log.info("文件分片下载-开始 code : {} ,length : {} ,sequence : {}, dynamicLink : {} ,storageType : {}, info: {}",  code,length,sequence,dynamicLink,storageType,info);
        InputStream is = null;
        ServletOutputStream outputStream = null;
        try {
        	is = fileService.getSingleStream(code,storageType);
        	outputStream = resp.getOutputStream();
        	if (StringUtils.isNotBlank(info)) {
    			String logicFileJson = new String(Base64Utils.decodeFromUrlSafeString(info));
    			LogicFile logicFile = JSON.parseObject(logicFileJson, LogicFile.class);
    			//返回的对象要设置文件名
                resp.reset();
                resp.setCharacterEncoding("utf-8");
                resp.setContentType(logicFile.getMimeType());
                resp.addHeader("Content-Disposition", "attachment;filename=" + new String(logicFile.getFileName().getBytes("gb2312"), "ISO8859-1"));
                String origin = request.getHeader("Origin");    // 获得客户端domain
                if(origin == null) {
                    origin = request.getHeader("Referer");
                }
                resp.setHeader("Access-Control-Allow-Origin", origin);            // 允许指定域访问跨域资源
                resp.setHeader("Access-Control-Allow-Credentials", "true");       // 允许客户端携带跨域cookie，此时origin值不能为“*”，只能为指定单一域名
                resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
                resp.setHeader("Access-Control-Max-Age", "3600");
                resp.setHeader("Access-Control-Allow-Headers", "x-requested-with,satoken");	// 允许的header参数
    		}
        	IOUtils.copy(is, outputStream);
		} catch (IOException e) {
			log.error("文件分片下载-错误!",e);
			throw new ServiceException(CommonRespInfo.SYS_ERROR);
		} finally {
			if (null != outputStream) {
				try {
					outputStream.flush();
				} catch (IOException e) {
					log.error("文件分片下载-错误!",e);
				}
			}
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(outputStream);
		}
        log.info("文件分片下载-完成: {}",code);
    }

    /**
     * 预上传
     * sysAlias sysCode filename mimeType length crc32 split 均为必填参数
     * 如果split传了1 那么total必填
     *
     * @param sysAlias
     * @param sysCode
     * @param filename
     * @param crc32
     * @param expire
     * @param split
     * @return
     */
    @PostMapping("/preupload")
    public RespEntity<String> preupload(
            @RequestHeader(value = "sysAlias") String sysAlias,
            @RequestHeader(value = "sysCode") String sysCode,
            @Nullable @RequestParam(value = "filename") String filename,
            @Nullable @RequestParam(value = "mimeType") String mimeType,
            @Nullable @RequestParam(value = "length") Long length,
            @Nullable @RequestParam(value = "storageType") Integer storageType,
            @Nullable @RequestParam(value = "crc32") String crc32,
            @Nullable @RequestParam(value="subCrcList") List<String> subCrcList,
            @Nullable @RequestParam(value = "expire") String expire,
            @Nullable @RequestHeader(value = "split") Integer split,
            @Nullable @RequestHeader(value = "total") Integer total
    ) {
        log.info("预上传文件-开始, sysAlias : {}, sysCode: {}, filename : {}, mimeType : {}, length : {}, storageType : {}, crc32 : {}, expire : {}, split : {}, total:{}", sysAlias, sysCode, filename, mimeType, length, storageType, crc32, expire, split, total);
        PreUploadDto preUploadDto = PreUploadDto
                .builder()
                .sysAlisa(sysAlias)
                .sysCode(sysCode)
                .fileName(filename)
                .mimeType(mimeType)
                .length(length)
                .storageType(storageType)
                .crc32(crc32)
                .split(split)
                .total(total)
                .build();

        if (!checkSubCrcList(split,total,subCrcList)){
            throw new IllegalArgumentException("subCrcList存在问题,请检查!");
        }
        LogicFile logicFile = preUploadDto.generateLogicFile();
        parseExpireAndSetToLogicFile(expire, logicFile);
        logicFile.setCreateTime(new Date());
        logicFile = logicFileService.preupload(logicFile,subCrcList);
        return new RespEntity<String>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData(logicFile.getCode());
    }

    private boolean checkSubCrcList(Integer split, Integer total, List<String> subCrcList) {
        if (split == 0){
            return true;
        }
        if (total == null || subCrcList == null){
            return false;
        }
        return (!subCrcList.isEmpty()) && total.equals(subCrcList.size());
    }

    /**
     * 上传文件
     * 注意!分片上传时,每个分片的大小必须在4KB-16MB之间!!
     *
     * @param sysAlias
     * @param crc32 该分片的CRC;上传code字段时,该字段不能为空
     * @param expire   yyyyMMdd 仅code不传时,该字段穿了有效,该字段选传,默认为1年后
     * @param code 通过预上传接口获取的code
     * @param sequence 分片号,上传code字段时,该字段不能为空
     * @param req
     * @return
     */
    @PostMapping("/upload")
    public RespEntity<String> upload(@RequestHeader(value = "sysAlias") String sysAlias,
    		@RequestHeader(value = "sysCode") String sysCode, 
    		@Nullable @RequestParam(value = "crc32") String crc32, 
    		@Nullable @RequestParam(value = "expire") String expire,
            @Nullable @RequestHeader(value = "code") String code, 
            @Nullable @RequestHeader(value = "sequence") Integer sequence,
            MultipartHttpServletRequest req) {
        log.info("上传文件-开始 ,sysAlias : {} ", sysAlias);
        Map<String, MultipartFile> fileMap = req.getFileMap();
        MultipartFile file = fileMap.values().iterator().next();
        Assert.notNull(file, "上傳文件-上傳的文件為空!");
        String filecode = "";
        UploadFileRespDto respDto = new UploadFileRespDto();
        if (null == code) {
            log.info("上傳文件-快速上傳邏輯-開始");
            LogicFile logicFile = new LogicFile();
            logicFile.setSysAlisa(sysAlias);
            logicFile.setSysCode(sysCode);
            if (null != expire) {
                SimpleDateFormat format = new SimpleDateFormat(com.pewee.util.Constants.DATE_FORMAT_DAY);
                try {
                	Date exparse = format.parse(expire);
                	Assert.isTrue( System.currentTimeMillis() <  exparse.getTime(),"过期时间不合法!");
                    logicFile.setExpire(exparse);
                } catch (ParseException e) {
                    throw new ServiceException(CommonRespInfo.NOT_LEGAL_PARAM);
                }
            } else {
                logicFile.setExpire(DateTime.now().plusYears(1).toDate());
            }
            logicFile.setSplit(0);
            filecode = logicFileService.upload(logicFile, file,0,null);
            code = logicFile.getCode();
        } else {
        	LogicFile logicFile = logicFileService.selectLogicFileByCode(code);
            Assert.notNull(logicFile, "該文件不存在");
            Assert.notNull(sequence, "序列号不能为空!");
            Assert.isTrue(StringUtils.isNotBlank(crc32),"分片crc32不能为空!");
            Assert.isTrue(logicFile.getStatus() != 3 &&logicFile.getStatus() != 2 , "文件上傳完成或已刪除");
            Assert.isTrue(System.currentTimeMillis() < logicFile.getExpire().getTime(), "文件已過期");
            //分片从0号开始!!
            Assert.isTrue(logicFile.getTotal() >= sequence, "分片号不能大于文件总数");
            Assert.isTrue(  file.getSize() <= SIXTEEN_MB, "分片大小必须<16MB!!");
            //同一个batchCode下的同一个序号文件防重!!上传失败会删除掉这个key
        	String key = String.format(UPLOAD_PARTS_KEY, code,sequence);
            String string = jedisCluster.set(key, "0", SetParams.setParams().nx().ex(60 * 60 * 24 * 7));
        	if (!"OK".equals(string)) {
                log.info("上傳文件-文件code: {} ,序号为:{}，已经上传或正在上传,本次申请已驳回!", code,sequence);
                return new RespEntity<String>().applyRespCodeMsg(CommonRespInfo.REPEAT_PART);
            }
            log.info("上傳文件-开始批量上传-文件code: {} ,序号为:{}",code,sequence);
            try {
            	filecode = logicFileService.upload(logicFile, file,sequence,crc32);
			} catch (Exception e) {
				jedisCluster.del(key);
				throw new ServiceException(CommonRespInfo.SYS_ERROR, e);
			}
        }
        respDto.setLength(file.getSize());
        respDto.setCode(code);
        respDto.setPartCode(filecode);
        respDto.setCrc(crc32);
        respDto.setSequence(sequence);
        log.info("上傳文件-完成: {}",JSON.toJSONString(respDto));
        return new RespEntity<String>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData(code);
    }


    /**
     * 删除文件
     */
    @PostMapping("/delete")
    public RespEntity<Integer> remove(@RequestHeader(value = "sysAlias") String sysAlias,@RequestBody ArrayList<String> codes) {
        Assert.notEmpty(codes,"待删除的文件code集合不能为空");
        log.info("删除文件-开始 ,sysAlias : {} ,codes : {} ", sysAlias, JSON.toJSONString(codes));
        return new RespEntity<Integer>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData(logicFileService.deleteLogicFileByCodes(codes));
    }

    /**
     * 解析expire并设置到logFile中
     * 若expire为null,则默认操作日期一年后的时间为过期时间,比如 now = 2023-02-15 14:20:49 ,则expire = 2024-02-15 14:20:49
     *
     * @param expire    过期时间字符串
     * @param logicFile 目标logicFile
     */
    private void parseExpireAndSetToLogicFile(final String expire, final LogicFile logicFile) {
        if (null != expire) {
            SimpleDateFormat format = new SimpleDateFormat(com.pewee.util.Constants.DATE_FORMAT_DAY);
            try {
                logicFile.setExpire(format.parse(expire));
            } catch (ParseException e) {
                throw new ServiceException(CommonRespInfo.NOT_LEGAL_PARAM);
            }
        } else {
            logicFile.setExpire(DateTime.now().plusYears(1).toDate());
        }
    }
}
