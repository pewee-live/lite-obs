package com.pewee.engine.bdpan;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "bdpan")
@Getter
@Setter
@Component
public class BdPanConfig {
	
	private String appid;
	private String appkey;
	private String secretkey;
	private String sign;
	private String backUrl;
	
}
