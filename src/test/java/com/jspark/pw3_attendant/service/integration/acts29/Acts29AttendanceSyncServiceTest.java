package com.jspark.pw3_attendant.service.integration.acts29;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpStatus;

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
                .containsExactly("Y", "Y");
        assertThat(captor.getValue()).doesNotContain(remoteOnly);
        assertThat(response.remotePresentAfter()).isEqualTo(3);
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

    @ParameterizedTest
    @ValueSource(strings = {"김하윤A", "김하윤b", " 김 하윤 AB "})
    @DisplayName("이름 끝 영문 구분자를 제거하고 생년월일로 동명이인을 구분해 반영한다")
    void matchesAlphabetSuffixByBirth(String localName) {
        StudentClass local = studentClass(1L, 10L, localName, LocalDate.of(2010, 1, 2));
        Acts29StudentRecord target = remoteRecord(0, "10", "김하윤", "20100102", "N");
        Acts29StudentRecord other = remoteRecord(1, "20", "김하윤", "20100203", "N");
        List<Acts29StudentRecord> before = List.of(target, other);
        given(studentClassRepository.findAllBySchoolYear(2026)).willReturn(List.of(local));
        given(attendanceRepository.findAllByStudentClassInAndDate(List.of(local), DATE))
                .willReturn(List.of(attendance(local, AttendanceStatus.ATTEND)));
        given(acts29Client.open(DATE, "전체", "전체")).willReturn(session);
        given(session.fetchAttendance()).willReturn(before, before, List.of(target.withAttendance("Y"), other));

        Acts29AttendanceSyncResponse response = service.sync(
                DATE, new Acts29AttendanceSyncRequest(false, null, DATE, null)
        );

        assertThat(response.matchedCount()).isEqualTo(1);
        assertThat(response.matchIssues()).isEmpty();
        ArgumentCaptor<List<Acts29StudentRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(session).saveAttendance(captor.capture());
        assertThat(captor.getValue()).extracting(Acts29StudentRecord::attendance)
                .containsExactly("Y");
        assertThat(local.getStudent().getName()).isEqualTo(localName);
        assertThat(captor.getValue().get(0).name()).isEqualTo("김하윤");
    }

    @Test
    @DisplayName("영문 구분자 제거 후 두 로컬 학생의 이름과 생년월일이 같으면 중복으로 건너뛴다")
    void skipsDuplicateLocalsAfterRemovingSuffix() {
        StudentClass first = studentClass(1L, 10L, "김하윤A", LocalDate.of(2010, 1, 2));
        StudentClass second = studentClass(2L, 20L, "김하윤B", LocalDate.of(2010, 1, 2));
        List<StudentClass> locals = List.of(first, second);
        given(studentClassRepository.findAllBySchoolYear(2026)).willReturn(locals);
        given(attendanceRepository.findAllByStudentClassInAndDate(locals, DATE))
                .willReturn(List.of(attendance(first, AttendanceStatus.ATTEND), attendance(second, AttendanceStatus.LATE)));
        given(acts29Client.open(DATE, "전체", "전체")).willReturn(session);
        given(session.fetchAttendance()).willReturn(List.of(remoteRecord(0, "10", "김하윤", "20100102", "N")));

        Acts29AttendanceSyncResponse response = service.sync(
                DATE, new Acts29AttendanceSyncRequest(false, null, DATE, null)
        );

        assertThat(response.result()).isEqualTo("NO_CHANGES");
        assertThat(response.matchedCount()).isZero();
        assertThat(response.matchIssues()).hasSize(2);
        verify(session, never()).saveAttendance(anyList());
    }

    @Test
    @DisplayName("일괄 저장 일부 반영 후 500이면 미반영 학생만 재시도하고 실패 학생 뒤의 학생도 반영한다")
    void retriesOutstandingStudentsAfterPartialBulkFailure() {
        StudentClass first = studentClass(1L, 10L, "기반영학생", LocalDate.of(2010, 1, 2));
        StudentClass bad = studentClass(2L, 20L, "저장실패", LocalDate.of(2010, 2, 3));
        StudentClass good = studentClass(3L, 30L, "재시도학생", LocalDate.of(2010, 3, 4));
        List<StudentClass> locals = List.of(first, bad, good);
        // Real ACTS29 records can have no user_id; seq still identifies them.
        Acts29StudentRecord firstRemote = remoteRecord(0, "", "기반영학생", "20100102", "N");
        Acts29StudentRecord badRemote = remoteRecord(1, "", "저장실패", "20100203", "N");
        Acts29StudentRecord goodRemote = remoteRecord(2, "", "재시도학생", "20100304", "N");
        List<Acts29StudentRecord> before = List.of(firstRemote, badRemote, goodRemote);
        List<Acts29StudentRecord> partlySaved = List.of(firstRemote.withAttendance("Y"), badRemote, goodRemote);
        List<Acts29StudentRecord> after = List.of(firstRemote.withAttendance("Y"), badRemote, goodRemote.withAttendance("Y"));
        given(studentClassRepository.findAllBySchoolYear(2026)).willReturn(locals);
        given(attendanceRepository.findAllByStudentClassInAndDate(locals, DATE))
                .willReturn(List.of(attendance(first, AttendanceStatus.ATTEND), attendance(bad, AttendanceStatus.LATE),
                        attendance(good, AttendanceStatus.LATE)));
        given(acts29Client.open(DATE, "전체", "전체")).willReturn(session);
        given(session.fetchAttendance()).willReturn(before, before, partlySaved, partlySaved, after, after);
        ApiException error = new ApiException(HttpStatus.BAD_GATEWAY, "ACTS29_REQUEST_FAILED", "저장 실패 (HTTP 500)");
        doThrow(error).when(session).saveAttendance(List.of(firstRemote.withAttendance("Y"),
                badRemote.withAttendance("Y"), goodRemote.withAttendance("Y")));
        doThrow(error).when(session).saveAttendance(List.of(badRemote.withAttendance("Y")));

        Acts29AttendanceSyncResponse response = service.sync(
                DATE, new Acts29AttendanceSyncRequest(false, null, DATE, null)
        );

        assertThat(response.result()).isEqualTo("PARTIALLY_SYNCED");
        assertThat(response.matchedCount()).isEqualTo(2);
        assertThat(response.changedCount()).isEqualTo(2);
        assertThat(response.remotePresentAfter()).isEqualTo(2);
        assertThat(response.matchIssues()).singleElement().satisfies(issue -> {
            assertThat(issue.studentId()).isEqualTo(20L);
            assertThat(issue.reason()).contains("저장에 실패");
        });
        verify(session, never()).saveAttendance(List.of(firstRemote.withAttendance("Y")));
        verify(session).saveAttendance(List.of(badRemote.withAttendance("Y")));
        verify(session).saveAttendance(List.of(goodRemote.withAttendance("Y")));
    }

    @ParameterizedTest
    @CsvSource({"false, 500", "true, 503"})
    @DisplayName("엄격 모드 또는 500 이외의 오류는 학생별 재시도 없이 반환한다")
    void doesNotRetryStrictModeOrOtherErrors(boolean allowPartial, int status) {
        StudentClass local = studentClass(1L, 10L, "테스트학생", LocalDate.of(2010, 1, 2));
        Acts29StudentRecord before = remoteRecord(0, "10", "테스트학생", "20100102", "N");
        given(studentClassRepository.findAllBySchoolYear(2026)).willReturn(List.of(local));
        given(attendanceRepository.findAllByStudentClassInAndDate(List.of(local), DATE))
                .willReturn(List.of(attendance(local, AttendanceStatus.ATTEND)));
        given(acts29Client.open(DATE, "전체", "전체")).willReturn(session);
        given(session.fetchAttendance()).willReturn(List.of(before));
        ApiException error = new ApiException(HttpStatus.BAD_GATEWAY, "ACTS29_REQUEST_FAILED", "실패 (HTTP " + status + ")");
        doThrow(error).when(session).saveAttendance(anyList());

        assertThatThrownBy(() -> service.sync(DATE,
                new Acts29AttendanceSyncRequest(false, allowPartial, DATE, null))).isSameAs(error);
        verify(session, times(2)).fetchAttendance();
        verify(session, times(1)).saveAttendance(anyList());
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
