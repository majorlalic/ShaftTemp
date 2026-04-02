package com.example.demo.controller;

import com.example.demo.service.MonitorDocService;
import com.example.demo.service.QueryService;
import com.example.demo.vo.RestObject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonitorController {

    private final MonitorDocService monitorDocService;
    private final QueryService queryService;

    public MonitorController(MonitorDocService monitorDocService, QueryService queryService) {
        this.monitorDocService = monitorDocService;
        this.queryService = queryService;
    }

    @GetMapping("/api/monitor/{id}")
    public RestObject<Map<String, Object>> detail(@PathVariable Long id) {
        return RestObject.newOk(monitorDocService.detail(id));
    }

    @GetMapping("/api/monitor/statistics")
    public RestObject<Map<String, Object>> statistics() {
        return RestObject.newOk(monitorDocService.statistics());
    }

    @GetMapping("/api/raw-data")
    public RestObject<List<Map<String, Object>>> listRawData(
        @RequestParam(required = false) Long monitorId,
        @RequestParam(required = false) Long deviceId,
        @RequestParam(required = false) Long shaftFloorId,
        @RequestParam(required = false) Integer partitionId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @RequestParam(required = false) Integer limit
    ) {
        return RestObject.newOk(
            queryService.listRawData(monitorId, deviceId, shaftFloorId, partitionId, from, to, limit)
        );
    }
}
