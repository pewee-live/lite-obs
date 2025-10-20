package com.pewee.bean.dto;

import lombok.Data;

/**
 * 上传文件响应
 * @author pewee
 *
 */
@Data
public class UploadFileRespDto {
	
	private String code;
	
	private String partCode;
	
	private Integer sequence;
	
	private long length;
	
	private String crc;
}
