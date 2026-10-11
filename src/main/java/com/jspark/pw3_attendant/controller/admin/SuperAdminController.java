package com.jspark.pw3_attendant.controller.admin;

import com.jspark.pw3_attendant.domain.admin.ApprovalStatus;
import com.jspark.pw3_attendant.service.auth.AdminManagementService;
import com.jspark.pw3_attendant.service.auth.dto.AdminAccountResponse;
import com.jspark.pw3_attendant.service.auth.dto.AdminPasswordResetRequest;
import com.jspark.pw3_attendant.service.auth.dto.RejectAdminRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/super-admin/admins")
@Tag(name = "슈퍼어드민 계정 관리", description = "관리자 가입 신청 조회, 승인 및 거절 API")
public class SuperAdminController {

    private final AdminManagementService adminManagementService;

    @PostMapping("/{id}/password-reset")
    @Operation(summary = "일반 관리자 비밀번호 초기화", description = "승인된 슈퍼어드민이 일반 관리자의 새 비밀번호를 지정합니다. 승인 상태는 유지하며 모든 Refresh Token과 미사용 복구 인증번호를 폐기합니다. 이미 발급된 Access Token은 만료까지 유효합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "초기화 완료"),
            @ApiResponse(responseCode = "400", description = "비밀번호 형식 오류 또는 슈퍼어드민 대상"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "승인된 슈퍼어드민이 아님"),
            @ApiResponse(responseCode = "404", description = "대상 계정 없음")
    })
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminPasswordResetRequest request) {
        adminManagementService.resetPassword(id, Long.valueOf(jwt.getSubject()), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "상태별 관리자 목록 조회", description = "승인 상태에 해당하는 관리자 목록을 신청 일시 오름차순으로 조회합니다. status를 생략하면 PENDING 계정을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "Access Token이 없거나 유효하지 않음"),
            @ApiResponse(responseCode = "403", description = "슈퍼어드민 권한이 없음")
    })
    public List<AdminAccountResponse> findAdmins(
            @Parameter(description = "조회할 승인 상태", example = "PENDING")
            @RequestParam(defaultValue = "PENDING") ApprovalStatus status) {
        return adminManagementService.findByStatus(status);
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "관리자 가입 승인", description = "승인 대기(PENDING) 중인 일반 관리자를 승인(APPROVED)합니다. 승인 직후부터 로그인할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "승인 완료"),
            @ApiResponse(responseCode = "400", description = "일반 관리자가 아닌 승인 대상"),
            @ApiResponse(responseCode = "403", description = "슈퍼어드민 권한이 없음"),
            @ApiResponse(responseCode = "404", description = "관리자 계정을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "승인 대기 상태가 아님")
    })
    public AdminAccountResponse approve(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return adminManagementService.approve(id, Long.valueOf(jwt.getSubject()));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "관리자 가입 거절", description = "승인 대기(PENDING) 중인 관리자의 가입을 거절(REJECTED)하고 사유를 기록합니다. 거절된 관리자는 정보를 수정해 재신청할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "거절 처리 완료"),
            @ApiResponse(responseCode = "400", description = "거절 사유 누락 또는 일반 관리자가 아닌 대상"),
            @ApiResponse(responseCode = "403", description = "슈퍼어드민 권한이 없음"),
            @ApiResponse(responseCode = "404", description = "관리자 계정을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "승인 대기 상태가 아님")
    })
    public AdminAccountResponse reject(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RejectAdminRequest request) {
        return adminManagementService.reject(id, Long.valueOf(jwt.getSubject()), request.reason());
    }
}
