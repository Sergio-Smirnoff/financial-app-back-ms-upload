package com.financialapp.upload.infrastructure.persistence.entity;
import com.financialapp.commons.core.domain.model.Cbu;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "import_runs", schema = "upload")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRunJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bank_number", nullable = false, length = 3)
    private String bankNumber;

    @Column(name = "account_cbu", nullable = false, length = 22)
    private String accountCbu;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "imported_count", nullable = false)
    private int importedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Column(name = "reconciliation", columnDefinition = "jsonb")
    private String reconciliationJson;

    @Column(name = "last_stale_alert_at")
    private LocalDateTime lastStaleAlertAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "import_run_transactions",
            schema = "upload",
            joinColumns = @JoinColumn(name = "import_run_id")
    )
    @Column(name = "transaction_id")
    @Builder.Default
    private List<Long> transactionIds = new ArrayList<>();
}
