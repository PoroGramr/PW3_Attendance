package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.service.qr.QrService;
import com.jspark.pw3_attendant.service.qr.dto.QrResolveResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/qr")
public class PublicQrController {

    private final QrService qrService;

    @GetMapping("/resolve")
    @Operation(summary = "학생 QR 코드 정보 조회 (공개)")
    public ResponseEntity<QrResolveResponseDto> resolveQr(@RequestParam String secret) {
        try {
            QrResolveResponseDto response = qrService.resolveQr(secret);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
