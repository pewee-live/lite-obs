package com.pewee.service.schedule;

import com.pewee.bean.LogicFile;
import com.pewee.service.ILogicFileService;
import com.pewee.util.GenerateCodeUtils;
import com.pewee.util.ILock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author GongRan
 */
@Service
@Slf4j
public class ScheduleService {

    @Autowired
    @Qualifier("redisLock")
    ILock redisLock;

    @Autowired
    ILogicFileService logicFileService;

    private static final String SETTING_STATUS_AND_DELETE_DELAY_LOCK = "com.pewee.obs:schedule:setting_status_delete_delay";

    @Scheduled(cron = "${corn.setting-status-delete-delay}")
    public void settingStatusAndDeleteDelayLogicFileForSplit() {
        log.info("定时任务-逻辑文件(分片上传类型)上传完成检查及延期文件删除 开始执行!");
        String reqId = GenerateCodeUtils.nextId();
        boolean getLock = false;
        try {
            if (redisLock.tryLock(SETTING_STATUS_AND_DELETE_DELAY_LOCK, reqId, 3,false)) {
                log.info("定时任务-逻辑文件(分片上传类型)上传完成检查及延期文件删除 竞争到分布式锁,可以进行后续操作!");
                getLock = true;
                final Integer SPLIT = 1;
                final Integer UPLOADED = 2;
                Example example = new Example(LogicFile.class);
                //分片且未完成
                example.createCriteria().andEqualTo("split", SPLIT).andNotEqualTo("status", UPLOADED);
                List<LogicFile> logicFiles = logicFileService.selectByExample(example);
                List<String> logicFileCodes = logicFiles.stream().map(LogicFile::getCode).distinct().collect(Collectors.toList());
                //进行状态设置的处理
                logicFileService.settingStatusToUploadedByCodes(logicFileCodes);
                log.info("定时任务-逻辑文件(分片上传类型)上传完成检查 执行成功!");
                //进行过期处理
                logicFileService.deleteDelayLogicFile(logicFileCodes);
                log.info("定时任务-逻辑文件(分片上传类型)延期文件删除 执行成功!");
            }
        } catch (Exception e) {
            log.error("定时任务-逻辑文件(分片上传类型)上传完成检查及延期文件删除 有失败条目!",e);
        } finally {
            redisLock.releaseLock(SETTING_STATUS_AND_DELETE_DELAY_LOCK, reqId, 5000);
        }
        if (getLock){
            log.info("定时任务-逻辑文件(分片上传类型)上传完成检查及延期文件删除 执行成功!");
            return;
        }
        log.info("定时任务-逻辑文件(分片上传类型)上传完成检查及延期文件删除 未竞争到分布式锁,退出!");
    }
}
