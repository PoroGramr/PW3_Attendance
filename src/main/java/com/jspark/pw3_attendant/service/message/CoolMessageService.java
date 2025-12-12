package com.jspark.pw3_attendant.service.message; // Corrected package

import com.jspark.pw3_attendant.domain.Student.Student;
import com.jspark.pw3_attendant.domain.message_log.MessageLog;
import jakarta.annotation.PostConstruct;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import javax.imageio.ImageIO;
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

//    @Async("smsExecutor")
//    public void sendSmsWithImg(String to,String messageTxt, String imgLink) throws IOException {
//        // TODO: As discussed, ClassPathResource expects classpath resource.
//        // If imgLink is an external URL, this logic needs to be changed to download the image first.
//        // For now, it assumes imgLink is a classpath resource path.
//        File file = new ClassPathResource(imgLink).getFile();
//        String imageId = messageService.uploadFile(file, StorageType.MMS, null);
//
//        Message message = new Message();
//        message.setFrom(this.fromPhoneNumber);
//        message.setTo(to);
//        message.setText(messageTxt);
//        message.setImageId(imageId);
//
//        SingleMessageSendingRequest request = new SingleMessageSendingRequest(message);
//
//        this.messageService.sendOne(request);
//        log.info("CoolMessageService: MMS sent to {} with content: {} and image: {}", to, messageTxt, imgLink);
//    }

    @Async("smsExecutor")
    public void sendSmsWithImg(String to, String messageTxt, String imgLink) throws IOException {
        Path tempPath = null;
        File tempFile = null; // 초기화하여 finally 블록에서 null-check 없이 사용 가능

        try {
            tempPath = downloadToTempFile(imgLink);
            tempFile = tempPath.toFile(); // tempFile 할당

            String imageId;
            try {
                imageId = messageService.uploadFile(tempFile, StorageType.MMS, null);
                log.info("Nurigo: Successfully uploaded file to get imageId: {}", imageId); // 추가
            } catch (Exception e) {
                log.error("Nurigo: Failed to upload file to Solapi from {}. Error: {}", tempFile.getAbsolutePath(), e.getMessage(), e); // 추가: 스택 트레이스 포함
                throw new IOException("Failed to upload image file to Solapi", e); // 예외 다시 던지기
            }

            Message message = new Message();
            message.setFrom(this.fromPhoneNumber);
            message.setTo(to);
            message.setText(messageTxt);
            message.setImageId(imageId);


            try {
                SingleMessageSendingRequest request = new SingleMessageSendingRequest(message);
                this.messageService.sendOne(request);
                log.info("CoolMessageService: MMS sent to {} with image link: {}", to, imgLink);
            } catch (Exception exception){
                log.error("CoolMessageService: Failed to send MMS to {} with image link {}. Error: {}", to, imgLink, exception.getMessage(), exception); // 로그 상세화: 스택 트레이스 포함
                // 여기서 예외를 다시 던지거나 적절히 처리해야 합니다. 현재는 로깅만 하고 넘어갑니다.
                // 상위 sendMessage 메서드에서 이 예외를 잡아서 처리하도록 throw new RuntimeException("MMS 발송 실패", exception); 할 수도 있습니다.
            }


        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("CoolMessageService: Image download interrupted for {}: {}", imgLink, e.getMessage(), e); // 추가: 스택 트레이스 포함
            throw new IOException("Image download interrupted", e);

        } catch (IOException e) { // downloadToTempFile 또는 uploadFile에서 발생한 IOException을 여기서 잡음
            log.error("CoolMessageService: Image processing or upload failed for {}: {}", imgLink, e.getMessage(), e); // 추가: 스택 트레이스 포함
            throw e; // 상위로 다시 던지기
        } finally {
            // 임시파일 정리
            if (tempPath != null) {
                try { Files.deleteIfExists(tempPath); } catch (Exception ignore) {
                    log.warn("CoolMessageService: Failed to delete temp file {}: {}", tempPath, ignore.getMessage()); // 정리 실패 시 로그
                }
            }
        }
    }

//    private Path downloadToTempFile(String imgLink) throws IOException, InterruptedException {
//        log.info("CoolMessageService: Attempting to download image from: {}", imgLink); // 추가
//        HttpClient client = HttpClient.newBuilder()
//            .followRedirects(HttpClient.Redirect.NORMAL)
//            .connectTimeout(Duration.ofSeconds(10))
//            .build();
//
//        HttpRequest request = HttpRequest.newBuilder()
//            .uri(URI.create(imgLink))
//            .timeout(Duration.ofSeconds(20))
//            .GET()
//            .build();
//
//        HttpResponse<byte[]> response;
//        try {
//            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
//        } catch (IOException | InterruptedException e) {
//            log.error("CoolMessageService: Failed to send HTTP request to download image from {}: {}", imgLink, e.getMessage(), e); // 추가: 스택 트레이스 포함
//            throw e;
//        }
//
//
//        int status = response.statusCode();
//        if (status < 200 || status >= 300) {
//            log.error("CoolMessageService: Failed to fetch image from {}. HTTP status: {}", imgLink, status); // 추가
//            throw new IOException("Failed to fetch image. status=" + status + ", body=" + new String(response.body())); // 응답 바디 포함
//        }
//        log.info("CoolMessageService: Successfully fetched image from {}. HTTP status: {}", imgLink, status); // 추가
//        // ... (이후 코드)
//
//        String contentType = response.headers().firstValue("content-type").orElse("");
//        String ext = guessExt(contentType, imgLink);
//
//        log.info("CoolMessageService: Guessed extension for {}: {}", imgLink, ext); // 추가
//        Path temp = Files.createTempFile("mms-img-", ext);
//        Files.write(temp, response.body(), StandardOpenOption.TRUNCATE_EXISTING);
//        log.info("CoolMessageService: Image saved to temporary file: {}", temp.toAbsolutePath()); // 추가
//
//        return temp;
//    }


    private Path downloadToTempFile(String imgLink) throws IOException, InterruptedException {
        log.info("CoolMessageService: Attempting to download image from: {}", imgLink);

        HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(imgLink))
            .timeout(Duration.ofSeconds(20))
            .GET()
            .build();

        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            log.error("CoolMessageService: Failed to send HTTP request to download image from {}: {}", imgLink, e.getMessage(), e);
            throw e;
        }

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            log.error("CoolMessageService: Failed to fetch image from {}. HTTP status: {}", imgLink, status);
            throw new IOException("Failed to fetch image. status=" + status + ", body=" + new String(response.body()));
        }
        log.info("CoolMessageService: Successfully fetched image from {}. HTTP status: {}", imgLink, status);

        // 🔴 여기부터: 항상 JPG로 변환해서 저장하기

        byte[] body = response.body();

        // 원본 바이트를 BufferedImage로 읽기
        BufferedImage original;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(body)) {
            original = ImageIO.read(bais);
        }

        if (original == null) {
            log.error("CoolMessageService: Failed to decode image from {}", imgLink);
            throw new IOException("Failed to decode image from response");
        }

        // JPEG는 알파 채널을 지원하지 않아서, RGB용 버퍼로 한 번 그려준다.
        BufferedImage rgbImage = new BufferedImage(
            original.getWidth(),
            original.getHeight(),
            BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = rgbImage.createGraphics();
        // 투명 배경이 있는 PNG일 경우 흰색 배경 위에 그리기
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, original.getWidth(), original.getHeight());
        g.drawImage(original, 0, 0, null);
        g.dispose();

        // 🔹 확장자는 무조건 ".jpg"로 고정
        String ext = ".jpg";
        log.info("CoolMessageService: Forcing extension to {}", ext);

        Path temp = Files.createTempFile("mms-img-", ext);
        // ImageIO를 이용해 JPEG로 인코딩하여 저장
        boolean written = ImageIO.write(rgbImage, "jpg", temp.toFile());
        if (!written) {
            log.error("CoolMessageService: Failed to write JPEG image to {}", temp.toAbsolutePath());
            throw new IOException("Failed to encode JPEG image");
        }

        log.info("CoolMessageService: Image saved as JPEG to temporary file: {}", temp.toAbsolutePath());

        return temp;
    }

    private String guessExt(String contentType, String url) {
        // Nurigo SDK가 .jpg만 처리한다고 가정하고 무조건 .jpg를 반환하도록 강제
        log.warn("CoolMessageService: Forcing image extension to .jpg due to reported Nurigo SDK limitation. Original Content-Type: {}, URL: {}", contentType, url);
        return ".jpg";
    }

    @Override
    public MessageLog.MessageChannel getMessageChannel() {
        return MessageLog.MessageChannel.SMS;
    }
}
