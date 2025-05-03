package com.jspark.pw3_attendant.service.StudentClass.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClassRoomStudentsResponse {
    private Long classRoomId;
    private String schoolType;    // MIDDLE, HIGH
    private Integer grade;
    private Integer classNumber;
    private List<StudentSummaryResponse> students; // 이 반에 속한 학생 리스트
}

