package com.xtransformers.distributedlock.entity;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class UserReg {
    private Integer id;
    private String userName;
    private String password;
    private Date createTime;
}
