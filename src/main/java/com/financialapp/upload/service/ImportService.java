package com.financialapp.upload.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialapp.upload.client.BanksClient;
import com.financialapp.upload.client.FinancesClient;
import com.financialapp.upload.exception.BusinessException;
import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.dto.request.*;
import com.financialapp.upload.model.dto.response.*;
import com.financialapp.upload.model.entity.StatementImport;
import com.financialapp.upload.model.enums.FileType;
import com.financialapp.upload.model.enums.ImportStatus;
import com.financialapp.upload.repository.StatementImportRepository;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportService {

    private final MinioStorageService storageService;
    private final ParsingService parsingService;
    private final StatementImportRepository repository;
    private final FinancesClient financesClient;
    private final BanksClient banksClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public PreviewResponse preview(MultipartFile file, FileType type, Long userId, Map<String, String> context) {
        try {
            byte[] bytes = file.getBytes();
            String hash = calculateHash(bytes);

            repository.findByUserIdAndFileHash(userId, hash).ifPresent(existing -> {
                throw new BusinessException("File already imported on " + existing.getCreatedAt());
            });

            String tempKey = "temp/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
            storageService.store(tempKey, new ByteArrayInputStream(bytes), bytes.length, file.getContentType());

            List<String> headers = null;
            List<List<String>> rows = null;

            if (type == FileType.CSV) {
                try (CSVReader reader = new CSVReader(new InputStreamReader(new ByteArrayInputStream(bytes)))) {
                    String[] headerArray = reader.readNext();
                    if (headerArray != null) {
                        headers = Arrays.asList(headerArray);
                        rows = new ArrayList<>();
                        String[] line;
                        while ((line = reader.readNext()) != null && rows.size() < 10) {
                            rows.add(Arrays.asList(line));
                        }
                    }
                }
            }

            List<ParsedTransaction> allTransactions = parsingService.parse(new ByteArrayInputStream(bytes), type, context);
            
            Map<String, Integer> currencyCounts = new HashMap<>();
            for (ParsedTransaction tx : allTransactions) {
                currencyCounts.merge(tx.getCurrency(), 1, Integer::sum);
            }

            return PreviewResponse.builder()
                    .tempKey(tempKey)
                    .fileHash(hash)
                    .headers(headers)
                    .rows(rows)
                    .preview(allTransactions.stream().limit(5).toList())
                    .totalCount(allTransactions.size())
                    .currencyCounts(currencyCounts)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating preview", e);
            throw new RuntimeException("Could not generate preview", e);
        }
    }

    @Transactional
    public ConfirmResponse confirm(ConfirmRequest request, Long userId) {
        try {
            InputStream is = storageService.retrieve(request.getTempKey());
            Map<String, String> context = new HashMap<>();
            if (request.getColumnMapping() != null) {
                context.put("dateCol", String.valueOf(request.getColumnMapping().getDateCol()));
                context.put("descCol", String.valueOf(request.getColumnMapping().getDescCol()));
                if (request.getColumnMapping().getExpenseCol() != null)
                    context.put("expenseCol", String.valueOf(request.getColumnMapping().getExpenseCol()));
                if (request.getColumnMapping().getIncomeCol() != null)
                    context.put("incomeCol", String.valueOf(request.getColumnMapping().getIncomeCol()));
            }
            // By not putting a forced "dateFormat" here, GenericCsvParser will use its detectPattern logic
            // But we can still provide yyyy-MM-dd as a hint if we want. 
            // The previous requirement was "the date must be saved in the db with that format", 
            // which the parser ensures by converting to LocalDate anyway.

            List<ParsedTransaction> allTransactions = parsingService.parse(is, request.getType(), context);
            log.info("Confirm step: re-parsed {} transactions for userId {}", allTransactions.size(), userId);

            List<ParsedTransaction> toImport = new ArrayList<>();
            List<ConfirmResponse.DuplicateItem> duplicates = new ArrayList<>();
            Map<String, ParsedTransaction> dupeMap = new HashMap<>();

            if (request.getType() == FileType.VISA_PDF) {
                List<CardExpenseCreateRequest> checkReqs = allTransactions.stream()
                        .map(tx -> new CardExpenseCreateRequest(tx.getDescription(), tx.getAmount(), tx.getCurrency(), 1, tx.getDate()))
                        .toList();

                log.info("Checking duplicates for {} Visa expenses", checkReqs.size());
                List<Integer> duplicateIndices = banksClient.checkDuplicates(request.getCardId(), checkReqs).getData();
                log.info("Found {} Visa duplicates", duplicateIndices.size());
                Set<Integer> dupeSet = new HashSet<>(duplicateIndices);

                for (int i = 0; i < allTransactions.size(); i++) {
                    ParsedTransaction tx = allTransactions.get(i);
                    if (dupeSet.contains(i)) {
                        String dupeId = UUID.randomUUID().toString();
                        duplicates.add(ConfirmResponse.DuplicateItem.builder()
                                .id(dupeId)
                                .date(tx.getDate().toString())
                                .description(tx.getDescription())
                                .amount(tx.getAmount().doubleValue())
                                .currency(tx.getCurrency())
                                .build());
                        dupeMap.put(dupeId, tx);
                    } else {
                        toImport.add(tx);
                    }
                }

                if (!toImport.isEmpty()) {
                    log.info("Importing {} new Visa expenses", toImport.size());
                    importToBanks(request.getCardId(), request.getArsAccountId(), request.getUsdAccountId(), userId, toImport);
                }
            } else {
                List<TransactionRequest> checkReqs = allTransactions.stream()
                        .map(tx -> TransactionRequest.builder()
                                .type(com.financialapp.upload.model.enums.TransactionType.valueOf(tx.getType().name()))
                                .amount(tx.getAmount())
                                .accountId(request.getAccountId())
                                .currency(tx.getCurrency())
                                .description(tx.getDescription())
                                .date(tx.getDate())
                                .build())
                        .toList();

                log.info("Checking duplicates for {} bank transactions", checkReqs.size());
                List<Integer> duplicateIndices = financesClient.checkDuplicates(checkReqs).getData();
                log.info("Found {} bank duplicates", duplicateIndices.size());
                Set<Integer> dupeSet = new HashSet<>(duplicateIndices);

                for (int i = 0; i < allTransactions.size(); i++) {
                    ParsedTransaction tx = allTransactions.get(i);
                    if (dupeSet.contains(i)) {
                        String dupeId = UUID.randomUUID().toString();
                        duplicates.add(ConfirmResponse.DuplicateItem.builder()
                                .id(dupeId)
                                .date(tx.getDate().toString())
                                .description(tx.getDescription())
                                .amount(tx.getAmount().doubleValue())
                                .currency(tx.getCurrency())
                                .build());
                        dupeMap.put(dupeId, tx);
                    } else {
                        toImport.add(tx);
                    }
                }

                if (!toImport.isEmpty()) {
                    log.info("Importing {} new bank transactions", toImport.size());
                    importToFinances(request.getAccountId(), userId, toImport);
                }
            }
            String sessionId = null;
            if (!duplicates.isEmpty()) {
                sessionId = UUID.randomUUID().toString();
                String sessionKey = "temp/dupes/" + sessionId + ".json";
                Map<String, Object> sessionData = new HashMap<>();
                sessionData.put("dupeMap", dupeMap);
                sessionData.put("request", request);
                sessionData.put("userId", userId);
                byte[] json = objectMapper.writeValueAsBytes(sessionData);
                storageService.store(sessionKey, new ByteArrayInputStream(json), json.length, "application/json");
            }

            // Record the import record
            StatementImport stmtImport = StatementImport.builder()
                    .userId(userId)
                    .fileType(request.getType().name())
                    .originalName(request.getTempKey().substring(request.getTempKey().lastIndexOf("/") + 1))
                    .fileHash(null)
                    .accountId(request.getAccountId())
                    .cardId(request.getCardId())
                    .minioPath(request.getTempKey())
                    .importedCount(toImport.size())
                    .importStatus(duplicates.isEmpty() ? ImportStatus.COMPLETED.name() : ImportStatus.PARTIAL.name())
                    .createdAt(LocalDateTime.now())
                    .build();
            repository.save(stmtImport);

            return ConfirmResponse.builder()
                    .imported(toImport.size())
                    .skipped(duplicates.size())
                    .errors(Collections.emptyList())
                    .duplicates(duplicates)
                    .sessionId(sessionId)
                    .build();

        } catch (Exception e) {
            log.error("Error confirming import", e);
            throw new RuntimeException("Import confirmation failed: " + e.getMessage(), e);
        }
    }

    private void importToBanks(Long cardId, Long arsId, Long usdId, Long userId, List<ParsedTransaction> txs) {
        CardExpenseImportRequest importReq = new CardExpenseImportRequest(
                arsId, usdId,
                txs.stream().map(tx -> new CardExpenseImportRequest.ImportedExpense(
                        tx.getDescription(), tx.getAmount(), tx.getCurrency(), tx.getDate()
                )).toList()
        );
        banksClient.importCardExpenses(cardId, userId, importReq);
    }

    private void importToFinances(Long accountId, Long userId, List<ParsedTransaction> txs) {
        for (ParsedTransaction tx : txs) {
            TransactionRequest txReq = TransactionRequest.builder()
                    .type(com.financialapp.upload.model.enums.TransactionType.valueOf(tx.getType().name()))
                    .amount(tx.getAmount())
                    .accountId(accountId)
                    .currency(tx.getCurrency())
                    .categoryId(tx.getType() == com.financialapp.upload.model.enums.TransactionType.INCOME ? 1105L : 1104L)
                    .description(tx.getDescription())
                    .date(tx.getDate())
                    .build();
            financesClient.createTransaction(userId, true, txReq);
        }
    }

    public List<ImportHistoryRecord> getHistory(Long userId) {
        return repository.findAll().stream()
                .filter(i -> i.getUserId().equals(userId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(i -> ImportHistoryRecord.builder()
                        .id(i.getId())
                        .originalName(i.getOriginalName())
                        .fileType(i.getFileType())
                        .accountId(i.getAccountId())
                        .cardId(i.getCardId())
                        .importedCount(i.getImportedCount())
                        .importStatus(i.getImportStatus())
                        .createdAt(i.getCreatedAt().toString())
                        .build())
                .toList();
    }

    @Transactional
    public ResolveResponse resolveDuplicates(ResolveRequest request, Long userId) {
        try {
            String sessionKey = "temp/dupes/" + request.getSessionId() + ".json";
            InputStream is = storageService.retrieve(sessionKey);
            
            // Need a TypeReference for complex nested map with ParsedTransaction
            Map<String, Object> sessionData = objectMapper.readValue(is, new TypeReference<Map<String, Object>>() {});
            
            ConfirmRequest originalRequest = objectMapper.convertValue(sessionData.get("request"), ConfirmRequest.class);
            Map<String, ParsedTransaction> dupeMap = objectMapper.convertValue(sessionData.get("dupeMap"), new TypeReference<Map<String, ParsedTransaction>>() {});
            
            List<ParsedTransaction> toImport = new ArrayList<>();
            for (String keepId : request.getKeepIds()) {
                if (dupeMap.containsKey(keepId)) {
                    toImport.add(dupeMap.get(keepId));
                }
            }

            if (!toImport.isEmpty()) {
                if (originalRequest.getType() == FileType.VISA_PDF) {
                    importToBanks(originalRequest.getCardId(), originalRequest.getArsAccountId(), originalRequest.getUsdAccountId(), userId, toImport);
                } else {
                    importToFinances(originalRequest.getAccountId(), userId, toImport);
                }
            }

            storageService.delete(sessionKey);
            return ResolveResponse.builder().imported(toImport.size()).skipped(dupeMap.size() - toImport.size()).build();
        } catch (Exception e) {
            log.error("Error resolving duplicates", e);
            throw new RuntimeException("Resolution failed: " + e.getMessage(), e);
        }
    }

    private String calculateHash(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(bytes);
        StringBuilder hexString = new StringBuilder();
        for (byte b : encodedHash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append("0");
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
