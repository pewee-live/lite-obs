package com.pewee.service.impl;

import com.pewee.bean.Sys;
import com.pewee.util.StringUtils;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于生成token,校验token的服务
 * @author pewee
 *
 */
@Service
@Slf4j
public class TokenService {
	@Autowired
    private JedisCluster jedisCluster;
	
	 public static final String TONEN_PREFIX = "OBS ";
	 
	 private static final String REDIS_KEY_FREFIX = "pewee:obs:accesstoken:%s";
	 
	 public static final String TIME = "yyyy-MM-dd HH:mm:ss";
	 
	 /**
	  * 3小时过期
	  */
	 public static final int TTL = 60 * 60 * 3;
	
	 /**
	  * 生成token
	  * @param sys
	  * @return
	  */
	 public String generateAccessToken(Sys sys) {
    	String sharedTokenSecret=sys.getSecret();
        Key key = new SecretKeySpec(sharedTokenSecret.getBytes(),
                SignatureAlgorithm.HS256.getJcaName());
        Map<String, Object> headerMap = new HashMap<>();
        headerMap.put("typ", "JWT");
        headerMap.put("alg", "HS256");

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("sysCode", sys.getCode());
        payloadMap.put("sysAlias", sys.getSysAlias());
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat(TIME);
        payloadMap.put("timestamp", format.format(now));
        String token = TONEN_PREFIX + Jwts.builder().setHeaderParams(headerMap).setClaims(payloadMap).signWith(SignatureAlgorithm.HS256,key).compact();
        refreshToken(sys,token);
        return token;
	}
	 
	 /**
	  * 刷新token,不带token时表示为刷新,带token表示为创建流程
	  * @param sys
	  * @param token
	  */
	public void refreshToken(Sys sys, String token) {
		if (StringUtils.isNotBlank(token)) {
			jedisCluster.set(String.format(REDIS_KEY_FREFIX, sys.getCode()), token, SetParams.setParams().ex(TTL));
		} else {
			jedisCluster.expire(String.format(REDIS_KEY_FREFIX, sys.getCode()), TTL);
		}
	}
	
	/**
	 * 检查是否为Obs 的token
	 * @param token
	 * @return
	 */
	public boolean isObsToken(String token) {
		return token.length() > 4 &&  token.startsWith(TONEN_PREFIX);
	}
	
	/**
	 * 检查token合法
	 * @param token
	 * @return
	 */
	public boolean checkToken(Sys sys,String token) {
		String temToken = new String(token);
		if(isObsToken(token)) {
			temToken = temToken.substring(4);
		} else {
			log.error("校验token-不是OBS token!");
			return false;
		}
		
		if (! isNotExpire(sys,token)) {
			return false;
		};
		
		String sharedTokenSecret=sys.getSecret();//密钥
        Key key = new SecretKeySpec(sharedTokenSecret.getBytes(),
                SignatureAlgorithm.HS256.getJcaName());//HS256算法
        Jws<Claims> claimsJws = null;
		try {
			claimsJws = Jwts.parser().setSigningKey(key).parseClaimsJws(temToken);
		} catch (ExpiredJwtException e) {
			log.error("校验token-token校验失败!!",e);
			return false;
		} catch (UnsupportedJwtException e) {
			log.error("校验token-token校验失败!!",e);
			return false;
		} catch (MalformedJwtException e) {
			log.error("校验token-token校验失败!!",e);
			return false;
		} catch (SignatureException e) {
			log.error("校验token-token校验失败!!",e);
			return false;
		} catch (IllegalArgumentException e) {
			log.error("校验token-token校验失败!!",e);
			return false;
		}
        //解析ID令牌主体信息
        Claims body = claimsJws.getBody();
        String sysCode = body.get("sysCode", String.class);
        if (!sys.getCode().equals(sysCode) ) {
        	log.error("校验token-系统号不一致!!");
        	return false;
        }
        refreshToken(sys, null);
        return true;
	}
	
	/**
	 * 检查token是否过期
	 * @param sys
	 * @param token
	 * @return
	 */
	private boolean isNotExpire(Sys sys, String token) {
		String remotestring = jedisCluster.get(String.format(REDIS_KEY_FREFIX, sys.getCode()));
		if (! token.equals(remotestring)) {
			log.info("校验token-token不一致 token:{},remoteToken:{}",token,remotestring);
			return false;
		}
		return true;
	}
	
	/**
	 * 返回token的生存时间
	 * 当 key 不存在时，返回 -2 。 
	 * 当 key 存在但没有设置剩余生存时间时，返回 -1 。 否则，以秒为单位，返回 key 的剩余生存时间
	 * @param sysCode
	 * @return
	 */
	public Long getTtl(String sysCode) {
		Long ttl = jedisCluster.ttl(String.format(REDIS_KEY_FREFIX, sysCode));
		return ttl;
	}
	
	public String getTokenBySysCode(String sysCode) {
		String remotestring = jedisCluster.get(String.format(REDIS_KEY_FREFIX, sysCode));
		return remotestring;
	}
	
	/**
	 * 根据sysCode返回未过期的token
	 * @param sysCode
	 * @return
	 */
	public String checkTtlAndGetToken(String sysCode) {
		if (getTtl(sysCode) > 30 * 60) {
			return getTokenBySysCode(sysCode);
		}
		return null;
	}
}
