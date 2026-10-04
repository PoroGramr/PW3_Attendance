package com.jspark.pw3_attendant.service.integration.acts29;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jspark.pw3_attendant.common.config.Acts29Properties;
import com.jspark.pw3_attendant.common.exception.ApiException;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import com.microsoft.playwright.options.WaitUntilState;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaywrightActs29Client implements Acts29Client {

    private static final String EMAIL_LOGIN_BUTTON = "button:has-text('이메일로 계속하기'):visible";
    private static final String USERNAME_INPUT = "input[name='userid']:visible, input[name='userId']:visible, "
            + "input[name='username']:visible, input[name='email']:visible, "
            + "input[type='email']:visible, input[type='text']:visible";
    private static final String PASSWORD_INPUT = "input[type='password']:visible";
    private static final String NCARE_BUTTON = "button:has-text('엔케어'):visible";

    private final Acts29Properties properties;
    private final ObjectMapper objectMapper;

    @Override
    public Session open(LocalDate date, String grade, String className) {
        validateConfiguration();

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        try {
            playwright = Playwright.create();
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(properties.headless());
            if (StringUtils.hasText(properties.browserChannel())) {
                launchOptions.setChannel(properties.browserChannel().trim());
            }
            browser = playwright.chromium().launch(launchOptions);
            context = browser.newContext(new Browser.NewContextOptions()
                    .setLocale("ko-KR")
                    .setTimezoneId("Asia/Seoul"));
            Page page = context.newPage();
            double timeoutMillis = timeout().toMillis();
            page.setDefaultTimeout(timeoutMillis);
            page.setDefaultNavigationTimeout(timeoutMillis);

            String targetUrl = targetUrl(date, grade, className);
            authenticate(context, page, targetUrl);
            return new PlaywrightSession(
                    playwright,
                    browser,
                    context,
                    context.pages().get(context.pages().size() - 1),
                    date,
                    grade,
                    className
            );
        } catch (RuntimeException exception) {
            closeQuietly(context, browser, playwright);
            if (exception instanceof ApiException apiException) {
                throw apiException;
            }
            log.warn("Acts29 browser session initialization failed: {}", exception.getClass().getSimpleName());
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "ACTS29_LOGIN_FAILED",
                    "Acts29 로그인 또는 엔케어 접속에 실패했습니다."
            );
        }
    }

    private void authenticate(BrowserContext context, Page initialPage, String targetUrl) {
        initialPage.navigate(targetUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        initialPage.waitForTimeout(3_000);
        if (isAttendancePage(initialPage)) {
            return;
        }

        FoundLocator emailLogin = findVisible(context, EMAIL_LOGIN_BUTTON);
        if (emailLogin == null) {
            throw authenticationFailure("이메일 로그인 버튼을 찾지 못했습니다.");
        }
        emailLogin.locator().click();
        emailLogin.page().waitForTimeout(1_500);

        FoundLocator username = findVisible(context, USERNAME_INPUT);
        FoundLocator password = findVisible(context, PASSWORD_INPUT);
        if (username == null || password == null) {
            throw authenticationFailure("통합인증 계정 입력 필드를 찾지 못했습니다.");
        }
        username.locator().fill(properties.userId());
        password.locator().fill(properties.password());
        if (!clickFirst(password.frame(), List.of(
                "button:has-text('로그인'):visible",
                "button[type='submit']:visible",
                "input[type='submit']:visible"
        ))) {
            password.locator().press("Enter");
        }

        password.page().waitForTimeout(6_000);
        FoundLocator ncare = findVisible(context, NCARE_BUTTON);
        if (ncare == null) {
            throw authenticationFailure("엔케어 서비스 버튼을 찾지 못했습니다.");
        }
        ncare.locator().click();
        ncare.page().waitForTimeout(7_000);

        Page page = context.pages().get(context.pages().size() - 1);
        page.navigate(targetUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.waitForTimeout(2_000);
        if (!isAttendancePage(page)) {
            throw authenticationFailure("출석 페이지 인증에 실패했습니다.");
        }
    }

    private FoundLocator findVisible(BrowserContext context, String selector) {
        for (Page page : context.pages()) {
            for (Frame frame : page.frames()) {
                Locator locator = frame.locator(selector);
                if (locator.count() > 0 && locator.first().isVisible()) {
                    return new FoundLocator(page, frame, locator.first());
                }
            }
        }
        return null;
    }

    private boolean clickFirst(Frame frame, List<String> selectors) {
        for (String selector : selectors) {
            Locator locator = frame.locator(selector);
            if (locator.count() > 0 && locator.first().isVisible()) {
                locator.first().click();
                return true;
            }
        }
        return false;
    }

    private boolean isAttendancePage(Page page) {
        URI uri = URI.create(page.url());
        String expectedHost = URI.create(properties.baseUrl()).getHost();
        return expectedHost != null
                && expectedHost.equalsIgnoreCase(uri.getHost())
                && uri.getPath() != null
                && uri.getPath().endsWith("/child/choolsuk.do");
    }

    private String targetUrl(LocalDate date, String grade, String className) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path("/child/choolsuk.do")
                .queryParam("act", "choolsukUpdate")
                .queryParam("dlb", grade)
                .queryParam("soon", className)
                .queryParam("bogoday", date)
                .build()
                .encode()
                .toUriString();
    }

    private Duration timeout() {
        return properties.timeout() == null ? Duration.ofSeconds(60) : properties.timeout();
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.userId()) || !StringUtils.hasText(properties.password())) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "ACTS29_NOT_CONFIGURED",
                    "ACTS29_USER_ID와 ACTS29_PASSWORD 환경변수가 필요합니다."
            );
        }
        if (!StringUtils.hasText(properties.baseUrl())) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "ACTS29_NOT_CONFIGURED",
                    "Acts29 기본 URL이 설정되지 않았습니다."
            );
        }
    }

    private ApiException authenticationFailure(String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, "ACTS29_LOGIN_FAILED", message);
    }

    private void closeQuietly(BrowserContext context, Browser browser, Playwright playwright) {
        try {
            if (context != null) {
                context.close();
            }
        } catch (RuntimeException ignored) {
        }
        try {
            if (browser != null) {
                browser.close();
            }
        } catch (RuntimeException ignored) {
        }
        try {
            if (playwright != null) {
                playwright.close();
            }
        } catch (RuntimeException ignored) {
        }
    }

    private record FoundLocator(Page page, Frame frame, Locator locator) {
    }

    private final class PlaywrightSession implements Session {
        private final Playwright playwright;
        private final Browser browser;
        private final BrowserContext context;
        private final Page page;
        private final LocalDate date;
        private final String grade;
        private final String className;
        private boolean closed;

        private PlaywrightSession(
                Playwright playwright,
                Browser browser,
                BrowserContext context,
                Page page,
                LocalDate date,
                String grade,
                String className
        ) {
            this.playwright = playwright;
            this.browser = browser;
            this.context = context;
            this.page = page;
            this.date = date;
            this.grade = grade;
            this.className = className;
        }

        @Override
        public List<Acts29StudentRecord> fetchAttendance() {
            ensureOpen();
            FormData form = FormData.create()
                    .set("dlb", grade)
                    .set("soon", className)
                    .set("bogoday", date.toString());

            APIResponse response = null;
            try {
                response = page.request().post(
                        properties.baseUrl() + "/child/choolsuk.do?act=choolsukUpdateList",
                        RequestOptions.create()
                                .setForm(form)
                                .setHeader("X-Requested-With", "XMLHttpRequest")
                                .setTimeout(timeout().toMillis())
                );
                if (!response.ok()) {
                    throw remoteFailure("목록 조회", response.status());
                }
                JsonNode list = objectMapper.readTree(response.text()).path("list");
                if (!list.isArray()) {
                    throw new ApiException(
                            HttpStatus.BAD_GATEWAY,
                            "ACTS29_INVALID_RESPONSE",
                            "Acts29 목록 응답에 list 배열이 없습니다."
                    );
                }
                List<Acts29StudentRecord> records = new ArrayList<>();
                int index = 0;
                for (JsonNode item : list) {
                    if (!(item instanceof ObjectNode objectNode)) {
                        throw new ApiException(
                                HttpStatus.BAD_GATEWAY,
                                "ACTS29_INVALID_RESPONSE",
                                "Acts29 학생 목록 형식이 올바르지 않습니다."
                        );
                    }
                    records.add(Acts29StudentRecord.from(index++, objectNode));
                }
                return records;
            } catch (ApiException exception) {
                throw exception;
            } catch (Exception exception) {
                log.warn("Acts29 roster request failed: {}", exception.getClass().getSimpleName());
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "ACTS29_REQUEST_FAILED",
                        "Acts29 목록 조회 또는 응답 해석에 실패했습니다."
                );
            } finally {
                if (response != null) {
                    response.dispose();
                }
            }
        }

        @Override
        public void saveAttendance(List<Acts29StudentRecord> records) {
            ensureOpen();
            ArrayNode listPayload = objectMapper.createArrayNode();
            List<String> reasons = new ArrayList<>(records.size());
            for (Acts29StudentRecord record : records) {
                listPayload.add(record.payloadCopy());
                reasons.add(record.absenceReason());
            }
            if (reasons.stream().anyMatch(reason -> reason.contains(":"))) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "ACTS29_INVALID_ABSENCE_REASON",
                        "결석 사유에 구분 문자 ':'가 포함되어 안전하게 저장할 수 없습니다."
                );
            }

            FormData form = FormData.create()
                    .set("list", listPayload.toString())
                    .set("sau_arr", String.join(":", reasons))
                    .set("bogoday", date.toString())
                    .set("dlb", grade)
                    .set("soon", className);

            APIResponse response = null;
            try {
                response = page.request().post(
                        properties.baseUrl() + "/child/choolsuk.do?act=choolsuk_update",
                        RequestOptions.create()
                                .setForm(form)
                                .setHeader("X-Requested-With", "XMLHttpRequest")
                                .setTimeout(timeout().toMillis())
                );
                if (!response.ok()) {
                    throw remoteFailure("저장", response.status());
                }
            } catch (ApiException exception) {
                throw exception;
            } catch (Exception exception) {
                log.warn("Acts29 save request failed: {}", exception.getClass().getSimpleName());
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "ACTS29_REQUEST_FAILED",
                        "Acts29 저장 요청에 실패했습니다."
                );
            } finally {
                if (response != null) {
                    response.dispose();
                }
            }
        }

        private void ensureOpen() {
            if (closed) {
                throw new IllegalStateException("Acts29 session is already closed");
            }
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            closeQuietly(context, browser, playwright);
        }

        private ApiException remoteFailure(String operation, int status) {
            return new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "ACTS29_REQUEST_FAILED",
                    "Acts29 " + operation + " 요청에 실패했습니다. (HTTP " + status + ")"
            );
        }
    }
}
