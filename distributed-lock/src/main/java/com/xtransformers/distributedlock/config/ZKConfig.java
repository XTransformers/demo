package com.xtransformers.distributedlock.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ZKConfig {

    @Autowired
    private Environment env;

    @Bean
    public CuratorFramework curatorFramework() {
        String host = env.getProperty("zk.host");
        String namespace = env.getProperty("zk.namespace");
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .connectString(host)
                .namespace(namespace)
                .retryPolicy(new RetryNTimes(5, 1000))
                .build();
        curatorFramework.start();
        return curatorFramework;
    }

}
