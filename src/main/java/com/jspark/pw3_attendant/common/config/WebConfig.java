package com.jspark.pw3_attendant.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000","http://localhost:8080", "https://pw-3-attendance-fe.vercel.app/","https://pw3hubapi.porogramr.site/" ,"https://api.pw3hub.xyz//")
            .allowedMethods("*");
    }
}