package com.financialapp.upload.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "statement_imports", schema = "upload")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "bank_id")
    private Long bankId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "minio_path", nullable = false, length = 500)
    private String minioPath;

    @Column(name = "imported_count", nullable = false)
    private int importedCount;

    @Column(name = "import_status", nullable = false, length = 20)
    private String importStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
