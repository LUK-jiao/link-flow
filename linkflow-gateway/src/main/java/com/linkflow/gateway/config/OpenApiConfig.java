package com.linkflow.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI linkFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LinkFlow Gateway API")
                        .version("v1")
                        .description("LinkFlow 统一网关接口文档（HTTP -> Dubbo）"));
    }
}
