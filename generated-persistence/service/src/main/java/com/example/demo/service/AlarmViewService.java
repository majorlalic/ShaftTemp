package com.example.demo.service;

import com.example.demo.service.AlarmService;
import com.example.demo.service.AlarmStatus;
import com.csg.dgri.szsiom.sysmanage.model.AlarmVO;
import com.csg.dgri.szsiom.sysmanage.model.DeviceVO;
import com.csg.dgri.szsiom.sysmanage.model.EventVO;
import com.csg.dgri.szsiom.sysmanage.model.MonitorVO;
import com.csg.dgri.szsiom.sysmanage.appservice.AlarmAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.DeviceAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.EventAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.MonitorAppService;
import com.example.demo.vo.AlarmHandleRequest;
import com.example.demo.vo.PagePayload;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AlarmViewService {

    private final AlarmAppService<?> alarmRepository;
    private final EventAppService<?> eventRepository;
    private final MonitorAppService<?> monitorRepository;
    private final DeviceAppService<?> deviceRepository;
    private final AlarmService alarmService;

    public AlarmViewService(
        AlarmAppService<?> alarmRepository,
        EventAppService<?> eventRepository,
        MonitorAppService<?> monitorRepository,
        DeviceAppService<?> deviceRepository,
        AlarmService alarmService
    ) {
        this.alarmRepository = alarmRepository;
        this.eventRepository = eventRepository;
        this.monitorRepository = monitorRepository;
        this.deviceRepository = deviceRepository;
        this.alarmService = alarmService;
    }

    public Map<String, Object> statistics(Long areaId, Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        List<AlarmVO> alarms = filterAlarms(areaId, deviceId, startTime, endTime, null);
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("pendingConfirm", countStatus(alarms, AlarmStatus.PENDING_CONFIRM));
        data.put("confirmed", countStatus(alarms, AlarmStatus.CONFIRMED));
        data.put("observing", countStatus(alarms, AlarmStatus.OBSERVING));
        data.put("falseAlarm", countStatus(alarms, AlarmStatus.FALSE_POSITIVE));
        data.put("total", alarms.size());
        return data;
    }

    public PagePayload<Map<String, Object>> list(
        Integer pageNum,
        Integer pageSize,
        Long areaId,
        Long deviceId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status
    ) {
        Integer statusCode = parseStatus(status);
        List<Map<String, Object>> rows = filterAlarms(areaId, deviceId, startTime, endTime, statusCode).stream()
            .sorted(Comparator.comparing(AlarmVO::getLastAlarmTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(this::toAlarmListRow)
            .collect(Collectors.toList());
        return paginate(rows, pageNum, pageSize);
    }

    public PagePayload<Map<String, Object>> mergeEvents(Long alarmId, Integer pageNum, Integer pageSize) {
        AlarmVO alarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        List<Map<String, Object>> rows = eventRepository.findByAlarmIdOrderByEventTimeDesc(alarm.getId()).stream()
            .filter(this::notDeleted)
            .map(this::toEventRow)
            .collect(Collectors.toList());
        return paginate(rows, pageNum, pageSize);
    }

    public Map<String, Object> detail(Long alarmId) {
        AlarmVO alarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        return toAlarmDetail(alarm);
    }

    public Map<String, Object> handle(AlarmHandleRequest request) {
        validateHandleRequest(request);
        Long alarmId = request.getAlarmIds().get(0);
        AlarmVO alarm = alarmService.handle(alarmId, request.getStatus(), request.getRemark());
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", alarm.getId());
        data.put("statusCode", alarm.getStatus());
        data.put("statusName", AlarmStatus.nameOf(alarm.getStatus()));
        data.put("handleTime", alarm.getHandleTime());
        return data;
    }

    public Map<String, Object> batchHandle(AlarmHandleRequest request) {
        validateHandleRequest(request);
        int successCount = 0;
        List<Long> failIds = new ArrayList<Long>();
        for (Long alarmId : request.getAlarmIds()) {
            try {
                alarmService.handle(alarmId, request.getStatus(), request.getRemark());
                successCount++;
            } catch (RuntimeException ex) {
                failIds.add(alarmId);
            }
        }
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("successCount", successCount);
        data.put("failCount", failIds.size());
        data.put("failIds", failIds);
        return data;
    }

    private void validateHandleRequest(AlarmHandleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getAlarmIds() == null || request.getAlarmIds().isEmpty()) {
            throw new IllegalArgumentException("alarmIds is required");
        }
        if (request.getStatus() == null) {
            throw new IllegalArgumentException("status is required");
        }
    }

    private List<AlarmVO> filterAlarms(Long areaId, Long deviceId, LocalDateTime startTime, LocalDateTime endTime, Integer statusCode) {
        Map<Long, MonitorVO> monitorMap = monitorRepository.findAllActive().stream()
            .collect(Collectors.toMap(MonitorVO::getId, monitor -> monitor));
        return alarmRepository.findAll().stream()
            .filter(this::notDeleted)
            .filter(alarm -> statusCode == null || statusCode.equals(alarm.getStatus()))
            .filter(alarm -> deviceId == null || deviceId.equals(alarm.getDeviceId()))
            .filter(withArea(areaId, monitorMap))
            .filter(alarm -> startTime == null || !safeTime(alarm).isBefore(startTime))
            .filter(alarm -> endTime == null || !safeTime(alarm).isAfter(endTime))
            .collect(Collectors.toList());
    }

    private Predicate<AlarmVO> withArea(Long areaId, Map<Long, MonitorVO> monitorMap) {
        if (areaId == null) {
            return alarm -> true;
        }
        return alarm -> {
            MonitorVO monitor = monitorMap.get(alarm.getMonitorId());
            return monitor != null && areaId.equals(monitor.getAreaId());
        };
    }

    private LocalDateTime safeTime(AlarmVO alarm) {
        return alarm.getLastAlarmTime() == null ? alarm.getUpdatedOn() : alarm.getLastAlarmTime();
    }

    private int countStatus(List<AlarmVO> alarms, int status) {
        return (int) alarms.stream().filter(alarm -> alarm.getStatus() != null && alarm.getStatus().intValue() == status).count();
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
        try {
            return Integer.valueOf(status);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
    }

    private Map<String, Object> toAlarmListRow(AlarmVO alarm) {
        MonitorVO monitor = monitorRepository.findActiveById(alarm.getMonitorId()).orElse(null);
        DeviceVO device = deviceRepository.findActiveById(alarm.getDeviceId()).orElse(null);
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", alarm.getId());
        row.put("alarmTime", alarm.getLastAlarmTime());
        row.put("alarmCount", alarm.getEventCount());
        row.put("areaId", monitor == null ? null : monitor.getAreaId());
        row.put("areaName", monitor == null ? null : monitor.getAreaName());
        row.put("deviceId", alarm.getDeviceId());
        row.put("deviceName", device == null ? null : device.getName());
        row.put("deviceType", device == null ? null : device.getDeviceType());
        row.put("alarmType", alarm.getAlarmType());
        row.put("alarmContent", alarm.getContent());
        row.put("statusCode", alarm.getStatus());
        row.put("statusName", AlarmStatus.nameOf(alarm.getStatus()));
        return row;
    }

    private Map<String, Object> toAlarmDetail(AlarmVO alarm) {
        MonitorVO monitor = monitorRepository.findActiveById(alarm.getMonitorId()).orElse(null);
        DeviceVO device = deviceRepository.findActiveById(alarm.getDeviceId()).orElse(null);
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", alarm.getId());
        row.put("alarmCode", alarm.getAlarmCode());
        row.put("alarmType", alarm.getAlarmType());
        row.put("sourceType", alarm.getSourceType());
        row.put("monitorId", alarm.getMonitorId());
        row.put("monitorName", monitor == null ? null : monitor.getName());
        row.put("deviceId", alarm.getDeviceId());
        row.put("deviceName", device == null ? null : device.getName());
        row.put("areaId", monitor == null ? null : monitor.getAreaId());
        row.put("areaName", monitor == null ? null : monitor.getAreaName());
        row.put("statusCode", alarm.getStatus());
        row.put("statusName", AlarmStatus.nameOf(alarm.getStatus()));
        row.put("firstAlarmTime", alarm.getFirstAlarmTime());
        row.put("lastAlarmTime", alarm.getLastAlarmTime());
        row.put("mergeCount", alarm.getMergeCount());
        row.put("alarmCount", alarm.getEventCount());
        row.put("alarmLevel", alarm.getAlarmLevel());
        row.put("title", alarm.getTitle());
        row.put("content", alarm.getContent());
        row.put("handler", alarm.getHandler());
        row.put("handleTime", alarm.getHandleTime());
        row.put("handleRemark", alarm.getHandleRemark());
        return row;
    }

    private Map<String, Object> toEventRow(EventVO event) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", event.getId());
        row.put("eventTime", event.getEventTime());
        row.put("eventContent", event.getContent());
        return row;
    }

    private <T> PagePayload<T> paginate(List<T> rows, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum.intValue() < 1 ? 1 : pageNum.intValue();
        int safePageSize = pageSize == null || pageSize.intValue() < 1 ? 10 : pageSize.intValue();
        int fromIndex = (safePageNum - 1) * safePageSize;
        if (fromIndex >= rows.size()) {
            return new PagePayload<T>(rows.size(), new ArrayList<T>(), safePageNum);
        }
        int toIndex = Math.min(rows.size(), fromIndex + safePageSize);
        return new PagePayload<T>(rows.size(), rows.subList(fromIndex, toIndex), safePageNum);
    }
}
