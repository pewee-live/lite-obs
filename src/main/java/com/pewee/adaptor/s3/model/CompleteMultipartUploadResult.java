package com.pewee.adaptor.s3.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "CompleteMultipartUploadResult")
public class CompleteMultipartUploadResult {

    private String location;
    private String bucket;
    private String key;
    private String etag;

    public CompleteMultipartUploadResult() {
    }

    public CompleteMultipartUploadResult(String location, String bucket, String key, String etag) {
        this.location = location;
        this.bucket = bucket;
        this.key = key;
        this.etag = etag;
    }

    @XmlElement(name = "Location")
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @XmlElement(name = "Bucket")
    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    @XmlElement(name = "Key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @XmlElement(name = "ETag")
    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }
}
