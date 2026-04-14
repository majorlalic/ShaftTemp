package com.example.demo.service;

import com.example.demo.service.AlarmService;
import com.example.demo.service.AlarmStatus;
import com.example.demo.vo.AlarmHandleRequest;
import com.example.demo.entity.AlarmEntity;
import com.example.demo.entity.DeviceEntity;
import com.example.demo.entity.EventEntity;
import com.example.demo.entity.MonitorDeviceBindEntity;
import com.example.demo.entity.MonitorEntity;
import com.example.demo.entity.ShaftFloorEntity;
import com.example.demo.dao.AlarmRepository;
import com.example.demo.dao.DeviceRepository;
import com.example.demo.dao.EventRepository;
import com.example.demo.dao.MonitorDeviceBindRepository;
import com.example.demo.dao.MonitorRepository;
import com.example.demo.dao.ShaftFloorRepository;
import com.example.demo.service.IdGenerator;
import com.example.demo.vo.PagePayload;
import com.example.demo.vo.DeviceCreateRequest;
import com.example.demo.vo.TerminalAccessConfirmRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TerminalDocService {

    private static final Set<String> TERMINAL_ALARM_TYPES = Arrays.stream(new String[] {"DEVICE_OFFLINE", "PARTITION_FAULT"})
        .collect(Collectors.toSet());

    private final DeviceRepository deviceRepository;
    private final AlarmRepository alarmRepository;
    private final EventRepository eventRepository;
    private final AlarmService alarmService;
    private final MonitorRepository monitorRepository;
    private final ShaftFloorRepository shaftFloorRepository;
    private final MonitorDeviceBindRepository monitorDeviceBindRepository;
    private final IdGenerator idGenerator;

    public TerminalDocService(
        DeviceRepository deviceRepository,
        AlarmRepository alarmRepository,
        EventRepository eventRepository,
        AlarmService alarmService,
        MonitorRepository monitorRepository,
        ShaftFloorRepository shaftFloorRepository,
        MonitorDeviceBindRepository monitorDeviceBindRepository,
        IdGenerator idGenerator
    ) {
        this.deviceRepository = deviceRepository;
        this.alarmRepository = alarmRepository;
        this.eventRepository = eventRepository;
        this.alarmService = alarmService;
        this.monitorRepository = monitorRepository;
        this.shaftFloorRepository = shaftFloorRepository;
        this.monitorDeviceBindRepository = monitorDeviceBindRepository;
        this.idGenerator = idGenerator;
    }

    public Map<String, Object> statistics(Long orgId, LocalDateTime startTime, LocalDateTime endTime) {
        List<DeviceEntity> devices = filterDevices(orgId, null, null, null, null, null).stream()
            .collect(Collectors.toList());
        long online = devices.stream().filter(device -> device.getOnlineStatus() != null && device.getOnlineStatus().intValue() == 1).count();
        long offline = devices.size() - online;
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("total", devices.size());
        data.put("online", online);
        data.put("offline", offline);
        data.put("onlineRate", devices.isEmpty() ? 0D : (online * 100.0D) / devices.size());
        data.put("startTime", startTime);
        data.put("endTime", endTime);
        return data;
    }

    public Map<String, Object> alarmStat() {
        Map<String, Long> counter = new LinkedHashMap<String, Long>();
        for (AlarmEntity alarm : terminalAlarms(null, null)) {
            counter.put(alarm.getAlarmType(), counter.getOrDefault(alarm.getAlarmType(), 0L) + 1L);
        }
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Long> entry : counter.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("alarmType", entry.getKey());
            row.put("alarmCount", entry.getValue());
            list.add(row);
        }
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("total", list.stream().mapToLong(row -> ((Number) row.get("alarmCount")).longValue()).sum());
        data.put("list", list);
        return data;
    }

    public PagePayload<Map<String, Object>> alarmList(
        Integer pageNum,
        Integer pageSize,
        String status,
        String orgName,
        String deviceType,
        LocalDateTime startTime,
        LocalDateTime endTime
    ) {
        Integer statusCode = parseStatus(status);
        int safePageNum = pageNum == null || pageNum.intValue() < 1 ? 1 : pageNum.intValue();
        int safePageSize = pageSize == null || pageSize.intValue() < 1 ? 10 : pageSize.intValue();
        int startRow = (safePageNum - 1) * safePageSize + 1;
        int endRow = safePageNum * safePageSize;
        Long total = eventRepository.countTerminalAlarmEventRows(statusCode, orgName, deviceType, startTime, endTime);
        if (total == null || total.longValue() == 0L) {
            return new PagePayload<Map<String, Object>>(0L, new ArrayList<Map<String, Object>>(), safePageNum);
        }
        List<Map<String, Object>> dbRows = eventRepository.findTerminalAlarmEventPage(
            statusCode,
            orgName,
            deviceType,
            startTime,
            endTime,
            startRow,
            endRow
        );
        List<Map<String, Object>> rows = dbRows.stream()
            .map(this::toTerminalAlarmEventRow)
            .collect(Collectors.toList());
        return new PagePayload<Map<String, Object>>(total, rows, safePageNum);
    }

    public Map<String, Object> alarmDetail(Long id) {
        AlarmEntity alarm = alarmRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + id));
        if (!TERMINAL_ALARM_TYPES.contains(alarm.getAlarmType())) {
            throw new IllegalArgumentException("Alarm is not terminal type: " + id);
        }
        List<Map<String, Object>> events = eventRepository.findByAlarmIdOrderByEventTimeDesc(id).stream()
            .filter(this::notDeleted)
            .map(this::toEventRow)
            .collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("alarm", toAlarmDetail(alarm));
        data.put("events", events);
        return data;
    }

    public Map<String, Object> handleAlarm(AlarmHandleRequest request, boolean batch) {
        if (batch) {
            return batchHandle(request);
        }
        if (request == null || request.getId() == null) {
            throw new IllegalArgumentException("id is required");
        }
        AlarmEntity alarm = applyAction(request.getId(), request.getAction(), request.getHandler(), request.getHandleRemark());
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", alarm.getId());
        data.put("statusCode", alarm.getStatus());
        data.put("statusName", AlarmStatus.nameOf(alarm.getStatus()));
        return data;
    }

    public Map<String, Object> detail(Long deviceId) {
        DeviceEntity device = deviceRepository.findActiveById(deviceId)
            .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        List<MonitorEntity> monitors = monitorDeviceBindRepository.findAllActiveByDeviceId(deviceId).stream()
            .map(bind -> monitorRepository.findActiveById(bind.getMonitorId()).orElse(null))
            .filter(monitor -> monitor != null)
            .collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("basicInfo", toDeviceInfo(device));
        data.put("monitorBindings", monitors.stream().map(this::toMonitorInfo).collect(Collectors.toList()));
        data.put("floors", shaftFloorRepository.findAllActiveByDeviceId(deviceId).stream().map(this::toFloorRow).collect(Collectors.toList()));
        return data;
    }

    @Transactional
    public Map<String, Object> createDevice(DeviceCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getIotCode() == null || request.getIotCode().trim().isEmpty()) {
            throw new IllegalArgumentException("iotCode is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        String iotCode = request.getIotCode().trim();
        if (deviceRepository.findActiveByIotCode(iotCode).isPresent()) {
            throw new IllegalArgumentException("iotCode already exists: " + iotCode);
        }
        LocalDateTime now = LocalDateTime.now();
        DeviceEntity entity = new DeviceEntity();
        entity.setId(idGenerator.nextId());
        entity.setIotCode(iotCode);
        entity.setName(request.getName().trim());
        entity.setDeviceType(request.getDeviceType());
        entity.setModel(request.getModel());
        entity.setManufacturer(request.getManufacturer());
        entity.setFactoryDate(request.getFactoryDate());
        entity.setRunDate(request.getRunDate());
        entity.setAssetStatus(
            request.getAssetStatus() == null || request.getAssetStatus().trim().isEmpty()
                ? DeviceAssetStatus.PENDING_ACCESS.value()
                : request.getAssetStatus().trim()
        );
        entity.setAreaId(request.getAreaId());
        entity.setOrgId(request.getOrgId());
        entity.setOnlineStatus(0);
        entity.setLastReportTime(null);
        entity.setLastOfflineTime(null);
        entity.setDeleted(0);
        entity.setCreatedOn(now);
        entity.setUpdatedOn(now);
        entity.setRemark(request.getRemark());
        deviceRepository.insert(entity);

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", entity.getId());
        data.put("iotCode", entity.getIotCode());
        data.put("name", entity.getName());
        data.put("assetStatus", entity.getAssetStatus());
        data.put("onlineStatus", entity.getOnlineStatus());
        return data;
    }

    public PagePayload<Map<String, Object>> ledgerList(
        Integer pageNum,
        Integer pageSize,
        String deviceType,
        String company,
        String model,
        Long orgId,
        LocalDate startDate,
        LocalDate endDate
    ) {
        List<Map<String, Object>> rows = filterDevices(orgId, deviceType, company, model, startDate, endDate).stream()
            .map(this::toLedgerRow)
            .collect(Collectors.toList());
        return paginate(rows, pageNum, pageSize);
    }

    public Map<String, Object> ledgerStat(LocalDateTime startTime, LocalDateTime endTime) {
        List<DeviceEntity> devices = deviceRepository.findAllActive().stream()
            .filter(device -> startTime == null || device.getCreatedOn() == null || !device.getCreatedOn().isBefore(startTime))
            .filter(device -> endTime == null || device.getCreatedOn() == null || !device.getCreatedOn().isAfter(endTime))
            .collect(Collectors.toList());
        Map<String, Long> deviceTypeCount = devices.stream()
            .collect(Collectors.groupingBy(device -> device.getDeviceType() == null ? "UNKNOWN" : device.getDeviceType(), LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> statusCount = devices.stream()
            .collect(Collectors.groupingBy(device -> device.getAssetStatus() == null ? "UNKNOWN" : device.getAssetStatus(), LinkedHashMap::new, Collectors.counting()));
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("total", devices.size());
        data.put("deviceTypeCount", deviceTypeCount);
        data.put("assetStatusCount", statusCount);
        data.put("startTime", startTime);
        data.put("endTime", endTime);
        return data;
    }

    public PagePayload<Map<String, Object>> accessList(
        Integer pageNum,
        Integer pageSize,
        String deviceType,
        String status,
        Long orgId,
        String manufacturer,
        String model
    ) {
        int safePageNum = pageNum == null || pageNum.intValue() < 1 ? 1 : pageNum.intValue();
        int safePageSize = pageSize == null || pageSize.intValue() < 1 ? 10 : pageSize.intValue();
        int startRow = (safePageNum - 1) * safePageSize + 1;
        int endRow = safePageNum * safePageSize;
        Long total = deviceRepository.countAccessListRows(deviceType, status, orgId, manufacturer, model);
        if (total == null || total.longValue() == 0L) {
            return new PagePayload<Map<String, Object>>(0L, new ArrayList<Map<String, Object>>(), safePageNum);
        }
        List<Map<String, Object>> rows = deviceRepository.findAccessListPage(
            deviceType,
            status,
            orgId,
            manufacturer,
            model,
            startRow,
            endRow
        ).stream()
            .map(this::toAccessRow)
            .collect(Collectors.toList());
        return new PagePayload<Map<String, Object>>(total, rows, safePageNum);
    }

    @Transactional
    public Map<String, Object> accessConfirm(TerminalAccessConfirmRequest request) {
        if (request == null || request.getDeviceIds() == null || request.getDeviceIds().isEmpty()) {
            throw new IllegalArgumentException("deviceIds is required");
        }
        int successCount = 0;
        List<Long> bindCreatedFor = new ArrayList<Long>();
        LocalDateTime now = LocalDateTime.now();
        for (Long deviceId : request.getDeviceIds()) {
            DeviceEntity device = deviceRepository.findActiveById(deviceId).orElse(null);
            if (device == null) {
                continue;
            }
            device.setAssetStatus(DeviceAssetStatus.CONNECTED.value());
            if (request.getAreaId() != null) {
                device.setAreaId(request.getAreaId());
            }
            if (request.getOrgId() != null) {
                device.setOrgId(request.getOrgId());
            }
            deviceRepository.updateById(device);
            successCount++;

            if (request.getMonitorId() != null && !monitorDeviceBindRepository.findActiveByMonitorIdAndDeviceId(request.getMonitorId(), deviceId).isPresent()) {
                MonitorDeviceBindEntity bind = new MonitorDeviceBindEntity();
                bind.setId(idGenerator.nextId());
                bind.setMonitorId(request.getMonitorId());
                bind.setDeviceId(deviceId);
                bind.setBindStatus(1);
                bind.setBindTime(now);
                bind.setCreatedOn(now);
                bind.setUpdatedOn(now);
                bind.setDeleted(0);
                monitorDeviceBindRepository.insert(bind);
                bindCreatedFor.add(deviceId);
            }
        }
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("successCount", successCount);
        data.put("bindCreatedDeviceIds", bindCreatedFor);
        return data;
    }

    private Map<String, Object> batchHandle(AlarmHandleRequest request) {
        if (request == null || request.getAlarmIds() == null || request.getAlarmIds().isEmpty()) {
            throw new IllegalArgumentException("alarmIds is required");
        }
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

    private AlarmEntity applyAction(Long alarmId, String action, Long handler, String remark) {
        AlarmEntity alarm = alarmRepository.findById(alarmId)
            .orElseThrow(() -> new IllegalArgumentException("Alarm not found: " + alarmId));
        if (!TERMINAL_ALARM_TYPES.contains(alarm.getAlarmType())) {
            throw new IllegalArgumentException("Alarm is not terminal type: " + alarmId);
        }
        if ("confirm".equals(action)) {
            return alarmService.confirm(alarmId, handler, remark);
        }
        if ("observe".equals(action)) {
            return alarmService.observe(alarmId, remark);
        }
        if ("false_positive".equals(action)) {
            return alarmService.markFalsePositive(alarmId, remark);
        }
        if ("close".equals(action)) {
            return alarmService.close(alarmId, remark);
        }
        throw new IllegalArgumentException("Unsupported action: " + action);
    }

    private List<DeviceEntity> filterDevices(
        Long orgId,
        String deviceType,
        String company,
        String model,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return deviceRepository.findAllActive().stream()
            .filter(device -> orgId == null || orgId.equals(device.getOrgId()))
            .filter(device -> deviceType == null || deviceType.equals(device.getDeviceType()))
            .filter(device -> company == null || company.equals(device.getManufacturer()))
            .filter(device -> model == null || model.equals(device.getModel()))
            .filter(device -> startDate == null || (device.getRunDate() != null && !device.getRunDate().isBefore(startDate)))
            .filter(device -> endDate == null || (device.getRunDate() != null && !device.getRunDate().isAfter(endDate)))
            .collect(Collectors.toList());
    }

    private List<AlarmEntity> terminalAlarms(Integer statusCode, Long deviceId) {
        return alarmRepository.findAll().stream()
            .filter(this::notDeleted)
            .filter(alarm -> TERMINAL_ALARM_TYPES.contains(alarm.getAlarmType()))
            .filter(alarm -> statusCode == null || statusCode.equals(alarm.getStatus()))
            .filter(alarm -> deviceId == null || deviceId.equals(parseLong(alarm.getDeviceId())))
            .collect(Collectors.toList());
    }

    private Integer parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        return Integer.valueOf(status);
    }

    private Map<String, Object> toAlarmRow(AlarmEntity alarm) {
        Long deviceId = parseLong(alarm.getDeviceId());
        DeviceEntity device = deviceId == null ? null : deviceRepository.findActiveById(deviceId).orElse(null);
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", alarm.getId());
        row.put("deviceId", alarm.getDeviceId());
        row.put("deviceName", device == null ? null : device.getName());
        row.put("deviceType", device == null ? null : device.getDeviceType());
        row.put("alarmType", alarm.getAlarmType());
        row.put("statusCode", alarm.getStatus());
        row.put("statusName", AlarmStatus.nameOf(alarm.getStatus()));
        row.put("alarmTime", alarm.getLastAlarmTime());
        row.put("alarmContent", alarm.getContent());
        row.put("alarmCount", alarm.getEventCount());
        return row;
    }

    private Map<String, Object> toTerminalAlarmEventRow(Map<String, Object> src) {
        Integer statusCode = asInteger(src.get("status_code"));
        Integer eventNo = asInteger(src.get("event_no"));
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("alarmId", src.get("alarm_id"));
        row.put("eventId", src.get("event_id"));
        row.put("deviceId", src.get("device_id"));
        row.put("deviceName", src.get("device_name"));
        row.put("orgName", src.get("org_name"));
        row.put("deviceType", src.get("device_type"));
        row.put("alarmType", src.get("alarm_type"));
        row.put("statusCode", statusCode);
        row.put("statusName", AlarmStatus.nameOf(statusCode));
        row.put("firstAlarmTime", src.get("first_alarm_time"));
        row.put("lastAlarmTime", src.get("last_alarm_time"));
        row.put("alarmCount", src.get("alarm_count"));
        row.put("eventNo", eventNo);
        row.put("eventLabel", eventNo == null ? null : ("第" + eventNo + "次告警"));
        row.put("eventTime", src.get("event_time"));
        row.put("eventType", src.get("event_type"));
        row.put("alarmContent", src.get("alarm_content"));
        return row;
    }

    private Map<String, Object> toAlarmDetail(AlarmEntity alarm) {
        Long deviceId = parseLong(alarm.getDeviceId());
        DeviceEntity device = deviceId == null ? null : deviceRepository.findActiveById(deviceId).orElse(null);
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", alarm.getId());
        row.put("alarmCode", alarm.getAlarmCode());
        row.put("alarmType", alarm.getAlarmType());
        row.put("sourceType", alarm.getSourceType());
        row.put("deviceId", alarm.getDeviceId());
        row.put("deviceName", device == null ? null : device.getName());
        row.put("deviceType", device == null ? null : device.getDeviceType());
        row.put("statusCode", alarm.getStatus());
        row.put("statusName", AlarmStatus.nameOf(alarm.getStatus()));
        row.put("firstAlarmTime", alarm.getFirstAlarmTime());
        row.put("lastAlarmTime", alarm.getLastAlarmTime());
        row.put("mergeCount", alarm.getMergeCount());
        row.put("alarmCount", alarm.getEventCount());
        row.put("content", alarm.getContent());
        row.put("handler", alarm.getHandler());
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

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return Integer.valueOf(text);
    }

    private Map<String, Object> toEventRow(EventEntity event) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", event.getId());
        row.put("eventTime", event.getEventTime());
        row.put("eventType", event.getEventType());
        row.put("content", event.getContent());
        return row;
    }

    private Map<String, Object> toDeviceInfo(DeviceEntity device) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", device.getId());
        row.put("name", device.getName());
        row.put("iotCode", device.getIotCode());
        row.put("deviceType", device.getDeviceType());
        row.put("model", device.getModel());
        row.put("manufacturer", device.getManufacturer());
        row.put("factoryDate", device.getFactoryDate());
        row.put("runDate", device.getRunDate());
        row.put("assetStatus", device.getAssetStatus());
        row.put("onlineStatus", device.getOnlineStatus());
        row.put("orgId", device.getOrgId());
        row.put("areaId", device.getAreaId());
        row.put("remark", device.getRemark());
        return row;
    }

    private Map<String, Object> toMonitorInfo(MonitorEntity monitor) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", monitor.getId());
        row.put("name", monitor.getName());
        row.put("areaId", monitor.getAreaId());
        row.put("areaName", monitor.getAreaName());
        row.put("shaftType", monitor.getShaftType());
        return row;
    }

    private Map<String, Object> toFloorRow(ShaftFloorEntity floor) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", floor.getId());
        row.put("name", floor.getName());
        row.put("startPoint", floor.getStartPoint());
        row.put("endPoint", floor.getEndPoint());
        return row;
    }

    private Map<String, Object> toLedgerRow(DeviceEntity device) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", device.getId());
        row.put("name", device.getName());
        row.put("iotCode", device.getIotCode());
        row.put("deviceType", device.getDeviceType());
        row.put("company", device.getManufacturer());
        row.put("model", device.getModel());
        row.put("factoryDate", device.getFactoryDate());
        row.put("runDate", device.getRunDate());
        row.put("assetStatus", device.getAssetStatus());
        row.put("orgId", device.getOrgId());
        row.put("areaId", device.getAreaId());
        return row;
    }

    private Map<String, Object> toAccessRow(Map<String, Object> src) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", src.get("id"));
        row.put("iotCode", src.get("iot_code"));
        row.put("name", src.get("name"));
        row.put("deviceType", src.get("device_type"));
        row.put("model", src.get("model"));
        row.put("manufacturer", src.get("manufacturer"));
        row.put("factoryDate", src.get("factory_date"));
        row.put("runDate", src.get("run_date"));
        row.put("assetStatus", src.get("asset_status"));
        row.put("areaId", src.get("area_id"));
        row.put("orgId", src.get("org_id"));
        row.put("onlineStatus", src.get("online_status"));
        row.put("lastReportTime", src.get("last_report_time"));
        row.put("lastOfflineTime", src.get("last_offline_time"));
        row.put("remark", src.get("remark"));
        row.put("createdOn", src.get("created_on"));
        row.put("updatedOn", src.get("updated_on"));
        row.put("monitorId", src.get("monitor_id"));
        row.put("monitorName", src.get("monitor_name"));
        return row;
    }

    private boolean notDeleted(AlarmEntity alarm) {
        return alarm.getDeleted() == null || alarm.getDeleted().intValue() == 0;
    }

    private boolean notDeleted(EventEntity event) {
        return event.getDeleted() == null || event.getDeleted().intValue() == 0;
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
