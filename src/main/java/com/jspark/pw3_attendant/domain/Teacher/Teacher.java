package com.jspark.pw3_attendant.domain.Teacher;

import com.jspark.pw3_attendant.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "teacher")
@SQLDelete(sql = "UPDATE teacher SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
public class Teacher extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDate birth;

    private Sex sex;

    private String phone;

    private TeacherType teacherType;

    private String memo;

    @Column
    private LocalDateTime deletedAt;

    public void setName(String name) {
        this.name = name;
    }

    public void setBirth(LocalDate birth) {
        this.birth = birth;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setSex(Sex sex) { this.sex = sex;}

    public void setTeacherType(TeacherType teacherType) { this.teacherType = teacherType;}

    public void setMemo(String memo){ this.memo = memo;}

    public enum Sex {
        MAN, WOMAN
    }

    public enum TeacherType {
        PASTOR, TEACHER, HELPER,
    }
    
}
