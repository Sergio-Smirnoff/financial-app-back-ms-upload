package com.financialapp.upload.infrastructure.gateway;

import com.financialapp.commons.core.response.ApiResponse;
import com.financialapp.upload.client.FinancesClient;
import com.financialapp.upload.domain.gateway.TransactionRecorderPort;
import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.dto.request.TransactionRequest;
import com.financialapp.upload.model.dto.response.CreatedTransactionResponse;
import com.financialapp.upload.model.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionRecorderPortImpl implements TransactionRecorderPort {

    private final FinancesClient financesClient;

    @Override
    public Long recordTransaction(Long userId, Long accountId, ParsedTransaction parsedTransaction) {
        Long defaultCategoryId = parsedTransaction.getType() == TransactionType.INCOME ? 1105L : 1104L;
        TransactionRequest req = TransactionRequest.builder()
                .type(parsedTransaction.getType())
                .amount(parsedTransaction.getAmount())
                .accountId(accountId)
                .currency(parsedTransaction.getCurrency() != null ? parsedTransaction.getCurrency() : "ARS")
                .categoryId(defaultCategoryId)
                .description(parsedTransaction.getDescription())
                .date(parsedTransaction.getDate())
                .build();

        ApiResponse<CreatedTransactionResponse> response = financesClient.createTransaction(userId, req);
        if (response != null && response.getData() != null && response.getData().getId() != null) {
            return response.getData().getId();
        }
        return null;
    }

    @Override
    public void deleteTransaction(Long userId, Long transactionId) {
        financesClient.deleteTransaction(transactionId, userId);
    }
}
