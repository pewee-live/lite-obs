package com.pewee.bean.nas.client.exception;

public class NasResponseException extends Exception {

    public NasResponseException() {
        super("获取数据时发生错误,请检查API版本兼容性");
    }

    public NasResponseException(String message) {
        super(message);
    }
}
