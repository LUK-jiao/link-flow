package com.linkflow.campaign.dubbo;

import com.linkflow.api.UserApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.UserCreateDTO;
import com.linkflow.api.dto.user.UserDTO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dubbo 联调测试。
 *
 * 运行前需要先启动 Docker 基础设施和 linkflow-user 服务：
 * docker compose up -d
 * UserApplication
 */
@SpringBootTest(
        classes = UserApiDubboIT.DubboConsumerTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.application.name=linkflow-campaign-user-api-dubbo-test",
                "spring.main.web-application-type=none",
                "dubbo.application.name=linkflow-campaign-user-api-dubbo-test",
                "dubbo.application.qos-enable=false",
                "dubbo.registry.address=nacos://localhost:8848",
                "dubbo.registry.parameters.namespace=linkflow",
                "dubbo.consumer.check=false",
                "dubbo.protocol.port=-1",
                "dubbo.scan.base-packages=com.linkflow.campaign.dubbo"
        }
)
class UserApiDubboIT {

    @DubboReference(check = false)
    private UserApi userApi;

    @Test
    void campaignCanCallUserApiThroughDubbo() {
        String username = "campaign-" + shortId();

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername(username);
        createDTO.setPassword("123456");
        createDTO.setEmail(username + "@example.com");
        createDTO.setPhone("13700000000");

        Result<Long> createResult = userApi.createUser(createDTO);
        assertThat(createResult.isSuccess()).isTrue();
        assertThat(createResult.getData()).isNotNull();

        Result<UserDTO> getResult = userApi.getUserById(createResult.getData());
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getData().getUsername()).isEqualTo(username);
    }

    @EnableDubbo(scanBasePackages = "com.linkflow.campaign.dubbo")
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class DubboConsumerTestApplication {
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
