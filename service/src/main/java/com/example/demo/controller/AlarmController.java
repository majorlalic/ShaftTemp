package com.example.demo.controller;

import com.example.demo.service.AlarmRuleManageService;
import com.example.demo.service.AlarmViewService;
import com.example.demo.service.QueryService;
import com.example.demo.service.TerminalDocService;
import com.example.demo.vo.AlarmHandleRequest;
import com.example.demo.vo.AlarmRuleUpsertRequest;
import com.example.demo.vo.PagePayload;
import com.example.demo.vo.RestObject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AlarmController {

    private final AlarmViewService alarmViewService;
    private final TerminalDocService terminalDocService;
    private final QueryService queryService;
    private final AlarmRuleManageService alarmRuleManageService;

    public AlarmController(
        AlarmViewService alarmViewService,
        TerminalDocService terminalDocService,
        QueryService queryService,
        AlarmRuleManageService alarmRuleManageService
    ) {
        this.alarmViewService = alarmViewService;
        this.terminalDocService = terminalDocService;
        this.queryService = queryService;
        this.alarmRuleManageService = alarmRuleManageService;
    }

    @GetMapping("/api/alarm/statistics")
    public RestObject<Map<String, Object>> statistics(
        @RequestParam(required = false) Long areaId,
        @RequestParam(required = false) Long deviceId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return RestObject.newOk(alarmViewService.statistics(areaId, deviceId, startTime, endTime));
    }

    @GetMapping("/api/alarm/list")
    public RestObject<PagePayload<Map<String, Object>>> list(
        @RequestParam(required = false) Integer pageNo,
        @RequestParam(required = false) Integer pageNum,
        @RequestParam(required = false) Integer pageSize,
        @RequestParam(required = false) Long areaId,
        @RequestParam(required = false) Long deviceId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
        @RequestParam(required = false) String status
    ) {
        Integer resolvedPageNo = pageNo == null ? pageNum : pageNo;
        return RestObject.newOk(alarmViewService.list(resolvedPageNo, pageSize, areaId, deviceId, startTime, endTime, status));
    }

    @GetMapping("/api/alarm/merge/list")
    public RestObject<PagePayload<Map<String, Object>>> mergeList(
        @RequestParam Long alarmId,
        @RequestParam(required = false) Integer pageNo,
        @RequestParam(required = false) Integer pageNum,
        @RequestParam Integer pageSize
    ) {
        Integer resolvedPageNo = pageNo == null ? pageNum : pageNo;
        return RestObject.newOk(alarmViewService.mergeEvents(alarmId, resolvedPageNo, pageSize));
    }

    @GetMapping("/api/alarm/detail")
    public RestObject<Map<String, Object>> detail(@RequestParam("id") Long alarmId) {
        return RestObject.newOk(alarmViewService.detail(alarmId));
    }

    @PostMapping("/api/alarm/handle")
    public RestObject<Map<String, Object>> handle(@RequestBody AlarmHandleRequest request) {
        return RestObject.newOk(alarmViewService.handle(request));
    }

    @PostMapping("/api/alarm/batchHandle")
    public RestObject<Map<String, Object>> batchHandle(@RequestBody AlarmHandleRequest request) {
        return RestObject.newOk(alarmViewService.batchHandle(request));
    }

    @GetMapping("/api/terminal/alarm/stat")
    public RestObject<Map<String, Object>> alarmStat() {
        return RestObject.newOk(terminalDocService.alarmStat());
    }

    @GetMapping("/api/terminal/alarm/list")
    public RestObject<PagePayload<Map<String, Object>>> terminalAlarmList(
        @RequestParam(required = false) Integer pageNo,
        @RequestParam(required = false) Integer pageNum,
        @RequestParam(required = false) Integer pageSize,
        @RequestParam(required = false) String status
    ) {
        Integer resolvedPageNo = pageNo == null ? pageNum : pageNo;
        return RestObject.newOk(terminalDocService.alarmList(resolvedPageNo, pageSize, status));
    }

    @PutMapping("/api/terminal/alarm/handle")
    public RestObject<Map<String, Object>> terminalHandle(@RequestBody AlarmHandleRequest request) {
        return RestObject.newOk(terminalDocService.handleAlarm(request, false));
    }

    @PutMapping("/api/terminal/alarm/batchHandle")
    public RestObject<Map<String, Object>> terminalBatchHandle(@RequestBody AlarmHandleRequest request) {
        return RestObject.newOk(terminalDocService.handleAlarm(request, true));
    }

    @GetMapping("/api/terminal/alarm/{id}/detail")
    public RestObject<Map<String, Object>> alarmDetail(@PathVariable Long id) {
        return RestObject.newOk(terminalDocService.alarmDetail(id));
    }

    @GetMapping("/api/events")
    public RestObject<List<Map<String, Object>>> listEvents(
        @RequestParam(required = false) Long alarmId,
        @RequestParam(required = false) Long shaftFloorId,
        @RequestParam(required = false) String partitionCode
    ) {
        return RestObject.newOk(queryService.listEvents(alarmId, shaftFloorId, partitionCode));
    }

    @GetMapping("/api/alarm-rules")
    public RestObject<Object> listRules(
        @RequestParam(required = false) String bizType,
        @RequestParam(required = false) String alarmType,
        @RequestParam(required = false) String scopeType,
        @RequestParam(required = false) Integer enabled
    ) {
        return RestObject.newOk(alarmRuleManageService.listRules(bizType, alarmType, scopeType, enabled));
    }

    @GetMapping("/api/alarm-rules/{id}")
    public RestObject<Map<String, Object>> ruleDetail(@PathVariable Long id) {
        return RestObject.newOk(alarmRuleManageService.getRule(id));
    }

    @PostMapping("/api/alarm-rules")
    public RestObject<Map<String, Object>> createRule(@RequestBody AlarmRuleUpsertRequest request) {
        return RestObject.newOk(alarmRuleManageService.createRule(request));
    }

    @PutMapping("/api/alarm-rules/{id}")
    public RestObject<Map<String, Object>> updateRule(@PathVariable Long id, @RequestBody AlarmRuleUpsertRequest request) {
        return RestObject.newOk(alarmRuleManageService.updateRule(id, request));
    }

    @PostMapping("/api/alarm-rules/{id}/enable")
    public RestObject<Map<String, Object>> enableRule(@PathVariable Long id) {
        return RestObject.newOk(alarmRuleManageService.setEnabled(id, 1));
    }

    @PostMapping("/api/alarm-rules/{id}/disable")
    public RestObject<Map<String, Object>> disableRule(@PathVariable Long id) {
        return RestObject.newOk(alarmRuleManageService.setEnabled(id, 0));
    }

    @DeleteMapping("/api/alarm-rules/{id}")
    public RestObject<Map<String, Object>> deleteRule(@PathVariable Long id) {
        return RestObject.newOk(alarmRuleManageService.deleteRule(id));
    }
}
