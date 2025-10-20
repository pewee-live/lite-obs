package com.pewee.bean.dto;

import lombok.Data;

/**
 * 真实文件信息
 * @author pewee
 *
 */
@Data
public class DownloadFileDto {
	
	/** 文件长度  length */
    private Long length;

	/** 文件码 切片文件一个批为一个码,对应logic的code  batch_code */
    private String batchCode;

	/** crc32校验码  crc32 */
    private String crc32;

	/** 切片时的排序 从0开始  sequence */
    private Integer sequence;
	
	/**
	 * 下载url
	 */
	private String url;
	
	
}
