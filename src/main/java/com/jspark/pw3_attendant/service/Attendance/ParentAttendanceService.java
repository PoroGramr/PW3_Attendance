package com.jspark.pw3_attendant.service.Attendance;

import com.jspark.pw3_attendant.domain.Attendance.ParentAttendance;
import com.jspark.pw3_attendant.domain.Attendance.ParentAttendance.ParentStatus;
import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.repository.Attendance.ParentAttendanceRepository;
import com.jspark.pw3_attendant.repository.Student.StudentRepository;
import com.jspark.pw3_attendant.service.Attendance.dto.ParentAttendanceRequest;
import com.jspark.pw3_attendant.service.Attendance.dto.ParentAttendanceResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.ParentAttendanceStatsResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.ParentSingleAttendanceRequest;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParentAttendanceService {

        private final ParentAttendanceRepository parentAttendanceRepository;
        private final StudentRepository studentRepository;
        private final SimpMessagingTemplate messagingTemplate;

        /** 부/모 출석 생성 또는 수정 (upsert) */
        @Transactional
        public boolean upsert(Long studentId, LocalDate date, ParentAttendanceRequest request) {
                Student student = studentRepository.findById(studentId)
                                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다. id=" + studentId));

                ParentStatus fatherStatus = parseStatus(request.getFatherStatus(), "fatherStatus");
                ParentStatus motherStatus = parseStatus(request.getMotherStatus(), "motherStatus");

                boolean created = parentAttendanceRepository.findByStudentAndDate(student, date)
                                .map(existing -> {
                                        existing.setFatherStatus(fatherStatus);
                                        existing.setMotherStatus(motherStatus);
                                        parentAttendanceRepository.save(existing); // 명시적 save
                                        return false; // 수정
                                })
                                .orElseGet(() -> {
                                        ParentAttendance pa = new ParentAttendance();
                                        pa.setStudent(student);
                                        pa.setDate(date);
                                        pa.setFatherStatus(fatherStatus);
                                        pa.setMotherStatus(motherStatus);
                                        parentAttendanceRepository.save(pa);
                                        return true; // 생성
                                });

                // WebSocket Broadcast
                messagingTemplate.convertAndSend("/topic/attendance",
                                new AttendanceService.AttendanceUpdateMessage("PARENT", student.getId(), student.getName(),
                                                "F:" + fatherStatus.name() + "/M:" + motherStatus.name(),
                                                java.time.LocalDateTime.now()));

                return created;
        }

        /** 부(아버지) 출석만 단독 생성·수정 */
        @Transactional
        public boolean upsertFather(Long studentId, LocalDate date, ParentSingleAttendanceRequest request) {
                Student student = studentRepository.findById(studentId)
                                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다. id=" + studentId));

                ParentStatus status = parseStatus(request.getStatus(), "status");

                boolean created = parentAttendanceRepository.findByStudentAndDate(student, date)
                                .map(existing -> {
                                        existing.setFatherStatus(status);
                                        parentAttendanceRepository.save(existing); // 명시적 save
                                        return false; // 수정
                                })
                                .orElseGet(() -> {
                                        ParentAttendance pa = new ParentAttendance();
                                        pa.setStudent(student);
                                        pa.setDate(date);
                                        pa.setFatherStatus(status);
                                        pa.setMotherStatus(ParentStatus.ABSENT); // 모 기본값
                                        parentAttendanceRepository.save(pa);
                                        return true; // 생성
                                });

                // WebSocket Broadcast
                messagingTemplate.convertAndSend("/topic/attendance",
                                new AttendanceService.AttendanceUpdateMessage("PARENT_FATHER", student.getId(), student.getName(),
                                                status.name(), java.time.LocalDateTime.now()));

                return created;
        }

        /** 모(어머니) 출석만 단독 생성·수정 */
        @Transactional
        public boolean upsertMother(Long studentId, LocalDate date, ParentSingleAttendanceRequest request) {
                Student student = studentRepository.findById(studentId)
                                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다. id=" + studentId));

                ParentStatus status = parseStatus(request.getStatus(), "status");

                boolean created = parentAttendanceRepository.findByStudentAndDate(student, date)
                                .map(existing -> {
                                        existing.setMotherStatus(status);
                                        parentAttendanceRepository.save(existing); // 명시적 save
                                        return false; // 수정
                                })
                                .orElseGet(() -> {
                                        ParentAttendance pa = new ParentAttendance();
                                        pa.setStudent(student);
                                        pa.setDate(date);
                                        pa.setFatherStatus(ParentStatus.ABSENT); // 부 기본값
                                        pa.setMotherStatus(status);
                                        parentAttendanceRepository.save(pa);
                                        return true; // 생성
                                });

                // WebSocket Broadcast
                messagingTemplate.convertAndSend("/topic/attendance",
                                new AttendanceService.AttendanceUpdateMessage("PARENT_MOTHER", student.getId(), student.getName(),
                                                status.name(), java.time.LocalDateTime.now()));

                return created;
        }

        /** 특정 학생, 특정 날짜 출석 조회 */
        @Transactional(readOnly = true)
        public ParentAttendanceResponse getByStudentAndDate(Long studentId, LocalDate date) {
                Student student = studentRepository.findById(studentId)
                                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다. id=" + studentId));

                ParentAttendance pa = parentAttendanceRepository.findByStudentAndDate(student, date)
                                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 출석 기록이 없습니다."));

                return ParentAttendanceResponse.from(pa);
        }

        /**
         * 특정 날짜 전체 학생 출석 조회.
         * 기록이 없는 학생은 포함하지 않음 (기록된 학생만 반환).
         */
        @Transactional(readOnly = true)
        public List<ParentAttendanceResponse> getAllByDate(LocalDate date) {
                return parentAttendanceRepository.findByDate(date).stream()
                                .sorted(Comparator.comparing(pa -> pa.getStudent().getName()))
                                .map(ParentAttendanceResponse::from)
                                .collect(Collectors.toList());
        }

        /**
         * 특정 날짜 전체 재학생 출석 조회.
         * 기록이 없는 학생은 ABSENT/ABSENT 기본값으로 포함.
         */
        @Transactional(readOnly = true)
        public List<ParentAttendanceResponse> getAllStudentsWithDefault(LocalDate date) {
                List<Student> allStudents = studentRepository.findAllByIsGraduatedFalse();

                Map<Long, ParentAttendance> recordMap = parentAttendanceRepository.findByDate(date).stream()
                                .collect(Collectors.toMap(pa -> pa.getStudent().getId(), pa -> pa));

                return allStudents.stream()
                                .sorted(Comparator.comparing(Student::getName))
                                .map(student -> recordMap.containsKey(student.getId())
                                                ? ParentAttendanceResponse.from(recordMap.get(student.getId()))
                                                : ParentAttendanceResponse.unchecked(student, date))
                                .collect(Collectors.toList());
        }

        /** 특정 학생, 특정 날짜 출석 삭제 */
        @Transactional
        public void delete(Long studentId, LocalDate date) {
                Student student = studentRepository.findById(studentId)
                                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다. id=" + studentId));

                ParentAttendance pa = parentAttendanceRepository.findByStudentAndDate(student, date)
                                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 출석 기록이 없습니다."));

                parentAttendanceRepository.delete(pa);
        }

        /**
         * 특정 날짜 부모 출석 통계
         * - totalStudents : 전체 재학생 수
         * - studentsWithParent : 부/모 중 1명이라도 ATTEND인 학생 수
         * - totalParentsAttended: 출석한 부모 총 인원 (부 ATTEND 수 + 모 ATTEND 수)
         */
        @Transactional(readOnly = true)
        public ParentAttendanceStatsResponse getStats(LocalDate date) {
                int totalStudents = (int) studentRepository.findAllByIsGraduatedFalse().size();

                List<ParentAttendance> records = parentAttendanceRepository.findByDate(date);

                int studentsWithParent = (int) records.stream()
                                .filter(pa -> pa.getFatherStatus() == ParentStatus.ATTEND
                                                || pa.getMotherStatus() == ParentStatus.ATTEND)
                                .count();

                int fatherCount = (int) records.stream()
                                .filter(pa -> pa.getFatherStatus() == ParentStatus.ATTEND)
                                .count();

                int motherCount = (int) records.stream()
                                .filter(pa -> pa.getMotherStatus() == ParentStatus.ATTEND)
                                .count();

                return new ParentAttendanceStatsResponse(totalStudents, studentsWithParent, fatherCount + motherCount);
        }

        /** 상태값 파싱 — 잘못된 값(UNCHECKED 등) 입력 시 명확한 에러 반환 */
        private ParentStatus parseStatus(String value, String fieldName) {
                try {
                        return ParentStatus.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                                        fieldName + " 값이 올바르지 않습니다: '" + value + "'. 허용 값: ATTEND, ABSENT");
                }
        }
}
