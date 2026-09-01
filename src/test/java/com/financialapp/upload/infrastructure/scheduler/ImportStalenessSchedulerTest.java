package com.financialapp.upload.infrastructure.scheduler;

import com.financialapp.commons.core.domain.model.Cbu;
import com.financialapp.upload.domain.common.model.BankNumber;
import com.financialapp.upload.domain.common.model.DateRange;
import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.domain.event.ImportStaleEvent;
import com.financialapp.upload.domain.gateway.DomainEventPublisher;
import com.financialapp.upload.domain.model.importrun.*;
import com.financialapp.upload.domain.repository.ImportRunRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImportStalenessSchedulerTest {

    private final ImportRunRepository repository = mock(ImportRunRepository.class);
    private final DomainEventPublisher publisher = mock(DomainEventPublisher.class);
    private final ImportStalenessScheduler scheduler = new ImportStalenessScheduler(repository, publisher);

    @Test
    void emitsAlertWhenImportIsOlderThan30DaysAndUnalerted() {
        LocalDateTime created = LocalDateTime.now().minusDays(35);
        ImportRun run = ImportRun.reconstitute(
                new ImportRunId(1L),
                new UserId(10L),
                new BankNumber("017"),
                new Cbu("0170099220000067797370"),
                new FileHash("a".repeat(64)),
                new DateRange(LocalDate.now().minusDays(40), LocalDate.now().minusDays(35)),
                ImportRunStatus.COMPLETED,
                List.of(100L),
                1,
                0,
                null,
                null,
                created
        );

        when(repository.findAllCompletedOrPartial()).thenReturn(List.of(run));

        scheduler.evaluateImportStaleness();

        verify(repository).save(argThat(r -> r.lastStaleAlertAt() != null));

        ArgumentCaptor<ImportStaleEvent> captor = ArgumentCaptor.forClass(ImportStaleEvent.class);
        verify(publisher).publish(captor.capture());

        ImportStaleEvent event = captor.getValue();
        assertThat(event.userId()).isEqualTo(new UserId(10L));
        assertThat(event.daysSinceImport()).isEqualTo(35);
    }

    @Test
    void skipsAlertWhenImportIsRecent() {
        LocalDateTime created = LocalDateTime.now().minusDays(10);
        ImportRun run = ImportRun.reconstitute(
                new ImportRunId(1L),
                new UserId(10L),
                new BankNumber("017"),
                new Cbu("0170099220000067797370"),
                new FileHash("a".repeat(64)),
                new DateRange(LocalDate.now().minusDays(15), LocalDate.now().minusDays(10)),
                ImportRunStatus.COMPLETED,
                List.of(100L),
                1,
                0,
                null,
                null,
                created
        );

        when(repository.findAllCompletedOrPartial()).thenReturn(List.of(run));

        scheduler.evaluateImportStaleness();

        verify(repository, never()).save(any());
        verify(publisher, never()).publish(any());
    }
}
