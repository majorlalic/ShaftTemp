package com.example.demo.persistence.service;

import com.example.demo.ingest.service.ReportIngestService.ReportMetrics;
import com.example.demo.persistence.entity.TempStatMinuteEntity;
import com.example.demo.persistence.repository.TempStatMinuteRepository;
import com.example.demo.support.IdGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TempStatMinuteService {

    private final TempStatMinuteRepository tempStatMinuteRepository;
    private final IdGenerator idGenerator;

    public TempStatMinuteService(TempStatMinuteRepository tempStatMinuteRepository, IdGenerator idGenerator) {
        this.tempStatMinuteRepository = tempStatMinuteRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public TempStatMinuteEntity aggregate(
        Long deviceId,
        Long monitorId,
        LocalDateTime collectTime,
        ReportMetrics metrics,
        int alarmPointCount
    ) {
        LocalDateTime statTime = collectTime.withSecond(0).withNano(0);
        TempStatMinuteEntity entity = tempStatMinuteRepository.findActiveByStatTime(deviceId, monitorId, statTime).orElse(null);
        if (entity == null) {
            entity = new TempStatMinuteEntity();
            entity.setId(idGenerator.nextId());
            entity.setDeviceId(deviceId);
            entity.setMonitorId(monitorId);
            entity.setStatTime(statTime);
            entity.setMaxTemp(metrics.getMaxTemp());
            entity.setMinTemp(metrics.getMinTemp());
            entity.setAvgTemp(metrics.getAvgTemp());
            entity.setAlarmPointCount(alarmPointCount);
            entity.setDeleted(0);
            entity.setCreatedOn(LocalDateTime.now());
        } else {
            entity.setMaxTemp(entity.getMaxTemp().max(metrics.getMaxTemp()));
            entity.setMinTemp(entity.getMinTemp().min(metrics.getMinTemp()));
            entity.setAvgTemp(entity.getAvgTemp().add(metrics.getAvgTemp()).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP));
            entity.setAlarmPointCount((entity.getAlarmPointCount() == null ? 0 : entity.getAlarmPointCount()) + alarmPointCount);
        }
        return tempStatMinuteRepository.save(entity);
    }
}
