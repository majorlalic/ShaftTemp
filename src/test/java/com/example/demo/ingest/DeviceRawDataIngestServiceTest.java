package com.example.demo.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.ingest.dto.DeviceArrayRawRequest;
import com.example.demo.ingest.service.DeviceRawDataIngestService;
import com.example.demo.persistence.entity.DeviceEntity;
import com.example.demo.persistence.entity.MonitorEntity;
import com.example.demo.persistence.repository.DeviceRawDataJdbcRepository;
import com.example.demo.persistence.repository.DeviceRepository;
import com.example.demo.persistence.repository.MonitorRepository;
import com.example.demo.support.IdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceRawDataIngestServiceTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private MonitorRepository monitorRepository;
    @Mock
    private DeviceRawDataJdbcRepository deviceRawDataJdbcRepository;
    @Mock
    private IdGenerator idGenerator;

    @Test
    void shouldResolveIotCodeFromIedFullPathAndPersist() {
        DeviceEntity device = new DeviceEntity();
        device.setId(3001L);
        device.setIotCode("shaft-dev-01");
        MonitorEntity monitor = new MonitorEntity();
        monitor.setId(4001L);
        when(deviceRepository.findActiveByIotCode("shaft-dev-01")).thenReturn(Optional.of(device));
        when(monitorRepository.findActiveByDeviceId(3001L)).thenReturn(Optional.of(monitor));
        when(idGenerator.nextId()).thenReturn(90001L);

        DeviceRawDataIngestService service = new DeviceRawDataIngestService(
            deviceRepository,
            monitorRepository,
            deviceRawDataJdbcRepository,
            idGenerator,
            new ObjectMapper().registerModule(new JavaTimeModule())
        );

        DeviceArrayRawRequest request = new DeviceArrayRawRequest();
        request.setTopic("/RAW/shaft-dev-01/DeviceArray");
        request.setIedFullPath("/IED/shaft-dev-01");
        request.setValues(Arrays.asList(60.0, 62.0, 64.0));
        request.setValidStartPoint(1);
        request.setValidEndPoint(3);
        request.setTimestamp(LocalDateTime.of(2026, 3, 25, 12, 0, 0));

        DeviceRawDataIngestService.DeviceRawIngestResult result = service.ingest(request);

        assertEquals(90001L, result.getId().longValue());
        assertEquals(3001L, result.getDeviceId().longValue());
        assertEquals(4001L, result.getMonitorId().longValue());
        assertEquals("shaft-dev-01", result.getIotCode());
        assertEquals("64.0", result.getMetrics().getMaxTemp().toString());
        assertEquals("60.0", result.getMetrics().getMinTemp().toString());
        assertEquals("62.00", result.getMetrics().getAvgTemp().toString());
        verify(deviceRawDataJdbcRepository).insert(any());
    }
}
