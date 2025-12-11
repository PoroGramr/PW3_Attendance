package com.jspark.pw3_attendant.service.message.generator;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.ContentDto;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.ContentType;

public interface MessageContentGenerator {
    /**
     * 이 Generator가 처리할 수 있는 ContentType을 반환합니다.
     * @return ContentType
     */
    ContentType getContentType();

    /**
     * 학생 정보와 요청 내용을 바탕으로 최종 메시지 문자열을 생성합니다.
     * @param student 개별 학생 Entity
     * @param contentDto API 요청에 포함된 content 정보
     * @return 발송될 최종 메시지 문자열
     */
    String generate(Student student, ContentDto contentDto);
}
