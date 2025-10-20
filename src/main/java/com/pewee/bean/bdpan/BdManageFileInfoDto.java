package com.pewee.bean.bdpan;

import lombok.Data;

/**
 * 管理文件info
 * @author pewee
 *
 */
@Data
public class BdManageFileInfoDto {
	
	//0表示成功
	private int errno;
	
	//路径
	private String path;
	
}
