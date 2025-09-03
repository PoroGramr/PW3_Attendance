package com.jspark.pw3_attendant.service.ClassRoom.dto;

import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import lombok.Getter;

@Getter
public class ClassRoomTeacherResponse {
    private final Long id;
    private final String schoolType;
    private final Integer grade;
    private final Integer classNumber;
    private final String name;
    private final String teacherName;

    public ClassRoomTeacherResponse(ClassRoom classRoom, Teacher teacher) {
        this.id = classRoom.getId();
        this.schoolType = classRoom.getSchoolType().name();
        this.grade = classRoom.getGrade();
        this.classNumber = classRoom.getClassNumber();
        this.name = classRoom.getName();
        this.teacherName = (teacher != null) ? teacher.getName() : null;
    }
}
