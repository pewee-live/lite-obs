package com.pewee.interceptor;

import com.alibaba.fastjson.JSON;
import com.pewee.bean.Sys;
import com.pewee.service.ISysService;
import com.pewee.service.impl.TokenService;
import com.pewee.util.IpUtil;
import com.pewee.util.resp.CommonRespInfo;
import com.pewee.util.resp.IResponse;
import com.pewee.util.resp.RespEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.RequestFacade;
import org.apache.tomcat.util.http.MimeHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @Classname ObsAccessInterceptor
 * @Description TODO
 * @Version 1.0.0
 * @Date 2022/4/20 8:46
 * @Created by Mr.GongRan
 */
@Component
@Slf4j
public class ObsAccessInterceptor implements HandlerInterceptor {
    @Autowired
    private ISysService sysService;

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取access_token
        String accessToken = request.getHeader("Authorization");
        String sysCode = null;
        boolean notEmpty = StringUtils.hasText(accessToken);
        if (!notEmpty) {
            //当前token为空
        	log.info("OBS-loginhandler - 校验token失败!!原因:{},调用方信息:{}","accessToken为空!!",IpUtil.getRequestIp());
            falseReturn(response, new RespEntity<>(CommonRespInfo.SYS_CODE_TOKEN_EMPTY, null));
            return false;
        }
        try {
            String[] split = accessToken.split(tokenService.TONEN_PREFIX);
            String encodeStr = split[1].split("\\.")[1];
            byte[] decode = Base64.getDecoder().decode(encodeStr);
            String json = new String(decode);
            sysCode = (String) JSON.parseObject(json).get("sysCode");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            falseReturn(response, new RespEntity<>(CommonRespInfo.SYS_CODE_TOKEN_INVALID, null));
            return false;
        }
        notEmpty = StringUtils.hasText(sysCode);
        if (!notEmpty) {
            //当前token为空
        	log.info("OBS-loginhandler - 校验token失败!!原因:{},调用方信息:{}","sysCode为空!!",IpUtil.getRequestIp());
            falseReturn(response, new RespEntity<>(CommonRespInfo.SYS_CODE_TOKEN_EMPTY, null));
            return false;
        }
        Sys sys = null;
        try {
            sys = sysService.selectSysByCode(sysCode);
        } catch (Exception e) {
            log.error("ObsAccessInterceptor 通过sysCode查询注册过的系统错误 ", e);
            falseReturn(response, new RespEntity<>(CommonRespInfo.SYS_CODE_ERROR, null));
            return false;
        }
        if (sys == null) {
        	log.info("OBS-loginhandler - 校验token失败!!原因:{},调用方信息:{}","sysCode:" + sysCode + "对应Sys为空!!",IpUtil.getRequestIp());
            falseReturn(response, new RespEntity<>(CommonRespInfo.SYS_CODE_ERROR, null));
            return false;
        }
        //校验token
        boolean flag = tokenService.checkToken(sys, accessToken);
        if (!flag) {
            falseReturn(response, new RespEntity<>(CommonRespInfo.SYS_CODE_TOKEN_INVALID, null));
            return false;
        }
        Map<String, String> unprocessed = new HashMap<>();
        unprocessed.put("sysAlias", sys.getSysAlias());
        unprocessed.put("sysCode", sysCode);
        if (request instanceof StandardMultipartHttpServletRequest) {
            doHandleMultipartRequestHeader(request, unprocessed);
        } else if (request instanceof RequestFacade) {
            doHandleDefaultRequestHeader(request, unprocessed);
        }
        return true;
    }

    private void doHandleMultipartRequestHeader(HttpServletRequest request, Map<String, String> unprocessed) throws NoSuchFieldException, IllegalAccessException {
        Class<ServletRequestWrapper> clazz = ServletRequestWrapper.class;
        StandardMultipartHttpServletRequest standardMultipartHttpServletRequest = (StandardMultipartHttpServletRequest) request;
        Field requestFacadeField = clazz.getDeclaredField("request");
        requestFacadeField.setAccessible(true);
        RequestFacade requestFacade = (RequestFacade) requestFacadeField.get(standardMultipartHttpServletRequest);
        Field connectorRequestField = RequestFacade.class.getDeclaredField("request");
        connectorRequestField.setAccessible(true);
        org.apache.catalina.connector.Request connectorRequest = (org.apache.catalina.connector.Request) connectorRequestField.get(requestFacade);
        org.apache.coyote.Request coyoteRequest = connectorRequest.getCoyoteRequest();
        MimeHeaders mimeHeaders = coyoteRequest.getMimeHeaders();
        for (Map.Entry<String, String> entry : unprocessed.entrySet()) {
            mimeHeaders.addValue(entry.getKey()).setString(entry.getValue());
        }
    }

    private void doHandleDefaultRequestHeader(HttpServletRequest request, Map<String, String> unprocessed) throws NoSuchFieldException, IllegalAccessException {
        RequestFacade requestFacade = (RequestFacade) request;
        Class<RequestFacade> clazz = RequestFacade.class;
        Field connectorRequestField = clazz.getDeclaredField("request");
        connectorRequestField.setAccessible(true);
        org.apache.catalina.connector.Request connectorRequest = (org.apache.catalina.connector.Request) connectorRequestField.get(requestFacade);
        org.apache.coyote.Request coyoteRequest = connectorRequest.getCoyoteRequest();
        MimeHeaders mimeHeaders = coyoteRequest.getMimeHeaders();
        for (Map.Entry<String, String> entry : unprocessed.entrySet()) {
            mimeHeaders.addValue(entry.getKey()).setString(entry.getValue());
        }
    }


    private void falseReturn(HttpServletResponse response, IResponse json) {
        PrintWriter pw = null;
        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json; charset=utf-8");
            pw = response.getWriter();
            pw.print(json);
        } catch (IOException e) {
            log.error("ObsAccessInterceptor  method:falseReturn error ", e);
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }
}
