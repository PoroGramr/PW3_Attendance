package com.jspark.pw3_attendant.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jspark.pw3_attendant.domain.admin.RecoveryChannel;
import com.jspark.pw3_attendant.repository.admin.AdminAccountRepository;
import com.jspark.pw3_attendant.repository.admin.AdminAccountRecoveryRepository;
import com.jspark.pw3_attendant.repository.admin.AdminRefreshTokenRepository;
import com.jspark.pw3_attendant.service.auth.AccountRecoveryNotifier;
import com.jspark.pw3_attendant.service.auth.AuthService;
import com.jspark.pw3_attendant.service.auth.dto.SignupRequest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "app.signup-notification.recipient=pjs9177@naver.com")
@AutoConfigureMockMvc
class SignupNotificationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AdminAccountRepository accounts;
    @Autowired AdminAccountRecoveryRepository recoveries;
    @Autowired AdminRefreshTokenRepository tokens;
    @Autowired AuthService authService;
    @Autowired PlatformTransactionManager transactionManager;
    @MockBean AccountRecoveryNotifier notifier;

    @BeforeEach
    void setup() {
        recoveries.deleteAll();
        tokens.deleteAll();
        accounts.findAll().stream().filter(a -> a.getApprovedBy() != null).forEach(accounts::delete);
        accounts.deleteAll();
        reset(notifier);
    }

    @Test
    void committedSignupEmailsDesignatedSuperAdminWithoutPassword() throws Exception {
        signup().andExpect(status().isCreated());
        ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
        verify(notifier).send(eq(RecoveryChannel.EMAIL), eq("pjs9177@naver.com"),
                eq("[PW3] 관리자 회원가입 승인 요청"), content.capture());
        assertTrue(content.getValue().contains("new.admin"));
        assertTrue(content.getValue().contains("신규 관리자"));
        assertTrue(content.getValue().contains("new.admin@example.com"));
        assertTrue(content.getValue().contains("01012345678"));
        assertFalse(content.getValue().contains("secret-password"));
        assertFalse(content.getValue().contains(accounts.findByUsername("new.admin").orElseThrow().getPasswordHash()));
    }

    @Test
    void duplicateAndInvalidSignupsDoNotNotify() throws Exception {
        signup().andExpect(status().isCreated());
        reset(notifier);
        signup().andExpect(status().isConflict());
        mvc.perform(post("/api/auth/signup").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(notifier);
    }

    @Test
    void rolledBackSignupDoesNotNotify() {
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager).execute(status -> {
            authService.signup(new SignupRequest("new.admin", "secret-password", "신규 관리자",
                    "new.admin@example.com", "01012345678"));
            throw new IllegalStateException("rollback");
        }));
        assertFalse(accounts.existsByUsername("new.admin"));
        verifyNoInteractions(notifier);
    }

    @Test
    void mailFailureDoesNotRollbackSignupOrFailTheResponse() throws Exception {
        doThrow(new IllegalStateException("SMTP unavailable")).when(notifier)
                .send(eq(RecoveryChannel.EMAIL), anyString(), anyString(), anyString());
        signup().andExpect(status().isCreated());
        assertTrue(accounts.existsByUsername("new.admin"));
        verify(notifier).send(eq(RecoveryChannel.EMAIL), eq("pjs9177@naver.com"), anyString(), anyString());
    }

    private org.springframework.test.web.servlet.ResultActions signup() throws Exception {
        return mvc.perform(post("/api/auth/signup").contentType("application/json")
                .content(mapper.writeValueAsBytes(Map.of("username", "New.Admin", "password", "secret-password",
                        "name", "신규 관리자", "email", "New.Admin@Example.com", "phone", "010-1234-5678"))));
    }
}
