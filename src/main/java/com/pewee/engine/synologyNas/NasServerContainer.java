package com.pewee.engine.synologyNas;

import java.util.HashMap;
import java.util.Map;

import com.pewee.bean.nas.client.SynoRestClient;
import com.pewee.engine.synologyNas.NasConfig.NasAuth;

import lombok.Data;

/**
 * 用于承载多个NasServer相关的容器
 * @author pewee
 * 2024年5月24日
 */
@Data
public class NasServerContainer {
	
	private Map<String,NasAuth> nasConfigMap =  new HashMap<>();
	
	private Map<String,SynoRestClient> synoClientMap =  new HashMap<>();
	
	private String prefer;
	
	public NasAuth getPreferConfig() {
		return nasConfigMap.get(prefer);
	}
	
	public NasAuth getConfigByNamespace(String namespace) {
		return nasConfigMap.get(namespace);
	}
	
	public SynoRestClient getPreferClient() {
		return synoClientMap.get(prefer);
	}
	
	public SynoRestClient getClientByNamespace(String namespace) {
		return synoClientMap.get(namespace);
	}
	
	
	
}
