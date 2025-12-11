package com.jspark.pw3_attendant.service.qr.dto;

import com.jspark.pw3_attendant.domain.message_log.MessageLog;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SendQrRequestDto {
    private Long courseId; // This corresponds to classRoomId in the current domain model
    private List<MessageLog.MessageChannel> channels;
    private boolean testMode = false;
}
