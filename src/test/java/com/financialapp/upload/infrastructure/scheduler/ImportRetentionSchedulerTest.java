package com.financialapp.upload.infrastructure.scheduler;

import com.financialapp.upload.domain.gateway.StatementStoragePort;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ImportRetentionSchedulerTest {

    @Test
    void shouldTriggerDeleteOlderThanThirtyDaysOnSweep() {
        StatementStoragePort storagePort = mock(StatementStoragePort.class);
        ImportRetentionScheduler scheduler = new ImportRetentionScheduler(storagePort);

        scheduler.runRetentionSweep();

        verify(storagePort).deleteOlderThan("imports/", Duration.ofDays(30));
    }
}
