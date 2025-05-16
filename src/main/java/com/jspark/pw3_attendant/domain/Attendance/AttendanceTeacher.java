package com.jspark.pw3_attendant.domain.Attendance;

import com.jspark.pw3_attendant.domain.BaseEntity;
import com.jspark.pw3_attendant.domain.Teacher.Teacher;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "attendance_teacher",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_attendance_teacher_date",
            columnNames = {"teacher_id", "date"}
        )
    }
)
public class AttendanceTeacher extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    private Teacher teacher;  // 출석 체크 대상인 선생님

    @Column(nullable = false)
    private LocalDate date;   // 출석 체크한 날짜

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status; // ATTEND, ABSENT, LATE, OTHER

    public enum AttendanceStatus {
        ATTEND,
        LATE,
        ABSENT,
        OTHER
    }
}