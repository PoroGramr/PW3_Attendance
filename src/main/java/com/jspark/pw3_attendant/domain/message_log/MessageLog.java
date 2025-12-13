package com.jspark.pw3_attendant.domain.message_log;

import com.jspark.pw3_attendant.domain.BaseEntity;
import com.jspark.pw3_attendant.domain.Student.Student;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "message_log")
public class MessageLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageStatus status;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String errorMessage;

    public enum MessageChannel {
        SMS, KAKAO, EMAIL
    }

    public enum MessageStatus {
        SUCCESS, FAIL
    }

    public MessageLog(Student student, MessageChannel channel, MessageStatus status, String content, String errorMessage) {
        this.student = student;
        this.channel = channel;
        this.status = status;
        this.content = content;
        this.errorMessage = errorMessage;
    }
}
