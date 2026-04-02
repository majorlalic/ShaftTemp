package com.example.demo.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.demo.service.ReportIngestService;
import org.junit.jupiter.api.Test;

class ReportIngestServiceTest {

    @Test
    void shouldExposePartitionCodeInIngestResult() {
        ReportIngestService.IngestResult result = new ReportIngestService.IngestResult(
            1L,
            2L,
            "dev_TMP_th01",
            3L,
            1,
            new ReportIngestService.ReportMetrics(null, null, null)
        );

        assertEquals("dev_TMP_th01", result.getPartitionCode());
        assertEquals(3L, result.getRawDataId().longValue());
    }
}
