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
        List<Acts29MatchIssue> matchIssues
) {
}
