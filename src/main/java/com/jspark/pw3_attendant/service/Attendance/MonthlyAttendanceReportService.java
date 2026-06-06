package com.jspark.pw3_attendant.service.Attendance;

import com.jspark.pw3_attendant.service.Attendance.dto.MonthlyAttendanceMarkdownResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.MonthlyAttendanceReportRequest;
import com.jspark.pw3_attendant.service.Attendance.dto.MonthlyClassAttendanceReportResponse;
import com.jspark.pw3_attendant.service.ai.AiChatService;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyAttendanceReportService {

    private static final double DEFAULT_WEAK_CLASS_THRESHOLD = 60.0;

    private final ConcurrentMap<MonthlyReportCacheKey, MonthlyAttendanceMarkdownResponse> reportCache =
            new ConcurrentHashMap<>();

    private final AttendanceAnalysisService attendanceAnalysisService;
    private final AiChatService aiChatService;

    public Optional<MonthlyAttendanceMarkdownResponse> getCachedMonthlyReport(
            int year,
            int month,
            int schoolYear,
            Double weakClassThreshold) {
        return Optional.ofNullable(reportCache.get(createCacheKey(year, month, schoolYear, weakClassThreshold)));
    }

    public MonthlyAttendanceMarkdownResponse generateMonthlyReport(MonthlyAttendanceReportRequest request) {
        double weakClassThreshold = request.getWeakClassThreshold() == null
                ? DEFAULT_WEAK_CLASS_THRESHOLD
                : request.getWeakClassThreshold();

        MonthlyClassAttendanceReportResponse reportData = attendanceAnalysisService.getMonthlyClassAttendanceReport(
                request.getYear(),
                request.getMonth(),
                request.getSchoolYear(),
                weakClassThreshold);

        String markdown = aiChatService.generateMonthlyAttendanceMarkdown(reportData);

        MonthlyAttendanceMarkdownResponse response = MonthlyAttendanceMarkdownResponse.builder()
                .year(request.getYear())
                .month(request.getMonth())
                .schoolYear(request.getSchoolYear())
                .markdown(markdown)
                .reportData(reportData)
                .build();

        reportCache.put(createCacheKey(
                request.getYear(),
                request.getMonth(),
                request.getSchoolYear(),
                request.getWeakClassThreshold()), response);

        return response;
    }

    private MonthlyReportCacheKey createCacheKey(int year, int month, int schoolYear, Double weakClassThreshold) {
        double normalizedWeakClassThreshold = weakClassThreshold == null
                ? DEFAULT_WEAK_CLASS_THRESHOLD
                : weakClassThreshold;
        return new MonthlyReportCacheKey(year, month, schoolYear, normalizedWeakClassThreshold);
    }

    private record MonthlyReportCacheKey(int year, int month, int schoolYear, double weakClassThreshold) {
    }
}
