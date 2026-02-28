package com.jspark.pw3_attendant.common.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import org.springframework.context.ApplicationContext;

public class ErrorLogAppender extends AppenderBase<ILoggingEvent> {

    // Spring ApplicationContext는 서버 기동 후 주입됨 (정적 참조)
    private static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
    }

    @Override
    protected void append(ILoggingEvent event) {
        // ERROR 레벨만 처리
        if (!Level.ERROR.equals(event.getLevel())) {
            return;
        }

        // Spring 컨텍스트가 아직 준비되지 않으면 무시
        if (applicationContext == null) {
            return;
        }

        String message = event.getFormattedMessage();
        String stackTrace = extractStackTrace(event);

        try {
            applicationContext.publishEvent(
                    new ErrorLogEvent(this, message, stackTrace));
        } catch (Exception e) {
            // Appender 내부 예외는 무시 (무한 루프 방지)
            addError("ErrorLogAppender 이벤트 발행 실패", e);
        }
    }

    private String extractStackTrace(ILoggingEvent event) {
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(throwableProxy.getClassName()).append(": ").append(throwableProxy.getMessage()).append("\n");

        StackTraceElementProxy[] stackTraceElements = throwableProxy.getStackTraceElementProxyArray();
        if (stackTraceElements != null) {
            int limit = Math.min(stackTraceElements.length, 15); // 최대 15줄
            for (int i = 0; i < limit; i++) {
                sb.append("  at ").append(stackTraceElements[i].getSTEAsString()).append("\n");
            }
            if (stackTraceElements.length > 15) {
                sb.append("  ... ").append(stackTraceElements.length - 15).append(" more\n");
            }
        }

        return sb.toString();
    }
}
