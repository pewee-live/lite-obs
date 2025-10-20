package com.pewee.obs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.servlet.http.Cookie;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.ServiceException;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class TestDownLoadNas {
	
    private static CloseableHttpClient client;
	
	private static RequestConfig rc;
	
	private static CookieStore cookieStore = new BasicCookieStore();
	
	static {
		
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
			      .setDefaultCookieStore(cookieStore)
			      .build();
		
	}
	
	public static void main(String[] args) throws IOException {
		String post = get("http://192.168.7.111:5000/fsdownload/qT9b80xIu/4.pdf", null);
		File file = new File("C:\\develop\\workspace\\ws3\\obs\\src\\test\\java\\" + System.currentTimeMillis());
		IOUtils.write(post.getBytes(), new FileOutputStream(file));
	}
	
	public static String get(String url,Map<String,String> headers) throws IOException{
		HttpGet httpget = new HttpGet(url);
		BasicClientCookie clientCookie = new BasicClientCookie("sharing_sid", "2UBP2YW_YodjW3Wo1RiLOmuK3DnQ7zUr");
		clientCookie.setDomain("192.168.7.111");   //设置范围
		clientCookie.setPath("/");
		cookieStore.addCookie(clientCookie);
		if(null != headers && headers.size() > 0) {
			headers.forEach( (k,v)->{
				httpget.addHeader(k, v);
			} );
		}
		return getEntity(httpget);
	}
	
	public static String post(String url,String text,Map<String,String> headers) throws IOException{
		HttpPost httpPost = new HttpPost(url);
		BasicClientCookie clientCookie = new BasicClientCookie("sharing_sid", "vh5HOAAhq1UxQzMay_BSjHYyf4ljN5pC");
		clientCookie.setDomain("192.168.7.111");   //设置范围
		clientCookie.setPath("/");
		cookieStore.addCookie(clientCookie);
		if(null != headers && headers.size() > 0) {
			headers.forEach( (k,v)->{
				httpPost.addHeader(k, v);
			} );
		}
		httpPost.setEntity(new StringEntity(text,"utf-8"));
		return postEntity(httpPost);
	}
	
	public static String postEntity(HttpPost httpPost)throws IOException{
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
	
	public static String getEntity(HttpGet httpegt)throws IOException{
		HttpEntity entity = null;
		httpegt.setConfig(rc);
		try {
			CloseableHttpResponse response = client.execute(httpegt);
			entity = response.getEntity();
			String string = EntityUtils.toString(entity, "utf-8");
			log.info("返回body：{}",string);
			return string;
		} catch (ClientProtocolException e) {
			log.error(e.getMessage(),e);
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		} finally {
			httpegt.abort();
			if(null !=entity ){
				EntityUtils.consume(entity);
			}
		}
		return null;
	}
}
