package com.jspark.pw3_attendant.repository.ClassRoom;


import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom.SchoolType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {

    Optional<ClassRoom> findBySchoolTypeAndGradeAndClassNumber(SchoolType schoolType, Integer grade, Integer classNumber);
}
