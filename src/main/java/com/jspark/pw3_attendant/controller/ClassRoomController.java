package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.service.ClassRoom.ClassRoomService;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomRequest;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/classrooms")
public class ClassRoomController {

    private final ClassRoomService classRoomService;

    /**
     * 반 등록
     */
    @PostMapping
    public ClassRoomResponse createClassRoom(@RequestBody ClassRoomRequest request) {
        ClassRoom savedClassRoom = classRoomService.save(request);
        return ClassRoomResponse.from(savedClassRoom);
    }

    /**
     * 반 단건 조회
     */
    @GetMapping("/{id}")
    public ClassRoomResponse getClassRoom(@PathVariable Long id) {
        ClassRoom classRoom = classRoomService.findById(id);
        return ClassRoomResponse.from(classRoom);
    }

    /**
     * 전체 반 조회
     */
    @GetMapping
    public List<ClassRoomResponse> getAllClassRooms() {
        return classRoomService.findAll()
            .stream()
            .map(ClassRoomResponse::from)
            .collect(Collectors.toList());
    }
}

