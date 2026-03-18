-- shaft 库结构校正 SQL
-- 用途：
-- 1. 在现有 shaft 库上补齐主键和必要索引
-- 2. 不调整业务字段语义，不清空数据
-- 3. 面向当前代码实现的最小可用基线

USE shaft;

-- 1. 主键补齐

ALTER TABLE area
    MODIFY COLUMN id bigint unsigned NOT NULL COMMENT '主键ID',
    ADD PRIMARY KEY (id);

ALTER TABLE org
    MODIFY COLUMN id bigint unsigned NOT NULL COMMENT '主键ID',
    ADD PRIMARY KEY (id);

ALTER TABLE device
    MODIFY COLUMN id bigint unsigned NOT NULL COMMENT '主键ID',
    ADD PRIMARY KEY (id);

ALTER TABLE monitor
    MODIFY COLUMN id bigint unsigned NOT NULL COMMENT '主键ID',
    ADD PRIMARY KEY (id);

ALTER TABLE shaft_floor
    MODIFY COLUMN id bigint unsigned NOT NULL COMMENT '主键ID',
    ADD PRIMARY KEY (id);

ALTER TABLE monitor_device_bind
    MODIFY COLUMN id bigint unsigned NOT NULL COMMENT '主键ID',
    ADD PRIMARY KEY (id);

ALTER TABLE monitor_partition_bind
    MODIFY COLUMN id bigint unsigned NOT NULL COMMENT '主键ID',
    ADD PRIMARY KEY (id);

ALTER TABLE alarm
    MODIFY COLUMN id bigint unsigned NOT NULL COMMENT '主键ID',
    ADD PRIMARY KEY (id);

ALTER TABLE event
    MODIFY COLUMN id bigint unsigned NOT NULL COMMENT '主键ID',
    ADD PRIMARY KEY (id);

ALTER TABLE raw_data
    MODIFY COLUMN id bigint unsigned NOT NULL COMMENT '主键ID',
    ADD PRIMARY KEY (id);

ALTER TABLE temp_stat_minute
    MODIFY COLUMN id bigint unsigned NOT NULL COMMENT '主键ID',
    ADD PRIMARY KEY (id);

ALTER TABLE device_online_log
    MODIFY COLUMN id bigint unsigned NOT NULL COMMENT '主键ID',
    ADD PRIMARY KEY (id);

-- 2. 关键查询与写入路径索引

ALTER TABLE area
    ADD KEY idx_area_parent_deleted (parent_id, deleted),
    ADD KEY idx_area_type_deleted (type, deleted);

ALTER TABLE org
    ADD KEY idx_org_parent_deleted (parent_id, deleted),
    ADD KEY idx_org_type_deleted (type, deleted);

ALTER TABLE device
    ADD KEY idx_device_iot_code_deleted (iot_code, deleted),
    ADD KEY idx_device_online_deleted (online_status, deleted),
    ADD KEY idx_device_last_report (last_report_time);

ALTER TABLE monitor
    ADD KEY idx_monitor_device_deleted (device_id, deleted),
    ADD KEY idx_monitor_area_deleted (area_id, deleted);

ALTER TABLE shaft_floor
    ADD KEY idx_floor_monitor_deleted (monitor_id, deleted),
    ADD KEY idx_floor_device_deleted (device_id, deleted);

ALTER TABLE monitor_device_bind
    ADD KEY idx_mdb_monitor_device_deleted (monitor_id, device_id, deleted),
    ADD KEY idx_mdb_device_deleted (device_id, deleted);

ALTER TABLE monitor_partition_bind
    ADD KEY idx_mpb_partition_deleted (partition_code, deleted),
    ADD KEY idx_mpb_reference_deleted (data_reference, deleted),
    ADD KEY idx_mpb_bind_deleted (bind_status, deleted),
    ADD KEY idx_mpb_monitor_deleted (monitor_id, deleted),
    ADD KEY idx_mpb_device_deleted (device_id, deleted);

ALTER TABLE alarm
    ADD KEY idx_alarm_merge_key (merge_key),
    ADD KEY idx_alarm_monitor_type_status_deleted (monitor_id, alarm_type, status, deleted),
    ADD KEY idx_alarm_last_time (last_alarm_time),
    ADD KEY idx_alarm_partition_type (partition_code, alarm_type),
    ADD KEY idx_alarm_device_time (device_id, last_alarm_time);

ALTER TABLE event
    ADD KEY idx_event_alarm_time (alarm_id, event_time),
    ADD KEY idx_event_monitor_time (monitor_id, event_time),
    ADD KEY idx_event_type_time (alarm_type, event_time),
    ADD KEY idx_event_partition_time (partition_code, event_time);

ALTER TABLE raw_data
    ADD KEY idx_raw_device_collect (device_id, collect_time),
    ADD KEY idx_raw_monitor_collect (monitor_id, collect_time),
    ADD KEY idx_raw_partition_collect (partition_code, collect_time),
    ADD KEY idx_raw_reference_collect (data_reference, collect_time);

ALTER TABLE temp_stat_minute
    ADD KEY idx_minute_partition_time (partition_code, stat_time),
    ADD KEY idx_minute_monitor_time (monitor_id, stat_time),
    ADD KEY idx_minute_device_time (device_id, stat_time);

ALTER TABLE device_online_log
    ADD KEY idx_online_log_device_time (device_id, change_time),
    ADD KEY idx_online_log_status_time (status, change_time);

-- 3. 当前结构核对
-- SHOW CREATE TABLE alarm\G;
-- SHOW CREATE TABLE event\G;
-- SHOW INDEX FROM raw_data;
