package com.jspark.pw3_attendant.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jspark.pw3_attendant.controller.admin.Acts29AttendanceSyncController;
import com.jspark.pw3_attendant.service.integration.acts29.Acts29AttendanceSyncService;
import com.jspark.pw3_attendant.service.integration.acts29.dto.Acts29AttendanceSyncResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(Acts29AttendanceSyncController.class)
@Import(Acts29AttendanceSyncControllerTest.MethodSecurityTestConfig.class)
class Acts29AttendanceSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Acts29AttendanceSyncService syncService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("관리자는 요청 본문 없이 기본 dry-run을 호출할 수 있다")
    void adminCanRunDefaultDryRun() throws Exception {
        LocalDate date = LocalDate.of(2026, 10, 4);
        Acts29AttendanceSyncResponse response = new Acts29AttendanceSyncResponse(
                date,
                2026,
                null,
                "DRY_RUN",
                true,
                178,
                178,
                178,
                10,
                0,
                3,
                13,
                List.of(),
                List.of()
        );
        given(syncService.sync(date, null)).willReturn(response);

        mockMvc.perform(post("/api/integrations/acts29/attendances/{date}/sync", date)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("DRY_RUN"))
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.changedCount").value(10));

        verify(syncService).sync(date, null);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("일반 사용자는 Acts29 동기화 API를 호출할 수 없다")
    void regularUserCannotSync() throws Exception {
        mockMvc.perform(post("/api/integrations/acts29/attendances/{date}/sync", "2026-10-04")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
