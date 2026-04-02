package com.example.demo.service;

import com.example.demo.service.AlarmService;
import com.example.demo.service.AlarmStatus;
import com.example.demo.vo.AlarmHandleRequest;
import com.csg.dgri.szsiom.sysmanage.model.AlarmVO;
import com.csg.dgri.szsiom.sysmanage.model.DeviceVO;
import com.csg.dgri.szsiom.sysmanage.model.EventVO;
import com.csg.dgri.szsiom.sysmanage.model.MonitorDeviceBindVO;
import com.csg.dgri.szsiom.sysmanage.model.MonitorVO;
import com.csg.dgri.szsiom.sysmanage.model.ShaftFloorVO;
import com.csg.dgri.szsiom.sysmanage.appservice.AlarmAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.DeviceAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.EventAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.MonitorDeviceBindAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.MonitorAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.ShaftFloorAppService;
import com.example.demo.service.IdGenerator;
import com.example.demo.vo.PagePayload;
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

    private final DeviceAppService<?> deviceRepository;
    private final AlarmAppService<?> alarmRepository;
    private final EventAppService<?> eventRepository;
    private final AlarmService alarmService;
    private final MonitorAppService<?> monitorRepository;
    private final ShaftFloorAppService<?> shaftFloorRepository;
    private final MonitorDeviceBindAppService<?> monitorDeviceBindRepository;
    private final IdGenerator idGenerator;

    public TerminalDocService(
        DeviceAppService<?> deviceRepository,
        AlarmAppService<?> alarmRepository,
        EventAppService<?> eventRepository,
        AlarmService alarmService,
        MonitorAppService<?> monitorRepository,
        ShaftFloorAppService<?> shaftFloorRepository,
        MonitorDeviceBindAppService<?> monitorDeviceBindRepository,
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
        List<DeviceVO> devices = filterDevices(orgId, null, null, null, null, null).stream()
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
        for (AlarmVO alarm : terminalAlarms(null, null)) {
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

    public PagePayload<Map<String, Object>> alarmList(Integer pageNum, Integer pageSize, String status) {
        Integer statusCode = parseStatus(status);
        List<Map<String, Object>> rows = terminalAlarms(statusCode, null).stream()
            .sorted(Comparator.comparing(AlarmVO::getLastAlarmTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(this::toAlarmRow)
            .collect(Collectors.toList());
        return paginate(rows, pageNum, pageSize);
    }

    public Map<String, Object> alarmDetail(Long id) {
        AlarmVO alarm = alarmRepository.findById(id)
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
        AlarmVO alarm = applyAction(request.getId(), request.getAction(), request.getHandler(), request.getHandleRemark());
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", alarm.getId());
        data.put("statusCode", alarm.getStatus());
        data.put("statusName", AlarmStatus.nameOf(alarm.getStatus()));
        return data;
    }

    public Map<String, Object> detail(Long deviceId) {
        DeviceVO device = deviceRepository.findActiveById(deviceId)
            .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));
        List<MonitorVO> monitors = monitorDeviceBindRepository.findAllActiveByDeviceId(deviceId).stream()
            .map(bind -> monitorRepository.findActiveById(bind.getMonitorId()).orElse(null))
            .filter(monitor -> monitor != null)
            .collect(Collectors.toList());
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("basicInfo", toDeviceInfo(device));
        data.put("monitorBindings", monitors.stream().map(this::toMonitorInfo).collect(Collectors.toList()));
        data.put("floors", shaftFloorRepository.findAllActiveByDeviceId(deviceId).stream().map(this::toFloorRow).collect(Collectors.toList()));
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

    public Map<String, Object> ledgerStat() {
        List<DeviceVO> devices = deviceRepository.findAllActive();
        Map<String, Long> deviceTypeCount = devices.stream()
            .collect(Collectors.groupingBy(device -> device.getDeviceType() == null ? "UNKNOWN" : device.getDeviceType(), LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> statusCount = devices.stream()
            .collect(Collectors.groupingBy(device -> device.getAssetStatus() == null ? "UNKNOWN" : device.getAssetStatus(), LinkedHashMap::new, Collectors.counting()));
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("total", devices.size());
        data.put("deviceTypeCount", deviceTypeCount);
        data.put("assetStatusCount", statusCount);
        return data;
    }

    public PagePayload<Map<String, Object>> accessList(Integer pageNum, Integer pageSize, String status) {
        List<Map<String, Object>> rows = deviceRepository.findAllActive().stream()
            .filter(device -> status == null || status.trim().isEmpty() || status.equals(device.getAssetStatus()))
            .map(this::toAccessRow)
            .collect(Collectors.toList());
        return paginate(rows, pageNum, pageSize);
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
            DeviceVO device = deviceRepository.findActiveById(deviceId).orElse(null);
            if (device == null) {
                continue;
            }
            device.setAssetStatus("CONNECTED");
            if (request.getAreaId() != null) {
                device.setAreaId(request.getAreaId());
            }
            if (request.getOrgId() != null) {
                device.setOrgId(request.getOrgId());
            }
            deviceRepository.updateById(device);
            successCount++;

            if (request.getMonitorId() != null && !monitorDeviceBindRepository.findActiveByMonitorIdAndDeviceId(request.getMonitorId(), deviceId).isPresent()) {
                MonitorDeviceBindVO bind = new MonitorDeviceBindVO();
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

    private AlarmVO applyAction(Long alarmId, String action, Long handler, String remark) {
        AlarmVO alarm = alarmRepository.findById(alarmId)
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

    private List<DeviceVO> filterDevices(
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

    private List<AlarmVO> terminalAlarms(Integer statusCode, Long deviceId) {
        return alarmRepository.findAll().stream()
            .filter(this::notDeleted)
            .filter(alarm -> TERMINAL_ALARM_TYPES.contains(alarm.getAlarmType()))
            .filter(alarm -> statusCode == null || statusCode.equals(alarm.getStatus()))
            .filter(alarm -> deviceId == null || deviceId.equals(alarm.getDeviceId()))
            .collect(Collectors.toList());
    }

    private Integer parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        return Integer.valueOf(status);
    }

    private Map<String, Object> toAlarmRow(AlarmVO alarm) {
        DeviceVO device = alarm.getDeviceId() == null ? null : deviceRepository.findActiveById(alarm.getDeviceId()).orElse(null);
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

    private Map<String, Object> toAlarmDetail(AlarmVO alarm) {
        DeviceVO device = alarm.getDeviceId() == null ? null : deviceRepository.findActiveById(alarm.getDeviceId()).orElse(null);
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

    private Map<String, Object> toEventRow(EventVO event) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", event.getId());
        row.put("eventTime", event.getEventTime());
        row.put("eventType", event.getEventType());
        row.put("content", event.getContent());
        return row;
    }

    private Map<String, Object> toDeviceInfo(DeviceVO device) {
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

    private Map<String, Object> toMonitorInfo(MonitorVO monitor) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", monitor.getId());
        row.put("name", monitor.getName());
        row.put("areaId", monitor.getAreaId());
        row.put("areaName", monitor.getAreaName());
        row.put("shaftType", monitor.getShaftType());
        return row;
    }

    private Map<String, Object> toFloorRow(ShaftFloorVO floor) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", floor.getId());
        row.put("name", floor.getName());
        row.put("startPoint", floor.getStartPoint());
        row.put("endPoint", floor.getEndPoint());
        return row;
    }

    private Map<String, Object> toLedgerRow(DeviceVO device) {
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

    private Map<String, Object> toAccessRow(DeviceVO device) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", device.getId());
        row.put("name", device.getName());
        row.put("deviceType", device.getDeviceType());
        row.put("model", device.getModel());
        row.put("assetStatus", device.getAssetStatus());
        row.put("orgId", device.getOrgId());
        row.put("areaId", device.getAreaId());
        return row;
    }

    private boolean notDeleted(AlarmVO alarm) {
        return alarm.getDeleted() == null || alarm.getDeleted().intValue() == 0;
    }

    private boolean notDeleted(EventVO event) {
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
