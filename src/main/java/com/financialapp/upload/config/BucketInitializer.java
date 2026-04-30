package com.financialapp.upload.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BucketInitializer implements CommandLineRunner {

    private final MinioClient minioClient;

    @Value("${minio.bucket.statements}")
    private String statementsBucket;

    @Value("${minio.bucket.receipts}")
    private String receiptsBucket;

    public BucketInitializer(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public void run(String... args) throws Exception {
        initializeBucket(statementsBucket);
        initializeBucket(receiptsBucket);
    }

    private void initializeBucket(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            log.info("Creating bucket: {}", bucketName);
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        } else {
            log.info("Bucket already exists: {}", bucketName);
        }
    }
}
