package com.financialapp.upload.model.entity;

import com.financialapp.upload.model.enums.FileUploadStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "files", schema = "upload")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FileUploadStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
