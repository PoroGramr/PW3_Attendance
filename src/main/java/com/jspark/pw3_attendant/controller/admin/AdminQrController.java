package com.jspark.pw3_attendant.controller.admin;

import com.jspark.pw3_attendant.service.qr.QrService;
import com.jspark.pw3_attendant.service.qr.dto.SendQrRequestDto;
import com.jspark.pw3_attendant.service.qr.dto.SendQrResponseDto;
import com.jspark.pw3_attendant.service.qr.dto.StudentQrResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/qr")
@RequiredArgsConstructor
public class AdminQrController {

    private final QrService qrService;

    @PostMapping("/send")
    @Operation(summary = "학생들에게 QR 코드 링크 일괄 발송 (관리자)")
    // TODO: Add security check to ensure only ADMIN
    public ResponseEntity<SendQrResponseDto> sendQrLinks(@RequestBody SendQrRequestDto request) {
        SendQrResponseDto response = qrService.sendQrLinks(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/class/{classRoomId}")
    @Operation(summary = "반 학생들의 QR 코드 정보 조회 (관리자)")
    // TODO: Add security check to ensure only ADMIN/TEACHER can access this.
    public ResponseEntity<List<StudentQrResponseDto>> getStudentQrsForClass(
        @PathVariable Long classRoomId,
        @RequestParam Integer schoolYear
    ) {
        List<StudentQrResponseDto> response = qrService.getStudentQrsForClass(classRoomId, schoolYear);
        return ResponseEntity.ok(response);
    }
}
