package com.jspark.pw3_attendant.controller.admin;

import com.jspark.pw3_attendant.service.integration.acts29.Acts29AttendanceSyncService;
import com.jspark.pw3_attendant.service.integration.acts29.dto.Acts29AttendanceSyncRequest;
import com.jspark.pw3_attendant.service.integration.acts29.dto.Acts29AttendanceSyncResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/integrations/acts29")
public class Acts29AttendanceSyncController {

    private final Acts29AttendanceSyncService syncService;

    @PostMapping("/attendances/{date}/sync")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "로컬 출석 데이터를 Acts29에 비교 또는 업로드")
    public Acts29AttendanceSyncResponse syncAttendance(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody(required = false) Acts29AttendanceSyncRequest request
    ) {
        return syncService.sync(date, request);
    }
}
