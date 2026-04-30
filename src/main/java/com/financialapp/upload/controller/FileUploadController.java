package com.financialapp.upload.controller;

import com.financialapp.upload.model.dto.response.ApiResponse;
import com.financialapp.upload.model.dto.response.FileUploadResponse;
import com.financialapp.upload.model.entity.FileUpload;
import com.financialapp.upload.model.enums.FileUploadStatus;
import com.financialapp.upload.repository.FileUploadRepository;
import com.financialapp.upload.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final StorageService storageService;
    private final FileUploadRepository fileUploadRepository;

    @Value("${minio.bucket.statements}")
    private String statementsBucket;

    @PostMapping("/files")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bankAccountId", required = false) Long bankAccountId,
            @RequestHeader("X-User-Id") String userIdHeader) {

        log.info("Received upload request for file: {}, user: {}, bankAccount: {}", 
                file.getOriginalFilename(), userIdHeader, bankAccountId);

        try {
            Long userId = Long.valueOf(userIdHeader);
            
            // 1. Store in MinIO
            String storagePath = storageService.store(file, statementsBucket);

            // 2. Save metadata in DB
            FileUpload fileUpload = FileUpload.builder()
                    .userId(userId)
                    .bankAccountId(bankAccountId)
                    .originalName(file.getOriginalFilename())
                    .storagePath(storagePath)
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .status(FileUploadStatus.UPLOADED)
                    .build();

            FileUpload savedFile = fileUploadRepository.save(fileUpload);

            // 3. Prepare response
            FileUploadResponse response = FileUploadResponse.builder()
                    .id(savedFile.getId())
                    .originalName(savedFile.getOriginalName())
                    .storagePath(savedFile.getStoragePath())
                    .status(savedFile.getStatus())
                    .createdAt(savedFile.getCreatedAt())
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("File uploaded successfully", response));

        } catch (NumberFormatException e) {
            log.error("Invalid user ID header: {}", userIdHeader);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<FileUploadResponse>builder()
                            .success(false)
                            .message("Invalid User ID")
                            .build());
        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<FileUploadResponse>builder()
                            .success(false)
                            .message("Error uploading file: " + e.getMessage())
                            .build());
        }
    }
}
