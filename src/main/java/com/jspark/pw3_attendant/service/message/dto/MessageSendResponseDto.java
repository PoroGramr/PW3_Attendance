package com.jspark.pw3_attendant.service.message.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MessageSendResponseDto {
    private final int total;
    private final int success;
    private final int failed;
}
