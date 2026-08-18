package com.jspark.pw3_attendant.service.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Schema(description = "로그인 아이디. 저장 시 소문자로 정규화됩니다.", example = "admin01")
        @NotBlank
        @Size(min = 4, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "아이디는 영문, 숫자, 마침표, 밑줄, 하이픈만 사용할 수 있습니다.")
        String username,

        @Schema(description = "비밀번호. 재신청에서는 기존 비밀번호를 입력합니다.", example = "admin-password")
        @NotBlank
        @Size(min = 8, max = 72)
        String password,

        @Schema(description = "관리자 이름", example = "홍길동")
        @NotBlank
        @Size(max = 50)
        String name,

        @Schema(description = "관리자 이메일. 저장 시 소문자로 정규화됩니다.", example = "admin@example.com")
        @NotBlank
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 254)
        String email,

        @Schema(description = "연락처. 숫자만 정규화하여 저장합니다.", example = "010-1234-5678")
        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "^[0-9+() -]+$", message = "올바른 연락처 형식이 아닙니다.")
        String phone
) {
}
