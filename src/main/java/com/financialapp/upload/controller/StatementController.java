package com.financialapp.upload.controller;

import com.financialapp.upload.model.dto.request.CsvConfirmRequest;
import com.financialapp.upload.model.dto.request.StatementConfirmRequest;
import com.financialapp.upload.model.dto.response.*;
import com.financialapp.upload.model.enums.FileType;
import com.financialapp.upload.service.StatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;

    @PostMapping("/statement/preview")
    public ResponseEntity<ApiResponse<StatementPreviewResponse>> previewPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") FileType fileType,
            @RequestHeader("X-User-Id") String userIdHeader) {
        
        Long userId = Long.valueOf(userIdHeader);
        StatementPreviewResponse response = statementService.previewPdf(file, fileType, userId);
        return ResponseEntity.ok(ApiResponse.ok("Preview generated", response));
    }

    @PostMapping("/statement/confirm")
    public ResponseEntity<ApiResponse<StatementConfirmResponse>> confirmPdf(
            @RequestBody StatementConfirmRequest request,
            @RequestHeader("X-User-Id") String userIdHeader) {
        
        Long userId = Long.valueOf(userIdHeader);
        StatementConfirmResponse response = statementService.confirmPdf(request, userId);
        return ResponseEntity.ok(ApiResponse.ok("Import completed", response));
    }

    @PostMapping("/csv/preview")
    public ResponseEntity<ApiResponse<CsvPreviewResponse>> previewCsv(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-User-Id") String userIdHeader) {
        
        Long userId = Long.valueOf(userIdHeader);
        CsvPreviewResponse response = statementService.previewCsv(file, userId);
        return ResponseEntity.ok(ApiResponse.ok("CSV preview generated", response));
    }

    @PostMapping("/csv/confirm")
    public ResponseEntity<ApiResponse<CsvImportResponse>> confirmCsv(
            @RequestBody CsvConfirmRequest request,
            @RequestHeader("X-User-Id") String userIdHeader) {
        
        Long userId = Long.valueOf(userIdHeader);
        CsvImportResponse response = statementService.confirmCsv(request, userId);
        return ResponseEntity.ok(ApiResponse.ok("CSV import completed", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<java.util.List<com.financialapp.upload.model.entity.StatementImport>>> getHistory(
            @RequestHeader("X-User-Id") String userIdHeader) {
        
        Long userId = Long.valueOf(userIdHeader);
        java.util.List<com.financialapp.upload.model.entity.StatementImport> history = statementService.getHistory(userId);
        return ResponseEntity.ok(ApiResponse.ok("History retrieved", history));
    }
}
