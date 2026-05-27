package com.linkflow.gateway;

import com.linkflow.api.ApproverConfigApi;
import com.linkflow.api.AgentApi;
import com.linkflow.api.CampaignApi;
import com.linkflow.api.ShortLinkApi;
import com.linkflow.api.UserApi;
import com.linkflow.api.WorkflowApi;
import com.linkflow.api.dto.agent.AgentChatDTO;
import com.linkflow.api.dto.agent.AgentChatCommand;
import com.linkflow.api.dto.campaign.CampaignDTO;
import com.linkflow.api.dto.common.PageResult;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.UserLoginDTO;
import com.linkflow.api.dto.user.UserLoginResultDTO;
import com.linkflow.api.dto.workflow.ApprovalRequestDTO;
import com.linkflow.api.dto.user.UserCreateDTO;
import com.linkflow.api.dto.user.UserDTO;
import com.linkflow.gateway.auth.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "dubbo.registry.address=N/A",
                "dubbo.protocol.port=-1",
                "dubbo.application.qos-enable=false",
                "spring.cloud.compatibility-verifier.enabled=false",
                "linkflow.auth.jwt.secret=linkflow-gateway-test-secret-key-2026",
                "linkflow.auth.jwt.expire-minutes=120"
        }
)
@AutoConfigureWebTestClient
class GatewayControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockBean(name = "agentApi")
    private AgentApi agentApi;

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
        Mockito.reset(agentApi, userApi, approverConfigApi, campaignApi, workflowApi, shortLinkApi);
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
                .headers(headers -> headers.setBearerAuth(testToken()))
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
                .headers(headers -> headers.setBearerAuth(testToken()))
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
                .headers(headers -> headers.setBearerAuth(testToken()))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(500)
                .jsonPath("$.message").isEqualTo("boom");
    }

    @Test
    void login_shouldReturnTokenWhenUserApiLoginSucceeds() {
        UserLoginResultDTO loginResultDTO = new UserLoginResultDTO();
        loginResultDTO.setUserId(7L);
        loginResultDTO.setUsername("admin");
        loginResultDTO.setRole("ADMIN");
        loginResultDTO.setStatus((byte) 1);
        when(userApi.login(any(UserLoginDTO.class))).thenReturn(Result.success(loginResultDTO));

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "admin", "password", "123456"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(200)
                .jsonPath("$.data.token").exists()
                .jsonPath("$.data.userId").isEqualTo(7)
                .jsonPath("$.data.username").isEqualTo("admin")
                .jsonPath("$.data.role").isEqualTo("ADMIN");
    }

    @Test
    void login_shouldNotReturnTokenWhenUserApiLoginFails() {
        when(userApi.login(any(UserLoginDTO.class))).thenReturn(Result.fail(401, "用户名或密码错误"));

        webTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "admin", "password", "bad"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.data.token").doesNotExist();
    }

    @Test
    void agentChat_shouldRejectMissingOrInvalidToken() {
        webTestClient.post()
                .uri("/api/agent/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("message", "hello"))
                .exchange()
                .expectStatus().isUnauthorized();

        webTestClient.post()
                .uri("/api/agent/chat")
                .headers(headers -> headers.setBearerAuth("bad-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("message", "hello"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void agentChat_shouldCallAgentApiWithCurrentUserFromTokenAndOverwriteForgedHeaders() {
        AgentChatDTO dto = new AgentChatDTO();
        dto.setSessionId("sess_1");
        dto.setMessageId("msg_1");
        dto.setType("TEXT");
        dto.setText("ok");
        when(agentApi.chat(any(AgentChatCommand.class))).thenReturn(Result.success(dto));

        webTestClient.post()
                .uri("/api/agent/chat")
                .headers(headers -> {
                    headers.setBearerAuth(testToken());
                    headers.add("X-User-Id", "999");
                    headers.add("X-Username", "forged");
                    headers.add("X-User-Role", "ADMIN");
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("message", "hello"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(200)
                .jsonPath("$.data.sessionId").isEqualTo("sess_1")
                .jsonPath("$.data.messageId").isEqualTo("msg_1");

        ArgumentCaptor<AgentChatCommand> commandCaptor = ArgumentCaptor.forClass(AgentChatCommand.class);
        verify(agentApi).chat(commandCaptor.capture());
        AgentChatCommand command = commandCaptor.getValue();
        assertThat(command.getUserId()).isEqualTo(7L);
        assertThat(command.getUsername()).isEqualTo("alice");
        assertThat(command.getRole()).isEqualTo("USER");
        assertThat(command.getMessage()).isEqualTo("hello");
    }

    @Test
    void register_shouldReturnTokenWhenCreateSucceeds() {
        when(userApi.createUser(any(UserCreateDTO.class))).thenReturn(Result.success(11L));
        UserDTO userDTO = new UserDTO();
        userDTO.setId(11L);
        userDTO.setUsername("new-user");
        userDTO.setRole("USER");
        userDTO.setStatus((byte) 1);
        when(userApi.getUserByUsername(eq("new-user"))).thenReturn(Result.success(userDTO));

        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "username", "new-user",
                        "password", "123456",
                        "email", "new-user@example.com",
                        "phone", "13800000000"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(200)
                .jsonPath("$.data.token").exists()
                .jsonPath("$.data.userId").isEqualTo(11)
                .jsonPath("$.data.username").isEqualTo("new-user")
                .jsonPath("$.data.role").isEqualTo("USER");
    }

    @Test
    void register_shouldReturnErrorWhenCreateFails() {
        when(userApi.createUser(any(UserCreateDTO.class))).thenReturn(Result.fail(400, "用户名已存在"));

        webTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "username", "exists",
                        "password", "123456"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(400)
                .jsonPath("$.data.token").doesNotExist();
    }

    private String testToken() {
        UserLoginResultDTO user = new UserLoginResultDTO();
        user.setUserId(7L);
        user.setUsername("alice");
        user.setRole("USER");
        user.setStatus((byte) 1);
        return jwtTokenService.generateToken(user);
    }

}
