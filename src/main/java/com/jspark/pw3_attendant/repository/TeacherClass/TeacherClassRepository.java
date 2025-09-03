package com.jspark.pw3_attendant.repository.TeacherClass;


import com.jspark.pw3_attendant.domain.TeacherClass.TeacherClass;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherClassRepository extends JpaRepository<TeacherClass, Long> {
    Optional<TeacherClass> findByClassRoomIdAndSchoolYear(Long classRoomId,Integer schoolYear);

    List<TeacherClass> findAllBySchoolYear(Integer schoolYear);
}
