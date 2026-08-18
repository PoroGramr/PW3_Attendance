package com.jspark.pw3_attendant.service.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAdminRequest(
        @Schema(description = "가입 승인 거절 사유", example = "관리자 등록 대상 여부를 확인할 수 없습니다.")
        @NotBlank @Size(max = 500) String reason
) {
}
