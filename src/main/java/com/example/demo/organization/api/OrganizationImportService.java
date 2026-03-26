package com.example.demo.organization.api;

import com.example.demo.persistence.entity.AreaEntity;
import com.example.demo.persistence.entity.OrgEntity;
import com.example.demo.persistence.repository.AreaRepository;
import com.example.demo.persistence.repository.OrgRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class OrganizationImportService {

    private final OrgRepository orgRepository;
    private final AreaRepository areaRepository;

    public OrganizationImportService(OrgRepository orgRepository, AreaRepository areaRepository) {
        this.orgRepository = orgRepository;
        this.areaRepository = areaRepository;
    }

    public Map<String, Object> tree() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("orgTree", buildTree(orgRepository.findAllActive().stream().map(this::toOrgNode).collect(Collectors.toList())));
        data.put("areaTree", buildTree(areaRepository.findAllActive().stream().map(this::toAreaNode).collect(Collectors.toList())));
        return data;
    }

    public byte[] template(String type) {
        if ("org".equalsIgnoreCase(type)) {
            return "id,parentId,name,type,sort\n100,0,示例机构,COMPANY,1\n".getBytes(StandardCharsets.UTF_8);
        }
        if ("area".equalsIgnoreCase(type)) {
            return "id,parentId,name,type,sort\n1000,0,示例区域,COMMUNITY,1\n".getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    @Transactional
    public Map<String, Object> importCsv(String type, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        if ("org".equalsIgnoreCase(type)) {
            List<OrgEntity> entities = parseOrg(file);
            for (OrgEntity entity : entities) {
                orgRepository.upsert(entity);
            }
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("type", "org");
            data.put("successCount", entities.size());
            return data;
        }
        if ("area".equalsIgnoreCase(type)) {
            List<AreaEntity> entities = parseArea(file);
            for (AreaEntity entity : entities) {
                areaRepository.upsert(entity);
            }
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("type", "area");
            data.put("successCount", entities.size());
            return data;
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private List<Map<String, Object>> buildTree(List<Map<String, Object>> rows) {
        Map<Long, Map<String, Object>> nodeMap = new LinkedHashMap<Long, Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            row.put("children", new ArrayList<Map<String, Object>>());
            nodeMap.put(((Number) row.get("id")).longValue(), row);
        }
        List<Map<String, Object>> roots = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            Long parentId = row.get("parentId") == null ? null : ((Number) row.get("parentId")).longValue();
            if (parentId == null || parentId.longValue() == 0L || !nodeMap.containsKey(parentId)) {
                roots.add(row);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) nodeMap.get(parentId).get("children");
                children.add(row);
            }
        }
        return roots;
    }

    private Map<String, Object> toOrgNode(OrgEntity entity) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", entity.getId());
        row.put("parentId", entity.getParentId());
        row.put("name", entity.getName());
        row.put("type", entity.getType());
        row.put("pathNames", entity.getPathNames());
        row.put("sort", entity.getSort());
        return row;
    }

    private Map<String, Object> toAreaNode(AreaEntity entity) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", entity.getId());
        row.put("parentId", entity.getParentId());
        row.put("name", entity.getName());
        row.put("type", entity.getType());
        row.put("pathNames", entity.getPathNames());
        row.put("sort", entity.getSort());
        return row;
    }

    private List<OrgEntity> parseOrg(MultipartFile file) {
        try {
            List<String[]> rows = parseCsv(file);
            Map<Long, OrgEntity> entities = new LinkedHashMap<Long, OrgEntity>();
            Map<Long, OrgEntity> existing = toOrgMap(orgRepository.findAllActive());
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 5) {
                    continue;
                }
                OrgEntity entity = new OrgEntity();
                entity.setId(Long.valueOf(row[0].trim()));
                entity.setParentId(parseNullableLong(row[1]));
                entity.setName(row[2].trim());
                entity.setType(row[3].trim());
                entity.setSort(Integer.valueOf(row[4].trim()));
                entity.setDeleted(0);
                entities.put(entity.getId(), entity);
            }
            fillOrgPaths(existing, entities);
            return new ArrayList<OrgEntity>(entities.values());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to parse org csv", ex);
        }
    }

    private List<AreaEntity> parseArea(MultipartFile file) {
        try {
            List<String[]> rows = parseCsv(file);
            Map<Long, AreaEntity> entities = new LinkedHashMap<Long, AreaEntity>();
            Map<Long, AreaEntity> existing = toAreaMap(areaRepository.findAllActive());
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 5) {
                    continue;
                }
                AreaEntity entity = new AreaEntity();
                entity.setId(Long.valueOf(row[0].trim()));
                entity.setParentId(parseNullableLong(row[1]));
                entity.setName(row[2].trim());
                entity.setType(row[3].trim());
                entity.setSort(Integer.valueOf(row[4].trim()));
                entity.setDeleted(0);
                entities.put(entity.getId(), entity);
            }
            fillAreaPaths(existing, entities);
            return new ArrayList<AreaEntity>(entities.values());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to parse area csv", ex);
        }
    }

    private List<String[]> parseCsv(MultipartFile file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
        List<String[]> rows = new ArrayList<String[]>();
        String line;
        while ((line = reader.readLine()) != null) {
            rows.add(line.split(",", -1));
        }
        return rows;
    }

    private void fillOrgPaths(Map<Long, OrgEntity> existing, Map<Long, OrgEntity> imported) {
        Map<Long, OrgEntity> lookup = new LinkedHashMap<Long, OrgEntity>(existing);
        lookup.putAll(imported);
        for (OrgEntity entity : imported.values()) {
            fillOrgPath(entity, lookup);
        }
    }

    private void fillAreaPaths(Map<Long, AreaEntity> existing, Map<Long, AreaEntity> imported) {
        Map<Long, AreaEntity> lookup = new LinkedHashMap<Long, AreaEntity>(existing);
        lookup.putAll(imported);
        for (AreaEntity entity : imported.values()) {
            fillAreaPath(entity, lookup);
        }
    }

    private void fillOrgPath(OrgEntity entity, Map<Long, OrgEntity> lookup) {
        List<String> ids = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        Set<Long> visited = new HashSet<Long>();
        OrgEntity cursor = entity;
        while (cursor != null) {
            if (!visited.add(cursor.getId())) {
                throw new IllegalArgumentException("Org parent relation contains a cycle: " + cursor.getId());
            }
            ids.add(0, String.valueOf(cursor.getId()));
            names.add(0, cursor.getName());
            Long parentId = cursor.getParentId();
            if (parentId == null || parentId.longValue() == 0L) {
                cursor = null;
            } else {
                cursor = lookup.get(parentId);
            }
        }
        entity.setPathIds(String.join("/", ids));
        entity.setPathNames(String.join("/", names));
    }

    private void fillAreaPath(AreaEntity entity, Map<Long, AreaEntity> lookup) {
        List<String> ids = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        Set<Long> visited = new HashSet<Long>();
        AreaEntity cursor = entity;
        while (cursor != null) {
            if (!visited.add(cursor.getId())) {
                throw new IllegalArgumentException("Area parent relation contains a cycle: " + cursor.getId());
            }
            ids.add(0, String.valueOf(cursor.getId()));
            names.add(0, cursor.getName());
            Long parentId = cursor.getParentId();
            if (parentId == null || parentId.longValue() == 0L) {
                cursor = null;
            } else {
                cursor = lookup.get(parentId);
            }
        }
        entity.setPathIds(String.join("/", ids));
        entity.setPathNames(String.join("/", names));
    }

    private Map<Long, OrgEntity> toOrgMap(List<OrgEntity> rows) {
        Map<Long, OrgEntity> result = new LinkedHashMap<Long, OrgEntity>();
        for (OrgEntity row : rows) {
            result.put(row.getId(), row);
        }
        return result;
    }

    private Map<Long, AreaEntity> toAreaMap(List<AreaEntity> rows) {
        Map<Long, AreaEntity> result = new LinkedHashMap<Long, AreaEntity>();
        for (AreaEntity row : rows) {
            result.put(row.getId(), row);
        }
        return result;
    }

    private Long parseNullableLong(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty() || "0".equals(trimmed)) {
            return 0L;
        }
        return Long.valueOf(trimmed);
    }
}
