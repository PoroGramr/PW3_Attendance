package com.jspark.pw3_attendant.repository.StudentClass;

import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentClassRepository extends JpaRepository<StudentClass, Long> {
    // 학생 + 연도 기준으로 학생-반 매핑 찾기
    Optional<StudentClass> findByStudentIdAndSchoolYear(Long studentId, Integer schoolYear);

    // 반 + 연도 기준으로 모든 학생-반 매핑 조회
    List<StudentClass> findAllByClassRoomIdAndSchoolYear(Long classRoomId, Integer schoolYear);

    @EntityGraph(attributePaths = { "student", "classRoom" })
    List<StudentClass> findAllBySchoolYear(Integer schoolYear);

    @EntityGraph(attributePaths = { "student", "classRoom" })
    List<StudentClass> findAllByClassRoom_IdAndSchoolYear(Long classRoomId, Integer schoolYear);

    long countBySchoolYear(Integer schoolYear);

    long countByClassRoomIdAndSchoolYear(Long classRoomId, Integer schoolYear);

    @EntityGraph(attributePaths = { "classRoom" })
    List<StudentClass> findAllByStudent(Student student);

    boolean existsByStudentAndClassRoomAndSchoolYear(Student student, ClassRoom classRoom, Integer schoolYear);

    long countByClassRoom_SchoolTypeAndClassRoom_GradeAndSchoolYear(
            ClassRoom.SchoolType schoolType,
            Integer grade,
            Integer schoolYear);

    // 배치 조회: 여러 학년도에 대한 학년별 학생 수 (N+1 문제 해결)
    @Query("""
            SELECT
                cr.schoolType as schoolType,
                cr.grade as grade,
                sc.schoolYear as schoolYear,
                COUNT(sc.id) as totalCount
            FROM StudentClass sc
            JOIN sc.classRoom cr
            WHERE sc.schoolYear IN :schoolYears
            GROUP BY cr.schoolType, cr.grade, sc.schoolYear
            """)
    List<GradeStudentCountProjection> findGradeStudentCounts(@Param("schoolYears") List<Integer> schoolYears);

    // Projection 인터페이스
    interface GradeStudentCountProjection {
        ClassRoom.SchoolType getSchoolType();

        Integer getGrade();

        Integer getSchoolYear();

        Long getTotalCount();
    }

    Optional<StudentClass> findTopByStudentIdOrderBySchoolYearDesc(Long studentId);

    // N+1 방지: ClassRoom을 함께 조회
    @EntityGraph(attributePaths = { "classRoom" })
    @Query("SELECT sc FROM StudentClass sc")
    List<StudentClass> findAllWithClassRoom();
}
