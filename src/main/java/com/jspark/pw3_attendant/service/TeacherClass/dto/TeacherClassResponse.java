package com.jspark.pw3_attendant.service.TeacherClass.dto;

import com.jspark.pw3_attendant.domain.TeacherClass.TeacherClass;
import lombok.Builder;
import lombok.Getter;

@Getter
public class TeacherClassResponse {
    private final Long id;
    private final Long teacherId;
    private final String teacherName;
    private final Long classRoomId;
    private final String classRoomName;
    private final int schoolYear;

    @Builder
    public TeacherClassResponse(Long id, Long teacherId, String teacherName, Long classRoomId, String classRoomName, int schoolYear) {
        this.id = id;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.classRoomId = classRoomId;
        this.classRoomName = classRoomName;
        this.schoolYear = schoolYear;
    }

    public static TeacherClassResponse from(TeacherClass teacherClass) {
        return TeacherClassResponse.builder()
            .id(teacherClass.getId())
            .teacherId(teacherClass.getTeacher().getId())
            .teacherName(teacherClass.getTeacher().getName())
            .classRoomId(teacherClass.getClassRoom().getId())
            .classRoomName(teacherClass.getClassRoom().getName())
            .schoolYear(teacherClass.getSchoolYear())
            .build();
    }
}
