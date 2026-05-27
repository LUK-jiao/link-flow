package com.linkflow.user;

import com.linkflow.api.ApproverConfigApi;
import com.linkflow.api.UserApi;
import com.linkflow.api.dto.common.Result;
import com.linkflow.api.dto.user.ApproverDTO;
import com.linkflow.api.dto.user.UserCreateDTO;
import com.linkflow.api.dto.user.UserDTO;
import com.linkflow.api.dto.user.UserLoginDTO;
import com.linkflow.api.dto.user.UserLoginResultDTO;
import com.linkflow.user.mapper.UserMapper;
import com.linkflow.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
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

    @Autowired
    private UserMapper userMapper;

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
    void loginShouldReturnUserInfoWhenPasswordMatches() {
        String username = uniqueName("login-user");
        Long userId = createUser(username, "123456", "USER", (byte) 1);

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername(username);
        loginDTO.setPassword("123456");

        Result<UserLoginResultDTO> result = userApi.login(loginDTO);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().getUserId()).isEqualTo(userId);
        assertThat(result.getData().getUsername()).isEqualTo(username);
        assertThat(result.getData().getRole()).isEqualTo("USER");
    }

    @Test
    void loginShouldRejectWrongPasswordAndDisabledUser() {
        String username = uniqueName("login-disabled");
        createUser(username, "123456", "USER", (byte) 0);

        UserLoginDTO disabledUserLogin = new UserLoginDTO();
        disabledUserLogin.setUsername(username);
        disabledUserLogin.setPassword("123456");

        Result<UserLoginResultDTO> disabledResult = userApi.login(disabledUserLogin);
        assertThat(disabledResult.getCode()).isEqualTo(403);

        UserLoginDTO wrongPasswordLogin = new UserLoginDTO();
        wrongPasswordLogin.setUsername("not-exists");
        wrongPasswordLogin.setPassword("bad");

        Result<UserLoginResultDTO> wrongPasswordResult = userApi.login(wrongPasswordLogin);
        assertThat(wrongPasswordResult.getCode()).isEqualTo(401);
    }

    @Test
    void createUserShouldStoreBcryptPassword() {
        String username = uniqueName("bcrypt-user");
        createUser(username, "123456", "USER", (byte) 1);

        User user = userMapper.selectByUsername(username);

        assertThat(user.getPassword()).startsWith("$2");
        assertThat(user.getPassword()).isNotEqualTo("123456");
    }

    @Test
    void loginShouldUpgradeLegacyPlaintextPassword() {
        String username = uniqueName("legacy-user");
        User user = new User();
        user.setUsername(username);
        user.setPassword("legacy123");
        user.setEmail(username + "@example.com");
        user.setPhone("13600000000");
        user.setRole("USER");
        user.setStatus((byte) 1);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        userMapper.insert(user);

        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername(username);
        loginDTO.setPassword("legacy123");

        Result<UserLoginResultDTO> result = userApi.login(loginDTO);

        assertThat(result.isSuccess()).isTrue();
        User upgradedUser = userMapper.selectByUsername(username);
        assertThat(upgradedUser.getPassword()).startsWith("$2");
        assertThat(upgradedUser.getPassword()).isNotEqualTo("legacy123");
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

    private Long createUser(String username, String password, String role, Byte status) {
        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setUsername(username);
        createDTO.setPassword(password);
        createDTO.setEmail(username + "@example.com");
        createDTO.setPhone("13700000000");
        createDTO.setRole(role);

        Result<Long> createResult = userApi.createUser(createDTO);
        assertThat(createResult.isSuccess()).isTrue();

        if (status != null && status != 1) {
            Result<Void> statusResult = userApi.updateUserStatus(createResult.getData(), status);
            assertThat(statusResult.isSuccess()).isTrue();
        }
        return createResult.getData();
    }

    private String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }
}
