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
        ObjectNode raw
) {
    static Acts29StudentRecord from(int index, ObjectNode raw) {
        return new Acts29StudentRecord(
                index,
                text(raw, "user_id"),
                text(raw, "seq"),
                text(raw, "name"),
                text(raw, "birth"),
                text(raw, "attend_yn"),
                text(raw, "sau"),
                raw.deepCopy()
        );
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
        ObjectNode payload = raw.deepCopy();
        // ACTS29 returns ISO dates but parses these fields as integers on save.
        for (String field : new String[]{"bogoday", "pass_day"}) {
            String value = payload.path(field).asText("");
            if (value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                payload.put(field, value.replace("-", ""));
            }
        }
        return payload;
    }

    private static String text(ObjectNode node, String field) {
        return node.path(field).isMissingNode() || node.path(field).isNull()
                ? ""
                : node.path(field).asText("");
    }
}
