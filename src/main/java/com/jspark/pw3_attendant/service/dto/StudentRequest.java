package com.jspark.pw3_attendant.service.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudentRequest {
    private String name;
    private LocalDate birth;
    private String phone;
}
