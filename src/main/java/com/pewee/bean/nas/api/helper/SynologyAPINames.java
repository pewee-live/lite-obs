package com.pewee.bean.nas.api.helper;

import java.util.Arrays;
import java.util.List;

public abstract class SynologyAPINames {

    public final static String BASE_API_URI = "webapi/";
    public final static String SYNO_SESSION = "FileStation";
    public final static String SYNO_INFO_API = "SYNO.API.Info";
    public final static String SYNO_AUTH_API = "SYNO.API.Auth";
    public final static String SYNO_FILESTATION_INFO_API = "SYNO.FileStation.Info";
    public final static String SYNO_LIST_API = "SYNO.FileStation.List";
    public final static String SYNO_CREATE_FOLDER_API = "SYNO.FileStation.CreateFolder";
    public final static String SYNO_UPLOAD_API = "SYNO.FileStation.Upload";
    public final static String SYNO_DELETE_API = "SYNO.FileStation.Delete";
    public final static String SYNO_SHARING_API = "SYNO.FileStation.Sharing";
    public final static String SYNO_DOWNLOAD_API = "SYNO.FileStation.Download";


    public List<String> getAllowedMethods(String apiName) {
        if(SYNO_AUTH_API.equals(apiName)) {
            return Arrays.asList("login", "logout");
        }
        if(SYNO_LIST_API.equals(apiName)) {
            return Arrays.asList("list_share", "list", "getinfo");
        }
        if(SYNO_CREATE_FOLDER_API.equals(apiName)) {
            return Arrays.asList("create");
        }
        if(SYNO_UPLOAD_API.equals(apiName)) {
            return Arrays.asList("upload");
        }
        if (SYNO_DELETE_API.equals(apiName)){
            return Arrays.asList("start","status","stop","delete");
        }
        if (SYNO_SHARING_API.equals(apiName)){
            return Arrays.asList("getinfo","list","create","delete","clear_invalid");
        }
        if (SYNO_DOWNLOAD_API.equals(apiName)){
            return Arrays.asList("download");
        }
        throw new IllegalStateException("Unexpected value: " + apiName);
    }


}
