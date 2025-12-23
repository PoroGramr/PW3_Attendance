package com.jspark.pw3_attendant.scheduler;

import com.jspark.pw3_attendant.service.Attendance.AttendanceService;
import com.jspark.pw3_attendant.service.message.CoolMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceReportScheduler {

    private final AttendanceService attendanceService;
    private final CoolMessageService coolMessageService;

    @Value("${app.report.recipient-phone-number}")
    private String recipientPhoneNumber;

    /**
     * 매주 일요일 오전 10시에 실행됩니다.
     * 당일 출석 현황 리포트를 생성하여 지정된 번호로 SMS를 발송합니다.
     */
    @Scheduled(cron = "0 0 10 * * SUN", zone = "Asia/Seoul")
    public void sendWeeklyAttendanceReport() {
        log.info("Scheduler: Starting weekly attendance report job.");
        try {
            // 당일 날짜로 리포트 생성
            LocalDate today = LocalDate.now();
            String report = attendanceService.getDailyAttendanceReport(today);

            // 지정된 번호로 SMS 발송
            if (recipientPhoneNumber != null && !recipientPhoneNumber.isBlank()) {
                coolMessageService.sendSms(recipientPhoneNumber, report);
                log.info("Scheduler: Successfully sent attendance report to {}.", recipientPhoneNumber);
            } else {
                log.warn("Scheduler: Recipient phone number for report is not configured. Skipping SMS.");
            }
        } catch (Exception e) {
            log.error("Scheduler: Failed to send weekly attendance report.", e);
        }
    }

//    @Scheduled(cron = "0 45 13 * * Tue", zone = "Asia/Seoul")
//    public void sendWeeklyAttendanceReportTest() {
//        log.info("Scheduler: Starting weekly attendance report job.");
//        try {
//            // 당일 날짜로 리포트 생성
//            LocalDate today = LocalDate.now();
//            String report = attendanceService.getDailyAttendanceReport(today);
//
//            // 지정된 번호로 SMS 발송
//            if (recipientPhoneNumber != null && !recipientPhoneNumber.isBlank()) {
//                coolMessageService.sendSms(recipientPhoneNumber, report);
//                log.info("Scheduler: Successfully sent attendance report to {}.", recipientPhoneNumber);
//            } else {
//                log.warn("Scheduler: Recipient phone number for report is not configured. Skipping SMS.");
//            }
//        } catch (Exception e) {
//            log.error("Scheduler: Failed to send weekly attendance report.", e);
//        }
//    }
}
