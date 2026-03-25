package com.example.demo.persistence;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.AppProperties;
import com.example.demo.persistence.repository.RawDataTableManager;
import com.example.demo.persistence.service.RawDataCleanupTask;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RawDataCleanupTaskTest {

    @Mock
    private RawDataTableManager rawDataTableManager;

    @Test
    void shouldSkipWhenCleanupDisabled() {
        AppProperties properties = new AppProperties();
        properties.getRawData().setCleanupEnabled(false);
        RawDataCleanupTask task = new RawDataCleanupTask(properties, rawDataTableManager);

        task.cleanup();

        verify(rawDataTableManager, never()).listExistingMonths();
    }

    @Test
    void shouldDropOnlyExpiredMonthTables() {
        AppProperties properties = new AppProperties();
        properties.getRawData().setCleanupEnabled(true);
        properties.getRawData().setRetentionDays(365);
        when(rawDataTableManager.listExistingMonths()).thenReturn(
            Arrays.asList(YearMonth.of(2025, 2), YearMonth.of(2025, 3), YearMonth.of(2026, 3))
        );
        RawDataCleanupTask task = new RawDataCleanupTask(properties, rawDataTableManager) {
            @Override
            protected LocalDate today() {
                return LocalDate.of(2026, 3, 25);
            }
        };

        task.cleanup();

        verify(rawDataTableManager).dropMonthTable(YearMonth.of(2025, 2));
        verify(rawDataTableManager, never()).dropMonthTable(YearMonth.of(2025, 3));
        verify(rawDataTableManager, never()).dropMonthTable(YearMonth.of(2026, 3));
    }
}
