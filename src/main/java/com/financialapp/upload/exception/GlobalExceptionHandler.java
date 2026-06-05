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
}
