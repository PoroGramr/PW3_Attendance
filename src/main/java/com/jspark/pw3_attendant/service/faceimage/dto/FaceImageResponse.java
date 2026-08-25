package com.jspark.pw3_attendant.service.faceimage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record FaceImageResponse(
        @Schema(description = "얼굴 사진을 조회할 수 있는 임시 URL")
        String url,

        @Schema(description = "임시 URL 만료 시각")
        Instant expiresAt
) {
}
