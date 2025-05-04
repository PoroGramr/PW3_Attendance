package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.service.ClassRoom.ClassRoomService;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomRequest;
import com.jspark.pw3_attendant.service.ClassRoom.dto.ClassRoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "반 생성")
    public ClassRoomResponse createClassRoom(@RequestBody ClassRoomRequest request) {
        ClassRoom savedClassRoom = classRoomService.save(request);
        return ClassRoomResponse.from(savedClassRoom);
    }

    /**
     * 반 단건 조회
     */
    @GetMapping("/{id}")
    @Operation(summary = "반 조회")
    public ClassRoomResponse getClassRoom(@PathVariable Long id) {
        ClassRoom classRoom = classRoomService.findById(id);
        return ClassRoomResponse.from(classRoom);
    }

    /**
     * 전체 반 조회
     */
    @GetMapping
    @Operation(summary = "반 전체 조회")
    public List<ClassRoomResponse> getAllClassRooms() {
        return classRoomService.findAll()
            .stream()
            .map(ClassRoomResponse::from)
            .collect(Collectors.toList());
    }
}

