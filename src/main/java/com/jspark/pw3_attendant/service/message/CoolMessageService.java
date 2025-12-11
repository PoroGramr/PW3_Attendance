package com.jspark.pw3_attendant.service.message; // Corrected package

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.message_log.MessageLog;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.model.StorageType;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.service.DefaultMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Primary
public class CoolMessageService implements MessageService { // Implement MessageService

    @Value("${coolsms.api.key}")
    private String apiKey;

    @Value("${coolsms.api.secret}")
    private String apiSecret;

    @Value("${coolsms.api.number}")
    private String fromPhoneNumber;

    private DefaultMessageService messageService;

    @PostConstruct
    private void initialize() {
        this.messageService = NurigoApp.INSTANCE.initialize(apiKey, apiSecret, "https://api.solapi.com");
    }

    @Override
    public boolean sendMessage(Student student, String content, String imageUrl) {
        try {
            if (imageUrl != null && !imageUrl.isBlank()) {
                sendSmsWithImg(student.getPhone(), content, imageUrl);
            } else {
                sendSms(student.getPhone(), content);
            }
            return true;
        } catch (IOException e) {
            log.error("Failed to send message via CoolMessageService to student {}: {}", student.getId(), e.getMessage());
            throw new RuntimeException("메시지 발송 중 이미지 처리 오류 발생", e);
        } catch (Exception e) {
            log.error("Failed to send message via CoolMessageService to student {}: {}", student.getId(), e.getMessage());
            return false;
        }
    }


    @Async("smsExecutor")
    public void sendSms(String to,String messageTxt) {
        Message message = new Message();
        message.setFrom(this.fromPhoneNumber);
        message.setTo(to);
        message.setText(messageTxt);

        SingleMessageSendingRequest request = new SingleMessageSendingRequest(message);

        this.messageService.sendOne(request);
        log.info("CoolMessageService: SMS sent to {} with content: {}", to, messageTxt);
    }

    @Async("smsExecutor")
    public void sendSmsWithImg(String to,String messageTxt, String imgLink) throws IOException {
        // TODO: As discussed, ClassPathResource expects classpath resource.
        // If imgLink is an external URL, this logic needs to be changed to download the image first.
        // For now, it assumes imgLink is a classpath resource path.
        File file = new ClassPathResource(imgLink).getFile();
        String imageId = messageService.uploadFile(file, StorageType.MMS, null);

        Message message = new Message();
        message.setFrom(this.fromPhoneNumber);
        message.setTo(to);
        message.setText(messageTxt);
        message.setImageId(imageId);

        SingleMessageSendingRequest request = new SingleMessageSendingRequest(message);

        this.messageService.sendOne(request);
        log.info("CoolMessageService: MMS sent to {} with content: {} and image: {}", to, messageTxt, imgLink);
    }

    @Override
    public MessageLog.MessageChannel getMessageChannel() {
        return MessageLog.MessageChannel.SMS;
    }
}
