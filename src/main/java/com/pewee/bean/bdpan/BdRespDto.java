package com.pewee.bean.bdpan;

import java.util.List;

import lombok.Data;

/**
 * 百度返回参数
 * @author pewee
 * 
 * 预上传返回示例:
 * {"errno":0,"return_type":1,"block_list":[0,1],"uploadid":"P1-MTAuMjM1LjE4My4xMToxNjYxNzQ2MDQ2OjkwMzY3NTg0MjEzNTY2NjYzOTg=","request_id":9036758421356666398}
 *
 */
@Data
public class BdRespDto {
	//错误码
	private Integer errno;
	
	//以下为预上传字段
	//文件的绝对路径
	private String path;
	//上传唯一ID标识此上传任务
	private String uploadid;
	//返回类型，系统内部状态字段
	private int return_type;
	//需要上传的分片序号列表，索引从0开始
	private String[] block_list;
	//切片后的内容,当预上传成功后会放置入该实体
	private List<byte[]> splitContent;
	
	//以下为分片上传字段
	private String md5;
	
	//以下为请求预上传参数
	private PreUpReq req;
	
	//以下为创建文件响应参数
	//文件在云端的唯一标识ID
	private Long fs_id;
}
