package com.jspark.pw3_attendant.common.logging;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Spring 컨텍스트가 완전히 초기화된 후, ErrorLogAppender에 ApplicationContext를 주입합니다.
 */
@Component
@RequiredArgsConstructor
public class AppenderContextInjector {

    private final ApplicationContext applicationContext;

    @PostConstruct
    public void injectContext() {
        ErrorLogAppender.setApplicationContext(applicationContext);
    }
}
