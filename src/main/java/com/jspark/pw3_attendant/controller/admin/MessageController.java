package com.jspark.pw3_attendant.controller.admin;

import com.jspark.pw3_attendant.service.message.MessageDispatchService;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto;
import com.jspark.pw3_attendant.service.message.dto.MessageSendResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.extern.slf4j.XSlf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {

    private final MessageDispatchService messageDispatchService;

    @PostMapping(value = "/send"
        , consumes = {"multipart/form-data"}
    )
    @Operation(summary = "메시지 일괄 발송 (관리자)")
    // TODO: Add security check to ensure only ADMIN/TEACHER can access this.
    public ResponseEntity<MessageSendResponseDto> sendMessage(
        @RequestPart("messageRequest") MessageRequestDto request
        , @RequestPart(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        log.info("MessageController: Received imageFile. Name: {}, Size: {}, Empty: {}",
        (imageFile != null ? imageFile.getOriginalFilename() : "null"),
        (imageFile != null ? imageFile.getSize() : 0),
        (imageFile != null ? imageFile.isEmpty() : true));

        if (imageFile != null && !imageFile.isEmpty()) {
            request.getContent().setImageFile(imageFile);
        }
        MessageSendResponseDto response = messageDispatchService.dispatchMessage(request);
        return ResponseEntity.ok(response);
    }
}
