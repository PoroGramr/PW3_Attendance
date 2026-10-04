package com.jspark.pw3_attendant.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.acts29")
public record Acts29Properties(
        String userId,
        String password,
        String baseUrl,
        boolean headless,
        String browserChannel,
        Duration timeout
) {
}
