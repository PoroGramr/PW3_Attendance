package com.jspark.pw3_attendant.repository.StudentClass;

import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentClassRepository extends JpaRepository<StudentClass, Long> {
    // 학생 + 연도 기준으로 학생-반 매핑 찾기
    Optional<StudentClass> findByStudentIdAndSchoolYear(Long studentId, Integer schoolYear);

    // 반 + 연도 기준으로 모든 학생-반 매핑 조회
    List<StudentClass> findAllByClassRoomIdAndSchoolYear(Long classRoomId, Integer schoolYear);

    @EntityGraph(attributePaths = {"student", "classRoom"})
    List<StudentClass> findAllBySchoolYear(Integer schoolYear);

    @EntityGraph(attributePaths = {"student", "classRoom"})
    List<StudentClass> findAllByClassRoom_IdAndSchoolYear(Long classRoomId, Integer schoolYear);



}
