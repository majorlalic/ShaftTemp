# raw_data 分区升级说明（达梦）

目标：在不改代码的前提下，把 `ODS_DWEQ_DM_RAW_DATA_D` 从普通表升级为按 `device_id` 分区表。  
原则：表名不变、字段不变、索引等价，业务 SQL 无需修改。

## 1. 升级前提

- 当前已存在表：`ODS_DWEQ_DM_RAW_DATA_D`
- 应用写入 SQL 仍是 `insert into ODS_DWEQ_DM_RAW_DATA_D ...`
- 建议在低峰窗口执行第 5 步表名切换

## 2. 增量升级 SQL

```sql
-- =============================
-- raw_data 增量迁移为按 device_id 分区（达梦）
-- 适用：ODS_DWEQ_DM_RAW_DATA_D 已存在（非分区）
-- =============================

-- 0) 防重复执行（可选，按需清理）
-- DROP TABLE ODS_DWEQ_DM_RAW_DATA_D_NEW;
-- DROP TABLE ODS_DWEQ_DM_RAW_DATA_D_BAK;

-- 1) 新建分区表（结构与原表一致）
CREATE TABLE ODS_DWEQ_DM_RAW_DATA_D_NEW (
    id BIGINT NOT NULL,
    device_id BIGINT,
    iot_code VARCHAR(100),
    topic VARCHAR(200),
    partition_id INT,
    monitor_id BIGINT,
    shaft_floor_id BIGINT,
    data_reference VARCHAR(200),
    ied_full_path VARCHAR(500),
    collect_time DATETIME,
    max_temp DECIMAL(10,2),
    min_temp DECIMAL(10,2),
    avg_temp DECIMAL(10,2),
    max_temp_position DECIMAL(10,2),
    min_temp_position DECIMAL(10,2),
    max_temp_channel INT,
    min_temp_channel INT,
    payload_json CLOB,
    deleted TINYINT DEFAULT 0,
    created_on DATETIME,
    PRIMARY KEY (id)
)
PARTITION BY HASH(device_id) PARTITIONS 16;

-- 2) 创建索引（与现网一致）
CREATE INDEX idx_ODS_DWEQ_DM_RAW_DATA_D_NEW_iot_time
ON ODS_DWEQ_DM_RAW_DATA_D_NEW (iot_code, collect_time);

CREATE INDEX idx_ODS_DWEQ_DM_RAW_DATA_D_NEW_device_partition_time
ON ODS_DWEQ_DM_RAW_DATA_D_NEW (device_id, partition_id, collect_time);

CREATE INDEX idx_ODS_DWEQ_DM_RAW_DATA_D_NEW_monitor_time
ON ODS_DWEQ_DM_RAW_DATA_D_NEW (monitor_id, collect_time);

CREATE INDEX idx_ODS_DWEQ_DM_RAW_DATA_D_NEW_partition_time
ON ODS_DWEQ_DM_RAW_DATA_D_NEW (partition_id, collect_time);

-- 3) 回灌历史数据
INSERT INTO ODS_DWEQ_DM_RAW_DATA_D_NEW (
    id, device_id, iot_code, topic, partition_id, monitor_id, shaft_floor_id, data_reference, ied_full_path,
    collect_time, max_temp, min_temp, avg_temp, max_temp_position, min_temp_position, max_temp_channel, min_temp_channel,
    payload_json, deleted, created_on
)
SELECT
    id, device_id, iot_code, topic, partition_id, monitor_id, shaft_floor_id, data_reference, ied_full_path,
    collect_time, max_temp, min_temp, avg_temp, max_temp_position, min_temp_position, max_temp_channel, min_temp_channel,
    payload_json, deleted, created_on
FROM ODS_DWEQ_DM_RAW_DATA_D;

-- 4) 校验（务必执行并确认）
-- SELECT COUNT(*) AS old_cnt FROM ODS_DWEQ_DM_RAW_DATA_D;
-- SELECT COUNT(*) AS new_cnt FROM ODS_DWEQ_DM_RAW_DATA_D_NEW;

-- 5) 低峰切换（建议停写窗口内执行）
RENAME TABLE ODS_DWEQ_DM_RAW_DATA_D TO ODS_DWEQ_DM_RAW_DATA_D_BAK;
RENAME TABLE ODS_DWEQ_DM_RAW_DATA_D_NEW TO ODS_DWEQ_DM_RAW_DATA_D;

-- 6) 切换后检查
-- SELECT COUNT(*) FROM ODS_DWEQ_DM_RAW_DATA_D;

-- 7) 观察稳定后再删除备份
-- DROP TABLE ODS_DWEQ_DM_RAW_DATA_D_BAK;
```

## 3. 验证建议

- 行数一致：`old_cnt == new_cnt`
- 业务写入正常：`/shaft/iot/reports/measure` 压测后 `raw_data` 行数持续增长
- 常用查询正常：按 `device_id/partition_id/collect_time` 过滤无报错

## 4. 回滚方案

如果切换后异常，立即回滚表名：

```sql
RENAME TABLE ODS_DWEQ_DM_RAW_DATA_D TO ODS_DWEQ_DM_RAW_DATA_D_NEW_BAD;
RENAME TABLE ODS_DWEQ_DM_RAW_DATA_D_BAK TO ODS_DWEQ_DM_RAW_DATA_D;
```

回滚后排查完成再重新执行升级流程。

