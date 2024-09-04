package com.daewon.xeno_backend.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.daewon.xeno_backend.domain.ProductsImage;
import com.daewon.xeno_backend.repository.Products.ProductsImageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class S3Service {
    private final ProductsImageRepository productsImageRepository;
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private final AmazonS3 s3Client;

    public static String calculateFileHash(InputStream inputStream) throws Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm not found", e);
        }
    }

    public String calculateFileHash(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            return Base64.getEncoder().encodeToString(hashBytes);
        }
    }

    public String getFileHash(String fileUrl) throws Exception {
        String keyPrefix = "https://" + bucketName + ".s3.ap-northeast-2.amazonaws.com/";
        if (fileUrl.startsWith(keyPrefix)) {
            String fileName = fileUrl.substring(keyPrefix.length());
            S3Object s3Object = s3Client.getObject(bucketName, fileName);
            try (InputStream inputStream = s3Object.getObjectContent()) {
                return calculateFileHash(inputStream);
            }
        } else {
            throw new IllegalArgumentException("URL does not start with expected prefix: " + fileUrl);
        }

    }

    public String saveImage(MultipartFile image) {
        String fileName = image.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String key = uuid + "_" + fileName;

        try (InputStream inputStream = image.getInputStream()) {
            // S3에 파일 업로드
            s3Client.putObject(new PutObjectRequest(bucketName, key, inputStream, null));
            log.info("이미지 업로드 성공: " + key);

            // S3 URL 생성
            String fileUrl = "https://" + bucketName + ".s3.ap-northeast-2.amazonaws.com/" + key;
            return fileUrl; // S3에서의 객체 URL 반환
        } catch (IOException e) {
            log.error("파일 업로드 도중 오류가 발생했습니다: ", e);
            throw new RuntimeException("File upload error", e);
        }
    }

    @Transactional
//    @Scheduled(cron = "0 */2 * * * ?") // 매 2분마다 실행
    @Scheduled(cron = "0 0 0 * * ?") // 매일 자정에 실행
    public void deleteOldS3Objects() {
        List<ProductsImage> oldImages = productsImageRepository.findImagesWithoutProductId();
        for (ProductsImage image : oldImages) {
            deleteObjectFromS3(image.getUrl_1());
            deleteObjectFromS3(image.getUrl_2());
            deleteObjectFromS3(image.getUrl_4());
            deleteObjectFromS3(image.getUrl_5());
            deleteObjectFromS3(image.getUrl_6());
            deleteObjectFromS3(image.getDetail_url());
            productsImageRepository.deleteById(image.getProductImageId());
        }
    }


    public void deleteObjectFromS3(String url) {
        if (url != null && !url.isEmpty()) {
            String key = extractKeyFromUrl(url); // URL에서 키 추출
            s3Client.deleteObject(bucketName, key);
        }
    }

    public String extractKeyFromUrl(String url) {
        // URL에서 S3 객체의 키를 추출하는 로직
        String keyPrefix = "https://" + bucketName + ".s3.ap-northeast-2.amazonaws.com/";
        if (url.startsWith(keyPrefix)) {
            return url.substring(keyPrefix.length());
        } else {
            throw new IllegalArgumentException("URL does not start with expected prefix: " + url);
        }
    }

}
