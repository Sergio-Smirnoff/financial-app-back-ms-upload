package com.financialapp.upload.service;

import com.financialapp.upload.client.FinancesClient;
import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.dto.request.CsvConfirmRequest;
import com.financialapp.upload.model.dto.request.StatementConfirmRequest;
import com.financialapp.upload.model.dto.request.TransactionMappingRequest;
import com.financialapp.upload.model.dto.request.TransactionRequest;
import com.financialapp.upload.model.dto.response.*;
import com.financialapp.upload.model.entity.StatementImport;
import com.financialapp.upload.model.entity.UploadSession;
import com.financialapp.upload.model.enums.FileType;
import com.financialapp.upload.model.enums.ImportStatus;
import com.financialapp.upload.repository.StatementImportRepository;
import com.financialapp.upload.repository.UploadSessionRepository;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatementService {

    private final MinioStorageService storageService;
    private final ParsingService parsingService;
    private final FinancesClient financesClient;
    private final StatementImportRepository repository;
    private final UploadSessionRepository sessionRepository;

    public StatementPreviewResponse previewPdf(MultipartFile file, FileType fileType, Long userId) {
        try {
            String tempKey = "temp/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
            storageService.store(tempKey, file.getInputStream(), file.getSize(), file.getContentType());

            sessionRepository.save(UploadSession.builder()
                    .tempKey(tempKey)
                    .userId(userId)
                    .build());

            List<ParsedTransaction> transactions = parsingService.parse(file.getInputStream(), fileType, Collections.emptyMap());
            
            BigDecimal total = transactions.stream()
                    .map(ParsedTransaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return StatementPreviewResponse.builder()
                    .tempKey(tempKey)
                    .accountNumber("Detected from file") // Simplified
                    .transactions(transactions)
                    .totalAmount(total)
                    .count(transactions.size())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF preview", e);
        }
    }

    public CsvPreviewResponse previewCsv(MultipartFile file, Long userId) {
        try {
            String tempKey = "temp/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
            storageService.store(tempKey, file.getInputStream(), file.getSize(), file.getContentType());

            sessionRepository.save(UploadSession.builder()
                    .tempKey(tempKey)
                    .userId(userId)
                    .build());

            try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
                String[] headers = reader.readNext();
                List<List<String>> rows = new ArrayList<>();
                String[] row;
                int count = 0;
                while ((row = reader.readNext()) != null && count < 5) {
                    rows.add(Arrays.asList(row));
                    count++;
                }

                return CsvPreviewResponse.builder()
                        .tempKey(tempKey)
                        .headers(headers != null ? Arrays.asList(headers) : Collections.emptyList())
                        .rows(rows)
                        .build();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating CSV preview", e);
        }
    }

    public StatementConfirmResponse confirmPdf(StatementConfirmRequest request, Long userId) {
        validateSession(request.getTempKey(), userId);
        try {
            int successCount = 0;
            if (request.getMappings() != null && !request.getMappings().isEmpty()) {
                successCount = processMappings(request.getMappings(), request.getAccountId(), userId);
            } else {
                InputStream is = storageService.retrieve(request.getTempKey());
                List<ParsedTransaction> transactions = parsingService.parse(is, request.getFileType(), Collections.emptyMap());
                successCount = processParsedTransactions(transactions, request.getAccountId(), userId);
            }

            StatementImport statementImport = recordImport(userId, request.getFileType(), request.getTempKey(), successCount);
            
            return StatementConfirmResponse.builder()
                    .importId(statementImport.getId())
                    .status(ImportStatus.COMPLETED)
                    .importedCount(successCount)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error confirming PDF import", e);
        }
    }

    public CsvImportResponse confirmCsv(CsvConfirmRequest request, Long userId) {
        validateSession(request.getTempKey(), userId);
        try {
            int successCount = 0;
            if (request.getMappings() != null && !request.getMappings().isEmpty()) {
                successCount = processMappings(request.getMappings(), request.getAccountId(), userId);
            } else {
                InputStream is = storageService.retrieve(request.getTempKey());
                Map<String, String> context = new HashMap<>();
                context.put("dateCol", String.valueOf(request.getDateCol()));
                context.put("descCol", String.valueOf(request.getDescCol()));
                context.put("debitCol", String.valueOf(request.getDebitCol()));
                context.put("creditCol", String.valueOf(request.getCreditCol()));
                context.put("dateFormat", request.getDateFormat() != null ? request.getDateFormat() : "MM/dd/yy");
                
                List<ParsedTransaction> transactions = parsingService.parse(is, request.getFileType(), context);
                successCount = processParsedTransactions(transactions, request.getAccountId(), userId);
            }

            StatementImport statementImport = recordImport(userId, request.getFileType(), request.getTempKey(), successCount);

            return CsvImportResponse.builder()
                    .importId(statementImport.getId())
                    .status(ImportStatus.COMPLETED)
                    .importedCount(successCount)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Error confirming CSV import", e);
        }
    }

    private int processParsedTransactions(List<ParsedTransaction> transactions, Long accountId, Long userId) {
        int successCount = 0;
        for (ParsedTransaction pt : transactions) {
            try {
                TransactionRequest txReq = TransactionRequest.builder()
                        .type(pt.getType())
                        .amount(pt.getAmount())
                        .accountId(accountId)
                        .currency(pt.getCurrency())
                        .categoryId(getDefaultCategoryId(pt.getType()))
                        .description(pt.getDescription())
                        .date(pt.getDate())
                        .build();
                financesClient.createTransaction(userId, txReq);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to import transaction: {}", pt.getDescription(), e);
            }
        }
        return successCount;
    }

    private int processMappings(List<TransactionMappingRequest> mappings, Long accountId, Long userId) {
        int successCount = 0;
        for (TransactionMappingRequest mapping : mappings) {
            try {
                TransactionRequest txReq = TransactionRequest.builder()
                        .type(mapping.getType())
                        .amount(mapping.getAmount())
                        .accountId(accountId)
                        .currency(mapping.getCurrency())
                        .categoryId(mapping.getCategoryId())
                        .description(mapping.getDescription())
                        .date(mapping.getDate())
                        .build();
                financesClient.createTransaction(userId, txReq);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to import mapped transaction: {}", mapping.getDescription(), e);
            }
        }
        return successCount;
    }

    private StatementImport recordImport(Long userId, FileType fileType, String path, int count) {
        StatementImport statementImport = StatementImport.builder()
                .userId(userId)
                .fileType(fileType != null ? fileType.name() : "UNKNOWN")
                .accountNumber("N/A")
                .periodKey(UUID.randomUUID().toString())
                .minioPath(path)
                .importedCount(count)
                .importStatus(ImportStatus.COMPLETED.name())
                .createdAt(LocalDateTime.now())
                .build();
        return repository.save(statementImport);
    }

    private Long getDefaultCategoryId(com.financialapp.upload.model.enums.TransactionType type) {
        // Based on V14 migration:
        // 1104 = Unassigned EXPENSE subcategory
        // 1105 = Unassigned INCOME subcategory
        return type == com.financialapp.upload.model.enums.TransactionType.INCOME ? 1105L : 1104L;
    }

    public List<StatementImport> getHistory(Long userId) {
        return repository.findAll().stream()
                .filter(i -> i.getUserId().equals(userId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    private void validateSession(String tempKey, Long userId) {
        UploadSession session = sessionRepository.findById(tempKey)
                .orElseThrow(() -> new RuntimeException("Upload session not found or expired"));
        if (!session.getUserId().equals(userId)) {
            log.error("Security alert: User {} tried to hijack upload session {} owned by user {}", 
                    userId, tempKey, session.getUserId());
            throw new RuntimeException("Unauthorized: You do not own this upload session");
        }
    }
}
