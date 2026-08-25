package com.jspark.pw3_attendant.service.faceimage;

import com.jspark.pw3_attendant.common.config.MinioProperties;
import com.jspark.pw3_attendant.common.exception.ApiException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http.Method;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.InputStream;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioFaceImageStorage {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public void upload(String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(inputStream, size, -1L)
                    .contentType(contentType)
                    .build());
        } catch (Exception exception) {
            throw storageFailure("얼굴 사진을 저장하지 못했습니다.", exception);
        }
    }

    public PresignedImageUrl createPresignedUrl(String objectKey) {
        int expirySeconds = Math.toIntExact(properties.presignedUrlValidity().toSeconds());
        if (expirySeconds < 1 || expirySeconds > 604_800) {
            throw new IllegalStateException("MINIO_PRESIGNED_URL_VALIDITY must be between 1 second and 7 days");
        }
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .expiry(expirySeconds)
                    .build());
            return new PresignedImageUrl(url, Instant.now().plusSeconds(expirySeconds));
        } catch (Exception exception) {
            throw storageFailure("얼굴 사진 조회 URL을 생성하지 못했습니다.", exception);
        }
    }

    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw storageFailure("얼굴 사진을 삭제하지 못했습니다.", exception);
        }
    }

    public void deleteQuietly(String objectKey) {
        try {
            delete(objectKey);
        } catch (RuntimeException exception) {
            log.warn("MinIO object cleanup failed: {}", objectKey, exception);
        }
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.bucket())
                .build());
        if (!exists) {
            try {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(properties.bucket())
                        .build());
            } catch (Exception exception) {
                if (!minioClient.bucketExists(BucketExistsArgs.builder()
                        .bucket(properties.bucket())
                        .build())) {
                    throw exception;
                }
            }
        }
    }

    private ApiException storageFailure(String message, Exception cause) {
        log.error(message, cause);
        return new ApiException(HttpStatus.BAD_GATEWAY, "FACE_IMAGE_STORAGE_ERROR", message);
    }

    public record PresignedImageUrl(String url, Instant expiresAt) {
    }
}
