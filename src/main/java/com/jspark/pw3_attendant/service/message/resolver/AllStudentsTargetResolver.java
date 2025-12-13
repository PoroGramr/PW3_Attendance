package com.jspark.pw3_attendant.service.message.resolver;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.TargetDto;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.TargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AllStudentsTargetResolver implements TargetResolver {

    private final StudentRepository studentRepository;

    @Override
    public TargetType getTargetType() {
        return TargetType.ALL_STUDENTS;
    }

    @Override
    public List<Student> resolve(TargetDto targetDto) {
        return studentRepository.findAll();
    }
}
