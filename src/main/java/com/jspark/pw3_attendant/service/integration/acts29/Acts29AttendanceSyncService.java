package com.jspark.pw3_attendant.service.integration.acts29;

import com.jspark.pw3_attendant.common.exception.ApiException;
import com.jspark.pw3_attendant.domain.Attendance.Attendance;
import com.jspark.pw3_attendant.domain.Attendance.Attendance.AttendanceStatus;
import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import com.jspark.pw3_attendant.repository.Attendance.AttendanceRepository;
import com.jspark.pw3_attendant.repository.StudentClass.StudentClassRepository;
import com.jspark.pw3_attendant.service.integration.acts29.dto.Acts29AttendanceChange;
import com.jspark.pw3_attendant.service.integration.acts29.dto.Acts29AttendanceSyncRequest;
import com.jspark.pw3_attendant.service.integration.acts29.dto.Acts29AttendanceSyncResponse;
import com.jspark.pw3_attendant.service.integration.acts29.dto.Acts29MatchIssue;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class Acts29AttendanceSyncService {

    private static final String ALL = "전체";
    private static final DateTimeFormatter BIRTH_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final StudentClassRepository studentClassRepository;
    private final AttendanceRepository attendanceRepository;
    private final Acts29Client acts29Client;
    private final ReentrantLock syncLock = new ReentrantLock();

    public Acts29AttendanceSyncResponse sync(LocalDate date, Acts29AttendanceSyncRequest requested) {
        Acts29AttendanceSyncRequest request = requested == null
                ? Acts29AttendanceSyncRequest.defaults()
                : requested;
        validateConfirmation(date, request);

        if (!syncLock.tryLock()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ACTS29_SYNC_IN_PROGRESS",
                    "다른 Acts29 출석 동기화가 진행 중입니다."
            );
        }

        try {
            int schoolYear = schoolYearFor(date);
            String className = normalizeScope(request.className());
            List<LocalAttendance> localAttendances = loadLocalAttendances(date, schoolYear, className);
            if (localAttendances.isEmpty()) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "ACTS29_NO_LOCAL_STUDENTS",
                        className == null
                                ? "해당 학년도의 로컬 학생 데이터가 없습니다."
                                : className + " 반의 로컬 학생 데이터가 없습니다."
                );
            }

            try (Acts29Client.Session session = acts29Client.open(date, ALL, ALL)) {
                List<Acts29StudentRecord> remoteRecords = session.fetchAttendance();
                if (remoteRecords.isEmpty()) {
                    throw new ApiException(
                            HttpStatus.BAD_GATEWAY,
                            "ACTS29_EMPTY_ROSTER",
                            "Acts29 학생 목록이 비어 있어 동기화를 중단했습니다."
                    );
                }

                SyncPlan plan = buildPlan(localAttendances, remoteRecords);
                if (!request.isDryRun()) {
                    validateMatchCompleteness(plan, request);
                    if (!plan.changes().isEmpty()) {
                        ensureRemoteUnchanged(remoteRecords, session.fetchAttendance());
                        session.saveAttendance(plan.updatedRecords());
                        verifySaved(plan, session.fetchAttendance());
                        log.info(
                                "Acts29 attendance sync completed: date={}, matched={}, changed={}, issues={}",
                                date,
                                plan.matchesByRemoteIndex().size(),
                                plan.changes().size(),
                                plan.issues().size()
                        );
                    }
                }

                String result = request.isDryRun()
                        ? "DRY_RUN"
                        : plan.changes().isEmpty() ? "NO_CHANGES" : "SYNCED";
                return toResponse(date, schoolYear, className, request.isDryRun(), result, plan);
            }
        } finally {
            syncLock.unlock();
        }
    }

    private List<LocalAttendance> loadLocalAttendances(
            LocalDate date,
            int schoolYear,
            String className
    ) {
        List<StudentClass> studentClasses = studentClassRepository.findAllBySchoolYear(schoolYear);
        if (className != null) {
            studentClasses = studentClasses.stream()
                    .filter(studentClass -> className.equals(
                            normalizeScope(studentClass.getClassRoom().getName())
                    ))
                    .toList();
        }
        List<Attendance> attendances = attendanceRepository.findAllByStudentClassInAndDate(studentClasses, date);
        Map<Long, AttendanceStatus> statusByStudentClassId = attendances.stream()
                .collect(Collectors.toMap(
                        attendance -> attendance.getStudentClass().getId(),
                        Attendance::getStatus
                ));

        return studentClasses.stream()
                .map(studentClass -> {
                    Student student = studentClass.getStudent();
                    AttendanceStatus status = statusByStudentClassId.getOrDefault(
                            studentClass.getId(),
                            AttendanceStatus.UNCHECKED
                    );
                    return new LocalAttendance(student.getId(), student.getName(), student.getBirth(), status);
                })
                .sorted(Comparator.comparing(LocalAttendance::name).thenComparing(LocalAttendance::studentId))
                .toList();
    }

    private SyncPlan buildPlan(
            List<LocalAttendance> localAttendances,
            List<Acts29StudentRecord> remoteRecords
    ) {
        Map<String, List<Acts29StudentRecord>> remoteByName = remoteRecords.stream()
                .collect(Collectors.groupingBy(record -> normalizeName(record.name())));

        Map<LocalAttendance, Acts29StudentRecord> singleCandidates = new LinkedHashMap<>();
        List<Acts29MatchIssue> issues = new ArrayList<>();
        for (LocalAttendance local : localAttendances) {
            List<Acts29StudentRecord> candidates = remoteByName
                    .getOrDefault(normalizeName(local.name()), List.of())
                    .stream()
                    .filter(remote -> birthMatches(local.birth(), remote.birth()))
                    .toList();
            if (candidates.isEmpty()) {
                issues.add(issue(local, "이름과 생년월일이 일치하는 Acts29 학생이 없습니다."));
            } else if (candidates.size() > 1) {
                issues.add(issue(local, "Acts29에서 이름과 생년월일이 같은 학생이 여러 명입니다."));
            } else {
                singleCandidates.put(local, candidates.get(0));
            }
        }

        Map<String, List<LocalAttendance>> claimsByRemote = singleCandidates.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getValue().identityKey(),
                        LinkedHashMap::new,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));

        Map<Integer, Match> matchByRemoteIndex = new HashMap<>();
        for (Map.Entry<String, List<LocalAttendance>> claim : claimsByRemote.entrySet()) {
            List<LocalAttendance> locals = claim.getValue();
            if (locals.size() > 1) {
                locals.forEach(local -> issues.add(issue(
                        local,
                        "여러 로컬 학생이 동일한 Acts29 학생과 매칭됩니다."
                )));
                continue;
            }
            LocalAttendance local = locals.get(0);
            Acts29StudentRecord remote = singleCandidates.get(local);
            matchByRemoteIndex.put(remote.index(), new Match(local, remote, toActs29Attendance(local.status())));
        }

        List<Acts29StudentRecord> updated = new ArrayList<>(remoteRecords.size());
        List<Acts29AttendanceChange> changes = new ArrayList<>();
        int remotePresentBefore = 0;
        int remotePresentAfter = 0;
        for (Acts29StudentRecord remote : remoteRecords) {
            if ("Y".equalsIgnoreCase(remote.attendance())) {
                remotePresentBefore++;
            }
            Match match = matchByRemoteIndex.get(remote.index());
            Acts29StudentRecord next = match == null
                    ? remote
                    : remote.withAttendance(match.desiredAttendance());
            updated.add(next);
            if ("Y".equalsIgnoreCase(next.attendance())) {
                remotePresentAfter++;
            }
            if (match != null && !sameAttendance(remote.attendance(), match.desiredAttendance())) {
                changes.add(new Acts29AttendanceChange(
                        match.local().studentId(),
                        match.local().name(),
                        match.local().status().name(),
                        normalizeAttendance(remote.attendance()),
                        match.desiredAttendance(),
                        remote.userId()
                ));
            }
        }

        issues.sort(Comparator.comparing(Acts29MatchIssue::studentName)
                .thenComparing(Acts29MatchIssue::studentId));
        return new SyncPlan(
                localAttendances.size(),
                remoteRecords.size(),
                matchByRemoteIndex,
                updated,
                changes,
                issues,
                remotePresentBefore,
                remotePresentAfter
        );
    }

    private void validateConfirmation(LocalDate date, Acts29AttendanceSyncRequest request) {
        if (!request.isDryRun() && !date.equals(request.confirmDate())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ACTS29_CONFIRM_DATE_MISMATCH",
                    "실제 반영 시 confirmDate가 경로의 날짜와 일치해야 합니다."
            );
        }
    }

    private void validateMatchCompleteness(SyncPlan plan, Acts29AttendanceSyncRequest request) {
        if (!plan.issues().isEmpty() && !request.isPartialAllowed()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ACTS29_MATCH_INCOMPLETE",
                    "매칭 실패 또는 중복 학생이 " + plan.issues().size()
                            + "명 있습니다. dry-run 결과를 확인하거나 allowPartial을 사용하세요."
            );
        }
    }

    private void ensureRemoteUnchanged(
            List<Acts29StudentRecord> initiallyLoaded,
            List<Acts29StudentRecord> latest
    ) {
        if (!remoteState(initiallyLoaded).equals(remoteState(latest))) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ACTS29_REMOTE_CHANGED",
                    "동기화 준비 중 Acts29 출석 데이터가 변경되어 반영을 중단했습니다. 다시 시도해 주세요."
            );
        }
    }

    private void verifySaved(SyncPlan plan, List<Acts29StudentRecord> savedRecords) {
        Map<String, String> savedAttendance = savedRecords.stream()
                .collect(Collectors.toMap(
                        Acts29StudentRecord::identityKey,
                        record -> normalizeAttendance(record.attendance()),
                        (first, ignored) -> first
                ));

        List<String> failures = plan.matchesByRemoteIndex().values().stream()
                .filter(match -> !match.desiredAttendance().equals(
                        savedAttendance.get(match.remote().identityKey())
                ))
                .map(match -> match.remote().identityKey())
                .toList();
        if (!failures.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "ACTS29_SAVE_VERIFICATION_FAILED",
                    "Acts29 저장 후 " + failures.size() + "명의 출석 상태가 일치하지 않습니다."
            );
        }
    }

    private List<String> remoteState(List<Acts29StudentRecord> records) {
        return records.stream()
                .map(record -> record.identityKey()
                        + "|" + normalizeAttendance(record.attendance())
                        + "|" + record.absenceReason())
                .sorted()
                .toList();
    }

    private Acts29AttendanceSyncResponse toResponse(
            LocalDate date,
            int schoolYear,
            String className,
            boolean dryRun,
            String result,
            SyncPlan plan
    ) {
        return new Acts29AttendanceSyncResponse(
                date,
                schoolYear,
                className,
                result,
                dryRun,
                plan.localCount(),
                plan.remoteCount(),
                plan.matchesByRemoteIndex().size(),
                plan.changes().size(),
                plan.localCount() - plan.matchesByRemoteIndex().size(),
                plan.remotePresentBefore(),
                plan.remotePresentAfter(),
                List.copyOf(plan.changes()),
                List.copyOf(plan.issues())
        );
    }

    private boolean birthMatches(LocalDate localBirth, String remoteBirth) {
        if (localBirth == null || remoteBirth == null) {
            return false;
        }
        String digits = remoteBirth.replaceAll("[^0-9]", "");
        if (digits.length() > 8) {
            digits = digits.substring(0, 8);
        }
        String localFull = localBirth.format(BIRTH_FORMAT);
        return localFull.equals(digits)
                || (digits.length() == 6 && localFull.substring(2).equals(digits));
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private String normalizeScope(String className) {
        if (className == null || className.isBlank()) {
            return null;
        }
        return className.replaceAll("\\s+", "");
    }

    private String toActs29Attendance(AttendanceStatus status) {
        return status == AttendanceStatus.ATTEND || status == AttendanceStatus.LATE ? "Y" : "N";
    }

    private boolean sameAttendance(String first, String second) {
        return normalizeAttendance(first).equals(normalizeAttendance(second));
    }

    private String normalizeAttendance(String attendance) {
        return "Y".equalsIgnoreCase(attendance) ? "Y" : "N";
    }

    private Acts29MatchIssue issue(LocalAttendance local, String reason) {
        return new Acts29MatchIssue(local.studentId(), local.name(), local.birth(), reason);
    }

    private int schoolYearFor(LocalDate date) {
        return date.getMonthValue() >= 3 ? date.getYear() : date.getYear() - 1;
    }

    private record LocalAttendance(
            Long studentId,
            String name,
            LocalDate birth,
            AttendanceStatus status
    ) {
    }

    private record Match(
            LocalAttendance local,
            Acts29StudentRecord remote,
            String desiredAttendance
    ) {
    }

    private record SyncPlan(
            int localCount,
            int remoteCount,
            Map<Integer, Match> matchesByRemoteIndex,
            List<Acts29StudentRecord> updatedRecords,
            List<Acts29AttendanceChange> changes,
            List<Acts29MatchIssue> issues,
            int remotePresentBefore,
            int remotePresentAfter
    ) {
    }
}
