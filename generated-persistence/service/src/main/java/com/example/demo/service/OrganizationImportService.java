package com.example.demo.service;

import com.csg.dgri.szsiom.sysmanage.model.AreaVO;
import com.csg.dgri.szsiom.sysmanage.model.OrgVO;
import com.csg.dgri.szsiom.sysmanage.appservice.AreaAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.OrgAppService;
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

    private final OrgAppService<?> orgRepository;
    private final AreaAppService<?> areaRepository;

    public OrganizationImportService(OrgAppService<?> orgRepository, AreaAppService<?> areaRepository) {
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
            List<OrgVO> entities = parseOrg(file);
            for (OrgVO entity : entities) {
                orgRepository.upsert(entity);
            }
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("type", "org");
            data.put("successCount", entities.size());
            return data;
        }
        if ("area".equalsIgnoreCase(type)) {
            List<AreaVO> entities = parseArea(file);
            for (AreaVO entity : entities) {
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

    private Map<String, Object> toOrgNode(OrgVO entity) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", entity.getId());
        row.put("parentId", entity.getParentId());
        row.put("name", entity.getName());
        row.put("type", entity.getType());
        row.put("pathNames", entity.getPathNames());
        row.put("sort", entity.getSort());
        return row;
    }

    private Map<String, Object> toAreaNode(AreaVO entity) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", entity.getId());
        row.put("parentId", entity.getParentId());
        row.put("name", entity.getName());
        row.put("type", entity.getType());
        row.put("pathNames", entity.getPathNames());
        row.put("sort", entity.getSort());
        return row;
    }

    private List<OrgVO> parseOrg(MultipartFile file) {
        try {
            List<String[]> rows = parseCsv(file);
            Map<Long, OrgVO> entities = new LinkedHashMap<Long, OrgVO>();
            Map<Long, OrgVO> existing = toOrgMap(orgRepository.findAllActive());
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 5) {
                    continue;
                }
                OrgVO entity = new OrgVO();
                entity.setId(Long.valueOf(row[0].trim()));
                entity.setParentId(parseNullableLong(row[1]));
                entity.setName(row[2].trim());
                entity.setType(row[3].trim());
                entity.setSort(Integer.valueOf(row[4].trim()));
                entity.setDeleted(0);
                entities.put(entity.getId(), entity);
            }
            fillOrgPaths(existing, entities);
            return new ArrayList<OrgVO>(entities.values());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to parse org csv", ex);
        }
    }

    private List<AreaVO> parseArea(MultipartFile file) {
        try {
            List<String[]> rows = parseCsv(file);
            Map<Long, AreaVO> entities = new LinkedHashMap<Long, AreaVO>();
            Map<Long, AreaVO> existing = toAreaMap(areaRepository.findAllActive());
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 5) {
                    continue;
                }
                AreaVO entity = new AreaVO();
                entity.setId(Long.valueOf(row[0].trim()));
                entity.setParentId(parseNullableLong(row[1]));
                entity.setName(row[2].trim());
                entity.setType(row[3].trim());
                entity.setSort(Integer.valueOf(row[4].trim()));
                entity.setDeleted(0);
                entities.put(entity.getId(), entity);
            }
            fillAreaPaths(existing, entities);
            return new ArrayList<AreaVO>(entities.values());
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

    private void fillOrgPaths(Map<Long, OrgVO> existing, Map<Long, OrgVO> imported) {
        Map<Long, OrgVO> lookup = new LinkedHashMap<Long, OrgVO>(existing);
        lookup.putAll(imported);
        for (OrgVO entity : imported.values()) {
            fillOrgPath(entity, lookup);
        }
    }

    private void fillAreaPaths(Map<Long, AreaVO> existing, Map<Long, AreaVO> imported) {
        Map<Long, AreaVO> lookup = new LinkedHashMap<Long, AreaVO>(existing);
        lookup.putAll(imported);
        for (AreaVO entity : imported.values()) {
            fillAreaPath(entity, lookup);
        }
    }

    private void fillOrgPath(OrgVO entity, Map<Long, OrgVO> lookup) {
        List<String> ids = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        Set<Long> visited = new HashSet<Long>();
        OrgVO cursor = entity;
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

    private void fillAreaPath(AreaVO entity, Map<Long, AreaVO> lookup) {
        List<String> ids = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        Set<Long> visited = new HashSet<Long>();
        AreaVO cursor = entity;
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

    private Map<Long, OrgVO> toOrgMap(List<OrgVO> rows) {
        Map<Long, OrgVO> result = new LinkedHashMap<Long, OrgVO>();
        for (OrgVO row : rows) {
            result.put(row.getId(), row);
        }
        return result;
    }

    private Map<Long, AreaVO> toAreaMap(List<AreaVO> rows) {
        Map<Long, AreaVO> result = new LinkedHashMap<Long, AreaVO>();
        for (AreaVO row : rows) {
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
