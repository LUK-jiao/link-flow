package com.linkflow.workflow.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
@MapperScan(
        basePackages = "com.linkflow.workflow.mapper",
        sqlSessionFactoryRef = "businessSqlSessionFactory",
        sqlSessionTemplateRef = "businessSqlSessionTemplate"
)
public class WorkflowDataSourceConfig {

    @Bean(name = "flowableDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.flowable")
    public DataSource flowableDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "businessDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.business")
    public DataSource businessDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "businessSqlSessionFactory")
    public SqlSessionFactory businessSqlSessionFactory(
            @Qualifier("businessDataSource") DataSource businessDataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(businessDataSource);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        factoryBean.setTypeAliasesPackage("com.linkflow.workflow.model");
        return factoryBean.getObject();
    }

    @Bean(name = "businessSqlSessionTemplate")
    public SqlSessionTemplate businessSqlSessionTemplate(
            @Qualifier("businessSqlSessionFactory") SqlSessionFactory businessSqlSessionFactory) {
        return new SqlSessionTemplate(businessSqlSessionFactory);
    }

    @Bean(name = "businessTransactionManager")
    public DataSourceTransactionManager businessTransactionManager(
            @Qualifier("businessDataSource") DataSource businessDataSource) {
        return new DataSourceTransactionManager(businessDataSource);
    }
}

