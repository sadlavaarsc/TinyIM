package com.tinyim;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TinyIM 启动类
 * 高并发即时通讯系统入口，集成 Netty、RocketMQ、Redis、MyBatis Plus
 */
@SpringBootApplication
@MapperScan("com.tinyim.offline")
public class IMApplication {

    public static void main(String[] args) {
        SpringApplication.run(IMApplication.class, args);
    }
}
