package com.example.demo.service;

import com.example.demo.entity.AlarmEntity;
import com.example.demo.entity.AreaEntity;
import com.example.demo.entity.DeviceEntity;
import com.example.demo.entity.MonitorEntity;
import com.example.demo.entity.MonitorPartitionBindEntity;
import com.example.demo.entity.ShaftFloorEntity;
import com.example.demo.dao.AlarmRepository;
import com.example.demo.dao.AreaRepository;
import com.example.demo.dao.DeviceRepository;
import com.example.demo.dao.MonitorPartitionBindRepository;
import com.example.demo.dao.MonitorRepository;
import com.example.demo.dao.ShaftFloorRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MonitorDocService {

    private final MonitorRepository monitorRepository;
    private final DeviceRepository deviceRepository;
    private final ShaftFloorRepository shaftFloorRepository;
    private final MonitorPartitionBindRepository monitorPartitionBindRepository;
    private final AlarmRepository alarmRepository;
    private final AreaRepository areaRepository;

    public MonitorDocService(
        MonitorRepository monitorRepository,
        DeviceRepository deviceRepository,
        ShaftFloorRepository shaftFloorRepository,
        MonitorPartitionBindRepository monitorPartitionBindRepository,
        AlarmRepository alarmRepository,
        AreaRepository areaRepository
    ) {
        this.monitorRepository = monitorRepository;
        this.deviceRepository = deviceRepository;
        this.shaftFloorRepository = shaftFloorRepository;
        this.monitorPartitionBindRepository = monitorPartitionBindRepository;
        this.alarmRepository = alarmRepository;
        this.areaRepository = areaRepository;
    }

    public Map<String, Object> detail(Long monitorId) {
        MonitorEntity monitor = monitorRepository.findActiveById(monitorId)
            .orElseThrow(() -> new IllegalArgumentException("Monitor not found: " + monitorId));
        DeviceEntity device = monitor.getDeviceId() == null ? null : deviceRepository.findActiveById(monitor.getDeviceId()).orElse(null);

        Map<Long, String> floorNameMap = shaftFloorRepository.findAllActiveByMonitorId(monitorId).stream()
            .collect(Collectors.toMap(ShaftFloorEntity::getId, ShaftFloorEntity::getName));

        List<Map<String, Object>> floors = shaftFloorRepository.findAllActiveByMonitorId(monitorId).stream()
            .map(this::toFloorMap)
            .collect(Collectors.toList());
        List<Map<String, Object>> partitionBinds = monitorPartitionBindRepository.findAllActiveByMonitorId(monitorId).stream()
            .map(bind -> toPartitionBindMap(bind, floorNameMap.get(bind.getShaftFloorId())))
            .collect(Collectors.toList());
        List<Map<String, Object>> recentAlarms = alarmRepository.findAll().stream()
            .filter(this::notDeleted)
            .filter(alarm -> monitorId.equals(alarm.getMonitorId()))
            .sorted(Comparator.comparing(AlarmEntity::getLastAlarmTime, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5)
            .map(this::toRecentAlarmMap)
            .collect(Collectors.toList());

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("basicInfo", toMonitorMap(monitor));
        data.put("deviceInfo", device == null ? null : toDeviceMap(device));
        data.put("floors", floors);
        data.put("partitionBinds", partitionBinds);
        data.put("recentAlarms", recentAlarms);
        return data;
    }

    public Map<String, Object> statistics(Long areaTreeId) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        Long monitorDeviceTotal;
        Long monitorAlarmTotal;
        Long todayNewAlarm;
        Long todayRecoveredAlarm;
        if (areaTreeId == null) {
            monitorDeviceTotal = monitorRepository.countMonitorDeviceTotal();
            monitorAlarmTotal = alarmRepository.countMonitorAlarmTotal();
            todayNewAlarm = alarmRepository.countTodayNewAlarm(todayStart, tomorrowStart);
            todayRecoveredAlarm = alarmRepository.countTodayRecoveredAlarm(todayStart, tomorrowStart);
        } else {
            monitorDeviceTotal = monitorRepository.countMonitorDeviceTotalByAreaTree(areaTreeId);
            monitorAlarmTotal = alarmRepository.countMonitorAlarmTotalByAreaTree(areaTreeId);
            todayNewAlarm = alarmRepository.countTodayNewAlarmByAreaTree(areaTreeId, todayStart, tomorrowStart);
            todayRecoveredAlarm = alarmRepository.countTodayRecoveredAlarmByAreaTree(areaTreeId, todayStart, tomorrowStart);
        }
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("areaTreeId", areaTreeId);
        data.put("monitorDeviceTotal", monitorDeviceTotal == null ? 0L : monitorDeviceTotal);
        data.put("monitorAlarmTotal", monitorAlarmTotal == null ? 0L : monitorAlarmTotal);
        data.put("todayNewAlarm", todayNewAlarm == null ? 0L : todayNewAlarm);
        data.put("todayRecoveredAlarm", todayRecoveredAlarm == null ? 0L : todayRecoveredAlarm);
        return data;
    }

    public Map<String, Object> listByAreaTree(Long areaTreeId) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        String mode = "SHAFT_MONITOR";
        String areaType = null;

        if (areaTreeId == null) {
            for (MonitorEntity monitor : monitorRepository.findAllActive()) {
                rows.add(toMonitorListRow(monitor));
            }
        } else {
            AreaEntity area = areaRepository.findActiveById(areaTreeId).orElse(null);
            areaType = area == null ? null : area.getType();
            if (isShaftArea(areaType)) {
                mode = "SHAFT_FLOOR";
                for (ShaftFloorEntity floor : shaftFloorRepository.findAllActiveByAreaTreeId(areaTreeId)) {
                    rows.add(toFloorListRow(floor));
                }
            } else {
                for (MonitorEntity monitor : monitorRepository.findAllActiveByAreaTreeId(areaTreeId)) {
                    rows.add(toMonitorListRow(monitor));
                }
            }
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("areaTreeId", areaTreeId);
        data.put("areaType", areaType);
        data.put("mode", mode);
        data.put("total", rows.size());
        data.put("list", rows);
        return data;
    }

    private Map<String, Object> toMonitorMap(MonitorEntity monitor) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", monitor.getId());
        row.put("name", monitor.getName());
        row.put("areaId", monitor.getAreaId());
        row.put("areaName", monitor.getAreaName());
        row.put("elevatorCount", monitor.getElevatorCount());
        row.put("shaftType", monitor.getShaftType());
        row.put("monitorStatus", monitor.getMonitorStatus());
        row.put("buildDate", monitor.getBuildDate());
        row.put("ownerCompany", monitor.getOwnerCompany());
        row.put("deviceId", monitor.getDeviceId());
        row.put("remark", monitor.getRemark());
        return row;
    }

    private Map<String, Object> toDeviceMap(DeviceEntity device) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", device.getId());
        row.put("name", device.getName());
        row.put("iotCode", device.getIotCode());
        row.put("deviceType", device.getDeviceType());
        row.put("model", device.getModel());
        row.put("manufacturer", device.getManufacturer());
        row.put("assetStatus", device.getAssetStatus());
        row.put("onlineStatus", device.getOnlineStatus());
        row.put("lastReportTime", device.getLastReportTime());
        return row;
    }

    private Map<String, Object> toFloorMap(ShaftFloorEntity floor) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", floor.getId());
        row.put("name", floor.getName());
        row.put("areaId", floor.getAreaId());
        row.put("deviceId", floor.getDeviceId());
        row.put("startPoint", floor.getStartPoint());
        row.put("endPoint", floor.getEndPoint());
        row.put("sort", floor.getSort());
        return row;
    }

    private Map<String, Object> toPartitionBindMap(MonitorPartitionBindEntity bind, String floorName) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", bind.getId());
        row.put("shaftFloorId", bind.getShaftFloorId());
        row.put("shaftFloorName", floorName);
        row.put("deviceId", bind.getDeviceId());
        row.put("partitionCode", bind.getPartitionCode());
        row.put("partitionName", bind.getPartitionName());
        row.put("dataReference", bind.getDataReference());
        row.put("deviceToken", bind.getDeviceToken());
        row.put("partitionNo", bind.getPartitionNo());
        return row;
    }

    private Map<String, Object> toRecentAlarmMap(AlarmEntity alarm) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", alarm.getId());
        row.put("alarmType", alarm.getAlarmType());
        row.put("statusCode", alarm.getStatus());
        row.put("lastAlarmTime", alarm.getLastAlarmTime());
        row.put("content", alarm.getContent());
        return row;
    }

    private Map<String, Object> toMonitorListRow(MonitorEntity monitor) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("objectType", "SHAFT_MONITOR");
        row.put("id", monitor.getId());
        row.put("name", monitor.getName());
        row.put("areaId", monitor.getAreaId());
        row.put("areaName", monitor.getAreaName());
        row.put("shaftType", monitor.getShaftType());
        row.put("monitorStatus", monitor.getMonitorStatus());
        row.put("deviceId", monitor.getDeviceId());
        return row;
    }

    private Map<String, Object> toFloorListRow(ShaftFloorEntity floor) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("objectType", "SHAFT_FLOOR");
        row.put("id", floor.getId());
        row.put("name", floor.getName());
        row.put("areaId", floor.getAreaId());
        row.put("monitorId", floor.getMonitorId());
        row.put("deviceId", floor.getDeviceId());
        row.put("startPoint", floor.getStartPoint());
        row.put("endPoint", floor.getEndPoint());
        row.put("sort", floor.getSort());
        return row;
    }

    private boolean isShaftArea(String areaType) {
        if (areaType == null) {
            return false;
        }
        String normalized = areaType.trim().toUpperCase();
        return "SHAFT".equals(normalized) || "ELEVATOR_SHAFT".equals(normalized) || "VERTICAL_SHAFT".equals(normalized);
    }

    private boolean notDeleted(AlarmEntity alarm) {
        return alarm.getDeleted() == null || alarm.getDeleted().intValue() == 0;
    }
}
