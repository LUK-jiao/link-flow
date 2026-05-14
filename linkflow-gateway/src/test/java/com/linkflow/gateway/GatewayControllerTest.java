package com.linkflow.gateway;

import com.linkflow.api.ApproverConfigApi;
import com.linkflow.api.CampaignApi;
import com.linkflow.api.ShortLinkApi;
import com.linkflow.api.UserApi;
import com.linkflow.api.WorkflowApi;
import com.linkflow.api.dto.campaign.CampaignDTO;
import com.linkflow.api.dto.common.PageResult;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.workflow.ApprovalRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "dubbo.registry.address=N/A",
                "dubbo.protocol.port=-1",
                "dubbo.application.qos-enable=false",
                "spring.cloud.compatibility-verifier.enabled=false"
        }
)
@AutoConfigureWebTestClient
class GatewayControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean(name = "userApi")
    private UserApi userApi;

    @MockBean(name = "approverConfigApi")
    private ApproverConfigApi approverConfigApi;

    @MockBean(name = "campaignApi")
    private CampaignApi campaignApi;

    @MockBean(name = "workflowApi")
    private WorkflowApi workflowApi;

    @MockBean(name = "shortLinkApi")
    private ShortLinkApi shortLinkApi;

    @BeforeEach
    void setUp() {
        Mockito.reset(userApi, approverConfigApi, campaignApi, workflowApi, shortLinkApi);
    }

    @Test
    void campaignQuery_shouldReturnPageResult() {
        CampaignDTO dto = new CampaignDTO();
        dto.setId(1001L);
        dto.setName("campaign-1001");

        PageResult<CampaignDTO> page = new PageResult<>();
        page.setRecords(List.of(dto));
        page.setTotal(1L);
        page.setPageNum(1L);
        page.setPageSize(10L);
        page.setPages(1L);

        when(campaignApi.getCampaignList(any())).thenReturn(Result.success(page));

        webTestClient.post()
                .uri("/api/campaigns/query")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("pageNum", 1, "pageSize", 10))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(200)
                .jsonPath("$.data.total").isEqualTo(1)
                .jsonPath("$.data.records[0].id").isEqualTo(1001);
    }

    @Test
    void approve_shouldPassThroughAndReturnSuccess() {
        when(workflowApi.approve(any())).thenReturn(Result.success());

        webTestClient.post()
                .uri("/api/workflows/approve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "processInstanceId", "proc-1",
                        "taskId", "task-1",
                        "approverId", 7,
                        "approverName", "alice",
                        "comment", "ok"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("success");
    }

    @Test
    void whenProviderThrowsException_gatewayShouldWrapAsResultFail() {
        when(userApi.getUserById(eq(1L))).thenThrow(new RuntimeException("boom"));

        webTestClient.get()
                .uri("/api/users/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(500)
                .jsonPath("$.message").isEqualTo("boom");
    }

}
