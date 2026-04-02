package com.example.demo.dao;

import com.example.demo.service.AlarmEventType;
import com.example.demo.service.AlarmStatus;
import com.csg.dgri.szsiom.sysmanage.model.AlarmVO;
import com.csg.dgri.szsiom.sysmanage.model.EventVO;
import com.csg.dgri.szsiom.sysmanage.model.RawDataVO;
import com.example.demo.service.RealtimeStateService.RealtimeSummary;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class QueryMapper {

    public Map<String, Object> toAlarmMap(AlarmVO alarm) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("id", alarm.getId());
        payload.put("alarmCode", alarm.getAlarmCode());
        payload.put("alarmType", alarm.getAlarmType());
        payload.put("sourceType", alarm.getSourceType());
        payload.put("monitorId", alarm.getMonitorId());
        payload.put("deviceId", alarm.getDeviceId());
        payload.put("shaftFloorId", alarm.getShaftFloorId());
        payload.put("partitionCode", alarm.getPartitionCode());
        payload.put("partitionName", alarm.getPartitionName());
        payload.put("dataReference", alarm.getDataReference());
        payload.put("statusCode", alarm.getStatus());
        payload.put("statusName", AlarmStatus.nameOf(alarm.getStatus()));
        payload.put("firstAlarmTime", alarm.getFirstAlarmTime());
        payload.put("lastAlarmTime", alarm.getLastAlarmTime());
        payload.put("mergeCount", alarm.getMergeCount());
        payload.put("eventCount", alarm.getEventCount());
        payload.put("alarmLevel", alarm.getAlarmLevel());
        payload.put("title", alarm.getTitle());
        payload.put("content", alarm.getContent());
        payload.put("handler", alarm.getHandler());
        payload.put("handleTime", alarm.getHandleTime());
        payload.put("handleRemark", alarm.getHandleRemark());
        payload.put("pushStatus", alarm.getPushStatus());
        payload.put("updatedOn", alarm.getUpdatedOn());
        return payload;
    }

    public Map<String, Object> toEventMap(EventVO event) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("id", event.getId());
        payload.put("alarmId", event.getAlarmId());
        payload.put("alarmType", event.getAlarmType());
        payload.put("sourceType", event.getSourceType());
        payload.put("monitorId", event.getMonitorId());
        payload.put("deviceId", event.getDeviceId());
        payload.put("shaftFloorId", event.getShaftFloorId());
        payload.put("partitionCode", event.getPartitionCode());
        payload.put("partitionName", event.getPartitionName());
        payload.put("dataReference", event.getDataReference());
        payload.put("eventType", event.getEventType());
        payload.put("eventTypeName", AlarmEventType.nameOf(event.getEventType()));
        payload.put("eventTime", event.getEventTime());
        payload.put("eventNo", event.getEventNo());
        payload.put("eventLevel", event.getEventLevel());
        payload.put("pointListJson", event.getPointListJson());
        payload.put("detailJson", event.getDetailJson());
        payload.put("content", event.getContent());
        payload.put("mergedFlag", event.getMergedFlag());
        return payload;
    }

    public Map<String, Object> toRawDataMap(RawDataVO rawData) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("id", rawData.getId());
        payload.put("deviceId", rawData.getDeviceId());
        payload.put("iotCode", rawData.getIotCode());
        payload.put("topic", rawData.getTopic());
        payload.put("partitionId", rawData.getPartitionId());
        payload.put("monitorId", rawData.getMonitorId());
        payload.put("shaftFloorId", rawData.getShaftFloorId());
        payload.put("dataReference", rawData.getDataReference());
        payload.put("iedFullPath", rawData.getIedFullPath());
        payload.put("collectTime", rawData.getCollectTime());
        payload.put("maxTemp", rawData.getMaxTemp());
        payload.put("minTemp", rawData.getMinTemp());
        payload.put("avgTemp", rawData.getAvgTemp());
        payload.put("maxTempPosition", rawData.getMaxTempPosition());
        payload.put("minTempPosition", rawData.getMinTempPosition());
        payload.put("maxTempChannel", rawData.getMaxTempChannel());
        payload.put("minTempChannel", rawData.getMinTempChannel());
        payload.put("payloadJson", rawData.getPayloadJson());
        return payload;
    }

    public Map<String, Object> toRealtimeMap(Long deviceId, RealtimeSummary summary) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("deviceId", deviceId);
        payload.put("monitorId", summary.getMonitorId());
        payload.put("shaftFloorId", summary.getShaftFloorId());
        payload.put("partitionCode", summary.getPartitionCode());
        payload.put("partitionName", summary.getPartitionName());
        payload.put("dataReference", summary.getDataReference());
        payload.put("collectTime", summary.getCollectTime());
        payload.put("maxTemp", summary.getMaxTemp());
        payload.put("minTemp", summary.getMinTemp());
        payload.put("avgTemp", summary.getAvgTemp());
        payload.put("maxTempPosition", summary.getMaxTempPosition());
        payload.put("minTempPosition", summary.getMinTempPosition());
        payload.put("maxTempChannel", summary.getMaxTempChannel());
        payload.put("minTempChannel", summary.getMinTempChannel());
        return payload;
    }
}
