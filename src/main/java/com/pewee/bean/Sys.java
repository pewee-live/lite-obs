package com.pewee.bean;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 接入系统注册对象 tbl_sys
 * 
 * @author pewee
 * @date 2022-04-13
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
@Table(name = "tbl_sys")
public class Sys implements Serializable {
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

	/** 是否可用(0:不可用 1:可用)  enabled */
	@Column(name = "enabled")
    private Integer enabled;

	/** 系统名  sys_name */
	@Column(name = "sys_name")
    private String sysName;

	/** 系统代号  sys_alias */
	@Column(name = "sys_alias")
    private String sysAlias;

	/** 系统密钥  secret */
	@Column(name = "secret")
    private String secret;

}
