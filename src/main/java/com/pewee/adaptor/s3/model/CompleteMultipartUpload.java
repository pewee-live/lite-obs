package com.pewee.adaptor.s3.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "CompleteMultipartUpload")
public class CompleteMultipartUpload {

    private List<Part> parts = new ArrayList<>();

    public CompleteMultipartUpload() {
    }

    @XmlElement(name = "Part")
    public List<Part> getParts() {
        return parts;
    }

    public void setParts(List<Part> parts) {
        this.parts = parts;
    }

    public static class Part {
        private int partNumber;
        private String etag;

        public Part() {
        }

        public Part(int partNumber, String etag) {
            this.partNumber = partNumber;
            this.etag = etag;
        }

        @XmlElement(name = "PartNumber")
        public int getPartNumber() {
            return partNumber;
        }

        public void setPartNumber(int partNumber) {
            this.partNumber = partNumber;
        }

        @XmlElement(name = "ETag")
        public String getEtag() {
            return etag;
        }

        public void setEtag(String etag) {
            this.etag = etag;
        }
    }
}
