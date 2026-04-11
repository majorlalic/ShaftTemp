package com.example.demo.controller;

import com.example.demo.service.MonitorDocService;
import com.example.demo.service.QueryService;
import com.example.demo.vo.RestObject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shaft/monitor")
public class MonitorController {

    private final MonitorDocService monitorDocService;
    private final QueryService queryService;

    public MonitorController(MonitorDocService monitorDocService, QueryService queryService) {
        this.monitorDocService = monitorDocService;
        this.queryService = queryService;
    }

    @GetMapping("/detail/{id}")
    public RestObject<Map<String, Object>> detail(@PathVariable Long id) {
        return RestObject.newOk(monitorDocService.detail(id));
    }

    @GetMapping("/statistics")
    public RestObject<Map<String, Object>> statistics(
        @RequestParam(required = false) Long areaTreeId
    ) {
        return RestObject.newOk(monitorDocService.statistics(areaTreeId));
    }

    @GetMapping("/list")
    public RestObject<Map<String, Object>> list(
        @RequestParam(required = false) Long areaTreeId
    ) {
        return RestObject.newOk(monitorDocService.listByAreaTree(areaTreeId));
    }

    @GetMapping("/raw-data")
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
