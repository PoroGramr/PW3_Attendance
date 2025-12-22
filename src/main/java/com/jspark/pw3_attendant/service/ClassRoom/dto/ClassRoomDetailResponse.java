package com.jspark.pw3_attendant.service.ClassRoom.dto;

import com.jspark.pw3_attendant.service.StudentClass.dto.StudentSummaryResponse;
import com.jspark.pw3_attendant.service.Teacher.dto.TeacherResponse;
import lombok.Getter;

import java.util.List;

@Getter
public class ClassRoomDetailResponse {
    private final ClassRoomResponse classRoomInfo;
    private final List<StudentSummaryResponse> students;
    private final List<TeacherResponse> teachers;

    public ClassRoomDetailResponse(ClassRoomResponse classRoomInfo, List<StudentSummaryResponse> students, List<TeacherResponse> teachers) {
        this.classRoomInfo = classRoomInfo;
        this.students = students;
        this.teachers = teachers;
    }
}
