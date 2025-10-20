package com.pewee.bean.nas.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pewee.bean.nas.UserSession;
import com.pewee.bean.nas.UserSessionRedis;
import com.pewee.bean.nas.api.genericresponses.Data;
import com.pewee.bean.nas.api.genericresponses.Error;
import com.pewee.bean.nas.api.genericresponses.NasResponse;
import com.pewee.bean.nas.api.helper.QueryURLBuilder;
import com.pewee.bean.nas.api.helper.SynologyAPINames;
import com.pewee.bean.nas.api.helper.SynologyAPIVersions;
import com.pewee.bean.nas.api.model.auth.responses.LoginResponse;
import com.pewee.bean.nas.api.model.list.APIDescription;
import com.pewee.bean.nas.api.model.list.responses.APIDescriptionResponse;
import com.pewee.bean.nas.api.model.list.responses.FoldersResponse;
import com.pewee.bean.nas.api.model.list.responses.ListSharesResponse;
import com.pewee.bean.nas.api.model.sharing.Link;
import com.pewee.bean.nas.api.model.sharing.responses.SharingResponse;
import com.pewee.bean.nas.api.model.upload.UploadQuery;
import com.pewee.bean.nas.client.exception.NasAuthenticationFailureException;
import com.pewee.bean.nas.client.exception.NasCompatibilityException;
import com.pewee.bean.nas.client.exception.NasResponseException;
import com.pewee.engine.synologyNas.NasConfig.NasAuth;
import com.pewee.interceptor.SynoUploadHttpRequestInterceptor;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.ServiceException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisCluster;

import org.joda.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.pewee.util.HttpClient.getIs;

/**
 * 这个类是和具体某个NAS服务器绑定的,若我们配置了多个NAS,则这个类应该在启动时被实例化多次
 * @author pewee
 * 2024年5月24日
 */
@Getter
@Setter
@Slf4j
public class SynoRestClient implements SynoRestClientIface {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    
    private static final String SHARE_URL_SUFFIX = "fsdownload";

    private String urlSharingPrefix;
    private RestTemplate rest;
    private QueryURLBuilder queryURLBuilder;
    private Map<SynologyAPIVersions, APIDescription> apiVersionList = new HashMap<>();
    private RestTemplate byteArrayRestTemplate;
    @Deprecated
    private RestTemplate streamTemplate;
    private NasAuth nasAuth;
    private UserSession userSession;
    private JedisCluster jedisCluster;
    

    public SynoRestClient(RestTemplate nasClientRestTemplate,NasAuth nasAuth,JedisCluster jedisCluster) {
        this.rest = nasClientRestTemplate;
        this.nasAuth = nasAuth;
        this.urlSharingPrefix = nasAuth.getUrl()+ SHARE_URL_SUFFIX;
        this.jedisCluster = jedisCluster;
        this.userSession = new  UserSessionRedis(jedisCluster,nasAuth.getNamespace());
        this.queryURLBuilder =  new QueryURLBuilder(nasAuth.getUrl(),userSession);
        init();
    }

    @Override
    public void init()  {
        String query = this.queryURLBuilder.buildInitQuery();
        NasResponse nasResponse = this.rest.getForObject(query, NasResponse.class);
        APIDescriptionResponse descriptionResponse = (APIDescriptionResponse) nasResponse.getData().get();
        this.queryURLBuilder.setApiDescription(descriptionResponse);
        this.apiVersionList.put(SynologyAPIVersions.SYNO_INFO_API, descriptionResponse.getInfo().orElse(null));
        this.apiVersionList.put(SynologyAPIVersions.SYNO_AUTH_API, descriptionResponse.getAuth().orElse(null));
        this.apiVersionList.put(SynologyAPIVersions.SYNO_CREATE_FOLDER_API, descriptionResponse.getCreateFolder().orElse(null));
        this.apiVersionList.put(SynologyAPIVersions.SYNO_UPLOAD_API, descriptionResponse.getUpload().orElse(null));
        this.apiVersionList.put(SynologyAPIVersions.SYNO_LIST_API, descriptionResponse.getList().orElse(null));
        this.apiVersionList.put(SynologyAPIVersions.SYNO_DELETE_API, descriptionResponse.getDelete().orElse(null));
        this.apiVersionList.put(SynologyAPIVersions.SYNO_SHARING_API, descriptionResponse.getSharing().orElse(null));
        this.apiVersionList.put(SynologyAPIVersions.SYNO_DOWNLOAD_API, descriptionResponse.getDownload().orElse(null));

        //to-do 当前restTemplate的设置不熟悉,造成无法使用默认restTemplate请求二进制数据(会走json解析报错),选取当下方式先解决当前问题,之后修改配置
        RestTemplate bytrArrayRestTemplate = new RestTemplate();
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        ByteArrayHttpMessageConverter converter = new ByteArrayHttpMessageConverter();
        converter.setSupportedMediaTypes(Arrays.asList(MediaType.ALL, MediaType.APPLICATION_OCTET_STREAM));
        messageConverters.add(converter);
        bytrArrayRestTemplate.setMessageConverters(messageConverters);
        this.byteArrayRestTemplate = bytrArrayRestTemplate;
        //流获取
        RestTemplate streamTemplate = new RestTemplate();
        ResourceHttpMessageConverter c = new ResourceHttpMessageConverter();
        c.setSupportedMediaTypes(Arrays.asList( MediaType.APPLICATION_OCTET_STREAM));
        List<HttpMessageConverter<?>> cs = new ArrayList<>();
        cs.add(c);
        streamTemplate.setMessageConverters(cs);
        this.streamTemplate = streamTemplate;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public void checkAPICompatibility(SynologyAPIVersions apiName) throws NasCompatibilityException {
        // check if the api is available on remote DiskStation
        if (this.apiVersionList.get(apiName) == null) {
            throw new NasCompatibilityException("当前api: " + apiName + " ,在远程磁盘工作站中不存在");
        }
        // check that version of this client is in the range of compatible versions
        if (apiName.getVersion() < this.apiVersionList.get(apiName).getMinVersion() || apiName.getVersion() > this.apiVersionList.get(apiName).getMaxVersion()) {
            throw new NasCompatibilityException("当前版本在远程磁盘工作站中不兼容");
        }
    }

    @Override
    public void authenticate(String login, String password) throws NasAuthenticationFailureException, NasCompatibilityException {
        Assert.notNull(login, "账号不能为空");
        Assert.notNull(password, "密码不能为空");

        checkAPICompatibility(SynologyAPIVersions.SYNO_AUTH_API);
        String query = this.queryURLBuilder.buildAuthenticationQuery(login, password);
        NasResponse nasResponse = this.rest.getForObject(query, NasResponse.class);
        if (nasResponse.isSuccess()) {
            if (nasResponse.getData().isPresent()) {
                LoginResponse loginResponse = (LoginResponse) nasResponse.getData().get();
                synchronized (UserSession.class) {

                }
                userSession.setSid(loginResponse.getSid());
                userSession.setTtl(24* 60 * 60);//设置过期时间为1天
            }
        } else {
            if (nasResponse.getError().isPresent()) {
                Error error = nasResponse.getError().get();
                throw new NasAuthenticationFailureException(error.toString());
            }
            throw new NasAuthenticationFailureException("在远程磁盘工作站中,进行登陆鉴权时发生错误");
        }
    }

    @Override
    public Data listShares() throws NasResponseException, NasCompatibilityException {
        checkAPICompatibility(SynologyAPIVersions.SYNO_LIST_API);
        String query = this.queryURLBuilder.buildListShareQuery();
        NasResponse nasResponse = rest.getForObject(query, NasResponse.class);
        if (nasResponse.isSuccess()) {
            Data data = nasResponse.getData().get();
            if (data instanceof ListSharesResponse) {
                return (ListSharesResponse) data;
            } else {
                throw new NasResponseException("数据类型不符合预期,请检查API版本的兼容性");
            }
        } else {
            throw new NasResponseException();
        }
    }

    @Override
    public Data getFolder(String path) throws NasResponseException, NasCompatibilityException {
        checkAPICompatibility(SynologyAPIVersions.SYNO_LIST_API);
        String query = queryURLBuilder.buildGetFolderQuery(path);
        NasResponse nasResponse = rest.getForObject(query, NasResponse.class);
        if (nasResponse.isSuccess()) {
            Data data = nasResponse.getData().get();
            if (data instanceof FoldersResponse) {
                return (FoldersResponse) data;
            } else {
                throw new NasResponseException("数据类型不符合预期,请检查API版本的兼容性");
            }
        } else {
            if (nasResponse.getError().isPresent()) {

            }
        }
        throw new NasResponseException();
    }

    @Override
    public void logout() throws NasAuthenticationFailureException, NasCompatibilityException {
        checkAPICompatibility(SynologyAPIVersions.SYNO_AUTH_API);
        String query = this.queryURLBuilder.buildLogoutQuery();
        NasResponse nasResponse = this.rest.getForObject(query, NasResponse.class);
        if (nasResponse == null || !nasResponse.isSuccess()) {
            throw new NasAuthenticationFailureException("在远程磁盘工作站中,进行登出操作发生错误");
        }
    }

    @Override
    public boolean createFolder(String folderName, String parentPath) throws NasResponseException, NasCompatibilityException {
        Assert.notNull(folderName, "文件夹名称不能为空");
        Assert.notNull(parentPath, "父路径不能为空");
        checkAPICompatibility(SynologyAPIVersions.SYNO_CREATE_FOLDER_API);
        String query = this.queryURLBuilder.buildCreateFolderQuery(folderName, parentPath);
        NasResponse nasResponse = this.rest.getForObject(query, NasResponse.class);
        if (nasResponse.isSuccess()) {
            return true;
        } else {
            throw new NasResponseException();
        }
    }

    @Override
    public NasResponse uploadFile(String filename, String filePath, byte[] arr) throws NasCompatibilityException {
        checkAPICompatibility(SynologyAPIVersions.SYNO_UPLOAD_API);
        String query = this.queryURLBuilder.buildUploadFileQuery();
        UploadQuery json = new UploadQuery("", 2, "", this.queryURLBuilder.getUserSession().getSid(), filePath, true, false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Arrays.asList(MediaType.ALL));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("api", SynologyAPINames.SYNO_UPLOAD_API.getBytes());
        body.add("version", String.valueOf(SynologyAPIVersions.SYNO_UPLOAD_API.getVersion()).getBytes());
        body.add("method", "upload".getBytes());
        body.add("path", json.getPath().getBytes());
        body.add("overwrite", "false".getBytes());
        body.add("create_parents", "true".getBytes());//如果当前上传的路径不存在,那么创建路径;改成false代表不允许创建
        body.add("_sid", this.queryURLBuilder.getUserSession().getSid().getBytes());
        body.add("filename", filename.getBytes());

        ContentDisposition content = null;
        MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();

        content = ContentDisposition.builder("form-data").name("file").filename(filename).build();

        fileMap.add(HttpHeaders.CONTENT_DISPOSITION, content.toString());
        HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(new ByteArrayResource(arr), fileMap);
        body.add("file", fileEntity);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        RestTemplate template = new RestTemplate();
        template.setInterceptors(Arrays.asList(new SynoUploadHttpRequestInterceptor()));
        ResponseEntity<String> result = template.exchange(query, HttpMethod.POST, requestEntity, String.class);
        NasResponse nasResponse = null;
        try {
            nasResponse = new ObjectMapper().readValue(result.getBody(), NasResponse.class);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
        return nasResponse;
    }

    @Override
    public NasResponse deleteFile(List<String> files) throws NasCompatibilityException {
        checkAPICompatibility(SynologyAPIVersions.SYNO_DELETE_API);
        URI query = this.queryURLBuilder.buildDeleteFilesQuery(files);
        return this.rest.getForObject(query, NasResponse.class);
    }

    /**
     * 传入要下载的文件路径(在Nas上的路径),获取这些文件的共享url(顺序一一对应)
     * 有效期为当前日期
     *
     * @return
     * @throws NasCompatibilityException
     */
    @Override
    public List<String> createSharingUrls(List<String> filePaths) throws NasCompatibilityException {
        Assert.notEmpty(filePaths, "待下载的文件路径不能为空");
        checkAPICompatibility(SynologyAPIVersions.SYNO_SHARING_API);
        log.info("createSharingUrls开始执行,filePaths: {}", filePaths);
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        //expireDate
        Date expireDate = Instant.now().toDateTime().plusDays(1).toDate();
        String query = this.queryURLBuilder.buildSharingUrlsQuery(filePaths, sdf.format(expireDate));
        NasResponse nasResponse = this.rest.getForObject(query, NasResponse.class);
        if (!nasResponse.isSuccess()) {
            log.error("createSharingUrls远程调用失败,filePaths: {},response:{}", filePaths, nasResponse);
            throw new ServiceException(CommonRespInfo.REMOTE_ACCESS_FAIL);
        }
        List<String> result = new ArrayList<>(filePaths.size());
        Optional<Data> data = nasResponse.getData();
        if (!data.isPresent()) {
            log.error("createSharingUrls调用结果为空,filePaths: {},response:{}", filePaths, nasResponse);
            throw new ServiceException(CommonRespInfo.REMOTE_ACCESS_FAIL);
        }
        SharingResponse sharingResponse = (SharingResponse) data.get();
        StringBuilder sb = new StringBuilder();
        for (Link link : sharingResponse.getLinks()) {
            sb.append(this.urlSharingPrefix).append("/").append(link.getId()).append("/").append(link.getName());
            result.add(sb.toString());
            sb.delete(0, sb.length());
        }
        return result;
    }

    @Override
    public byte[] downFile(String path) throws NasCompatibilityException {
        Assert.hasText(path, "待下载文件路径不能为空");
        checkAPICompatibility(SynologyAPIVersions.SYNO_DOWNLOAD_API);
        log.info("downFile开始执行,path: {}", path);
        URI url = this.queryURLBuilder.buildDownloadQuery(path);
        ResponseEntity<byte[]> byteData = this.byteArrayRestTemplate.getForEntity(url, byte[].class);
        return byteData.getBody();
    }
    
    /**
     * 由於restTemplate中无法获取DownloadStream(在其Converter中已经关掉了,convert的设计是在其中处理全部的下载流),此方法获取的是一个BytearrayInputStream,对内存影响很大
     * @param path
     * @return
     * @throws NasCompatibilityException
     * @throws IOException
     */
    @Deprecated
    public InputStream downFileStream(String path) throws NasCompatibilityException, IOException {
        Assert.hasText(path, "待下载文件路径不能为空");
        checkAPICompatibility(SynologyAPIVersions.SYNO_DOWNLOAD_API);
        log.info("downFile开始执行,path: {}", path);
        URI url = this.queryURLBuilder.buildDownloadQuery(path);
        ResponseEntity<org.springframework.core.io.Resource> entity  =  this.streamTemplate.getForEntity(url,org.springframework.core.io.Resource.class);
        return Objects.requireNonNull(entity.getBody()).getInputStream();
    }
    
    public InputStream downFileStream0(String path) throws NasCompatibilityException, IOException {
        Assert.hasText(path, "待下载文件路径不能为空");
        checkAPICompatibility(SynologyAPIVersions.SYNO_DOWNLOAD_API);
        log.info("downFile开始执行,path: {}", path);
        URI url = this.queryURLBuilder.buildDownloadQuery(path);
        return getIs(url.toString(), null);
    }
    
    /**
     * filestation信息查新
     */
    public boolean fileStationInfoQuery() {
    	String query = this.queryURLBuilder.buildFileStationInfoQuery();
    	String[] split = query.split("&_sid=");
    	NasResponse nasResponse = this.rest.getForObject(query,NasResponse.class);
    	log.info("sid:{} 有效状态为:{}!!",split[1],nasResponse.isSuccess());
    	return nasResponse.isSuccess();
    }
    
    /**
     * 检验token
     * 注意:此操作较重!!应当尽量少使用
     * @return
     */
    public boolean checkToken() {
    	if (userSession.getTtl() < 60 * 60) {
    		return false;
    	}
    	return fileStationInfoQuery();
    }   
	
}
