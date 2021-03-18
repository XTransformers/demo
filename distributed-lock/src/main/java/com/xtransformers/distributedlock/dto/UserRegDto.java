package com.xtransformers.distributedlock.dto;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class UserRegDto implements Serializable {
    private String userName;
    private String password;
}
