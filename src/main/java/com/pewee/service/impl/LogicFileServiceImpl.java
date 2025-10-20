package com.pewee.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.pewee.bean.File;
import com.pewee.bean.LogicFile;
import com.pewee.bean.SingleUploadFileContext;
import com.pewee.bean.dto.DownloadLogicFileRespDto;
import com.pewee.bean.dto.MultiStreamDto;
import com.pewee.bean.dto.SingleStreamDto;
import com.pewee.engine.DynamicLink;
import com.pewee.engine.EngineAccess;
import com.pewee.engine.EngineRegistor;
import com.pewee.mapper.LogicFileMapper;
import com.pewee.service.ILogicFileService;
import com.pewee.util.CommonTask;
import com.pewee.util.GenerateCodeUtils;
import com.pewee.util.StringUtils;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.ServiceException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.PureJavaCrc32;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.Base64Utils;
import org.springframework.web.multipart.MultipartFile;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;
import tk.mybatis.mapper.entity.Example;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 逻辑文件Service业务层处理
 * 
 * @author pewee
 * @date 2022-04-13
 */
@Service
@Transactional
@Getter
@Setter
public class LogicFileServiceImpl implements ILogicFileService {

    private static final Logger log = LoggerFactory.getLogger(LogicFileServiceImpl.class);

    private static final String PREUPLOAD_KEY = "com.pewee.obs:logic_file:preupload:%s";

    private final Integer SPLIT = 1;
    private final Integer UPLOADED = 2;
    private final Integer DELETED = 3;

    @Value("${split.file.lease.time.seconds}")
    private long maxSplitFileLeaseTimeSeconds;
    @Autowired
    private LogicFileMapper logicFileMapper;
    @Autowired
    private FileServiceImpl fileServiceImpl;
    @Autowired
    private EngineRegistor engineRegistor;
    @Autowired
    private JedisCluster jedisCluster;

    @Autowired
	private DynamicLink dynamicLink;

	@Value("${obs.ip}")
    private String ip;

    @Value("${server.port}")
    private int port;

    private static final String LOGIC_FILE_SPLIT_FILE_COUNTER = "com.pewee:obs:logicfile:split:counter:%s";


    @Override
    public LogicFile selectByKey(Object key) {
        return logicFileMapper.selectByPrimaryKey(key);
    }

	@Override
	public int save(LogicFile entity) {
		return logicFileMapper.insertSelective(entity);
	}

	@Override
	public int delete(Object key) {
		return logicFileMapper.deleteByPrimaryKey(key);
	}

	@Override
	public int updateAll(LogicFile entity) {
		return logicFileMapper.updateByPrimaryKey(entity);
	}

	@Override
	public int updateNotNull(LogicFile entity) {
		return logicFileMapper.updateByPrimaryKeySelective(entity);
	}

	@Override
	public List<LogicFile> selectByExample(Object example) {
		return logicFileMapper.selectByExample(example);
	}

	@Override
	public int batchInsert(List<LogicFile> entities) {
		return logicFileMapper.insertLogicFileListSelective(entities);
	}

	@Override
	public List<LogicFile> selectAll() {
		return logicFileMapper.selectAll();
	}
	
	@Override
	public int batchInsertSelective(List<LogicFile> entities) {
		return logicFileMapper.insertLogicFileListSelective(entities);
	}

    /**
     * 根据code批量修改逻辑文件
     *
     * @param logicFileList 逻辑文件 List
     * @return 结果
     */
    @Override
    public int updateLogicFileListSelectiveByCode(List<LogicFile> logicFileList) {
    	return logicFileMapper.updateLogicFileListSelectiveByCode(logicFileList);
    }     
    
    /**
     * 查询逻辑文件列表
     *
     * @param logicFile 逻辑文件
     * @return 逻辑文件集合
     */
    @Override
    public List<LogicFile> selectLogicFileList(LogicFile logicFile) {
    	return logicFileMapper.selectLogicFileList(logicFile);
    }
    
    /**
     * 批量删除逻辑文件
     * 
     * @param codes 需要删除的逻辑文件code集合
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteLogicFileByCodes(List<String> codes) {
        //1.查询所有待删除的逻辑文件信息
        List<LogicFile> logicFiles = logicFileMapper.selectLogicFileListByCodes(codes);
        Assert.notEmpty(logicFiles, "待删除的逻辑文件code集合查询出的逻辑文件为空!");
        //2.将所有logicFiles的status改成3,已删除
        int result = logicFileMapper.logicallyDeleteLogicFileByCodes(codes);
        //3.开启异步任务,去做之后的删除操作
        CommonTask.executor.execute(() -> {
            try {
                fileServiceImpl.deleteFileByLogicFiles(logicFiles);
            } catch (ServiceException e) {
                log.error("删除逻辑文件异常!!   " + e.getMessage(), e);
            }
        });
        return result;
    }
    
    /**
     * 查询逻辑文件
     * @param code code
     * @return 逻辑文件
     */
    @Override
    public LogicFile selectLogicFileByCode(String code) {
        return logicFileMapper.selectLogicFileByCode(code);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogicFile preupload(LogicFile logicFile,List<String> subCrcList) {
        logicFile.setCode(GenerateCodeUtils.nextId());
        logicFile.setStatus(0);
        logicFileMapper.insertSelective(logicFile);
        cacheSplitCrc(logicFile.getCode(),subCrcList);
        return logicFile;
    }

    /**
     * 不需要保证事务
     * @param logicFileCodes 逻辑文件 code集合
     */
    @Override
    public void settingStatusToUploadedByCodes(List<String> logicFileCodes) {
        if (logicFileCodes.isEmpty()){
            return ;
        }
        List<LogicFile> logicFilesForHandle = getSplitLogicFilesThatNotUploadedAndNotDeletedFromCodes(logicFileCodes);

        logicFilesForHandle.forEach(var -> {
            Integer totalNumberOfFiles = var.getTotal();
            String logicFileCode = var.getCode();
            Long expectedTotalSize = var.getLength();
            List<File> files = fileServiceImpl.selectFilesByBatchCode(logicFileCode);
            if (totalNumberOfFiles != null && files.size() == totalNumberOfFiles) {
                //如果所有上传的分片其length之和与最初预上传文件时声明的length不符合,直接走删除
                Long actualTotalSize = files.stream().mapToLong(File::getLength).sum();
                LogicFile logicFileForHandle = new LogicFile();
                logicFileForHandle.setId(var.getId());
                if (!expectedTotalSize.equals(actualTotalSize)){
                    log.info("更新预上传逻辑文件状态为已上传-由于实际上传文件总长与预上传声明长度不符合,将逻辑文件及实际文件进行删除,LogicFile code:{}",logicFileCode);
                    fileServiceImpl.deleteFileByLogicFiles(Lists.newArrayList(var));
                    logicFileForHandle.setStatus(DELETED);
                }else{
                    log.info("更新预上传逻辑文件状态为已上传-开始进行状态更新,将更新为已上传,LogicFile code:{}",logicFileCode);
                    logicFileForHandle.setStatus(UPLOADED);
                }
                logicFileMapper.updateByPrimaryKeySelective(logicFileForHandle);
                //设置为已完成后,尝试清除缓存中的切片crc数据
                clearSplitCrcCache(logicFileCode);
            }
        });
    }

    @Override
    public void deleteDelayLogicFile(List<String> logicFileCodes) {
        if (logicFileCodes.isEmpty()){
            return;
        }
        List<LogicFile> splitLogicFilesThatNotUploadedAndNotDeleted = getSplitLogicFilesThatNotUploadedAndNotDeletedFromCodes(logicFileCodes);
        List<LogicFile> logicFilesForHandle = splitLogicFilesThatNotUploadedAndNotDeleted.stream().filter(var->{
            long createTime = var.getCreateTime().getTime();
            long maxReleaseTime = createTime + maxSplitFileLeaseTimeSeconds * 1000;
            return System.currentTimeMillis() >= maxReleaseTime;
        }).collect(Collectors.toList());
        logicFilesForHandle.forEach(var->{
            fileServiceImpl.deleteFileByLogicFiles(Lists.newArrayList(var));
            LogicFile logicDelete = new LogicFile();
            logicDelete.setId(var.getId());
            logicDelete.setStatus(DELETED);
            logicFileMapper.updateByPrimaryKeySelective(logicDelete);
            //设置为 删除后,尝试清除缓存中的切片crc数据
            clearSplitCrcCache(var.getCode());
        });
    }

    @Override
    public boolean cacheSplitCrc(String logicFileCode, List<String> subCrcList) {
        String key = String.format(PREUPLOAD_KEY,logicFileCode);
        String operatorInfo = jedisCluster.set(key, JSON.toJSONString(subCrcList), SetParams.setParams().ex((int) maxSplitFileLeaseTimeSeconds));
        return "OK".equals(operatorInfo);
    }
    
    @Override
	public List<String> getSplitCrcCache(String logicFileCode) {
    	String key = String.format(PREUPLOAD_KEY,logicFileCode);
    	String string = jedisCluster.get(key);
	    return JSON.parseArray(string, String.class);
	}

    @Override
    public boolean clearSplitCrcCache(String logicFileCode) {
        Assert.hasText(logicFileCode,"逻辑文件code不能为空!");
        return jedisCluster.del(String.format(PREUPLOAD_KEY,logicFileCode)) == 1;
    }

    private List<LogicFile> getSplitLogicFilesThatNotUploadedAndNotDeletedFromCodes(List<String> logicFileCodes) {
        Example example = new Example(LogicFile.class);
        example.createCriteria().andIn("code", logicFileCodes);
        List<LogicFile> logicFiles = logicFileMapper.selectByExample(example);
        return logicFiles.stream().filter(var -> {
            Integer status = var.getStatus();
            boolean isSplit = SPLIT.equals(var.getSplit());
            boolean notUploaded = status != null && !UPLOADED.equals(status);
            boolean notDeleted = status != null && !DELETED.equals(status);
            return isSplit && notUploaded && notDeleted ;
        }).collect(Collectors.toList());
    }


	/**
	 * 獲取下載信息
	 *  forceStaticLink : 是否强行生成静态链接 这个配置对分片链接的生成无影响!!
	 *  
	 */
	@Override
	public DownloadLogicFileRespDto getDownloadInfo(LogicFile logicFile,Boolean forceStaticLink) {
		List<File> list = fileServiceImpl.selectByLogicFileCode(logicFile.getCode());
		Integer storageType = list.stream().map(File::getStorageType).filter(Objects::nonNull).findFirst().orElseThrow(()-> new IllegalArgumentException("未找到存储类型!"));
		EngineAccess engineAccess = engineRegistor.switchTo(storageType);
		DownloadLogicFileRespDto respDto = new DownloadLogicFileRespDto();
		BeanUtils.copyProperties(logicFile, respDto);
		respDto.setFiles(engineAccess.getDownloadUrls(logicFile,list));
		//TODO 生成临时链接逻辑
		respDto.setUrl(generateMergeUrl(logicFile,forceStaticLink));
		return respDto;
	}

	/**
	 * 生成合并文件的下载链接
	 * @param logicFile
	 * @param forceStaticLink 
	 * @return
	 */
	private String generateMergeUrl(LogicFile logicFile, Boolean forceStaticLink) {
		String logicFileJson = JSON.toJSONString(logicFile);
        //将logicFileJson通过base64进行编码
        String info = Base64Utils.encodeToUrlSafeString(logicFileJson.getBytes(java.nio.charset.Charset.forName("UTF-8")));
		StringBuilder sb = new StringBuilder("");
        sb.append("http://" + ip +  ":" + port + "/pewee/logicfile/download/merge/");
        if (null != forceStaticLink && forceStaticLink) {
        	//强制生成静态链接
        	log.info("文件 : {} 生成静态链接!" ,JSON.toJSONString(logicFile));
        	sb.append(logicFile.getCode());
        	sb.append("?info=" + info);
        	sb.append("&dynamicLink=false");
        } else {
        	//照旧
        	sb.append(getDynamicLink().generateDynamicLink(logicFile.getCode()) );
        	sb.append("?info=" + info);
        }
        return  sb.toString();
	}

	/**
	 * 上傳
	 */
	@Override
	@Transactional
	public String upload(LogicFile logicFile, MultipartFile file,Integer seq,String crc32) {
		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException e1) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR,e1);
		}
		SingleUploadFileContext fileContext = new SingleUploadFileContext();
		PureJavaCrc32 pureJavaCrc32 = new PureJavaCrc32();
		pureJavaCrc32.update(bytes, 0, bytes.length);
		String crc = Long.toHexString(pureJavaCrc32.getValue());
		if (null == logicFile.getId()) {
			//快速上传
			logicFile.setCode(GenerateCodeUtils.nextId());
			if (file.getOriginalFilename().length() > 100) {
				logicFile.setFileName(file.getOriginalFilename().substring(file.getOriginalFilename().length() - 100, file.getOriginalFilename().length()));
			} else {
				logicFile.setFileName(file.getOriginalFilename());
			}
			logicFile.setLength(file.getSize());
			logicFile.setMimeType(file.getContentType());
			logicFile.setCrc32(crc);
			logicFile.setTotal(1);
			logicFile.setCreateTime(new Date());
			BeanUtils.copyProperties(logicFile, fileContext);
		} else {
			//预上传
			BeanUtils.copyProperties(logicFile, fileContext);
			fileContext.setLength(file.getSize());
			fileContext.setFileName(logicFile.getCode() + ".part" + seq);
			fileContext.setSeq(seq);
    		Assert.isTrue(crc.equals(crc32), "分片上传文件 - 分片CRC不一致!!!请检查文件");
    		fileContext.setCrc32(crc32);
    		List<String> splitCrcCache = getSplitCrcCache(logicFile.getCode());
    		Assert.isTrue(null != splitCrcCache && splitCrcCache.size() == logicFile.getTotal(), "分片上传文件 - code : " + logicFile.getCode() + " 获取的crc缓存错误!!!!");
    		Assert.isTrue(crc32.equals(splitCrcCache.get(seq)) , "分片上传文件 - code : " + logicFile.getCode() + " 获取的crc缓存与上传文件CRC不一致!!" + splitCrcCache.get(seq) + "," + crc32);
		}
		fileContext.setBatchCode(logicFile.getCode());
		String filecode = fileServiceImpl.upload(engineRegistor.switchTo(engineRegistor.getDefaultType()),fileContext,bytes);
		if (null == logicFile.getId()) {
			//快速上传
			logicFile.setStatus(2);
			logicFileMapper.insertSelective(logicFile);
		} else {
			//预上传
			if (logicFile.getStatus() != 1) {
				logicFile.setStatus(1);
				logicFileMapper.updateLogicFile(logicFile);
			}
			//预上传累加分片计数器
			String key = String.format(LOGIC_FILE_SPLIT_FILE_COUNTER, logicFile.getCode());
			Long now = jedisCluster.incr(key);
			jedisCluster.expire(key, 60 * 60 * 24 * 7);
			if (now.intValue() == logicFile.getTotal()) {
				//需要通知更新状态
				settingStatusToUploadedByCodes(Lists.newArrayList(logicFile.getCode()));
			}
		}
		return filecode;
	}

	/**
	 * 获取某个逻辑文件的全部分片下载流
	 * 并将其下载次数扣减1
	 * 废弃原因见 @code{com.pewee.controller.LogicFileController#getMerge)}
	 */
	@Override
	@Deprecated 
	public MultiStreamDto getMultiStream(String code,Boolean dynamicLink) {
		//转换code
		String transformCode = null;
		if (null != dynamicLink && !dynamicLink) {
			transformCode = code;
		} else {
			transformCode = getDynamicLink().transformCode(code);
		}
		LogicFile logicFile = selectLogicFileByCode(transformCode);
        Assert.notNull(logicFile, "該文件不存在");
        Assert.isTrue(logicFile.getStatus() == 2, "文件尚未上傳完成或已刪除");
        Assert.isTrue(System.currentTimeMillis() < logicFile.getExpire().getTime(), "文件已過期");
        MultiStreamDto multiStreamDto = new MultiStreamDto();
        multiStreamDto.setFile(logicFile);
        List<File> list = fileServiceImpl.selectByLogicFileCode(logicFile.getCode());
		Integer storageType = list.stream().map(File::getStorageType).filter(Objects::nonNull).findFirst().orElseThrow(()-> new IllegalArgumentException("未找到存储类型!"));
        list.sort(Comparator.comparing(File::getSequence));
        List<InputStream> streamList;
        try {
        	streamList = engineRegistor.switchTo(storageType).doGetStream(list);
		} catch (IOException e) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR, e);
		}
        multiStreamDto.setStreamlist(streamList);
        getDynamicLink().countDown(code);
		return multiStreamDto;
	}

	@Override
	public void getSingleStream(SingleStreamDto singleStreamDto) {
		Integer storageType = singleStreamDto.getSinglefile().stream().map(File::getStorageType).filter(Objects::nonNull).findFirst().orElseThrow(()-> new IllegalArgumentException("未找到存储类型!"));
		try {
			singleStreamDto.setStream(engineRegistor.switchTo(storageType).doGetStream(Lists.newArrayList(singleStreamDto.getSinglefile().get(singleStreamDto.getSequence()))).get(0));
		} catch (IOException e) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR, e);
		}
		singleStreamDto.setSequence(singleStreamDto.getSequence() + 1);
	}

	@Override
	public SingleStreamDto getMergeFileInfo(String code, Boolean dynamicLink) {
		 SingleStreamDto singleStreamDto = new SingleStreamDto();
	     singleStreamDto.setMaskcode(code);
	     singleStreamDto.setDynamicLink(dynamicLink);
	     singleStreamDto.setSequence(0);
	     //转换code
	     String transformCode = null;
	     if (null != singleStreamDto.getDynamicLink() && !singleStreamDto.getDynamicLink()) {
	    	 transformCode = singleStreamDto.getMaskcode();
	     } else {
	    	 transformCode = getDynamicLink().transformCode(singleStreamDto.getMaskcode());
	     }
	     LogicFile logicFile = selectLogicFileByCode(transformCode);
	     Assert.notNull(logicFile, "該文件不存在");
	     Assert.isTrue(logicFile.getStatus() == 2, "文件尚未上傳完成或已刪除");
	     Assert.isTrue(System.currentTimeMillis() < logicFile.getExpire().getTime(), "文件已過期");
	     singleStreamDto.setFile(logicFile);
	     List<File> list = fileServiceImpl.selectByLogicFileCode(logicFile.getCode());
	     list.sort(Comparator.comparing(File::getSequence));
	     singleStreamDto.setSinglefile(list);
		return singleStreamDto;
	}
}
