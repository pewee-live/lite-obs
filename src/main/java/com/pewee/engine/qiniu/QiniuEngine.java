package com.pewee.engine.qiniu;

import com.google.gson.Gson;
import com.pewee.bean.File;
import com.pewee.engine.FileContext;
import com.pewee.engine.EngineInfoEnum;
import com.pewee.engine.meta.EngineDefinition;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.ServiceException;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class QiniuEngine extends EngineDefinition{
	
	String manageurl = "https://uc.qbox.me";
	
	String sourceUpurl = "https://up-z2.qiniup.com";
	
	String createBucket = "/mkbucketv3/pewee/region/z2";
	
	
	
	private Auth auth; 
	
	private CloseableHttpClient client;
	
	private RequestConfig rc;
	
	@Autowired
	private QiniuConfig conf;
	
	@Override
	public EngineInfoEnum getDefinition() {
		return EngineInfoEnum.SEVENNIU;
	}
	
	@Override
	public void load() {
		/**
		 * 初始化sdk
		 */
		auth = Auth.create(conf.getAk(), conf.getSk());
		StringMap authorizationV2 = auth.authorizationV2(manageurl + createBucket, "POST", new byte[0], "application/x-www-form-urlencoded");
		Map<String, Object> map = authorizationV2.map();
		HashMap<String,String> hashMap = new HashMap<String,String>();
		hashMap.put("Authorization", (String) map.get("Authorization"));
		hashMap.put("Content-Type", "application/x-www-form-urlencoded");
		
		/**
		 * 初始化http
		 */
		SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder()
				      .loadTrustMaterial(null, (certificate, authType) -> true).build();
		} catch (KeyManagementException e) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR,e);
		} catch (NoSuchAlgorithmException e) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR,e);
		} catch (KeyStoreException e) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR,e);
		}
		SSLConnectionSocketFactory ssf = new SSLConnectionSocketFactory(sslContext,NoopHostnameVerifier.INSTANCE);
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("https", ssf).register("http", new PlainConnectionSocketFactory()).build();
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		cm.setMaxTotal(500);//客户端总并行链接最大数
		cm.setDefaultMaxPerRoute(500);//每个主机的最大并行链接数
		
		rc = RequestConfig.custom().setSocketTimeout(10000).setConnectTimeout(10000).setConnectionRequestTimeout(10000).build();
		client = HttpClients.custom()
			      .setSSLContext(sslContext).setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
			      .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)
			      .setConnectionManager(cm)
			      .build();
		
		/**
		 * createbucket
		 */
		//try {
		//	post(manageurl + createBucket, "", hashMap);
		//} catch (IOException e) {
		//	e.printStackTrace();
		//}
		
		/**
		 * testUpload
		 */
		/*
		 * HttpPost httppost = new HttpPost(sourceUpurl); String uploadToken =
		 * auth.uploadToken("pewee"); MultipartEntityBuilder multipartEntityBuilder =
		 * MultipartEntityBuilder.create();
		 * multipartEntityBuilder.addTextBody("upload_token",uploadToken);
		 * multipartEntityBuilder.addTextBody("fileName","Myfile" +
		 * System.currentTimeMillis());
		 * multipartEntityBuilder.addTextBody("fileName","1234");
		 * multipartEntityBuilder.addBinaryBody("fileBinaryData", new
		 * java.io.File("C:\\Users\\pewee\\Desktop\\gen.zip")); HttpEntity reqEntity =
		 * multipartEntityBuilder.build(); httppost.setEntity(reqEntity);
		 * httppost.addHeader("Host",sourceUpurl); try { postEntity(httppost); } catch
		 * (IOException e) { // TODO Auto-generated catch block e.printStackTrace(); }
		 */
		/**
		java.io.File file = new java.io.File("D:\\workspace\\upload-nas\\src\\main\\resources\\2.pdf");
		byte[] c = new byte[(int) file.length()];
		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			IOUtils.read(fileInputStream, c);
			//save(null, c);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}**/
		
	}
	
	public String post(String url,String text,Map<String,String> headers) throws IOException{
		HttpPost httpPost = new HttpPost(url);
		if(null != headers && headers.size() > 0) {
			headers.forEach( (k,v)->{
				httpPost.addHeader(k, v);
			} );
		}
		httpPost.setEntity(new StringEntity(text,"utf-8"));
		return postEntity(httpPost);
	}
	
	public String postEntity(HttpPost httpPost)throws IOException{
		HttpEntity entity = null;
		httpPost.setConfig(rc);
		try {
			CloseableHttpResponse response = client.execute(httpPost);
			entity = response.getEntity();
			String string = EntityUtils.toString(entity, "utf-8");
			log.info("返回body：{}",string);
			return string;
		} catch (ClientProtocolException e) {
			log.error(e.getMessage(),e);
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		} finally {
			httpPost.abort();
			if(null !=entity ){
				EntityUtils.consume(entity);
			}
		}
		return null;
	}


	@Override
	protected void loginAndAutoRefreshSession() {
		
	}

	@Override
	public String save(FileContext context) {
		//构造一个带指定 Region 对象的配置类
		Region region = new Region.Builder()
                .region("z2")
                .accUpHost("upload-z2.qiniup.com")
                .srcUpHost("up-z2.qiniup.com")
                .iovipHost("iovip-z2.qbox.me")
                .rsHost("rs-z2.qiniu.com")
                .rsfHost("rsf-z2.qiniu.com")
                .apiHost("api.qiniu.com")
                .build();
		Configuration cfg = new Configuration(region);
		cfg.useHttpsDomains = true;
		//...其他参数参考类注释
		UploadManager uploadManager = new UploadManager(cfg);
		//...生成上传凭证，然后准备上传
		String accessKey = conf.getAk();
		String secretKey = conf.getSk();
		String bucket = "pewee";
		//默认不指定key的情况下，以文件内容的hash值作为文件名
		String key = null;
		Auth auth = Auth.create(accessKey, secretKey);
		StringMap policy = new StringMap();
        String upToken = auth.uploadToken(bucket, null, 3600, policy);
		try {
			com.qiniu.http.Response response = uploadManager.put(context.getContent(), "gen" + System.currentTimeMillis(), upToken);
		    //解析上传成功的结果
		    DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
		    System.out.println(putRet.key);
		    System.out.println(putRet.hash);
		} catch (QiniuException ex) {
			com.qiniu.http.Response r = ex.response;
		}
		return null;
	}


	@Override
	public int deleteFile(File file) {
		return 0;
	}

	@Override
	public int deleteFiles(List<File> files) {
		return 0;
	}

	@Override
	public List<InputStream> doGetStream(List<File> fs) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}



}
