package com.jspark.pw3_attendant.service.dto;

import com.jspark.pw3_attendant.domain.ClassRoom;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClassRoomResponse {

    private Long id;
    private String name;

    public static ClassRoomResponse from(ClassRoom classRoom) {
        return new ClassRoomResponse(
            classRoom.getId(),
            classRoom.getName()
        );
    }
}

