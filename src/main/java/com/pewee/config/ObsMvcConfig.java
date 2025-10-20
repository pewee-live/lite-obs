package com.pewee.config;

import com.pewee.interceptor.ObsAccessInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Classname ObsMvcConfig
 * @Description TODO
 * @Version 1.0.0
 * @Date 2022/4/20 9:24
 * @Created by Mr.GongRan
 */
@Configuration
public class ObsMvcConfig implements WebMvcConfigurer {
    @Autowired
    private ObsAccessInterceptor accessInterceptor;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessInterceptor).addPathPatterns("/**").excludePathPatterns("/obs/sys/register","/obs/sys/authorize","/obs/logicfile/download/**","/bdpan/token","/bdpan/login");
    }
}
