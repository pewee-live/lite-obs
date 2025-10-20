package com.pewee.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

/**
 * 分布式自增器
 * @author pewee
 *
 */
@Component
public class DistributedAtomicInteger {
	
	@Autowired
	private JedisCluster jedisCluster;
	
	public synchronized int incrAndGet(String key) {
		Long incr = jedisCluster.incr(key);
		jedisCluster.expire(key, 60 * 60 * 24);
		return incr.intValue();
	}
	
	public synchronized int get(String key) {
		String incr = jedisCluster.get(key);
		if(StringUtils.isBlank(incr)) {
			incr = "0";
			jedisCluster.set(key, incr);
		}
		jedisCluster.expire(key, 60 * 60 * 24);
		return Integer.valueOf(incr);
	}

	public synchronized int incrAndGetWithoutExpire(String key) {
		return jedisCluster.incr(key).intValue();
	}
}
