-- raw_data 按 device_id 分区（达梦）
-- 目标：
-- 1) 维持单表名 ODS_DWEQ_DM_RAW_DATA_D 不变
-- 2) 物理上按 device_id HASH 分区，提升高并发写入与按设备查询性能
-- 3) 最小侵入：业务代码无需改 SQL

-- =========================
-- A. 全新环境（直接建分区表）
-- =========================
-- 若是新环境，可直接用本段替换建表语句。

CREATE TABLE ODS_DWEQ_DM_RAW_DATA_D (
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

CREATE INDEX idx_ODS_DWEQ_DM_RAW_DATA_D_iot_time
ON ODS_DWEQ_DM_RAW_DATA_D (iot_code, collect_time);

CREATE INDEX idx_ODS_DWEQ_DM_RAW_DATA_D_device_partition_time
ON ODS_DWEQ_DM_RAW_DATA_D (device_id, partition_id, collect_time);

CREATE INDEX idx_ODS_DWEQ_DM_RAW_DATA_D_monitor_time
ON ODS_DWEQ_DM_RAW_DATA_D (monitor_id, collect_time);

CREATE INDEX idx_ODS_DWEQ_DM_RAW_DATA_D_partition_time
ON ODS_DWEQ_DM_RAW_DATA_D (partition_id, collect_time);


-- ===================================
-- B. 现网/已有数据（在线迁移建议步骤）
-- ===================================
-- 说明：
-- 1) 用 _NEW 表承接分区结构
-- 2) 数据回灌后做表名切换
-- 3) 切换窗口尽量选择低峰期

-- 1) 新建分区表
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

-- 2) 建索引
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

-- 4) 行数校验（切换前必须校验）
-- SELECT COUNT(*) FROM ODS_DWEQ_DM_RAW_DATA_D;
-- SELECT COUNT(*) FROM ODS_DWEQ_DM_RAW_DATA_D_NEW;

-- 5) 低峰切换（先停写再切）
-- RENAME TABLE ODS_DWEQ_DM_RAW_DATA_D TO ODS_DWEQ_DM_RAW_DATA_D_BAK;
-- RENAME TABLE ODS_DWEQ_DM_RAW_DATA_D_NEW TO ODS_DWEQ_DM_RAW_DATA_D;

-- 6) 观察稳定后删除备份表
-- DROP TABLE ODS_DWEQ_DM_RAW_DATA_D_BAK;

