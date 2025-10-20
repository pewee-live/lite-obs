package com.pewee.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPoolConfig;
/**
 * 配置jediscluster
 * @author pewee
 *
 */
@Configuration
public class RedisConfig {
	
	@Value("${spring.redis.jedis.pool.max-idle}")
	private int maxIdle;
	
	@Value("${spring.redis.jedis.pool.max-wait}")
	private long maxWaitMillis;
	
	@Value("${spring.redis.jedis.pool.testOnBorrow}")
	private boolean testOnBorrow;
	
	@Value("${spring.redis.jedis.pool.maxTotal}")
	private int maxTotal;
	
	@Value("${spring.redis.jedis.pool.min-idle}")
	private int minIdle;
	
	@Value("${spring.redis.password}")
	private String passWord;
	
	@Value("${spring.redis.cluster.nodes}")
	private String hostAndPort;
	
	
	@Bean
	public redis.clients.jedis.JedisPoolConfig getRedisPoolConfig(){
		JedisPoolConfig poolConfig = new redis.clients.jedis.JedisPoolConfig();
		poolConfig.setMaxIdle(maxIdle);
		poolConfig.setMaxWaitMillis(maxWaitMillis);
		poolConfig.setTestOnBorrow(testOnBorrow);
		poolConfig.setMaxTotal(maxTotal);
		poolConfig.setMinIdle(minIdle);
		return poolConfig;
	}
	
	@Bean
	public JedisClusterFactory getClusterFactory(){
		
		JedisClusterFactory factory = new JedisClusterFactory();
		factory.setTimeout(3000);
		factory.setMaxRedirections(6);
		factory.setPassWord(passWord);
		factory.setGenericObjectPoolConfig(getRedisPoolConfig());
		factory.setHostAndPort(hostAndPort);
		return factory;
	}

}
