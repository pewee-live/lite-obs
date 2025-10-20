package com.pewee.bean.dto;

import java.io.InputStream;
import java.util.List;

import com.pewee.bean.File;
import com.pewee.bean.LogicFile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SingleStreamDto {
	
	String maskcode;
	
	Boolean dynamicLink;
	
	LogicFile file;
	
	//必须按照sequence排序
	List<File> singlefile;
	
	Integer sequence;
	
	InputStream stream;
	
	/**
	 * 是否还有下一个流
	 * @return
	 */
	public boolean hasNextStream() {
		return sequence <= singlefile.size() -1;
	}
	
	
}
