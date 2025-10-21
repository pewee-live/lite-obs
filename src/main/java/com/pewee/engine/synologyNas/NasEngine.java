package com.pewee.engine.synologyNas;

import com.alibaba.fastjson.JSON;
import com.pewee.bean.File;
import com.pewee.bean.nas.api.genericresponses.Error;
import com.pewee.bean.nas.api.genericresponses.NasResponse;
import com.pewee.bean.nas.client.SynoRestClient;
import com.pewee.bean.nas.client.exception.NasAuthenticationFailureException;
import com.pewee.bean.nas.client.exception.NasCompatibilityException;
import com.pewee.engine.FileContext;
import com.pewee.engine.EngineInfoEnum;
import com.pewee.engine.meta.EngineDefinition;
import com.pewee.engine.synologyNas.NasConfig.NasAuth;
import com.pewee.util.CommonTask;
import com.pewee.util.GenerateCodeUtils;
import com.pewee.util.RedisLock;
import com.pewee.util.StringUtils;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.ServiceException;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisCluster;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * NAS引擎改造相关
 * 1. NasConfig 现在包含了多个NAS配置,通过InitializingBean在初始化时以namespace来做区分,
 * 	一个springboot进程中需要指定一个优先的配置,每个配置会初始化一个对应的SynoRestClient
 * 2.SynoRestClient 改为由NasEngine来配置启动(com.pewee.bean.nas.api.helper.QueryURLBuilder的NASURL改造及UserSession中key的改造)
 * 3.关于不同NAS中获取Session的锁改造
 * 4.NasEngine在做文件相关处理时,会优先选择NAS配置中prefer=true的那个配置来做IO操作;
 * 5.在tbl_file中添加engine_namespace的字段用于标识当前storage_type下选取的engine_namespace,当文件上传时,根据上传者账号进行存储实例的选择
 * 
 * 6.202508对上述第4条进行优化,将会优先选择syscode配置的namespace实例来做IO操作,如果该账号没有做任何配置,则进行4的处理逻辑
 * @author pewee
 * 2024年5月24日
 */
@Component
@RestController
@Slf4j
public class NasEngine extends EngineDefinition implements DisposableBean,InitializingBean{

    @Autowired
    private NasConfig conf;
    @Autowired
    private JedisCluster jedisCluster;
    @Autowired
    @Qualifier(value = "nasClientRestTemplate")
    private RestTemplate nasClientRestTemplate;
    
    private NasServerContainer serverContainer;
    
    @Autowired
    private RedisLock redisLock;

    @Override
	public void afterPropertiesSet() throws Exception {
    	List<NasAuth> authList = conf.getAuthList();
    	this.serverContainer = new NasServerContainer();
    	authList.forEach( config -> {
    		if (config.isPrefer()) {
    			serverContainer.setPrefer(config.getNamespace());
    			log.info("NAS引擎启动中 --> 设置默认NasServer为: {} ",config.getNamespace());
    		}
    		serverContainer.getNasConfigMap().put(config.getNamespace(), config);
    		//初始化client
    		SynoRestClient client = new SynoRestClient(nasClientRestTemplate,config,jedisCluster);
    		serverContainer.getSynoClientMap().put(config.getNamespace(), client);
    	} );
    	if (StringUtils.isBlank(serverContainer.getPrefer())) {
			throw new ServiceException(CommonRespInfo.UNKNOWN_NAS_ENGINE_PREFERCONFIG);
		}
	}


	@Value("${obs.ip}")
    private String ip;

    @Value("${server.port}")
    private int port;

    private final static String HTTP_PREFIX = "http://";

    private static final String DATE_FORMAT = "yyyyMMdd";

    private static final String PATH_PREFIX = "/obs/";
    
    //obs sid刷新锁
    private static final String OBS_AUTHENTICATION_LOCK_KEY = "com.pewee:obs:nas:authentication:getlock:%s";
    
    //obs 定时任务更新sid锁
    private static final String NAS_REFRESH_TOKEN_LOCK = "com.pewee:obs:nas:refreshtoken:lock";

    private static final int SID_NOT_FOUND = 119;
    
    @Override
    public EngineInfoEnum getDefinition() {
        return EngineInfoEnum.SYNOLOGYNAS;
    }

    @Override
    public void load() {
       
    }

    @Override
    public void destroy() {
        log.info("nasEngine开始销毁,准备调用nas远程文件工作站登出方法...");
        Map<String, SynoRestClient> synoClientMap = serverContainer.getSynoClientMap();
        synoClientMap.forEach( (namespace,synoRestClient) -> {
        	try {
                synoRestClient.logout();
            } catch (NasAuthenticationFailureException | NasCompatibilityException e) {
                log.error("nas远程文件工作站"+ namespace +"登出异常 " + e.getMessage(), e);
            }
        } );
        log.info("nas远程文件工作站登出成功,nasEngine销毁");
    }

    /**
     * 刷新token,注意这个方法会直接刷新掉redis中的sid
     * 注意:该方法较重,每次都会去检查sid的过期情况
     * @param force
     * @throws NasAuthenticationFailureException
     * @throws NasCompatibilityException
     */
    private void updateSession(boolean force) {
    	 Map<String, SynoRestClient> synoClientMap = serverContainer.getSynoClientMap();
    	 synoClientMap.forEach( (namespace,synoRestClient) -> {
    		 CommonTask.executor.execute( () -> {
        		 
        		 String reqId = GenerateCodeUtils.nextId();
            	 boolean locked = false;
            	 try {
                 	if (!synoRestClient.checkToken() || force) {//检查不通过刷新
                 		 if (redisLock.tryLock(String.format(OBS_AUTHENTICATION_LOCK_KEY, namespace) , reqId, 15000,true)) {
                 			locked = true;
                 			if(!synoRestClient.checkToken() || force) {
                 				 //update token
                     	       NasAuth nasAuth = synoRestClient.getNasAuth();
                     	       synoRestClient.authenticate(nasAuth.getUser(), nasAuth.getAuth());
                 			}
                 		 } else {
                 			log.info("nasEngine-获取锁超时,无法为namespace: {} 更新session!",namespace); 
                 		 }
                 	} else {
                 		log.info("nasEngine-sid有效,无需为namespace: {}刷新sid~!",namespace);
                 	}
                 } catch (Exception e) {
                     log.error(e.getMessage(), e);
                 } finally {
                     //无论失败了,还是成功了,将分布式锁释放掉,唤醒其他线程
                	 if (locked) {
                		 redisLock.releaseLock(String.format(OBS_AUTHENTICATION_LOCK_KEY, namespace), reqId, 15000); 
                	 }
                 }
        	 } );
    	 } );
    }
    
    @Override
    protected void loginAndAutoRefreshSession() {
    	updateSession(false);
		//定时任务刷新!!由于nas没有提供刷新接口,这里直接update
    	startTask();
    }

    private void startTask() {
    	CommonTask.executor1.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				boolean flag = false;
				String nextId = GenerateCodeUtils.nextId();
				try {
					flag = redisLock.tryLock(NAS_REFRESH_TOKEN_LOCK, nextId, 1000,true);
					if (flag) {
						updateSession(false);
					}  else {
						log.info("nasEngine-全量更新全部client持有session - 获取锁失败!!");
					}
				} catch (Exception e) {
						log.error(e.getMessage(),e);
					} finally {
						if (flag) {
							redisLock.releaseLock(NAS_REFRESH_TOKEN_LOCK, nextId, 1000);
						}
					}
				} 
			}, 1000L, 30*60*//任务30MIN一次
				1000L, TimeUnit.MILLISECONDS);
	}


	@Override
    public String save(FileContext context) {
        //session本身就有效或者更新session成功(无论是自己更新的,还是其他线程更新后唤醒当前线程),执行上传操作
        String nextId = getFileToken(context);
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        //当前日期的字符串
        String dateStr = sdf.format(context.getNow());
        //获得上传路径(PATH_PREFIX  + {系统编码} + "/" + dateStr)
        StringBuilder sb = new StringBuilder();
        determineDirectoryPath(sb, context.getEngineNamespace(), context.getSysCode(), dateStr);
        String filePath = sb.toString();
        NasResponse nasResponse;
        try {
            nasResponse = getClientByNamespaceOrPrefer(context.getEngineNamespace()).uploadFile(nextId, filePath, context.getContent());
        } catch (NasCompatibilityException e) {
            throw new ServiceException(CommonRespInfo.SYS_ERROR, e);
        }
        if (!nasResponse.isSuccess()) {
        	 log.info("上传文件出现错误返回码 - {},信息: {}",nasResponse.getError().get().getCode(),
        			 JSON.toJSONString(nasResponse.getError().get().getErrors()));
            Error error = nasResponse.getError().get();
            if (error.getCode() == SID_NOT_FOUND) {
                //当前失败是session失效引起的,刷新session
            	log.info("上传文件: {} 未成功,原因:{}",JSON.toJSONString(context),JSON.toJSONString(nasResponse));
            	updateSession(false);
                //再调用一次
            	return save(context);
            } else {
                //如果不是session失效引起的,直接抛异常
                log.info("远程访问失败! respCode:{} ", JSON.toJSONString(nasResponse));
                throw new ServiceException(CommonRespInfo.REMOTE_ACCESS_FAIL);
            }
        }
        return nextId;
    }

    @Override
    public int deleteFile(File file) {
    	ArrayList<File> arrayList = new ArrayList<File>();
    	arrayList.add(file);
        return deleteFiles(arrayList);
    }

    /**
     * 批量删除文件 nas引擎实现
     * 返回0代表删除失败  1代表删除成功
     * 当前实现类行为:成功返回1没有差异  如果失败了,返回异常给上层(未来可以拓展成失败做记录,然后再做错误恢复)
     *
     * @param files
     * @return
     */
    @Override
    public int deleteFiles(List<File> files) {
        Assert.notEmpty(files, "待删除nas文件不能为空!");
        List<String> namespaces = files.stream().map(File::getEngineNamespace).distinct().collect(Collectors.toList());
        if (namespaces.size() > 1){
            throw new IllegalArgumentException(StringUtils.format("当前批量删除nas文件,namespace不匹配,请检查files参数,files:{}", JSON.toJSONString(files)));
        }
        //1.由files的信息拿到所有要删除的文件的路径
        List<String> filePaths = getFilePathsFromFileList(files);
        NasResponse nasResponse;
        try {
            String namespace = namespaces.get(0);
            SynoRestClient client = getClientByNamespaceOrPrefer(namespace);
            //调用synoRestClient删除文件
            nasResponse = client.deleteFile(filePaths);
        } catch (NasCompatibilityException e) {
            throw new ServiceException(CommonRespInfo.REMOTE_ACCESS_FAIL.getCode(), StringUtils.format("当前调用与远程磁盘工作站支持的api版本不兼容,files:{}", files));
        }
        boolean success = nasResponse.isSuccess();
        if (!success) {
        	 log.info("删除文件出现错误返回码 - {},信息: {}",nasResponse.getError().get().getCode(),
        			 JSON.toJSONString(nasResponse.getError().get().getErrors()));
        	Error error = nasResponse.getError().get();
            if (error.getCode() == SID_NOT_FOUND) {
                //当前失败是session失效引起的,刷新session
            	updateSession(false);
                //再调用一次
            	return deleteFiles(files);
            } else {
            	throw new ServiceException(CommonRespInfo.REMOTE_ACCESS_FAIL.getCode(), StringUtils.format("远程删除NAS文件集合失败,files:{}", files));
            }
        }
        return files.size();
    }

    /**
     * 通过nas服务器生成共享link的方式去生成url地址,该方式可以减小obs服务器的内存及带宽压力(调用方拿到url去sharingLink下载文件)
     *
     * @param files
     * @return
     */
    private List<String> doGetDownloadUrlsForSharingLinks(List<File> files) {
        List<String> sharingUrls = new ArrayList<>();
        try {
            // Group files by their engine's namespace
            Map<String, List<File>> namespaceGroup = files.stream().collect(Collectors.groupingBy(File::getEngineNamespace));
            for (String namespace : namespaceGroup.keySet()) {
                sharingUrls.addAll(getClientByNamespaceOrPrefer(namespace)
                        .createSharingUrls(getFilePathsFromFileList(namespaceGroup.get(namespace))));
            }
        } catch (NasCompatibilityException e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(CommonRespInfo.REMOTE_ACCESS_FAIL.getCode(), "当前 Nas创建共享文件链接操作 调用与远程磁盘工作站支持的api版本不兼容");
        }
        return sharingUrls;
    }

    private List<String> getFilePathsFromFileList(List<File> files) {
        List<String> filePaths = new ArrayList<>(files.size());
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        for (File file : files) {
            String createTime = dateFormat.format(file.getCreateTime());
            determineDirectoryPath(sb, file.getEngineNamespace(), file.getSysCode(), createTime);
            sb.append("/").append(file.getToken());
            filePaths.add(sb.toString());
            sb.delete(0, sb.length());
        }
        return filePaths;
    }

    private void determineDirectoryPath(final StringBuilder sb,final String namespace ,final String sysCode, final String createTime) {
        Assert.notNull(sb, "StringBuilder对象不能为null!");
        sb.delete(0, sb.length());
        List<NasConfig.NasAuth> authList = conf.getAuthList();
        NasAuth nasAuth = authList.stream().filter(auth -> auth.getNamespace().equals(namespace)).findFirst().orElseThrow(() -> new IllegalArgumentException("未找到对应namespace的nas配置信息!"));
        String nasUsername = nasAuth.getUser();
        sb.append(PATH_PREFIX).append(nasUsername).append("/").append(sysCode).append("/").append(createTime);
    }


    @Override
	public List<InputStream> doGetStream(List<com.pewee.bean.File> fs) throws IOException {
		List<InputStream> list = new ArrayList<>();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		for (com.pewee.bean.File file : fs) {
	        StringBuilder sb = new StringBuilder();
	        String dateStr = sdf.format(file.getCreateTime());
            determineDirectoryPath(sb, file.getEngineNamespace(), file.getSysCode(), dateStr);
            sb.append("/").append(file.getToken());
	        String url = sb.toString();
	        InputStream downFileStream;
	        try {
                SynoRestClient client = serverContainer.getClientByNamespace(file.getEngineNamespace());
                downFileStream = client.downFileStream0(url);
	        } catch (Exception e) {
	            log.error("Nasengine获取多文件流- 失败 - 需要关闭前面的流!!共需关闭:" + list.size() + "个流;" + e.getMessage(), e);
	            if (!list.isEmpty()) {
					for (InputStream inputStream : list) {
						try {
							IOUtils.closeQuietly(inputStream);
						} catch (Exception e1) {
							log.error("Nasengine获取多文件流- 失败 - 关闭前面的流时失败!!",e1);
						}
					}
				}
	            throw new ServiceException(CommonRespInfo.REMOTE_ACCESS_FAIL);
	        }
            list.add(downFileStream);
		}
		return list;
	}
    
    @Override
    public String defaultNamespace() {
        return serverContainer.getPrefer();
    }
    
    /**
     * 通过namespace获取相应的nas节点的客户端
     * @param namespace 命名空间
     * @return 命名空间对应的nas节点的客户端
     */
    private SynoRestClient getClientByNamespaceOrPrefer(String namespace){
        if (namespace == null){
            return serverContainer.getPreferClient();
        }
        return Optional.ofNullable(serverContainer.getClientByNamespace(namespace)).orElseGet(()->serverContainer.getPreferClient());
    }
}
