package com.financialapp.upload.application.importrun;
import com.financialapp.commons.core.domain.model.Cbu;

import com.financialapp.upload.application.importrun.impl.ConfirmImportUseCaseImpl;
import com.financialapp.upload.domain.common.model.*;
import com.financialapp.upload.domain.exception.DuplicateImportException;
import com.financialapp.upload.domain.gateway.StatementParserPort;
import com.financialapp.upload.domain.gateway.StatementStoragePort;
import com.financialapp.upload.domain.gateway.TransactionRecorderPort;
import com.financialapp.upload.domain.model.importrun.*;
import com.financialapp.upload.domain.model.mapping.ColumnMapping;
import com.financialapp.upload.domain.model.mapping.SeparateDebitCredit;
import com.financialapp.upload.domain.repository.ImportRunRepository;
import com.financialapp.upload.domain.usecase.importrun.command.ConfirmImportCommand;
import com.financialapp.upload.model.dto.ParsedTransaction;
import com.financialapp.upload.model.enums.FileType;
import com.financialapp.upload.model.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmImportUseCaseTest {

    @Mock
    private ImportRunRepository importRunRepository;
    @Mock
    private StatementParserPort parserPort;
    @Mock
    private StatementStoragePort storagePort;
    @Mock
    private TransactionRecorderPort transactionRecorderPort;

    private ConfirmImportUseCaseImpl useCase;

    private final UserId userId = new UserId(1L);
    private final BankNumber bankNumber = new BankNumber("011");
    private final Cbu cbu = new Cbu("0110000000000000000001");
    private final String tempKey = "temp/uuid-123/file.csv";

    @BeforeEach
    void setUp() {
        useCase = new ConfirmImportUseCaseImpl(importRunRepository, parserPort, storagePort, transactionRecorderPort);
    }

    @Test
    void shouldRejectDuplicateImportForSameUser() {
        byte[] fileBytes = "test".getBytes(StandardCharsets.UTF_8);
        when(storagePort.retrieve(tempKey)).thenReturn(new ByteArrayInputStream(fileBytes));

        FileHash expectedHash = FileHash.ofBytes(fileBytes);
        when(importRunRepository.existsActiveByUserAndFileHash(userId, expectedHash)).thenReturn(true);

        ConfirmImportCommand command = new ConfirmImportCommand(
                userId, tempKey, FileType.CSV, bankNumber, cbu, 10L, null, null
        );

        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(DuplicateImportException.class)
                .hasMessageContaining(expectedHash.value());

        verify(storagePort, never()).move(any(), any());
    }

    @Test
    void shouldConfirmImportAndMoveFileOnSuccess() {
        byte[] fileBytes = "test".getBytes(StandardCharsets.UTF_8);
        when(storagePort.retrieve(tempKey)).thenReturn(new ByteArrayInputStream(fileBytes));

        FileHash expectedHash = FileHash.ofBytes(fileBytes);
        when(importRunRepository.existsActiveByUserAndFileHash(userId, expectedHash)).thenReturn(false);

        ParsedTransaction pt1 = ParsedTransaction.builder()
                .date(LocalDate.of(2026, 1, 15))
                .description("Supermarket")
                .amount(new BigDecimal("100.00"))
                .type(TransactionType.EXPENSE)
                .runningBalance(new BigDecimal("900.00"))
                .build();

        ColumnMapping mapping = new ColumnMapping(0, 1, new SeparateDebitCredit(2, 3), 4, "yyyy-MM-dd");
        when(parserPort.parse(any(), eq(FileType.CSV), eq(mapping))).thenReturn(List.of(pt1));

        when(importRunRepository.save(any(ImportRun.class))).thenAnswer(invocation -> {
            ImportRun run = invocation.getArgument(0);
            if (run.id() == null) {
                return ImportRun.reconstitute(
                        new ImportRunId(99L), run.userId(), run.bankNumber(), run.accountCbu(),
                        run.fileHash(), run.period(), run.status(), run.createdTransactionIds(),
                        run.importedCount(), run.skippedCount(), run.reconciliation(),
                        run.lastStaleAlertAt(), run.createdAt()
                );
            }
            return run;
        });

        when(transactionRecorderPort.recordTransaction(eq(1L), eq(10L), eq(pt1))).thenReturn(501L);

        ConfirmImportCommand command = new ConfirmImportCommand(
                userId, tempKey, FileType.CSV, bankNumber, cbu, 10L, mapping, null
        );

        ImportRun result = useCase.execute(command);

        assertThat(result.status()).isEqualTo(ImportRunStatus.COMPLETED);
        assertThat(result.createdTransactionIds()).containsExactly(501L);
        assertThat(result.reconciliation()).isNotNull();
        assertThat(result.reconciliation().statementBalance().amount()).isEqualByComparingTo("900.00");

        verify(storagePort).move(eq(tempKey), eq("imports/1/99/statement"));
    }
}
