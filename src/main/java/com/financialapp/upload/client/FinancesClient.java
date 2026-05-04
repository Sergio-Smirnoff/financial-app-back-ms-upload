package com.financialapp.upload.client;

import com.financialapp.upload.model.dto.request.TransactionRequest;
import com.financialapp.upload.model.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "ms-finances", url = "${FINANCES_SERVICE_URL:http://localhost:8082}")
public interface FinancesClient {

    @PostMapping("/api/v1/finances/transactions")
    ApiResponse<Void> createTransaction(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("bypassBalance") boolean bypassBalance,
            @RequestBody TransactionRequest request);

    @PostMapping("/api/v1/finances/transactions/duplicates-check")
    ApiResponse<List<Integer>> checkDuplicates(
            @RequestBody List<TransactionRequest> transactions);
}
