package com.jspark.pw3_attendant.service.promotion;

import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import com.jspark.pw3_attendant.repository.ClassRoom.ClassRoomRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.service.promotion.dto.PromotionDto;
import com.jspark.pw3_attendant.service.promotion.dto.PromotionRequestDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromotionService {

    private final StudentClassRepository studentClassRepository;
    private final ClassRoomRepository classRoomRepository;

    @Transactional
    public void promoteStudents(PromotionRequestDto request) {
        for (PromotionDto promotion : request.getPromotions()) {
            List<StudentClass> studentsToPromote = studentClassRepository.findAllByClassRoomIdAndSchoolYear(
                promotion.getFromClassRoomId(), request.getFromYear());

            for (StudentClass studentClass : studentsToPromote) {
                StudentClass newStudentClass = new StudentClass();
                newStudentClass.setStudent(studentClass.getStudent());
                newStudentClass.setClassRoom(classRoomRepository.findById(promotion.getToClassRoomId())
                    .orElseThrow(() -> new IllegalArgumentException("Target class room not found")));
                newStudentClass.setSchoolYear(request.getToYear());

                // Avoid creating duplicates
                if (!studentClassRepository.existsByStudentAndClassRoomAndSchoolYear(
                    newStudentClass.getStudent(), newStudentClass.getClassRoom(), newStudentClass.getSchoolYear())) {
                    studentClassRepository.save(newStudentClass);
                }
            }
        }
    }
}
