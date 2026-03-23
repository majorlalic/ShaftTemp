package com.example.demo.organization.api;

import com.example.demo.web.ApiResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    private final OrganizationImportService organizationImportService;

    public OrganizationController(OrganizationImportService organizationImportService) {
        this.organizationImportService = organizationImportService;
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<Map<String, Object>>> tree() {
        return ResponseEntity.ok(ApiResponse.success(organizationImportService.tree()));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(@RequestParam String type) {
        byte[] body = organizationImportService.template(type);
        String filename = "organization-" + type + "-template.csv";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(body);
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importCsv(@RequestParam String type, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(organizationImportService.importCsv(type, file)));
    }
}
