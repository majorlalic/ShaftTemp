package com.example.demo.service;

import com.example.demo.service.AlarmService;
import com.example.demo.service.AlarmStatus;
import com.example.demo.entity.AlarmEntity;
import com.example.demo.entity.DeviceEntity;
import com.example.demo.entity.EventEntity;
import com.example.demo.entity.MonitorEntity;
import com.example.demo.dao.AlarmRepository;
import com.example.demo.dao.DeviceRepository;
import com.example.demo.dao.EventRepository;
import com.example.demo.dao.MonitorRepository;
import com.example.demo.vo.AlarmHandleRequest;
import com.example.demo.vo.PagePayload;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AlarmViewService {

    private static final Set<String> TERMINAL_ALARM_TYPES = new HashSet<String>(
        Arrays.asList("DEVICE_OFFLINE", "PARTITION_FAULT")
    );
    private static final Set<String> MONITOR_ALARM_TYPES = new HashSet<String>(
        Arrays.asList("TEMP_THRESHOLD", "TEMP_DIFFERENCE", "TEMP_RISE_RATE")
    );
    private static final Map<Integer, Set<String>> ALARM_TYPE_GROUPS = new LinkedHashMap<Integer, Set<String>>();

    static {
        ALARM_TYPE_GROUPS.put(Integer.valueOf(0), TERMINAL_ALARM_TYPES);
        ALARM_TYPE_GROUPS.put(Integer.valueOf(1), MONITOR_ALARM_TYPES);
    }

    private final AlarmRepository alarmRepository;
    private final EventRepository eventRepository;
    private final MonitorRepository monitorRepository;
    private final DeviceRepository deviceRepository;
    private final AlarmService alarmService;

    public AlarmViewService(
        AlarmRepository alarmRepository,
        EventRepository eventRepository,
        MonitorRepository monitorRepository,
        DeviceRepository deviceRepository,
        AlarmService alarmService
    ) {
        this.alarmRepository = alarmRepository;
        this.eventRepository = eventRepository;
        this.monitorRepository = monitorRepository;
        this.deviceRepository = deviceRepository;
        this.alarmService = alarmService;
    }

    public Map<String, Object> statistics(Long areaId, Long deviceId, LocalDateTime startTime, LocalDateTime endTime, String domain) {
        List<AlarmEntity> alarms = filterAlarms(areaId, null, deviceId, startTime, endTime, null, null, domain);
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
        String areaName,
        Long deviceId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String status,
        Integer alarmTypeBig,
        Integer type
    ) {
        Integer statusCode = parseStatus(status);
        List<Map<String, Object>> rows = filterAlarms(areaId, areaName, deviceId, startTime, endTime, statusCode, alarmTypeBig, null).stream()
            .filter(withType(type))
            .sorted(Comparator.comparing(AlarmEntity::getLastAlarmTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(this::toAlarmListRow)
            .collect(Collectors.toList());
        return paginate(rows, pageNum, pageSize);
    }

    public PagePayload<Map<String, Object>> mergeEvents(Long alarmId, Integer pageNum, Integer pageSize) {
        AlarmEntity alarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        List<Map<String, Object>> rows = eventRepository.findByAlarmIdOrderByEventTimeDesc(parseLong(alarm.getId())).stream()
            .filter(this::notDeleted)
            .map(this::toEventRow)
            .collect(Collectors.toList());
        return paginate(rows, pageNum, pageSize);
    }

    public Map<String, Object> detail(Long alarmId) {
        AlarmEntity alarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        return toAlarmDetail(alarm);
    }

    public Map<String, Object> handle(AlarmHandleRequest request) {
        validateActionRequest(request, true);
        AlarmEntity alarm = applyAction(request.getId(), request.getAction(), request.getHandler(), request.getHandleRemark());
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", alarm.getId());
        data.put("statusCode", alarm.getStatus());
        data.put("statusName", AlarmStatus.nameOf(alarm.getStatus()));
        data.put("handleTime", alarm.getHandleTime());
        return data;
    }

    public Map<String, Object> batchHandle(AlarmHandleRequest request) {
        validateActionRequest(request, false);
        int successCount = 0;
        List<Long> failIds = new ArrayList<Long>();
        for (Long alarmId : request.getAlarmIds()) {
            try {
                applyAction(alarmId, request.getAction(), request.getHandler(), request.getHandleRemark());
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

    private AlarmEntity applyAction(Long alarmId, String action, Long handler, String handleRemark) {
        if ("confirm".equals(action)) {
            return alarmService.confirm(alarmId, handler, handleRemark);
        }
        if ("observe".equals(action)) {
            return alarmService.observe(alarmId, handleRemark);
        }
        if ("false_positive".equals(action)) {
            return alarmService.markFalsePositive(alarmId, handleRemark);
        }
        if ("close".equals(action)) {
            return alarmService.close(alarmId, handleRemark);
        }
        throw new IllegalArgumentException("Unsupported action: " + action);
    }

    private void validateActionRequest(AlarmHandleRequest request, boolean single) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (single) {
            if (request.getId() == null) {
                throw new IllegalArgumentException("id is required");
            }
        } else if (request.getAlarmIds() == null || request.getAlarmIds().isEmpty()) {
            throw new IllegalArgumentException("alarmIds is required");
        }
        if (request.getAction() == null || request.getAction().trim().isEmpty()) {
            throw new IllegalArgumentException("action is required");
        }
    }

    private List<AlarmEntity> filterAlarms(
        Long areaId,
        String areaName,
        Long deviceId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer statusCode,
        Integer alarmTypeBig,
        String domain
    ) {
        Map<Long, MonitorEntity> monitorMap = monitorRepository.findAllActive().stream()
            .collect(Collectors.toMap(MonitorEntity::getId, monitor -> monitor));
        return alarmRepository.findAll().stream()
            .filter(this::notDeleted)
            .filter(alarm -> statusCode == null || statusCode.equals(alarm.getStatus()))
            .filter(alarm -> alarmTypeBig == null || alarmTypeBig.equals(alarm.getAlarmTypeBig()))
            .filter(withDomain(domain))
            .filter(withDeviceId(deviceId))
            .filter(withArea(areaId, monitorMap))
            .filter(withAreaNameLike(areaName))
            .filter(alarm -> startTime == null || !safeTime(alarm).isBefore(startTime))
            .filter(alarm -> endTime == null || !safeTime(alarm).isAfter(endTime))
            .collect(Collectors.toList());
    }

    private Predicate<AlarmEntity> withAreaNameLike(String areaName) {
        if (areaName == null || areaName.trim().isEmpty()) {
            return alarm -> true;
        }
        String keyword = areaName.trim().toLowerCase();
        return alarm -> alarm.getAreaName() != null && alarm.getAreaName().toLowerCase().contains(keyword);
    }

    private Predicate<AlarmEntity> withDeviceId(Long deviceId) {
        if (deviceId == null) {
            return alarm -> true;
        }
        return alarm -> {
            Long alarmDeviceId = parseLong(alarm.getDeviceId());
            return alarmDeviceId != null && deviceId.equals(alarmDeviceId);
        };
    }

    private Predicate<AlarmEntity> withDomain(String domain) {
        if (domain == null || domain.trim().isEmpty() || "all".equalsIgnoreCase(domain)) {
            return alarm -> true;
        }
        if ("device".equalsIgnoreCase(domain)) {
            return alarm -> TERMINAL_ALARM_TYPES.contains(alarm.getAlarmType());
        }
        if ("monitor".equalsIgnoreCase(domain)) {
            return alarm -> !TERMINAL_ALARM_TYPES.contains(alarm.getAlarmType());
        }
        throw new IllegalArgumentException("Invalid domain: " + domain + ", supported values: monitor, device, all");
    }

    private Predicate<AlarmEntity> withType(Integer type) {
        if (type == null) {
            return alarm -> true;
        }
        Set<String> alarmTypes = ALARM_TYPE_GROUPS.get(type);
        if (alarmTypes == null) {
            throw new IllegalArgumentException(
                "Invalid type: " + type + ", supported values: " + ALARM_TYPE_GROUPS.keySet()
            );
        }
        return alarm -> alarmTypes.contains(alarm.getAlarmType());
    }

    private Predicate<AlarmEntity> withArea(Long areaId, Map<Long, MonitorEntity> monitorMap) {
        if (areaId == null) {
            return alarm -> true;
        }
        return alarm -> {
            MonitorEntity monitor = monitorMap.get(parseLong(alarm.getMonitorId()));
            return monitor != null && areaId.equals(monitor.getAreaId());
        };
    }

    private LocalDateTime safeTime(AlarmEntity alarm) {
        return alarm.getLastAlarmTime() == null ? alarm.getUpdatedOn() : alarm.getLastAlarmTime();
    }

    private int countStatus(List<AlarmEntity> alarms, int status) {
        return (int) alarms.stream().filter(alarm -> alarm.getStatus() != null && alarm.getStatus().intValue() == status).count();
    }

    private boolean notDeleted(AlarmEntity alarm) {
        return alarm.getDeleted() == null || alarm.getDeleted().intValue() == 0;
    }

    private boolean notDeleted(EventEntity event) {
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

    private Map<String, Object> toAlarmListRow(AlarmEntity alarm) {
        MonitorEntity monitor = monitorRepository.findActiveById(parseLong(alarm.getMonitorId())).orElse(null);
        DeviceEntity device = deviceRepository.findActiveById(parseLong(alarm.getDeviceId())).orElse(null);
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", alarm.getId());
        row.put("alarmTime", alarm.getLastAlarmTime());
        row.put("alarmCount", alarm.getEventCount());
        row.put("areaId", monitor == null ? null : monitor.getAreaId());
        row.put("areaName", alarm.getAreaName() == null ? (monitor == null ? null : monitor.getAreaName()) : alarm.getAreaName());
        row.put("deviceId", alarm.getDeviceId());
        row.put("deviceName", alarm.getDeviceName() == null ? (device == null ? null : device.getName()) : alarm.getDeviceName());
        row.put("deviceType", device == null ? null : device.getDeviceType());
        row.put("alarmTypeBig", alarm.getAlarmTypeBig());
        row.put("alarmTypeBigName", AlarmTypeBig.nameOf(alarm.getAlarmTypeBig()));
        row.put("alarmType", alarm.getAlarmType());
        row.put("alarmContent", alarm.getContent());
        row.put("statusCode", alarm.getStatus());
        row.put("statusName", AlarmStatus.nameOf(alarm.getStatus()));
        return row;
    }

    private Map<String, Object> toAlarmDetail(AlarmEntity alarm) {
        MonitorEntity monitor = monitorRepository.findActiveById(parseLong(alarm.getMonitorId())).orElse(null);
        DeviceEntity device = deviceRepository.findActiveById(parseLong(alarm.getDeviceId())).orElse(null);
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", alarm.getId());
        row.put("alarmCode", alarm.getAlarmCode());
        row.put("alarmType", alarm.getAlarmType());
        row.put("alarmTypeBig", alarm.getAlarmTypeBig());
        row.put("alarmTypeBigName", AlarmTypeBig.nameOf(alarm.getAlarmTypeBig()));
        row.put("sourceType", alarm.getSourceType());
        row.put("monitorId", alarm.getMonitorId());
        row.put("monitorName", alarm.getMonitorName() == null ? (monitor == null ? null : monitor.getName()) : alarm.getMonitorName());
        row.put("deviceId", alarm.getDeviceId());
        row.put("deviceName", alarm.getDeviceName() == null ? (device == null ? null : device.getName()) : alarm.getDeviceName());
        row.put("areaId", monitor == null ? null : monitor.getAreaId());
        row.put("areaName", alarm.getAreaName() == null ? (monitor == null ? null : monitor.getAreaName()) : alarm.getAreaName());
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
        row.put("handlerName", alarm.getHandlerName());
        row.put("manufacturer", alarm.getManufacturer());
        row.put("deviceModel", alarm.getDeviceModel());
        row.put("pushTime", alarm.getPushTime());
        row.put("handleTime", alarm.getHandleTime());
        row.put("handleRemark", alarm.getHandleRemark());
        return row;
    }

    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private Map<String, Object> toEventRow(EventEntity event) {
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
