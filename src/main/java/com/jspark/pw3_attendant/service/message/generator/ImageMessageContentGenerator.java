package com.jspark.pw3_attendant.service.message.generator;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.ContentDto;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.ContentType;
import org.springframework.stereotype.Component;

@Component
public class ImageMessageContentGenerator implements MessageContentGenerator {

    @Override
    public ContentType getContentType() {
        return ContentType.TEXT_WITH_IMAGE;
    }

    @Override
    public String generate(Student student, ContentDto contentDto) {
        // 이미지 URL과 텍스트를 조합하여 메시지 내용을 생성합니다.
        // 예를 들어, "안녕하세요 {student_name}님. 이미지를 확인해주세요: [이미지 URL]"
        String messageText = contentDto.getText() != null ? contentDto.getText().replace("{student_name}", student.getName()) : "";
        String imageUrl = contentDto.getImageUrl() != null ? contentDto.getImageUrl() : "";

        // 이미지 URL이 있을 경우 메시지에 포함시킵니다.
        if (!imageUrl.isEmpty()) {
            return messageText + " (이미지: " + imageUrl + ")";
        } else {
            return messageText;
        }
    }
}
