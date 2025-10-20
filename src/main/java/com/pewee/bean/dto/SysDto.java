package com.pewee.bean.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @Classname SysDto
 * @Description 接入系统注册对象Dto(请求参数)
 * @Version 1.0.0
 * @Date 2022/4/13 17:56
 * @Created by Mr.GongRan
 */
@Getter
@Setter
@ToString
public class SysDto implements Serializable {

    /**
     * 系统名  sys_name
     */
    @NotNull(message = "系统名不能为空")
    private String sysName;

    /**
     * 系统代号  sys_alias
     */
    @NotNull(message = "系统代号不能为空")
    private String sysAlias;
}
