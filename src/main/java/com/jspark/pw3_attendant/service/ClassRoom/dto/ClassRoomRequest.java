package com.jspark.pw3_attendant.service.ClassRoom.dto;

import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ClassRoomRequest {
    private ClassRoom.SchoolType schoolType;  // 🔥 추가
    private Integer grade;
    private Integer classNumber;
}

