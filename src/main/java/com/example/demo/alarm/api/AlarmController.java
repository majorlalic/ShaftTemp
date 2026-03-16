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
        @RequestParam(required = false) Long deviceId
    ) {
        return ResponseEntity.ok(queryService.listAlarms(status, monitorId, deviceId));
    }

    @GetMapping("/{alarmId}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long alarmId) {
        return ResponseEntity.ok(queryService.getAlarmDetail(alarmId));
    }

    @PostMapping("/{alarmId}/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@PathVariable Long alarmId, @RequestBody(required = false) AlarmActionRequest request) {
        AlarmActionRequest payload = request == null ? new AlarmActionRequest() : request;
        alarmService.confirm(alarmId, payload.getUserId(), payload.getRemark());
        return ResponseEntity.ok(queryService.getAlarmDetail(alarmId));
    }

    @PostMapping("/{alarmId}/close")
    public ResponseEntity<Map<String, Object>> close(@PathVariable Long alarmId, @RequestBody(required = false) AlarmActionRequest request) {
        AlarmActionRequest payload = request == null ? new AlarmActionRequest() : request;
        alarmService.close(alarmId, payload.getRemark());
        return ResponseEntity.ok(queryService.getAlarmDetail(alarmId));
    }
}
