package com.jspark.pw3_attendant.service.inoutday.dto;

import java.time.LocalDate;
import lombok.Getter;

@Getter
public class NewFriendRequest {
    private String name;
    private LocalDate birth;
    private String phone;
    private Long studentId;
}
