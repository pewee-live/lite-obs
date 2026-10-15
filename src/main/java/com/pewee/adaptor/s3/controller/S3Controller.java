package com.pewee.adaptor.s3.controller;

import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.PureJavaCrc32;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.pewee.adaptor.s3.interceptor.S3SigV4AuthInterceptor;
import com.pewee.adaptor.s3.model.CompleteMultipartUpload;
import com.pewee.adaptor.s3.model.CompleteMultipartUploadResult;
import com.pewee.adaptor.s3.model.InitiateMultipartUploadResult;
import com.pewee.adaptor.s3.model.ListAllMyBucketsResult;
import com.pewee.adaptor.s3.model.ListBucketResult;
import com.pewee.adaptor.s3.model.S3Error;
import com.pewee.adaptor.s3.util.S3XmlUtil;
import com.pewee.bean.File;
import com.pewee.bean.LogicFile;
import com.pewee.bean.SingleUploadFileContext;
import com.pewee.bean.Sys;
import com.pewee.engine.EngineRegistor;
import com.pewee.mapper.FileMapper;
import com.pewee.mapper.LogicFileMapper;
import com.pewee.service.IFileService;
import com.pewee.service.ILogicFileService;
import com.pewee.service.ISysService;
import com.pewee.util.GenerateCodeUtils;

import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.params.SetParams;

/**
 * 标准 AWS S3 协议控制器
 * 挂载在根路径，支持 Dify/boto3 直接使用 http://<ip>:<port> 接入
 */
@RestController
public class S3Controller {

    private static final Logger log = LoggerFactory.getLogger(S3Controller.class);

    private static final String S3_MULTIPART_KEY = "com.pewee.obs:s3:multipart:%s";
    private static final String S3_MULTIPART_PARTS_KEY = "com.pewee.obs:s3:multipart:parts:%s";

    @Autowired
    private ILogicFileService logicFileService;

    @Autowired
    private IFileService fileService;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private LogicFileMapper logicFileMapper;

    @Autowired
    private ISysService sysService;

    @Autowired
    private EngineRegistor engineRegistor;

    @Autowired
    private JedisCluster jedisCluster;

    /**
     * S3 ListAllMyBuckets (GET /)
     */
    @GetMapping(value = "/", produces = "application/xml;charset=UTF-8")
    public String listBuckets(HttpServletRequest request) {
        String sysCode = getCallerSysCode(request);
        ListAllMyBucketsResult result = new ListAllMyBucketsResult(sysCode, sysCode);
        String nowIso = DateTime.now().toString(ISODateTimeFormat.dateTimeNoMillis());
        result.getBuckets().add(new ListAllMyBucketsResult.BucketEntry(sysCode, nowIso));
        return S3XmlUtil.toXml(result);
    }

    /**
     * S3 HeadBucket (HEAD /{bucket})
     */
    @RequestMapping(value = "/{bucket:[a-zA-Z0-9._-]+}", method = RequestMethod.HEAD)
    public void headBucket(@PathVariable("bucket") String bucket, HttpServletResponse response) {
        if ("opto".equalsIgnoreCase(bucket) || "bdpan".equalsIgnoreCase(bucket)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Sys sys = sysService.selectSysByCode(bucket);
        if (sys != null) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * S3 CreateBucket (PUT /{bucket})
     */
    @PutMapping(value = "/{bucket:[a-zA-Z0-9._-]+}")
    public void createBucket(@PathVariable("bucket") String bucket, HttpServletResponse response) {
        if ("opto".equalsIgnoreCase(bucket) || "bdpan".equalsIgnoreCase(bucket)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        // 校验或幂等返回
        response.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * S3 ListObjects / ListObjectsV2 (GET /{bucket})
     */
    @GetMapping(value = "/{bucket:[a-zA-Z0-9._-]+}", produces = "application/xml;charset=UTF-8")
    public String listObjects(
            @PathVariable("bucket") String bucket,
            @RequestParam(name = "prefix", required = false, defaultValue = "") String prefix,
            @RequestParam(name = "delimiter", required = false) String delimiter,
            @RequestParam(name = "max-keys", defaultValue = "1000") int maxKeys,
            @RequestParam(name = "marker", required = false) String marker,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if ("opto".equalsIgnoreCase(bucket) || "bdpan".equalsIgnoreCase(bucket)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        String sysCode = getCallerSysCode(request);
        if (StringUtils.isBlank(sysCode)) {
            sysCode = bucket;
        }

        List<LogicFile> logicFiles = logicFileService.selectBySysCodeAndPrefix(sysCode, prefix);
        ListBucketResult result = new ListBucketResult();
        result.setName(bucket);
        result.setPrefix(prefix);
        result.setDelimiter(delimiter);
        result.setMaxKeys(maxKeys);
        result.setMarker(marker);

        Set<String> commonPrefixSet = new HashSet<>();
        List<ListBucketResult.ObjectEntry> contents = new ArrayList<>();

        for (LogicFile lf : logicFiles) {
            String key = lf.getFileName();
            if (StringUtils.isNotBlank(delimiter) && key.length() > prefix.length()) {
                String sub = key.substring(prefix.length());
                int delimIdx = sub.indexOf(delimiter);
                if (delimIdx != -1) {
                    commonPrefixSet.add(prefix + sub.substring(0, delimIdx + 1));
                    continue;
                }
            }

            ListBucketResult.ObjectEntry entry = new ListBucketResult.ObjectEntry();
            entry.setKey(key);
            entry.setSize(lf.getLength() != null ? lf.getLength() : 0L);
            String etagVal = StringUtils.isNotBlank(lf.getMd5()) ? lf.getMd5() : lf.getCrc32();
            entry.setEtag(formatEtag(etagVal));
            Date modTime = lf.getUpdateTime() != null ? lf.getUpdateTime() : lf.getCreateTime();
            entry.setLastModified(modTime != null ? new DateTime(modTime).toString(ISODateTimeFormat.dateTimeNoMillis()) : "");
            contents.add(entry);
            if (contents.size() >= maxKeys) {
                result.setTruncated(true);
                break;
            }
        }

        result.setContents(contents);
        for (String cp : commonPrefixSet) {
            result.getCommonPrefixes().add(new ListBucketResult.CommonPrefix(cp));
        }

        return S3XmlUtil.toXml(result);
    }

    /**
     * S3 HeadObject (HEAD /{bucket}/**)
     */
    @RequestMapping(value = "/{bucket:[a-zA-Z0-9._-]+}/**", method = RequestMethod.HEAD)
    public void headObject(@PathVariable("bucket") String bucket, HttpServletRequest request, HttpServletResponse response) {
        if ("opto".equalsIgnoreCase(bucket) || "bdpan".equalsIgnoreCase(bucket)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String sysCode = getCallerSysCode(request);
        if (StringUtils.isBlank(sysCode)) {
            sysCode = bucket;
        }
        String key = extractObjectKey(request, bucket);

        LogicFile logicFile = logicFileService.selectBySysCodeAndFileName(sysCode, key);
        if (logicFile == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String etag = StringUtils.isNotBlank(logicFile.getMd5()) ? logicFile.getMd5() : logicFile.getCrc32();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("ETag", formatEtag(etag));
        response.setHeader("Content-Length", String.valueOf(logicFile.getLength()));
        response.setHeader("Content-Type", StringUtils.isNotBlank(logicFile.getMimeType()) ? logicFile.getMimeType() : "application/octet-stream");
        response.setHeader("Accept-Ranges", "bytes");
        Date modTime = logicFile.getUpdateTime() != null ? logicFile.getUpdateTime() : logicFile.getCreateTime();
        if (modTime != null) {
            response.setHeader("Last-Modified", new DateTime(modTime).toString(ISODateTimeFormat.dateTimeNoMillis()));
        }
    }

    /**
     * S3 GetObject (GET /{bucket}/**) 支持 Range 字节范围精准截取
     */
    @GetMapping(value = "/{bucket:[a-zA-Z0-9._-]+}/**")
    public void getObject(@PathVariable("bucket") String bucket, HttpServletRequest request, HttpServletResponse response) {
        if ("opto".equalsIgnoreCase(bucket) || "bdpan".equalsIgnoreCase(bucket)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String sysCode = getCallerSysCode(request);
        if (StringUtils.isBlank(sysCode)) {
            sysCode = bucket;
        }
        String key = extractObjectKey(request, bucket);

        LogicFile logicFile = logicFileService.selectBySysCodeAndFileName(sysCode, key);
        if (logicFile == null) {
            writeS3Error(response, HttpServletResponse.SC_NOT_FOUND, "NoSuchKey", "The specified key does not exist.", request.getRequestURI());
            return;
        }

        String etag = StringUtils.isNotBlank(logicFile.getMd5()) ? logicFile.getMd5() : logicFile.getCrc32();
        long fileLength = logicFile.getLength() != null ? logicFile.getLength() : 0L;
        String contentType = StringUtils.isNotBlank(logicFile.getMimeType()) ? logicFile.getMimeType() : "application/octet-stream";

        response.setHeader("ETag", formatEtag(etag));
        response.setHeader("Content-Type", contentType);
        response.setHeader("Accept-Ranges", "bytes");

        String rangeHeader = request.getHeader("Range");
        try {
            if (StringUtils.isNotBlank(rangeHeader) && rangeHeader.startsWith("bytes=")) {
                // HTTP Range 分段读取
                long[] range = parseRange(rangeHeader.substring(6).trim(), fileLength);
                long start = range[0];
                long end = range[1];
                long contentLength = end - start + 1;

                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", String.format("bytes %d-%d/%d", start, end, fileLength));
                response.setHeader("Content-Length", String.valueOf(contentLength));

                logicFileService.writeRangeStream(logicFile, start, end, response.getOutputStream());
            } else {
                // 全量读取
                response.setStatus(HttpServletResponse.SC_OK);
                response.setHeader("Content-Length", String.valueOf(fileLength));
                if (fileLength > 0) {
                    logicFileService.writeRangeStream(logicFile, 0, fileLength - 1, response.getOutputStream());
                }
            }
        } catch (Exception e) {
            log.error("S3 GetObject 读取流失败 key=" + key, e);
            writeS3Error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "InternalError", e.getMessage(), request.getRequestURI());
        }
    }

    /**
     * S3 PutObject (PUT /{bucket}/**) 或 UploadPart (带有 uploadId & partNumber)
     */
    @PutMapping(value = "/{bucket:[a-zA-Z0-9._-]+}/**")
    public void putObject(
            @PathVariable("bucket") String bucket,
            @RequestParam(name = "uploadId", required = false) String uploadId,
            @RequestParam(name = "partNumber", required = false) Integer partNumber,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if ("opto".equalsIgnoreCase(bucket) || "bdpan".equalsIgnoreCase(bucket)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String sysCode = getCallerSysCode(request);
        if (StringUtils.isBlank(sysCode)) {
            sysCode = bucket;
        }
        String key = extractObjectKey(request, bucket);

        try {
            if (StringUtils.isNotBlank(uploadId) && partNumber != null) {
                // S3 分片上传阶段: UploadPart
                handleUploadPart(sysCode, bucket, key, uploadId, partNumber, request, response);
            } else {
                // S3 单文件流式直传: PutObject
                handlePutObject(sysCode, bucket, key, request, response);
            }
        } catch (Exception e) {
            log.error("S3 PutObject 异常: key=" + key, e);
            writeS3Error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "InternalError", e.getMessage(), request.getRequestURI());
        }
    }

    /**
     * S3 PostObject:
     * - InitiateMultipartUpload (参数含 uploads)
     * - CompleteMultipartUpload (参数含 uploadId)
     */
    @PostMapping(value = "/{bucket:[a-zA-Z0-9._-]+}/**")
    public void postObject(
            @PathVariable("bucket") String bucket,
            @RequestParam(name = "uploads", required = false) String uploads,
            @RequestParam(name = "uploadId", required = false) String uploadId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if ("opto".equalsIgnoreCase(bucket) || "bdpan".equalsIgnoreCase(bucket)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String sysCode = getCallerSysCode(request);
        if (StringUtils.isBlank(sysCode)) {
            sysCode = bucket;
        }
        String key = extractObjectKey(request, bucket);

        try {
            if (request.getParameterMap().containsKey("uploads") || uploads != null) {
                // 1. 初始化分片上传
                handleInitiateMultipart(sysCode, bucket, key, response);
            } else if (StringUtils.isNotBlank(uploadId)) {
                // 2. 完成分片合并
                handleCompleteMultipart(sysCode, bucket, key, uploadId, request, response);
            } else {
                writeS3Error(response, HttpServletResponse.SC_BAD_REQUEST, "InvalidRequest", "Missing uploads or uploadId", request.getRequestURI());
            }
        } catch (Exception e) {
            log.error("S3 PostObject 异常: key=" + key, e);
            writeS3Error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "InternalError", e.getMessage(), request.getRequestURI());
        }
    }

    /**
     * S3 DeleteObject 或 AbortMultipartUpload
     */
    @DeleteMapping(value = "/{bucket:[a-zA-Z0-9._-]+}/**")
    public void deleteObject(
            @PathVariable("bucket") String bucket,
            @RequestParam(name = "uploadId", required = false) String uploadId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if ("opto".equalsIgnoreCase(bucket) || "bdpan".equalsIgnoreCase(bucket)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String sysCode = getCallerSysCode(request);
        if (StringUtils.isBlank(sysCode)) {
            sysCode = bucket;
        }
        String key = extractObjectKey(request, bucket);

        try {
            if (StringUtils.isNotBlank(uploadId)) {
                // 取消分片上传
                handleAbortMultipart(uploadId, response);
            } else {
                // 删除普通对象
                LogicFile logicFile = logicFileService.selectBySysCodeAndFileName(sysCode, key);
                if (logicFile != null) {
                    logicFileService.deleteLogicFileByCodes(Lists.newArrayList(logicFile.getCode()));
                }
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } catch (Exception e) {
            log.error("S3 DeleteObject 异常: key=" + key, e);
            writeS3Error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "InternalError", e.getMessage(), request.getRequestURI());
        }
    }

    // =========================================================================
    // 内部实现辅助方法
    // =========================================================================

    private void handlePutObject(String sysCode, String bucket, String key, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LogicFile logicFile = new LogicFile();
        logicFile.setSysCode(sysCode);
        logicFile.setSysAlisa(getCallerSysAlias(request, bucket));

        String contentType = request.getContentType();
        long contentLength = request.getContentLengthLong();

        String code = logicFileService.uploadStream(logicFile, request.getInputStream(), contentLength, contentType, key);
        LogicFile saved = logicFileService.selectLogicFileByCode(code);
        String etag = saved != null && StringUtils.isNotBlank(saved.getMd5()) ? saved.getMd5() : saved != null ? saved.getCrc32() : code;

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("ETag", formatEtag(etag));
    }

    private void handleInitiateMultipart(String sysCode, String bucket, String key, HttpServletResponse response) throws Exception {
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        JSONObject meta = new JSONObject();
        meta.put("bucket", bucket);
        meta.put("key", key);
        meta.put("sysCode", sysCode);
        meta.put("sysAlias", bucket);
        meta.put("createTime", System.currentTimeMillis());

        // 缓存 uploadId 元数据，租期默认 7 天
        jedisCluster.set(String.format(S3_MULTIPART_KEY, uploadId), meta.toJSONString(), SetParams.setParams().ex(7 * 24 * 3600));

        InitiateMultipartUploadResult result = new InitiateMultipartUploadResult(bucket, key, uploadId);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/xml; charset=utf-8");
        response.getWriter().write(S3XmlUtil.toXml(result));
    }

    private void handleUploadPart(String sysCode, String bucket, String key, String uploadId, int partNumber, HttpServletRequest request, HttpServletResponse response) throws Exception {
        byte[] bytes = IOUtils.toByteArray(request.getInputStream());
        String partEtag = DigestUtils.md5Hex(bytes);

        PureJavaCrc32 pureJavaCrc32 = new PureJavaCrc32();
        pureJavaCrc32.update(bytes, 0, bytes.length);
        String partCrc = Long.toHexString(pureJavaCrc32.getValue());

        SingleUploadFileContext fileContext = new SingleUploadFileContext();
        fileContext.setSysCode(sysCode);
        fileContext.setBatchCode(uploadId);
        fileContext.setSeq(partNumber - 1);
        fileContext.setFileName(key + ".part" + partNumber);
        fileContext.setLength((long) bytes.length);
        fileContext.setCrc32(partCrc);
        fileContext.setSplit(1);

        String fileCode = fileService.upload(engineRegistor.switchTo(engineRegistor.getDefaultType()), fileContext, bytes);

        // 记录分片信息至 Redis
        JSONObject partInfo = new JSONObject();
        partInfo.put("partNumber", partNumber);
        partInfo.put("etag", partEtag);
        partInfo.put("fileCode", fileCode);
        partInfo.put("length", bytes.length);
        partInfo.put("crc32", partCrc);

        String partsKey = String.format(S3_MULTIPART_PARTS_KEY, uploadId);
        jedisCluster.hset(partsKey, String.valueOf(partNumber), partInfo.toJSONString());
        jedisCluster.expire(partsKey, 7 * 24 * 3600);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("ETag", formatEtag(partEtag));
    }

    private void handleCompleteMultipart(String sysCode, String bucket, String key, String uploadId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String metaStr = jedisCluster.get(String.format(S3_MULTIPART_KEY, uploadId));
        if (StringUtils.isBlank(metaStr)) {
            writeS3Error(response, HttpServletResponse.SC_NOT_FOUND, "NoSuchUpload", "The specified multipart upload does not exist.", request.getRequestURI());
            return;
        }

        // 解析客户端提交的 CompleteMultipartUpload XML
        CompleteMultipartUpload clientUpload = S3XmlUtil.fromXml(request.getInputStream(), CompleteMultipartUpload.class);
        List<CompleteMultipartUpload.Part> clientParts = clientUpload != null ? clientUpload.getParts() : Collections.emptyList();
        clientParts.sort(Comparator.comparingInt(CompleteMultipartUpload.Part::getPartNumber));

        // 覆盖写检查：清理旧文件
        LogicFile oldFile = logicFileService.selectBySysCodeAndFileName(sysCode, key);
        if (oldFile != null) {
            log.info("S3分片合并-清理旧版本文件: code={}, key={}", oldFile.getCode(), key);
            logicFileService.deleteLogicFileByCodes(Lists.newArrayList(oldFile.getCode()));
        }

        // 计算所有切片的总长与合成 ETag
        String partsKey = String.format(S3_MULTIPART_PARTS_KEY, uploadId);
        Map<String, String> partMap = jedisCluster.hgetAll(partsKey);

        long totalSize = 0L;
        StringBuilder etagConcat = new StringBuilder();
        for (CompleteMultipartUpload.Part cp : clientParts) {
            String pJson = partMap.get(String.valueOf(cp.getPartNumber()));
            if (pJson != null) {
                JSONObject obj = JSON.parseObject(pJson);
                totalSize += obj.getLongValue("length");
                etagConcat.append(obj.getString("etag"));
            }
        }

        String combinedEtag = DigestUtils.md5Hex(etagConcat.toString()) + "-" + clientParts.size();

        // 创建最终 LogicFile 记录
        LogicFile logicFile = new LogicFile();
        logicFile.setCode(GenerateCodeUtils.nextId());
        logicFile.setFileName(key);
        logicFile.setSysCode(sysCode);
        logicFile.setSysAlisa(getCallerSysAlias(request, bucket));
        logicFile.setLength(totalSize);
        logicFile.setMimeType("application/octet-stream");
        logicFile.setSplit(1);
        logicFile.setTotal(clientParts.size());
        logicFile.setStatus(2);
        logicFile.setMd5(combinedEtag);
        logicFile.setCreateTime(new Date());
        logicFile.setExpire(DateTime.now().plusYears(10).toDate());
        logicFileMapper.insertSelective(logicFile);

        // 将之前切片的 batch_code (原为 uploadId) 原子迁移到 logicFile.code
        fileMapper.updateBatchCode(uploadId, logicFile.getCode());

        // 清理缓存
        jedisCluster.del(String.format(S3_MULTIPART_KEY, uploadId));
        jedisCluster.del(partsKey);

        CompleteMultipartUploadResult result = new CompleteMultipartUploadResult(
                request.getRequestURL().toString(), bucket, key, formatEtag(combinedEtag));

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/xml; charset=utf-8");
        response.getWriter().write(S3XmlUtil.toXml(result));
    }

    private void handleAbortMultipart(String uploadId, HttpServletResponse response) {
        List<File> files = fileService.selectFilesByBatchCode(uploadId);
        if (files != null && !files.isEmpty()) {
            engineRegistor.getDefault().deleteFiles(files);
            Long[] ids = files.stream().map(File::getId).toArray(Long[]::new);
            fileService.deleteFileByIds(ids);
        }
        jedisCluster.del(String.format(S3_MULTIPART_KEY, uploadId));
        jedisCluster.del(String.format(S3_MULTIPART_PARTS_KEY, uploadId));
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private String extractObjectKey(HttpServletRequest request, String bucket) {
        String uri = request.getRequestURI();
        String prefix = "/" + bucket + "/";
        int idx = uri.indexOf(prefix);
        String rawKey;
        if (idx != -1) {
            rawKey = uri.substring(idx + prefix.length());
        } else {
            rawKey = uri.replaceFirst("^/+", "");
        }
        try {
            return URLDecoder.decode(rawKey, "UTF-8");
        } catch (Exception e) {
            return rawKey;
        }
    }

    private String getCallerSysCode(HttpServletRequest request) {
        Object attr = request.getAttribute(S3SigV4AuthInterceptor.ATTR_SYS_CODE);
        return attr != null ? attr.toString() : null;
    }

    private String getCallerSysAlias(HttpServletRequest request, String defaultAlias) {
        Object attr = request.getAttribute(S3SigV4AuthInterceptor.ATTR_SYS_ALIAS);
        return attr != null ? attr.toString() : defaultAlias;
    }

    private String formatEtag(String etag) {
        if (StringUtils.isBlank(etag)) {
            return "\"\"";
        }
        if (etag.startsWith("\"") && etag.endsWith("\"")) {
            return etag;
        }
        return "\"" + etag + "\"";
    }

    private long[] parseRange(String rangeStr, long totalLength) {
        long start = 0;
        long end = totalLength - 1;
        String[] parts = rangeStr.split("-", 2);
        if (parts.length > 0 && StringUtils.isNotBlank(parts[0])) {
            start = Long.parseLong(parts[0].trim());
        }
        if (parts.length > 1 && StringUtils.isNotBlank(parts[1])) {
            end = Long.parseLong(parts[1].trim());
        } else if (parts.length == 1 || StringUtils.isBlank(parts[1])) {
            end = totalLength - 1;
        }
        if (start < 0) {
            start = 0;
        }
        if (end >= totalLength) {
            end = totalLength - 1;
        }
        return new long[]{start, end};
    }

    private void writeS3Error(HttpServletResponse response, int status, String code, String message, String resource) {
        response.setStatus(status);
        response.setContentType("application/xml; charset=utf-8");
        S3Error error = new S3Error(code, message, resource, UUID.randomUUID().toString());
        String xml = S3XmlUtil.toXml(error);
        try {
            response.getOutputStream().write(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            response.getOutputStream().flush();
        } catch (IllegalStateException e) {
            try (PrintWriter writer = response.getWriter()) {
                writer.write(xml);
                writer.flush();
            } catch (Exception ex) {
                log.error("写入 S3 错误 XML 异常", ex);
            }
        } catch (Exception e) {
            log.error("写入 S3 错误 XML 异常", e);
        }
    }
}
