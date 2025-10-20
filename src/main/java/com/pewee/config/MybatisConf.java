package com.pewee.config;

import com.pewee.util.MyMapper;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tk.mybatis.spring.annotation.MapperScan;
import tk.mybatis.spring.mapper.MapperScannerConfigurer;

import java.util.Properties;


@Configuration
//必须在DataSourceConfig注册后再加载MapperScannerConfigurer，否则会报错
@AutoConfigureAfter(MybatisAutoConfiguration.class)
@MapperScan("com.pewee.mapper")
public class MybatisConf{
	
	/**
	 * 配置扫描XML路径
	 * @return
	 */
	@Bean
	public MapperScannerConfigurer getConfigur() {
		MapperScannerConfigurer configurer = new MapperScannerConfigurer();
		configurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
		/**
		 * 扫描mapper.java
		 */
		configurer.setBasePackage("com.pewee.mapper");
		
		/**
		 * 在properties中配好了,这里不需要了
		 */
		Properties properties = new Properties();
	    properties.setProperty("mappers", MyMapper.class.getName());//MyMapper这个类接下来会创建
	    properties.setProperty("notEmpty", "false");
	    properties.setProperty("IDENTITY", "MYSQL");
	    //特别注意mapperScannerConfigurer的引入import tk.mybatis.spring.mapper.MapperScannerConfigurer;引入错误则没下面这个方法	
	    configurer.setProperties(properties);
		return configurer;
	}
	
	
}
