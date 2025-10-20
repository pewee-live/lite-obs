package com.pewee.bean;

import lombok.Data;

/**
 * 单词上传文件的信息上下文
 * 用来传递一些需要的信息
 * @author pewee
 *
 */
@Data
public class SingleUploadFileContext {
	
	/**
	 * 文件名 上传文件原名;在分片时,所有分片文件可以根据该文件名来命名part文件,他将作为存储到各个引擎的显示文件名
	 * 如 batchcode.part1 
	 */
	private String fileName;
	/**
	 * 长度
	 * 当split = 0时代表整个文件的长度
	 * 当split = 1时代表本分片文件的长度
	 */
	private Long length;
	/**
	 * 引擎类型
	 */
	private Integer storageType;
	/** 是否切片 (0:不切片 1:切片)  split */
	private Integer split;
	/** 分片数 只有当split = 1时会大于1,默认为1  total */
	private Integer total;
	/** 批次号  等于LogicFile 的code 字段 */
	private String batchCode;
	/** 片号  */
	private Integer seq;
	/** 校验和  crc32 */
	private String crc32;
	/** 系统编码  sys_code */
	private String sysCode;
	/** 系统代号  sys_alisa */
	private String sysAlisa;
	
	
}
