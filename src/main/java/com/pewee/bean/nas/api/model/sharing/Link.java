package com.pewee.bean.nas.api.model.sharing;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @Classname Link
 * @Description TODO
 * @Version 1.0.0
 * @Date 2022/4/21 11:06
 * @Created by Mr.GongRan
 */
public class Link {
    @JsonProperty("enable_upload")
    private boolean enableUpload;

    @JsonProperty("has_password")
    private boolean hasPassword;

    @JsonProperty("id")
    private String id;

    @JsonProperty("isFolder")
    private boolean isFolder;

    @JsonProperty("name")
    private String name;

    @JsonProperty("path")
    private String path;

    @JsonProperty("qrcode")
    private String qrcode;

    @JsonProperty("url")
    private String url;

    public boolean isEnableUpload() {
        return enableUpload;
    }

    public void setEnableUpload(boolean enableUpload) {
        this.enableUpload = enableUpload;
    }

    @JsonProperty("has_password")
    public boolean isHasPassword() {
        return hasPassword;
    }

    @JsonProperty("has_password")
    public void setHasPassword(boolean hasPassword) {
        this.hasPassword = hasPassword;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("isFolder")
    public boolean isFolder() {
        return isFolder;
    }

    @JsonProperty("isFolder")
    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("path")
    public void setPath(String path) {
        this.path = path;
    }

    @JsonProperty("qrcode")
    public String getQrcode() {
        return qrcode;
    }

    @JsonProperty("qrcode")
    public void setQrcode(String qrcode) {
        this.qrcode = qrcode;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Link{" +
                "enableUpload='" + enableUpload + '\'' +
                ", hasPassword=" + hasPassword +
                ", id='" + id + '\'' +
                ", isFolder=" + isFolder +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", qrcode='" + qrcode + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
