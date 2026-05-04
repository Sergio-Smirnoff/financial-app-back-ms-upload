package com.financialapp.upload.controller;

import com.financialapp.upload.model.dto.request.ConfirmRequest;
import com.financialapp.upload.model.dto.request.ResolveRequest;
import com.financialapp.upload.model.dto.response.*;
import com.financialapp.upload.model.enums.FileType;
import com.financialapp.upload.service.ImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<PreviewResponse>> preview(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") FileType type,
            @RequestParam(required = false) Integer dateCol,
            @RequestParam(required = false) Integer descCol,
            @RequestParam(required = false) Integer expenseCol,
            @RequestParam(required = false) Integer incomeCol,
            @RequestParam(required = false) String dateFormat,
            @RequestHeader("X-User-Id") Long userId) {
        
        Map<String, String> context = new HashMap<>();
        if (dateCol != null) context.put("dateCol", dateCol.toString());
        if (descCol != null) context.put("descCol", descCol.toString());
        if (expenseCol != null) context.put("expenseCol", expenseCol.toString());
        if (incomeCol != null) context.put("incomeCol", incomeCol.toString());
        if (dateFormat != null) context.put("dateFormat", dateFormat);

        PreviewResponse response = importService.preview(file, type, userId, context);
        return ResponseEntity.ok(ApiResponse.ok("Preview generated", response));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ConfirmResponse>> confirm(
            @RequestBody ConfirmRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        
        ConfirmResponse response = importService.confirm(request, userId);
        return ResponseEntity.ok(ApiResponse.ok("Import processed", response));
    }

    @PostMapping("/duplicates/resolve")
    public ResponseEntity<ApiResponse<ResolveResponse>> resolveDuplicates(
            @RequestBody ResolveRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        
        ResolveResponse response = importService.resolveDuplicates(request, userId);
        return ResponseEntity.ok(ApiResponse.ok("Duplicates resolved", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ImportHistoryRecord>>> getHistory(
            @RequestHeader("X-User-Id") Long userId) {
        
        List<ImportHistoryRecord> history = importService.getHistory(userId);
        return ResponseEntity.ok(ApiResponse.ok("History retrieved", history));
    }
}
