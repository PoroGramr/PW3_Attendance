package com.jspark.pw3_attendant.service.integration.acts29.dto;

import java.time.LocalDate;
import java.util.List;

public record Acts29AttendanceSyncResponse(
        LocalDate date,
        int schoolYear,
        String className,
        String result,
        boolean dryRun,
        int localStudentCount,
        int remoteStudentCount,
        int matchedCount,
        int changedCount,
        int unmatchedTargetCount,
        int remotePresentBefore,
        int remotePresentAfter,
        List<Acts29AttendanceChange> changes,
        List<Acts29MatchIssue> matchIssues,
        // Acts29 출석(Y)을 기준으로 로컬 출석을 지각(LATE)으로 바꾼(dry-run이면 바꿀) 학생
        int localLateUpdatedCount,
        List<Acts29LocalAttendanceUpdate> localLateUpdates
) {
}
