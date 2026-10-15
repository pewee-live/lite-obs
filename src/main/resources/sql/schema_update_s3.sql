-- ========================================================
-- OBS 微服务 S3 协议适配层数据库增量升级脚本
-- 执行环境: MySQL 5.7+ / 8.0+
-- ========================================================

USE `obs`;

-- 1. 扩容 tbl_logic_file.file_name 至 512，支持完整层级路径 S3 Key
ALTER TABLE `tbl_logic_file` 
  MODIFY COLUMN `file_name` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件名或完整S3对象Key';

-- 2. 新增 md5 字段，用于存放标准 S3 ETag (32位十六进制)
ALTER TABLE `tbl_logic_file` 
  ADD COLUMN `md5` VARCHAR(64) NULL DEFAULT NULL COMMENT '文件MD5校验和(S3 ETag)' AFTER `crc32`;

-- 3. 增加联合索引，加速 S3 基于租户与Key的高频检索与覆盖更新操作
ALTER TABLE `tbl_logic_file` 
  ADD INDEX `idx_sys_filename_status` (`sys_code`, `file_name`(255), `status`);

-- 4. 插入 Dify 专属租户注册数据（如果尚未注册过 dify）
-- 默认系统代号: dify，系统编码: dify，密钥: DifyObsSecretKey2026!
INSERT INTO `tbl_sys` (`code`, `secret`, `sys_name`, `sys_alias`, `create_time`, `update_time`)
SELECT 'dify', 'DifyObsSecretKey2026!', 'Dify智能体平台存储', 'dify', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `tbl_sys` WHERE `code` = 'dify');
