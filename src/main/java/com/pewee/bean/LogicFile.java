package com.pewee.bean;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 逻辑文件对象 tbl_logic_file
 * 
 * @author pewee
 * @date 2022-04-13
 */
@Getter
@Setter
@ToString
@Table(name = "tbl_logic_file")
public class LogicFile implements Serializable {
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
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

	/** 更新时间  update_time */
	@Column(name = "update_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

	/** 文件名  file_name */
	@Column(name = "file_name")
    private String fileName;

	/** mime类型  mime_type */
	@Column(name = "mime_type")
    private String mimeType;

	/** 长度  length */
	@Column(name = "length")
    private Long length;

	/** 是否切片 (0:不切片 1:切片)  split */
	@Column(name = "split")
    private Integer split;

	/** 分片数  total */
	@Column(name = "total")
    private Integer total;

	/** 校验和  crc32 */
	@Column(name = "crc32")
    private String crc32;

	/** 系统编码  sys_code */
	@Column(name = "sys_code")
    private String sysCode;

	/** 系统代号  sys_alisa */
	@Column(name = "sys_alisa")
    private String sysAlisa;

	/** 状态 (0 : 初始化 1: 上传中 2.上传完成  3:已删除)   status */
	@Column(name = "status")
    private Integer status;

	/** 过期时间  expire */
	@Column(name = "expire")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expire;

}
