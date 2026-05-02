package com.linkflow.workflow.config;

import com.zaxxer.hikari.HikariDataSource;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Flowable 多数据源配置
 * 
 * 业务数据使用 linkflow_db
 * Flowable 引擎数据使用 linkflow_flowable（独立数据库）
 */
@Configuration
public class FlowableDataSourceConfig {

    /**
     * 业务数据源（主数据源）
     */
    @Primary
    @Bean(name = "businessDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource businessDataSource() {
        return new HikariDataSource();
    }

    /**
     * Flowable 数据源（独立数据库）
     */
    @Bean(name = "flowableDataSource")
    @ConfigurationProperties(prefix = "flowable.datasource")
    public DataSource flowableDataSource() {
        return new HikariDataSource();
    }

    /**
     * 配置 Flowable 使用独立数据源
     */
    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> flowableDataSourceConfigurer(
            @Qualifier("flowableDataSource") DataSource flowableDataSource) {
        return config -> {
            config.setDataSource(flowableDataSource);
            config.setDatabaseSchemaUpdate("true");
            config.setAsyncExecutorActivate(true);
            // Flowable 自动扫描 classpath:processes/ 目录下的 bpmn20.xml 文件
        };
    }
}