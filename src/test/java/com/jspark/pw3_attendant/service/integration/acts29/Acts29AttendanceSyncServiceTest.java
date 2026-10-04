package com.jspark.pw3_attendant.service.integration.acts29;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
