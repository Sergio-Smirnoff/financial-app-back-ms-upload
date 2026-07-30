package com.financialapp.upload.application.importrun.impl;

import com.financialapp.upload.domain.common.model.DateRange;
import com.financialapp.upload.domain.common.model.Money;
import com.financialapp.upload.domain.exception.DuplicateImportException;

import com.financialapp.upload.domain.gateway.StatementParserPort;
import com.financialapp.upload.domain.gateway.StatementStoragePort;
import com.financialapp.upload.domain.gateway.TransactionRecorderPort;
import com.financialapp.upload.domain.model.importrun.FileHash;
import com.financialapp.upload.domain.model.importrun.ImportRun;

import com.financialapp.upload.domain.model.importrun.ReconciliationResult;
import com.financialapp.upload.domain.repository.ImportRunRepository;
import com.financialapp.upload.domain.usecase.importrun.ConfirmImport;
import com.financialapp.upload.domain.usecase.importrun.command.ConfirmImportCommand;
import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.dto.request.TransactionMappingRequest;
import com.financialapp.upload.model.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmImportUseCaseImpl implements ConfirmImport {

    private final ImportRunRepository importRunRepository;
    private final StatementParserPort parserPort;
    private final StatementStoragePort storagePort;
    private final TransactionRecorderPort transactionRecorderPort;

    @Override
    @Transactional
    public ImportRun execute(ConfirmImportCommand command) {
        byte[] rawBytes;
        try (InputStream is = storagePort.retrieve(command.tempKey())) {
            rawBytes = is.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file from storage for key: " + command.tempKey(), e);
        }

        FileHash fileHash = FileHash.ofBytes(rawBytes);

        if (importRunRepository.existsActiveByUserAndFileHash(command.userId(), fileHash)) {
            throw new DuplicateImportException(fileHash.value());
        }

        List<ParsedTransaction> transactions;
        if (command.manualMappings() != null && !command.manualMappings().isEmpty()) {
            transactions = mapManualRequests(command.manualMappings());
        } else {
            transactions = parserPort.parse(
                    new ByteArrayInputStream(rawBytes),
                    command.fileType(),
                    command.columnMapping()
            );
        }

        LocalDate minDate = LocalDate.now();
        LocalDate maxDate = LocalDate.now();
        if (!transactions.isEmpty()) {
            minDate = transactions.stream().map(ParsedTransaction::getDate).min(LocalDate::compareTo).orElse(LocalDate.now());
            maxDate = transactions.stream().map(ParsedTransaction::getDate).max(LocalDate::compareTo).orElse(LocalDate.now());
        }
        DateRange period = new DateRange(minDate, maxDate);

        ImportRun pendingRun = ImportRun.create(
                null,
                command.userId(),
                command.bankNumber(),
                command.accountCbu(),
                fileHash,
                period,
                LocalDateTime.now()
        );
        ImportRun savedRun = importRunRepository.save(pendingRun);

        List<Long> createdTransactionIds = new ArrayList<>();
        int skippedCount = 0;

        for (ParsedTransaction pt : transactions) {
            try {
                Long txId = transactionRecorderPort.recordTransaction(
                        command.userId().value(),
                        command.accountId(),
                        pt
                );
                if (txId != null) {
                    createdTransactionIds.add(txId);
                } else {
                    skippedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to record transaction: {}", pt.getDescription(), e);
                skippedCount++;
            }
        }

        BigDecimal calculatedSum = BigDecimal.ZERO;
        BigDecimal statementBalanceVal = null;

        for (ParsedTransaction pt : transactions) {
            if (pt.getAmount() != null) {
                if (pt.getType() == TransactionType.EXPENSE) {
                    calculatedSum = calculatedSum.subtract(pt.getAmount());
                } else {
                    calculatedSum = calculatedSum.add(pt.getAmount());
                }
            }
            if (pt.getRunningBalance() != null) {
                statementBalanceVal = pt.getRunningBalance();
            }
        }

        Money calculatedMoney = Money.of(calculatedSum, "ARS");
        Money statementMoney = statementBalanceVal != null ? Money.of(statementBalanceVal, "ARS") : null;
        ReconciliationResult reconciliation = ReconciliationResult.of(statementMoney, calculatedMoney);

        ImportRun completedRun = savedRun.markCompleted(createdTransactionIds, skippedCount, reconciliation);
        ImportRun finalRun = importRunRepository.save(completedRun);

        String destinationPath = "imports/" + command.userId().value() + "/" + finalRun.id().value() + "/statement";
        try {
            storagePort.move(command.tempKey(), destinationPath);
        } catch (Exception e) {
            log.warn("Failed to move file in MinIO from {} to {}: {}", command.tempKey(), destinationPath, e.getMessage());
        }

        return finalRun;
    }

    private List<ParsedTransaction> mapManualRequests(List<TransactionMappingRequest> manualMappings) {
        return manualMappings.stream().map(m -> ParsedTransaction.builder()
                .date(m.getDate())
                .description(m.getDescription())
                .amount(m.getAmount())
                .currency(m.getCurrency() != null ? m.getCurrency() : "ARS")
                .type(m.getType())
                .build()
        ).toList();
    }
}
