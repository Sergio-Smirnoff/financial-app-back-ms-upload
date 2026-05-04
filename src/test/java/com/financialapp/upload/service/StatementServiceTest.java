package com.financialapp.upload.service;

import com.financialapp.upload.client.FinancesClient;
import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.dto.request.CsvConfirmRequest;
import com.financialapp.upload.model.dto.request.StatementConfirmRequest;
import com.financialapp.upload.model.dto.request.TransactionMappingRequest;
import com.financialapp.upload.model.dto.response.CsvImportResponse;
import com.financialapp.upload.model.dto.response.StatementConfirmResponse;
import com.financialapp.upload.model.entity.StatementImport;
import com.financialapp.upload.model.enums.FileType;
import com.financialapp.upload.model.enums.ImportStatus;
import com.financialapp.upload.model.enums.TransactionType;
import com.financialapp.upload.repository.StatementImportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock
    private MinioStorageService storageService;
    @Mock
    private ParsingService parsingService;
    @Mock
    private FinancesClient financesClient;
    @Mock
    private StatementImportRepository repository;

    @InjectMocks
    private StatementService statementService;

    @Test
    void confirmPdf_withMappings_shouldUseMappings() {
        // Arrange
        Long userId = 1L;
        Long accountId = 10L;
        Long customCategoryId = 500L;
        
        TransactionMappingRequest mapping = TransactionMappingRequest.builder()
                .date(LocalDate.now())
                .description("Test mapped")
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .type(TransactionType.EXPENSE)
                .categoryId(customCategoryId)
                .build();

        StatementConfirmRequest request = new StatementConfirmRequest();
        request.setAccountId(accountId);
        request.setFileType(FileType.VISA_ICBC);
        request.setTempKey("temp/test.pdf");
        request.setMappings(List.of(mapping));

        when(repository.save(any())).thenAnswer(invocation -> {
            StatementImport si = invocation.getArgument(0);
            si.setId(1L);
            return si;
        });

        // Act
        StatementConfirmResponse response = statementService.confirmPdf(request, userId);

        // Assert
        assertEquals(1, response.getImportedCount());
        assertEquals(ImportStatus.COMPLETED, response.getStatus());
        
        verify(financesClient, times(1)).createTransaction(eq(userId), argThat(tx -> 
            tx.getCategoryId().equals(customCategoryId) && 
            tx.getDescription().equals("Test mapped") &&
            tx.getAccountId().equals(accountId)
        ));
        verify(repository, times(1)).save(any());
    }

    @Test
    void confirmPdf_withoutMappings_shouldUseFallbackLogic() {
        // Arrange
        Long userId = 1L;
        Long accountId = 10L;
        
        StatementConfirmRequest request = new StatementConfirmRequest();
        request.setAccountId(accountId);
        request.setFileType(FileType.VISA_ICBC);
        request.setTempKey("temp/test.pdf");
        request.setMappings(null); // Trigger fallback

        ParsedTransaction pt1 = ParsedTransaction.builder()
                .date(LocalDate.now())
                .description("Expense item")
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .type(TransactionType.EXPENSE)
                .build();

        ParsedTransaction pt2 = ParsedTransaction.builder()
                .date(LocalDate.now())
                .description("Income item")
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .type(TransactionType.INCOME)
                .build();

        when(storageService.retrieve(any())).thenReturn(new ByteArrayInputStream("fake".getBytes()));
        when(parsingService.parse(any(), any(), any())).thenReturn(List.of(pt1, pt2));

        when(repository.save(any())).thenAnswer(invocation -> {
            StatementImport si = invocation.getArgument(0);
            si.setId(1L);
            return si;
        });

        // Act
        StatementConfirmResponse response = statementService.confirmPdf(request, userId);

        // Assert
        assertEquals(2, response.getImportedCount());
        
        // Verify EXPENSE uses 1104
        verify(financesClient).createTransaction(eq(userId), argThat(tx -> 
            tx.getCategoryId().equals(1104L) && tx.getDescription().equals("Expense item")
        ));
        
        // Verify INCOME uses 1105
        verify(financesClient).createTransaction(eq(userId), argThat(tx -> 
            tx.getCategoryId().equals(1105L) && tx.getDescription().equals("Income item")
        ));
        
        verify(repository, times(1)).save(any());
    }

    @Test
    void confirmCsv_withMappings_shouldUseMappings() {
        // Arrange
        Long userId = 1L;
        Long accountId = 10L;
        Long customCategoryId = 600L;

        TransactionMappingRequest mapping = TransactionMappingRequest.builder()
                .date(LocalDate.now())
                .description("CSV mapped")
                .amount(new BigDecimal("150.00"))
                .currency("ARS")
                .type(TransactionType.EXPENSE)
                .categoryId(customCategoryId)
                .build();

        CsvConfirmRequest request = new CsvConfirmRequest();
        request.setAccountId(accountId);
        request.setFileType(FileType.CSV);
        request.setTempKey("temp/test.csv");
        request.setMappings(List.of(mapping));

        when(repository.save(any())).thenAnswer(invocation -> {
            StatementImport si = invocation.getArgument(0);
            si.setId(2L);
            return si;
        });

        // Act
        CsvImportResponse response = statementService.confirmCsv(request, userId);

        // Assert
        assertEquals(1, response.getImportedCount());
        assertEquals(ImportStatus.COMPLETED, response.getStatus());

        verify(financesClient, times(1)).createTransaction(eq(userId), argThat(tx ->
            tx.getCategoryId().equals(customCategoryId) &&
            tx.getDescription().equals("CSV mapped") &&
            tx.getAccountId().equals(accountId)
        ));
        verify(repository, times(1)).save(any());
    }
}
