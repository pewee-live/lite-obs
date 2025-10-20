package com.pewee.engine;

import java.util.Date;

import javax.persistence.Transient;

import lombok.Builder;
import lombok.Data;

/**
 * 文件上下文
 * @author pewee
 *
 */
@Data
@Builder
public class FileContext {
	
	private String fileName;
	private String sysCode;
	/** 是否切片 (0:不切片 1:切片)  split */
    private Integer split;
    @Transient
	private byte[] content;
    
    //用于插入到file表create_time的时间,在调用save()前设置,在调用后会将该值insert到file表,引擎有需要的话可以自行做修改
    //防止上传时跨天导致下载时用的是后一天的bug
    private Date now;
	
	private String engineNamespace;
}
