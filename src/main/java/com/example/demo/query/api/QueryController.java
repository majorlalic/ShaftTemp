package com.example.demo.query.api;

import com.example.demo.query.QueryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/events")
    public ResponseEntity<List<Map<String, Object>>> listEvents(@RequestParam(required = false) Long alarmId) {
        return ResponseEntity.ok(queryService.listEvents(alarmId));
    }

    @GetMapping("/raw-data")
    public ResponseEntity<List<Map<String, Object>>> listRawData(
        @RequestParam(required = false) Long monitorId,
        @RequestParam(required = false) Long deviceId,
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(queryService.listRawData(monitorId, deviceId, limit));
    }

    @GetMapping("/temp-stats/minute")
    public ResponseEntity<List<Map<String, Object>>> listTempStats(
        @RequestParam(required = false) Long monitorId,
        @RequestParam(required = false) Long deviceId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(queryService.listTempStats(monitorId, deviceId, from, to, limit));
    }

    @GetMapping("/devices/{deviceId}/realtime")
    public ResponseEntity<Map<String, Object>> getRealtime(@PathVariable Long deviceId) {
        return ResponseEntity.ok(queryService.getRealtimeDeviceState(deviceId));
    }
}
