package com.financialapp.upload.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UndoResultResponse {
    private int deletedCount;
    private int skippedCount;
    private List<Long> skippedTransactionIds;
}
