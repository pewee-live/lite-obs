package com.pewee.bean.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @Classname SysVo
 * @Description 接入系统注册对象Vo(返回参数)
 * @Version 1.0.0
 * @Date 2022/4/13 17:53
 * @Created by Mr.GongRan
 */
@Getter
@Setter
@ToString
public class SysVo implements Serializable {

    private static final long serialVersionUID = 4840819405915976431L;
    /**
     * 系统编码
     */
    private String code;

    /**
     * 系统秘钥
     */
    private String secret;
}
