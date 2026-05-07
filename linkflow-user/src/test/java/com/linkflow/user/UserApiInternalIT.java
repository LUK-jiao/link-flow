package com.linkflow.user;

import com.linkflow.api.ApproverConfigApi;
import com.linkflow.api.UserApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.ApproverDTO;
import com.linkflow.api.dto.user.UserCreateDTO;
import com.linkflow.api.dto.user.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest(
        classes = UserApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.web-application-type=none",
                "dubbo.registry.address=N/A",
                "dubbo.protocol.port=-1",
                "dubbo.application.qos-enable=false"
        }
)
class UserApiInternalIT {

    @Autowired
    private UserApi userApi;

    @Autowired
    private ApproverConfigApi approverConfigApi;

    @Test
    void userApiFullLifecycle() {
        String username = uniqueName("user-api");

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername(username);
        createDTO.setPassword("123456");
        createDTO.setEmail(username + "@example.com");
        createDTO.setPhone("13800000000");

        Result<Long> createResult = userApi.createUser(createDTO);
        assertThat(createResult.isSuccess()).isTrue();
        assertThat(createResult.getData()).isNotNull();

        Long userId = createResult.getData();
        Result<UserDTO> byIdResult = userApi.getUserById(userId);
        assertThat(byIdResult.isSuccess()).isTrue();
        assertThat(byIdResult.getData().getUsername()).isEqualTo(username);
        assertThat(byIdResult.getData().getRole()).isEqualTo("USER");
        assertThat(byIdResult.getData().getStatus()).isEqualTo((byte) 1);

        Result<UserDTO> byUsernameResult = userApi.getUserByUsername(username);
        assertThat(byUsernameResult.isSuccess()).isTrue();
        assertThat(byUsernameResult.getData().getId()).isEqualTo(userId);

        Result<Void> updateStatusResult = userApi.updateUserStatus(userId, (byte) 0);
        assertThat(updateStatusResult.isSuccess()).isTrue();

        Result<UserDTO> updatedResult = userApi.getUserById(userId);
        assertThat(updatedResult.isSuccess()).isTrue();
        assertThat(updatedResult.getData().getStatus()).isEqualTo((byte) 0);
    }

    @Test
    void approverConfigApiFullLifecycle() {
        Long approverId = createApproverUser();
        String campaignType = uniqueName("campaign-type");

        ApproverDTO configDTO = new ApproverDTO();
        configDTO.setCampaignType(campaignType);
        configDTO.setApproverId(approverId);
        configDTO.setApproverLevel(1);

        Result<Long> configResult = approverConfigApi.configApprover(configDTO);
        assertThat(configResult.isSuccess()).isTrue();
        assertThat(configResult.getData()).isNotNull();

        Long configId = configResult.getData();
        Result<List<ApproverDTO>> byTypeResult = approverConfigApi.getApproverByType(campaignType);
        assertThat(byTypeResult.isSuccess()).isTrue();
        assertThat(byTypeResult.getData())
                .extracting(ApproverDTO::getId)
                .contains(configId);

        Result<List<ApproverDTO>> byTypeAndLevelResult =
                approverConfigApi.getApproverByTypeAndLevel(campaignType, 1);
        assertThat(byTypeAndLevelResult.isSuccess()).isTrue();
        assertThat(byTypeAndLevelResult.getData())
                .extracting(ApproverDTO::getApproverId)
                .contains(approverId);

        Result<Void> deleteResult = approverConfigApi.deleteApproverConfig(configId);
        assertThat(deleteResult.isSuccess()).isTrue();

        Result<List<ApproverDTO>> afterDeleteResult =
                approverConfigApi.getApproverByTypeAndLevel(campaignType, 1);
        assertThat(afterDeleteResult.isSuccess()).isTrue();
        assertThat(afterDeleteResult.getData())
                .extracting(ApproverDTO::getId)
                .doesNotContain(configId);
    }

    private Long createApproverUser() {
        String username = uniqueName("approver");

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername(username);
        createDTO.setPassword("123456");
        createDTO.setEmail(username + "@example.com");
        createDTO.setPhone("13900000000");
        createDTO.setRole("APPROVER");

        Result<Long> result = userApi.createUser(createDTO);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        return result.getData();
    }

    private String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
