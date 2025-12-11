package com.jspark.pw3_attendant.service.message;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.message_log.MessageLog;

public interface MessageService {
    /**
     * Sends a message to a student.
     * @param student The recipient student.
     * @param content The message content.
     * @param imageUrl The URL of an image to send, or null if text-only.
     * @return true if the message was sent successfully, false otherwise.
     */
    boolean sendMessage(Student student, String content, String imageUrl);
    /**
     * Returns the MessageChannel this service implementation handles.
     * @return The MessageChannel enum value.
     */
    MessageLog.MessageChannel getMessageChannel();
}
