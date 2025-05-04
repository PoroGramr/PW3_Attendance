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

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "student")
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birth;

    @Column
    private String phone;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentClass> studentClasses = new ArrayList<>();
}
