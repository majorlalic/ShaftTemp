package com.example.demo.service;

import com.csg.dgri.szsiom.sysmanage.model.AlarmVO;
import com.csg.dgri.szsiom.sysmanage.model.DeviceVO;
import com.csg.dgri.szsiom.sysmanage.model.MonitorVO;
import com.csg.dgri.szsiom.sysmanage.model.MonitorPartitionBindVO;
import com.csg.dgri.szsiom.sysmanage.model.ShaftFloorVO;
import com.csg.dgri.szsiom.sysmanage.appservice.AlarmAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.DeviceAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.MonitorPartitionBindAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.MonitorAppService;
import com.csg.dgri.szsiom.sysmanage.appservice.ShaftFloorAppService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MonitorDocService {

    private final MonitorAppService<?> monitorRepository;
    private final DeviceAppService<?> deviceRepository;
    private final ShaftFloorAppService<?> shaftFloorRepository;
    private final MonitorPartitionBindAppService<?> monitorPartitionBindRepository;
    private final AlarmAppService<?> alarmRepository;

    public MonitorDocService(
        MonitorAppService<?> monitorRepository,
        DeviceAppService<?> deviceRepository,
        ShaftFloorAppService<?> shaftFloorRepository,
        MonitorPartitionBindAppService<?> monitorPartitionBindRepository,
        AlarmAppService<?> alarmRepository
    ) {
        this.monitorRepository = monitorRepository;
        this.deviceRepository = deviceRepository;
        this.shaftFloorRepository = shaftFloorRepository;
        this.monitorPartitionBindRepository = monitorPartitionBindRepository;
        this.alarmRepository = alarmRepository;
    }

    public Map<String, Object> detail(Long monitorId) {
        MonitorVO monitor = monitorRepository.findActiveById(monitorId)
            .orElseThrow(() -> new IllegalArgumentException("Monitor not found: " + monitorId));
        DeviceVO device = monitor.getDeviceId() == null ? null : deviceRepository.findActiveById(monitor.getDeviceId()).orElse(null);

        Map<Long, String> floorNameMap = shaftFloorRepository.findAllActiveByMonitorId(monitorId).stream()
            .collect(Collectors.toMap(ShaftFloorVO::getId, ShaftFloorVO::getName));

        List<Map<String, Object>> floors = shaftFloorRepository.findAllActiveByMonitorId(monitorId).stream()
            .map(this::toFloorMap)
            .collect(Collectors.toList());
        List<Map<String, Object>> partitionBinds = monitorPartitionBindRepository.findAllActiveByMonitorId(monitorId).stream()
            .map(bind -> toPartitionBindMap(bind, floorNameMap.get(bind.getShaftFloorId())))
            .collect(Collectors.toList());
        List<Map<String, Object>> recentAlarms = alarmRepository.findAll().stream()
            .filter(this::notDeleted)
            .filter(alarm -> monitorId.equals(alarm.getMonitorId()))
            .sorted(Comparator.comparing(AlarmVO::getLastAlarmTime, Comparator.nullsLast(Comparator.reverseOrder())))
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

    public Map<String, Object> statistics() {
        Map<Long, MonitorVO> monitorMap = monitorRepository.findAllActive().stream()
            .collect(Collectors.toMap(MonitorVO::getId, monitor -> monitor));
        Map<String, Long> counter = new LinkedHashMap<String, Long>();
        for (AlarmVO alarm : alarmRepository.findAll()) {
            if (!notDeleted(alarm)) {
                continue;
            }
            MonitorVO monitor = monitorMap.get(alarm.getMonitorId());
            String shaftType = monitor == null || monitor.getShaftType() == null ? "UNKNOWN" : monitor.getShaftType();
            counter.put(shaftType, counter.getOrDefault(shaftType, 0L) + 1L);
        }

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Long> entry : counter.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("shaftType", entry.getKey());
            row.put("alarmCount", entry.getValue());
            list.add(row);
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("total", list.stream().mapToLong(row -> ((Number) row.get("alarmCount")).longValue()).sum());
        data.put("list", list);
        return data;
    }

    private Map<String, Object> toMonitorMap(MonitorVO monitor) {
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

    private Map<String, Object> toDeviceMap(DeviceVO device) {
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

    private Map<String, Object> toFloorMap(ShaftFloorVO floor) {
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

    private Map<String, Object> toPartitionBindMap(MonitorPartitionBindVO bind, String floorName) {
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

    private Map<String, Object> toRecentAlarmMap(AlarmVO alarm) {
        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("id", alarm.getId());
        row.put("alarmType", alarm.getAlarmType());
        row.put("statusCode", alarm.getStatus());
        row.put("lastAlarmTime", alarm.getLastAlarmTime());
        row.put("content", alarm.getContent());
        return row;
    }

    private boolean notDeleted(AlarmVO alarm) {
        return alarm.getDeleted() == null || alarm.getDeleted().intValue() == 0;
    }
}
