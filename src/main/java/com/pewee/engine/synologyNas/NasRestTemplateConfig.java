package com.pewee.engine.synologyNas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
@Configuration
public class NasRestTemplateConfig {
	
	/**
	 * 群晖nas rest调用时需要的restTemplate
	 * @return
	 */
	@Bean("nasClientRestTemplate")
	public RestTemplate nasClientRestTemplate(){
		RestTemplate nasClientRestTemplate = new RestTemplate();
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
		MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
		converter.setSupportedMediaTypes(Arrays.asList(MediaType.ALL, MediaType.APPLICATION_OCTET_STREAM));
		messageConverters.add(converter);
		nasClientRestTemplate.setMessageConverters(messageConverters);
		return nasClientRestTemplate;
	}

}
