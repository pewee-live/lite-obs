package com.pewee.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
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
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpClient {
	
	public static CloseableHttpClient client;
    
    public static RequestConfig rc;
    
    static {
    	log.info("HttpClient-开始初始化资源");
    	SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder()
				      .loadTrustMaterial(null, (certificate, authType) -> true).build();
		} catch (KeyManagementException e) {
			log.error("HttpClient初始化失败!!",e);
		} catch (NoSuchAlgorithmException e) {
			log.error("HttpClient初始化失败!!",e);
		} catch (KeyStoreException e) {
			log.error("HttpClient初始化失败!!",e);
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
		log.info("HttpClient-已初始化资源");
    	
    }
    
	public static String postMultiPart(String url,MultipartEntityBuilder builder,Map<String,String> headers) throws IOException{
		HttpPost httpPost = new HttpPost(url);
		if(null != headers && headers.size() > 0) {
			headers.forEach( (k,v)->{
				httpPost.addHeader(k, v);
			} );
		}
		httpPost.setConfig(rc);
		httpPost.setEntity(builder.build());
		return postEntity(httpPost);
	}
	
	public static String postUrlEncodedForm(String url,Map<String,String> kvPairs,Map<String,String> headers) throws IOException{
		HttpPost httpPost = new HttpPost(url);
		if(null != headers && headers.size() > 0) {
			headers.forEach( (k,v)->{
				httpPost.addHeader(k, v);
			} );
		}
		httpPost.setConfig(rc);
		List<NameValuePair> nvps = new ArrayList<>();
		if(null != kvPairs && kvPairs.size() > 0) {
		     for (String key : kvPairs.keySet()) {
		         nvps.add(new BasicNameValuePair(key, String.valueOf(kvPairs.get(key))));
		     }
		}
		httpPost.setEntity(new UrlEncodedFormEntity(nvps,"utf-8"));
		return postEntity(httpPost);
	}
	
	public static String postJson(String url,String text,Map<String,String> headers)throws IOException{
		if(null == headers) {
			headers = new HashMap<>();
		}
		headers.put("Content-Type","application/json");
		return post(url,text,headers);
	}
	
	public static String post(String url,String text,Map<String,String> headers) throws IOException{
		HttpPost httpPost = new HttpPost(url);
		if(null != headers && headers.size() > 0) {
			headers.forEach( (k,v)->{
				httpPost.addHeader(k, v);
			} );
		}
		httpPost.setConfig(rc);
		httpPost.setEntity(new StringEntity(text,"utf-8"));
		return postEntity(httpPost);
	}
	
	public static String postEntity(HttpPost httpPost)throws IOException{
		HttpEntity entity = null;
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
	
	public static String get(String url,Map<String,String> headers)throws IOException{
		if(null == headers) {
			headers = new HashMap<>();
		}
		HttpGet get = new HttpGet(url);
		if(null != headers && headers.size() > 0) {
			headers.forEach( (k,v)->{
				get.addHeader(k, v);
			} );
		}
		get.setConfig(rc);
		return getEntity(get);
	}
	
	public static byte[] getByteArr(String url,Map<String,String> headers)throws IOException{
		log.info("将开始请求url:{},headers:{}",url,JSON.toJSONString(headers));
		if(null == headers) {
			headers = new HashMap<>();
		}
		HttpGet get = new HttpGet(url);
		if(null != headers && headers.size() > 0) {
			headers.forEach( (k,v)->{
				get.addHeader(k, v);
			} );
		}
		get.setConfig(rc);
		return getByteArrEntity(get);
	}
	
	public static InputStream getIs(String url,Map<String,String> headers)throws IOException{
		log.info("将开始请求url:{},headers:{}",url,JSON.toJSONString(headers));
		if(null == headers) {
			headers = new HashMap<>();
		}
		HttpGet get = new HttpGet(url);
		if(null != headers && headers.size() > 0) {
			headers.forEach( (k,v)->{
				get.addHeader(k, v);
			} );
		}
		RequestConfig localrc = RequestConfig.custom().setSocketTimeout(1000000).setConnectTimeout(1000000).setConnectionRequestTimeout(1000000).build();
		get.setConfig(localrc);
		get.addHeader("Connection", "Keep-Alive"); 
		get.addHeader("Keep-Alive", "timeout=1000, max=1000"); // 300秒
		get.addHeader("Referer", url); 
		get.addHeader("Pragma", "no-cache"); 
		get.addHeader("Upgrade-Insecure-Requests", "1"); 
		return getIsEntity(get);
	}
	
	public static String getEntity(HttpGet get) throws IOException {
		HttpEntity entity = null;
		try {
			CloseableHttpResponse response = client.execute(get);
			entity = response.getEntity();
			String string = EntityUtils.toString(entity, "utf-8");
			log.info("返回body：{}",string);
			return string;
		} catch (ClientProtocolException e) {
			log.error(e.getMessage(),e);
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		} finally {
			get.abort();
			if(null !=entity ){
				EntityUtils.consume(entity);
			}
		}
		return null;
	}
	
	public static byte[] getByteArrEntity(HttpGet get) throws IOException {
		HttpEntity entity = null;
		try {
			CloseableHttpResponse response = client.execute(get);
			entity = response.getEntity();
			byte[] array = EntityUtils.toByteArray(entity);
			return array;
		} catch (ClientProtocolException e) {
			log.error(e.getMessage(),e);
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		} finally {
			get.abort();
			if(null !=entity ){
				EntityUtils.consume(entity);
			}
		}
		return null;
	}
	
	public static InputStream getIsEntity(HttpGet get) throws IOException {
		HttpEntity entity = null;
		try {
			CloseableHttpResponse response = client.execute(get);
			entity = response.getEntity();
			return entity.getContent();
		} catch (ClientProtocolException e) {
			log.error(e.getMessage(),e);
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		}
		return null;
	}

	public static Map<String, String> fastJsonBean2Map(Object bean) {
		return JSON.parseObject(JSON.toJSONString(bean), 
				new TypeReference<Map<String, String>>() {
		});
	}
	
	/** 
	 * 
	 * @param data1 
	 * @param data2 
	 * @return data1 与 data2拼接的结果 
	 */ 
	public static byte[] addBytes(byte[] data1, byte[] data2) { 
		 byte[] data3 = new byte[data1.length + data2.length]; 
		 System.arraycopy(data1, 0, data3, 0, data1.length); 
		 System.arraycopy(data2, 0, data3, data1.length, data2.length); 
		 return data3; 
	}
}
