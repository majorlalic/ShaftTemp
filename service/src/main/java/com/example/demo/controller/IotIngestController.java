package com.example.demo.controller;

import com.example.demo.service.ReportIngestService;
import com.example.demo.vo.PartitionAlarmRequest;
import com.example.demo.vo.PartitionMeasureRequest;
import com.example.demo.vo.RestObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shaft/iot")
public class IotIngestController {

    private final ReportIngestService reportIngestService;

    public IotIngestController(ReportIngestService reportIngestService) {
        this.reportIngestService = reportIngestService;
    }

    @PostMapping("/reports/measure")
    public RestObject<ReportIngestService.IngestResult> ingestMeasure(@Valid @RequestBody PartitionMeasureRequest request) {
        return RestObject.newOk(reportIngestService.ingestMeasure(request));
    }

    @PostMapping("/reports/alarm")
    public RestObject<ReportIngestService.IngestResult> ingestAlarm(@Valid @RequestBody PartitionAlarmRequest request) {
        return RestObject.newOk(reportIngestService.ingestAlarm(request));
    }

    @PostMapping("/data/batch")
    public RestObject<List<Map<String, Object>>> processDataBatch(
        @RequestBody List<PartitionMeasureRequest> requests
    ) {
        List<PartitionMeasureRequest> safeRequests = requests == null ? Collections.<PartitionMeasureRequest>emptyList() : requests;
        List<Map<String, Object>> payload = new ArrayList<Map<String, Object>>();
        for (PartitionMeasureRequest request : safeRequests) {
            ReportIngestService.IngestResult result = reportIngestService.processMeasureWithoutRawData(request);
            java.util.LinkedHashMap<String, Object> item = new java.util.LinkedHashMap<String, Object>();
            item.put("deviceId", result.getDeviceId());
            item.put("monitorId", result.getMonitorId());
            item.put("partitionCode", result.getPartitionCode());
            item.put("alarmCount", result.getAlarmCount());
            payload.add(item);
        }
        return RestObject.newOk(payload);
    }

    @PostMapping("/alarm/batch")
    public RestObject<String> processAlarmBatch(
        @RequestBody(required = false) List<PartitionAlarmRequest> requests
    ) {
        int size = requests == null ? 0 : requests.size();
        return RestObject.newOk("ack:" + size);
    }
}
