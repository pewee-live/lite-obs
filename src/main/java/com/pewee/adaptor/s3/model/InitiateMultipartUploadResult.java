package com.pewee.adaptor.s3.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "InitiateMultipartUploadResult")
public class InitiateMultipartUploadResult {

    private String bucket;
    private String key;
    private String uploadId;

    public InitiateMultipartUploadResult() {
    }

    public InitiateMultipartUploadResult(String bucket, String key, String uploadId) {
        this.bucket = bucket;
        this.key = key;
        this.uploadId = uploadId;
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

    @XmlElement(name = "UploadId")
    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }
}
