package com.financialapp.upload.model.dto.response;

import com.financialapp.upload.model.enums.FileUploadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private Long id;
    private String originalName;
    private String storagePath;
    private FileUploadStatus status;
    private LocalDateTime createdAt;
}
