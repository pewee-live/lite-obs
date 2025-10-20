package com.pewee.bean;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 文件对象 tbl_file
 * 
 * @author pewee
 * @date 2022-04-13
 */
@Getter
@Setter
@ToString
@Table(name = "tbl_file")
public class File implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键  id */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY,generator = "SELECT LAST_INSERT_ID()")
    @Column(name = "id")
    private Long id;

	/** 唯一码  code */
	@Column(name = "code")
    private String code;

	/** 创建时间  create_time */
	@Column(name = "create_time")
    private Date createTime;

	/** 更新时间  update_time */
	@Column(name = "update_time")
    private Date updateTime;
	
	/** 存储类型 (0: nas)  storage_type */
	@Column(name = "storage_type")
	private Integer storageType;
	
	/** 引擎命名空间(某些自建的多节点文件存储类型下使用,用于区分不同节点)  engine_namespace*/
	@Column(name = "engine_namespace")
	private String engineNamespace;

	/** 文件长度  length */
	@Column(name = "length")
    private Long length;

	/** 文件码 切片文件一个批为一个码,对应logic的code  batch_code */
	@Column(name = "batch_code")
    private String batchCode;

	/** crc32校验码  crc32 */
	@Column(name = "crc32")
    private String crc32;

	/** 切片时的排序 从0开始  sequence */
	@Column(name = "sequence")
    private Integer sequence;

	/** 文件token 用于从文件存储取文件的凭证  token */
	@Column(name = "token")
    private String token;
	
	/** 系统编码  sys_code */
	@Column(name = "sys_code")
    private String sysCode;
	
}
