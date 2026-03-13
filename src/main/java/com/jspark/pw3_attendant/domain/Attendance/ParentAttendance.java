package com.jspark.pw3_attendant.domain.Attendance;

import com.jspark.pw3_attendant.domain.BaseEntity;
import com.jspark.pw3_attendant.domain.Student.Student;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "parent_attendance", uniqueConstraints = {
        @UniqueConstraint(name = "uk_parent_attendance_student_date", columnNames = { "student_id", "date" })
})
public class ParentAttendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private LocalDate date; // 행사 날짜

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParentStatus fatherStatus; // 부 출석 여부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParentStatus motherStatus; // 모 출석 여부

    public enum ParentStatus {
        ATTEND, // 출석
        ABSENT // 미출석
    }
}
