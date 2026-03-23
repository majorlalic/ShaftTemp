package com.example.demo.alarm.rule.api;

import com.example.demo.alarm.rule.service.AlarmRuleManageService;
import com.example.demo.web.ApiResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alarm-rules")
public class AlarmRuleController {

    private final AlarmRuleManageService alarmRuleManageService;

    public AlarmRuleController(AlarmRuleManageService alarmRuleManageService) {
        this.alarmRuleManageService = alarmRuleManageService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> list(
        @RequestParam(required = false) String bizType,
        @RequestParam(required = false) String alarmType,
        @RequestParam(required = false) String scopeType,
        @RequestParam(required = false) Integer enabled
    ) {
        return ResponseEntity.ok(ApiResponse.success(alarmRuleManageService.listRules(bizType, alarmType, scopeType, enabled)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(alarmRuleManageService.getRule(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody AlarmRuleUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.success(alarmRuleManageService.createRule(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(@PathVariable Long id, @RequestBody AlarmRuleUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.success(alarmRuleManageService.updateRule(id, request)));
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<Map<String, Object>>> enable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(alarmRuleManageService.setEnabled(id, 1)));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<Map<String, Object>>> disable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(alarmRuleManageService.setEnabled(id, 0)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(alarmRuleManageService.deleteRule(id)));
    }
}
