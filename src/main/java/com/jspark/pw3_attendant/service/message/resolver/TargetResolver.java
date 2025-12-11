package com.jspark.pw3_attendant.service.message.resolver;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.TargetDto;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.TargetType;

import java.util.List;

public interface TargetResolver {
    /**
     * 이 Resolver가 처리할 수 있는 TargetType을 반환합니다.
     * @return TargetType
     */
    TargetType getTargetType();

    /**
     * TargetDto를 기반으로 대상이 되는 학생 목록을 조회합니다.
     * @param targetDto API 요청에 포함된 target 정보
     * @return 대상 학생 Entity 목록
     */
    List<Student> resolve(TargetDto targetDto);
}
