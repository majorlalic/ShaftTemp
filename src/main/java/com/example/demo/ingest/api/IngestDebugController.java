package com.example.demo.ingest.api;

import com.example.demo.ingest.dto.PartitionAlarmRequest;
import com.example.demo.ingest.dto.PartitionMeasureRequest;
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

    @PostMapping("/measure")
    public ResponseEntity<ReportIngestService.IngestResult> ingestMeasure(@Valid @RequestBody PartitionMeasureRequest request) {
        return ResponseEntity.ok(reportIngestService.ingestMeasure(request));
    }

    @PostMapping("/alarm")
    public ResponseEntity<ReportIngestService.IngestResult> ingestAlarm(@Valid @RequestBody PartitionAlarmRequest request) {
        return ResponseEntity.ok(reportIngestService.ingestAlarm(request));
    }
}
