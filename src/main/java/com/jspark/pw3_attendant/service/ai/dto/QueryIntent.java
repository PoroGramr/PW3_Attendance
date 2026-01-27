package com.jspark.pw3_attendant.service.ai.dto;

public enum QueryIntent {
    LIST_FULL_ABSENCE_STUDENTS, // 장기 결석자 조회
    FIND_CONSECUTIVE_ABSENCE_STUDENTS, // 연속 결석 패턴
    FIND_NEW_CONSECUTIVE_ATTENDEES, // 신입생 정착 현황
    GET_AVERAGE_ATTENDANCE_RATE_BY_GRADE, // 학년별 평균 출석률
    FIND_STUDENTS_NEEDING_CARE, // 관리 필요 학생
    FIND_FREQUENT_LATE_STUDENTS, // 지각 빈도 높은 학생
    UNKNOWN // 알 수 없는 의도
}
