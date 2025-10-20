package com.pewee.bean.nas;

import lombok.extern.slf4j.Slf4j;
/**
 * 不再使用内存存储session
 * @See  {@link com.pewee.bean.nas.UserSessionRedis}
 * @author pewee
 *
 */
@Slf4j
@Deprecated
public class UserSessionLocal implements UserSession{
	private String sid;
	
	private int ttl;

    public UserSessionLocal(String sid) {
        //开发环境下打印调试用,上线前删除
        
    }

    public String getSid() {
        return sid;
    }

	@Override
	public void setSid(String sid) {
		log.info(sid);
        System.out.println(sid);
        this.sid = sid;
	}

	@Override
	public int getTtl() {
		return this.ttl;
	}
	
	public void setTtl(int ttl) {
		this.ttl = ttl;
	}
}
