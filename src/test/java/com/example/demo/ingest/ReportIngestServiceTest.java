package com.example.demo.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.demo.ingest.service.ReportIngestService;
import java.math.BigDecimal;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ReportIngestServiceTest {

    @Test
    void shouldCalculateMetrics() {
        ReportIngestService.ReportMetrics metrics = new ReportIngestService(
            null, null, null, null, null, null, null, null, null
        ).calculateMetrics(Arrays.asList(
            new BigDecimal("10"),
            new BigDecimal("20"),
            new BigDecimal("30")
        ));

        assertEquals(new BigDecimal("30"), metrics.getMaxTemp());
        assertEquals(new BigDecimal("10"), metrics.getMinTemp());
        assertEquals(new BigDecimal("20.00"), metrics.getAvgTemp());
    }
}
