package com.jspark.pw3_attendant.service.thirdparty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j; // 추가
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.http.InvalidMediaTypeException; // 추가
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Objects;

@Service
@Slf4j // 추가
public class ImgbbService {

    @Value("${imgbb.api.key}")
    private String imgbbApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ImgbbService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String uploadImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty.");
        }

        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("key", imgbbApiKey);

            // Content-Type이 null이거나 비어있을 경우 기본값을 설정하고 유효하지 않은 경우도 처리
            MediaType contentType = null;
            if (imageFile.getContentType() != null && !imageFile.getContentType().isEmpty()) {
                try {
                    contentType = MediaType.parseMediaType(imageFile.getContentType());
                } catch (InvalidMediaTypeException e) {
                    log.warn("ImgbbService: Invalid media type received for image file '{}'. Using default application/octet-stream. Error: {}", imageFile.getOriginalFilename(), e.getMessage());
                    contentType = MediaType.APPLICATION_OCTET_STREAM;
                }
            } else {
                log.info("ImgbbService: Image file content type is null or empty for '{}'. Using default application/octet-stream.", imageFile.getOriginalFilename());
                contentType = MediaType.APPLICATION_OCTET_STREAM;
            }

            builder.part("image", new ByteArrayResource(imageFile.getBytes()))
                .filename(imageFile.getOriginalFilename())
                .contentType(contentType); // 수정된 contentType 사용

            MultiValueMap<String, HttpEntity<?>> body = builder.build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, HttpEntity<?>>> requestEntity = new HttpEntity<>(body, headers);

            String imgbbApiUrl = "https://api.imgbb.com/1/upload";
            ResponseEntity<String> response = restTemplate.exchange(
                imgbbApiUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(Objects.requireNonNull(response.getBody()));
                if (root.has("data") && root.get("data").has("url")) {
                    return root.get("data").get("url").asText();
                } else {
                    throw new RuntimeException("Failed to get image URL from ImgBB response: " + response.getBody());
                }
            } else {
                throw new RuntimeException("Failed to upload image to ImgBB: " + response.getStatusCode() + " - " + response.getBody());
            }

        } catch (IOException e) {
            throw new RuntimeException("Error processing image file for ImgBB upload", e);
        }
    }
}
