package com.linkflow.workflow.e2e;

import com.linkflow.api.ApproverConfigApi;
import com.linkflow.api.CampaignApi;
import com.linkflow.api.ShortLinkApi;
import com.linkflow.api.UserApi;
import com.linkflow.api.WorkflowApi;
import com.linkflow.api.dto.campaign.CampaignCreateDTO;
import com.linkflow.api.dto.campaign.CampaignDTO;
import com.linkflow.api.dto.common.PageResult;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.ApproverDTO;
import com.linkflow.api.dto.user.UserDTO;
import com.linkflow.api.dto.workflow.ApprovalRequestDTO;
import com.linkflow.api.dto.workflow.RejectRequestDTO;
import com.linkflow.api.dto.workflow.WorkflowStatusDTO;
import com.linkflow.api.dto.workflow.WorkflowTaskDTO;
import com.linkflow.api.dto.workflow.WorkflowTaskQueryDTO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端联调测试（Dubbo consumer-only）。
 *
 * 运行前需要先启动：
 * 1) Docker: nacos/kafka/zookeeper (+ 你本地的 mysql/redis)
 * 2) 应用：short-link、linkflow-user、linkflow-campaign、linkflow-workflow
 */
@SpringBootTest(
        classes = ApprovalAndShortLinkE2EIT.DubboConsumerTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.application.name=linkflow-e2e-approval-shortlink-test",
                "spring.main.web-application-type=none",
                "dubbo.application.name=linkflow-e2e-approval-shortlink-test",
                "dubbo.application.qos-enable=false",
                "dubbo.registry.address=nacos://localhost:8848",
                "dubbo.registry.parameters.namespace=linkflow",
                "dubbo.consumer.check=false",
                "dubbo.consumer.timeout=10000",
                "dubbo.consumer.retries=0",
                "dubbo.protocol.port=-1",
                "dubbo.scan.base-packages=com.linkflow.workflow.e2e",
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
class ApprovalAndShortLinkE2EIT {

    private static final Logger log = LoggerFactory.getLogger(ApprovalAndShortLinkE2EIT.class);
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration WAIT_POLL_INTERVAL = Duration.ofMillis(200);
    private static final Set<String> APPROVER_ROLES = Set.of("APPROVER", "ADMIN");

    @DubboReference(check = false)
    private UserApi userApi;

    @DubboReference(check = false)
    private ApproverConfigApi approverConfigApi;

    @DubboReference(check = false)
    private CampaignApi campaignApi;

    @DubboReference(check = false)
    private WorkflowApi workflowApi;

    @DubboReference(check = false)
    private ShortLinkApi shortLinkApi;

    @Test
    void approve_should_update_campaign_and_bind_shortlink() {
        String campaignType = "e2e-" + shortId();

        CreatedUser creator = pickExistingUser(Set.of(), Set.of());
        CreatedUser approver1 = pickExistingUser(Set.of(creator.id()), APPROVER_ROLES);
        CreatedUser approver2 = pickExistingUser(Set.of(creator.id(), approver1.id()), APPROVER_ROLES);

        Long approver1ConfigId = configApprover(campaignType, approver1, 1);
        Long approver2ConfigId = configApprover(campaignType, approver2, 2);

        String longUrl = "https://example.com/linkflow-e2e/" + shortId();
        Long campaignId = createCampaign(creator.id(), campaignType, longUrl);
        log.info("E2E approve: campaignId={}", campaignId);

        Result<Void> submitResult = campaignApi.submitCampaign(campaignId);
        assertThat(submitResult.isSuccess()).isTrue();

        String processInstanceId = waitForProcessInstanceId(campaignId);
        log.info("E2E approve: processInstanceId={}", processInstanceId);

        WorkflowTaskDTO level1Task = waitForPendingTask(approver1.id(), campaignType);
        log.info("E2E approve: level1 taskId={}", level1Task.getTaskId());

        ApprovalRequestDTO level1Approve = new ApprovalRequestDTO();
        level1Approve.setProcessInstanceId(processInstanceId);
        level1Approve.setTaskId(level1Task.getTaskId());
        level1Approve.setApproverId(approver1.id());
        level1Approve.setApproverName(approver1.username());
        level1Approve.setComment("E2E level1 approve");
        assertThat(workflowApi.approve(level1Approve).isSuccess()).isTrue();

        WorkflowTaskDTO level2Task = waitForPendingTask(approver2.id(), campaignType);
        log.info("E2E approve: level2 taskId={}", level2Task.getTaskId());

        ApprovalRequestDTO level2Approve = new ApprovalRequestDTO();
        level2Approve.setProcessInstanceId(processInstanceId);
        level2Approve.setTaskId(level2Task.getTaskId());
        level2Approve.setApproverId(approver2.id());
        level2Approve.setApproverName(approver2.username());
        level2Approve.setComment("E2E level2 approve");
        assertThat(workflowApi.approve(level2Approve).isSuccess()).isTrue();

        CampaignDTO approvedCampaign = waitForCampaignStatus(campaignId, "APPROVED");
        assertThat(approvedCampaign.getLongUrl()).isEqualTo(longUrl);

        CampaignDTO boundCampaign = waitForCampaignShortCode(campaignId);
        String shortCode = boundCampaign.getShortCode();
        assertThat(shortCode).isNotBlank();
        log.info("E2E approve: shortCode={}", shortCode);

        Result<Boolean> existsResult = shortLinkApi.exists(shortCode);
        assertThat(existsResult.isSuccess()).isTrue();
        assertThat(existsResult.getData()).isTrue();

        Result<String> urlResult = shortLinkApi.getUrlByCode(shortCode);
        assertThat(urlResult.isSuccess()).isTrue();
        assertThat(urlResult.getData()).isEqualTo(longUrl);

        assertThat(boundCampaign.getShortCode()).isEqualTo(shortCode);

        // best-effort cleanup of approver configs to reduce DB noise
        approverConfigApi.deleteApproverConfig(approver1ConfigId);
        approverConfigApi.deleteApproverConfig(approver2ConfigId);
    }

    @Test
    void reject_should_update_campaign_status() {
        String campaignType = "e2e-" + shortId();

        CreatedUser creator = pickExistingUser(Set.of(), Set.of());
        CreatedUser approver1 = pickExistingUser(Set.of(creator.id()), APPROVER_ROLES);
        CreatedUser approver2 = pickExistingUser(Set.of(creator.id(), approver1.id()), APPROVER_ROLES);

        Long approver1ConfigId = configApprover(campaignType, approver1, 1);
        Long approver2ConfigId = configApprover(campaignType, approver2, 2);

        String longUrl = "https://example.com/linkflow-e2e/" + shortId();
        Long campaignId = createCampaign(creator.id(), campaignType, longUrl);
        log.info("E2E reject: campaignId={}", campaignId);

        assertThat(campaignApi.submitCampaign(campaignId).isSuccess()).isTrue();

        String processInstanceId = waitForProcessInstanceId(campaignId);
        log.info("E2E reject: processInstanceId={}", processInstanceId);

        WorkflowTaskDTO level1Task = waitForPendingTask(approver1.id(), campaignType);
        log.info("E2E reject: level1 taskId={}", level1Task.getTaskId());

        RejectRequestDTO rejectRequestDTO = new RejectRequestDTO();
        rejectRequestDTO.setProcessInstanceId(processInstanceId);
        rejectRequestDTO.setTaskId(level1Task.getTaskId());
        rejectRequestDTO.setApproverId(approver1.id());
        rejectRequestDTO.setApproverName(approver1.username());
        rejectRequestDTO.setComment("E2E reject");
        rejectRequestDTO.setRejectReason("预算不通过");
        assertThat(workflowApi.reject(rejectRequestDTO).isSuccess()).isTrue();

        CampaignDTO rejectedCampaign = waitForCampaignStatus(campaignId, "REJECTED");
        assertThat(rejectedCampaign.getRejectReason()).isEqualTo("预算不通过");

        approverConfigApi.deleteApproverConfig(approver1ConfigId);
        approverConfigApi.deleteApproverConfig(approver2ConfigId);
    }

    private Long createCampaign(Long creatorUserId, String campaignType, String longUrl) {
        CampaignCreateDTO dto = new CampaignCreateDTO();
        dto.setName("e2e-campaign-" + shortId());
        dto.setDescription("e2e campaign");
        dto.setCampaignType(campaignType);
        dto.setCreatorUserId(creatorUserId);
        dto.setStartTime(new Date());
        dto.setEndTime(new Date(System.currentTimeMillis() + Duration.ofDays(1).toMillis()));
        dto.setBudget(BigDecimal.valueOf(100));
        dto.setLongUrl(longUrl);

        Result<Long> createResult = campaignApi.createCampaign(dto);
        assertThat(createResult.isSuccess()).isTrue();
        assertThat(createResult.getData()).isNotNull();
        return createResult.getData();
    }

    private CreatedUser pickExistingUser(Set<Long> excludedIds, Set<String> requiredRoles) {
        for (long id = 1; id <= 200; id++) {
            if (excludedIds.contains(id)) {
                continue;
            }
            Result<UserDTO> result = userApi.getUserById(id);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                continue;
            }
            UserDTO user = result.getData();
            if (!requiredRoles.isEmpty() && !requiredRoles.contains(user.getRole())) {
                continue;
            }
            return new CreatedUser(user.getId(), user.getUsername());
        }
        if (requiredRoles.isEmpty()) {
            throw new IllegalStateException("no existing user found, please seed short_link_db.user first");
        }
        throw new IllegalStateException("no existing approver/admin user found, please seed short_link_db.user with role APPROVER or ADMIN");
    }

    private Long configApprover(String campaignType, CreatedUser approver, int level) {
        ApproverDTO dto = new ApproverDTO();
        dto.setCampaignType(campaignType);
        dto.setApproverId(approver.id());
        dto.setApproverName(approver.username());
        dto.setApproverLevel(level);

        Result<Long> configResult = approverConfigApi.configApprover(dto);
        assertThat(configResult.isSuccess()).isTrue();
        assertThat(configResult.getData()).isNotNull();
        return configResult.getData();
    }

    private String waitForProcessInstanceId(Long businessKey) {
        WorkflowStatusDTO status = waitUntil(
                () -> workflowApi.getProcessStatusByBusinessKey(businessKey),
                result -> result != null && result.isSuccess() && result.getData() != null && result.getData().getProcessInstanceId() != null
        ).getData();
        assertThat(status.getProcessInstanceId()).isNotBlank();
        return status.getProcessInstanceId();
    }

    private WorkflowTaskDTO waitForPendingTask(Long approverId, String campaignType) {
        WorkflowTaskQueryDTO query = new WorkflowTaskQueryDTO();
        query.setApproverId(approverId);
        query.setBusinessType("CAMPAIGN_APPROVAL");
        query.setCampaignType(campaignType);
        query.setPageNum(1);
        query.setPageSize(10);

        Result<PageResult<WorkflowTaskDTO>> pending = waitUntil(
                () -> workflowApi.getPendingTasks(query),
                result -> result != null && result.isSuccess()
                        && result.getData() != null
                        && result.getData().getTotal() != null
                        && result.getData().getTotal() > 0
                        && result.getData().getRecords() != null
                        && !result.getData().getRecords().isEmpty()
        );

        WorkflowTaskDTO task = pending.getData().getRecords().get(0);
        assertThat(task.getTaskId()).isNotBlank();
        return task;
    }

    private CampaignDTO waitForCampaignStatus(Long campaignId, String expectedStatus) {
        Result<CampaignDTO> result = waitUntil(
                () -> campaignApi.getCampaignById(campaignId),
                r -> r != null && r.isSuccess() && r.getData() != null && expectedStatus.equals(r.getData().getStatus())
        );
        return result.getData();
    }

    private CampaignDTO waitForCampaignShortCode(Long campaignId) {
        Result<CampaignDTO> result = waitUntil(
                () -> campaignApi.getCampaignById(campaignId),
                r -> r != null
                        && r.isSuccess()
                        && r.getData() != null
                        && r.getData().getShortCode() != null
                        && !r.getData().getShortCode().isBlank()
        );
        return result.getData();
    }

    private <T> T waitUntil(Supplier<T> supplier, java.util.function.Predicate<T> done) {
        long deadline = System.nanoTime() + WAIT_TIMEOUT.toNanos();
        RuntimeException lastError = null;

        while (System.nanoTime() < deadline) {
            try {
                T value = supplier.get();
                if (done.test(value)) {
                    return value;
                }
            } catch (RuntimeException e) {
                lastError = e;
            }

            try {
                Thread.sleep(WAIT_POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting", e);
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new IllegalStateException("timeout waiting for condition");
    }

    @EnableDubbo(scanBasePackages = "com.linkflow.workflow.e2e")
    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class DubboConsumerTestApplication {
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record CreatedUser(Long id, String username) {
    }
}
