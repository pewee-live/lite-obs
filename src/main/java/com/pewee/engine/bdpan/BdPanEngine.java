package com.pewee.engine.bdpan;

import static com.pewee.util.HttpClient.client;
import static com.pewee.util.HttpClient.fastJsonBean2Map;
import static com.pewee.util.HttpClient.get;
import static com.pewee.util.HttpClient.getIs;
import static com.pewee.util.HttpClient.postMultiPart;
import static com.pewee.util.HttpClient.postUrlEncodedForm;
import static com.pewee.util.HttpClient.rc;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Lists;
import com.pewee.bean.bdpan.BdManageFileInfoDto;
import com.pewee.bean.bdpan.BdManageFileReqDto;
import com.pewee.bean.bdpan.BdManageFileRespDto;
import com.pewee.bean.bdpan.BdQueryFileDto;
import com.pewee.bean.bdpan.BdRespDto;
import com.pewee.bean.bdpan.BdTokenInfo;
import com.pewee.bean.bdpan.PreUpReq;
import com.pewee.engine.EngineInfoEnum;
import com.pewee.engine.FileContext;
import com.pewee.engine.meta.EngineDefinition;
import com.pewee.util.CommonTask;
import com.pewee.util.GenerateCodeUtils;
import com.pewee.util.RedisLock;
import com.pewee.util.StringUtils;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.RespEntity;
import com.pewee.util.resp.ServiceException;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;
/**
 * 百度网盘存储
 * 流程: 项目启动 -> 访问/bdpan/login -> 重定向到百度oauth2 -> 用户登录授权 -> 获取code回调到/bdpan/token -> 通过code获取accessKey -> 正常使用
 * 引擎使用说明:
 * 百度网盘使用必须要通过使用者本人申请百度网盘api平台的密钥 - 需要在控制台中自己创建应用后获得;
 * 本引擎使用的是 百度公共平台oauth2授权码模式授权 ,在控制台 - 应用 - 安全设置里填写OAuth授权回调页,回调页调用本引擎的地址为 - /bdpan/token,对应方法为com.pewee.engine.bdpan.BdPanEngine.receiveBack(HttpServletRequest, String)
 * 使用者必须在配置文件中填写回调地址.必须与在百度openapi平台上填写的保持一致
 * 项目启动后 - 通过访问/bdpan/login发起百度oauth2流程登录授权引擎获取accessToken和refreshToken后引擎才能初始化完成 
 * @author pewee
 *
 */
@Component
@Slf4j
@RestController
@Data
@EqualsAndHashCode(callSuper=false)
public class BdPanEngine extends EngineDefinition{
	@Value("${obs.ip}")
    private String ip;

    @Value("${server.port}")
    private int port;
    
    @Autowired
    private BdPanConfig bdPanConfig;
    @Autowired
    private JedisCluster jedisCluster;
    @Autowired
    private RedisLock redisLock;
    
    //获取code
    private static final String REQ_4_CODE = "http://openapi.baidu.com/oauth/2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=basic,netdisk&scope=basic,netdisk&device_id=%s";
    //获取accesstoken
    private static final String REQ_4_TOKEN = "https://openapi.baidu.com/oauth/2.0/token?grant_type=authorization_code&code=%s&client_id=%s&client_secret=%s&redirect_uri=%s";
    //刷新accesstoken
    private static final String REQ_4_REFRESH = "https://openapi.baidu.com/oauth/2.0/token?grant_type=refresh_token&refresh_token=%s&client_id=%s&client_secret=%s";
    //预上传
    private static final String PRE_UPLOAD = "https://pan.baidu.com/rest/2.0/xpan/file?method=precreate&access_token=%s";  
    //分片上传
    private static final String UPLOAD = "https://d.pcs.baidu.com/rest/2.0/pcs/superfile2?method=upload&path=%s&uploadid=%s&partseq=%s&access_token=%s&type=tmpfile";
    //创建文件
    private static final String CREATE = "https://pan.baidu.com/rest/2.0/xpan/file?method=create&access_token=%s";
    //查询文件-baiduopenapi支持fsids数组同时最多查询100个个fsid,我们这里限制只查一个,因为可以通过前端多线程去调用查询多个文件
    private static final String QUERY = "https://pan.baidu.com/rest/2.0/xpan/multimedia?method=filemetas&access_token=%s&fsids=[%s]&dlink=1";
    //下载文件
    private static final String DOWN = "%s&access_token=%s";
    //管理文件 opera为 copy、move、rename、delete,本次只需要用到delete
    private static final String MANAGE = "https://pan.baidu.com/rest/2.0/xpan/file?method=filemanager&access_token=%s&opera=%s";
    
    
    private static final String BD_ACCESS_TOKEN_KEY = "com.pewee:obs:bd:accesstoken";
    private static final String BD_REFRESH_TOKEN_KEY = "com.pewee:obs:bd:refresh";
    
    private static final String BD_REFRESH_TOKEN_LOCK = "com.pewee:obs:bd:refresh:lock";
    
    //上传路径/obs/系统名/日期/文件名(注意处理重名文件)
    private static final String PATH = "/obs/%s/%s/%s";
    private static final String DATE = "yyyy-MM-dd";
    
    private static final int FOUR_MB = 1 * 1024 *1024 * 4;
    
    //Range头
    private static final String RANGE_FORMAT = "%d-%d";
    
    /**
     * 查看是否初始化
     * @return
     */
    private boolean inited() {
    	return jedisCluster.exists(BD_ACCESS_TOKEN_KEY) && jedisCluster.exists(BD_REFRESH_TOKEN_KEY);
    }
    
    /**
     * 让用户登录baudu,授权获取code
     * @param req
     * @param resp
     * @return
     * @throws IOException
     */
    @GetMapping("/bdpan/login")
    public RespEntity<String> login(HttpServletRequest req,HttpServletResponse resp) throws IOException{
    	resp.sendRedirect(String.format(REQ_4_CODE,bdPanConfig.getAppkey(),bdPanConfig.getBackUrl(), bdPanConfig.getAppid()));
    	return new RespEntity<String>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData("OK");
    }
    
    
    @GetMapping("/bdpan/token")
    public RespEntity<String> receiveBack(HttpServletRequest req, @RequestParam("code")String code){
    	if (StringUtils.isNotBlank(code)) {
    		log.info("baidu存储-获取到code:{},开始获取accesstoken",code);
    		String url = String.format(REQ_4_TOKEN, code,bdPanConfig.getAppkey(),bdPanConfig.getSecretkey(),bdPanConfig.getBackUrl());
    		HttpGet get = new HttpGet(url);
    		get.setConfig(rc);
    		HttpEntity entity = null;
    		try {
    			CloseableHttpResponse response = client.execute(get);
    			entity = response.getEntity();
    			String string = EntityUtils.toString(entity, "utf-8");
    			log.info("返回body：{}",string);
    			BdTokenInfo tokenInfo = JSON.parseObject(string, BdTokenInfo.class);
    			if (null == tokenInfo) {
    				log.error("baidu存储-获取accesstoken失败!!对应code:" + code + ",返回的tokenInfo为空!!");
    				return new RespEntity<String>().applyRespCodeMsg(CommonRespInfo.ERROR_ENGINE);
    			}
    			log.info("获取到了bd盘的回调accessToken:{},expire:{},refrersh_token:{}",tokenInfo.getAccess_token(),tokenInfo.getExpires_in(),tokenInfo.getRefresh_token());
    			jedisCluster.set(BD_ACCESS_TOKEN_KEY, tokenInfo.getAccess_token(), SetParams.setParams().ex(Integer.valueOf(tokenInfo.getExpires_in()) - 60));
    			jedisCluster.set(BD_REFRESH_TOKEN_KEY, tokenInfo.getRefresh_token());
    		} catch (IOException e) {
    			log.error("baidu存储-获取accesstoken失败!!对应code:" + code ,e);
    		} finally {
    			get.abort();
    			if(null !=entity ){
    				try {
						EntityUtils.consume(entity);
					} catch (IOException e) {
						log.error("baidu存储-处理返回信息时错误!!",e);
					}
    			}
    		}
    		return new RespEntity<String>().applyRespCodeMsg(CommonRespInfo.SUCCESS).applyData("OK");
    	} else {
    		log.info("baidu存储-未获取到code!!");
    		return new RespEntity<String>().applyRespCodeMsg(CommonRespInfo.ERROR_ENGINE);
    	}
    	 
    }
    
    /**
     * 上传文件
     * 注意这里返回的是百度网盘的fsid
     */
	@Override
	public String save(FileContext context) {
		if (!inited()) {
			log.error("百度网盘还没初始化");
			throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
		}
		Date now = new Date();
		SimpleDateFormat format = new SimpleDateFormat(DATE);
		String dateString = format.format(now);
		//初始化公共参数
		String path = "";
		String pathUrlEncode = "";
		try {
			path = String.format(PATH, context.getSysCode(),dateString,getFileToken(context));
			pathUrlEncode = URLEncoder.encode(path,"UTF-8");
		} catch (UnsupportedEncodingException e1) {
			throw new ServiceException(CommonRespInfo.ERROR_ENGINE,e1);
		}
		String accessToken = jedisCluster.get(BD_ACCESS_TOKEN_KEY);
		Long fs_id = null ;
		try {
			//预上传 
			BdRespDto preUpResp = preUp(path, context.getContent(), accessToken);
			//分片上传
			if (upload(pathUrlEncode, preUpResp.getUploadid(), preUpResp.getSplitContent(), accessToken)) {
				//创建文件
				fs_id = createFile(preUpResp.getReq(), accessToken);
			} else {
				throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
			}
			
		} catch (Exception e) {
			throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
		}
		return fs_id.toString();
	}
	
	/**
	 * 预上传
	 * 
	 * @return
	 * @throws IOException 
	 */
	private BdRespDto preUp(String path,byte[] arr,String accessToken) throws IOException {
		//切片
		int i = 0;
		ArrayList<String> block_list = new ArrayList<String>();
		ArrayList<byte[]> content_list = new ArrayList<byte[]>();
		int parts = arr.length / FOUR_MB;
		if (0 != arr.length % FOUR_MB) {
			parts = parts + 1;
		}
		while (i < parts ) {
			byte[] seq = null;
			if ( (i + 1) * FOUR_MB <= arr.length) {//看是不是最后一次读字节数组
				seq = Arrays.copyOfRange(arr, i * FOUR_MB , (i + 1)*FOUR_MB);
			} else {
				//这次读不能用4096的长度接收
				seq = Arrays.copyOfRange(arr, i * FOUR_MB , arr.length);
			}
			content_list.add(seq);
			block_list.add(DigestUtils.md5Hex(seq));
			i++;
		}
		//组装报文
		PreUpReq req = new PreUpReq();
		req.setPath(path);
		req.setSize(arr.length);
		req.setBlock_list(block_list.toArray(new String[0]));
		
		log.info("baidu存储-预上传开始req:{}",JSON.toJSONString(req));
		//String form = postJson(String.format(PRE_UPLOAD, accessToken), JSON.toJSONString(req), new HashMap<String,String>());
		String form = postUrlEncodedForm(String.format(PRE_UPLOAD, accessToken), fastJsonBean2Map(req), new HashMap<>());
		BdRespDto respDto = JSON.parseObject(form, BdRespDto.class);
		if (null == respDto) {
			log.error("baidu存储-预上传,返回结果为空!!");
			throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
		}
		if (null != respDto.getErrno() || 0 == respDto.getErrno()) {
			respDto.setSplitContent(content_list);
		}
		respDto.setReq(req);
		req.setUploadid(respDto.getUploadid());
		return respDto;
	}
	
	/**
	 * 上传文件Type: multipart/form-data
	 * curl -F 'file=@example.png' https://example.com/files
	 * 示例:https://c3.pcs.baidu.com/rest/2.0/pcs/superfile2?method=upload&logid=MTY2MTc1NjY3NjQxNjAuMzg2NTc1MDY3ODc2Mjk3MTQ=&app_id=250528&channel=chunlei&web=1&clienttype=0&path=%2Fapps%2F%E6%9D%A5%E8%87%AA%EF%BC%9A%E5%BC%80%E6%94%BE%E5%B9%B3%E5%8F%B0%2FActiviti-develop.zip&uploadid=P1-MTAuMzkuMTcuNzk6MTY2MTc1NjcyODo5MDM5NjI1OTE3OTUyOTEyMzA2&uploadsign=0&partseq=0&access_token=123.d18e75df2824e78ffdb76e6627087182.YsD954OmqHhtmyw7r4vlamDU8U0_6GehQ4axbo-.P-COKw&type=tmpfile
	 * 
	 *	curl -F 'file=@/Downloads/filename.jpg' "https://d.pcs.baidu.com/rest/2.0/pcs/superfile2?access_token=xxx&method=upload&type=tmpfile&path=/apps/AppName/filename.jpg&uploadid=N1-NjEuMTM1LjE2OS44NDoxNTQ1OTY1NTQyOjgzODMxMTY0MDkyNDY2NjQ5Nzg&partseq=0"
	 * @param path 路径
	 * @param upLoadId 本次上传id
	 * @param content_list 切片的文件列表
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	private boolean upload(String path,String upLoadId,List<byte[]> content_list,String accessToken) throws UnsupportedEncodingException {
		log.info("baidu存储-分片上传-开始!,待上传listSize:{}",content_list.size());
		List<Future<String>> futures = new ArrayList<Future<String>>();
		for (int i = 0; i < content_list.size(); i++) {
			String url = String.format(UPLOAD,path,upLoadId,i,accessToken);
			final UploadContext c = new UploadContext();
			c.setUrl(url);
			c.setSeq(i);
			MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
			//multipartEntityBuilder.addBinaryBody("file", content_list.get(i));
			multipartEntityBuilder.addPart("file", new ByteArrayBody(content_list.get(i), "file"));
			c.setMultipartEntityBuilder(multipartEntityBuilder);
			futures.add(CommonTask.submitSingle(new Callable<String>() {

				@Override
				public String call() throws Exception {
					return postMultiPart(c.getUrl(), c.getMultipartEntityBuilder(), new HashMap<>()) ;
				}}));
		}
		List<String> md5list = futures.stream().map( f -> {
			String result;
			try {
				result = f.get();
				log.info("baidu存储-分片上传-返回结果:{}",result);
			} catch (InterruptedException | ExecutionException e) {
				log.error("baidu存储-上传文件失败!!",e);
				return null;
			}
			BdRespDto respDto = JSON.parseObject(result, BdRespDto.class);
			if (null == respDto) {
				log.error("baidu存储-分片上传,返回结果为空!!");
				return null;
			}
			if ( StringUtils.isBlank(respDto.getMd5())  ) {
				log.error("baidu存储-分片上传,返回结果为空!!");
				return null;
			}
			return respDto.getMd5();
		} ).filter( md5 -> StringUtils.isNotBlank(md5) ).collect(Collectors.toList());
		if (md5list.size() != content_list.size()) {
			log.info("baidu存储-分片上传-失败!,返回md5listSize:{}",md5list.size());
			return false;
		}
		log.info("baidu存储-分片上传-完成!,返回md5list:{}",JSON.toJSONString(md5list));
		return true;
	}
	
	
	
	@Data
	private static class UploadContext {
		private String url;
		
		private int seq;
		
		private MultipartEntityBuilder multipartEntityBuilder;
		
	} 
	
	/**
	 * 创建文件
	 * @param req 预上传时的请求参数
	 * !!他妈的,这里上传的block_list是第二部返回的md5list,注意对照
	 * 示例:https://pan.baidu.com/rest/2.0/xpan/file?method=create&isdir=0&access_token=123.d18e75df2824e78ffdb76e6627087182.YsD954OmqHhtmyw7r4vlamDU8U0_6GehQ4axbo-.P-COKw&app_id=250528&channel=chunlei&web=1&clienttype=0
	 * path=%2Fapps%2F%E6%9D%A5%E8%87%AA%EF%BC%9A%E5%BC%80%E6%94%BE%E5%B9%B3%E5%8F%B0%2FActiviti-develop.zip&size=7499933&uploadid=P1-MTAuODguODYuMjE5OjE2NjE3NjE3NTU6OTA0MDk3NTM2NDY4NDI1NDg1Mw%3D%3D&target_path=%2Fapps%2F%E6%9D%A5%E8%87%AA%EF%BC%9A%E5%BC%80%E6%94%BE%E5%B9%B3%E5%8F%B0%2F&block_list=%5B%229418a1d529db0e611b78be2419e14c82%22%2C%2285a8f6a19d0dbf1192d3159f1a84842e%22%5D&local_mtime=1647242613
	 * @return
	 */
	private Long createFile(PreUpReq req,String accessToken) throws Exception{
		log.info("baidu存储-开始创建文件:{}",JSON.toJSONString(req));
		String url = String.format(CREATE, accessToken);
		String postJson = postUrlEncodedForm(url, fastJsonBean2Map(req), new HashMap<>());
		//String postJson = postJson(url, JSON.toJSONString(req), new  HashMap<>());
		BdRespDto respDto = JSON.parseObject(postJson, BdRespDto.class);
		if (null == respDto) {
			log.error("baidu存储-预上传,返回结果为空!!");
			throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
		}
		if (null == respDto.getErrno() || 0 != respDto.getErrno() || null == respDto.getFs_id()) {
			throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
		}
		return respDto.getFs_id();
	}

	@Override
	public int deleteFile(com.pewee.bean.File flie) {
		if (!inited()) {
			log.error("百度网盘还没初始化");
			throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
		}
		return deleteFiles(Lists.newArrayList(flie));
	}

	@Override
	public int deleteFiles(List<com.pewee.bean.File> files) {
		if (!inited()) {
			log.error("百度网盘还没初始化");
			throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
		}
		String accessToken = jedisCluster.get(BD_ACCESS_TOKEN_KEY);
		List<String> fsidList = files.stream().map(com.pewee.bean.File::getToken).collect(Collectors.toList());
		String fsids = String.join(",", fsidList);
		BdManageFileRespDto fileRespDto = null;
		try {
			String string = get(String.format(QUERY, accessToken,fsids), null);
			BdQueryFileDto result = JSON.parseObject(string, new TypeReference<BdQueryFileDto>() {});
			if (null == result || 0 != result.getErrno() ) {
				throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
			}
			log.info("baidu存储-删除文件-查询文件fsids:{}信息完成!:{}",fsids,JSON.toJSONString(result));
			if ( null == result.getList() || result.getList().isEmpty() || fsidList.size() != result.getList().size()) {
				throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
			}
			List<String> pathList = result.getList().stream().map(m -> m.get("path")).collect(Collectors.toList());
			BdManageFileReqDto bdManageFileReqDto = new BdManageFileReqDto();
			bdManageFileReqDto.setFilelist(pathList.toArray(new String[0]));
			fileRespDto = bdDeleteFile(bdManageFileReqDto, accessToken);
		} catch (IOException e) {
			throw new ServiceException(CommonRespInfo.ERROR_ENGINE,e);
		}
		List<BdManageFileInfoDto> info = fileRespDto.getInfo();
		List<BdManageFileInfoDto> failList = info.stream().filter( var -> var.getErrno()!=0  ).collect(Collectors.toList());
		if (null != failList && !failList.isEmpty()) {
			log.info("baidu存储-删除文件-以下文件: {} 删除失败!,进行重试(不保证删除结果)!",JSON.toJSON(failList));
			BdManageFileReqDto bdManageFileReqDto = new BdManageFileReqDto();
			bdManageFileReqDto.setFilelist(failList.stream().map(BdManageFileInfoDto::getPath).collect(Collectors.toList()).toArray(new String[0]));
			for (int j = 0 ;j< 3 ; j++) {
				try {
					bdDeleteFile(bdManageFileReqDto, accessToken);
				} catch (Exception e) {
					log.error("baidu存储-删除文件-重试删除发生失败!!",e);
				}
			}
		}
		return (int)info.stream().filter( var -> var.getErrno()==0  ).count();
	}
	
	/**
	 * 调用百度删除
	 * @param req
	 * @param accessToken
	 * @return
	 */
	private BdManageFileRespDto bdDeleteFile(BdManageFileReqDto req,String accessToken) {
		log.info("baidu存储-删除文件-文件开始!:{}",JSON.toJSONString(req));
		BdManageFileRespDto fileRespDto = null;
		try {
			
			String form = postUrlEncodedForm(String.format(MANAGE, accessToken,"delete"), fastJsonBean2Map(req), new HashMap<>());
			fileRespDto = JSON.parseObject(form, new TypeReference<BdManageFileRespDto>() {});
		} catch (IOException e) {
			throw new ServiceException(CommonRespInfo.ERROR_ENGINE,e);
		}
		if ((null == fileRespDto || 0 != fileRespDto.getErrno() )) {
			throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
		}
		log.info("baidu存储-删除文件-文件结果!:{}",JSON.toJSONString(fileRespDto));
		return fileRespDto;
	}

	@Override
	public void load() throws Exception{
		
	}

	@Override
	protected void loginAndAutoRefreshSession() {
		//定时任务刷新!!
		CommonTask.executor1.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				if (inited()) {
					Long ttl = jedisCluster.ttl(BD_ACCESS_TOKEN_KEY);
					if (ttl < 60L) {
						log.info("baidu存储-将开始刷新百度accesstoken!!");
						//获取锁
						String nextId = GenerateCodeUtils.nextId();
						boolean flag = false;
						try {
							flag = redisLock.tryLock(BD_REFRESH_TOKEN_LOCK, nextId, 15*1000,false);
							if (flag) {
								//更新token
								refreshToken();
							}
						} catch (Exception e) {
							log.error(e.getMessage(),e);
						} finally {
							if (flag) {
								redisLock.releaseLock(BD_REFRESH_TOKEN_LOCK, nextId, 15*1000);
							}
						}
					} else {
						log.info("baidu存储-无需刷新百度accesstoken!!");
					}
				} else {
					log.error("baidu存储-还没初始化accesstoken和refreshtoken,无法刷新!!请先在浏览器手动刷新token");
				}
			}}, 1000L, 24*60*60*
				1000L, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * 注意这个方法只能在BdPanEngine#loginAndAutoRefreshSession()中被调用
	 */
	private void refreshToken() {
		String refreshToken = jedisCluster.get(BD_REFRESH_TOKEN_KEY);
		String url = String.format(REQ_4_REFRESH,refreshToken ,bdPanConfig.getAppkey(),bdPanConfig.getSecretkey());
		HttpGet get = new HttpGet(url);
		get.setConfig(rc);
		HttpEntity entity = null;
		try {
			CloseableHttpResponse response = client.execute(get);
			entity = response.getEntity();
			String string = EntityUtils.toString(entity, "utf-8");
			log.info("返回body：{}",string);
			BdTokenInfo tokenInfo = JSON.parseObject(string, BdTokenInfo.class);
			if (null == tokenInfo) {
				log.error("baidu存储-刷新accesstoken失败!!对应refreshtoken:" + refreshToken + ",返回的tokenInfo为空!!");
			}
			log.info("baidu存储-刷新accesstoken成功-accessToken:{},expire:{},refrersh_token:{}",tokenInfo.getAccess_token(),tokenInfo.getExpires_in(),tokenInfo.getRefresh_token());
			jedisCluster.set(BD_ACCESS_TOKEN_KEY, tokenInfo.getAccess_token(), SetParams.setParams().ex(Integer.valueOf(tokenInfo.getExpires_in()) - 60));
			jedisCluster.set(BD_REFRESH_TOKEN_KEY, tokenInfo.getRefresh_token());
		} catch (IOException e) {
			log.error("baidu存储-获取accesstoken失败!!对应refreshtoken:" + refreshToken ,e);
		} finally {
			get.abort();
			if(null !=entity ){
				try {
					EntityUtils.consume(entity);
				} catch (IOException e) {
					log.error("baidu存储-处理返回信息时错误!!",e);
				}
			}
		}
	}

	@Override
	public EngineInfoEnum getDefinition() {
		return EngineInfoEnum.BAIDU;
	}
	


	@Override
	public List<InputStream> doGetStream(List<com.pewee.bean.File> fs) throws IOException {
		List<InputStream> list = new ArrayList<>();
		String accessToken = jedisCluster.get(BD_ACCESS_TOKEN_KEY);
		List<String> fsidList = fs.stream().map(com.pewee.bean.File::getToken).collect(Collectors.toList());
		List<String> fsidListtmp = new ArrayList<>();
		Map<String,String> dLinkMap = new HashMap<>();
		int j = 0;//用于计算批次数
		for (int i = 0 ; i < fsidList.size() ;  i++) {
			fsidListtmp.add(fsidList.get(i));
			//每拿100个文件或者拿完了
			if (fsidListtmp.size() == 100 || i == (fsidList.size() -1)) {
				//需要将之前的fsid全拿出去查询一次
				j++;
				String fsids = String.join(",", fsidListtmp);
				String string = get(String.format(QUERY, accessToken,fsids), null);
				//查出的dlink变为无序,需要我们自己排序
				BdQueryFileDto result = JSON.parseObject(string, new TypeReference<BdQueryFileDto>() {});
				if (null == result || 0 != result.getErrno() ) {
					throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
				}
				log.info("baidu存储-下载文件-第: {} 批查询 - 查询文件 fsid 个数 : {} ; \r\n 内容 :{} \r\n 信息完成!:{}",j,fsidListtmp.size(),fsids,JSON.toJSONString(result));
				if ( null == result.getList() || result.getList().isEmpty() || result.getList().size() != fsidListtmp.size()) {
					throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
				}
				fsidListtmp.clear();
				//取出每个fsid对应的dlink
				result.getList().forEach( m -> {
					dLinkMap.put( m.get("fs_id"), m.get("dlink"));
				}  );
			}
		}
		
		try {
			for (String fsid : fsidList) {
				Map<String,String> publicheaders = new HashMap<String,String>();
				publicheaders.put("User-Agent", "pan.baidu.com");
				//百度网盘不允许大范围range且不支持大量线程
				/**
				if (false && length > FOUR_MB) {
					//多线程下载优化
					//计算线程数量
					int threads = (int) ((length/ (FOUR_MB)) + 1);
					log.info("baidu存储-下载文件-文件fsid:{}将开启:{}个线程下载",token,threads);
					List<Map<String, String>> downThread = new ArrayList<Map<String, String>>();
					for (int i= 0; i< threads;i++) {
						//计算该线程下载的范围
						long start = FOUR_MB * i;
						long end = FOUR_MB * ( i + 1) - 1 ;
						if (i == (threads - 1)) {
							end = -1;
						}
						Map<String, String> thisHead = new HashMap<String,String>();
						thisHead.put("Range", String.format(RANGE_FORMAT, start,end));
						thisHead.putAll(publicheaders);
						downThread.add(thisHead);
					}
					@SuppressWarnings("unchecked")
					CompletableFuture<byte[]>[] futures = downThread.stream().map(t -> {
						return CompletableFuture.supplyAsync(() -> {
							try {
								return getByteArr(String.format(DOWN,dLink, accessToken), t);
							} catch (IOException e) {
								log.error("baidu存储-下载文件-:" + t.get("Range") + " 失败!!", e);
								throw new ServiceException(CommonRespInfo.ERROR_ENGINE,e);
							}
						},CommonTask.executor).handle((ret, ex) -> {
			                if (null != ex) {
			                	log.error("baidu存储-下载文件失败!!", ex);
			                	throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
			                }
			                return ret;
			            });
					}).filter(byarr -> null != byarr).toArray(CompletableFuture[]::new);
					if (futures.length != downThread.size() ) {
						log.error("baidu存储-分片下载文件失败!!");
						throw new ServiceException(CommonRespInfo.ERROR_ENGINE);
					}
					CompletableFuture.allOf(futures).join();
					List<byte[]> dtos = Stream.of(futures).map(f -> {
			            try {
			                return f.get();
			            } catch (InterruptedException | ExecutionException e) {
			            	log.error("baidu存储-分片下载文件失败!!");
			            	throw new ServiceException(CommonRespInfo.ERROR_ENGINE,e);
			            }
			        }).filter(dto -> null != dto).collect(Collectors.toList());
					for (byte[] bs : dtos) {
						arr = addBytes(arr, bs);
					}
				}  else {**/
					InputStream is = getIs(String.format(DOWN,dLinkMap.get(fsid), accessToken), publicheaders);
					list.add(is);
				/**}**/
			}
		} catch (IOException e) {
			log.error("百度engine获取多文件流- 失败 - 需要关闭前面的流!!共需关闭:" + list.size() + "个流",e);	
			if (!list.isEmpty()) {
				for (InputStream inputStream : list) {
					try {
						IOUtils.closeQuietly(inputStream);
					} catch (Exception e1) {
						log.error("百度engine获取多文件流- 失败 - 关闭前面的流时失败!!",e1);
					}
				}
			}
			throw new ServiceException(CommonRespInfo.SYS_ERROR,e);
		} finally {
		}
		return list;
	} 

}
