package com.pewee.adaptor.s3.interceptor;

import com.pewee.adaptor.s3.model.S3Error;
import com.pewee.adaptor.s3.util.AwsSigV4Utils;
import com.pewee.adaptor.s3.util.S3XmlUtil;
import com.pewee.bean.Sys;
import com.pewee.service.ISysService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.UUID;

/**
 * S3 协议专属 AWS SigV4 签名拦截器
 */
@Component
public class S3SigV4AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(S3SigV4AuthInterceptor.class);

    public static final String ATTR_SYS_CODE = "S3_SYS_CODE";
    public static final String ATTR_SYS_ALIAS = "S3_SYS_ALIAS";
    public static final String ATTR_SYS = "S3_SYS";

    @Autowired
    private ISysService sysService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        // 排除现有非 S3 路由
        if (uri.startsWith("/obs/") || uri.startsWith("/bdpan/")) {
            return true;
        }

        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String accessKey = AwsSigV4Utils.extractAccessKey(request);
        if (StringUtils.isBlank(accessKey)) {
            log.warn("S3 请求未携带凭证: URI={}", uri);
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "AccessDenied", "Access Denied", uri);
            return false;
        }

        Sys sys = null;
        try {
            sys = sysService.selectSysByCode(accessKey);
        } catch (Exception e) {
            log.error("S3 查询租户异常: accessKey=" + accessKey, e);
        }

        if (sys == null) {
            log.warn("S3 无效凭证: accessKey={}", accessKey);
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "InvalidAccessKeyId",
                    "The AWS Access Key Id you provided does not exist in our records.", uri);
            return false;
        }

        boolean verified = AwsSigV4Utils.verify(request, sys.getSecret());
        if (!verified) {
            log.warn("S3 签名验证失败: accessKey={}, URI={}", accessKey, uri);
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "SignatureDoesNotMatch",
                    "The request signature we calculated does not match the signature you provided. Check your key and signing method.", uri);
            return false;
        }

        request.setAttribute(ATTR_SYS_CODE, sys.getCode());
        request.setAttribute(ATTR_SYS_ALIAS, sys.getSysAlias());
        request.setAttribute(ATTR_SYS, sys);
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String code, String message, String resource) {
        response.setStatus(status);
        response.setContentType("application/xml; charset=utf-8");
        S3Error error = new S3Error(code, message, resource, UUID.randomUUID().toString());
        String xml = S3XmlUtil.toXml(error);
        try (PrintWriter writer = response.getWriter()) {
            writer.print(xml);
            writer.flush();
        } catch (Exception e) {
            log.error("写入S3错误响应失败", e);
        }
    }
}
