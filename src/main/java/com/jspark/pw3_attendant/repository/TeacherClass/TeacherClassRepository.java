package com.jspark.pw3_attendant.repository.TeacherClass;

import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.domain.TeacherClass.TeacherClass;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherClassRepository extends JpaRepository<TeacherClass, Long> {
    Optional<TeacherClass> findByClassRoomIdAndSchoolYear(Long classRoomId, Integer schoolYear);

    List<TeacherClass> findAllByClassRoomIdAndSchoolYear(Long classRoomId, int schoolYear);

    List<TeacherClass> findAllBySchoolYear(Integer schoolYear);

    List<TeacherClass> findAllByTeacher(Teacher teacher);

    @EntityGraph(attributePaths = { "teacher", "classRoom" })
    List<TeacherClass> findAllBySchoolYearAndClassRoomIn(Integer schoolYear, List<ClassRoom> classRooms);
}
