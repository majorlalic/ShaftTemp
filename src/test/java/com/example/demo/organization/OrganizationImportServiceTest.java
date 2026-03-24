package com.example.demo.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.organization.api.OrganizationImportService;
import com.example.demo.persistence.entity.AreaEntity;
import com.example.demo.persistence.entity.OrgEntity;
import com.example.demo.persistence.repository.AreaRepository;
import com.example.demo.persistence.repository.OrgRepository;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class OrganizationImportServiceTest {

    @Mock
    private OrgRepository orgRepository;

    @Mock
    private AreaRepository areaRepository;

    @Test
    void shouldInheritExistingOrgPathWhenImportingChild() {
        OrgEntity parent = new OrgEntity();
        parent.setId(2001L);
        parent.setParentId(0L);
        parent.setName("测试机构");
        parent.setPathIds("2001");
        parent.setPathNames("测试机构");
        when(orgRepository.findAllActive()).thenReturn(Collections.singletonList(parent));

        OrganizationImportService service = new OrganizationImportService(orgRepository, areaRepository);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "org.csv",
            "text/csv",
            "id,parentId,name,type,sort\n2100,2001,导入机构,COMPANY,1\n".getBytes(StandardCharsets.UTF_8)
        );

        Map<String, Object> result = service.importCsv("org", file);

        assertEquals(1, result.get("successCount"));
        ArgumentCaptor<List<OrgEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(orgRepository).saveAll(captor.capture());
        OrgEntity imported = captor.getValue().get(0);
        assertEquals("2001/2100", imported.getPathIds());
        assertEquals("测试机构/导入机构", imported.getPathNames());
    }

    @Test
    void shouldInheritExistingAreaPathWhenImportingChild() {
        AreaEntity parent = new AreaEntity();
        parent.setId(1001L);
        parent.setParentId(0L);
        parent.setName("测试区域");
        parent.setPathIds("1001");
        parent.setPathNames("测试区域");
        when(areaRepository.findAllActive()).thenReturn(Collections.singletonList(parent));

        OrganizationImportService service = new OrganizationImportService(orgRepository, areaRepository);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "area.csv",
            "text/csv",
            "id,parentId,name,type,sort\n1100,1001,导入区域,COMMUNITY,1\n".getBytes(StandardCharsets.UTF_8)
        );

        Map<String, Object> result = service.importCsv("area", file);

        assertEquals(1, result.get("successCount"));
        ArgumentCaptor<List<AreaEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(areaRepository).saveAll(captor.capture());
        AreaEntity imported = captor.getValue().get(0);
        assertEquals("1001/1100", imported.getPathIds());
        assertEquals("测试区域/导入区域", imported.getPathNames());
    }
}
