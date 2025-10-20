package com.pewee.engine;

import lombok.Getter;

@Getter
public enum EngineInfoEnum {
	LOCAL("local",0),
	SYNOLOGYNAS("synologynas",1),
	SEVENNIU("qiniu",2),
	BAIDU("baidu",3),
	;
	
	private String name;
	
	private Integer type;
	
	EngineInfoEnum(String name,Integer type){
		this.name = name;
		this.type = type;
	}
	
}
