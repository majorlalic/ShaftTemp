package com.example.demo.controller;

import com.example.demo.service.OrganizationImportService;
import com.example.demo.vo.RestObject;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/shaft/areaOrg")
public class AreaOrgController {

    private final OrganizationImportService organizationImportService;

    public AreaOrgController(OrganizationImportService organizationImportService) {
        this.organizationImportService = organizationImportService;
    }

    @GetMapping("/tree")
    public RestObject<Map<String, Object>> tree() {
        return RestObject.newOk(organizationImportService.tree());
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
    public RestObject<Map<String, Object>> importCsv(@RequestParam String type, @RequestParam("file") MultipartFile file) {
        return RestObject.newOk(organizationImportService.importCsv(type, file));
    }
}
