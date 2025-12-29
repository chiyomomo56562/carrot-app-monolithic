package com.carrot.app.infra.s3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.carrot.app.global.exception.ImageProcessFailedException;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;

    @Value("${aws.cloudfront.domain}")
    private String CLOUDFRONT_DOMAIN;

    @Value("${aws.s3.bucket-name}")
    private String BUCKET_NAME;

    private static final String PRODUCT_IMAGE_DIR = "product-images/";
    private static final int MAX_WIDTH = 800; // 리사이징 기준 최대 너비 (800px)
    private static final float JPEG_QUALITY = 0.8f; // JPEG 압축 품질 (80%)

    // 최적화된 이미지 업로드
    public String uploadOptimizedImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return null;
        }

        String fileName = UUID.randomUUID().toString();
        String key = PRODUCT_IMAGE_DIR + fileName + ".jpg";

        try {
            byte[] resizedBytes = resizeImage(file);
            log.info("### 이미지 업로드 전");
            // InputStream으로 변환
            ByteArrayInputStream inputStream = new ByteArrayInputStream(resizedBytes);

            ObjectMetadata metadata = ObjectMetadata.builder()
                    .contentLength((long) resizedBytes.length)
                    .contentType("image/jpeg")
                    .build();

            s3Template.upload(BUCKET_NAME, key, inputStream, metadata);
            log.info("### 이미지 업로드 후");
            return CLOUDFRONT_DOMAIN + key;
        } catch (Exception e) {
            log.error("### 이미지 업로드 실패: {}", e.getMessage(), e);
            throw new ImageProcessFailedException("이미지 처리에 실패했습니다.");
        }
    }

    // 이미지 리사이징
    public byte[] resizeImage(MultipartFile file) {
        try {
            InputStream originalBytes = file.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Thumbnails.of(originalBytes)
                    .width(MAX_WIDTH)
                    .outputFormat("jpg")
                    .outputQuality(JPEG_QUALITY)
                    .toOutputStream(outputStream);

            log.info("이미지 리사이징 완료");
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ImageProcessFailedException("이미지 처리에 실패했습니다.");
        }
    }

    // 이미지 삭제
    public void deleteFile(String key) {
        s3Template.deleteObject(BUCKET_NAME, key);
    }
}