package com.jspark.pw3_attendant.service.Teacher.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.domain.Teacher.Teacher.Sex;
import com.jspark.pw3_attendant.domain.Teacher.Teacher.TeacherType;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherResponse {

    private Long id;
    private String name;
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birth;
    private Sex sex;
    private String phone;
    private TeacherType teacherType;
    private String memo;
    private LocalDateTime deletedAt;
    private Map<Integer, List<ClassRoomResponse>> classesByYear;


    public static TeacherResponse from(Teacher teacher, Map<Integer, List<ClassRoomResponse>> classesByYear) {
        return new TeacherResponse(
            teacher.getId(),
            teacher.getName(),
            teacher.getBirth(),
            teacher.getSex(),
            teacher.getPhone(),
            teacher.getTeacherType(),
            teacher.getMemo(),
            teacher.getDeletedAt(),
            classesByYear
        );
    }

    public static TeacherResponse from(Teacher teacher) {
        return new TeacherResponse(
            teacher.getId(),
            teacher.getName(),
            teacher.getBirth(),
            teacher.getSex(),
            teacher.getPhone(),
            teacher.getTeacherType(),
            teacher.getMemo(),
            teacher.getDeletedAt(),
            null // 담당 반 정보는 포함하지 않음
        );
    }
}
