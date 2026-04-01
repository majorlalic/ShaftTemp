package com.example.demo.controller;

import com.example.demo.service.TerminalDocService;
import com.example.demo.vo.AlarmHandleRequest;
import com.example.demo.vo.ApiResponse;
import com.example.demo.vo.PagePayload;
import com.example.demo.vo.TerminalAccessConfirmRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/terminal")
public class TerminalDocController {

    private final TerminalDocService terminalDocService;

    public TerminalDocController(TerminalDocService terminalDocService) {
        this.terminalDocService = terminalDocService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> statistics(
        @RequestParam(required = false) Long orgId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        return ResponseEntity.ok(ApiResponse.success(terminalDocService.statistics(orgId, startTime, endTime)));
    }

    @GetMapping("/alarm/stat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> alarmStat() {
        return ResponseEntity.ok(ApiResponse.success(terminalDocService.alarmStat()));
    }

    @GetMapping("/alarm/list")
    public ResponseEntity<ApiResponse<PagePayload<Map<String, Object>>>> alarmList(
        @RequestParam(required = false) Integer pageNo,
        @RequestParam(required = false) Integer pageNum,
        @RequestParam(required = false) Integer pageSize,
        @RequestParam(required = false) String status
    ) {
        Integer resolvedPageNo = pageNo == null ? pageNum : pageNo;
        return ResponseEntity.ok(ApiResponse.success(terminalDocService.alarmList(resolvedPageNo, pageSize, status)));
    }

    @PutMapping("/alarm/handle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handle(@RequestBody AlarmHandleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(terminalDocService.handleAlarm(request, false)));
    }

    @PutMapping("/alarm/batchHandle")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchHandle(@RequestBody AlarmHandleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(terminalDocService.handleAlarm(request, true)));
    }

    @GetMapping("/alarm/{id}/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> alarmDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(terminalDocService.alarmDetail(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(terminalDocService.detail(id)));
    }

    @GetMapping("/ledger/list")
    public ResponseEntity<ApiResponse<PagePayload<Map<String, Object>>>> ledgerList(
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
        return ResponseEntity.ok(ApiResponse.success(
            terminalDocService.ledgerList(resolvedPageNo, pageSize, deviceType, company, model, orgId, startDate, endDate)
        ));
    }

    @GetMapping("/stat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ledgerStat() {
        return ResponseEntity.ok(ApiResponse.success(terminalDocService.ledgerStat()));
    }

    @GetMapping("/access/list")
    public ResponseEntity<ApiResponse<PagePayload<Map<String, Object>>>> accessList(
        @RequestParam(required = false) Integer pageNo,
        @RequestParam(required = false) Integer pageNum,
        @RequestParam(required = false) Integer pageSize,
        @RequestParam(required = false) String status
    ) {
        Integer resolvedPageNo = pageNo == null ? pageNum : pageNo;
        return ResponseEntity.ok(ApiResponse.success(terminalDocService.accessList(resolvedPageNo, pageSize, status)));
    }

    @PostMapping("/access/confirm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> accessConfirm(@RequestBody TerminalAccessConfirmRequest request) {
        return ResponseEntity.ok(ApiResponse.success(terminalDocService.accessConfirm(request)));
    }
}
