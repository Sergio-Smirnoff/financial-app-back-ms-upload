package com.financialapp.upload.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.financialapp.commons.core.response.ApiResponse;
import com.financialapp.commons.web.error.ApiExceptionHandler;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler extends ApiExceptionHandler {

    private final ObjectMapper objectMapper;

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiResponse.failure(
                HttpStatus.PAYLOAD_TOO_LARGE, DomainError.FILE_TOO_LARGE.code(),
                "File too large. Maximum 20MB allowed.", null));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException ex) {
        log.warn("Feign call failed: status={}, message={}", ex.status(), ex.getMessage());
        String message = "Communication error between services";
        String code = DomainError.DOWNSTREAM_ERROR.code();
        try {
            JsonNode body = objectMapper.readTree(ex.contentUTF8());
            if (body.has("message")) message = body.get("message").asText();
            if (body.has("code")) code = body.get("code").asText();
        } catch (Exception parseFailure) {
            log.debug("Downstream error body not parseable: {}", parseFailure.getMessage());
        }
        HttpStatus status = ex.status() > 0 ? HttpStatus.valueOf(ex.status()) : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(ApiResponse.failure(status, code, message, null));
    }

    @ExceptionHandler(com.financialapp.upload.domain.exception.DuplicateImportException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateImport(com.financialapp.upload.domain.exception.DuplicateImportException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(
                HttpStatus.CONFLICT, "duplicate_import", ex.getMessage(), null));
    }

    @ExceptionHandler(com.financialapp.upload.domain.exception.ImportAlreadyUndoneException.class)
    public ResponseEntity<ApiResponse<Void>> handleImportAlreadyUndone(com.financialapp.upload.domain.exception.ImportAlreadyUndoneException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.failure(
                HttpStatus.CONFLICT, "import_already_undone", ex.getMessage(), null));
    }

    @ExceptionHandler(com.financialapp.upload.domain.exception.ImportRunNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleImportRunNotFound(com.financialapp.upload.domain.exception.ImportRunNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(
                HttpStatus.NOT_FOUND, "resource_not_found", ex.getMessage(), null));
    }

    @ExceptionHandler(com.financialapp.upload.domain.exception.DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(com.financialapp.upload.domain.exception.DomainException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(
                HttpStatus.BAD_REQUEST, "business_rule_violation", ex.getMessage(), null));
    }
}
