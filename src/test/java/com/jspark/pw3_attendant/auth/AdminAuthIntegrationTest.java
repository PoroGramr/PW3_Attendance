package com.jspark.pw3_attendant.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jspark.pw3_attendant.domain.admin.AdminAccount;
import com.jspark.pw3_attendant.domain.admin.RecoveryChannel;
import com.jspark.pw3_attendant.repository.admin.AdminAccountRecoveryRepository;
import com.jspark.pw3_attendant.repository.admin.AdminAccountRepository;
import com.jspark.pw3_attendant.repository.admin.AdminRefreshTokenRepository;
import com.jspark.pw3_attendant.service.auth.AccountRecoveryNotifier;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.mockito.ArgumentCaptor;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminAccountRepository adminAccountRepository;

    @Autowired
    private AdminRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AdminAccountRecoveryRepository recoveryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AccountRecoveryNotifier recoveryNotifier;

    @BeforeEach
    void setUp() {
        recoveryRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        adminAccountRepository.deleteAll();
        reset(recoveryNotifier);
        adminAccountRepository.save(AdminAccount.superAdmin(
                "rootadmin",
                passwordEncoder.encode("super-password"),
                "슈퍼 관리자",
                "root@example.com",
                "01000000000"
        ));
    }

    @Test
    void signupApprovalLoginAndRefreshRotation() throws Exception {
        Map<String, String> signup = Map.of(
                "username", "New.Admin",
                "password", "admin-password",
                "name", "신규 관리자",
                "email", "New.Admin@Example.com",
                "phone", "010-1234-5678"
        );

        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(signup)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new.admin"))
                .andExpect(jsonPath("$.email").value("new.admin@example.com"))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"))
                .andReturn();
        Long adminId = objectMapper.readTree(signupResult.getResponse().getContentAsByteArray()).get("id").asLong();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "new.admin",
                                "password", "admin-password"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_APPROVAL_PENDING"));

        JsonNode superTokens = login("rootadmin", "super-password");
        mockMvc.perform(patch("/api/super-admin/admins/{id}/approve", adminId)
                        .header("Authorization", "Bearer " + superTokens.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));

        JsonNode adminTokens = login("new.admin", "admin-password");
        String oldRefreshToken = adminTokens.get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("refreshToken", oldRefreshToken))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode rotatedTokens = objectMapper.readTree(refreshResult.getResponse().getContentAsByteArray());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("refreshToken", oldRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + rotatedTokens.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new.admin"))
                .andExpect(jsonPath("$.email").value("new.admin@example.com"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "another.admin",
                                "password", "admin-password",
                                "name", "다른 관리자",
                                "email", "NEW.ADMIN@example.com",
                                "phone", "010-2222-3333"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void loginAndSwaggerArePublicButBusinessApiRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "unknown",
                                "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void authenticatedAdminCanUpdateProfileAndPassword() throws Exception {
        JsonNode tokens = login("rootadmin", "super-password");

        mockMvc.perform(patch("/api/auth/me")
                        .header("Authorization", "Bearer " + tokens.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "currentPassword", "super-password",
                                "name", "수정된 관리자",
                                "email", "changed@example.com",
                                "phone", "010-9999-8888",
                                "newPassword", "changed-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("수정된 관리자"))
                .andExpect(jsonPath("$.email").value("changed@example.com"))
                .andExpect(jsonPath("$.phone").value("01099998888"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "rootadmin",
                                "password", "super-password"))))
                .andExpect(status().isUnauthorized());
        login("rootadmin", "changed-password");
    }

    @Test
    void usernameReminderSendsUsernameToRegisteredEmail() throws Exception {
        mockMvc.perform(post("/api/auth/find-username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "name", "슈퍼 관리자",
                                "channel", "EMAIL",
                                "destination", "ROOT@example.com"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());

        verify(recoveryNotifier).send(
                eq(RecoveryChannel.EMAIL),
                eq("root@example.com"),
                anyString(),
                eq("[PW3] 가입 아이디 안내\n아이디: rootadmin"));
    }

    @Test
    void passwordResetCodeChangesPasswordAndRevokesExistingRefreshTokens() throws Exception {
        JsonNode oldTokens = login("rootadmin", "super-password");

        MvcResult requestResult = mockMvc.perform(post("/api/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", "ROOTADMIN",
                                "channel", "SMS",
                                "destination", "010-0000-0000"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").exists())
                .andReturn();
        String requestId = objectMapper.readTree(requestResult.getResponse().getContentAsByteArray())
                .get("requestId").asText();

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(recoveryNotifier).send(
                eq(RecoveryChannel.SMS),
                eq("01000000000"),
                anyString(),
                contentCaptor.capture());
        Matcher codeMatcher = Pattern.compile("(?<![0-9])[0-9]{6}(?![0-9])")
                .matcher(contentCaptor.getValue());
        if (!codeMatcher.find()) {
            throw new AssertionError("인증번호를 찾을 수 없습니다.");
        }

        mockMvc.perform(post("/api/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "requestId", requestId,
                                "verificationCode", codeMatcher.group(),
                                "newPassword", "reset-password"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "refreshToken", oldTokens.get("refreshToken").asText()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
        login("rootadmin", "reset-password");
    }

    private JsonNode login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", username,
                                "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
