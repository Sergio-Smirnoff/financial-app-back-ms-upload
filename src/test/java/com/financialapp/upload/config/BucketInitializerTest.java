package com.financialapp.upload.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BucketInitializerTest {

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private BucketInitializer bucketInitializer;

    @Test
    void shouldCreateBucketsIfMissing() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(bucketInitializer, "statementsBucket", "statements");
        ReflectionTestUtils.setField(bucketInitializer, "receiptsBucket", "receipts");

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        // Act
        bucketInitializer.run();

        // Assert
        verify(minioClient, times(2)).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, times(2)).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void shouldNotCreateBucketsIfExist() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(bucketInitializer, "statementsBucket", "statements");
        ReflectionTestUtils.setField(bucketInitializer, "receiptsBucket", "receipts");

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        // Act
        bucketInitializer.run();

        // Assert
        verify(minioClient, times(2)).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }
}
