package com.jspark.pw3_attendant.service.notification;

import com.jspark.pw3_attendant.common.logging.ErrorLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorNotificationService {

    private final RestTemplate restTemplate;

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

    @Value("${discord.webhook.url}")
    private String discordWebhookUrl;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Async
    @EventListener
    public void handleErrorLog(ErrorLogEvent event) {
        try {
            String analysis = analyzeWithGemini(event.getMessage(), event.getStackTrace());
            sendToDiscord(event, analysis);
        } catch (Exception e) {
            log.warn("[ErrorNotificationService] Discord 알림 전송 실패: {}", e.getMessage());
        }
    }

    private String analyzeWithGemini(String message, String stackTrace) {
        String prompt = String.format("""
                다음 Java 서버 에러 로그를 분석해서 아래 형식으로 한국어로 답해줘:

                [원인]
                (에러가 발생한 이유를 2-3줄로 간결하게)

                [해결 방법]
                (구체적인 해결 방법을 2-3줄로)

                에러 메시지: %s

                스택 트레이스:
                %s
                """, message, stackTrace);

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    GEMINI_URL + geminiApiKey,
                    HttpMethod.POST,
                    entity,
                    Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> contentMap = (Map<String, Object>) candidates.get(0).get("content");
                    List<Map<String, String>> partsList = (List<Map<String, String>>) contentMap.get("parts");
                    if (partsList != null && !partsList.isEmpty()) {
                        return partsList.get(0).get("text");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[ErrorNotificationService] Gemini API 호출 실패: {}", e.getMessage());
        }

        return "Gemini 분석 실패 - 로그를 직접 확인해주세요.";
    }

    private void sendToDiscord(ErrorLogEvent event, String analysis) {
        String occurredAt = event.getOccurredAt().format(FORMATTER);
        String causeText = extractSection(analysis, "[원인]", "[해결 방법]");
        String solutionText = extractSection(analysis, "[해결 방법]", null);

        Map<String, Object> payload = Map.of(
                "username", "🚨 에러 모니터",
                "embeds", List.of(Map.of(
                        "title", "❌ 서버 에러 발생",
                        "color", 15548997,
                        "fields", List.of(
                                Map.of(
                                        "name", "📋 에러 메시지",
                                        "value", truncate(event.getMessage(), 1000),
                                        "inline", false),
                                Map.of(
                                        "name", "🔍 원인",
                                        "value", truncate(causeText.isBlank() ? analysis : causeText, 1000),
                                        "inline", false),
                                Map.of(
                                        "name", "✅ 해결 방법",
                                        "value", truncate(solutionText.isBlank() ? "-" : solutionText, 1000),
                                        "inline", false)),
                        "footer", Map.of("text", "발생 시각: " + occurredAt))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        restTemplate.postForEntity(discordWebhookUrl, request, String.class);
    }

    private String extractSection(String text, String startTag, String endTag) {
        int startIdx = text.indexOf(startTag);
        if (startIdx == -1)
            return "";
        startIdx += startTag.length();
        int endIdx = endTag != null ? text.indexOf(endTag, startIdx) : text.length();
        if (endIdx == -1)
            endIdx = text.length();
        return text.substring(startIdx, endIdx).trim();
    }

    private String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 3) + "..." : text;
    }
}
