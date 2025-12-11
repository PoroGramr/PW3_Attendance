package com.jspark.pw3_attendant.service.message.generator;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.ContentDto;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.ContentType;
import org.springframework.stereotype.Component;

@Component
public class SimpleTextGenerator implements MessageContentGenerator {

    @Override
    public ContentType getContentType() {
        return ContentType.TEXT;
    }

    @Override
    public String generate(Student student, ContentDto contentDto) {
        // Replace placeholders like {student_name}
        return contentDto.getText().replace("{student_name}", student.getName());
    }
}
