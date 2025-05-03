package com.jspark.pw3_attendant.domain.ClassRoom;

import com.jspark.pw3_attendant.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
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

    public void setSchoolType(SchoolType schoolType) {
        this.schoolType = schoolType;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public void setClassNumber(Integer classNumber) {
        this.classNumber = classNumber;
    }

    public enum SchoolType {
        MIDDLE,  // 중학교
        HIGH     // 고등학교
    }
}
