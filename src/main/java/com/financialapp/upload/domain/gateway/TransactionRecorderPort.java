package com.financialapp.upload.domain.gateway;

import com.financialapp.upload.model.dto.ParsedTransaction;

public interface TransactionRecorderPort {

    Long recordTransaction(Long userId, Long accountId, ParsedTransaction parsedTransaction);

    void deleteTransaction(Long userId, Long transactionId);
}
