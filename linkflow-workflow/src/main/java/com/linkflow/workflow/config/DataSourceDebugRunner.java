package com.linkflow.workflow.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class DataSourceDebugRunner implements ApplicationRunner {

    private final SpringProcessEngineConfiguration processEngineConfiguration;

    private final SqlSessionFactory sqlSessionFactory;

    public DataSourceDebugRunner(
            SpringProcessEngineConfiguration processEngineConfiguration,SqlSessionFactory  sqlSessionFactory) {
        this.processEngineConfiguration = processEngineConfiguration;
        this.sqlSessionFactory = sqlSessionFactory;
    }
    @Override
    public void run(ApplicationArguments args) throws Exception {
        DataSource flowableDataSource = processEngineConfiguration.getDataSource();

        try (Connection connection = flowableDataSource.getConnection()) {
            System.out.println("[DEBUG] Flowable DataSource URL = "
                    + connection.getMetaData().getURL());
        }

        DataSource mybatisDataSource =
                sqlSessionFactory.getConfiguration()
                        .getEnvironment()
                        .getDataSource();

        try (Connection connection = mybatisDataSource.getConnection()) {
            System.out.println("[DEBUG] MyBatis DataSource URL = "
                    + connection.getMetaData().getURL());
        }
    }
}
