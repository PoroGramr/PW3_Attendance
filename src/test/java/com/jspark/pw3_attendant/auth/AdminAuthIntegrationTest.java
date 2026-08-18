package com.jspark.pw3_attendant.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jspark.pw3_attendant.domain.admin.AdminAccount;
import com.jspark.pw3_attendant.repository.admin.AdminAccountRepository;
import com.jspark.pw3_attendant.repository.admin.AdminRefreshTokenRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        adminAccountRepository.deleteAll();
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
