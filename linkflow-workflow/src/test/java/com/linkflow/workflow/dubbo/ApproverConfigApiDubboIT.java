package com.linkflow.workflow.dubbo;

import com.linkflow.api.ApproverConfigApi;
import com.linkflow.api.UserApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.ApproverDTO;
import com.linkflow.api.dto.user.UserCreateDTO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
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
        classes = ApproverConfigApiDubboIT.DubboConsumerTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.application.name=linkflow-workflow-approver-api-dubbo-test",
                "spring.main.web-application-type=none",
                "dubbo.application.name=linkflow-workflow-approver-api-dubbo-test",
                "dubbo.application.qos-enable=false",
                "dubbo.registry.address=nacos://localhost:8848",
                "dubbo.registry.parameters.namespace=linkflow",
                "dubbo.consumer.check=false",
                "dubbo.protocol.port=-1",
                "dubbo.scan.base-packages=com.linkflow.workflow.dubbo",
                "spring.autoconfigure.exclude=" +
                        "org.flowable.spring.boot.actuate.info.FlowableInfoAutoConfiguration," +
                        "org.flowable.spring.boot.EndpointAutoConfiguration," +
                        "org.flowable.spring.boot.RestApiAutoConfiguration," +
                        "org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration," +
                        "org.flowable.spring.boot.app.AppEngineAutoConfiguration," +
                        "org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration," +
                        "org.flowable.spring.boot.ProcessEngineAutoConfiguration," +
                        "org.flowable.spring.boot.FlowableJpaAutoConfiguration," +
                        "org.flowable.spring.boot.dmn.DmnEngineAutoConfiguration," +
                        "org.flowable.spring.boot.dmn.DmnEngineServicesAutoConfiguration," +
                        "org.flowable.spring.boot.idm.IdmEngineAutoConfiguration," +
                        "org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration," +
                        "org.flowable.spring.boot.eventregistry.EventRegistryAutoConfiguration," +
                        "org.flowable.spring.boot.eventregistry.EventRegistryServicesAutoConfiguration," +
                        "org.flowable.spring.boot.cmmn.CmmnEngineAutoConfiguration," +
                        "org.flowable.spring.boot.cmmn.CmmnEngineServicesAutoConfiguration," +
                        "org.flowable.spring.boot.ldap.FlowableLdapAutoConfiguration," +
                        "org.flowable.spring.boot.FlowableSecurityAutoConfiguration"
        }
)
class ApproverConfigApiDubboIT {

    @DubboReference(check = false)
    private UserApi userApi;

    @DubboReference(check = false)
    private ApproverConfigApi approverConfigApi;

    @Test
    void workflowCanCallApproverConfigApiThroughDubbo() {
        Long approverId = createApproverUser();
        String campaignType = "workflow-" + shortId();

        ApproverDTO configDTO = new ApproverDTO();
        configDTO.setCampaignType(campaignType);
        configDTO.setApproverId(approverId);
        configDTO.setApproverLevel(1);

        Result<Long> configResult = approverConfigApi.configApprover(configDTO);
        assertThat(configResult.isSuccess()).isTrue();
        assertThat(configResult.getData()).isNotNull();

        Result<List<ApproverDTO>> getResult =
                approverConfigApi.getApproverByTypeAndLevel(campaignType, 1);
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getData())
                .extracting(ApproverDTO::getApproverId)
                .contains(approverId);

        Result<Void> deleteResult = approverConfigApi.deleteApproverConfig(configResult.getData());
        assertThat(deleteResult.isSuccess()).isTrue();
    }

    private Long createApproverUser() {
        String username = "wf-approver-" + shortId();

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername(username);
        createDTO.setPassword("123456");
        createDTO.setEmail(username + "@example.com");
        createDTO.setPhone("13600000000");
        createDTO.setRole("APPROVER");

        Result<Long> result = userApi.createUser(createDTO);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        return result.getData();
    }

    @EnableDubbo(scanBasePackages = "com.linkflow.workflow.dubbo")
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class DubboConsumerTestApplication {
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
