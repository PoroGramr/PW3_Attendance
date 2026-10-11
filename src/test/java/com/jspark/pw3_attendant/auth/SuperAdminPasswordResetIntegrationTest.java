package com.jspark.pw3_attendant.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jspark.pw3_attendant.domain.admin.AdminAccount;
import com.jspark.pw3_attendant.domain.admin.AdminAccountRecovery;
import com.jspark.pw3_attendant.domain.admin.RecoveryChannel;
import com.jspark.pw3_attendant.repository.admin.AdminAccountRepository;
import com.jspark.pw3_attendant.repository.admin.AdminAccountRecoveryRepository;
import com.jspark.pw3_attendant.repository.admin.AdminRefreshTokenRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SuperAdminPasswordResetIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AdminAccountRepository accounts;
    @Autowired AdminAccountRecoveryRepository recoveries;
    @Autowired AdminRefreshTokenRepository tokens;
    @Autowired PasswordEncoder encoder;
    private AdminAccount root;
    private AdminAccount target;
    private String rootToken;

    @BeforeEach
    void setup() throws Exception {
        recoveries.deleteAll();
        tokens.deleteAll();
        // Approval has a self-referencing FK; delete children first.
        accounts.findAll().stream().filter(a -> a.getApprovedBy() != null).forEach(accounts::delete);
        accounts.deleteAll();
        root = accounts.save(AdminAccount.superAdmin("root", encoder.encode("root-password"),
                "슈퍼어드민", "root@example.com", "01000000000"));
        target = AdminAccount.pending("target", encoder.encode("old-password"),
                "관리자", "target@example.com", "01011111111");
        target.approve(root);
        target = accounts.save(target);
        rootToken = login("root", "root-password").get("accessToken").asText();
    }

    @Test
    void resetsPasswordRevokesAllRefreshTokensAndRecoveryCodesWithoutChangingApproval() throws Exception {
        JsonNode first = login("target", "old-password");
        JsonNode second = login("target", "old-password");
        AdminAccountRecovery recovery = recoveries.save(AdminAccountRecovery.passwordReset(target,
                RecoveryChannel.EMAIL, encoder.encode("123456"), LocalDateTime.now().plusMinutes(10)));
        mvc.perform(post("/api/super-admin/admins/{id}/password-reset", target.getId())
                .header("Authorization", "Bearer " + rootToken)
                .contentType("application/json").content(mapper.writeValueAsBytes(Map.of("newPassword", "reset-password"))))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        AdminAccount updated = accounts.findById(target.getId()).orElseThrow();
        assertTrue(encoder.matches("reset-password", updated.getPasswordHash()));
        assertNotEquals("reset-password", updated.getPasswordHash());
        assertEquals(target.getApprovalStatus(), updated.getApprovalStatus());
        for (JsonNode old : new JsonNode[]{first, second}) {
            mvc.perform(post("/api/auth/refresh").contentType("application/json")
                    .content(mapper.writeValueAsBytes(Map.of("refreshToken", old.get("refreshToken").asText()))))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(mapper.writeValueAsBytes(Map.of("username", "target", "password", "old-password"))))
                .andExpect(status().isUnauthorized());
        login("target", "reset-password");
        mvc.perform(post("/api/auth/password-reset/confirm").contentType("application/json")
                .content(mapper.writeValueAsBytes(Map.of("requestId", recovery.getPublicId(),
                        "verificationCode", "123456", "newPassword", "attacker-password"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousAndOrdinaryAdminCannotResetPasswords() throws Exception {
        mvc.perform(post("/api/super-admin/admins/{id}/password-reset", target.getId())
                .contentType("application/json").content("{\"newPassword\":\"reset-password\"}"))
                .andExpect(status().isUnauthorized());
        String token = login("target", "old-password").get("accessToken").asText();
        mvc.perform(post("/api/super-admin/admins/{id}/password-reset", target.getId())
                .header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"newPassword\":\"reset-password\"}"))
                .andExpect(status().isForbidden());
        assertTrue(encoder.matches("old-password", accounts.findById(target.getId()).orElseThrow().getPasswordHash()));
    }

    @Test
    void invalidPasswordsMissingAccountsAndSuperAdminTargetsAreRejected() throws Exception {
        for (String password : new String[]{"", "short", " ".repeat(8), "a".repeat(73), "가".repeat(25)}) {
            mvc.perform(post("/api/super-admin/admins/{id}/password-reset", target.getId())
                    .header("Authorization", "Bearer " + rootToken)
                    .contentType("application/json").content(mapper.writeValueAsBytes(Map.of("newPassword", password))))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/super-admin/admins/{id}/password-reset", target.getId())
                .header("Authorization", "Bearer " + rootToken).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/super-admin/admins/{id}/password-reset", Long.MAX_VALUE)
                .header("Authorization", "Bearer " + rootToken)
                .contentType("application/json").content("{\"newPassword\":\"reset-password\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/super-admin/admins/{id}/password-reset", root.getId())
                .header("Authorization", "Bearer " + rootToken)
                .contentType("application/json").content("{\"newPassword\":\"reset-password\"}"))
                .andExpect(status().isBadRequest());
        assertTrue(encoder.matches("old-password", accounts.findById(target.getId()).orElseThrow().getPasswordHash()));
    }

    @Test
    void pendingAndRejectedAccountsRemainUnableToLoginAfterReset() throws Exception {
        for (boolean reject : new boolean[]{false, true}) {
            AdminAccount account = AdminAccount.pending("pending" + reject, encoder.encode("old-password"),
                    "대기 관리자", "pending" + reject + "@example.com", "01022222222");
            if (reject) account.reject(root, "거절");
            account = accounts.save(account);
            mvc.perform(post("/api/super-admin/admins/{id}/password-reset", account.getId())
                    .header("Authorization", "Bearer " + rootToken)
                    .contentType("application/json").content("{\"newPassword\":\"reset-password\"}"))
                    .andExpect(status().isNoContent());
            mvc.perform(post("/api/auth/login").contentType("application/json")
                    .content(mapper.writeValueAsBytes(Map.of("username", account.getUsername(), "password", "reset-password"))))
                    .andExpect(status().isForbidden());
            assertEquals(account.getApprovalStatus(), accounts.findById(account.getId()).orElseThrow().getApprovalStatus());
        }
    }

    @Test
    void databaseRoleIsCheckedEvenWhenJwtClaimsSuperAdmin() throws Exception {
        mvc.perform(post("/api/super-admin/admins/{id}/password-reset", target.getId())
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> jwt.subject(target.getId().toString()))
                        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                .contentType("application/json").content("{\"newPassword\":\"reset-password\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUPER_ADMIN_REQUIRED"));
        assertTrue(encoder.matches("old-password", accounts.findById(target.getId()).orElseThrow().getPasswordHash()));
    }

    private JsonNode login(String username, String password) throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(mapper.writeValueAsBytes(Map.of("username", username, "password", password))))
                .andExpect(status().isOk()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsByteArray());
    }
}
