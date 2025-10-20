package com.pewee.bean.bdpan;

import lombok.Data;
/**
 * 使用authorization_code时,百度盘返回的信息
 * @author pewee
 *
 */
@Data
public class BdTokenInfo {
	  
	private String access_token;private String expires_in;
	private String session_secret;private String session_key;
	private String refresh_token;private String scope;
}
