package com.jspark.pw3_attendant.controller.auth;

import com.jspark.pw3_attendant.service.auth.AuthService;
import com.jspark.pw3_attendant.service.auth.AccountRecoveryService;
import com.jspark.pw3_attendant.service.auth.dto.AdminAccountResponse;
import com.jspark.pw3_attendant.service.auth.dto.LoginRequest;
import com.jspark.pw3_attendant.service.auth.dto.LogoutRequest;
import com.jspark.pw3_attendant.service.auth.dto.RefreshRequest;
import com.jspark.pw3_attendant.service.auth.dto.SignupRequest;
import com.jspark.pw3_attendant.service.auth.dto.TokenResponse;
import com.jspark.pw3_attendant.service.auth.dto.PasswordResetConfirmRequest;
import com.jspark.pw3_attendant.service.auth.dto.PasswordResetRequest;
import com.jspark.pw3_attendant.service.auth.dto.PasswordResetRequestResponse;
import com.jspark.pw3_attendant.service.auth.dto.RecoveryAcceptedResponse;
import com.jspark.pw3_attendant.service.auth.dto.UpdateProfileRequest;
import com.jspark.pw3_attendant.service.auth.dto.UsernameReminderRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "관리자 인증", description = "관리자 가입 신청, 로그인, 토큰 재발급 및 로그아웃 API")
public class AuthController {

    private final AuthService authService;
    private final AccountRecoveryService accountRecoveryService;

    @PostMapping("/signup")
    @SecurityRequirements
    @Operation(summary = "관리자 회원가입 신청", description = "일반 관리자 계정을 승인 대기(PENDING) 상태로 생성합니다. 슈퍼어드민이 승인하기 전에는 로그인할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "가입 신청 완료"),
            @ApiResponse(responseCode = "400", description = "입력값 또는 연락처 형식 오류"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 아이디 또는 이메일")
    })
    public ResponseEntity<AdminAccountResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PutMapping("/resubmit")
    @SecurityRequirements
    @Operation(summary = "거절된 가입 신청 재신청", description = "거절(REJECTED)된 계정을 다시 승인 대기(PENDING) 상태로 변경합니다. 기존 비밀번호가 일치해야 하며 이름과 연락처를 수정할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재신청 완료"),
            @ApiResponse(responseCode = "401", description = "기존 비밀번호 불일치"),
            @ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "거절 상태가 아니거나 이미 사용 중인 이메일")
    })
    public AdminAccountResponse resubmit(@Valid @RequestBody SignupRequest request) {
        return authService.resubmit(request);
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "관리자 로그인", description = "승인(APPROVED)된 관리자만 로그인할 수 있습니다. 성공하면 Access Token과 Refresh Token을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치"),
            @ApiResponse(responseCode = "403", description = "승인 대기 또는 승인 거절 계정")
    })
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token과 Refresh Token을 모두 새로 발급합니다. 기존 Refresh Token은 즉시 폐기됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "만료·폐기되었거나 유효하지 않은 Refresh Token"),
            @ApiResponse(responseCode = "403", description = "승인되지 않은 관리자 계정")
    })
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken(), request.deviceInfo());
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @Operation(summary = "현재 기기 로그아웃", description = "요청한 Refresh Token만 폐기합니다. 다른 기기의 로그인 상태는 유지됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 처리 완료"),
            @ApiResponse(responseCode = "400", description = "Refresh Token 누락")
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "모든 기기에서 로그아웃", description = "현재 관리자의 활성 Refresh Token을 모두 폐기합니다. Bearer Access Token 인증이 필요합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "전체 로그아웃 처리 완료"),
            @ApiResponse(responseCode = "401", description = "Access Token이 없거나 유효하지 않음")
    })
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        authService.logoutAll(Long.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "현재 관리자 정보 조회", description = "Access Token으로 인증된 관리자의 계정·권한·승인 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관리자 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "Access Token이 없거나 유효하지 않음")
    })
    public AdminAccountResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.getMe(Long.valueOf(jwt.getSubject()));
    }

    @PatchMapping("/me")
    @Operation(
            summary = "현재 관리자 정보 수정",
            description = "현재 비밀번호를 확인한 후 이름·이메일·전화번호를 수정합니다. 새 비밀번호는 선택 사항입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원정보 수정 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 형식 오류"),
            @ApiResponse(responseCode = "401", description = "인증 실패 또는 현재 비밀번호 불일치"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일")
    })
    public AdminAccountResponse updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateMe(Long.valueOf(jwt.getSubject()), request);
    }

    @PostMapping("/find-username")
    @SecurityRequirements
    @Operation(
            summary = "아이디 찾기",
            description = "가입한 이름과 이메일 또는 전화번호가 일치하면 등록된 수신처로 아이디를 발송합니다."
    )
    @ApiResponse(responseCode = "202", description = "발송 요청 접수")
    public ResponseEntity<RecoveryAcceptedResponse> findUsername(
            @Valid @RequestBody UsernameReminderRequest request) {
        return ResponseEntity.accepted().body(accountRecoveryService.sendUsernameReminder(request));
    }

    @PostMapping("/password-reset/request")
    @SecurityRequirements
    @Operation(
            summary = "비밀번호 초기화 인증번호 요청",
            description = "아이디와 등록된 이메일 또는 전화번호가 일치하면 6자리 인증번호를 발송합니다."
    )
    @ApiResponse(responseCode = "202", description = "인증번호 발송 요청 접수")
    public ResponseEntity<PasswordResetRequestResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.accepted().body(accountRecoveryService.requestPasswordReset(request));
    }

    @PostMapping("/password-reset/confirm")
    @SecurityRequirements
    @Operation(
            summary = "비밀번호 초기화 완료",
            description = "발급된 요청 ID와 인증번호를 확인한 후 새 비밀번호로 변경하고 모든 Refresh Token을 폐기합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "인증번호 오류·만료 또는 입력값 오류")
    })
    public RecoveryAcceptedResponse confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        return accountRecoveryService.confirmPasswordReset(request);
    }
}
