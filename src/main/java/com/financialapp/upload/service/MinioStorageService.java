package com.financialapp.upload.service;

import com.financialapp.upload.config.MinioConfig;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    public String store(String objectPath, InputStream data, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getStatementsBucket())
                    .object(objectPath)
                    .stream(data, size, -1)
                    .contentType(contentType)
                    .build());
            log.debug("Stored object: {}", objectPath);
            return objectPath;
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file in MinIO: " + e.getMessage(), e);
        }
    }

    public InputStream retrieve(String objectPath) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioConfig.getStatementsBucket())
                    .object(objectPath)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve file from MinIO: " + objectPath, e);
        }
    }

    public void move(String fromPath, String toPath) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(minioConfig.getStatementsBucket())
                    .object(toPath)
                    .source(CopySource.builder()
                            .bucket(minioConfig.getStatementsBucket())
                            .object(fromPath)
                            .build())
                    .build());
            delete(fromPath);
            log.debug("Moved {} -> {}", fromPath, toPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to move object in MinIO: " + e.getMessage(), e);
        }
    }

    public void delete(String objectPath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioConfig.getStatementsBucket())
                    .object(objectPath)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete object {}: {}", objectPath, e.getMessage());
        }
    }
}
