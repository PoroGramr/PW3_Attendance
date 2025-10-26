package com.jspark.pw3_attendant.domain.inoutday;

import com.jspark.pw3_attendant.domain.Student.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "new_friend")
public class NewFriend {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birth;

    @Column
    private String phone;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    public void update(String name, LocalDate birth, String phone, Student student) {
        this.name = name;
        this.birth = birth;
        this.phone = phone;
        this.student = student;
    }
}
