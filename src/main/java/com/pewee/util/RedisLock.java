package com.pewee.util;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * redis实现
 * @author pewee
 *
 */
@Component
public class RedisLock implements ILock{
	private static Logger logger = LoggerFactory.getLogger(RedisLock.class);
 	private static final Long FAIL = 0L;
	private static final Long SUCCESS = 1L;
	private static final Long WRONG_OPA = 3L;
	private static final String LOCK = "lock";
	private static final String UNLOCK = "unlock";
	static String luasrc;
	static {
		try {
			luasrc = IOUtils.toString(RedisLock.class.getClassLoader().getResourceAsStream("redislock.lua"),
					"UTF-8") ;
		} catch (IOException e) {
			logger.error("读取锁脚本失败!!!",e);
		}
	}
	@Autowired
	JedisCluster jedisCluster;

	@Override
	public boolean tryLock(String lockKey, String requestId, int expireTime,boolean failfast) {
		long start = System.currentTimeMillis();
		long end = start + expireTime;
		boolean flag = false;
		while(System.currentTimeMillis() <= end && !flag ) {
			flag = doTryLock(jedisCluster, lockKey, requestId);
			if(flag) {
				return flag;
			} else {
				if (failfast) {
					return flag;
				}
				try {
					Thread.currentThread().sleep(100L);
				} catch (InterruptedException e) {
					logger.error("线程被打断",e);
				}	
			}
		}
		return flag;
	}
	
	private synchronized boolean doTryLock(JedisCluster jedisCluster, String lockKey, String requestId) {
		ArrayList<String> keys = new ArrayList<String>();
		keys.add(getHashKey(lockKey) + LOCK);
		keys.add(getHashKey(lockKey) + lockKey);
		Object result = jedisCluster.eval(luasrc, keys, Collections.singletonList(requestId));
        if (SUCCESS.equals(result)) {
            return true;
        }
        return false;
    }
	
	private static String getHashKey(String lockKey) {
		return "{" + lockKey + "}";
	}

	@Override
	public boolean releaseLock(String lockKey, String requestId, int expireTime) {
		long start = System.currentTimeMillis();
		long end = start + expireTime;
		boolean flag = false;
		while(System.currentTimeMillis() <= end && !flag) {
			flag = doReleaseLock(jedisCluster, lockKey, requestId);
			if(flag) {
				return flag;
			} else {
				try {
					Thread.currentThread().sleep(100L);
				} catch (InterruptedException e) {
					logger.error("线程被打断",e);
				}	
			}
		}
		return flag;
	}
	
	public synchronized boolean doReleaseLock(JedisCluster jedisCluster, String lockKey, String requestId) {
    	ArrayList<String> keys = new ArrayList<String>();
		keys.add(getHashKey(lockKey) + UNLOCK);
		keys.add(getHashKey(lockKey) + lockKey);
    	Object result = jedisCluster.eval(luasrc, keys, Collections.singletonList(requestId));
    	if (SUCCESS.equals(result)) {
            return true;
        }
        return false;
    }
	
}
