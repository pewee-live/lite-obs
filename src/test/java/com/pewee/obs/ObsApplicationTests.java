package com.pewee.obs;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.pewee.util.CommonTask;
import com.pewee.util.HttpClient;
import com.pewee.util.StringUtils;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.RespEntity;
import com.pewee.util.resp.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.PureJavaCrc32;
import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

@Slf4j
class ObsApplicationTests {

    static CloseableHttpClient client;

    static RequestConfig rc;

    static {
        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (certificate, authType) -> true).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        SSLConnectionSocketFactory ssf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().register("https", ssf).register("http", new PlainConnectionSocketFactory()).build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        cm.setMaxTotal(500);//客户端总并行链接最大数
        cm.setDefaultMaxPerRoute(500);//每个主机的最大并行链接数
        rc = RequestConfig.custom().setSocketTimeout(150000).setConnectTimeout(150000).setConnectionRequestTimeout(150000).build();
        client = HttpClients.custom()
                .setSSLContext(sslContext).setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                .setConnectionReuseStrategy(NoConnectionReuseStrategy.INSTANCE)
                .setConnectionManager(cm)
                .build();

    }

    public static String postMultiPart(String url, MultipartEntityBuilder builder, Map<String, String> headers) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        if (null != headers && headers.size() > 0) {
            headers.forEach((k, v) -> {
                httpPost.addHeader(k, v);
            });
        }
        httpPost.setConfig(rc);
        httpPost.setEntity(builder.build());
        return postEntity(httpPost);
    }

    public static String postUrlEncodedForm(String url, Map<String, String> kvPairs, Map<String, String> headers) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        if (null != headers && headers.size() > 0) {
            headers.forEach((k, v) -> {
                httpPost.addHeader(k, v);
            });
        }
        httpPost.setConfig(rc);
        List<NameValuePair> nvps = new ArrayList<>();
        if (null != kvPairs && kvPairs.size() > 0) {
            for (String key : kvPairs.keySet()) {
                nvps.add(new BasicNameValuePair(key, String.valueOf(kvPairs.get(key))));
            }
        }
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, "utf-8"));
        return postEntity(httpPost);
    }

    public static String postJson(String url, String text, Map<String, String> headers) throws IOException {
        if (null == headers) {
            headers = new HashMap<>();
        }
        headers.put("Content-Type", "application/json");
        return post(url, text, headers);
    }

    public static String post(String url, String text, Map<String, String> headers) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        if (null != headers && headers.size() > 0) {
            headers.forEach((k, v) -> {
                httpPost.addHeader(k, v);
            });
        }
        httpPost.setConfig(rc);
        httpPost.setEntity(new StringEntity(text, "utf-8"));
        return postEntity(httpPost);
    }

    public static String postEntity(HttpPost httpPost) throws IOException {
        HttpEntity entity = null;
        try {
            CloseableHttpResponse response = client.execute(httpPost);
            entity = response.getEntity();
            String string = EntityUtils.toString(entity, "utf-8");
            log.info("返回body：{}", string);
            return string;
        } catch (ClientProtocolException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            httpPost.abort();
            if (null != entity) {
                EntityUtils.consume(entity);
            }
        }
        return null;
    }

    static void testUpload() throws IOException {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("Authorization", "OBS eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzeXNDb2RlIjoic3lzdGVtX25vXzAwMDAzIiwic3lzQWxpYXMiOiJCU1AtVEVTVCJ9.r1UcFftsnSenz4LXufFyl0lYjecng_mQCvK1ghKGuxQ");
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        File file = new File("C:\\Users\\pewee\\Desktop\\obs-client-1.0.2.RELEASE.jar");
        Long length = file.length();

        FileInputStream inputStream = new FileInputStream(file);
        byte[] arr = new byte[length.intValue()];
        IOUtils.readFully(inputStream, arr);
        //multipartEntityBuilder.addBinaryBody("file", arr);
        multipartEntityBuilder.addPart("file", new ByteArrayBody(arr, "file"));
        String part = postMultiPart("http://localhost:12000/pewee/logicfile/upload", multipartEntityBuilder, map);
        System.out.println(part);
    }

    static Long SIZE = 16 * 1024 * 1024L;

    static void testPreUpload(String filePath) throws IOException {
        File file = new File(filePath);
        long length = file.length();
        long mod = length % SIZE;
        long parts = length / SIZE;
        if (mod != 0L) {
            parts = parts + 1;
        }
        FileInputStream inputStream0 = new FileInputStream(file);
        byte[] readFully = IOUtils.readFully(inputStream0, (int) length);
        PureJavaCrc32 pureJavaCrc32 = new PureJavaCrc32();
        pureJavaCrc32.update(readFully, 0, readFully.length);
        String crc = Long.toHexString(pureJavaCrc32.getValue());
        System.out.println("FullCrc:" + crc);
        ArrayList<String> crcList = new ArrayList<String>();
        ArrayList<String> partsList = new ArrayList<String>();
        FileInputStream inputStream = new FileInputStream(file);
        for (long i = 0; i < parts; i++) {
            byte[] arr;
            if (i == parts - 1) {
                arr = new byte[(int) mod];
            } else {
                arr = new byte[SIZE.intValue()];
            }
            IOUtils.read(inputStream, arr);
            PureJavaCrc32 pureJavaCrc320 = new PureJavaCrc32();
            pureJavaCrc320.update(arr, 0, arr.length);
            String crc0 = Long.toHexString(pureJavaCrc320.getValue());
            crcList.add(crc0);
            String filePath0 = filePath + ".part" + i;
            File tmp = new File(filePath0);
            FileOutputStream outputStream = new FileOutputStream(tmp);
            IOUtils.write(arr, outputStream);
            partsList.add(filePath0);
        }

        HashMap<String, String> hashMap = new HashMap<String, String>();
        StringBuilder stringBuilder = new StringBuilder("");
        stringBuilder.append("http://127.0.0.1:12000/pewee/logicfile/preupload?");
        stringBuilder.append("sysAlias=BSP-TEST");
        stringBuilder.append("&sysCode=system_no_00003");
        stringBuilder.append("&filename=" + file.getName());
        stringBuilder.append("&mimeType=" + StringUtils.guessContentType(file.getName()));
        stringBuilder.append("&length=" + length);
        stringBuilder.append("&storageType=" + 3);
        stringBuilder.append("&crc32=" + crc);
        for (String string : crcList) {
            stringBuilder.append("&subCrcList=" + string);
        }
        hashMap.put("split", "" + 1);
        hashMap.put("total", "" + parts);
        hashMap.put("Authorization", token);
        String post = post(stringBuilder.toString(), "", hashMap);

        System.out.println("code:" + post);
        System.out.println("CRCLIST:" + JSON.toJSONString(crcList));
        System.out.println("pathLIST:" + JSON.toJSONString(partsList));

        RespEntity respEntity = JSON.parseObject(post, RespEntity.class);
        code = (String) respEntity.getData();
        crcList0.addAll(crcList);
        pathList0.addAll(partsList);
    }

    public static String token = "OBS eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzeXNDb2RlIjoic3lzdGVtX25vXzAwMDAzIiwic3lzQWxpYXMiOiJCU1AtVEVTVCIsInRpbWVzdGFtcCI6IjIwMjUtMDgtMTggMTE6MzY6MzYifQ.6hZzHOsIHcNKUl0jdQJiv89tw3mU7ix8EdvOnwjWakg";
    public static String code = null;
    public static List<String> crcList0 = new ArrayList<>();
    public static List<String> pathList0 = new ArrayList<>();

    static void testUploadParts(String code, int sequence, String crc32, String filePath, CountDownLatch downLatch) throws IOException {
        File file = new File(filePath);
        Long length = file.length();

        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        FileInputStream inputStream = new FileInputStream(file);
        byte[] arr = new byte[length.intValue()];
        IOUtils.readFully(inputStream, arr);
        multipartEntityBuilder.addPart("file", new ByteArrayBody(arr, "file"));

        HashMap<String, String> hashMap = new HashMap<String, String>();
        StringBuilder stringBuilder = new StringBuilder("");
        stringBuilder.append("http://127.0.0.1:12000/pewee/logicfile/upload?");
        stringBuilder.append("&crc32=" + crc32);
        hashMap.put("code", code);
        hashMap.put("sequence", "" + sequence);
        hashMap.put("Authorization", token);
        String post = postMultiPart(stringBuilder.toString(), multipartEntityBuilder, hashMap);
        System.out.println(post);
        downLatch.countDown();
    }
    
    @Test
    public void cutAndUpload() throws IOException, InterruptedException {
        testPreUpload("C:\\develop\\Anaconda3-2024.10-1-Windows-x86_64.exe");
        CountDownLatch downLatch = new CountDownLatch(crcList0.size());
        for (int i = 0; i < crcList0.size(); i++) {
            final int j = i;
            CommonTask.executor.submit(() -> {
                try {
                    testUploadParts(code, j, crcList0.get(j), pathList0.get(j), downLatch);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        }
        downLatch.await();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        
    }


    @Test
    public void should_generate_uuid(){
        System.out.println(UUID.randomUUID());
    }



}
