package com.jspark.pw3_attendant.domain.Student;

import com.jspark.pw3_attendant.domain.BaseEntity;
import com.jspark.pw3_attendant.domain.StudentClass.StudentClass;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "student")
@SQLDelete(sql = "UPDATE student SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public void setName(String name) {
        this.name = name;
    }

    public void setBirth(LocalDate birth) {
        this.birth = birth;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setParentPhone(String parentPhone) {
        this.parentPhone = parentPhone;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public void setIsGraduated(Boolean isGraduated) {
        this.isGraduated = isGraduated;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birth;

    private Sex sex;

    @Column
    private String phone;

    private String parentPhone;

    private String school;

    private String memo;

    @Column(nullable = false)
    private Boolean isGraduated = false;

    @Column
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentClass> studentClasses = new ArrayList<>();

    public enum Sex {
        MAN, WOMAN
    }
}
