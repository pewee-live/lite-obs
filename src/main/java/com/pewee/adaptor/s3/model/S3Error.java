package com.pewee.adaptor.s3.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * S3 标准错误响应 XML 模型
 */
@XmlRootElement(name = "Error")
public class S3Error {

    private String code;
    private String message;
    private String resource;
    private String requestId;

    public S3Error() {
    }

    public S3Error(String code, String message, String resource, String requestId) {
        this.code = code;
        this.message = message;
        this.resource = resource;
        this.requestId = requestId;
    }

    @XmlElement(name = "Code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @XmlElement(name = "Message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @XmlElement(name = "Resource")
    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    @XmlElement(name = "RequestId")
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
