package com.pewee.bean.nas;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

import javax.annotation.Resource;
/**
 * redis的session实现,默认15分钟有效期
 * 需要为每个配置手动注入初始化
 * @author pewee
 *
 */
@Slf4j
public class UserSessionRedis implements UserSession{
	
	private static final String SESSION_KEY = "com.pewee:obs:session-key:%s";
	
	public UserSessionRedis (JedisCluster jedisCluster,String namespace) {
		this.namespace = namespace;
		this.jedisCluster = jedisCluster;
	}
	
	private JedisCluster jedisCluster;
	
	private String namespace;
	
	private String getRedisKey() {
		return String.format(SESSION_KEY, namespace);
	}

	@Override
	public String getSid() {
		String sid = null;
		if (jedisCluster.exists(getRedisKey())) {
			sid = jedisCluster.get(getRedisKey());
		}
		log.info("获取到sid :" + sid);
		return sid;
	}
	
	@Override
	public void setSid(String sid) {
		log.info("设置sid:{}",sid);
        jedisCluster.set(getRedisKey(), sid, SetParams.setParams());
	}

	@Override
	public int getTtl() {
		Long ttl = jedisCluster.ttl(getRedisKey());
		return ttl.intValue();
	}
	
	public void setTtl(int seconds) {
		jedisCluster.expire(getRedisKey(), seconds);
	}

}
