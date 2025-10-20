package com.pewee.bean.bdpan;

import java.util.List;

import lombok.Data;

/**
 * 
 * @author pewee
 *
 */
@Data
public class BdManageFileRespDto {
	
	//0表示成功
	private int errno;
	
	
	private String request_id;
	
	/**
	 * 异步下返回有taskid,请求时async字段设置
	 */
	private String taskid;
	
	private List<BdManageFileInfoDto> info;
	
}	


