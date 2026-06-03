package com.jspark.pw3_attendant.service.Attendance;

import com.jspark.pw3_attendant.service.Attendance.dto.MonthlyAttendanceMarkdownResponse;
import com.jspark.pw3_attendant.service.Attendance.dto.MonthlyAttendanceReportRequest;
import com.jspark.pw3_attendant.service.Attendance.dto.MonthlyClassAttendanceReportResponse;
import com.jspark.pw3_attendant.service.ai.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyAttendanceReportService {

    private static final double DEFAULT_WEAK_CLASS_THRESHOLD = 60.0;

    private final AttendanceAnalysisService attendanceAnalysisService;
    private final AiChatService aiChatService;

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

        return MonthlyAttendanceMarkdownResponse.builder()
                .year(request.getYear())
                .month(request.getMonth())
                .schoolYear(request.getSchoolYear())
                .markdown(markdown)
                .reportData(reportData)
                .build();
    }
}
