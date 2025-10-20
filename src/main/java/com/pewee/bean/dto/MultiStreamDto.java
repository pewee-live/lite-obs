package com.pewee.bean.dto;

import java.io.InputStream;
import java.util.List;

import com.pewee.bean.LogicFile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MultiStreamDto {
	
	LogicFile file;
	
	List<InputStream> streamlist;
}
