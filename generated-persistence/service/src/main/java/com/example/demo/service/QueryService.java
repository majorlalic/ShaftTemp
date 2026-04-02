package com.example.demo.service;

import com.csg.dgri.szsiom.sysmanage.model.AlarmVO;
import com.csg.dgri.szsiom.sysmanage.model.EventVO;
import com.csg.dgri.szsiom.sysmanage.model.RawDataVO;
import com.csg.dgri.szsiom.sysmanage.appservice.AlarmAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.EventAppService;
import com.example.demo.dao.QueryMapper;
import com.csg.dgri.szsiom.sysmanage.appservice.RawDataAppService;
import com.example.demo.service.RealtimeStateService.RealtimeSummary;
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

    private final AlarmAppService<?> alarmRepository;
    private final EventAppService<?> eventRepository;
    private final RawDataAppService<?> rawDataQueryRepository;
    private final RealtimeStateService realtimeStateService;
    private final QueryMapper queryMapper;

    public QueryService(
        AlarmAppService<?> alarmRepository,
        EventAppService<?> eventRepository,
        RawDataAppService<?> rawDataQueryRepository,
        RealtimeStateService realtimeStateService,
        QueryMapper queryMapper
    ) {
        this.alarmRepository = alarmRepository;
        this.eventRepository = eventRepository;
        this.rawDataQueryRepository = rawDataQueryRepository;
        this.realtimeStateService = realtimeStateService;
        this.queryMapper = queryMapper;
    }

    public List<Map<String, Object>> listAlarms(String status, Long monitorId, Long deviceId, Long shaftFloorId, String partitionCode) {
        Integer statusCode = parseStatus(status);
        return alarmRepository.findAll().stream()
            .filter(this::notDeleted)
            .filter(alarm -> statusCode == null || statusCode.equals(alarm.getStatus()))
            .filter(alarm -> monitorId == null || monitorId.equals(alarm.getMonitorId()))
            .filter(alarm -> deviceId == null || deviceId.equals(alarm.getDeviceId()))
            .sorted(Comparator.comparing(AlarmVO::getUpdatedOn, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(queryMapper::toAlarmMap)
            .collect(Collectors.toList());
    }

    public Map<String, Object> getAlarmDetail(Long alarmId) {
        AlarmVO alarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("alarm", queryMapper.toAlarmMap(alarm));
        payload.put("events", eventRepository.findByAlarmIdOrderByEventTimeDesc(alarmId).stream()
            .map(queryMapper::toEventMap)
            .collect(Collectors.toList()));
        return payload;
    }

    public List<Map<String, Object>> listEvents(Long alarmId, Long shaftFloorId, String partitionCode) {
        List<EventVO> events = alarmId == null ? eventRepository.findAll() : eventRepository.findByAlarmIdOrderByEventTimeDesc(alarmId);
        return events.stream()
            .filter(this::notDeleted)
            .filter(event -> shaftFloorId == null || shaftFloorId.equals(event.getShaftFloorId()))
            .filter(event -> partitionCode == null || partitionCode.equals(event.getPartitionCode()))
            .sorted(Comparator.comparing(EventVO::getEventTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(queryMapper::toEventMap)
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> listRawData(
        Long monitorId,
        Long deviceId,
        Long shaftFloorId,
        Integer partitionId,
        LocalDateTime from,
        LocalDateTime to,
        Integer limit
    ) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required for raw data query");
        }
        int safeLimit = limit == null ? 50 : Math.max(1, limit.intValue());
        List<RawDataVO> results = rawDataQueryRepository.query(
            monitorId,
            deviceId,
            shaftFloorId,
            partitionId,
            from,
            to,
            safeLimit
        );
        List<Map<String, Object>> payload = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < results.size() && i < safeLimit; i++) {
            payload.add(queryMapper.toRawDataMap(results.get(i)));
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

    private boolean notDeleted(AlarmVO alarm) {
        return alarm.getDeleted() == null || alarm.getDeleted().intValue() == 0;
    }

    private boolean notDeleted(EventVO event) {
        return event.getDeleted() == null || event.getDeleted().intValue() == 0;
    }

    private Integer parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        return Integer.valueOf(status);
    }
}
