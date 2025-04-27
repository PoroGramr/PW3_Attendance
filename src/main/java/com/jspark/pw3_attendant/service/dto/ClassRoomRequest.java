package com.jspark.pw3_attendant.service.dto;


import com.jspark.pw3_attendant.domain.SchoolType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ClassRoomRequest {
    private SchoolType schoolType;  // 🔥 추가
    private Integer grade;
    private Integer classNumber;
}

