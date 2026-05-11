package com.jspark.pw3_attendant.service.Attendance;

import com.jspark.pw3_attendant.domain.Attendance.AttendanceTeacher;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import com.jspark.pw3_attendant.repository.Attendance.AttendanceTeacherRepository;
import com.jspark.pw3_attendant.repository.Teacher.TeacherRepository;
import com.jspark.pw3_attendant.service.Attendance.dto.TeacherAttendance;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class AttendanceTeacherService {

    private final AttendanceTeacherRepository attendanceTeacherRepository;
    private final TeacherRepository teacherRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AttendanceTeacherService(AttendanceTeacherRepository attendanceTeacherRepository,
        TeacherRepository teacherRepository, SimpMessagingTemplate messagingTemplate) {
        this.attendanceTeacherRepository = attendanceTeacherRepository;
        this.teacherRepository = teacherRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // 선생님 본인 출석 체크
    public void markTeacherAttendance(Long teacherId, AttendanceTeacher.AttendanceStatus status, LocalDate date) {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("선생님을 찾을 수 없습니다."));

        // 선생님과 날짜에 해당하는 출석 정보를 조회
        AttendanceTeacher attendanceTeacher = attendanceTeacherRepository.findByTeacherAndDate(teacher, date)
            .orElse(new AttendanceTeacher()); // 없으면 새로 생성

        // 출석 정보 업데이트
        attendanceTeacher.setTeacher(teacher);
        attendanceTeacher.setDate(date);
        attendanceTeacher.setStatus(status);

        // 출석 정보 저장 (새로 추가하거나 기존 값을 업데이트)
        attendanceTeacherRepository.save(attendanceTeacher);

        // WebSocket Broadcast
        messagingTemplate.convertAndSend("/topic/attendance",
            new AttendanceService.AttendanceUpdateMessage("TEACHER", teacher.getId(), teacher.getName(), status.name(), attendanceTeacher.getUpdatedAt()));
    }

    // 선생님 본인 출석 상태 조회
    public AttendanceTeacher getTeacherAttendanceStatus(Long teacherId, LocalDate date) {
        Teacher teacher = teacherRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("선생님을 찾을 수 없습니다."));

        return attendanceTeacherRepository.findByTeacherAndDate(teacher, date)
            .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 출석 기록이 없습니다."));
    }

    public List<TeacherAttendance> getAllTeachersAttendanceByDate(LocalDate date) {
        // 출석 체크가 된 선생님들
        List<AttendanceTeacher> attendanceList = attendanceTeacherRepository.findByDate(date);

        // 출석 체크가 되지 않은 선생님들
        List<Teacher> allTeachers = teacherRepository.findAll();

        // 출석 체크된 선생님들을 DTO로 변환
        List<TeacherAttendance> attendanceTeacherDTOs = attendanceList.stream()
            .map(attendanceTeacher -> {
                // 출석 상태가 null일 경우 "UNCHECKED"로 처리
                String status = (attendanceTeacher.getStatus() == null) ? "UNCHECKED" : attendanceTeacher.getStatus().name();
                return new TeacherAttendance(
                    attendanceTeacher.getTeacher().getId(),
                    attendanceTeacher.getTeacher().getName(),
                    status  // 상태를 문자열로 설정
                );
            })
            .collect(Collectors.toList());

        // 출석 체크되지 않은 선생님들을 DTO로 변환하여 추가
        allTeachers.forEach(teacher -> {
            // 출석 체크되지 않은 선생님을 "UNCHECKED" 상태로 추가
            boolean isChecked = attendanceTeacherDTOs.stream()
                .anyMatch(att -> att.getTeacherId().equals(teacher.getId()));

            if (!isChecked) {
                attendanceTeacherDTOs.add(new TeacherAttendance(
                    teacher.getId(),
                    teacher.getName(),
                    "UNCHECKED"  // 문자열로 처리
                ));
            }
        });

        attendanceTeacherDTOs.sort(Comparator.comparing(TeacherAttendance::getTeacherName));

        return attendanceTeacherDTOs;
    }

}