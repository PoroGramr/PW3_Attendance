package com.jspark.pw3_attendant.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    servers = {
        @Server(url = "http://localhost:8080", description = "Local Dev Server"),
        @Server(url = "https://pw3api.porogramr.site", description = "Production Server")
    }
)
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("출석 체크 시스템 API 문서")
                .description("학생-반-출석 관리용 Swagger 문서입니다.")
                .version("v1.0.0")
            );
    }
}
