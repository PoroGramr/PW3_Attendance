package com.jspark.pw3_attendant.service.message;

import com.jspark.pw3_attendant.domain.Student.Student;

public interface MessageService {
    /**
     * Sends a message to a student.
     * @param student The recipient student.
     * @param content The message content.
     * @return true if the message was sent successfully, false otherwise.
     */
    boolean sendMessage(Student student, String content);
}
