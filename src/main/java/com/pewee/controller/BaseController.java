package com.pewee.controller;

import com.alibaba.fastjson.JSON;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.RespEntity;
import com.pewee.util.resp.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice
@Slf4j
public class BaseController {
	
	@ExceptionHandler
	public RespEntity handException(HttpServletRequest request , Exception e){
		log.error(e.getMessage(),e);
		if (e instanceof ServiceException){
			return new RespEntity<>((ServiceException)e,null);
		} else if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
			return new RespEntity<String>().applyRespCodeMsg(CommonRespInfo.NOT_LEGAL_PARAM).applyData(e.getMessage());
		} else if (e instanceof org.springframework.validation.BindException){
			return new RespEntity<String>().applyRespCodeMsg(CommonRespInfo.NOT_LEGAL_PARAM.getCode(),((org.springframework.validation.BindException)e).getAllErrors().get(0).getDefaultMessage()).applyData(((org.springframework.validation.BindException)e).getAllErrors().get(0).getDefaultMessage());
		} else {
			return new RespEntity<>(CommonRespInfo.SYS_ERROR, JSON.toJSONString(e.getStackTrace()));
		}
	}
	
}
