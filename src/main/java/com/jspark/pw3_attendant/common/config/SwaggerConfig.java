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
        @Server(url = "https://api.pw3hub.xyz/", description = "aws Production Server"),
        @Server(url = "https://pw3hubapi.porogramr.site/", description = "aws alb Production Server"),
        @Server(url = "https://pw3api.porogramr.site", description = "Production Server")

    }
)
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("출석 체크 시스템 API 문서")
                .description("### [실시간 출석 업데이트 웹소켓 안내]\n\n" +
                             "본 시스템은 실시간 출석 업데이트를 위해 WebSocket(STOMP)을 지원합니다.\n\n" +
                             "- **연결 엔드포인트**: `/ws-attendance` (SockJS 사용 권장)\n" +
                             "- **구독 경로(Topic)**: `/topic/attendance` \n" +
                             "- **메시지 형식**: \n" +
                             "```json\n" +
                             "{\n" +
                             "  \"type\": \"STUDENT | TEACHER | PARENT\",\n" +
                             "  \"name\": \"이름\",\n" +
                             "  \"status\": \"ATTEND | LATE | ...\",\n" +
                             "  \"timestamp\": \"2024-05-11T22:00:00\"\n" +
                             "}\n" +
                             "```\n\n" +
                             "--- \n" +
                             "학생-반-출석 관리용 Swagger 문서입니다.")
                .version("v1.0.0")
            );
    }
}
