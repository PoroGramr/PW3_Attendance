package com.jspark.pw3_attendant.controller.admin;

import com.jspark.pw3_attendant.service.message.MessageDispatchService;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto;
import com.jspark.pw3_attendant.service.message.dto.MessageSendResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageDispatchService messageDispatchService;

    @PostMapping("/send")
    @Operation(summary = "메시지 일괄 발송 (관리자)")
    // TODO: Add security check to ensure only ADMIN/TEACHER can access this.
    public ResponseEntity<MessageSendResponseDto> sendMessage(@RequestBody MessageRequestDto request) {
        MessageSendResponseDto response = messageDispatchService.dispatchMessage(request);
        return ResponseEntity.ok(response);
    }
}
