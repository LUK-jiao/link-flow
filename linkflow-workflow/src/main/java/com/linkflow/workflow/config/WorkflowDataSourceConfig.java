package com.linkflow.workflow.config;

import org.flowable.spring.boot.ProcessEngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class WorkflowDataSourceConfig {

    @Bean(name = "flowableDataSource", defaultCandidate = false)
    @ConfigurationProperties(prefix = "flowable.datasource")
    public DataSource flowableDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public ProcessEngineConfigurationConfigurer flowableDataSourceConfigurer(
            @Qualifier("flowableDataSource") DataSource flowableDataSource) {
        return configuration -> configuration.setDataSource(flowableDataSource);
    }
}
