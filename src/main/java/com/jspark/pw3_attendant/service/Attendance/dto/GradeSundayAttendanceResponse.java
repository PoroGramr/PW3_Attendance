package com.jspark.pw3_attendant.service.Attendance.dto;

import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom.SchoolType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GradeSundayAttendanceResponse {
    private SchoolType schoolType;
    private Integer grade;
    private String gradeName;
    private List<SundayStatDto> sundayStats;
}
