package com.jspark.pw3_attendant.service.auth;

import com.jspark.pw3_attendant.common.config.AccountRecoveryProperties;
import com.jspark.pw3_attendant.domain.admin.RecoveryChannel;
import com.jspark.pw3_attendant.service.message.CoolMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSmsAccountRecoveryNotifier implements AccountRecoveryNotifier {

    private final JavaMailSender mailSender;
    private final CoolMessageService coolMessageService;
    private final AccountRecoveryProperties properties;

    @Override
    public void send(RecoveryChannel channel, String destination, String subject, String content) {
        if (channel == RecoveryChannel.EMAIL) {
            sendEmail(destination, subject, content);
            return;
        }
        coolMessageService.sendSensitiveSms(destination, content);
    }

    private void sendEmail(String destination, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.mailFrom());
        message.setTo(destination);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}
