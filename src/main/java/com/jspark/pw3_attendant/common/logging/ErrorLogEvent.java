package com.jspark.pw3_attendant.common.logging;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

public class ErrorLogEvent extends ApplicationEvent {

    private final String message;
    private final String stackTrace;
    private final LocalDateTime occurredAt;

    public ErrorLogEvent(Object source, String message, String stackTrace) {
        super(source);
        this.message = message;
        this.stackTrace = stackTrace;
        this.occurredAt = LocalDateTime.now();
    }

    public String getMessage() {
        return message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
