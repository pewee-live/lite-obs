package com.pewee.adaptor.s3.util;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * AWS Signature Version 4 (SigV4) 验签与解析工具
 */
public class AwsSigV4Utils {

    private static final Logger log = LoggerFactory.getLogger(AwsSigV4Utils.class);

    public static final String SCHEME = "AWS4-HMAC-SHA256";
    public static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * 从请求头或查询参数中解析出 AccessKey (对应 OBS 中的 sysCode)
     */
    public static String extractAccessKey(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(authHeader) && authHeader.startsWith(SCHEME)) {
            // Header 格式: AWS4-HMAC-SHA256 Credential=dify/20261012/us-east-1/s3/aws4_request, SignedHeaders=...
            int credIdx = authHeader.indexOf("Credential=");
            if (credIdx != -1) {
                String credPart = authHeader.substring(credIdx + 11);
                int commaIdx = credPart.indexOf(",");
                String fullCred = commaIdx != -1 ? credPart.substring(0, commaIdx) : credPart;
                String[] parts = fullCred.trim().split("/");
                if (parts.length > 0) {
                    return parts[0];
                }
            }
        }

        // Query 参数格式 (Presigned URL)
        String credParam = request.getParameter("X-Amz-Credential");
        if (StringUtils.isNotBlank(credParam)) {
            String[] parts = credParam.split("/");
            if (parts.length > 0) {
                return parts[0];
            }
        }
        return null;
    }

    /**
     * 校验 AWS SigV4 签名
     */
    public static boolean verify(HttpServletRequest request, String secretKey) {
        if (StringUtils.isBlank(secretKey)) {
            return false;
        }

        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(authHeader) && authHeader.startsWith(SCHEME)) {
            return verifyHeaderAuth(request, authHeader, secretKey);
        }

        String sigParam = request.getParameter("X-Amz-Signature");
        if (StringUtils.isNotBlank(sigParam)) {
            return verifyQueryAuth(request, sigParam, secretKey);
        }

        return false;
    }

    private static boolean verifyHeaderAuth(HttpServletRequest request, String authHeader, String secretKey) {
        try {
            Map<String, String> authMap = parseAuthHeader(authHeader);
            String credential = authMap.get("Credential");
            String signedHeadersStr = authMap.get("SignedHeaders");
            String clientSignature = authMap.get("Signature");

            if (credential == null || signedHeadersStr == null || clientSignature == null) {
                return false;
            }

            String[] credParts = credential.split("/");
            if (credParts.length < 5) {
                return false;
            }
            String dateStamp = credParts[1];
            String region = credParts[2];
            String service = credParts[3];

            String amzDate = request.getHeader("x-amz-date");
            if (StringUtils.isBlank(amzDate)) {
                amzDate = request.getHeader("date");
            }
            if (StringUtils.isBlank(amzDate)) {
                return false;
            }

            // 1. 构建 Canonical Request
            String canonicalRequest = buildCanonicalRequest(request, signedHeadersStr, false);

            // 2. 构建 String to Sign
            String stringToSign = SCHEME + "\n" +
                    amzDate + "\n" +
                    dateStamp + "/" + region + "/" + service + "/aws4_request\n" +
                    Hex.encodeHexString(DigestUtils.sha256(canonicalRequest));

            // 3. 计算签名
            byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, service);
            String calculatedSignature = Hex.encodeHexString(hmacSha256(signingKey, stringToSign));

            boolean matched = calculatedSignature.equalsIgnoreCase(clientSignature);
            if (!matched) {
                log.warn("AWS SigV4 Header 签名校验不匹配! 期望: {}, 客户端: {}", calculatedSignature, clientSignature);
            }
            return matched;
        } catch (Exception e) {
            log.error("AWS SigV4 Header 验签异常", e);
            return false;
        }
    }

    private static boolean verifyQueryAuth(HttpServletRequest request, String clientSignature, String secretKey) {
        try {
            String credential = request.getParameter("X-Amz-Credential");
            String signedHeadersStr = request.getParameter("X-Amz-SignedHeaders");
            String amzDate = request.getParameter("X-Amz-Date");

            if (credential == null || signedHeadersStr == null || amzDate == null) {
                return false;
            }

            String[] credParts = credential.split("/");
            if (credParts.length < 5) {
                return false;
            }
            String dateStamp = credParts[1];
            String region = credParts[2];
            String service = credParts[3];

            // 1. 构建 Canonical Request
            String canonicalRequest = buildCanonicalRequest(request, signedHeadersStr, true);

            // 2. 构建 String to Sign
            String stringToSign = SCHEME + "\n" +
                    amzDate + "\n" +
                    dateStamp + "/" + region + "/" + service + "/aws4_request\n" +
                    Hex.encodeHexString(DigestUtils.sha256(canonicalRequest));

            // 3. 计算签名
            byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, service);
            String calculatedSignature = Hex.encodeHexString(hmacSha256(signingKey, stringToSign));

            boolean matched = calculatedSignature.equalsIgnoreCase(clientSignature);
            if (!matched) {
                log.warn("AWS SigV4 Query 签名校验不匹配! 期望: {}, 客户端: {}", calculatedSignature, clientSignature);
            }
            return matched;
        } catch (Exception e) {
            log.error("AWS SigV4 Query 验签异常", e);
            return false;
        }
    }

    private static String buildCanonicalRequest(HttpServletRequest request, String signedHeadersStr, boolean isQueryAuth) {
        StringBuilder sb = new StringBuilder();
        // 1. HTTP 方法
        sb.append(request.getMethod()).append("\n");

        // 2. Canonical URI
        String uri = request.getRequestURI();
        sb.append(normalizeUri(uri)).append("\n");

        // 3. Canonical Query String
        sb.append(buildCanonicalQueryString(request, isQueryAuth)).append("\n");

        // 4. Canonical Headers
        String[] signedHeaders = signedHeadersStr.split(";");
        Arrays.sort(signedHeaders);
        for (String header : signedHeaders) {
            String value = request.getHeader(header);
            if (value == null) {
                value = "";
            }
            sb.append(header.toLowerCase().trim()).append(":").append(value.trim().replaceAll("\\s+", " ")).append("\n");
        }
        sb.append("\n");

        // 5. Signed Headers
        sb.append(signedHeadersStr.toLowerCase()).append("\n");

        // 6. Payload Hash
        String payloadHash = request.getHeader("x-amz-content-sha256");
        if (StringUtils.isBlank(payloadHash)) {
            payloadHash = "UNSIGNED-PAYLOAD";
        }
        sb.append(payloadHash);

        return sb.toString();
    }

    private static String normalizeUri(String uri) {
        if (StringUtils.isBlank(uri)) {
            return "/";
        }
        return uri.replaceAll("/{2,}", "/");
    }

    private static String buildCanonicalQueryString(HttpServletRequest request, boolean isQueryAuth) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return "";
        }

        List<String> keys = new ArrayList<>(parameterMap.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (isQueryAuth && "X-Amz-Signature".equalsIgnoreCase(key)) {
                continue; // 验签时排除签名本身
            }
            String[] values = parameterMap.get(key);
            for (String val : values) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(urlEncode(key)).append("=").append(urlEncode(val));
            }
        }
        return sb.toString();
    }

    private static String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLEncoder.encode(value, "UTF-8")
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private static Map<String, String> parseAuthHeader(String authHeader) {
        Map<String, String> map = new HashMap<>();
        String afterScheme = authHeader.substring(SCHEME.length()).trim();
        String[] parts = afterScheme.split(",");
        for (String part : parts) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    private static byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kSecret = ("AWS4" + key).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmacSha256(kSecret, dateStamp);
        byte[] kRegion = hmacSha256(kDate, regionName);
        byte[] kService = hmacSha256(kRegion, serviceName);
        return hmacSha256(kService, "aws4_request");
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(key, HMAC_SHA256));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
}
