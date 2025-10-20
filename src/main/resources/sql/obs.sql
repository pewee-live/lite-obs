/*==============================================================*/
/* DBMS name:      MySQL 5.0                                    */
/* Created on:     2025/10/20 13:11:33                          */
/*==============================================================*/


drop index idx_batchCode on tbl_file;

drop index idx_createTime on tbl_file;

drop index idx_code on tbl_file;

drop table if exists tbl_file;

drop index idx_sys_alias on tbl_logic_file;

drop index idx_sys_code on tbl_logic_file;

drop index idx_expireTime on tbl_logic_file;

drop index idx_createTime on tbl_logic_file;

drop index idx_code on tbl_logic_file;

drop table if exists tbl_logic_file;

drop index idx_sys_alias on tbl_sys;

drop index unidx_code on tbl_sys;

drop table if exists tbl_sys;

/*==============================================================*/
/* Table: tbl_file                                              */
/*==============================================================*/
create table tbl_file
(
   id                   bigint not null auto_increment,
   code                 varchar(50) not null default '',
   create_time          timestamp(3) not null default CURRENT_TIMESTAMP,
   update_time          timestamp not null default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   storage_type         tinyint(2) not null default 0 comment '存储类型(0:LOCAL 1:NAS)',
   engine_namespace     varchar(32) not null default '' comment '引擎命名空间(某些自建的多节点文件存储类型下使用,用于区分不同节点)',
   length               bigint(50) not null default 0 comment '文件长度',
   batch_code           varchar(50) not null default '' comment '文件码 切片文件一个批为一个码,对应logic的code',
   crc32                varchar(20) not null default '' comment 'crc32校验码',
   sequence             int(5) not null default 0 comment '切片时的排序 从0开始',
   token                varchar(50) not null default '' comment '文件token 用于从文件存储取文件的凭证',
   sys_code             varchar(50) not null default '' comment '系统编码',
   primary key (id)
);

alter table tbl_file comment '文件';

/*==============================================================*/
/* Index: idx_code                                              */
/*==============================================================*/
create unique index idx_code on tbl_file
(
   code
);

/*==============================================================*/
/* Index: idx_createTime                                        */
/*==============================================================*/
create index idx_createTime on tbl_file
(
   create_time
);

/*==============================================================*/
/* Index: idx_batchCode                                         */
/*==============================================================*/
create index idx_batchCode on tbl_file
(
   batch_code
);

/*==============================================================*/
/* Table: tbl_logic_file                                        */
/*==============================================================*/
create table tbl_logic_file
(
   id                   bigint not null auto_increment,
   code                 varchar(50) not null default '',
   create_time          timestamp(3) not null default CURRENT_TIMESTAMP,
   update_time          timestamp not null default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   file_name            varchar(100) not null default '' comment '文件名',
   mime_type            varchar(100) not null default '' comment 'mime类型',
   length               bigint(50) not null default 0 comment '长度',
   split                tinyint(1) not null default 0 comment '是否切片 (0:不切片 1:切片)',
   total                int(5) not null default 1 comment '分片数',
   crc32                varchar(50) not null default '' comment '校验和',
   sys_code             varchar(50) not null default '' comment '系统编码',
   sys_alisa            varchar(50) not null default '' comment '系统代号',
   status               tinyint(1) not null default 0 comment '状态 (0 : 初始化 1: 上传中 2.上传完成  3:已删除) ',
   expire               timestamp(3) not null default '0000-00-00 00:00:00' comment '过期时间',
   primary key (id)
);

alter table tbl_logic_file comment '逻辑文件';

/*==============================================================*/
/* Index: idx_code                                              */
/*==============================================================*/
create unique index idx_code on tbl_logic_file
(
   code
);

/*==============================================================*/
/* Index: idx_createTime                                        */
/*==============================================================*/
create index idx_createTime on tbl_logic_file
(
   create_time
);

/*==============================================================*/
/* Index: idx_expireTime                                        */
/*==============================================================*/
create index idx_expireTime on tbl_logic_file
(
   expire
);

/*==============================================================*/
/* Index: idx_sys_code                                          */
/*==============================================================*/
create index idx_sys_code on tbl_logic_file
(
   sys_code
);

/*==============================================================*/
/* Index: idx_sys_alias                                         */
/*==============================================================*/
create index idx_sys_alias on tbl_logic_file
(
   sys_alisa
);

/*==============================================================*/
/* Table: tbl_sys                                               */
/*==============================================================*/
create table tbl_sys
(
   id                   bigint not null auto_increment,
   code                 varchar(50) not null default '',
   create_time          timestamp(3) not null default CURRENT_TIMESTAMP,
   update_time          timestamp not null default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   enabled              tinyint(1) not null default 0 comment '是否可用(0:不可用 1:可用)',
   sys_name             varchar(50) not null default '' comment '系统名',
   sys_alias            varchar(50) not null default '' comment '系统代号',
   secret               varchar(255) not null default '' comment '系统密钥',
   primary key (id)
);

alter table tbl_sys comment '接入系统注册表';

/*==============================================================*/
/* Index: unidx_code                                            */
/*==============================================================*/
create unique index unidx_code on tbl_sys
(
   code
);

/*==============================================================*/
/* Index: idx_sys_alias                                         */
/*==============================================================*/
create unique index idx_sys_alias on tbl_sys
(
   sys_alias
);

