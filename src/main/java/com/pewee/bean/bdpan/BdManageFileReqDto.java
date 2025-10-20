package com.pewee.bean.bdpan;

import lombok.Data;

/**
 * 管理文件
 * @author pewee
 *
 */
@Data
public class BdManageFileReqDto {
	//0 同步，1 自适应，2 异步
	private int async = 1;
	
	//待操作文件 本次只需将文件path放入即可
	//[{"path":"/test/123456.docx","dest":"/test/abc","newname":"11223.docx","ondup":"fail"}]【copy/move示例】
	//[{"path":"/test/123456.docx","newname":"123.docx"}]【rename示例】
	//["/test/123456.docx"]【delete示例】
	private String[] filelist;
	
	//全局ondup,遇到重复文件的处理策略,可不传
	//fail(默认，直接返回失败)、newcopy(重命名文件)、overwrite、skip
	private String ondup;
}
