package com.pewee.engine.qiniu;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "qiniu")
@Getter
@Setter
@Component
public class QiniuConfig {
	
	private String ak;
	
	private String sk;
	
}
