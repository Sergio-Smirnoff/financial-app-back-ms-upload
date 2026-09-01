package com.financialapp.upload.client;

import com.financialapp.commons.core.response.ApiResponse;
import com.financialapp.upload.model.dto.request.TransactionRequest;
import com.financialapp.upload.model.dto.response.CreatedTransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "ms-finances", url = "${FINANCES_SERVICE_URL:http://localhost:8082}")
public interface FinancesClient {

    @PostMapping("/api/v1/finances/transactions")
    ApiResponse<CreatedTransactionResponse> createTransaction(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody TransactionRequest request);

    @DeleteMapping("/api/v1/finances/transactions/{id}")
    ApiResponse<Void> deleteTransaction(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long userId);

    @PostMapping("/api/v1/finances/transactions/duplicates-check")
    ApiResponse<List<Integer>> checkDuplicates(
            @RequestBody List<TransactionRequest> transactions);
}
