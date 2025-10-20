package com.pewee.bean.nas;


public interface UserSession {

    public String getSid();
    
    public void setSid(String sid);
    
    public int getTtl();
    
    public void setTtl(int ttl);
}
