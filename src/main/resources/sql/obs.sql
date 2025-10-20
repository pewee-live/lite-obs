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
   storage_type         tinyint(2) not null default 0 comment '�洢����(0:LOCAL 1:NAS)',
   engine_namespace     varchar(32) not null default '' comment '���������ռ�(ĳЩ�Խ��Ķ�ڵ��ļ��洢������ʹ��,�������ֲ�ͬ�ڵ�)',
   length               bigint(50) not null default 0 comment '�ļ�����',
   batch_code           varchar(50) not null default '' comment '�ļ��� ��Ƭ�ļ�һ����Ϊһ����,��Ӧlogic��code',
   crc32                varchar(20) not null default '' comment 'crc32У����',
   sequence             int(5) not null default 0 comment '��Ƭʱ������ ��0��ʼ',
   token                varchar(50) not null default '' comment '�ļ�token ���ڴ��ļ��洢ȡ�ļ���ƾ֤',
   sys_code             varchar(50) not null default '' comment 'ϵͳ����',
   primary key (id)
);

alter table tbl_file comment '�ļ�';

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
   file_name            varchar(100) not null default '' comment '�ļ���',
   mime_type            varchar(100) not null default '' comment 'mime����',
   length               bigint(50) not null default 0 comment '����',
   split                tinyint(1) not null default 0 comment '�Ƿ���Ƭ (0:����Ƭ 1:��Ƭ)',
   total                int(5) not null default 1 comment '��Ƭ��',
   crc32                varchar(50) not null default '' comment 'У���',
   sys_code             varchar(50) not null default '' comment 'ϵͳ����',
   sys_alisa            varchar(50) not null default '' comment 'ϵͳ����',
   status               tinyint(1) not null default 0 comment '״̬ (0 : ��ʼ�� 1: �ϴ��� 2.�ϴ����  3:��ɾ��) ',
   expire               timestamp(3) not null default '0000-00-00 00:00:00' comment '����ʱ��',
   primary key (id)
);

alter table tbl_logic_file comment '�߼��ļ�';

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
   enabled              tinyint(1) not null default 0 comment '�Ƿ����(0:������ 1:����)',
   sys_name             varchar(50) not null default '' comment 'ϵͳ��',
   sys_alias            varchar(50) not null default '' comment 'ϵͳ����',
   secret               varchar(255) not null default '' comment 'ϵͳ��Կ',
   primary key (id)
);

alter table tbl_sys comment '����ϵͳע���';

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

