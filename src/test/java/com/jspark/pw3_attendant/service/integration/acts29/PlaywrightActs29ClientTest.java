package com.jspark.pw3_attendant.service.integration.acts29;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jspark.pw3_attendant.common.config.Acts29Properties;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlaywrightActs29ClientTest {

    @Test
    @DisplayName("ACTS29 페이지와 실제 조회·저장 요청은 날짜를 yyyyMMdd로 전달한다")
    void sendsNumericDateOnPageFetchAndSave() throws Exception {
        List<String> requests = new CopyOnWriteArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/child/choolsuk.do", exchange -> {
            requests.add(URLDecoder.decode(
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8));
            byte[] response = "{\"list\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try (Playwright playwright = Playwright.create()) {
            var requestContext = playwright.request().newContext();
            try {
                String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
                var client = new PlaywrightActs29Client(
                        new Acts29Properties("test", "test", baseUrl, true, null, Duration.ofSeconds(5)),
                        new ObjectMapper());
                LocalDate date = LocalDate.of(2026, 10, 11);
                String targetUrl = ReflectionTestUtils.invokeMethod(client, "targetUrl", date, "전체", "전체");
                assertThat(targetUrl).contains("bogoday=20261011");

                Page page = mock(Page.class);
                when(page.request()).thenReturn(requestContext);
                var constructor = Class.forName(PlaywrightActs29Client.class.getName() + "$PlaywrightSession")
                        .getDeclaredConstructors()[0];
                constructor.setAccessible(true);
                try (var session = (Acts29Client.Session) constructor.newInstance(
                        client, null, null, null, page, date, "전체", "전체")) {
                    assertThat(session.fetchAttendance()).isEmpty();
                    var raw = new ObjectMapper().createObjectNode();
                    raw.put("user_id", "123");
                    raw.put("attend_yn", "Y");
                    raw.put("sau", "기존 사유");
                    raw.put("bogoday", "2026-10-11");
                    raw.put("pass_day", "2025-03-02");
                    session.saveAttendance(List.of(Acts29StudentRecord.from(0, raw)));
                    assertThat(raw.path("bogoday").asText()).isEqualTo("2026-10-11");
                    assertThat(raw.path("pass_day").asText()).isEqualTo("2025-03-02");
                }
                assertThat(requests).hasSize(2).allSatisfy(body -> {
                    assertThat(body).contains("bogoday=20261011").doesNotContain("2026-10-11");
                });
                assertThat(requests.get(1)).contains("sau_arr=기존 사유", "\"attend_yn\":\"Y\"");
                assertThat(requests.get(1)).contains("\"bogoday\":\"20261011\"", "\"pass_day\":\"20250302\"");
            } finally {
                requestContext.dispose();
            }
        } finally {
            server.stop(0);
        }
    }
}
