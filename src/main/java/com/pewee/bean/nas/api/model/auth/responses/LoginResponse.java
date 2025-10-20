package com.pewee.bean.nas.api.model.auth.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pewee.bean.nas.api.genericresponses.Data;

@JsonDeserialize(using = JsonDeserializer.None.class)
public class LoginResponse extends Data {

    @JsonProperty("sid")
    private String sid;

    public LoginResponse() {
        super();
    }

    public LoginResponse(String sid) {
        super();
        this.sid = sid;
    }

    @JsonProperty("sid")
    public String getSid() {
        return sid;
    }

    @JsonProperty("sid")
    public void setSid(String sid) {
        this.sid = sid;
    }

    @Override
    public String toString() {
        return "sid: "+this.sid;
    }
}
