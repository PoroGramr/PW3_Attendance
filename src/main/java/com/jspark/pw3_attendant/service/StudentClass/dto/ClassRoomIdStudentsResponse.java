package com.jspark.pw3_attendant.service.StudentClass.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class ClassRoomIdStudentsResponse {
    private Long classRoomId;
    private String schoolType;    // MIDDLE, HIGH
    private Integer grade;
    private Integer classNumber;
    private String teacherName; // Added field
    private List<StudentClassSummaryResponse> students; // 이 반에 속한 학생 리스트

    public ClassRoomIdStudentsResponse(Long classRoomId, String schoolType, Integer grade, Integer classNumber, String teacherName, List<StudentClassSummaryResponse> students) {
        this.classRoomId = classRoomId;
        this.schoolType = schoolType;
        this.grade = grade;
        this.classNumber = classNumber;
        this.teacherName = teacherName;
        this.students = students;
    }
}

