package com.jspark.pw3_attendant.service.faceimage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jspark.pw3_attendant.common.exception.ApiException;
import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.repository.Teacher.TeacherRepository;
import com.jspark.pw3_attendant.service.faceimage.MinioFaceImageStorage.PresignedImageUrl;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class FaceImageServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private MinioFaceImageStorage storage;

    private FaceImageService faceImageService;

    @BeforeEach
    void setUp() {
        faceImageService = new FaceImageService(studentRepository, teacherRepository, storage);
    }

    @Test
    void uploadStudentImageReplacesPreviousImage() throws Exception {
        Student student = new Student();
        student.setFaceImageKey("face-images/students/1/old.jpg");
        MockMultipartFile file = pngFile();
        Instant expiresAt = Instant.now().plusSeconds(600);

        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.saveAndFlush(student)).thenReturn(student);
        when(storage.createPresignedUrl(anyString()))
                .thenReturn(new PresignedImageUrl("http://localhost/presigned", expiresAt));

        var response = faceImageService.uploadStudentImage(1L, file);

        assertThat(student.getFaceImageKey()).startsWith("face-images/students/1/").endsWith(".png");
        assertThat(response.url()).isEqualTo("http://localhost/presigned");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        verify(storage).upload(anyString(), any(), anyLong(), anyString());
        verify(storage).deleteQuietly("face-images/students/1/old.jpg");
    }

    @Test
    void uploadTeacherImageReplacesPreviousImage() throws Exception {
        Teacher teacher = new Teacher();
        teacher.setFaceImageKey("face-images/teachers/2/old.jpg");
        MockMultipartFile file = pngFile();
        Instant expiresAt = Instant.now().plusSeconds(600);

        when(teacherRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(teacher));
        when(teacherRepository.saveAndFlush(teacher)).thenReturn(teacher);
        when(storage.createPresignedUrl(anyString()))
                .thenReturn(new PresignedImageUrl("http://localhost/presigned", expiresAt));

        var response = faceImageService.uploadTeacherImage(2L, file);

        assertThat(teacher.getFaceImageKey()).startsWith("face-images/teachers/2/").endsWith(".png");
        assertThat(response.url()).isEqualTo("http://localhost/presigned");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
        verify(storage).upload(anyString(), any(), anyLong(), anyString());
        verify(storage).deleteQuietly("face-images/teachers/2/old.jpg");
    }

    @Test
    void rejectUnsupportedImageType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "face.gif", "image/gif", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> faceImageService.uploadStudentImage(1L, file))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("UNSUPPORTED_FACE_IMAGE_TYPE"));
    }

    @Test
    void getStudentImageFailsWhenNoImageIsRegistered() {
        Student student = new Student();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> faceImageService.getStudentImage(1L))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("FACE_IMAGE_NOT_FOUND"));
    }

    private MockMultipartFile pngFile() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return new MockMultipartFile(
                "file", "face.png", "image/png", outputStream.toByteArray());
    }
}
