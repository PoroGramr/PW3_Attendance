package com.jspark.pw3_attendant.service.integration.acts29;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jspark.pw3_attendant.common.exception.ApiException;
import com.jspark.pw3_attendant.domain.Attendance.Attendance;
import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import com.jspark.pw3_attendant.repository.Attendance.AttendanceRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.service.integration.acts29.dto.Acts29AttendanceSyncRequest;
import com.jspark.pw3_attendant.service.integration.acts29.dto.Acts29AttendanceSyncResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class Acts29AttendanceSyncServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 10, 4);

    @Mock
    private StudentClassRepository studentClassRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private Acts29Client acts29Client;

    @Mock
    private Acts29Client.Session session;

    private Acts29AttendanceSyncService service;

    @BeforeEach
    void setUp() {
        service = new Acts29AttendanceSyncService(
                studentClassRepository,
                attendanceRepository,
                acts29Client
        );
    }

    @Test
    @DisplayName("미체크 학생은 dry-run에서 결석(N) 변경 대상으로 계산한다")
    void uncheckedStudentMapsToAbsentInDryRun() {
        StudentClass studentClass = studentClass(1L, 10L, "테스트학생", LocalDate.of(2010, 1, 2));
        Acts29StudentRecord remote = remoteRecord(0, "target-10", "테스트학생", "20100102", "Y");
        Acts29StudentRecord unrelated = remoteRecord(1, "other-20", "다른반학생", "20100203", "N");
        given(studentClassRepository.findAllBySchoolYear(2026)).willReturn(List.of(studentClass));
        given(attendanceRepository.findAllByStudentClassInAndDate(List.of(studentClass), DATE))
                .willReturn(List.of());
        given(acts29Client.open(DATE, "전체", "전체")).willReturn(session);
        given(session.fetchAttendance()).willReturn(List.of(remote, unrelated));

        Acts29AttendanceSyncResponse response = service.sync(
                DATE,
                new Acts29AttendanceSyncRequest(true, false, null, null)
        );

        assertThat(response.result()).isEqualTo("DRY_RUN");
        assertThat(response.matchedCount()).isEqualTo(1);
        assertThat(response.unmatchedTargetCount()).isZero();
        assertThat(response.changedCount()).isEqualTo(1);
        assertThat(response.remotePresentBefore()).isEqualTo(1);
        assertThat(response.remotePresentAfter()).isZero();
        assertThat(response.changes()).singleElement().satisfies(change -> {
            assertThat(change.localStatus()).isEqualTo("UNCHECKED");
            assertThat(change.previousAttendance()).isEqualTo("Y");
            assertThat(change.nextAttendance()).isEqualTo("N");
        });
        verify(session, never()).saveAttendance(anyList());
    }

    @Test
    @DisplayName("실제 동기화는 저장 후 다시 조회해 출석 상태를 검증한다")
    void appliesAndVerifiesAttendance() {
        StudentClass studentClass = studentClass(1L, 10L, "테스트학생", LocalDate.of(2010, 1, 2));
        Acts29StudentRecord before = remoteRecord(0, "target-10", "테스트학생", "100102", "Y");
        Acts29StudentRecord after = before.withAttendance("N");
        given(studentClassRepository.findAllBySchoolYear(2026)).willReturn(List.of(studentClass));
        given(attendanceRepository.findAllByStudentClassInAndDate(List.of(studentClass), DATE))
                .willReturn(List.of());
        given(acts29Client.open(DATE, "전체", "전체")).willReturn(session);
        given(session.fetchAttendance())
                .willReturn(List.of(before), List.of(before), List.of(after));

        Acts29AttendanceSyncResponse response = service.sync(
                DATE,
                new Acts29AttendanceSyncRequest(false, false, DATE, null)
        );

        assertThat(response.result()).isEqualTo("SYNCED");
        ArgumentCaptor<List<Acts29StudentRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(session).saveAttendance(captor.capture());
        assertThat(captor.getValue()).singleElement()
                .extracting(Acts29StudentRecord::attendance)
                .isEqualTo("N");
    }

    @Test
    @DisplayName("양쪽 명단이 달라도 기본 동기화는 출석과 지각을 반영하고 원격 전용 학생은 보존한다")
    void syncsMatchedAttendanceByDefaultWithDifferentRosters() {
        StudentClass present = studentClass(1L, 10L, "출석학생", LocalDate.of(2010, 1, 2));
        StudentClass late = studentClass(2L, 20L, "지각학생", LocalDate.of(2010, 2, 3));
        StudentClass localOnly = studentClass(3L, 30L, "로컬전용", LocalDate.of(2010, 3, 4));
        List<StudentClass> locals = List.of(present, late, localOnly);
        Acts29StudentRecord remotePresent = remoteRecord(0, "10", "출석학생", "20100102", "N");
        Acts29StudentRecord remoteLate = remoteRecord(1, "20", "지각학생", "20100203", "N");
        Acts29StudentRecord remoteOnly = remoteRecord(2, "40", "원격전용", "20100405", "Y");
        List<Acts29StudentRecord> before = List.of(remotePresent, remoteLate, remoteOnly);
        given(studentClassRepository.findAllBySchoolYear(2026)).willReturn(locals);
        given(attendanceRepository.findAllByStudentClassInAndDate(locals, DATE))
                .willReturn(List.of(
                        attendance(present, AttendanceStatus.ATTEND),
                        attendance(late, AttendanceStatus.LATE),
                        attendance(localOnly, AttendanceStatus.ATTEND)
                ));
        given(acts29Client.open(DATE, "전체", "전체")).willReturn(session);
        given(session.fetchAttendance()).willReturn(before, before, List.of(
                remotePresent.withAttendance("Y"), remoteLate.withAttendance("Y"), remoteOnly
        ));

        Acts29AttendanceSyncResponse response = service.sync(
                DATE, new Acts29AttendanceSyncRequest(false, null, DATE, null)
        );

        assertThat(response.result()).isEqualTo("SYNCED");
        assertThat(response.matchedCount()).isEqualTo(2);
        assertThat(response.changedCount()).isEqualTo(2);
        assertThat(response.unmatchedTargetCount()).isEqualTo(1);
        assertThat(response.matchIssues()).singleElement()
                .satisfies(issue -> assertThat(issue.studentId()).isEqualTo(30L));
        ArgumentCaptor<List<Acts29StudentRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(session).saveAttendance(captor.capture());
        assertThat(captor.getValue()).extracting(Acts29StudentRecord::attendance)
                .containsExactly("Y", "Y", "Y");
        assertThat(captor.getValue().get(2)).isEqualTo(remoteOnly);
    }

    @Test
    @DisplayName("allowPartial을 false로 명시하면 매칭 실패 시 저장을 중단한다")
    void explicitStrictModeBlocksUnmatchedStudents() {
        StudentClass local = studentClass(1L, 10L, "로컬전용", LocalDate.of(2010, 1, 2));
        given(studentClassRepository.findAllBySchoolYear(2026)).willReturn(List.of(local));
        given(attendanceRepository.findAllByStudentClassInAndDate(List.of(local), DATE))
                .willReturn(List.of());
        given(acts29Client.open(DATE, "전체", "전체")).willReturn(session);
        given(session.fetchAttendance())
                .willReturn(List.of(remoteRecord(0, "20", "원격전용", "20100203", "Y")));

        assertThatThrownBy(() -> service.sync(
                DATE, new Acts29AttendanceSyncRequest(false, false, DATE, null)
        )).isInstanceOf(ApiException.class).hasMessageContaining("매칭 실패 또는 중복 학생");
        verify(session, never()).saveAttendance(anyList());
    }

    private Attendance attendance(StudentClass studentClass, AttendanceStatus status) {
        Attendance attendance = new Attendance();
        attendance.setStudentClass(studentClass);
        attendance.setDate(DATE);
        attendance.setStatus(status);
        return attendance;
    }

    private StudentClass studentClass(
            Long studentClassId,
            Long studentId,
            String name,
            LocalDate birth
    ) {
        Student student = new Student();
        ReflectionTestUtils.setField(student, "id", studentId);
        student.setName(name);
        student.setBirth(birth);

        StudentClass studentClass = new StudentClass();
        studentClass.setId(studentClassId);
        studentClass.setStudent(student);
        studentClass.setSchoolYear(2026);
        return studentClass;
    }

    private Acts29StudentRecord remoteRecord(
            int index,
            String userId,
            String name,
            String birth,
            String attendance
    ) {
        ObjectNode raw = new ObjectMapper().createObjectNode();
        raw.put("user_id", userId);
        raw.put("seq", "seq-" + index);
        raw.put("name", name);
        raw.put("birth", birth);
        raw.put("attend_yn", attendance);
        raw.put("sau", "");
        raw.put("simbang_yn", "N");
        return Acts29StudentRecord.from(index, raw);
    }
}
