package com.jspark.pw3_attendant.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jspark.pw3_attendant.service.Attendance.AttendanceAnalysisService;
import com.jspark.pw3_attendant.service.Attendance.dto.*;
import com.jspark.pw3_attendant.service.ai.dto.IntentDetectionResponse;
import com.jspark.pw3_attendant.service.ai.dto.QueryIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final AttendanceAnalysisService attendanceAnalysisService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    /**
     * 사용자 질문에 대한 AI 답변 생성 (RAG 패턴)
     */
    public String chatWithAgent(String userQuestion) {
        try {
            // 1. 질문 의도 파악 (Retrieval - Intent Detection)
            IntentDetectionResponse intentResponse = detectIntent(userQuestion);
            QueryIntent intent = QueryIntent.valueOf(intentResponse.getIntent());
            Map<String, Object> params = intentResponse.getParams();

            log.info("Detected intent: {}, params: {}", intent, params);

            // 2. 데이터 검색 (Retrieval - Data Fetching)
            Object data = retrieveData(intent, params);
            log.info("[DEBUG] Retrieved data - Type: {}, IsNull: {}",
                    data != null ? data.getClass().getSimpleName() : "null",
                    data == null);

            // 3. 답변 생성 (Generation)
            return generateAnswer(userQuestion, data, intent);

        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return "죄송합니다. 질문을 처리하는 중 오류가 발생했습니다. 다시 시도해주세요.";
        }
    }

    public String generateMonthlyAttendanceMarkdown(MonthlyClassAttendanceReportResponse reportData) {
        try {
            String dataJson = objectMapper.writeValueAsString(reportData);
            String prompt = """
                    당신은 교회 출석부 관리자를 위한 월별 출석 리포트를 작성하는 조교입니다.
                    아래 JSON 데이터만 근거로 마크다운 리포트를 작성하세요.
                    숫자는 제공된 값을 그대로 사용하고, 없는 데이터는 추측하지 마세요.

                    리포트 목적:
                    - 관리자가 월별 반 출석률을 빠르게 파악
                    - 출석률이 미비한 반과 전월 대비 하락한 반을 명확히 표시
                    - 어떤 반을 우선 확인해야 하는지 판단 가능하게 작성

                    출력 형식:
                    # {year}년 {month}월 출석 리포트
                    ## 전체 요약
                    ## 출석률 상위 반
                    ## 관리 필요 반
                    ## 반별 출석률 표
                    ## 관리자 확인 포인트

                    작성 규칙:
                    - 반드시 마크다운만 출력하세요.
                    - 표를 적극적으로 사용하세요.
                    - 관리 필요 반은 낮은 출석률 순으로 정리하세요.
                    - status 값은 GOOD=우수, NORMAL=보통, WEAK=미비, DROPPED=하락으로 표현하세요.
                    - 과장하지 말고 실무적으로 간결하게 작성하세요.

                    JSON 데이터:
                    {data}
                    """
                    .replace("{year}", String.valueOf(reportData.getYear()))
                    .replace("{month}", String.valueOf(reportData.getMonth()))
                    .replace("{data}", dataJson);

            String markdown = callGeminiApi(prompt);
            if (markdown != null && !markdown.isBlank()) {
                return markdown.trim();
            }
        } catch (Exception e) {
            log.warn("Failed to generate monthly attendance markdown with Gemini: {}", e.getMessage());
        }

        return buildFallbackMonthlyAttendanceMarkdown(reportData);
    }

    /**
     * Gemini API 직접 호출
     */
    private String callGeminiApi(String prompt) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                    + apiKey;

            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, String>> parts = new ArrayList<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", prompt);
            parts.add(part);
            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map<String, Object> candidate = candidates.get(0);
                    Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
                    List<Map<String, String>> partsList = (List<Map<String, String>>) contentMap.get("parts");
                    if (partsList != null && !partsList.isEmpty()) {
                        return partsList.get(0).get("text");
                    }
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            return null;
        }
    }

    /**
     * LLM을 사용하여 질문 의도 파악
     */
    private IntentDetectionResponse detectIntent(String question) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();

        String intentPrompt = String.format("""
                당신은 출석 관리 시스템의 질문 분석 전문가입니다.
                사용자의 질문을 분석하여 의도와 파라미터를 JSON 형식으로 반환하세요.

                **중요: 오늘 날짜는 %s이고, 현재 연도는 %d년, 현재 월은 %d월입니다.**

                지원하는 의도:
                - LIST_FULL_ABSENCE_STUDENTS: 특정 기간 한 번도 출석하지 않은 학생 ("이번 달 한 번도 안 나온", "장기 결석자")
                - FIND_CONSECUTIVE_ABSENCE_STUDENTS: N주 연속 결석 ("3주 연속 결석", "계속 안 나온")
                - FIND_FREQUENT_LATE_STUDENTS: 지각 빈도 높은 학생 ("지각이 잦은", "지각 많은")
                - GET_AVERAGE_ATTENDANCE_RATE_BY_GRADE: 학년별 평균 출석률 ("학년별 출석률", "학년별 평균")
                - FIND_STUDENTS_NEEDING_CARE: 관리가 필요한 학생 ("관리가 필요한", "위험 학생", "문제 학생")
                - FIND_NEW_CONSECUTIVE_ATTENDEES: 신입생 정착 현황 ("신입인데 연속 출석", "신입생 정착")

                파라미터 추출 규칙:
                - "이번 달": startDate는 %s, endDate는 %s
                - "지난 달": 이전 달의 1일과 마지막 날
                - "N주": weeks 파라미터로 추출
                - "N명": topN 파라미터로 추출
                - 학년도는 현재 연도 %d로 설정

                사용자 질문: %s

                응답 형식 (반드시 JSON만 반환):
                {
                  "intent": "의도명",
                  "params": {
                    "weeks": 3,
                    "startDate": "%s",
                    "endDate": "%s",
                    "topN": 10,
                    "schoolYear": %d
                  }
                }
                """,
                today,
                today.getYear(),
                today.getMonthValue(),
                currentMonth.atDay(1),
                currentMonth.atEndOfMonth(),
                today.getYear(),
                question,
                currentMonth.atDay(1),
                currentMonth.atEndOfMonth(),
                today.getYear());

        try {
            String response = callGeminiApi(intentPrompt);

            if (response != null) {
                log.info("Intent detection response: {}", response);

                // JSON 추출 (```json 태그 제거)
                String jsonResponse = response.trim();
                if (jsonResponse.startsWith("```json")) {
                    jsonResponse = jsonResponse.substring(7);
                }
                if (jsonResponse.endsWith("```")) {
                    jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
                }
                jsonResponse = jsonResponse.trim();

                return objectMapper.readValue(jsonResponse, IntentDetectionResponse.class);
            }

            return new IntentDetectionResponse("UNKNOWN", new HashMap<>());

        } catch (Exception e) {
            log.error("Error detecting intent", e);
            return new IntentDetectionResponse("UNKNOWN", new HashMap<>());
        }
    }

    /**
     * 의도에 따라 데이터 검색
     */
    private Object retrieveData(QueryIntent intent, Map<String, Object> params) {
        // 기본값: 이번 달
        YearMonth currentMonth = YearMonth.now();
        LocalDate defaultStartDate = currentMonth.atDay(1);
        LocalDate defaultEndDate = currentMonth.atEndOfMonth();

        LocalDate startDate = parseDate(params.get("startDate"), defaultStartDate);
        LocalDate endDate = parseDate(params.get("endDate"), defaultEndDate);
        Integer schoolYear = parseInteger(params.get("schoolYear"), LocalDate.now().getYear());
        Integer weeks = parseInteger(params.get("weeks"), 3);
        Integer topN = parseInteger(params.get("topN"), 10);

        log.info("Retrieving data with: startDate={}, endDate={}, schoolYear={}, weeks={}, topN={}",
                startDate, endDate, schoolYear, weeks, topN);

        return switch (intent) {
            case LIST_FULL_ABSENCE_STUDENTS ->
                attendanceAnalysisService.findLongTermAbsentees(startDate, endDate);

            case FIND_CONSECUTIVE_ABSENCE_STUDENTS ->
                attendanceAnalysisService.findConsecutiveAbsenceStudents(weeks);

            case FIND_NEW_CONSECUTIVE_ATTENDEES ->
                attendanceAnalysisService.findNewConsecutiveAttendees(weeks, 3);

            case GET_AVERAGE_ATTENDANCE_RATE_BY_GRADE ->
                attendanceAnalysisService.getAverageAttendanceRateByGrade(startDate, endDate, schoolYear);

            case FIND_STUDENTS_NEEDING_CARE ->
                attendanceAnalysisService.findStudentsNeedingCare(startDate, endDate, schoolYear);

            case FIND_FREQUENT_LATE_STUDENTS ->
                attendanceAnalysisService.findFrequentLateStudents(topN, startDate, endDate);

            default -> "데이터를 찾을 수 없습니다.";
        };
    }

    /**
     * LLM을 사용하여 자연어 답변 생성
     */
    private String generateAnswer(String question, Object data, QueryIntent intent) {
        String dataString = formatData(data, intent);

        log.info("[DEBUG] Formatted data string - Length: {}, Preview: {}",
                dataString.length(),
                dataString.length() > 200 ? dataString.substring(0, 200) + "..." : dataString);

        String answerPrompt = """
                당신은 친절한 출석 관리 조교입니다.
                아래 제공된 데이터를 **반드시** 사용하여 사용자의 질문에 답변하세요.
                제공되는 데이터는 해당 질문에 대한 쿼리 조회 결과 데이터입니다.
                해당 응답 데이터를 바탕으로 답변해주세요.

                **중요: 아래 데이터는 이미 조회된 실제 결과입니다. "데이터가 없다"거나 "파악할 수 없다"는 답변은 절대 하지 마세요.**

                # 조회된 데이터:
                {data}

                # 사용자 질문:
                {question}

                # 답변 예시:
                만약 데이터가 "총 3명:\\n1. 김철수 (중 1-1)\\n2. 이영희 (중 2-2)\\n3. 박민수 (고 1-3)"라면,

                답변은 다음과 같이 해야 합니다:
                "신입생 중 3주 연속 출석한 학생은 총 3명입니다:

                1. 김철수 (중 1-1)
                2. 이영희 (중 2-2)
                3. 박민수 (고 1-3)

                이 학생들은 꾸준히 출석하고 있어 정착이 잘 되고 있습니다!"

                # 답변 규칙:
                1. 위 데이터에 학생 목록이 있다면, 그대로 사용하여 답변하세요
                2. 학생 이름과 반 정보를 모두 포함하세요
                3. 존댓말을 사용하세요
                4. "총 N명"과 같이 구체적인 숫자를 언급하세요
                5. 데이터에 "해당하는 학생이 없습니다"라고 명시된 경우에만 그렇게 답변하세요
                6. 간결하고 명확하게 답변하세요

                **다시 한 번 강조: 위 데이터는 실제 조회 결과이므로, 반드시 이 데이터를 기반으로 답변하세요.**
                """;

        try {
            String finalPrompt = answerPrompt
                    .replace("{data}", dataString)
                    .replace("{question}", question);

            log.info("[DEBUG] Final prompt sent to Gemini - Length: {}, Preview: {}",
                    finalPrompt.length(),
                    finalPrompt.length() > 500 ? finalPrompt.substring(0, 500) + "..." : finalPrompt);

            String response = callGeminiApi(finalPrompt);

            log.info("[DEBUG] Gemini API response - IsNull: {}, Length: {}",
                    response == null,
                    response != null ? response.length() : 0);

            if (response != null) {
                log.info("[DEBUG] Using Gemini response");
                return response;
            } else {
                log.warn("[DEBUG] Gemini response is null, using fallback");
                return generateFallbackAnswer(dataString, intent);
            }

        } catch (Exception e) {
            log.warn("Gemini API failed, using fallback response: {}", e.getMessage());
            return generateFallbackAnswer(dataString, intent);
        }
    }

    /**
     * API 실패 시 템플릿 기반 답변 생성
     */
    private String generateFallbackAnswer(String dataString, QueryIntent intent) {
        String prefix = switch (intent) {
            case LIST_FULL_ABSENCE_STUDENTS ->
                "📋 **이번 달 한 번도 출석하지 않은 학생 목록**\n\n";
            case FIND_CONSECUTIVE_ABSENCE_STUDENTS ->
                "📋 **연속으로 결석한 학생 목록**\n\n";
            case FIND_FREQUENT_LATE_STUDENTS ->
                "📋 **지각이 잦은 학생 목록**\n\n";
            case GET_AVERAGE_ATTENDANCE_RATE_BY_GRADE ->
                "📊 **학년별 평균 출석률**\n\n";
            case FIND_STUDENTS_NEEDING_CARE ->
                "⚠️ **관리가 필요한 학생 목록**\n\n";
            case FIND_NEW_CONSECUTIVE_ATTENDEES ->
                "✅ **신입생 중 연속 출석한 학생 목록**\n\n";
            default ->
                "📋 **조회 결과**\n\n";
        };

        return prefix + dataString + "\n\n💡 더 궁금한 사항이 있으시면 언제든 질문해주세요!";

    }

    /**
     * 데이터를 문자열로 포맷팅
     */
    private String formatData(Object data, QueryIntent intent) {
        log.info("[DEBUG] formatData - Intent: {}, Data null: {}, Data type: {}",
                intent,
                data == null,
                data != null ? data.getClass().getName() : "null");

        if (data == null) {
            return "데이터 없음";
        }

        try {
            return switch (intent) {
                case LIST_FULL_ABSENCE_STUDENTS, FIND_CONSECUTIVE_ABSENCE_STUDENTS -> {
                    @SuppressWarnings("unchecked")
                    List<AbsenteeResponse> students = (List<AbsenteeResponse>) data;

                    log.info("[DEBUG] After cast - List size: {}, isEmpty: {}", students.size(), students.isEmpty());
                    if (!students.isEmpty()) {
                        log.info("[DEBUG] First element: {}", students.get(0));
                    }

                    if (students.isEmpty()) {
                        yield "해당하는 학생이 없습니다.";
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("총 ").append(students.size()).append("명:\n");
                    log.info("[DEBUG] Building response string for {} students", students.size());
                    for (int i = 0; i < students.size(); i++) {
                        AbsenteeResponse student = students.get(i);
                        sb.append((i + 1)).append(". ")
                                .append(student.getStudentName())
                                .append(" (").append(student.getClassName()).append(")\n");
                    }
                    yield sb.toString();
                }

                case FIND_NEW_CONSECUTIVE_ATTENDEES -> {
                    @SuppressWarnings("unchecked")
                    List<NewStudentAttendeeResponse> students = (List<NewStudentAttendeeResponse>) data;

                    if (students.isEmpty()) {
                        yield "해당하는 학생이 없습니다.";
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("총 ").append(students.size()).append("명:\n");
                    for (int i = 0; i < students.size(); i++) {
                        NewStudentAttendeeResponse student = students.get(i);
                        sb.append((i + 1)).append(". ")
                                .append(student.getStudentName())
                                .append(" (").append(student.getClassName()).append(")\n");
                    }
                    yield sb.toString();
                }

                case GET_AVERAGE_ATTENDANCE_RATE_BY_GRADE -> {
                    @SuppressWarnings("unchecked")
                    List<GradeAttendanceRateDto> rates = (List<GradeAttendanceRateDto>) data;
                    if (rates.isEmpty()) {
                        yield "출석률 데이터가 없습니다.";
                    }
                    StringBuilder sb = new StringBuilder();
                    for (GradeAttendanceRateDto rate : rates) {
                        sb.append("- ").append(rate.getGradeName())
                                .append(": ").append(rate.getAttendanceRate()).append("%")
                                .append(" (").append(rate.getAttendedCount())
                                .append("/").append(rate.getTotalStudents()).append("명)\n");
                    }
                    yield sb.toString();
                }

                case FIND_STUDENTS_NEEDING_CARE -> {
                    @SuppressWarnings("unchecked")
                    List<StudentNeedsCareDto> students = (List<StudentNeedsCareDto>) data;
                    if (students.isEmpty()) {
                        yield "관리가 필요한 학생이 없습니다.";
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("총 ").append(students.size()).append("명:\n");
                    for (int i = 0; i < students.size(); i++) {
                        StudentNeedsCareDto student = students.get(i);
                        sb.append((i + 1)).append(". ")
                                .append(student.getStudentName())
                                .append(" (").append(student.getClassName()).append(")")
                                .append(" - 결석: ").append(student.getAbsenceCount()).append("회")
                                .append(", 지각: ").append(student.getLateCount()).append("회\n");
                    }
                    yield sb.toString();
                }

                case FIND_FREQUENT_LATE_STUDENTS -> {
                    @SuppressWarnings("unchecked")
                    List<StudentLatenessDto> students = (List<StudentLatenessDto>) data;
                    if (students.isEmpty()) {
                        yield "지각 기록이 없습니다.";
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("총 ").append(students.size()).append("명:\n");
                    for (int i = 0; i < students.size(); i++) {
                        StudentLatenessDto student = students.get(i);
                        sb.append((i + 1)).append(". ")
                                .append(student.getStudentName())
                                .append(" (").append(student.getClassName()).append(")")
                                .append(" - 지각: ").append(student.getLateCount()).append("회\n");
                    }
                    yield sb.toString();
                }

                default -> data.toString();
            };
        } catch (Exception e) {
            log.error("Error formatting data", e);
            return data.toString();
        }
    }

    private String buildFallbackMonthlyAttendanceMarkdown(MonthlyClassAttendanceReportResponse data) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(data.getYear()).append("년 ").append(data.getMonth()).append("월 출석 리포트\n\n");

        sb.append("## 전체 요약\n\n");
        sb.append("| 항목 | 값 |\n");
        sb.append("| --- | ---: |\n");
        sb.append("| 학년도 | ").append(data.getSchoolYear()).append(" |\n");
        sb.append("| 집계 주일 수 | ").append(data.getTotalSundays()).append("회 |\n");
        sb.append("| 전체 반 수 | ").append(data.getTotalClasses()).append("개 |\n");
        sb.append("| 전체 평균 출석률 | ").append(data.getAverageAttendanceRate()).append("% |\n");
        sb.append("| 관리 필요 반 | ").append(data.getWeakClassCount()).append("개 |\n");
        sb.append("| 미비 기준 | ").append(data.getWeakClassThreshold()).append("% 미만 |\n\n");

        sb.append("## 출석률 상위 반\n\n");
        if (data.getTopClasses().isEmpty()) {
            sb.append("집계된 반 데이터가 없습니다.\n\n");
        } else {
            sb.append("| 순위 | 반 | 담당 | 출석률 | 평균 출석 인원 |\n");
            sb.append("| ---: | --- | --- | ---: | ---: |\n");
            for (MonthlyClassAttendanceDto item : data.getTopClasses()) {
                sb.append("| ").append(item.getRank())
                        .append(" | ").append(item.getClassName())
                        .append(" | ").append(item.getTeacherName())
                        .append(" | ").append(item.getAttendanceRate()).append("%")
                        .append(" | ").append(item.getAverageAttendedCount()).append("명 |\n");
            }
            sb.append("\n");
        }

        sb.append("## 관리 필요 반\n\n");
        if (data.getWeakClasses().isEmpty()) {
            sb.append("기준치 미만이거나 전월 대비 크게 하락한 반은 없습니다.\n\n");
        } else {
            sb.append("| 우선순위 | 반 | 담당 | 출석률 | 전월 대비 | 사유 |\n");
            sb.append("| ---: | --- | --- | ---: | ---: | --- |\n");
            for (int i = 0; i < data.getWeakClasses().size(); i++) {
                WeakClassDto item = data.getWeakClasses().get(i);
                sb.append("| ").append(i + 1)
                        .append(" | ").append(item.getClassName())
                        .append(" | ").append(item.getTeacherName())
                        .append(" | ").append(item.getAttendanceRate()).append("%")
                        .append(" | ").append(formatChange(item.getMonthOverMonthChange()))
                        .append(" | ").append(item.getReason()).append(" |\n");
            }
            sb.append("\n");
        }

        sb.append("## 반별 출석률 표\n\n");
        sb.append("| 순위 | 상태 | 반 | 담당 | 재적 | 평균 출석 | 출석률 | 전월 대비 |\n");
        sb.append("| ---: | --- | --- | --- | ---: | ---: | ---: | ---: |\n");
        for (MonthlyClassAttendanceDto item : data.getClasses()) {
            sb.append("| ").append(item.getRank())
                    .append(" | ").append(formatStatus(item.getStatus()))
                    .append(" | ").append(item.getClassName())
                    .append(" | ").append(item.getTeacherName())
                    .append(" | ").append(item.getTotalStudents()).append("명")
                    .append(" | ").append(item.getAverageAttendedCount()).append("명")
                    .append(" | ").append(item.getAttendanceRate()).append("%")
                    .append(" | ").append(formatChange(item.getMonthOverMonthChange())).append(" |\n");
        }
        sb.append("\n");

        sb.append("## 관리자 확인 포인트\n\n");
        if (data.getWeakClasses().isEmpty()) {
            sb.append("- 이번 달은 출석률 미비 기준에 해당하는 반이 없습니다.\n");
            sb.append("- 하위권 반의 주차별 변동을 확인해 다음 달 관리 기준을 조정해보세요.\n");
        } else {
            sb.append("- 관리 필요 반은 담당 교사와 결석 사유를 먼저 확인하세요.\n");
            sb.append("- 전월 대비 하락한 반은 특정 주일의 출석 급락 여부를 함께 확인하세요.\n");
            sb.append("- 기준치 미만 반은 다음 달에도 동일 기준으로 추적하는 것이 좋습니다.\n");
        }

        return sb.toString();
    }

    private String formatStatus(String status) {
        return switch (status) {
            case "GOOD" -> "우수";
            case "WEAK" -> "미비";
            case "DROPPED" -> "하락";
            default -> "보통";
        };
    }

    private String formatChange(Double value) {
        if (value == null) {
            return "-";
        }
        if (value > 0) {
            return "+" + value + "%p";
        }
        return value + "%p";
    }

    /**
     * 파라미터에서 날짜 파싱
     */
    private LocalDate parseDate(Object dateObj, LocalDate defaultValue) {
        if (dateObj == null) {
            return defaultValue;
        }
        if (dateObj instanceof String) {
            try {
                return LocalDate.parse((String) dateObj);
            } catch (Exception e) {
                log.warn("Failed to parse date: {}, using default", dateObj);
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 파라미터에서 정수 파싱
     */
    private Integer parseInteger(Object intObj, Integer defaultValue) {
        if (intObj == null) {
            return defaultValue;
        }
        if (intObj instanceof Integer) {
            return (Integer) intObj;
        }
        if (intObj instanceof String) {
            try {
                return Integer.parseInt((String) intObj);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
