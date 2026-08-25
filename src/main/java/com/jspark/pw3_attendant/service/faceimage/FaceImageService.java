package com.jspark.pw3_attendant.service.faceimage;

import com.jspark.pw3_attendant.common.exception.ApiException;
import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.repository.Teacher.TeacherRepository;
import com.jspark.pw3_attendant.service.faceimage.MinioFaceImageStorage.PresignedImageUrl;
import com.jspark.pw3_attendant.service.faceimage.dto.FaceImageResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FaceImageService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_IMAGE_DIMENSION = 4096;
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png"
    );

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final MinioFaceImageStorage storage;

    @Transactional
    public FaceImageResponse uploadStudentImage(Long studentId, MultipartFile file) {
        ValidatedImage image = validate(file);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> notFound("학생을 찾을 수 없습니다."));
        String objectKey = objectKey("students", studentId, image.extension());
        upload(objectKey, file, image.contentType());

        String previousKey = student.getFaceImageKey();
        try {
            student.setFaceImageKey(objectKey);
            studentRepository.saveAndFlush(student);
        } catch (RuntimeException exception) {
            storage.deleteQuietly(objectKey);
            throw exception;
        }
        deleteReplacedImage(previousKey);
        return response(objectKey);
    }

    @Transactional(readOnly = true)
    public FaceImageResponse getStudentImage(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> notFound("학생을 찾을 수 없습니다."));
        return response(requireImageKey(student.getFaceImageKey()));
    }

    @Transactional
    public void deleteStudentImage(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> notFound("학생을 찾을 수 없습니다."));
        String objectKey = requireImageKey(student.getFaceImageKey());
        student.setFaceImageKey(null);
        studentRepository.saveAndFlush(student);
        storage.delete(objectKey);
    }

    @Transactional
    public FaceImageResponse uploadTeacherImage(Long teacherId, MultipartFile file) {
        ValidatedImage image = validate(file);
        Teacher teacher = teacherRepository.findByIdAndDeletedAtIsNull(teacherId)
                .orElseThrow(() -> notFound("선생님을 찾을 수 없습니다."));
        String objectKey = objectKey("teachers", teacherId, image.extension());
        upload(objectKey, file, image.contentType());

        String previousKey = teacher.getFaceImageKey();
        try {
            teacher.setFaceImageKey(objectKey);
            teacherRepository.saveAndFlush(teacher);
        } catch (RuntimeException exception) {
            storage.deleteQuietly(objectKey);
            throw exception;
        }
        deleteReplacedImage(previousKey);
        return response(objectKey);
    }

    @Transactional(readOnly = true)
    public FaceImageResponse getTeacherImage(Long teacherId) {
        Teacher teacher = teacherRepository.findByIdAndDeletedAtIsNull(teacherId)
                .orElseThrow(() -> notFound("선생님을 찾을 수 없습니다."));
        return response(requireImageKey(teacher.getFaceImageKey()));
    }

    @Transactional
    public void deleteTeacherImage(Long teacherId) {
        Teacher teacher = teacherRepository.findByIdAndDeletedAtIsNull(teacherId)
                .orElseThrow(() -> notFound("선생님을 찾을 수 없습니다."));
        String objectKey = requireImageKey(teacher.getFaceImageKey());
        teacher.setFaceImageKey(null);
        teacherRepository.saveAndFlush(teacher);
        storage.delete(objectKey);
    }

    private ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("FACE_IMAGE_EMPTY", "얼굴 사진 파일이 필요합니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FACE_IMAGE_TOO_LARGE", "얼굴 사진은 5MB 이하여야 합니다.");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = ALLOWED_CONTENT_TYPES.get(contentType);
        if (extension == null) {
            throw badRequest("UNSUPPORTED_FACE_IMAGE_TYPE", "JPEG 또는 PNG 이미지만 업로드할 수 있습니다.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                throw badRequest("INVALID_FACE_IMAGE", "읽을 수 없는 이미지 파일입니다.");
            }
            if (image.getWidth() > MAX_IMAGE_DIMENSION || image.getHeight() > MAX_IMAGE_DIMENSION) {
                throw badRequest("FACE_IMAGE_DIMENSION_TOO_LARGE", "이미지 가로와 세로는 각각 4096px 이하여야 합니다.");
            }
        } catch (IOException exception) {
            throw badRequest("INVALID_FACE_IMAGE", "이미지 파일을 읽을 수 없습니다.");
        }
        return new ValidatedImage(contentType, extension);
    }

    private void upload(String objectKey, MultipartFile file, String contentType) {
        try (InputStream inputStream = file.getInputStream()) {
            storage.upload(objectKey, inputStream, file.getSize(), contentType);
        } catch (IOException exception) {
            throw badRequest("INVALID_FACE_IMAGE", "이미지 파일을 읽을 수 없습니다.");
        }
    }

    private FaceImageResponse response(String objectKey) {
        PresignedImageUrl imageUrl = storage.createPresignedUrl(objectKey);
        return new FaceImageResponse(imageUrl.url(), imageUrl.expiresAt());
    }

    private String requireImageKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "FACE_IMAGE_NOT_FOUND", "등록된 얼굴 사진이 없습니다.");
        }
        return objectKey;
    }

    private void deleteReplacedImage(String previousKey) {
        if (previousKey != null && !previousKey.isBlank()) {
            storage.deleteQuietly(previousKey);
        }
    }

    private String objectKey(String ownerType, Long ownerId, String extension) {
        return "face-images/%s/%d/%s.%s".formatted(ownerType, ownerId, UUID.randomUUID(), extension);
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, "PERSON_NOT_FOUND", message);
    }

    private ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    private record ValidatedImage(String contentType, String extension) {
    }
}
