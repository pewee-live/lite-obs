package com.pewee.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.pewee.engine.synologyNas.NasConfig.NasAuth;

import lombok.Getter;
import lombok.Setter;

/**
 * Engine的多实例通过账号切换配置
 * 虽然该配置为引擎定义中集成的配置,但是对于那些没有多实例的引擎实现,
 * 不需要考虑实现com.pewee.engine.EngineAccess.defaultNamespace()和com.pewee.engine.EngineAccess.getNamespaceBySysCode(),直接返回""就是默认实现
 * @author pewee
 */
@ConfigurationProperties(prefix = "engine")
@Getter
@Setter
@Component
public class EngineNameSpaceConfig {
	
private List<EngineNamespace> namespaces;
	
	@Getter
	@Setter
	public static class EngineNamespace {
		private String namespace;
		private String sysCode;
	}
}
