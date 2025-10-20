package com.pewee.bean.nas.api.model.sharing.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pewee.bean.nas.api.genericresponses.Data;
import com.pewee.bean.nas.api.model.sharing.Link;

import java.util.List;

/**
 * @Classname SharingResponse
 * @Description TODO
 * @Version 1.0.0
 * @Date 2022/4/21 11:04
 * @Created by Mr.GongRan
 */
@JsonDeserialize(using = JsonDeserializer.None.class)
public class SharingResponse extends Data {
    @JsonProperty("has_folder")
    private String hasFolder;

    @JsonProperty("links")
    private List<Link> links;

    @JsonProperty("has_folder")
    public String getHasFolder() {
        return hasFolder;
    }

    @JsonProperty("has_folder")
    public void setHasFolder(String hasFolder) {
        this.hasFolder = hasFolder;
    }

    @JsonProperty("links")
    public List<Link> getLinks() {
        return links;
    }

    @JsonProperty("links")
    public void setLinks(List<Link> links) {
        this.links = links;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("\n");
        buffer.append("\thas_folder: ").append(this.hasFolder).append(",\n");
        buffer.append("\tlinks: [\n");
        for (Link link : this.links) {
            buffer.append("\t\t{\n");
            buffer.append("\t\t\tenableUpload: ").append(link.isEnableUpload()).append("\n");
            buffer.append("\t\t\thasPassword: ").append(link.isHasPassword()).append("\n");
            buffer.append("\t\t\tid: ").append(link.getId()).append("\n");
            buffer.append("\t\t\tisFolder: ").append(link.isFolder()).append("\n");
            buffer.append("\t\t\tname: ").append(link.getName()).append("\n");
            buffer.append("\t\t\tpath: ").append(link.getPath()).append("\n");
            buffer.append("\t\t\tqrcode: ").append(link.getQrcode()).append("\n");
            buffer.append("\t\t\turl: ").append(link.getUrl()).append("\n");
            buffer.append("\t\t},\n");
        }
        buffer.append("\t]\n");
        return buffer.toString();
    }
}
