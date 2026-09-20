package com.jspark.pw3_attendant.service.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Schema(description = "현재 비밀번호", example = "current-password")
        @NotBlank
        String currentPassword,

        @Schema(description = "관리자 이름", example = "홍길동")
        @NotBlank
        @Size(max = 50)
        String name,

        @Schema(description = "변경할 이메일", example = "admin@example.com")
        @NotBlank
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 254)
        String email,

        @Schema(description = "변경할 연락처", example = "010-1234-5678")
        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "^[0-9+() -]+$", message = "올바른 연락처 형식이 아닙니다.")
        String phone,

        @Schema(description = "변경할 비밀번호. 변경하지 않으면 생략합니다.", example = "new-password")
        @Size(min = 8, max = 72)
        String newPassword
) {
}
