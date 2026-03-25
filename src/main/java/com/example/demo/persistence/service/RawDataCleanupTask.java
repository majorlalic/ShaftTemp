package com.example.demo.persistence.service;

import com.example.demo.AppProperties;
import com.example.demo.persistence.repository.RawDataTableManager;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RawDataCleanupTask {

    private final AppProperties appProperties;
    private final RawDataTableManager rawDataTableManager;

    public RawDataCleanupTask(AppProperties appProperties, RawDataTableManager rawDataTableManager) {
        this.appProperties = appProperties;
        this.rawDataTableManager = rawDataTableManager;
    }

    @Scheduled(fixedDelayString = "${shaft.raw-data.cleanup-fixed-delay-ms:86400000}")
    public void cleanup() {
        if (!appProperties.getRawData().isCleanupEnabled()) {
            return;
        }
        int retentionDays = Math.max(1, appProperties.getRawData().getRetentionDays());
        YearMonth cutoffMonth = YearMonth.from(today().minusDays(retentionDays));
        List<YearMonth> existingMonths = rawDataTableManager.listExistingMonths();
        for (YearMonth month : existingMonths) {
            if (month.isBefore(cutoffMonth)) {
                rawDataTableManager.dropMonthTable(month);
            }
        }
    }

    protected LocalDate today() {
        return LocalDate.now();
    }
}
