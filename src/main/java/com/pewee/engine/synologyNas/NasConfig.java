package com.pewee.engine.synologyNas;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NAS的配置入口
 * @author pewee
 * 2024年5月24日
 */
@ConfigurationProperties(prefix = "synologynas")
@Getter
@Setter
@Component
public class NasConfig {
	
	private List<NasAuth> authList;
	
	@Getter
	@Setter
	public static class NasAuth {
		private String namespace;
		private boolean prefer = false;
		private String url;
		private String user;
		private String auth;
	}
}
