package com.financialapp.upload.infrastructure.scheduler;

import com.financialapp.upload.domain.gateway.StatementStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportRetentionScheduler {

    private final StatementStoragePort storagePort;

    @Scheduled(cron = "0 0 3 * * *")
    public void runRetentionSweep() {
        log.info("Starting daily 30-day retention sweep for imported statements...");
        try {
            storagePort.deleteOlderThan("imports/", Duration.ofDays(30));
            log.info("Completed retention sweep.");
        } catch (Exception e) {
            log.error("Failed to execute retention sweep: {}", e.getMessage(), e);
        }
    }
}
