# MQ 分区化改造方案

## 1. 背景

当前接收到的 MQ 文档并不是“设备一次上报一个完整一维温度数组”，而是“设备下某个分区的测量摘要和告警状态”。

示例：

- `/TMP/xx_TMP_th01/Measure`
- `/TMP/xx_TMP_th01/Alarm`

这说明系统模型应从“设备整体数组模型”切换为“设备下多分区模型”。

## 2. 新业务模型

建议采用以下关系：

- 一个 `device` 对应一个现场测温装置
- 一个 `monitor` 对应一个监控对象
- 一个 `shaft_floor` 对应监控对象中的一个楼层
- 一个设备下有多个 `partition`
- 一个 `partition` 通过绑定关系映射到一个 `shaft_floor`

推荐业务主键维度：

- `device_id`
- `monitor_id`
- `shaft_floor_id`
- `partition_code`

## 3. 数据库改动建议

### 3.1 新增绑定表

建议新增：

```sql
CREATE TABLE monitor_partition_bind (
    id bigint unsigned COMMENT '主键ID',
    monitor_id bigint unsigned COMMENT '监测对象ID',
    device_id bigint unsigned COMMENT '终端ID',
    shaft_floor_id bigint unsigned COMMENT '楼层ID',
    partition_code varchar(100) COMMENT '分区编码',
    partition_name varchar(100) COMMENT '分区名称',
    data_reference varchar(200) COMMENT 'MQ数据引用',
    bind_status tinyint COMMENT '绑定状态',
    deleted tinyint COMMENT '是否删除',
    creator bigint unsigned COMMENT '创建人',
    updator bigint unsigned COMMENT '更新人',
    deletor bigint unsigned COMMENT '删除人',
    created_on datetime COMMENT '创建时间',
    updated_on datetime COMMENT '更新时间',
    deleted_on datetime COMMENT '删除时间'
) COMMENT='monitor_partition_bind表';
```

### 3.2 扩展现有业务表

建议对以下表补字段：

#### raw_data

```sql
ALTER TABLE raw_data
    ADD COLUMN shaft_floor_id bigint unsigned COMMENT '楼层ID',
    ADD COLUMN partition_code varchar(100) COMMENT '分区编码',
    ADD COLUMN partition_name varchar(100) COMMENT '分区名称',
    ADD COLUMN data_reference varchar(200) COMMENT 'MQ数据引用',
    ADD COLUMN source_format varchar(30) COMMENT '数据来源格式';
```

#### alarm

```sql
ALTER TABLE alarm
    ADD COLUMN shaft_floor_id bigint unsigned COMMENT '楼层ID',
    ADD COLUMN partition_code varchar(100) COMMENT '分区编码',
    ADD COLUMN partition_name varchar(100) COMMENT '分区名称',
    ADD COLUMN data_reference varchar(200) COMMENT 'MQ数据引用',
    ADD COLUMN source_format varchar(30) COMMENT '数据来源格式';
```

#### event

```sql
ALTER TABLE event
    ADD COLUMN shaft_floor_id bigint unsigned COMMENT '楼层ID',
    ADD COLUMN partition_code varchar(100) COMMENT '分区编码',
    ADD COLUMN partition_name varchar(100) COMMENT '分区名称',
    ADD COLUMN data_reference varchar(200) COMMENT 'MQ数据引用',
    ADD COLUMN source_format varchar(30) COMMENT '数据来源格式';
```

#### temp_stat_minute

```sql
ALTER TABLE temp_stat_minute
    ADD COLUMN shaft_floor_id bigint unsigned COMMENT '楼层ID',
    ADD COLUMN partition_code varchar(100) COMMENT '分区编码',
    ADD COLUMN partition_name varchar(100) COMMENT '分区名称',
    ADD COLUMN data_reference varchar(200) COMMENT 'MQ数据引用',
    ADD COLUMN source_format varchar(30) COMMENT '数据来源格式';
```

## 4. 分区绑定规则

### 4.1 为什么需要绑定表

MQ 文档里的：

- `dataReference`
- topic
- `IedFullPath`

都不适合直接长期作为业务主关联键。

最稳妥的方案是通过绑定表显式配置：

- 某个分区属于哪个设备
- 某个分区映射哪个监控对象
- 某个分区映射哪个楼层

### 4.2 示例数据

例如一个设备有两个分区：

```sql
INSERT INTO monitor_partition_bind
(id, monitor_id, device_id, shaft_floor_id, partition_code, partition_name, data_reference, bind_status, deleted, created_on, updated_on)
VALUES
(90001, 10001, 20001, 30001, 'xx_TMP_th01', '1楼分区', '/TMP/xx_TMP_th01', 1, 0, NOW(), NOW()),
(90002, 10001, 20001, 30002, 'xx_TMP_th02', '2楼分区', '/TMP/xx_TMP_th02', 1, 0, NOW(), NOW());
```

这样：

- `xx_TMP_th01` -> 1楼
- `xx_TMP_th02` -> 2楼

### 4.3 关于 partitionCode 的命名理解

以示例：

- `xx_TMP_th02`

当前可做如下理解：

- `xx`
  - 很像设备标识、竖井装置标识或设备编码片段
- `TMP`
  - 温度测量类型标识
- `th02`
  - 很像第 2 个分区

结合文档中的说明：

- `xx_TMP_th01：表示xx竖井装置的第一个分区`

可以基本判断：

- `xx` 代表某个竖井装置
- `th01 / th02 / th03 ...` 代表该装置下的分区序号

但是这里不建议把字符串解析规则直接写死为系统唯一主关联规则。

原因：

- 文档当前是示例，不保证所有现场命名完全一致
- 后续厂家或现场可能出现命名变体
- 设备编码本身可能包含额外下划线或其他片段

因此建议采用以下策略：

- `partition_code`
  - 保存完整值，例如 `xx_TMP_th02`
- `device_token`
  - 可选解析字段，例如 `xx`
- `partition_no`
  - 可选解析字段，例如 `2`
- 真正的业务归属关系仍然走绑定表
  - `partition_code / data_reference -> device_id / monitor_id / shaft_floor_id`

### 4.4 推荐补充字段

如果希望保留这部分命名信息，建议可选增加：

#### monitor_partition_bind

- `device_token varchar(100) COMMENT '设备编码片段'`
- `partition_no int COMMENT '分区序号'`

#### raw_data

- `device_token varchar(100) COMMENT '设备编码片段'`
- `partition_no int COMMENT '分区序号'`

#### alarm

- `device_token varchar(100) COMMENT '设备编码片段'`
- `partition_no int COMMENT '分区序号'`

#### event

- `device_token varchar(100) COMMENT '设备编码片段'`
- `partition_no int COMMENT '分区序号'`

#### temp_stat_minute

- `device_token varchar(100) COMMENT '设备编码片段'`
- `partition_no int COMMENT '分区序号'`

## 5. MQ 订阅方案

### 5.1 订阅主题

建议订阅两类主题：

- `/TMP/+/Measure`
- `/TMP/+/Alarm`

或 broker 对应的等价模式。

### 5.2 消息类型

#### Measure

示例：

```json
{
  "MaxTemp": "38.4",
  "MaxTempPosition": "1.2",
  "MaxTempChannel": "1",
  "AvgTemp": "32.1",
  "MinTemp": "27.8",
  "MinTempPosition": "7.2",
  "MinTempChannel": "3",
  "timestamp": "2025-04-21T02:50:29.190Z",
  "IedFullPath": "南宁站|xx县|xx区|xx栋|1楼",
  "dataReference": "/TMP/xx_TMP_th01"
}
```

#### Alarm

示例：

```json
{
  "AlarmStatus": true,
  "FaultStatus": false,
  "timestamp": "2025-04-21T02:50:29.190Z",
  "IedFullPath": "南宁站|xx县|xx区|xx栋|1楼",
  "dataReference": "/TMP/xx_TMP_th01"
}
```

### 5.3 topic 解析

例如：

- topic: `/TMP/xx_TMP_th01/Measure`

解析后得到：

- `partition_code = xx_TMP_th01`
- `message_type = MEASURE`

例如：

- topic: `/TMP/xx_TMP_th02/Alarm`

解析后得到：

- `partition_code = xx_TMP_th02`
- `message_type = ALARM`

## 6. DTO 设计建议

### 6.1 分区测量消息 DTO

```java
class PartitionMeasureMessage {
    String topic;
    String partitionCode;
    String dataReference;
    String iedFullPath;
    OffsetDateTime timestamp;
    BigDecimal maxTemp;
    BigDecimal minTemp;
    BigDecimal avgTemp;
    String maxTempPosition;
    String minTempPosition;
    String maxTempChannel;
    String minTempChannel;
}
```

### 6.2 分区状态消息 DTO

```java
class PartitionAlarmMessage {
    String topic;
    String partitionCode;
    String dataReference;
    String iedFullPath;
    OffsetDateTime timestamp;
    Boolean alarmStatus;
    Boolean faultStatus;
}
```

### 6.3 统一内部命令对象

```java
class PartitionContext {
    Long deviceId;
    Long monitorId;
    Long shaftFloorId;
    String partitionCode;
    String partitionName;
    String dataReference;
}
```

## 7. 处理链路调整

### 7.1 Measure 链路

1. 订阅 `/Measure`
2. 解析 `partitionCode`
3. 根据绑定表找到：
   - `device_id`
   - `monitor_id`
   - `shaft_floor_id`
4. 更新 Redis 分区实时快照
5. 写 `raw_data`
6. 写 `temp_stat_minute`
7. 执行规则：
   - 定温
   - 差温
   - 升温速率
8. 生成或合并告警

### 7.2 Alarm 链路

1. 订阅 `/Alarm`
2. 解析 `partitionCode`
3. 查绑定表
4. 根据状态驱动告警：
   - `AlarmStatus=true` -> 火警
   - `FaultStatus=true` -> 故障告警
   - 状态恢复 -> 恢复告警

## 8. Redis 设计建议

建议 Redis 按分区维度缓存：

- `partition:last_measure:{partitionCode}`
- `partition:last_alarm:{partitionCode}`
- `partition:window:{partitionCode}`
- `partition:active_alarm:{alarmType}:{partitionCode}`

设备级保留：

- `device:last_report:{deviceId}`
- `device:offline_level:{deviceId}`

## 9. 规则重定义建议

### 9.1 保留

- 定温
  - `MaxTemp > threshold`
- 升温速率
  - `当前MaxTemp - 上次MaxTemp > threshold`

### 9.2 改造

- 差温
  - 改成 `MaxTemp - MinTemp > threshold`

### 9.3 替换

- 断纤
  - 不再从数组点位推断
  - 改为 `FaultStatus == true`

### 9.4 新增来源

- 火警
  - `AlarmStatus == true`

## 10. 告警合并规则

改造后建议按以下维度合并：

- `monitor_id`
- `shaft_floor_id`
- `partition_code`
- `alarm_type`
- `status in (ACTIVE, CONFIRMED)`

## 11. 查询接口调整建议

后续查询应支持以下过滤条件：

- `deviceId`
- `monitorId`
- `shaftFloorId`
- `partitionCode`
- `alarmType`
- `status`

## 12. 实施顺序

建议按以下顺序改造：

1. 数据库新增绑定表和分区字段
2. 新建 MQ 消息 DTO
3. 新建分区绑定解析服务
4. 改造消费链路
5. 改造 Redis
6. 改造规则引擎
7. 改造告警合并
8. 改造查询接口
9. 补测试数据与联调
