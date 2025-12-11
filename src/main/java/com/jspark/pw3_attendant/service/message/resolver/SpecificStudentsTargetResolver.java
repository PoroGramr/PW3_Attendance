package com.jspark.pw3_attendant.service.message.resolver;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.TargetDto;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.TargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SpecificStudentsTargetResolver implements TargetResolver {

    private final StudentRepository studentRepository;

    @Override
    public TargetType getTargetType() {
        return TargetType.SPECIFIC_STUDENTS;
    }

    @Override
    public List<Student> resolve(TargetDto targetDto) {
        if (targetDto.getIds() == null || targetDto.getIds().isEmpty()) {
            return Collections.emptyList();
        }
        return studentRepository.findAllById(targetDto.getIds());
    }
}
