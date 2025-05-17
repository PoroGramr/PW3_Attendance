package com.jspark.pw3_attendant.domain.Teacher;

import com.jspark.pw3_attendant.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "teacher")
public class Teacher extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDate birth;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeacherStatus status;  // 선생님의 상태 (ACTIVE, INACTIVE 등)

    public enum TeacherStatus {
        ACTIVE,    // 재직 중
        INACTIVE   // 퇴직
    }

    public void setBirth(LocalDate birth) {
        this.birth = birth;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(TeacherStatus status) {
        this.status = status;
    }
}
