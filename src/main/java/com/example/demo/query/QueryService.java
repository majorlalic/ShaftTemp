package com.example.demo.query;

import com.example.demo.persistence.entity.AlarmEntity;
import com.example.demo.persistence.entity.EventEntity;
import com.example.demo.persistence.entity.RawDataEntity;
import com.example.demo.persistence.entity.TempStatMinuteEntity;
import com.example.demo.persistence.repository.AlarmRepository;
import com.example.demo.persistence.repository.EventRepository;
import com.example.demo.persistence.repository.RawDataRepository;
import com.example.demo.persistence.repository.TempStatMinuteRepository;
import com.example.demo.realtime.RealtimeStateService;
import com.example.demo.realtime.RealtimeStateService.RealtimeSummary;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class QueryService {

    private final AlarmRepository alarmRepository;
    private final EventRepository eventRepository;
    private final RawDataRepository rawDataRepository;
    private final TempStatMinuteRepository tempStatMinuteRepository;
    private final RealtimeStateService realtimeStateService;
    private final QueryMapper queryMapper;

    public QueryService(
        AlarmRepository alarmRepository,
        EventRepository eventRepository,
        RawDataRepository rawDataRepository,
        TempStatMinuteRepository tempStatMinuteRepository,
        RealtimeStateService realtimeStateService,
        QueryMapper queryMapper
    ) {
        this.alarmRepository = alarmRepository;
        this.eventRepository = eventRepository;
        this.rawDataRepository = rawDataRepository;
        this.tempStatMinuteRepository = tempStatMinuteRepository;
        this.realtimeStateService = realtimeStateService;
        this.queryMapper = queryMapper;
    }

    public List<Map<String, Object>> listAlarms(String status, Long monitorId, Long deviceId) {
        return alarmRepository.findAll().stream()
            .filter(this::notDeleted)
            .filter(alarm -> status == null || status.equalsIgnoreCase(alarm.getStatus()))
            .filter(alarm -> monitorId == null || monitorId.equals(alarm.getMonitorId()))
            .filter(alarm -> deviceId == null || deviceId.equals(alarm.getDeviceId()))
            .sorted(Comparator.comparing(AlarmEntity::getUpdatedOn, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(queryMapper::toAlarmMap)
            .collect(Collectors.toList());
    }

    public Map<String, Object> getAlarmDetail(Long alarmId) {
        AlarmEntity alarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("alarm", queryMapper.toAlarmMap(alarm));
        payload.put("events", eventRepository.findByAlarmIdOrderByEventTimeDesc(alarmId).stream()
            .map(queryMapper::toEventMap)
            .collect(Collectors.toList()));
        return payload;
    }

    public List<Map<String, Object>> listEvents(Long alarmId) {
        List<EventEntity> events = alarmId == null ? eventRepository.findAll() : eventRepository.findByAlarmIdOrderByEventTimeDesc(alarmId);
        return events.stream()
            .filter(this::notDeleted)
            .sorted(Comparator.comparing(EventEntity::getEventTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(queryMapper::toEventMap)
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> listRawData(Long monitorId, Long deviceId, Integer limit) {
        List<RawDataEntity> results = rawDataRepository.findRecentAll().stream()
            .filter(rawData -> monitorId == null || monitorId.equals(rawData.getMonitorId()))
            .filter(rawData -> deviceId == null || deviceId.equals(rawData.getDeviceId()))
            .collect(Collectors.toList());
        int safeLimit = limit == null ? 50 : Math.max(1, limit.intValue());
        List<Map<String, Object>> payload = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < results.size() && i < safeLimit; i++) {
            payload.add(queryMapper.toRawDataMap(results.get(i)));
        }
        return payload;
    }

    public List<Map<String, Object>> listTempStats(Long monitorId, Long deviceId, LocalDateTime from, LocalDateTime to, Integer limit) {
        List<TempStatMinuteEntity> results = tempStatMinuteRepository.findRecentAll().stream()
            .filter(stat -> monitorId == null || monitorId.equals(stat.getMonitorId()))
            .filter(stat -> deviceId == null || deviceId.equals(stat.getDeviceId()))
            .filter(stat -> from == null || !stat.getStatTime().isBefore(from))
            .filter(stat -> to == null || !stat.getStatTime().isAfter(to))
            .collect(Collectors.toList());
        int safeLimit = limit == null ? 100 : Math.max(1, limit.intValue());
        List<Map<String, Object>> payload = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < results.size() && i < safeLimit; i++) {
            payload.add(queryMapper.toTempStatMap(results.get(i)));
        }
        return payload;
    }

    public Map<String, Object> getRealtimeDeviceState(Long deviceId) {
        Optional<RealtimeSummary> summary = realtimeStateService.getLastSummary(deviceId);
        if (!summary.isPresent()) {
            throw new IllegalArgumentException("Realtime state not found for deviceId=" + deviceId);
        }
        return queryMapper.toRealtimeMap(deviceId, summary.get());
    }

    private boolean notDeleted(AlarmEntity alarm) {
        return alarm.getDeleted() == null || alarm.getDeleted().intValue() == 0;
    }

    private boolean notDeleted(EventEntity event) {
        return event.getDeleted() == null || event.getDeleted().intValue() == 0;
    }
}
