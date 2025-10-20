package com.pewee.bean.nas.client.exception;

public class NasCompatibilityException extends Exception {
    public NasCompatibilityException() {
        super("当前调用与远程磁盘工作站支持的api版本不兼容");
    }

    public NasCompatibilityException(String message) {
        super(message);
    }
}
