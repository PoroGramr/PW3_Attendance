package com.jspark.pw3_attendant.service.integration.acts29;

import com.fasterxml.jackson.databind.node.ObjectNode;

public record Acts29StudentRecord(
        int index,
        String userId,
        String sequence,
        String name,
        String birth,
        String attendance,
        String absenceReason,
        String reasonForSave,
        ObjectNode raw
) {
    // ACTS29 출결 화면의 결석사유 select가 " " 사유를 표시하는 기본값
    static final String UNKNOWN_ABSENCE_REASON = "사유를 알지 못하는 결석";

    static Acts29StudentRecord from(int index, ObjectNode raw) {
        String attendance = text(raw, "attend_yn");
        String absenceReason = text(raw, "sau");
        return new Acts29StudentRecord(
                index,
                text(raw, "user_id"),
                text(raw, "seq"),
                text(raw, "name"),
                text(raw, "birth"),
                attendance,
                absenceReason,
                browserReason(attendance, absenceReason),
                raw.deepCopy()
        );
    }

    /**
     * ACTS29 화면은 목록을 불러올 때 결석사유 select 값을 정하고, 출석 체크를 바꿔도 select 값은 그대로 둔 채
     * 그 값을 sau_arr로 전송한다. 브라우저와 동일한 값을 보내기 위해 조회 시점의 상태로 계산한다.
     */
    private static String browserReason(String attendance, String absenceReason) {
        if ("Y".equalsIgnoreCase(attendance)) {
            return " ";
        }
        return " ".equals(absenceReason) ? UNKNOWN_ABSENCE_REASON : absenceReason;
    }

    Acts29StudentRecord withAttendance(String nextAttendance) {
        ObjectNode changed = raw.deepCopy();
        changed.put("attend_yn", nextAttendance);
        return new Acts29StudentRecord(
                index,
                userId,
                sequence,
                name,
                birth,
                nextAttendance,
                absenceReason,
                reasonForSave,
                changed
        );
    }

    String identityKey() {
        if (!userId.isBlank()) {
            return "user:" + userId;
        }
        if (!sequence.isBlank()) {
            return "seq:" + sequence;
        }
        return "index:" + index;
    }

    ObjectNode payloadCopy() {
        // ACTS29 화면은 조회 응답의 날짜(bogoday, pass_day)를 yyyy-MM-dd 그대로 다시 전송한다.
        ObjectNode payload = raw.deepCopy();
        payload.put("attend_yn", "Y".equalsIgnoreCase(attendance) ? "Y" : "N");
        payload.put("simbang_yn", "Y".equalsIgnoreCase(text(raw, "simbang_yn")) ? "Y" : "N");
        return payload;
    }

    private static String text(ObjectNode node, String field) {
        return node.path(field).isMissingNode() || node.path(field).isNull()
                ? ""
                : node.path(field).asText("");
    }
}
