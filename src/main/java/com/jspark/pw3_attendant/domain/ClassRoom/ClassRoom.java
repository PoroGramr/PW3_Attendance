package com.jspark.pw3_attendant.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "class_room")
public class ClassRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SchoolType schoolType;  // 🔥 중학교/고등학교 구분

    @Column(nullable = false)
    private Integer grade;          // 🔥 몇 학년

    @Column(nullable = false)
    private Integer classNumber;    // 🔥 몇 반

    public String getName() {
        return schoolType.name() + " " + grade + "학년 " + classNumber + "반";
    }
}
