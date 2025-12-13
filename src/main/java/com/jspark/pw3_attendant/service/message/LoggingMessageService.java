package com.jspark.pw3_attendant.service.message;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.message_log.MessageLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoggingMessageService implements MessageService {

    @Override
    public boolean sendMessage(Student student, String content, String imageUrl) {
        log.info("-----> Sending message to: {} (ID: {})", student.getName(), student.getId());
        log.info("-----> Message content: {}", content);
        if (imageUrl != null && !imageUrl.isBlank()) {
            log.info("-----> Image URL: {}", imageUrl);
        }
        log.info("-----> Message sent successfully (simulation).");
        return true;
    }

    @Override
    public MessageLog.MessageChannel getMessageChannel() {
        return MessageLog.MessageChannel.SMS; // Default or fallback channel
    }
}
