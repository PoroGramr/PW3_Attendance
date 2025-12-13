package com.jspark.pw3_attendant.service.qr;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import com.jspark.pw3_attendant.domain.message_log.MessageLog;
import com.jspark.pw3_attendant.domain.student_qr.StudentQr;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.repository.student_qr.StudentQrRepository;
import com.jspark.pw3_attendant.service.message.MessageDispatchService;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto;
import com.jspark.pw3_attendant.service.message.dto.MessageSendResponseDto;
import com.jspark.pw3_attendant.service.qr.dto.QrResolveResponseDto;
import com.jspark.pw3_attendant.service.qr.dto.SendQrRequestDto;
import com.jspark.pw3_attendant.service.qr.dto.SendQrResponseDto;
import java.security.SecureRandom;
import java.util.Base64;
import com.jspark.pw3_attendant.service.qr.dto.StudentQrResponseDto;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jspark.pw3_attendant.repository.ClassRoom.ClassRoomRepository;
import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom;
import com.jspark.pw3_attendant.domain.ClassRoom.ClassRoom.SchoolType;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QrService {

    private final StudentQrRepository studentQrRepository;
    private final StudentClassRepository studentClassRepository;
    private final MessageDispatchService messageDispatchService;
    private final ClassRoomRepository classRoomRepository;
    private final StudentRepository studentRepository; // 의존성 주입 추가

    @Value("${app.qr-url-base}")
    private String qrUrlBase;

    public QrResolveResponseDto resolveQr(String qrSecret) {
        StudentQr studentQr = studentQrRepository.findByQrSecret(qrSecret)
            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 QR 코드입니다."));

        Student student = studentQr.getStudent();
        String qrPayload = generateQrPayload(student, studentQr);

        // Determine current school year (assuming March is the start of a new school year)
        int currentYear = java.time.LocalDate.now().getYear();
        int schoolYear = java.time.LocalDate.now().getMonthValue() >= 3 ? currentYear : currentYear - 1;

        // Find the student's current class
        QrResolveResponseDto.StudentCurrentClassInfo currentClassInfo = studentClassRepository
            .findByStudentIdAndSchoolYear(student.getId(), schoolYear)
            .map(sc -> new QrResolveResponseDto.StudentCurrentClassInfo(sc.getClassRoom()))
            .orElse(null); // Or throw an exception if a student must have a current class

        return new QrResolveResponseDto(student, currentClassInfo, qrPayload);
    }

    @Transactional
    public SendQrResponseDto sendQrLinks(SendQrRequestDto request) {
        // Based on the domain, courseId corresponds to classRoomId.
        // The school year is also needed. I will assume the current year for now.
        // TODO: The logic to determine the school year should be refined.
        int schoolYear = java.time.LocalDate.now().getYear();
        List<StudentClass> studentClasses = studentClassRepository.findAllByClassRoomIdAndSchoolYear(request.getCourseId(), schoolYear);

        int totalSent = 0;
        int totalSuccess = 0;
        int totalFailed = 0;

        for (StudentClass sc : studentClasses) {
            Student student = sc.getStudent();
            totalSent++;
            try {
                sendPersonalQrCodeSms(student.getId()); // 새로 만든 메서드 호출
                totalSuccess++;
            } catch (Exception e) {
                totalFailed++;
                // Log the exception if necessary, MessageDispatchService already logs send failures.
            }
        }
        return new SendQrResponseDto(totalSent, totalSuccess, totalFailed);
    }

    @Transactional
    public boolean sendPersonalQrCodeSms(Long studentId) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("해당 학생을 찾을 수 없습니다. ID: " + studentId));

        try {
            StudentQr studentQr = studentQrRepository.findByStudentId(student.getId())
                .orElseGet(() -> {
                    String newSecret = generateRandomSecret();
                    return studentQrRepository.save(new StudentQr(student, newSecret));
                });

            String qrUrl = String.format("%s/s/%s", qrUrlBase, studentQr.getQrSecret());
            String messageContent = createMessageContent(student, qrUrl);

            // Construct MessageRequestDto for this single student
            MessageRequestDto messageRequest = new MessageRequestDto();

            MessageRequestDto.TargetDto targetDto = new MessageRequestDto.TargetDto();
            targetDto.setType(MessageRequestDto.TargetType.SPECIFIC_STUDENTS);
            targetDto.setIds(List.of(student.getId())); // Target only this student
            messageRequest.setTarget(targetDto);

            // 채널은 SMS로 고정
            messageRequest.setChannel(MessageLog.MessageChannel.SMS);

            // 수신자는 학생으로 고정
            messageRequest.setRecipientType(MessageRequestDto.RecipientType.STUDENTS);

            MessageRequestDto.ContentDto contentDto = new MessageRequestDto.ContentDto();
            contentDto.setType(MessageRequestDto.ContentType.TEXT); // QR URL은 텍스트 기반
            contentDto.setText(messageContent);
            contentDto.setImageUrl(null); // QR 링크 발송 시에는 이미지 없음
            messageRequest.setContent(contentDto);

            MessageSendResponseDto dispatchResult = messageDispatchService.dispatchMessage(messageRequest);

            return dispatchResult.getSuccess() > 0;

        } catch (Exception e) {
            // Log the exception if necessary, MessageDispatchService already logs send failures.
            return false;
        }
    }

    private String createMessageContent(Student student, String qrUrl) {
        return String.format(
            "[출석 시스템 안내]\n\n%s 학생의 출석용 QR 페이지 링크입니다.\n 예배당 입장 시 아래 링크를 열어 QR 코드를 제시해 주세요.\n\n%s\n\n*본 링크와 QR은 본인만 사용해 주세요.",
            student.getName(),
            qrUrl
        );
    }

    /**
     * Generates a unique, URL-safe, random secret.
     * @return A new secret string.
     */
    private String generateRandomSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[16]; // 16 bytes = 128 bits
        String secret;
        do {
            random.nextBytes(bytes);
            secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (studentQrRepository.existsByQrSecret(secret));
        return secret;
    }



    @Transactional
    public List<StudentQrResponseDto> getStudentQrsForClass(Long classRoomId, Integer schoolYear) {
        List<StudentClass> studentClasses = studentClassRepository.findAllByClassRoomIdAndSchoolYear(classRoomId, schoolYear);

        return studentClasses.stream()
            .map(sc -> {
                Student student = sc.getStudent();
                StudentQr studentQr = studentQrRepository.findByStudentId(student.getId())
                    .orElseGet(() -> {
                        String newSecret = generateRandomSecret();
                        return studentQrRepository.save(new StudentQr(student, newSecret));
                    });
                return new StudentQrResponseDto(student, studentQr, qrUrlBase);
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public List<StudentQrResponseDto> getStudentQrsForClass(Integer schoolYear, SchoolType schoolType, Integer grade, Integer classNumber) {
        ClassRoom classRoom = classRoomRepository.findBySchoolTypeAndGradeAndClassNumber(schoolType, grade, classNumber)
            .orElseThrow(() -> new IllegalArgumentException("해당하는 반을 찾을 수 없습니다."));
        return getStudentQrsForClass(classRoom.getId(), schoolYear);
    }

    /**
     * Generates the payload to be embedded in the QR code.
     * Format: "ATT-STU:{studentId}:{qrSecret}"
     * This format allows the server to easily parse and verify the student's identity upon scan.
     * - "ATT-STU": A prefix to identify this as an attendance QR code.
     * - studentId: The student's primary ID for quick database lookups.
     * - qrSecret: The unique secret to verify the QR code's authenticity.
     * @param student The student entity.
     * @param studentQr The student's QR entity.
     * @return The formatted QR payload string.
     */
    private String generateQrPayload(Student student, StudentQr studentQr) {
        return String.format("ATT-STU:%d:%s", student.getId(), studentQr.getQrSecret());
    }
}