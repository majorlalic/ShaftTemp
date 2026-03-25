package com.example.demo.query.api;

import com.example.demo.query.QueryService;
import com.example.demo.web.ApiResponse;
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
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listEvents(
        @RequestParam(required = false) Long alarmId,
        @RequestParam(required = false) Long shaftFloorId,
        @RequestParam(required = false) String partitionCode
    ) {
        return ResponseEntity.ok(ApiResponse.success(queryService.listEvents(alarmId, shaftFloorId, partitionCode)));
    }

    @GetMapping("/raw-data")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listRawData(
        @RequestParam(required = false) Long monitorId,
        @RequestParam(required = false) Long deviceId,
        @RequestParam(required = false) Long shaftFloorId,
        @RequestParam(required = false) String partitionCode,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            queryService.listRawData(monitorId, deviceId, shaftFloorId, partitionCode, from, to, limit)
        ));
    }

    @GetMapping("/temp-stats/minute")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listTempStats(
        @RequestParam(required = false) Long monitorId,
        @RequestParam(required = false) Long deviceId,
        @RequestParam(required = false) Long shaftFloorId,
        @RequestParam(required = false) String partitionCode,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(
            queryService.listTempStats(monitorId, deviceId, shaftFloorId, partitionCode, from, to, limit)
        ));
    }

    @GetMapping("/devices/{deviceId}/realtime")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRealtime(@PathVariable Long deviceId) {
        return ResponseEntity.ok(ApiResponse.success(queryService.getRealtimeDeviceState(deviceId)));
    }
}
