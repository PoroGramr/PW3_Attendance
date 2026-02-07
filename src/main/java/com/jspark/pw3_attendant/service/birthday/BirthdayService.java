package com.jspark.pw3_attendant.service.birthday;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.repository.Teacher.TeacherRepository;
import com.jspark.pw3_attendant.service.birthday.dto.MonthlyBirthdayResponse;
import com.jspark.pw3_attendant.service.birthday.dto.StudentBirthdayResponse;
import com.jspark.pw3_attendant.service.birthday.dto.TeacherBirthdayResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BirthdayService {


    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudentClassRepository studentClassRepository;

    public List<StudentBirthdayResponse> getStudentBirthdays(int month){

        List<Student> students = studentRepository.findByBirthMonth(month);

        int currentYear = getCurrentSchoolYear();

        return students.stream()
            .map(student -> {
                String className = getStudentClassName(student.getId(), currentYear);
                return new StudentBirthdayResponse(
                    student.getId(),
                    student.getName(),
                    student.getBirth(),
                    className,
                    student.getPhone()
                );
            })
            .collect(Collectors.toList());
    }


    public List<TeacherBirthdayResponse> getTeacherBirthdays(int month) {
        List<Teacher> teachers = teacherRepository.findByBirthMonth(month);

        return teachers.stream()
            .map(teacher -> new TeacherBirthdayResponse(
                teacher.getId(),
                teacher.getName(),
                teacher.getBirth(),
                teacher.getPhone()
            ))
            .collect(Collectors.toList());
    }

    public MonthlyBirthdayResponse getAllBirthdays(int month) {
        return new MonthlyBirthdayResponse(
            month,
            getStudentBirthdays(month),
            getTeacherBirthdays(month)
        );
    }

    private String getStudentClassName(Long studentId, int schoolYear) {
        return studentClassRepository.findByStudentIdAndSchoolYear(studentId, schoolYear)
            .map(sc -> sc.getClassRoom().getName())
            .orElse("미배정");
    }

    private int getCurrentSchoolYear() {
        LocalDate now = LocalDate.now();
        return now.getYear();
    }
}
