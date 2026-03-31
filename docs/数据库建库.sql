-- 测温物联网系统完整建库脚本
-- 当前版本对应：
-- 1. 设备级 Topic + 分区数组消息模型（Measure / Alarm）
-- 2. 告警状态码 + eventType + eventCount + pushStatus
-- 3. 待确认告警 merge_key 唯一合并
-- 4. alarm_raw 原始告警存档表
-- 5. 主键、必要索引、唯一约束已内置

CREATE DATABASE IF NOT EXISTS shaft
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_general_ci;

USE shaft;

-- 清理历史 raw_data_yyyyMM 月表，避免结构漂移
SET @drop_month_sql = NULL;
SELECT GROUP_CONCAT(CONCAT('DROP TABLE IF EXISTS `', table_name, '`') SEPARATOR '; ')
INTO @drop_month_sql
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name REGEXP '^raw_data_[0-9]{6}$';
SET @drop_month_sql = IFNULL(@drop_month_sql, 'SELECT 1');
PREPARE stmt_drop_month FROM @drop_month_sql;
EXECUTE stmt_drop_month;
DEALLOCATE PREPARE stmt_drop_month;

DROP TABLE IF EXISTS device_online_log;
DROP TABLE IF EXISTS raw_data_template;
DROP TABLE IF EXISTS raw_data;
DROP TABLE IF EXISTS device_raw_data;
DROP TABLE IF EXISTS alarm_raw;
DROP TABLE IF EXISTS event;
DROP TABLE IF EXISTS alarm;
DROP TABLE IF EXISTS alarm_rule;
DROP TABLE IF EXISTS monitor_partition_bind;
DROP TABLE IF EXISTS monitor_device_bind;
DROP TABLE IF EXISTS shaft_floor;
DROP TABLE IF EXISTS monitor;
DROP TABLE IF EXISTS device;
DROP TABLE IF EXISTS org;
DROP TABLE IF EXISTS area;

CREATE TABLE area (
    id bigint unsigned NOT NULL COMMENT '主键ID',
    parent_id bigint unsigned DEFAULT NULL COMMENT '父级区域ID',
    name varchar(100) DEFAULT NULL COMMENT '区域名称',
    type varchar(30) DEFAULT NULL COMMENT '区域类型',
    path_ids varchar(500) DEFAULT NULL COMMENT '路径ID串',
    path_names varchar(1000) DEFAULT NULL COMMENT '路径名称串',
    deleted tinyint DEFAULT 0 COMMENT '是否删除 0否1是',
    sort int DEFAULT 0 COMMENT '排序号',
    creator bigint unsigned DEFAULT NULL COMMENT '创建人',
    updator bigint unsigned DEFAULT NULL COMMENT '更新人',
    deletor bigint unsigned DEFAULT NULL COMMENT '删除人',
    created_on datetime DEFAULT NULL COMMENT '创建时间',
    updated_on datetime DEFAULT NULL COMMENT '更新时间',
    deleted_on datetime DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id)
) COMMENT='area表';

CREATE TABLE org (
    id bigint unsigned NOT NULL COMMENT '主键ID',
    parent_id bigint unsigned DEFAULT NULL COMMENT '父级机构ID',
    name varchar(100) DEFAULT NULL COMMENT '机构名称',
    type varchar(30) DEFAULT NULL COMMENT '机构类型',
    path_ids varchar(500) DEFAULT NULL COMMENT '路径ID串',
    path_names varchar(1000) DEFAULT NULL COMMENT '路径名称串',
    deleted tinyint DEFAULT 0 COMMENT '是否删除',
    sort int DEFAULT 0 COMMENT '排序号',
    creator bigint unsigned DEFAULT NULL COMMENT '创建人',
    updator bigint unsigned DEFAULT NULL COMMENT '更新人',
    deletor bigint unsigned DEFAULT NULL COMMENT '删除人',
    created_on datetime DEFAULT NULL COMMENT '创建时间',
    updated_on datetime DEFAULT NULL COMMENT '更新时间',
    deleted_on datetime DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id)
) COMMENT='org表';

CREATE TABLE device (
    id bigint unsigned NOT NULL COMMENT '主键ID',
    device_type varchar(32) DEFAULT NULL COMMENT '设备类别',
    name varchar(100) DEFAULT NULL COMMENT '设备名称',
    area_id bigint unsigned DEFAULT NULL COMMENT '所属区域ID',
    iot_code varchar(100) DEFAULT NULL COMMENT '物联编码',
    model varchar(100) DEFAULT NULL COMMENT '设备型号',
    manufacturer varchar(100) DEFAULT NULL COMMENT '生产厂家',
    factory_date date DEFAULT NULL COMMENT '出厂日期',
    run_date date DEFAULT NULL COMMENT '投运日期',
    asset_status varchar(20) DEFAULT NULL COMMENT '资产状态',
    org_id bigint unsigned DEFAULT NULL COMMENT '所属机构ID',
    remark varchar(500) DEFAULT NULL COMMENT '备注',
    online_status tinyint DEFAULT 0 COMMENT '在线状态',
    last_report_time datetime DEFAULT NULL COMMENT '最近上报时间',
    last_offline_time datetime DEFAULT NULL COMMENT '最近离线时间',
    deleted tinyint DEFAULT 0 COMMENT '是否删除',
    creator bigint unsigned DEFAULT NULL COMMENT '创建人',
    updator bigint unsigned DEFAULT NULL COMMENT '更新人',
    deletor bigint unsigned DEFAULT NULL COMMENT '删除人',
    created_on datetime DEFAULT NULL COMMENT '创建时间',
    updated_on datetime DEFAULT NULL COMMENT '更新时间',
    deleted_on datetime DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_device_iot_code (iot_code),
    KEY idx_device_area_deleted (area_id, deleted),
    KEY idx_device_org_deleted (org_id, deleted)
) COMMENT='device表';

CREATE TABLE monitor (
    id bigint unsigned NOT NULL COMMENT '主键ID',
    name varchar(100) DEFAULT NULL COMMENT '监测对象名称',
    area_id bigint unsigned DEFAULT NULL COMMENT '区域ID',
    area_name varchar(1000) DEFAULT NULL COMMENT '区域全路径名称，使用斜杠分隔',
    elevator_count int unsigned DEFAULT NULL COMMENT '电梯数量',
    shaft_type varchar(20) DEFAULT NULL COMMENT '竖井类型',
    monitor_status varchar(20) DEFAULT NULL COMMENT '监测状态',
    build_date date DEFAULT NULL COMMENT '建成日期',
    owner_company varchar(200) DEFAULT NULL COMMENT '产权单位',
    device_id bigint unsigned DEFAULT NULL COMMENT '终端ID',
    remark varchar(500) DEFAULT NULL COMMENT '备注',
    deleted tinyint DEFAULT 0 COMMENT '是否删除',
    creator bigint unsigned DEFAULT NULL COMMENT '创建人',
    updator bigint unsigned DEFAULT NULL COMMENT '更新人',
    deletor bigint unsigned DEFAULT NULL COMMENT '删除人',
    created_on datetime DEFAULT NULL COMMENT '创建时间',
    updated_on datetime DEFAULT NULL COMMENT '更新时间',
    deleted_on datetime DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_monitor_area_deleted (area_id, deleted)
) COMMENT='monitor表';

CREATE TABLE shaft_floor (
    id bigint unsigned NOT NULL COMMENT '主键ID',
    monitor_id bigint unsigned DEFAULT NULL COMMENT '监测对象ID',
    area_id bigint unsigned DEFAULT NULL COMMENT '区域ID',
    name varchar(100) DEFAULT NULL COMMENT '楼层名称',
    device_id bigint unsigned DEFAULT NULL COMMENT '终端ID',
    start_point int unsigned DEFAULT NULL COMMENT '开始点位',
    end_point int unsigned DEFAULT NULL COMMENT '结束点位',
    sort int DEFAULT 0 COMMENT '排序号',
    deleted tinyint DEFAULT 0 COMMENT '是否删除',
    creator bigint unsigned DEFAULT NULL COMMENT '创建人',
    updator bigint unsigned DEFAULT NULL COMMENT '更新人',
    deletor bigint unsigned DEFAULT NULL COMMENT '删除人',
    created_on datetime DEFAULT NULL COMMENT '创建时间',
    updated_on datetime DEFAULT NULL COMMENT '更新时间',
    deleted_on datetime DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_shaft_floor_monitor_deleted (monitor_id, deleted),
    KEY idx_shaft_floor_device_deleted (device_id, deleted)
) COMMENT='shaft_floor表';

CREATE TABLE monitor_device_bind (
    id bigint unsigned NOT NULL COMMENT '主键ID',
    monitor_id bigint unsigned DEFAULT NULL COMMENT '监测对象ID',
    device_id bigint unsigned DEFAULT NULL COMMENT '终端ID',
    bind_status tinyint DEFAULT 1 COMMENT '绑定状态',
    bind_time datetime DEFAULT NULL COMMENT '绑定时间',
    unbind_time datetime DEFAULT NULL COMMENT '解绑时间',
    deleted tinyint DEFAULT 0 COMMENT '是否删除',
    creator bigint unsigned DEFAULT NULL COMMENT '创建人',
    updator bigint unsigned DEFAULT NULL COMMENT '更新人',
    deletor bigint unsigned DEFAULT NULL COMMENT '删除人',
    created_on datetime DEFAULT NULL COMMENT '创建时间',
    updated_on datetime DEFAULT NULL COMMENT '更新时间',
    deleted_on datetime DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    KEY idx_monitor_device_bind_monitor_device_deleted (monitor_id, device_id, deleted)
) COMMENT='monitor_device_bind表';

CREATE TABLE monitor_partition_bind (
    id bigint unsigned NOT NULL COMMENT '主键ID',
    monitor_id bigint unsigned DEFAULT NULL COMMENT '监测对象ID',
    device_id bigint unsigned DEFAULT NULL COMMENT '终端ID',
    shaft_floor_id bigint unsigned DEFAULT NULL COMMENT '楼层ID',
    partition_id int DEFAULT NULL COMMENT '分区标识，对应消息PartitionId',
    partition_code varchar(100) DEFAULT NULL COMMENT '分区编码',
    partition_name varchar(100) DEFAULT NULL COMMENT '分区名称',
    data_reference varchar(200) DEFAULT NULL COMMENT 'MQ数据引用',
    device_token varchar(100) DEFAULT NULL COMMENT '设备编码片段',
    partition_no int DEFAULT NULL COMMENT '分区序号',
    bind_status tinyint DEFAULT 1 COMMENT '绑定状态',
    deleted tinyint DEFAULT 0 COMMENT '是否删除',
    creator bigint unsigned DEFAULT NULL COMMENT '创建人',
    updator bigint unsigned DEFAULT NULL COMMENT '更新人',
    deletor bigint unsigned DEFAULT NULL COMMENT '删除人',
    created_on datetime DEFAULT NULL COMMENT '创建时间',
    updated_on datetime DEFAULT NULL COMMENT '更新时间',
    deleted_on datetime DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_monitor_partition_bind_device_partition_deleted (device_id, partition_id, deleted),
    KEY idx_monitor_partition_bind_partition_deleted (partition_code, deleted),
    KEY idx_monitor_partition_bind_reference_deleted (data_reference, deleted),
    KEY idx_monitor_partition_bind_monitor_deleted (monitor_id, deleted)
) COMMENT='monitor_partition_bind表';

CREATE TABLE alarm (
    id bigint unsigned NOT NULL COMMENT '主键ID',
    alarm_code varchar(64) DEFAULT NULL COMMENT '告警单号',
    alarm_type varchar(30) DEFAULT NULL COMMENT '告警类型',
    source_type varchar(32) DEFAULT NULL COMMENT '来源类型',
    monitor_id bigint unsigned DEFAULT NULL COMMENT '监测对象ID',
    device_id bigint unsigned DEFAULT NULL COMMENT '终端ID',
    shaft_floor_id bigint unsigned DEFAULT NULL COMMENT '楼层ID',
    partition_code varchar(100) DEFAULT NULL COMMENT '分区编码',
    partition_name varchar(100) DEFAULT NULL COMMENT '分区名称',
    data_reference varchar(200) DEFAULT NULL COMMENT 'MQ数据引用',
    device_token varchar(100) DEFAULT NULL COMMENT '设备编码片段',
    partition_no int DEFAULT NULL COMMENT '分区序号',
    source_format varchar(30) DEFAULT NULL COMMENT '数据来源格式',
    merge_key varchar(128) DEFAULT NULL COMMENT '待确认合并占位键',
    status tinyint DEFAULT 0 COMMENT '告警状态码 0待确认1已确认2持续观察3误报4关闭5自动恢复',
    first_alarm_time datetime DEFAULT NULL COMMENT '首次告警时间',
    last_alarm_time datetime DEFAULT NULL COMMENT '最近告警时间',
    merge_count int unsigned DEFAULT 0 COMMENT '累计触发次数',
    event_count int unsigned DEFAULT 0 COMMENT '已记录事件次数',
    alarm_level tinyint unsigned DEFAULT NULL COMMENT '告警等级',
    title varchar(200) DEFAULT NULL COMMENT '告警标题',
    content varchar(1000) DEFAULT NULL COMMENT '告警内容',
    handler bigint unsigned DEFAULT NULL COMMENT '处理人ID',
    handle_time datetime DEFAULT NULL COMMENT '处理时间',
    handle_remark varchar(500) DEFAULT NULL COMMENT '处理备注',
    push_status tinyint DEFAULT 0 COMMENT '推送状态 0未推送1已推送',
    deleted tinyint DEFAULT 0 COMMENT '是否删除',
    created_on datetime DEFAULT NULL COMMENT '创建时间',
    updated_on datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_alarm_merge_key (merge_key),
    KEY idx_alarm_monitor_type_status_deleted (monitor_id, alarm_type, status, deleted),
    KEY idx_alarm_device_time (device_id, last_alarm_time)
) COMMENT='alarm表';

CREATE TABLE alarm_raw (
    id bigint unsigned NOT NULL COMMENT '主键ID',
    iot_code varchar(100) DEFAULT NULL COMMENT '设备唯一标识，取topic设备段',
    topic varchar(200) DEFAULT NULL COMMENT '原始topic',
    partition_id int DEFAULT NULL COMMENT '分区标识',
    alarm_status tinyint DEFAULT NULL COMMENT '上游告警状态 0/1',
    fault_status tinyint DEFAULT NULL COMMENT '上游故障状态 0/1',
    ied_full_path varchar(500) DEFAULT NULL COMMENT '完整路径',
    data_reference varchar(200) DEFAULT NULL COMMENT '消息数据引用',
    collect_time datetime DEFAULT NULL COMMENT '采集时间',
    payload_json json DEFAULT NULL COMMENT '原始消息项JSON',
    deleted tinyint DEFAULT 0 COMMENT '是否删除',
    created_on datetime DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_alarm_raw_iot_time (iot_code, collect_time),
    KEY idx_alarm_raw_partition_time (partition_id, collect_time)
) COMMENT='上游告警原始数据表';

CREATE TABLE alarm_rule (
    id bigint unsigned NOT NULL COMMENT '主键ID',
    rule_name varchar(100) DEFAULT NULL COMMENT '规则名称',
    biz_type varchar(20) DEFAULT NULL COMMENT '业务类型 MONITOR/DEVICE',
    alarm_type varchar(30) DEFAULT NULL COMMENT '告警类型',
    scope_type varchar(20) DEFAULT NULL COMMENT '作用范围，当前固定为GLOBAL',
    scope_id bigint unsigned DEFAULT NULL COMMENT '作用对象ID，当前固定为NULL',
    level tinyint unsigned DEFAULT NULL COMMENT '告警等级',
    threshold_value decimal(10,2) DEFAULT NULL COMMENT '主阈值',
    threshold_value2 decimal(10,2) DEFAULT NULL COMMENT '副阈值',
    duration_seconds int DEFAULT NULL COMMENT '持续时长秒数',
    enabled tinyint DEFAULT 1 COMMENT '是否启用',
    remark varchar(500) DEFAULT NULL COMMENT '备注',
    deleted tinyint DEFAULT 0 COMMENT '是否删除',
    created_on datetime DEFAULT NULL COMMENT '创建时间',
    updated_on datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_alarm_rule_scope (biz_type, alarm_type, scope_type, scope_id, enabled, deleted)
) COMMENT='告警规则表';

CREATE TABLE event (
    id bigint unsigned NOT NULL COMMENT '主键ID',
    alarm_id bigint unsigned DEFAULT NULL COMMENT '告警ID',
    alarm_type varchar(30) DEFAULT NULL COMMENT '告警类型',
    source_type varchar(32) DEFAULT NULL COMMENT '来源类型',
    monitor_id bigint unsigned DEFAULT NULL COMMENT '监测对象ID',
    device_id bigint unsigned DEFAULT NULL COMMENT '终端ID',
    shaft_floor_id bigint unsigned DEFAULT NULL COMMENT '楼层ID',
    partition_code varchar(100) DEFAULT NULL COMMENT '分区编码',
    partition_name varchar(100) DEFAULT NULL COMMENT '分区名称',
    data_reference varchar(200) DEFAULT NULL COMMENT 'MQ数据引用',
    device_token varchar(100) DEFAULT NULL COMMENT '设备编码片段',
    partition_no int DEFAULT NULL COMMENT '分区序号',
    source_format varchar(30) DEFAULT NULL COMMENT '数据来源格式',
    event_type tinyint DEFAULT NULL COMMENT '事件类型码 0触发1合并2确认3观察4误报5恢复6关闭',
    event_time datetime DEFAULT NULL COMMENT '事件时间',
    event_no int unsigned DEFAULT NULL COMMENT '事件序号',
    event_level tinyint unsigned DEFAULT NULL COMMENT '事件等级',
    point_list_json json DEFAULT NULL COMMENT '触发点位列表',
    detail_json json DEFAULT NULL COMMENT '事件详情',
    content varchar(1000) DEFAULT NULL COMMENT '事件内容',
    merged_flag tinyint DEFAULT 0 COMMENT '是否合并',
    deleted tinyint DEFAULT 0 COMMENT '是否删除',
    created_on datetime DEFAULT NULL COMMENT '创建时间',
    updated_on datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_event_alarm_time (alarm_id, event_time),
    KEY idx_event_monitor_time (monitor_id, event_time)
) COMMENT='event表';

CREATE TABLE raw_data (
    id bigint unsigned NOT NULL COMMENT '主键ID',
    device_id bigint unsigned DEFAULT NULL COMMENT '终端ID',
    iot_code varchar(100) DEFAULT NULL COMMENT '设备唯一标识，取topic设备段',
    topic varchar(200) DEFAULT NULL COMMENT '原始topic',
    partition_id int DEFAULT NULL COMMENT '分区标识',
    monitor_id bigint unsigned DEFAULT NULL COMMENT '监测对象ID',
    shaft_floor_id bigint unsigned DEFAULT NULL COMMENT '楼层ID',
    data_reference varchar(200) DEFAULT NULL COMMENT 'MQ数据引用',
    ied_full_path varchar(500) DEFAULT NULL COMMENT '完整路径',
    collect_time datetime DEFAULT NULL COMMENT '采集时间',
    max_temp decimal(8,2) DEFAULT NULL COMMENT '最大温度',
    min_temp decimal(8,2) DEFAULT NULL COMMENT '最小温度',
    avg_temp decimal(8,2) DEFAULT NULL COMMENT '平均温度',
    max_temp_position decimal(10,2) DEFAULT NULL COMMENT '最大温度所在位置',
    min_temp_position decimal(10,2) DEFAULT NULL COMMENT '最小温度所在位置',
    max_temp_channel int DEFAULT NULL COMMENT '最大温度所在通道',
    min_temp_channel int DEFAULT NULL COMMENT '最小温度所在通道',
    payload_json json DEFAULT NULL COMMENT '原始消息项JSON',
    deleted tinyint DEFAULT 0 COMMENT '是否删除',
    created_on datetime DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_raw_data_iot_time (iot_code, collect_time),
    KEY idx_raw_data_device_partition_time (device_id, partition_id, collect_time),
    KEY idx_raw_data_monitor_time (monitor_id, collect_time),
    KEY idx_raw_data_partition_time (partition_id, collect_time)
) COMMENT='raw_data表';

CREATE TABLE raw_data_template LIKE raw_data;
ALTER TABLE raw_data_template COMMENT='raw_data月表模板';

CREATE TABLE device_online_log (
    id bigint unsigned NOT NULL COMMENT '主键ID',
    device_id bigint unsigned DEFAULT NULL COMMENT '终端ID',
    status tinyint DEFAULT NULL COMMENT '状态',
    change_time datetime DEFAULT NULL COMMENT '状态变化时间',
    reason varchar(200) DEFAULT NULL COMMENT '变化原因',
    deleted tinyint DEFAULT 0 COMMENT '是否删除',
    created_on datetime DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_device_online_log_device_time (device_id, change_time)
) COMMENT='device_online_log表';
