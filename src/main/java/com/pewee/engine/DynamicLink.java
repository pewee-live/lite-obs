package com.pewee.engine;

import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.ServiceException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 和动态下载链接相关
 * @author pewee
 *
 */

@ConfigurationProperties(prefix = "obs.engine")
@Getter
@Setter
@Component
public class DynamicLink {
	private Boolean dynamicLink;
	
	private Integer seconds;
	
	private Integer downloadNumber;
	
	@Resource
	private JedisCluster jedisCluster;
	
	private static final String DYNAMIC_LINK_KEY = "com.pewee:obs.engine:dynamicLink:key:%s";
	
	private static final String DYNAMIC_LINK_CODE_KEY = "code";
	private static final String DYNAMIC_LINK_DOWNLOAD_NUMBER_KEY = "num";
	
	/**
	 * 是否为动态下载链接
	 * @return
	 */
	public boolean isDynamicLink() {
		return this.dynamicLink;
	}
	
	/**
	 * 获取动态链接redis的key
	 * @param code
	 * @return
	 */
	public String getKey(String code) {
		return String.format(DYNAMIC_LINK_KEY, code);
	}
	
	/**
	 * 将送入的key转化
	 * 如果OBS支持动态链接就做转化,如果不支持,会将code原样返回
	 * 如果送入的code没有在缓存找到,或者剩余时间小于10秒会抛出 ERROR_DYNAMIC_CODE("-100053","该链接已经过期失效,请重新查询下载链接!!"),
	 * 如果送入的code找到,但是剩余可用次数<=0,会抛出TOO_MANY_DYNAMIC_CODE("-100054","该链接下载次数过多,已被禁止,请重新查询下载链接!!"),
	 * @param code -> 这里送入的code是之前通过convertDynamicLink转换的uuid,这里再转回来
	 * @return
	 */
	public String transformCode(String code){
		if (isDynamicLink()) {
			String key = getKey(code);
			if (! jedisCluster.exists(key)  && jedisCluster.ttl(key) < 10) {
				throw new ServiceException(CommonRespInfo.ERROR_DYNAMIC_CODE);
			}
			Map<String, String> all = jedisCluster.hgetAll(key);
			if (  Integer.valueOf(all.get(DYNAMIC_LINK_DOWNLOAD_NUMBER_KEY))  <= 0) {
				throw new ServiceException(CommonRespInfo.TOO_MANY_DYNAMIC_CODE);
			}
			return all.get(DYNAMIC_LINK_CODE_KEY);
		} else {
			return code;
		}
	}
	
	/**
	 * 将送入的code减少一次计数
	 * 用于某个动态链接下载前做减操作
	 * 如果OBS支持动态链接就做减操作,如果不支持,什么都不做
	 * @param code 
	 * @return 现在可用次数
	 */
	public int countDown(String code) {
		if (isDynamicLink()) {
			return jedisCluster.hincrBy(getKey(code), DYNAMIC_LINK_DOWNLOAD_NUMBER_KEY, -1L).intValue();
		}
		return Integer.MAX_VALUE;
	}
	
	
	/**
	 * 创建动态链接
	 * 如果OBS支持动态链接就做转化,如果不支持,会将code原样返回
	 * @param code
	 * @return
	 */
	public String generateDynamicLink(String code) {
		if (isDynamicLink()) {
			String uuidstring = UUID.randomUUID().toString().replaceAll("-", "");
			String key = getKey(uuidstring);
			HashMap<String,String> hashMap = new HashMap<String,String>();
			hashMap.put(DYNAMIC_LINK_CODE_KEY, code);
			hashMap.put(DYNAMIC_LINK_DOWNLOAD_NUMBER_KEY, downloadNumber.toString());
			jedisCluster.hset(key, hashMap);
			jedisCluster.expire(key, seconds);
			return uuidstring;
		}
		return code;
	}
	
}
