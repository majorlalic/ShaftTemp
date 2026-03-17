# 分区MQ联调示例

先执行以下脚本：

1. [测温物联网系统最终建表_分区MQ版.sql](/Users/sketch/Documents/project/ShaftTemp/docs/测温物联网系统最终建表_分区MQ版.sql)
2. [本地联调初始化数据.sql](/Users/sketch/Documents/project/ShaftTemp/docs/本地联调初始化数据.sql)

初始化后会生成：

- 设备：`shaft-dev-01`
- 监控对象：`1号竖井`
- 分区：
  - `shaft-dev-01_TMP_th01` -> `1层`
  - `shaft-dev-01_TMP_th02` -> `2层`

## Measure 调试

```bash
curl -sS http://127.0.0.1:8080/api/reports/measure \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "topic": "/TMP/shaft-dev-01_TMP_th01/Measure",
    "IedFullPath": "/IED/shaft-dev-01",
    "dataReference": "/TMP/shaft-dev-01_TMP_th01",
    "MaxTemp": 85.5,
    "MinTemp": 62.3,
    "AvgTemp": 71.8,
    "MaxTempPosition": 15.5,
    "MinTempPosition": 2.1,
    "MaxTempChannel": 1,
    "MinTempChannel": 1,
    "timestamp": "2026-03-17T09:30:00"
  }'
```

预期：

- `raw_data` 新增 1 行
- `temp_stat_minute` 新增或更新 1 行
- Redis 生成：
  - `device:last_report:3001`
  - `partition:last_measure:shaft-dev-01_TMP_th01`
  - `partition:window:shaft-dev-01_TMP_th01`
- 因为 `85.5 > 70`，会生成 `TEMP_THRESHOLD`
- 因为 `85.5 - 62.3 > 20`，会生成 `TEMP_DIFFERENCE`

## Alarm 调试

```bash
curl -sS http://127.0.0.1:8080/api/reports/alarm \
  -X POST \
  -H 'Content-Type: application/json' \
  -d '{
    "topic": "/TMP/shaft-dev-01_TMP_th01/Alarm",
    "IedFullPath": "/IED/shaft-dev-01",
    "dataReference": "/TMP/shaft-dev-01_TMP_th01",
    "AlarmStatus": true,
    "FaultStatus": false,
    "timestamp": "2026-03-17T09:31:00"
  }'
```

预期：

- Redis 生成 `partition:last_alarm:shaft-dev-01_TMP_th01`
- `alarm/event` 生成 `PARTITION_FIRE`

## 查询验证

```bash
curl -sS 'http://127.0.0.1:8080/api/raw-data?partitionCode=shaft-dev-01_TMP_th01'
curl -sS 'http://127.0.0.1:8080/api/temp-stats/minute?partitionCode=shaft-dev-01_TMP_th01'
curl -sS 'http://127.0.0.1:8080/api/alarms?partitionCode=shaft-dev-01_TMP_th01'
curl -sS 'http://127.0.0.1:8080/api/events?partitionCode=shaft-dev-01_TMP_th01'
```
