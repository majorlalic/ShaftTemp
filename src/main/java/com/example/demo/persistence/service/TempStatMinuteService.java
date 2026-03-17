package com.example.demo.persistence.service;

import com.example.demo.ingest.service.DeviceResolverService;
import com.example.demo.ingest.service.ReportIngestService.ReportMetrics;
import com.example.demo.persistence.entity.TempStatMinuteEntity;
import com.example.demo.persistence.repository.TempStatMinuteRepository;
import com.example.demo.realtime.RealtimeStateService;
import com.example.demo.realtime.RealtimeStateService.MinuteStatAggregate;
import com.example.demo.support.IdGenerator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TempStatMinuteService {

    private final TempStatMinuteRepository tempStatMinuteRepository;
    private final RealtimeStateService realtimeStateService;
    private final IdGenerator idGenerator;

    public TempStatMinuteService(
        TempStatMinuteRepository tempStatMinuteRepository,
        RealtimeStateService realtimeStateService,
        IdGenerator idGenerator
    ) {
        this.tempStatMinuteRepository = tempStatMinuteRepository;
        this.realtimeStateService = realtimeStateService;
        this.idGenerator = idGenerator;
    }

    public void aggregate(
        DeviceResolverService.ResolvedTarget resolved,
        LocalDateTime collectTime,
        ReportMetrics metrics,
        int alarmPointCount
    ) {
        realtimeStateService.updateMinuteAggregate(resolved, collectTime, metrics, alarmPointCount);
    }

    @Scheduled(fixedDelayString = "${shaft.stat.flush-delay-ms:15000}")
    @Transactional
    public void flushPendingMinuteStats() {
        List<String> pendingKeys = realtimeStateService.getPendingMinuteStatKeys();
        LocalDateTime currentMinute = LocalDateTime.now().withSecond(0).withNano(0);
        for (String key : pendingKeys) {
            MinuteStatAggregate aggregate = realtimeStateService.getMinuteAggregate(key).orElse(null);
            if (aggregate == null) {
                realtimeStateService.removePendingMinuteStat(key);
                continue;
            }
            if (!aggregate.getStatTime().isBefore(currentMinute)) {
                continue;
            }
            upsertAggregate(aggregate);
            realtimeStateService.removePendingMinuteStat(key);
        }
    }

    private TempStatMinuteEntity upsertAggregate(MinuteStatAggregate aggregate) {
        TempStatMinuteEntity entity = tempStatMinuteRepository.findActiveByStatTime(
            aggregate.getDeviceId(),
            aggregate.getMonitorId(),
            aggregate.getPartitionCode(),
            aggregate.getStatTime()
        ).orElse(null);
        if (entity == null) {
            entity = new TempStatMinuteEntity();
            entity.setId(idGenerator.nextId());
            entity.setDeviceId(aggregate.getDeviceId());
            entity.setMonitorId(aggregate.getMonitorId());
            entity.setShaftFloorId(aggregate.getShaftFloorId());
            entity.setPartitionCode(aggregate.getPartitionCode());
            entity.setPartitionName(aggregate.getPartitionName());
            entity.setDataReference(aggregate.getDataReference());
            entity.setDeviceToken(aggregate.getDeviceToken());
            entity.setPartitionNo(aggregate.getPartitionNo());
            entity.setSourceFormat(aggregate.getSourceFormat());
            entity.setStatTime(aggregate.getStatTime());
            entity.setMaxTemp(aggregate.getMaxTemp());
            entity.setMinTemp(aggregate.getMinTemp());
            entity.setAvgTemp(aggregate.getAvgTemp());
            entity.setAlarmPointCount(aggregate.getAlarmCount());
            entity.setDeleted(0);
            entity.setCreatedOn(LocalDateTime.now());
        } else {
            entity.setMaxTemp(entity.getMaxTemp().max(aggregate.getMaxTemp()));
            entity.setMinTemp(entity.getMinTemp().min(aggregate.getMinTemp()));
            entity.setAvgTemp(entity.getAvgTemp().add(aggregate.getAvgTemp()).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP));
            entity.setAlarmPointCount((entity.getAlarmPointCount() == null ? 0 : entity.getAlarmPointCount()) + aggregate.getAlarmCount());
        }
        return tempStatMinuteRepository.save(entity);
    }
}
