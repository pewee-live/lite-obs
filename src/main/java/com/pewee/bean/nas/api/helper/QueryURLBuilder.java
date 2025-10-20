package com.pewee.bean.nas.api.helper;

import com.pewee.bean.nas.UserSession;
import com.pewee.bean.nas.api.model.list.responses.APIDescriptionResponse;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.ServiceException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;

/**
 * 群晖源码采用双引号方式来处理文件名当文件中有特殊字符会导致群晖报119错误
 * 未来需要暴露出接口时,需要将对应的buildQuery相关处理逻辑修改.
 * 目前我们暴露和操作文件名相关的接口只有buildDownloadQuery,buildDeleteFilesQuery这2个
 * @author pewee
 *
 */
@Getter
@Setter
@Slf4j
public class QueryURLBuilder {

    private String baseURL;
    private APIDescriptionResponse apiDescription;
    private UserSession userSession;
    
    public QueryURLBuilder (String baseURL,UserSession userSession ) {
    	this.baseURL = baseURL;
    	this.userSession = userSession;
    }

    public String buildInitQuery() {
        StringBuilder builder = new StringBuilder(baseURL);
        builder.append(SynologyAPINames.BASE_API_URI).append("query.cgi?")
                .append("api=").append(SynologyAPINames.SYNO_INFO_API)
                .append("&version=").append(SynologyAPIVersions.SYNO_INFO_API.getVersion())
                .append("&method=query")
                .append("&query=all");
        return builder.toString();
    }

    public String buildAuthenticationQuery(String login, String password) {
        StringBuilder builder = new StringBuilder(baseURL);
        builder.append(SynologyAPINames.BASE_API_URI).append(apiDescription.getAuth().get().getPath()).append("?")
                .append("api=").append(SynologyAPINames.SYNO_AUTH_API)
                .append("&version=").append(SynologyAPIVersions.SYNO_AUTH_API.getVersion())
                .append("&method=login")
                .append("&account=").append(login)
                .append("&passwd=").append(password)
                .append("&session=").append(SynologyAPINames.SYNO_SESSION)
                .append("&format=sid");
        return builder.toString();
    }

    public String buildLogoutQuery() {
        StringBuilder builder = new StringBuilder(baseURL);
        builder.append(SynologyAPINames.BASE_API_URI)
                .append(apiDescription.getAuth().get().getPath()).append("?")
                .append("api=").append(SynologyAPINames.SYNO_AUTH_API)
                .append("&version=").append(SynologyAPIVersions.SYNO_AUTH_API.getVersion())
                .append("&method=logout")
                .append("&session=").append(SynologyAPINames.SYNO_SESSION);
        return builder.toString();
    }

    public String buildCreateFolderQuery(String folderName, String parentPath) {
        StringBuilder builder = new StringBuilder(baseURL);
        builder.append(SynologyAPINames.BASE_API_URI).append(apiDescription.getCreateFolder().get().getPath()).append("?")
                .append("api=").append(SynologyAPINames.SYNO_CREATE_FOLDER_API)
                .append("&version=").append(SynologyAPIVersions.SYNO_CREATE_FOLDER_API.getVersion())
                .append("&method=create")
                .append("&folder_path=").append("\"").append(parentPath).append("\"")
                .append("&name=").append("\"").append(folderName).append("\"")
                .append("&_sid=").append(userSession.getSid());
        return builder.toString();
    }

    public String buildListShareQuery() {
        StringBuilder builder = new StringBuilder(baseURL);
        builder.append(SynologyAPINames.BASE_API_URI).append(apiDescription.getList().get().getPath()).append("?")
                .append("api=").append(SynologyAPINames.SYNO_LIST_API)
                .append("&version=").append(SynologyAPIVersions.SYNO_LIST_API.getVersion())
                .append("&method=list_share")
                .append("&_sid=").append(userSession.getSid());
        return builder.toString();
    }

    public String buildGetFolderQuery(String folderpath) {
        StringBuilder builder = new StringBuilder(baseURL);
        builder.append(SynologyAPINames.BASE_API_URI).append(apiDescription.getList().get().getPath()).append("?")
                .append("api=").append(SynologyAPINames.SYNO_LIST_API)
                .append("&version=").append(SynologyAPIVersions.SYNO_LIST_API.getVersion())
                .append("&method=list")
                .append("&filetype=all").append("&folder_path=").append("\"").append(folderpath).append("\"")
                .append("&_sid=").append(userSession.getSid());
        return builder.toString();
    }

    public String buildUploadFileQuery() {
        StringBuilder builder = new StringBuilder(baseURL);
        builder.append(SynologyAPINames.BASE_API_URI).append(apiDescription.getUpload().get().getPath()).append("?")
                .append("api=").append(SynologyAPINames.SYNO_UPLOAD_API)
                .append("&version=").append(SynologyAPIVersions.SYNO_UPLOAD_API.getVersion())
                .append("&method=upload")
                .append("&_sid=").append(userSession.getSid());
        return builder.toString();
    }
    
    /**
     * 这里群晖源码采用双引号方式来处理文件名当文件中有特殊字符会导致群晖报119错误
     * 具体信息参考buildDownloadQuery 
     * 在最新改造中我们判断群晖返回-119会去刷新_sid再递归执行逻辑会导致死循环
     * 股这里处理逻辑与下载一致
     * 
     * @param files
     * @return
     */
    public URI buildDeleteFilesQuery(List<String> files){
        //拼接要操作的路径,eg.  ["obs/test/1.pdf","obs/test/2.pdf"]
        /**
    	StringBuilder sb = new StringBuilder("[");
        for (String file : files) {
            sb.append("\"").append(file).append("\"").append(",");
        }
        int deleteIndex = sb.lastIndexOf(",");
        sb.deleteCharAt(deleteIndex);
        sb.append("]");
        StringBuilder builder = new StringBuilder(baseURL);
        builder.append(SynologyAPINames.BASE_API_URI).append(apiDescription.getDelete().get().getPath()).append("?")
                .append("api=").append(SynologyAPINames.SYNO_DELETE_API)
                .append("&version=").append(SynologyAPIVersions.SYNO_DELETE_API.getVersion())
                .append("&method=delete")
                .append("&path=").append(sb.toString())
                .append("&_sid=").append(userSession.getSid());
        return builder.toString();
        **/
    	StringBuilder sb = new StringBuilder("[");
        for (String file : files) {
            sb.append("\"").append(file).append("\"").append(",");
        }
        int deleteIndex = sb.lastIndexOf(",");
        sb.deleteCharAt(deleteIndex);
        sb.append("]");
        StringBuilder builder = new StringBuilder(baseURL);
        try {
			builder.append(SynologyAPINames.BASE_API_URI).append(apiDescription.getDelete().get().getPath());
			UriComponentsBuilder uribuilder = UriComponentsBuilder
            .fromHttpUrl(builder.toString())
            .queryParam("api", SynologyAPINames.SYNO_DELETE_API)
            .queryParam("version", SynologyAPIVersions.SYNO_DELETE_API.getVersion())
            .queryParam("method", "delete")
            .queryParam("path", java.net.URLEncoder.encode(sb.toString(),"UTF-8"))
            .queryParam("_sid", userSession.getSid());
			return uribuilder.build(true).toUri();  
		} catch (UnsupportedEncodingException e) {
			log.error("下载文件!!",e);
			throw new ServiceException(CommonRespInfo.SYS_ERROR,e);
		}
    }

    /**
     *
     * @param filePaths
     * @param expired
     * @return
     */
    public String buildSharingUrlsQuery(List<String> filePaths, String expired){
        //拼接要操作的路径,eg.  ["obs/test/1.pdf","obs/test/2.pdf"]
        StringBuilder sb = new StringBuilder("[");
        for (String file : filePaths) {
            sb.append("\"").append(file).append("\"").append(",");
        }
        int deleteIndex = sb.lastIndexOf(",");
        sb.deleteCharAt(deleteIndex);
        sb.append("]");
        StringBuilder builder = new StringBuilder(baseURL);
        builder.append(SynologyAPINames.BASE_API_URI).append(apiDescription.getSharing().get().getPath()).append("?")
                .append("api=").append(SynologyAPINames.SYNO_SHARING_API)
                .append("&version=").append(SynologyAPIVersions.SYNO_SHARING_API.getVersion())
                .append("&method=create")
                .append("&path=").append(sb.toString())
                .append("&date_expired=").append(expired)
                .append("&_sid=").append(userSession.getSid());
        return builder.toString();
    }

    /**
     * 这里源代码使用双引号处理文件路径 当文件中有特殊字符会导致群晖报119错误
     * 正确的做法是将path做URLEncode
	 * ->spring中org.springframework.web.client.RestTemplate.execute(String, HttpMethod, RequestCallback, ResponseExtractor<T>, Object...)中调用了
     * -> org.springframework.web.client.RestTemplate.execute(String, HttpMethod, RequestCallback, ResponseExtractor<T>, Object...)
     * -> org.springframework.web.util.DefaultUriBuilderFactory.DefaultUriBuilder.createUri(UriComponents)
     * -> org.springframework.web.util.HierarchicalUriComponents.encodeUriComponent(String, Charset, Type)
     * 他对传入的uri参数转换为asci2码并逐一判断是否转码(),我们如果将文件名转码了送入会导致转码的符号再转码引起错误
     * 但是如果我们不转码直接传入的参数路径中有# 又会被这个方法当成切片,把文件路径切割
     * 故这里使用URI的方式,避免送入string的http路径导致不可预料的编码和解码
     * @throws UnsupportedEncodingException 
     */
    public URI buildDownloadQuery(String path) {
        StringBuilder builder = new StringBuilder(baseURL);
        try {
			builder.append(SynologyAPINames.BASE_API_URI).append(apiDescription.getDownload().get().getPath());
			UriComponentsBuilder uribuilder = UriComponentsBuilder
            .fromHttpUrl(builder.toString())
            .queryParam("api", SynologyAPINames.SYNO_DOWNLOAD_API)
            .queryParam("version", SynologyAPIVersions.SYNO_DOWNLOAD_API.getVersion())
            .queryParam("method", "download")
            .queryParam("path", java.net.URLEncoder.encode(path,"UTF-8"))
            .queryParam("mode", "download")
            .queryParam("_sid", userSession.getSid());
			return uribuilder.build(true).toUri();  
		} catch (UnsupportedEncodingException e) {
			log.error("下载文件!!",e);
			throw new ServiceException(CommonRespInfo.SYS_ERROR,e);
		}
    }
    
    public String buildFileStationInfoQuery(){
        StringBuilder builder = new StringBuilder(baseURL);
        builder.append(SynologyAPINames.BASE_API_URI)
        .append("entry.cgi").append("?")
                .append("api=").append(SynologyAPINames.SYNO_FILESTATION_INFO_API)
                .append("&version=").append(SynologyAPIVersions.SYNO_FILESTATION_INFO_API.getVersion())
                .append("&method=get")
                .append("&_sid=").append(userSession.getSid());
        return builder.toString();
    }
}
