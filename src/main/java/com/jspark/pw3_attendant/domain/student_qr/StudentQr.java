package com.jspark.pw3_attendant.domain.student_qr;

import com.jspark.pw3_attendant.domain.BaseEntity;
import com.jspark.pw3_attendant.domain.Student.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "student_qr")
public class StudentQr extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    @Column(nullable = false, unique = true, length = 32)
    private String qrSecret;

    public StudentQr(Student student, String qrSecret) {
        this.student = student;
        this.qrSecret = qrSecret;
    }
}
