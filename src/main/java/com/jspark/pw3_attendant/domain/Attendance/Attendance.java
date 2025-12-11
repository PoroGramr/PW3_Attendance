package com.jspark.pw3_attendant.domain.Attendance;

import com.jspark.pw3_attendant.domain.BaseEntity;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "attendance",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_attendance_studentclass_date",
            columnNames = {"student_class_id", "date"}
        )
    }
)
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private StudentClass studentClass; //

    @Column(nullable = false)
    private LocalDate date; // 출석한 날짜

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status; // ATTEND, ABSENT, LATE, OTHER

    public enum AttendanceStatus{
        ATTEND,
        LATE,
        ABSENT,
        OTHER,
        UNCHECKED
    }
}
