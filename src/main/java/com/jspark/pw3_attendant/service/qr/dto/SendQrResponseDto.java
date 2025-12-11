package com.jspark.pw3_attendant.service.qr.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SendQrResponseDto {
    private int total;
    private int success;
    private int failed;
}
