package com.pewee.bean.dto;

import java.util.List;

import lombok.Data;

/**
 * 用于查询上载文件状态的实体
 * @author pewee
 * 2026年9月29日
 */
@Data
public class LogicFileStausDTO {
	
	private String code;
	
	//状态 (0 : 初始化 1: 上传中 2.上传完成 3:已删除)
	private Integer status;
	
	//是否切片 (0:不切片 1:切片)
	private Integer split;
	
	//分片数
	private Integer total;
	
	//已经上传的切片列表
	private List<Integer> uploadSequences;
}
