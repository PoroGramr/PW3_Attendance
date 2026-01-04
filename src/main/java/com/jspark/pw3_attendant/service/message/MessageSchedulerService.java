package com.jspark.pw3_attendant.service.message;

import com.jspark.pw3_attendant.service.Attendance.AttendanceService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSchedulerService {

    private final AttendanceService attendanceService;
    private final CoolMessageService coolMessageService;

    @Value("${app.report.recipients:}") // Default to empty string if not set
    private String recipientPhoneNumbers;

    @Scheduled(cron = "0 0 10 * * SUN", zone = "Asia/Seoul")
    public void sendWeeklySundayReport() {
        log.info("Sending weekly Sunday report...");

        if (recipientPhoneNumbers.isEmpty()) {
            log.warn("No recipient phone numbers configured for weekly report. Skipping.");
            return;
        }

        // 1. Get the daily attendance report
        String report = attendanceService.getDailyAttendanceReport(LocalDate.now());

        // 2. Define the list of recipients from environment variable
        List<String> recipients = Arrays.asList(recipientPhoneNumbers.split(","));

        // 3. Send the message to each recipient
        for (String phoneNumber : recipients) {
            try {
                coolMessageService.sendSms(phoneNumber.trim(), report);
                log.info("Successfully sent weekly report to {}", phoneNumber.trim());
            } catch (Exception e) {
                log.error("Failed to send weekly report to {}: {}", phoneNumber.trim(), e.getMessage(), e);
            }
        }
    }
}
