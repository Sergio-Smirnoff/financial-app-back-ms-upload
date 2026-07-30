package com.financialapp.upload.infrastructure.gateway;

import com.financialapp.upload.config.MinioConfig;
import com.financialapp.upload.domain.gateway.StatementStoragePort;
import com.financialapp.upload.service.MinioStorageService;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatementStoragePortImpl implements StatementStoragePort {

    private final MinioStorageService storageService;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public String store(String objectPath, InputStream data, long size, String contentType) {
        return storageService.store(objectPath, data, size, contentType);
    }

    @Override
    public InputStream retrieve(String objectPath) {
        return storageService.retrieve(objectPath);
    }

    @Override
    public void move(String fromPath, String toPath) {
        storageService.move(fromPath, toPath);
    }

    @Override
    public void deleteOlderThan(String prefix, Duration ageThreshold) {
        try {
            ZonedDateTime cutoff = ZonedDateTime.now().minus(ageThreshold);
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioConfig.getStatementsBucket())
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : results) {
                try {
                    Item item = result.get();
                    if (item.lastModified() != null && item.lastModified().isBefore(cutoff)) {
                        storageService.delete(item.objectName());
                        log.info("Deleted expired MinIO object: {}", item.objectName());
                    }
                } catch (Exception e) {
                    log.warn("Error processing MinIO retention for object: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to execute retention sweep on prefix {}: {}", prefix, e.getMessage());
        }
    }
}
