package com.example.demo.ingest.api;

import com.example.demo.ingest.dto.TemperatureReportRequest;
import com.example.demo.ingest.service.ReportIngestService;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class IngestDebugController {

    private final ReportIngestService reportIngestService;

    public IngestDebugController(ReportIngestService reportIngestService) {
        this.reportIngestService = reportIngestService;
    }

    @PostMapping
    public ResponseEntity<ReportIngestService.IngestResult> ingest(@Valid @RequestBody TemperatureReportRequest request) {
        return ResponseEntity.ok(reportIngestService.ingest(request));
    }
}
