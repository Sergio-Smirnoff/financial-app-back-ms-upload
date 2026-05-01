package com.financialapp.upload.service;

import com.financialapp.upload.client.FinancesClient;
import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.dto.request.TransactionRequest;
import com.financialapp.upload.model.dto.response.ProcessingReport;
import com.financialapp.upload.model.entity.FileUpload;
import com.financialapp.upload.model.enums.FileUploadStatus;
import com.financialapp.upload.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingService {

    private final FileUploadRepository fileUploadRepository;
    private final StorageService storageService;
    private final ParsingService parsingService;
    private final FinancesClient financesClient;

    @Value("${minio.bucket.statements}")
    private String statementsBucket;

    public ProcessingReport process(Long fileId) {
        FileUpload fileUpload = fileUploadRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));

        log.info("Processing file: {} (ID: {})", fileUpload.getOriginalName(), fileId);

        List<ProcessingReport.RowError> errors = new ArrayList<>();
        int successCount = 0;
        int totalRows = 0;

        try {
            // 1. Load from storage
            try (InputStream is = storageService.load(fileUpload.getStoragePath(), statementsBucket)) {
                
                // 2. Parse content
                List<ParsedTransaction> parsedTransactions = parsingService.parse(is, fileUpload.getOriginalName(), Collections.emptyMap());
                totalRows = parsedTransactions.size();
                log.info("Found {} transactions in file", totalRows);

                // 3. Process each transaction
                int rowNumber = 1;
                for (ParsedTransaction pt : parsedTransactions) {
                    try {
                        TransactionRequest request = TransactionRequest.builder()
                                .type(pt.getType())
                                .amount(pt.getAmount())
                                .accountId(fileUpload.getBankAccountId())
                                .currency(pt.getCurrency())
                                .categoryId(1L) // Defaulting to category 1 (Uncategorized)
                                .description(pt.getDescription())
                                .date(pt.getDate())
                                .build();

                        financesClient.createTransaction(fileUpload.getUserId(), request);
                        successCount++;
                    } catch (Exception e) {
                        log.error("Error processing row {}: {}", rowNumber, e.getMessage());
                        errors.add(ProcessingReport.RowError.builder()
                                .rowNumber(rowNumber)
                                .description(pt.getDescription())
                                .errorMessage(e.getMessage())
                                .build());
                    }
                    rowNumber++;
                }

                // 4. Update status
                if (errors.isEmpty() && totalRows > 0) {
                    fileUpload.setStatus(FileUploadStatus.PROCESSED);
                } else if (successCount > 0) {
                    fileUpload.setStatus(FileUploadStatus.PROCESSED);
                } else {
                    fileUpload.setStatus(FileUploadStatus.ERROR);
                }
                fileUploadRepository.save(fileUpload);
                log.info("Processing completed for file {}. Success: {}, Errors: {}", fileId, successCount, errors.size());

            }
        } catch (Exception e) {
            log.error("Global error processing file {}", fileId, e);
            fileUpload.setStatus(FileUploadStatus.ERROR);
            fileUploadRepository.save(fileUpload);
            // Re-throw or return report with error status?
            // The task says return the report.
        }

        return ProcessingReport.builder()
                .fileId(fileId)
                .status(fileUpload.getStatus().name())
                .totalRows(totalRows)
                .successCount(successCount)
                .errorCount(errors.size())
                .errors(errors)
                .build();
    }
}
