package com.financialapp.upload.client;

import com.financialapp.commons.core.response.ApiResponse;
import com.financialapp.upload.model.dto.request.CardExpenseImportRequest;
import com.financialapp.upload.model.dto.request.CardExpenseCreateRequest;
import com.financialapp.upload.model.dto.response.BatchImportResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "ms-banks", url = "${BANKS_SERVICE_URL:http://localhost:8083}")
public interface BanksClient {

    @PostMapping("/api/v1/banks/cards/{cardId}/installments/import")
    ApiResponse<BatchImportResponse> importCardExpenses(
        @PathVariable("cardId") Long cardId,
        @RequestHeader("X-User-Id") Long userId,
        @RequestBody CardExpenseImportRequest request);

    @PostMapping("/api/v1/banks/cards/{cardId}/installments/duplicates-check")
    ApiResponse<List<Integer>> checkDuplicates(
        @PathVariable("cardId") Long cardId,
        @RequestBody List<CardExpenseCreateRequest> expenses);
}
