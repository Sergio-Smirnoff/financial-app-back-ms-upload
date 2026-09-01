package com.financialapp.upload.infrastructure.scheduler;

import com.financialapp.upload.domain.event.ImportStaleEvent;
import com.financialapp.upload.domain.gateway.DomainEventPublisher;
import com.financialapp.upload.domain.model.importrun.ImportRun;
import com.financialapp.upload.domain.repository.ImportRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImportStalenessScheduler {

    private final ImportRunRepository importRunRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Scheduled(cron = "${upload.scheduler.import-staleness.cron:0 30 8 * * *}")
    @Transactional
    public void evaluateImportStaleness() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Running ImportStalenessScheduler at {}", now);

        List<ImportRun> runs = importRunRepository.findAllCompletedOrPartial();
        if (runs.isEmpty()) {
            return;
        }

        Map<AccountKey, List<ImportRun>> grouped = runs.stream()
                .collect(Collectors.groupingBy(r -> new AccountKey(r.userId().value(), r.accountCbu().value())));

        for (List<ImportRun> accountRuns : grouped.values()) {
            accountRuns.stream()
                    .max(Comparator.comparing(ImportRun::createdAt))
                    .ifPresent(latestRun -> {
                        long daysSinceImport = ChronoUnit.DAYS.between(latestRun.createdAt(), now);
                        if (daysSinceImport > 30) {
                            boolean unalerted = latestRun.lastStaleAlertAt() == null
                                    || latestRun.lastStaleAlertAt().isBefore(latestRun.createdAt());
                            if (unalerted) {
                                log.info("Import stale detected for userId={}, accountCbu={}, daysSinceImport={}",
                                        latestRun.userId().value(), latestRun.accountCbu().value(), daysSinceImport);

                                ImportRun updated = latestRun.markStaleAlerted(now);
                                importRunRepository.save(updated);

                                ImportStaleEvent event = new ImportStaleEvent(
                                        latestRun.id(),
                                        latestRun.userId(),
                                        latestRun.accountCbu(),
                                        latestRun.bankNumber(),
                                        (int) daysSinceImport
                                );
                                domainEventPublisher.publish(event);
                            }
                        }
                    });
        }
    }

    private record AccountKey(Long userId, String accountCbu) {}
}
