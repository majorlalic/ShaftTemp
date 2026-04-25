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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final RealtimeStateService realtimeStateService;

    public MonitorDocService(
        MonitorRepository monitorRepository,
        DeviceRepository deviceRepository,
        ShaftFloorRepository shaftFloorRepository,
        MonitorPartitionBindRepository monitorPartitionBindRepository,
        AlarmRepository alarmRepository,
        AreaRepository areaRepository,
        RealtimeStateService realtimeStateService
    ) {
        this.monitorRepository = monitorRepository;
        this.deviceRepository = deviceRepository;
        this.shaftFloorRepository = shaftFloorRepository;
        this.monitorPartitionBindRepository = monitorPartitionBindRepository;
        this.alarmRepository = alarmRepository;
        this.areaRepository = areaRepository;
        this.realtimeStateService = realtimeStateService;
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
            List<MonitorEntity> monitors = monitorRepository.findAllActive();
            enrichMonitorRows(monitors, rows);
        } else {
            AreaEntity area = areaRepository.findActiveById(areaTreeId).orElse(null);
            areaType = area == null ? null : area.getType();
            if (isShaftArea(areaType)) {
                mode = "SHAFT_FLOOR";
                List<ShaftFloorEntity> floors = shaftFloorRepository.findAllActiveByAreaTreeId(areaTreeId);
                enrichFloorRows(floors, rows);
            } else {
                List<MonitorEntity> monitors = monitorRepository.findAllActiveByAreaTreeId(areaTreeId);
                enrichMonitorRows(monitors, rows);
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

    private void enrichMonitorRows(List<MonitorEntity> monitors, List<Map<String, Object>> rows) {
        if (monitors == null || monitors.isEmpty()) {
            return;
        }
        List<Long> monitorIds = monitors.stream().map(MonitorEntity::getId).collect(Collectors.toList());
        Set<Long> todayAlarmMonitorIds = new HashSet<Long>(
            alarmRepository.findTodayAlarmMonitorIds(monitorIds, startOfToday(), startOfTomorrow())
        );
        Map<Long, TempAggregate> tempByMonitor = buildMonitorTempAggregates(monitorIds);
        for (MonitorEntity monitor : monitors) {
            Map<String, Object> row = toMonitorListRow(monitor);
            fillTodayAlarmAndTemperature(
                row,
                todayAlarmMonitorIds.contains(monitor.getId()),
                tempByMonitor.get(monitor.getId())
            );
            rows.add(row);
        }
    }

    private void enrichFloorRows(List<ShaftFloorEntity> floors, List<Map<String, Object>> rows) {
        if (floors == null || floors.isEmpty()) {
            return;
        }
        List<Long> shaftFloorIds = floors.stream().map(ShaftFloorEntity::getId).collect(Collectors.toList());
        Set<Long> todayAlarmShaftFloorIds = new HashSet<Long>(
            alarmRepository.findTodayAlarmShaftFloorIds(shaftFloorIds, startOfToday(), startOfTomorrow())
        );
        Map<Long, TempAggregate> tempByFloor = buildFloorTempAggregates(shaftFloorIds);
        for (ShaftFloorEntity floor : floors) {
            Map<String, Object> row = toFloorListRow(floor);
            fillTodayAlarmAndTemperature(
                row,
                todayAlarmShaftFloorIds.contains(floor.getId()),
                tempByFloor.get(floor.getId())
            );
            rows.add(row);
        }
    }

    private LocalDateTime startOfToday() {
        return LocalDate.now().atStartOfDay();
    }

    private LocalDateTime startOfTomorrow() {
        return startOfToday().plusDays(1);
    }

    private void fillTodayAlarmAndTemperature(Map<String, Object> row, boolean todayAlarm, TempAggregate aggregate) {
        row.put("todayAlarm", todayAlarm ? 1 : 0);
        if (aggregate == null || aggregate.getCount() == 0) {
            row.put("maxTemp", null);
            row.put("minTemp", null);
            row.put("avgTemp", null);
            row.put("lastCollectTime", null);
            return;
        }
        row.put("maxTemp", aggregate.getMaxTemp());
        row.put("minTemp", aggregate.getMinTemp());
        row.put("avgTemp", aggregate.getAvgTemp());
        row.put("lastCollectTime", aggregate.getLastCollectTime());
    }

    private Map<Long, TempAggregate> buildMonitorTempAggregates(List<Long> monitorIds) {
        if (monitorIds == null || monitorIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<MonitorPartitionBindEntity> bindings = monitorPartitionBindRepository.findAllActiveByMonitorIds(monitorIds);
        return aggregateTemperatures(
            bindings,
            new IdExtractor() {
                @Override
                public Long extract(MonitorPartitionBindEntity binding) {
                    return binding.getMonitorId();
                }
            }
        );
    }

    private Map<Long, TempAggregate> buildFloorTempAggregates(List<Long> shaftFloorIds) {
        if (shaftFloorIds == null || shaftFloorIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<MonitorPartitionBindEntity> bindings = monitorPartitionBindRepository.findAllActiveByShaftFloorIds(shaftFloorIds);
        return aggregateTemperatures(
            bindings,
            new IdExtractor() {
                @Override
                public Long extract(MonitorPartitionBindEntity binding) {
                    return binding.getShaftFloorId();
                }
            }
        );
    }

    private Map<Long, TempAggregate> aggregateTemperatures(
        List<MonitorPartitionBindEntity> bindings,
        IdExtractor idExtractor
    ) {
        if (bindings == null || bindings.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<String> partitionCodes = bindings.stream()
            .map(MonitorPartitionBindEntity::getPartitionCode)
            .collect(Collectors.toList());
        Map<String, RealtimeStateService.RealtimeSummary> summaryByPartitionCode =
            realtimeStateService.getLastPartitionSummaries(partitionCodes);
        Map<Long, TempAggregate> aggregates = new HashMap<Long, TempAggregate>();
        for (MonitorPartitionBindEntity binding : bindings) {
            Long objectId = idExtractor.extract(binding);
            if (objectId == null) {
                continue;
            }
            RealtimeStateService.RealtimeSummary summary = summaryByPartitionCode.get(binding.getPartitionCode());
            if (summary == null) {
                continue;
            }
            TempAggregate aggregate = aggregates.get(objectId);
            if (aggregate == null) {
                aggregate = new TempAggregate();
                aggregates.put(objectId, aggregate);
            }
            aggregate.accept(summary.getMaxTemp(), summary.getMinTemp(), summary.getAvgTemp(), summary.getCollectTime());
        }
        return aggregates;
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

    private interface IdExtractor {
        Long extract(MonitorPartitionBindEntity binding);
    }

    private static final class TempAggregate {
        private BigDecimal maxTemp;
        private BigDecimal minTemp;
        private BigDecimal avgSum = BigDecimal.ZERO;
        private int count;
        private LocalDateTime lastCollectTime;

        void accept(BigDecimal max, BigDecimal min, BigDecimal avg, LocalDateTime collectTime) {
            if (max != null) {
                maxTemp = maxTemp == null || max.compareTo(maxTemp) > 0 ? max : maxTemp;
            }
            if (min != null) {
                minTemp = minTemp == null || min.compareTo(minTemp) < 0 ? min : minTemp;
            }
            if (avg != null) {
                avgSum = avgSum.add(avg);
                count++;
            }
            if (collectTime != null) {
                if (lastCollectTime == null || collectTime.isAfter(lastCollectTime)) {
                    lastCollectTime = collectTime;
                }
            }
        }

        int getCount() {
            return count;
        }

        BigDecimal getMaxTemp() {
            return maxTemp;
        }

        BigDecimal getMinTemp() {
            return minTemp;
        }

        BigDecimal getAvgTemp() {
            if (count <= 0) {
                return null;
            }
            return avgSum.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP);
        }

        LocalDateTime getLastCollectTime() {
            return lastCollectTime;
        }
    }
}
