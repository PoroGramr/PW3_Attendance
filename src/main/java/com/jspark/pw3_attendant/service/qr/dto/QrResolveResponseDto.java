package com.jspark.pw3_attendant.service.qr.dto;

import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.Student.Student;
import lombok.Getter;

@Getter
public class QrResolveResponseDto {
    private final StudentInfo student;
    private final StudentCurrentClassInfo currentClass;
    private final String qrPayload;

    public QrResolveResponseDto(Student student, StudentCurrentClassInfo currentClass, String qrPayload) {
        this.student = new StudentInfo(student);
        this.currentClass = currentClass;
        this.qrPayload = qrPayload;
    }

    @Getter
    private static class StudentInfo {
        private final Long id;
        private final String name;

        public StudentInfo(Student student) {
            this.id = student.getId();
            this.name = student.getName();
        }
    }

    @Getter
    public static class StudentCurrentClassInfo {
        private final String schoolType;
        private final Integer grade;
        private final Integer classNumber;

        public StudentCurrentClassInfo(ClassRoom classRoom) {
            this.schoolType = classRoom.getSchoolType().name();
            this.grade = classRoom.getGrade();
            this.classNumber = classRoom.getClassNumber();
        }
    }
}
