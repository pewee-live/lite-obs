package com.pewee.adaptor.s3.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "ListAllMyBucketsResult")
public class ListAllMyBucketsResult {

    private Owner owner;
    private List<BucketEntry> buckets = new ArrayList<>();

    public ListAllMyBucketsResult() {
    }

    public ListAllMyBucketsResult(String ownerId, String ownerName) {
        this.owner = new Owner(ownerId, ownerName);
    }

    @XmlElement(name = "Owner")
    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    @XmlElementWrapper(name = "Buckets")
    @XmlElement(name = "Bucket")
    public List<BucketEntry> getBuckets() {
        return buckets;
    }

    public void setBuckets(List<BucketEntry> buckets) {
        this.buckets = buckets;
    }

    public static class Owner {
        private String id;
        private String displayName;

        public Owner() {
        }

        public Owner(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        @XmlElement(name = "ID")
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @XmlElement(name = "DisplayName")
        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }

    public static class BucketEntry {
        private String name;
        private String creationDate;

        public BucketEntry() {
        }

        public BucketEntry(String name, String creationDate) {
            this.name = name;
            this.creationDate = creationDate;
        }

        @XmlElement(name = "Name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlElement(name = "CreationDate")
        public String getCreationDate() {
            return creationDate;
        }

        public void setCreationDate(String creationDate) {
            this.creationDate = creationDate;
        }
    }
}
