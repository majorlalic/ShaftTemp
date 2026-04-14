package com.example.demo.controller;

import com.example.demo.service.QueryService;
import com.example.demo.service.TerminalDocService;
import com.example.demo.vo.PagePayload;
import com.example.demo.vo.RestObject;
import com.example.demo.vo.TerminalAccessConfirmRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceController {

    private final TerminalDocService terminalDocService;
    private final QueryService queryService;

    public DeviceController(TerminalDocService terminalDocService, QueryService queryService) {
        this.terminalDocService = terminalDocService;
        this.queryService = queryService;
    }

    @GetMapping("/api/terminal/statistics")
    public RestObject<Map<String, Object>> statistics(
        @RequestParam(required = false) Long orgId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return RestObject.newOk(terminalDocService.statistics(orgId, startTime, endTime));
    }

    @GetMapping("/api/terminal/{id}")
    public RestObject<Map<String, Object>> detail(@PathVariable Long id) {
        return RestObject.newOk(terminalDocService.detail(id));
    }

    @GetMapping("/api/terminal/ledger/list")
    public RestObject<PagePayload<Map<String, Object>>> ledgerList(
        @RequestParam(required = false) Integer pageNo,
        @RequestParam(required = false) Integer pageNum,
        @RequestParam(required = false) Integer pageSize,
        @RequestParam(required = false) String deviceType,
        @RequestParam(required = false) String company,
        @RequestParam(required = false) String model,
        @RequestParam(required = false) Long orgId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        Integer resolvedPageNo = pageNo == null ? pageNum : pageNo;
        return RestObject.newOk(
            terminalDocService.ledgerList(resolvedPageNo, pageSize, deviceType, company, model, orgId, startDate, endDate)
        );
    }

    @GetMapping("/api/terminal/stat")
    public RestObject<Map<String, Object>> ledgerStat(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return RestObject.newOk(terminalDocService.ledgerStat(startTime, endTime));
    }

    @GetMapping("/api/terminal/access/list")
    public RestObject<PagePayload<Map<String, Object>>> accessList(
        @RequestParam(required = false) Integer pageNo,
        @RequestParam(required = false) Integer pageNum,
        @RequestParam(required = false) Integer pageSize,
        @RequestParam(required = false) String deviceType,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long orgId,
        @RequestParam(required = false) String manufacturer,
        @RequestParam(required = false) String model
    ) {
        Integer resolvedPageNo = pageNo == null ? pageNum : pageNo;
        return RestObject.newOk(terminalDocService.accessList(resolvedPageNo, pageSize, deviceType, status, orgId, manufacturer, model));
    }

    @PostMapping("/api/terminal/access/confirm")
    public RestObject<Map<String, Object>> accessConfirm(@RequestBody TerminalAccessConfirmRequest request) {
        return RestObject.newOk(terminalDocService.accessConfirm(request));
    }

    @GetMapping("/api/devices/{deviceId}/realtime")
    public RestObject<Map<String, Object>> getRealtime(@PathVariable Long deviceId) {
        return RestObject.newOk(queryService.getRealtimeDeviceState(deviceId));
    }
}
