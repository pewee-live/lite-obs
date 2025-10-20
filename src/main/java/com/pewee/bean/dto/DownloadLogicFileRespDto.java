package com.pewee.bean.dto;

import lombok.Data;

import java.util.List;

/**
 * 下载文件响应
 * 
 * @author pewee
 *
 */
@Data
public class DownloadLogicFileRespDto {

	private Long id;

	/** 唯一码 code */
	private String code;

	/** 文件名 file_name */
	private String fileName;

	/** mime类型 mime_type */
	private String mimeType;

	/** 长度 length */
	private Long length;

	/** 是否切片 (0:不切片 1:切片) split */
	private Integer split;

	/** 分片数 total */
	private Integer total;

	/** 校验和 crc32 */
	private String crc32;
	
	/**
	 * 下载URL
	 */
	private String url;
	
	List<DownloadFileDto> files;
}
