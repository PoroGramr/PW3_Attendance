package com.jspark.pw3_attendant.service.message;

import com.jspark.pw3_attendant.domain.Student.Student;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoggingMessageService implements MessageService {

    @Override
    public boolean sendMessage(Student student, String content) {
        // This is a dummy implementation that logs the message to the console.
        // In a real-world scenario, this service would integrate with an external SMS/Kakao/Email provider.
        log.info("-----> Sending message to: {} (ID: {})", student.getName(), student.getId());
        log.info("-----> Message content: {}", content);
        log.info("-----> Message sent successfully (simulation).");
        return true;
    }
}
