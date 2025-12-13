package com.jspark.pw3_attendant.service.message.resolver;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.TargetDto;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.TargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClassRoomTargetResolver implements TargetResolver {

    private final StudentClassRepository studentClassRepository;

    @Override
    public TargetType getTargetType() {
        return TargetType.CLASS_ROOM;
    }

    @Override
    public List<Student> resolve(TargetDto targetDto) {
        if (targetDto.getIds() == null || targetDto.getIds().isEmpty()) {
            return Collections.emptyList();
        }

        // Assuming the first ID is the classRoomId and we need the current year.
        // This part might need to be more robust, e.g., by passing the year in the DTO.
        // For now, we'll assume a single classRoomId is passed.
        Long classRoomId = targetDto.getIds().get(0);
        int currentYear = java.time.LocalDate.now().getYear();

        return studentClassRepository.findAllByClassRoomIdAndSchoolYear(classRoomId, currentYear)
            .stream()
            .map(sc -> sc.getStudent())
            .collect(Collectors.toList());
    }
}
