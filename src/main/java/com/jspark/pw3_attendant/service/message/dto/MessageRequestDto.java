package com.jspark.pw3_attendant.service.message.dto;

import com.jspark.pw3_attendant.domain.message_log.MessageLog.MessageChannel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageRequestDto {

    private TargetDto target;
    private RecipientType recipientType;
    private MessageChannel channel;
    private ContentDto content;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TargetDto {
        private TargetType type;
        private List<Long> ids;
    }

    @Getter
    @NoArgsConstructor
    @Setter
    public static class ContentDto {
        private ContentType type;
        private String text;
        private String imageUrl; // Optional
    }

    public enum TargetType {
        ALL_STUDENTS,
        SPECIFIC_STUDENTS,
        CLASS_ROOM
    }

    public enum RecipientType {
        STUDENTS,
        PARENTS,
        ALL
    }

    public enum ContentType {
        TEXT,
        TEXT_WITH_IMAGE
    }
}
