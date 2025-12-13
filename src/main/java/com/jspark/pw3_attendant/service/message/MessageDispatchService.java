package com.jspark.pw3_attendant.service.message;

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.message_log.MessageLog;
import com.jspark.pw3_attendant.repository.message_log.MessageLogRepository;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto;
import com.jspark.pw3_attendant.service.message.dto.MessageRequestDto.ContentType;
import com.jspark.pw3_attendant.service.message.dto.MessageSendResponseDto;
import com.jspark.pw3_attendant.service.message.generator.MessageContentGenerator;
import com.jspark.pw3_attendant.service.message.resolver.TargetResolver;
import com.jspark.pw3_attendant.service.thirdparty.ImgbbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageDispatchService {

    private final Map<MessageRequestDto.TargetType, TargetResolver> targetResolvers;
    private final Map<MessageRequestDto.ContentType, MessageContentGenerator> contentGenerators;
    private final MessageService messageService; // Assuming one channel for now (e.g., LoggingMessageService)
    private final MessageLogRepository messageLogRepository;
    private final CoolMessageService coolMessageService;
    private final ImgbbService imgbbService;

    public MessageDispatchService(
        List<TargetResolver> targetResolvers,
        List<MessageContentGenerator> contentGenerators,
        MessageService messageService,
        MessageLogRepository messageLogRepository,
        CoolMessageService coolMessageService,
        ImgbbService imgbbService
    ) {
        this.targetResolvers = targetResolvers.stream()
            .collect(Collectors.toMap(TargetResolver::getTargetType, Function.identity()));
        this.contentGenerators = contentGenerators.stream()
            .collect(Collectors.toMap(MessageContentGenerator::getContentType, Function.identity()));
        this.messageService = messageService;
        this.messageLogRepository = messageLogRepository;
        this.coolMessageService = coolMessageService;
        this.imgbbService = imgbbService;
    }

    public MessageSendResponseDto dispatchMessage(MessageRequestDto request) {
        // Upload image if present
        MultipartFile imageFile = request.getContent().getImageFile();
        System.out.println("111");
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = imgbbService.uploadImage(imageFile);
            imageUrl = imageUrl.substring(0, imageUrl.length() - 4) + ".jpg";
            request.getContent().setImageUrl(imageUrl);
            // Ensure content type is TEXT_WITH_IMAGE if an image is provided
            request.getContent().setType(ContentType.TEXT_WITH_IMAGE);
        }

        // 1. Resolve targets
        TargetResolver resolver = targetResolvers.get(request.getTarget().getType());
        if (resolver == null) {
            throw new IllegalArgumentException("지원하지 않는 대상 타입입니다: " + request.getTarget().getType());
        }
        List<Student> targets = resolver.resolve(request.getTarget());

        // 2. Get content generator
        MessageContentGenerator generator = contentGenerators.get(request.getContent().getType());
        if (generator == null) {
            throw new IllegalArgumentException("지원하지 않는 콘텐츠 타입입니다: " + request.getContent().getType());
        }

        int successCount = 0;
        int failedCount = 0;

        for (Student student : targets) {
            String messageContent = "";
            try {
                // 3. Generate message content for each student
                messageContent = generator.generate(student, request.getContent());

                if (request.getContent().getType() == ContentType.TEXT){
                    coolMessageService.sendSms(student.getPhone(), messageContent);
                } else if (request.getContent().getType() == ContentType.TEXT_WITH_IMAGE){
                    log.info(request.getContent().getImageUrl());
                    coolMessageService.sendSmsWithImg(student.getPhone(), messageContent, request.getContent().getImageUrl());
                }

                // 4. Log success
                messageLogRepository.save(new MessageLog(student, request.getChannel(), MessageLog.MessageStatus.SUCCESS, messageContent, null));
                successCount++;

            } catch (Exception e) {
                log.error("Failed to dispatch message to student {}: {}", student.getId(), e.getMessage(), e);
                messageLogRepository.save(new MessageLog(student, request.getChannel(), MessageLog.MessageStatus.FAIL, messageContent, e.getMessage()));
                failedCount++;
            }
        }

        return new MessageSendResponseDto(targets.size(), successCount, failedCount);
    }
}
