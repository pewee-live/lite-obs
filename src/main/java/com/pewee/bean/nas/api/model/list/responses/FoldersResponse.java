package com.pewee.bean.nas.api.model.list.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pewee.bean.nas.api.genericresponses.Data;
import com.pewee.bean.nas.api.model.list.Folder;

import java.util.List;

@JsonDeserialize(using = JsonDeserializer.None.class)
public class FoldersResponse extends Data {
    @JsonProperty("files")
    private List<Folder> folders;

    public FoldersResponse() {
        super();
    }

    public FoldersResponse(List<Folder> folders) {
        super();
        this.folders = folders;
    }

    @JsonProperty("folders")
    public List<Folder> getFolders() {
        return folders;
    }

    @JsonProperty("folders")
    public void setFolders(List<Folder> folders) {
        this.folders = folders;
    }
}
