package com.xtransformers.distributedlock.dao;

import com.xtransformers.distributedlock.entity.UserReg;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

@Component
public interface UserRegMapper {

    //插入用户注册信息
    int insertSelective(UserReg record);

    //根据用户名查询用户实体
    UserReg selectByUserName(@Param("userName") String userName);

}
