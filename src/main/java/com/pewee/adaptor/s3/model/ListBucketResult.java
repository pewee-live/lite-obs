package com.pewee.adaptor.s3.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "ListBucketResult")
public class ListBucketResult {

    private String name;
    private String prefix;
    private String marker;
    private int maxKeys = 1000;
    private String delimiter;
    private boolean isTruncated = false;
    private List<ObjectEntry> contents = new ArrayList<>();
    private List<CommonPrefix> commonPrefixes = new ArrayList<>();

    public ListBucketResult() {
    }

    @XmlElement(name = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement(name = "Prefix")
    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @XmlElement(name = "Marker")
    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    @XmlElement(name = "MaxKeys")
    public int getMaxKeys() {
        return maxKeys;
    }

    public void setMaxKeys(int maxKeys) {
        this.maxKeys = maxKeys;
    }

    @XmlElement(name = "Delimiter")
    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    @XmlElement(name = "IsTruncated")
    public boolean isTruncated() {
        return isTruncated;
    }

    public void setTruncated(boolean truncated) {
        isTruncated = truncated;
    }

    @XmlElement(name = "Contents")
    public List<ObjectEntry> getContents() {
        return contents;
    }

    public void setContents(List<ObjectEntry> contents) {
        this.contents = contents;
    }

    @XmlElement(name = "CommonPrefixes")
    public List<CommonPrefix> getCommonPrefixes() {
        return commonPrefixes;
    }

    public void setCommonPrefixes(List<CommonPrefix> commonPrefixes) {
        this.commonPrefixes = commonPrefixes;
    }

    public static class CommonPrefix {
        private String prefix;

        public CommonPrefix() {
        }

        public CommonPrefix(String prefix) {
            this.prefix = prefix;
        }

        @XmlElement(name = "Prefix")
        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }

    public static class ObjectEntry {
        private String key;
        private String lastModified;
        private String etag;
        private long size;
        private String storageClass = "STANDARD";

        public ObjectEntry() {
        }

        @XmlElement(name = "Key")
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @XmlElement(name = "LastModified")
        public String getLastModified() {
            return lastModified;
        }

        public void setLastModified(String lastModified) {
            this.lastModified = lastModified;
        }

        @XmlElement(name = "ETag")
        public String getEtag() {
            return etag;
        }

        public void setEtag(String etag) {
            this.etag = etag;
        }

        @XmlElement(name = "Size")
        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        @XmlElement(name = "StorageClass")
        public String getStorageClass() {
            return storageClass;
        }

        public void setStorageClass(String storageClass) {
            this.storageClass = storageClass;
        }
    }
}
