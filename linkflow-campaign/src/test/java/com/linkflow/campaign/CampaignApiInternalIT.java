package com.linkflow.campaign;

import com.linkflow.api.CampaignApi;
import com.linkflow.api.dto.campaign.CampaignCreateDTO;
import com.linkflow.api.dto.campaign.CampaignDTO;
import com.linkflow.api.dto.campaign.CampaignQueryDTO;
import com.linkflow.api.dto.campaign.CampaignStatusUpdateDTO;
import com.linkflow.api.dto.common.PageResult;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.UserDTO;
import com.linkflow.api.dto.workflow.WorkflowStartDTO;
import com.linkflow.campaign.service.UserAndWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@Transactional
@SpringBootTest(
        classes = CampaignApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.web-application-type=none",
                "dubbo.registry.address=N/A",
                "dubbo.protocol.port=-1",
                "dubbo.application.qos-enable=false"
        }
)
class CampaignApiInternalIT {

    @Autowired
    private CampaignApi campaignApi;

    @MockitoBean
    private UserAndWorkflowService userAndWorkflowService;

    @BeforeEach
    void setUp() {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(10001L);
        userDTO.setUsername("campaign-creator");

        when(userAndWorkflowService.getUserById(anyLong()))
                .thenReturn(Result.success(userDTO));
        when(userAndWorkflowService.startApprovalProcess(any(WorkflowStartDTO.class)))
                .thenReturn(Result.success("process-" + UUID.randomUUID()));
    }

    @Test
    void campaignApiFullLifecycle() {
        Long campaignId = createCampaign("lifecycle");

        Result<CampaignDTO> getResult = campaignApi.getCampaignById(campaignId);
        assertThat(getResult.isSuccess()).isTrue();
        assertThat(getResult.getData().getStatus()).isEqualTo("DRAFT");
        assertThat(getResult.getData().getCreatorUserId()).isEqualTo(10001L);

        CampaignQueryDTO queryDTO = new CampaignQueryDTO();
        queryDTO.setCreatorUserId(10001L);
        queryDTO.setCampaignType(getResult.getData().getCampaignType());
        queryDTO.setStatus("DRAFT");
        queryDTO.setPageNum(1);
        queryDTO.setPageSize(10);

        Result<PageResult<CampaignDTO>> listResult = campaignApi.getCampaignList(queryDTO);
        assertThat(listResult.isSuccess()).isTrue();
        assertThat(listResult.getData().getRecords())
                .extracting(CampaignDTO::getId)
                .contains(campaignId);

        Result<Void> bindResult = campaignApi.bindShortCode(campaignId, "short-code-001");
        assertThat(bindResult.isSuccess()).isTrue();
        assertThat(campaignApi.getCampaignById(campaignId).getData().getShortCode())
                .isEqualTo("short-code-001");

        Result<Void> submitResult = campaignApi.submitCampaign(campaignId);
        assertThat(submitResult.isSuccess()).isTrue();
        assertThat(campaignApi.getCampaignById(campaignId).getData().getStatus())
                .isEqualTo("APPROVING");

        CampaignStatusUpdateDTO statusUpdateDTO = new CampaignStatusUpdateDTO();
        statusUpdateDTO.setCampaignId(campaignId);
        statusUpdateDTO.setStatus("REJECTED");
        statusUpdateDTO.setRejectReason("预算不足");

        Result<Void> updateStatusResult = campaignApi.updateCampaignStatus(statusUpdateDTO);
        assertThat(updateStatusResult.isSuccess()).isTrue();
        CampaignDTO rejectedCampaign = campaignApi.getCampaignById(campaignId).getData();
        assertThat(rejectedCampaign.getStatus()).isEqualTo("REJECTED");
        assertThat(rejectedCampaign.getRejectReason()).isEqualTo("预算不足");
    }

    @Test
    void deleteAndCancelDraftCampaign() {
        Long deleteCampaignId = createCampaign("delete");
        Result<Void> deleteResult = campaignApi.deleteCampaign(deleteCampaignId);
        assertThat(deleteResult.isSuccess()).isTrue();
        assertThat(campaignApi.getCampaignById(deleteCampaignId).isSuccess()).isFalse();

        Long cancelCampaignId = createCampaign("cancel");
        Result<Void> cancelResult = campaignApi.cancelCampaign(cancelCampaignId);
        assertThat(cancelResult.isSuccess()).isTrue();
        assertThat(campaignApi.getCampaignById(cancelCampaignId).getData().getStatus())
                .isEqualTo("CANCELLED");
    }

    private Long createCampaign(String suffix) {
        CampaignCreateDTO createDTO = new CampaignCreateDTO();
        createDTO.setName(uniqueName("campaign-" + suffix));
        createDTO.setDescription("internal campaign api test");
        createDTO.setCampaignType(uniqueName("MARKETING"));
        createDTO.setCreatorUserId(10001L);
        createDTO.setStartTime(new Date(System.currentTimeMillis() + 3_600_000));
        createDTO.setEndTime(new Date(System.currentTimeMillis() + 7_200_000));
        createDTO.setBudget(new BigDecimal("1000.00"));
        createDTO.setLongUrl("https://example.com/" + suffix);

        Result<Long> createResult = campaignApi.createCampaign(createDTO);
        assertThat(createResult.isSuccess()).isTrue();
        assertThat(createResult.getData()).isNotNull();
        return createResult.getData();
    }

    private String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
