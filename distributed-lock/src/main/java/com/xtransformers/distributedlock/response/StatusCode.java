package com.xtransformers.distributedlock.response;

public enum StatusCode {

    //以下是暂时设定的几种状态码类
    Success(0, "success"),
    Fail(-1, "fail"),
    InvalidParams(201, "illegal parameters"),
    InvalidGrantType(202, "illegal grant type");

    //状态码
    private Integer code;
    //描述信息
    private String msg;

    StatusCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode
            () {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
