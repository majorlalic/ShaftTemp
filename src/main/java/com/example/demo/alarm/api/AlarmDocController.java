package com.example.demo.alarm.api;

import com.example.demo.web.ApiResponse;
import com.example.demo.web.PagePayload;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alarm")
public class AlarmDocController {

    private final AlarmViewService alarmViewService;

    public AlarmDocController(AlarmViewService alarmViewService) {
        this.alarmViewService = alarmViewService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> statistics(
        @RequestParam(required = false) Long areaId,
        @RequestParam(required = false) Long deviceId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ResponseEntity.ok(ApiResponse.success(alarmViewService.statistics(areaId, deviceId, startTime, endTime)));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PagePayload<Map<String, Object>>>> list(
        @RequestParam(required = false) Integer pageNum,
        @RequestParam(required = false) Integer pageSize,
        @RequestParam(required = false) Long areaId,
        @RequestParam(required = false) Long deviceId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
        @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success(alarmViewService.list(pageNum, pageSize, areaId, deviceId, startTime, endTime, status)));
    }

    @GetMapping("/merge/list")
    public ResponseEntity<ApiResponse<PagePayload<Map<String, Object>>>> mergeList(
        @RequestParam Long alarmId,
        @RequestParam Integer pageNum,
        @RequestParam Integer pageSize
    ) {
        return ResponseEntity.ok(ApiResponse.success(alarmViewService.mergeEvents(alarmId, pageNum, pageSize)));
    }

    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@RequestParam("id") Long alarmId) {
        return ResponseEntity.ok(ApiResponse.success(alarmViewService.detail(alarmId)));
    }

    @PostMapping("/handle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handle(@RequestBody AlarmHandleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(alarmViewService.handle(request)));
    }

    @PostMapping("/batchHandle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchHandle(@RequestBody AlarmHandleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(alarmViewService.batchHandle(request)));
    }
}
