package com.pewee.util.resp;

import com.alibaba.fastjson.JSON;

import java.io.Serializable;

/**
 * 返回对象
 * @author pewee
 *
 */
public class  RespEntity<T>  implements IResponse,Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 239216757737141280L;

	private String code;
	
	private String msg;
	
	private T data;
	
	public RespEntity() {
		super();
	}

	public  RespEntity(IResponse resp,T data) {
		this.code = resp.getCode();
		this.msg = resp.getMsg();
		this.data = data;
	}
	
	public RespEntity<T> applyRespCodeMsg(IResponse resp) {
		this.code = resp.getCode();
		this.msg = resp.getMsg();
		return this;
	}

	public RespEntity<T> applyRespCodeMsg(String code,String msg) {
		this.code = code;
		this.msg = msg;
		return this;
	}
	
	public RespEntity<T> applyData(T data) {
		this.data = data;
		return this;
	}


	public void setCode(String code){this.code = code;}
	public void setMsg(String msg){this.msg = msg;}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMsg() {
		return msg;
	}
	
	public T getData() {
		return data;
	}


	public void setData(T data) {
		this.data = data;
	}
	
	@Override
	public String toString() {
		return JSON.toJSONString(this);
	}
	
}
