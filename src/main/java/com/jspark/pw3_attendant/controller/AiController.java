package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.service.ai.dto.AiChatRequest;
import com.jspark.pw3_attendant.service.ai.dto.AiChatResponse;
import com.jspark.pw3_attendant.service.ai.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Chat", description = "AI 기반 출석 관리 질의응답 API")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiChatService aiChatService;

    @Operation(summary = "AI 채팅", description = "자연어 질문을 통해 출석 데이터를 조회합니다")
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        String answer = aiChatService.chatWithAgent(request.getQuestion());
        return ResponseEntity.ok(new AiChatResponse(answer));
    }
}
