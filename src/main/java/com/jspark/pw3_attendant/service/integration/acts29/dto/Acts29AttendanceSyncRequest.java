package com.jspark.pw3_attendant.service.integration.acts29.dto;

import java.time.LocalDate;

public record Acts29AttendanceSyncRequest(
        Boolean dryRun,
        Boolean allowPartial,
        LocalDate confirmDate,
        String className
) {
    public static Acts29AttendanceSyncRequest defaults() {
        return new Acts29AttendanceSyncRequest(true, false, null, null);
    }

    public boolean isDryRun() {
        return !Boolean.FALSE.equals(dryRun);
    }

    public boolean isPartialAllowed() {
        return Boolean.TRUE.equals(allowPartial);
    }
}
