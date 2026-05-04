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

    @Column(name = "account_number", nullable = false, length = 100)
    private String accountNumber;

    @Column(name = "period_key", nullable = false, length = 50)
    private String periodKey;

    @Column(name = "minio_path", nullable = false, length = 500)
    private String minioPath;

    @Column(name = "imported_count", nullable = false)
    private int importedCount;

    @Column(name = "import_status", nullable = false, length = 20)
    private String importStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
