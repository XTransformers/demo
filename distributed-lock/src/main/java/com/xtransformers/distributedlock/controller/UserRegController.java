package com.xtransformers.distributedlock.controller;

import com.mysql.jdbc.StringUtils;
import com.xtransformers.distributedlock.dto.UserRegDto;
import com.xtransformers.distributedlock.response.BaseResponse;
import com.xtransformers.distributedlock.response.StatusCode;
import com.xtransformers.distributedlock.service.UserRegService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
public class UserRegController {

    private static final String prefix = "user/reg";

    @Autowired
    private UserRegService userRegService;

    /**
     * 提交用户注册信息
     *
     * @param dto * @return
     */
    @RequestMapping(value = prefix + "/submit", method = RequestMethod.GET)
    public BaseResponse reg(UserRegDto dto) {
        //校验提交的用户名、密码等信息
        if (StringUtils.isNullOrEmpty(dto.getUserName()) || StringUtils.isNullOrEmpty(dto.getPassword())) {
            return new BaseResponse(StatusCode.InvalidParams);
        }        //定义返回信息实例
        BaseResponse response = new BaseResponse(StatusCode.Success);
        try {
            //处理用户提交请求-不加分布式锁
            userRegService.userRegNoLock(dto);
        } catch (Exception e) {
            //发生异常情况的处理
            response = new BaseResponse(StatusCode.Fail.getCode(), e.getMessage());
        }
        //返回响应信息
        return response;
    }

    @RequestMapping(value = prefix + "/submit/redis/withLock", method = RequestMethod.GET)
    public BaseResponse regWithLock(UserRegDto dto) {
        //校验提交的用户名、密码等信息
        if (StringUtils.isNullOrEmpty(dto.getUserName()) || StringUtils.isNullOrEmpty(dto.getPassword())) {
            return new BaseResponse(StatusCode.InvalidParams);
        }        //定义返回信息实例
        BaseResponse response = new BaseResponse(StatusCode.Success);
        try {
            //处理用户提交请求-加分布式锁
            userRegService.userRegWithRedisLock(dto);
        } catch (Exception e) {
            //发生异常情况的处理
            response = new BaseResponse(StatusCode.Fail.getCode(), e.getMessage());
        }
        //返回响应信息
        return response;
    }

    @RequestMapping(value = prefix + "/submit/zk/withLock", method = RequestMethod.GET)
    public BaseResponse regWithZKLock(UserRegDto dto, HttpServletResponse resp) {
        //校验提交的用户名、密码等信息
        if (StringUtils.isNullOrEmpty(dto.getUserName()) || StringUtils.isNullOrEmpty(dto.getPassword())) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new BaseResponse(StatusCode.InvalidParams);
        }        //定义返回信息实例
        BaseResponse response = new BaseResponse(StatusCode.Success);
        try {
            //处理用户提交请求-加分布式锁
            userRegService.userRegWithZKLock(dto);
        } catch (Exception e) {
            //发生异常情况的处理
            // You do not own the lock: /middleware/zkLock/Daniel-lock
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            response = new BaseResponse(StatusCode.Fail.getCode(), e.getMessage());
        }
        //返回响应信息
        return response;
    }

}
