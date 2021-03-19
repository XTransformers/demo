package com.xtransformers.distributedlock.service;

import com.xtransformers.distributedlock.dao.UserRegMapper;
import com.xtransformers.distributedlock.dto.UserRegDto;
import com.xtransformers.distributedlock.entity.UserReg;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserRegService {

    @Autowired
    private UserRegMapper userRegMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CuratorFramework curatorFramework;

    /**
     * 处理用户提交注册的请求
     * -不加分布式锁
     *
     * @param dto * @throws Exception
     */
    public void userRegNoLock(UserRegDto dto) throws Exception {
        //根据用户名查询用户实体信息
        UserReg reg = userRegMapper.selectByUserName(dto.getUserName());
        //如果当前用户名还未被注册，则将当前用户信息注册入数据库中
        if (reg == null) {
            log.info("---不加分布式锁-- -, 当前用户名为：{} ", dto.getUserName());
            //创建用户注册实体信息
            UserReg entity = new UserReg();
            //将提交的用户注册请求实体信息中对应的字段取值
            //复制到新创建的用户注册实体的相应字段中
            BeanUtils.copyProperties(dto, entity);
            //设置注册时间
            entity.setCreateTime(new Date());
            //插入用户注册信息
            userRegMapper.insertSelective(entity);
        } else {
            //如果用户名已被注册，则抛出异常
            throw new Exception("用户信息已经存在!");
        }
    }

    /**
     * 处理用户提交注册的请求
     * -加分布式锁
     *
     * @param dto * @throws Exception
     */
    public void userRegWithRedisLock(UserRegDto dto) throws Exception {
        //精心设计并构造SETNX操作中的Key，一定要跟实际的业务或共享资源挂钩
        final String key = dto.getUserName() + "-lock";
        //设计Key对应的Value，为了具有随机性
        //在这里采用系统提供的纳秒级别的时间戳+ UUID生成的随机数作为Value
        final String value = System.nanoTime() + "" + UUID.randomUUID();
        //获取操作Key的ValueOperations实例
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        //调用SETNX操作获取锁，如果返回true，则获取锁成功
        //代表当前的共享资源还没被其他线程所占用
        Boolean res = valueOperations.setIfAbsent(key, value);
        //返回true，即代表获取到分布式锁
        if (res) {
            //为了防止出现死锁的状况，加上EXPIRE操作，即Key的过期时间，在这里设置为20s
            // 具体应根据实际情况而定
            stringRedisTemplate.expire(key, 20L, TimeUnit.SECONDS);
            try {                //根据用户名查询用户实体信息
                UserReg reg = userRegMapper.selectByUserName(dto.getUserName());
                //如果当前用户名还未被注册，则将当前的用户信息注册入数据库中
                if (reg == null) {
                    log.info("---加了 Redis 分布式锁 ---,当前用户名为： {} ", dto.getUserName());
                    //创建用户注册实体信息
                    UserReg entity = new UserReg();
                    //将提交的用户注册请求实体信息中对应的字段取值
                    //复制到新创建的用户注册实体的相应字段中
                    BeanUtils.copyProperties(dto, entity);
                    //设置注册时间
                    entity.setCreateTime(new Date());
                    //插入用户注册信息
                    userRegMapper.insertSelective(entity);
                } else {
                    //如果用户名已被注册，则抛出异常
                    throw new Exception("user info already exists.");
                }
            } catch (Exception e) {
                throw e;
            } finally {
                //不管发生任何情况，都需要在redis加锁成功并访问操作完共享资源后释放锁
                if (value.equals(valueOperations.get(key))) {
                    stringRedisTemplate.delete(key);
                }
            }
        } else {
            throw new Exception("can not get the Redis distributed lock.");
        }
    }

    //ZooKeeper分布式锁的实现原理是由ZNode节点的创建与删除跟监听机制构成的
    //而ZNoe节点将对应一个具体的路径-跟Unix文件夹路径类似，需要以 / 开头
    private static final String pathPrefix = "/middleware/zkLock/";
    private static final String pathSuffix = "-lock";

    /**
     * 处理用户提交注册的请求-加ZooKeeper分布式锁
     *
     * @param dto
     * @throws Exception
     */
    public void userRegWithZKLock(UserRegDto dto) throws Exception {
        // 创建 ZK 互斥锁组件实例，需要将监控用的客户端实例、精心构造的共享资源作为构造参数
        InterProcessMutex mutex = new InterProcessMutex(curatorFramework, pathPrefix + dto.getUserName() + pathSuffix);
        try {
            // 尝试获取互斥锁，等待最大时间为 10s
            if (mutex.acquire(1L, TimeUnit.SECONDS)) {
                UserReg reg = userRegMapper.selectByUserName(dto.getUserName());
                if (reg == null) {
                    log.info("---加了 ZK 分布式锁 ---,当前用户名为：{} ", dto.getUserName());
                    UserReg entity = new UserReg();
                    BeanUtils.copyProperties(dto, entity);
                    entity.setCreateTime(new Date());
                    userRegMapper.insertSelective(entity);
                } else {
                    //如果用户名已被注册，则抛出异常
                    throw new Exception("user info already exists.");
                }
            } else {
                throw new Exception("can not get the ZK distributed lock.");
            }
        } finally {
            mutex.release();
        }
    }
}
