package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.service.faceimage.FaceImageService;
import com.jspark.pw3_attendant.service.faceimage.dto.FaceImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "얼굴 사진", description = "학생·교사의 대표 얼굴 사진을 관리하는 API")
public class FaceImageController {

    private final FaceImageService faceImageService;

    @PutMapping(value = "/students/{id}/face-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "학생 얼굴 사진 등록·교체", description = "JPEG 또는 PNG 사진 1장을 등록합니다. 기존 사진이 있으면 교체합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 또는 교체 완료"),
            @ApiResponse(responseCode = "400", description = "이미지 형식 또는 해상도 오류"),
            @ApiResponse(responseCode = "404", description = "학생을 찾을 수 없음"),
            @ApiResponse(responseCode = "413", description = "5MB를 초과한 파일")
    })
    public FaceImageResponse uploadStudentImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {
        return faceImageService.uploadStudentImage(id, file);
    }

    @GetMapping("/students/{id}/face-image")
    @Operation(summary = "학생 얼굴 사진 URL 조회", description = "비공개 MinIO 객체를 조회할 수 있는 만료형 URL을 반환합니다.")
    public FaceImageResponse getStudentImage(@PathVariable Long id) {
        return faceImageService.getStudentImage(id);
    }

    @DeleteMapping("/students/{id}/face-image")
    @Operation(summary = "학생 얼굴 사진 삭제")
    public ResponseEntity<Void> deleteStudentImage(@PathVariable Long id) {
        faceImageService.deleteStudentImage(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/teacher/{id}/face-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "교사 얼굴 사진 등록·교체", description = "JPEG 또는 PNG 사진 1장을 등록합니다. 기존 사진이 있으면 교체합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록 또는 교체 완료"),
            @ApiResponse(responseCode = "400", description = "이미지 형식 또는 해상도 오류"),
            @ApiResponse(responseCode = "404", description = "교사를 찾을 수 없음"),
            @ApiResponse(responseCode = "413", description = "5MB를 초과한 파일")
    })
    public FaceImageResponse uploadTeacherImage(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {
        return faceImageService.uploadTeacherImage(id, file);
    }

    @GetMapping("/teacher/{id}/face-image")
    @Operation(summary = "교사 얼굴 사진 URL 조회", description = "비공개 MinIO 객체를 조회할 수 있는 만료형 URL을 반환합니다.")
    public FaceImageResponse getTeacherImage(@PathVariable Long id) {
        return faceImageService.getTeacherImage(id);
    }

    @DeleteMapping("/teacher/{id}/face-image")
    @Operation(summary = "교사 얼굴 사진 삭제")
    public ResponseEntity<Void> deleteTeacherImage(@PathVariable Long id) {
        faceImageService.deleteTeacherImage(id);
        return ResponseEntity.noContent().build();
    }
}
