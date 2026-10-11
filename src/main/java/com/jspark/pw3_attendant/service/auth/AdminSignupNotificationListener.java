package com.jspark.pw3_attendant.service.auth;

import com.jspark.pw3_attendant.domain.admin.RecoveryChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class AdminSignupNotificationListener {
    private final AccountRecoveryNotifier notifier;
    private final String recipient;

    public AdminSignupNotificationListener(AccountRecoveryNotifier notifier,
            @Value("${app.signup-notification.recipient:pjs9177@naver.com}") String recipient) {
        this.notifier = notifier;
        this.recipient = recipient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSignupRequested(AdminSignupRequested event) {
        String content = "[PW3] 새로운 관리자 회원가입 요청이 있습니다.\n"
                + "계정 ID: " + event.accountId() + "\n"
                + "아이디: " + singleLine(event.username()) + "\n"
                + "이름: " + singleLine(event.name()) + "\n"
                + "이메일: " + singleLine(event.email()) + "\n"
                + "전화번호: " + singleLine(event.phone()) + "\n"
                + "요청 시각 (서버 시간): " + event.requestedAt() + "\n"
                + "상태: 승인 대기(PENDING)\n"
                + "관리자 화면에서 신청자를 확인한 후 승인 또는 거절해 주세요.";
        try {
            notifier.send(RecoveryChannel.EMAIL, recipient,
                    "[PW3] 관리자 회원가입 승인 요청", content);
        } catch (RuntimeException exception) {
            // Signup is already committed. SMTP outages must not fail signup.
            // Do not log the message, recipient, or exception text (possible PII).
            log.error("Signup notification failed: accountId={}, errorType={}",
                    event.accountId(), exception.getClass().getSimpleName());
        }
    }

    private static String singleLine(String value) {
        return value.replace('\r', ' ').replace('\n', ' ');
    }
}
