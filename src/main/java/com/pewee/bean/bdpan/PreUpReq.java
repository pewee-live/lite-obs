package com.pewee.bean.bdpan;

import lombok.Data;

/**
 * 预上传请求
 * 请求示例:
 * path=%2Fapps%2F%E6%9D%A5%E8%87%AA%EF%BC%9A%E5%BC%80%E6%94%BE%E5%B9%B3%E5%8F%B0%2FActiviti-develop.zip&autoinit=1&target_path=%2Fapps%2F%E6%9D%A5%E8%87%AA%EF%BC%9A%E5%BC%80%E6%94%BE%E5%B9%B3%E5%8F%B0%2F&block_list=%5B%225910a591dd8fc18c32a8f3df4fdc1761%22%2C%22a5fc157d78e6ad1c7e114b056c92821e%22%5D&local_mtime=1647242613
 * @author pewee
 *
 */
@Data
public class PreUpReq {
	///上传后使用的文件绝对路径，需要urlencode  exp:apps/appName/filename.jpg
	private String path;
	//文件和目录两种情况：上传文件时，表示文件的大小，单位B；上传目录时，表示目录的大小，目录的话大小默认为0
	private int size;
	//是否为目录，0 文件，1 目录
	private int isdir = 0;
	//文件各分片MD5数组的json串。block_list的含义如下，
	//如果上传的文件小于4MB，其md5值（32位小写）即为block_list字符串数组的唯一元素；
	//如果上传的文件大于4MB，需要将上传的文件按照4MB大小在本地切分成分片，
	//不足4MB的分片自动成为最后一个分片，所有分片的md5值（32位小写）组成的字符串数组即为block_list。
	private String[] block_list;
	private int autoinit = 1;
	/**
	 * 文件命名策略。
		1 表示当path冲突时，进行重命名
		2 表示当path冲突且block_list不同时，进行重命名
		3 当云端存在同名文件时，对该文件进行覆盖
	 */
	private int rtype = 1;
	/**
	 * 上传ID
	 */
	private String uploadid;
	
	
	
}
