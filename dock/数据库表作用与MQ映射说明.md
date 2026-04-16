# 数据库表作用与 MQ 映射说明

## 1. 总体原则
- MQ 上报到达后，系统只做“归属解析 + 业务落库”，**不会创建主数据**。
- 主数据必须提前准备好，尤其是分区绑定表 `ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D`。
- 实时链路核心是：`MQ -> 分区绑定解析 -> monitor/device/floor 归属 -> raw_data/alarm/event`。

---

## 2. 主要表作用

### 2.1 主数据表（需提前初始化）
- `ODS_DWEQ_DM_AREA_D`
  - 场所树（区域树），用于场所维度查询、统计、过滤。
  - 关键字段：`id`, `parent_id`, `type`, `path_ids`, `path_names`。

- `ODS_DWEQ_DM_ORG_D`
  - 机构树，用于设备机构归属和机构维度查询。
  - 关键字段：`id`, `parent_id`, `type`, `path_ids`, `path_names`。

- `ODS_DWEQ_DM_DEVICE_D`
  - 终端设备主数据。
  - 关键字段：`id`, `iot_code`, `device_type`, `area_id`, `org_id`, `online_status`, `last_report_time`。

- `ODS_DWEQ_DM_MONITOR_D`
  - 监测对象（竖井对象）主数据。
  - 关键字段：`id`, `area_id`, `area_name`, `device_id`, `shaft_type`, `monitor_status`。

- `ODS_DWEQ_DM_SHAFT_FLOOR_D`
  - 竖井下楼层对象。
  - 关键字段：`id`, `monitor_id`, `device_id`, `area_id`, `name`, `sort`。

- `ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D`
  - monitor 和 device 绑定关系。
  - 关键字段：`monitor_id`, `device_id`, `bind_status`。

- `ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D`
  - 分区绑定表（实时链路核心映射表）。
  - 关键字段：`monitor_id`, `device_id`, `shaft_floor_id`, `partition_id`, `partition_code`, `data_reference`, `bind_status`。

- `ODS_DWEQ_DM_ALARM_RULE_D`
  - 告警规则配置。
  - 关键字段：`biz_type`, `alarm_type`, `enabled`, `threshold_value`, `duration_seconds`。

### 2.2 业务数据表（由上报触发写入）
- `ODS_DWEQ_DM_RAW_DATA_D`
  - 实时测温原始数据。
  - 关键字段：`device_id`, `monitor_id`, `shaft_floor_id`, `partition_id`, `data_reference`, `collect_time`, 温度字段。

- `ODS_DWEQ_DM_ALARM_RAW_D`
  - 上游告警原文归档（AlarmStatus/FaultStatus 原始消息）。

- `ODS_DWEQ_DM_ALARM_D`
  - 告警主单（合并后的主记录）。
  - 关键字段：`alarm_type`, `status`, `merge_key`, `merge_count`, `event_count`, `first_alarm_time`, `last_alarm_time`。

- `ODS_DWEQ_DM_EVENT_D`
  - 告警事件流水（触发、合并、处警、恢复等过程事件）。
  - 关键字段：`alarm_id`, `alarm_type`, `event_type`, `event_time`, `event_no`, `merged_flag`。

- `ODS_DWEQ_DM_DEVICE_ONLINE_LOG_D`
  - 在线状态变化日志（上线/离线轨迹）。

---

## 3. 关键关联关系（字段级）

- `monitor.area_id -> area.id`
- `device.area_id -> area.id`
- `device.org_id -> org.id`
- `shaft_floor.monitor_id -> monitor.id`
- `shaft_floor.device_id -> device.id`
- `monitor_device_bind.monitor_id -> monitor.id`
- `monitor_device_bind.device_id -> device.id`
- `monitor_partition_bind.monitor_id -> monitor.id`
- `monitor_partition_bind.device_id -> device.id`
- `monitor_partition_bind.shaft_floor_id -> shaft_floor.id`
- `raw_data.monitor_id/device_id/shaft_floor_id` 来自分区绑定解析结果
- `alarm.monitor_id/device_id/shaft_floor_id` 来自分区绑定解析结果
- `event.alarm_id -> alarm.id`

---

## 4. MQ Topic 与业务归属映射

## 4.1 Topic 识别
- 约定订阅：
  - `/TMP/+/Measure`
  - `/TMP/+/Alarm`
- 消息类型识别：
  - topic 以 `/Measure` 结尾 -> 测温数据
  - topic 以 `/Alarm` 结尾 -> 上游告警数据

## 4.2 iotCode 解析
- 从 topic 的第 3 段提取 `iotCode`（例如 `/TMP/shaft-dev-01_TMP_th01/Measure` 的第三段为 `shaft-dev-01_TMP_th01`）。
- 但最终归属不只依赖 topic，而是依赖分区绑定解析。

## 4.3 分区绑定解析优先级（service 真实逻辑）
1. `iotCode + partitionId` 命中 `monitor_partition_bind`
2. 否则按 `dataReference` 命中
3. 否则按 `partitionCode` 命中
4. 三者都失败 -> 报错 `partition binding not found`

## 4.4 归属后的落库结果
- 解析成功后，拿到唯一归属：
  - `monitor_id`
  - `device_id`
  - `shaft_floor_id`
  - `partition_id/code/data_reference`
- 用这组归属写入 `raw_data`，并驱动告警规则计算与 `alarm/event` 生成。
- **不会自动创建 monitor/device/floor/area/org/bind 主数据**。

---

## 5. 两个 bind 表的区别

- `monitor_device_bind`
  - 表示“这个监测对象绑定哪台设备”。
  - 偏业务关系、资产关系。

- `monitor_partition_bind`
  - 表示“这个设备的这个分区归属哪个监测对象/楼层”。
  - 偏实时路由关系，是 MQ 数据落库与告警归属的核心。

---

## 6. 常见报错与原因

- 报错：`partition binding not found for partitionCode=...`
  - 根因：`monitor_partition_bind` 缺记录或字段不匹配（`partition_id / data_reference / partition_code`）。

- 报错：设备或监测对象不存在
  - 根因：绑定表中引用的 `device_id / monitor_id / shaft_floor_id` 对应主表记录不存在或已删除。

- 数据进了 `alarm_raw` 但没有生成 `alarm/event`
  - 根因：上游 Alarm 报文当前仅做原文入库；主告警生成主要由测温链路规则触发，或离线巡检触发。

---

## 7. 联调最低必备主数据
- `area`
- `org`（如涉及设备机构筛选）
- `device`
- `monitor`
- `shaft_floor`
- `monitor_device_bind`
- `monitor_partition_bind`
- `alarm_rule`

缺任一关键映射，MQ 链路将无法完整落库与告警归属。
