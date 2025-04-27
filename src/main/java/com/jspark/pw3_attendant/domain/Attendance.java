package com.jspark.pw3_attendant.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "attendance")
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    private StudentClass studentClass; // 🔥 출석은 StudentClass 기준

    @Column(nullable = false)
    private LocalDate date; // 출석한 날짜

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status; // ATTEND, ABSENT, LATE, OTHER
}
