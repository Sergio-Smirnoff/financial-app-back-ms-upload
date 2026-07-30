package com.financialapp.upload.controller;

import com.financialapp.commons.core.response.ApiResponse;
import com.financialapp.commons.web.openapi.ApiErrorCodes;
import com.financialapp.upload.domain.common.model.BankNumber;
import com.financialapp.upload.domain.common.model.Cbu;
import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.exception.DomainError;
import com.financialapp.upload.domain.exception.ImportRunNotFoundException;
import com.financialapp.upload.domain.model.importrun.ImportRun;
import com.financialapp.upload.domain.model.importrun.ImportRunId;
import com.financialapp.upload.domain.model.mapping.AmountMapping;
import com.financialapp.upload.domain.model.mapping.ColumnMapping;
import com.financialapp.upload.domain.model.mapping.SeparateDebitCredit;
import com.financialapp.upload.domain.model.mapping.SingleSignedColumn;
import com.financialapp.upload.domain.usecase.importrun.*;
import com.financialapp.upload.domain.usecase.importrun.command.ConfirmImportCommand;
import com.financialapp.upload.domain.usecase.importrun.command.UndoImportCommand;
import com.financialapp.upload.model.dto.request.CsvConfirmRequest;
import com.financialapp.upload.model.dto.request.StatementConfirmRequest;
import com.financialapp.upload.model.dto.response.*;
import com.financialapp.upload.model.enums.FileType;
import com.financialapp.upload.model.enums.ImportStatus;
import com.financialapp.upload.service.StatementService;
import com.financialapp.upload.web.dto.response.ImportRunResponse;
import com.financialapp.upload.web.dto.response.UndoResultResponse;
import com.financialapp.upload.web.mapper.ImportRunWebMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;
    private final ConfirmImport confirmImport;
    private final UndoImport undoImport;
    private final GetImportRun getImportRun;
    private final ListImportRuns listImportRuns;
    private final FindImportRunByTransaction findImportRunByTransaction;
    private final ImportRunWebMapper mapper;

    @PostMapping("/statement/preview")
    @ApiErrorCodes(catalog = DomainError.class, value = {"invalid_file", "parse_failed", "business_rule_violation"})
    public ResponseEntity<ApiResponse<StatementPreviewResponse>> previewPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") FileType fileType,
            @RequestHeader("X-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        StatementPreviewResponse response = statementService.previewPdf(file, fileType, userId);
        return ResponseEntity.ok(ApiResponse.ok("Preview generated", response));
    }

    @PostMapping("/statement/confirm")
    @ApiErrorCodes(catalog = DomainError.class, value = {"resource_not_found", "business_rule_violation", "downstream_error", "duplicate_import"})
    public ResponseEntity<ApiResponse<StatementConfirmResponse>> confirmPdf(
            @RequestBody StatementConfirmRequest request,
            @RequestHeader("X-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        String bankNum = request.getBankNumber() != null ? request.getBankNumber() : "011";
        String cbuStr = request.getAccountCbu() != null ? request.getAccountCbu() : "0110000000000000000001";

        ConfirmImportCommand command = new ConfirmImportCommand(
                new UserId(userId),
                request.getTempKey(),
                request.getFileType() != null ? request.getFileType() : FileType.BANK_PDF,
                new BankNumber(bankNum),
                new Cbu(cbuStr),
                request.getAccountId(),
                null,
                request.getMappings()
        );

        ImportRun run = confirmImport.execute(command);

        StatementConfirmResponse response = StatementConfirmResponse.builder()
                .importId(run.id().value())
                .status(ImportStatus.valueOf(run.status().name()))
                .importedCount(run.importedCount())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Import completed", response));
    }

    @PostMapping("/csv/preview")
    @ApiErrorCodes(catalog = DomainError.class, value = {"invalid_file", "parse_failed", "business_rule_violation"})
    public ResponseEntity<ApiResponse<CsvPreviewResponse>> previewCsv(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        CsvPreviewResponse response = statementService.previewCsv(file, userId);
        return ResponseEntity.ok(ApiResponse.ok("CSV preview generated", response));
    }

    @PostMapping("/csv/confirm")
    @ApiErrorCodes(catalog = DomainError.class, value = {"resource_not_found", "business_rule_violation", "downstream_error", "duplicate_import"})
    public ResponseEntity<ApiResponse<CsvImportResponse>> confirmCsv(
            @RequestBody CsvConfirmRequest request,
            @RequestHeader("X-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        String bankNum = request.getBankNumber() != null ? request.getBankNumber() : "011";
        String cbuStr = request.getAccountCbu() != null ? request.getAccountCbu() : "0110000000000000000001";

        AmountMapping amountMapping;
        if (request.getMontoCol() != null) {
            amountMapping = new SingleSignedColumn(request.getMontoCol());
        } else {
            amountMapping = new SeparateDebitCredit(request.getDebitCol(), request.getCreditCol());
        }

        ColumnMapping columnMapping = new ColumnMapping(
                request.getDateCol(),
                request.getDescCol(),
                amountMapping,
                request.getBalanceCol(),
                request.getDateFormat() != null ? request.getDateFormat() : "MM/dd/yy"
        );

        ConfirmImportCommand command = new ConfirmImportCommand(
                new UserId(userId),
                request.getTempKey(),
                FileType.CSV,
                new BankNumber(bankNum),
                new Cbu(cbuStr),
                request.getAccountId(),
                columnMapping,
                request.getMappings()
        );

        ImportRun run = confirmImport.execute(command);

        CsvImportResponse response = CsvImportResponse.builder()
                .importId(run.id().value())
                .status(ImportStatus.valueOf(run.status().name()))
                .importedCount(run.importedCount())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("CSV import completed", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ImportRunResponse>>> getHistory(
            @RequestHeader("X-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        List<ImportRunResponse> response = listImportRuns.execute(new UserId(userId)).stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok("History retrieved", response));
    }

    @PostMapping("/runs/{id}/undo")
    @ApiErrorCodes(catalog = DomainError.class, value = {"resource_not_found", "import_already_undone", "business_rule_violation"})
    public ResponseEntity<ApiResponse<UndoResultResponse>> undo(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        UndoImport.UndoResult result = undoImport.execute(new UndoImportCommand(new UserId(userId), new ImportRunId(id)));

        UndoResultResponse response = UndoResultResponse.builder()
                .deletedCount(result.deletedCount())
                .skippedCount(result.skippedCount())
                .skippedTransactionIds(result.skippedTransactionIds())
                .build();

        return ResponseEntity.ok(ApiResponse.ok("Import undone", response));
    }

    @GetMapping("/runs/{id}")
    @ApiErrorCodes(catalog = DomainError.class, value = {"resource_not_found"})
    public ResponseEntity<ApiResponse<ImportRunResponse>> getRun(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") String userIdHeader) {

        Long userId = Long.valueOf(userIdHeader);
        ImportRun run = getImportRun.execute(new UserId(userId), new ImportRunId(id))
                .orElseThrow(() -> new ImportRunNotFoundException(id));

        return ResponseEntity.ok(ApiResponse.ok("Import run detail", mapper.toResponse(run)));
    }

    @GetMapping("/runs/by-transaction/{transactionId}")
    @ApiErrorCodes(catalog = DomainError.class, value = {"resource_not_found"})
    public ResponseEntity<ApiResponse<ImportRunResponse>> getRunByTransaction(
            @PathVariable("transactionId") Long transactionId) {

        ImportRun run = findImportRunByTransaction.execute(transactionId)
                .orElseThrow(() -> new ImportRunNotFoundException(transactionId));

        return ResponseEntity.ok(ApiResponse.ok("Import run for transaction", mapper.toResponse(run)));
    }
}
