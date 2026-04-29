package com.linkFlow.campaign;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Campaign 服务启动类
 */
@SpringBootApplication
@EnableDubbo
@MapperScan("com.linkFlow.campaign.mapper")
public class CampaignApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampaignApplication.class, args);
    }
}