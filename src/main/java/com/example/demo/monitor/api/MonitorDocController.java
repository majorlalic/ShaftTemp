package com.example.demo.monitor.api;

import com.example.demo.web.ApiResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor")
public class MonitorDocController {

    private final MonitorDocService monitorDocService;

    public MonitorDocController(MonitorDocService monitorDocService) {
        this.monitorDocService = monitorDocService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(monitorDocService.detail(id)));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> statistics() {
        return ResponseEntity.ok(ApiResponse.success(monitorDocService.statistics()));
    }
}
