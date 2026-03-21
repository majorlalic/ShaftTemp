package com.example.demo.alarm.api;

import com.example.demo.alarm.AlarmService;
import com.example.demo.query.QueryService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

    private final AlarmService alarmService;
    private final QueryService queryService;

    public AlarmController(AlarmService alarmService, QueryService queryService) {
        this.alarmService = alarmService;
        this.queryService = queryService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long monitorId,
        @RequestParam(required = false) Long deviceId,
        @RequestParam(required = false) Long shaftFloorId,
        @RequestParam(required = false) String partitionCode
    ) {
        return ResponseEntity.ok(queryService.listAlarms(status, monitorId, deviceId, shaftFloorId, partitionCode));
    }

    @GetMapping("/{alarmId}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long alarmId) {
        return ResponseEntity.ok(queryService.getAlarmDetail(alarmId));
    }

    @PostMapping("/{alarmId}/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@PathVariable Long alarmId, @RequestBody(required = false) AlarmActionRequest request) {
        AlarmActionRequest payload = request == null ? new AlarmActionRequest() : request;
        alarmService.confirm(alarmId, payload.getHandler(), payload.getRemark());
        return ResponseEntity.ok(queryService.getAlarmDetail(alarmId));
    }

    @PostMapping("/{alarmId}/observe")
    public ResponseEntity<Map<String, Object>> observe(@PathVariable Long alarmId, @RequestBody(required = false) AlarmActionRequest request) {
        AlarmActionRequest payload = request == null ? new AlarmActionRequest() : request;
        alarmService.observe(alarmId, payload.getRemark());
        return ResponseEntity.ok(queryService.getAlarmDetail(alarmId));
    }

    @PostMapping("/{alarmId}/false-positive")
    public ResponseEntity<Map<String, Object>> falsePositive(@PathVariable Long alarmId, @RequestBody(required = false) AlarmActionRequest request) {
        AlarmActionRequest payload = request == null ? new AlarmActionRequest() : request;
        alarmService.markFalsePositive(alarmId, payload.getRemark());
        return ResponseEntity.ok(queryService.getAlarmDetail(alarmId));
    }

    @PostMapping("/{alarmId}/close")
    public ResponseEntity<Map<String, Object>> close(@PathVariable Long alarmId, @RequestBody(required = false) AlarmActionRequest request) {
        AlarmActionRequest payload = request == null ? new AlarmActionRequest() : request;
        alarmService.close(alarmId, payload.getRemark());
        return ResponseEntity.ok(queryService.getAlarmDetail(alarmId));
    }
}
