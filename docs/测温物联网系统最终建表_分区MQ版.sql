-- 测温物联网系统最终建库脚本（分区MQ版）
-- 说明：
-- 1. 面向当前代码实现，按“设备 + 分区 + 楼层 + MQ Measure/Alarm”模型整理
-- 2. 保持与历史约定一致：不加主键、索引、外键、唯一约束
-- 3. 用于本地快速建库与联调，库名固定为 shaft

CREATE DATABASE IF NOT EXISTS shaft
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_general_ci;

USE shaft;

DROP TABLE IF EXISTS device_online_log;
DROP TABLE IF EXISTS temp_stat_minute;
DROP TABLE IF EXISTS raw_data;
DROP TABLE IF EXISTS event;
DROP TABLE IF EXISTS alarm;
DROP TABLE IF EXISTS monitor_partition_bind;
DROP TABLE IF EXISTS monitor_device_bind;
DROP TABLE IF EXISTS shaft_floor;
DROP TABLE IF EXISTS monitor;
DROP TABLE IF EXISTS device;
DROP TABLE IF EXISTS org;
DROP TABLE IF EXISTS area;

CREATE TABLE area (
    id bigint unsigned COMMENT '主键ID',
    parent_id bigint unsigned COMMENT '父级区域ID',
    name varchar(100) COMMENT '区域名称',
    type varchar(30) COMMENT '区域类型',
    path_ids varchar(500) COMMENT '路径ID串',
    path_names varchar(1000) COMMENT '路径名称串',
    deleted tinyint COMMENT '是否删除 0否1是',
    sort int COMMENT '排序号',
    creator bigint unsigned COMMENT '创建人',
    updator bigint unsigned COMMENT '更新人',
    deletor bigint unsigned COMMENT '删除人',
    created_on datetime COMMENT '创建时间',
    updated_on datetime COMMENT '更新时间',
    deleted_on datetime COMMENT '删除时间'
) COMMENT='area表';

CREATE TABLE org (
    id bigint unsigned COMMENT '主键ID',
    parent_id bigint unsigned COMMENT '父级机构ID',
    name varchar(100) COMMENT '机构名称',
    type varchar(30) COMMENT '机构类型',
    path_ids varchar(500) COMMENT '路径ID串',
    path_names varchar(1000) COMMENT '路径名称串',
    deleted tinyint COMMENT '是否删除',
    sort int COMMENT '排序号',
    creator bigint unsigned COMMENT '创建人',
    updator bigint unsigned COMMENT '更新人',
    deletor bigint unsigned COMMENT '删除人',
    created_on datetime COMMENT '创建时间',
    updated_on datetime COMMENT '更新时间',
    deleted_on datetime COMMENT '删除时间'
) COMMENT='org表';

CREATE TABLE device (
    id bigint unsigned COMMENT '主键ID',
    device_type varchar(32) COMMENT '设备类别',
    name varchar(100) COMMENT '设备名称',
    area_id bigint unsigned COMMENT '所属区域ID',
    iot_code varchar(100) COMMENT '物联编码',
    model varchar(100) COMMENT '设备型号',
    manufacturer varchar(100) COMMENT '生产厂家',
    factory_date date COMMENT '出厂日期',
    run_date date COMMENT '投运日期',
    asset_status varchar(20) COMMENT '资产状态',
    org_id bigint unsigned COMMENT '所属机构ID',
    remark varchar(500) COMMENT '备注',
    online_status tinyint COMMENT '在线状态',
    last_report_time datetime COMMENT '最近上报时间',
    last_offline_time datetime COMMENT '最近离线时间',
    deleted tinyint COMMENT '是否删除',
    creator bigint unsigned COMMENT '创建人',
    updator bigint unsigned COMMENT '更新人',
    deletor bigint unsigned COMMENT '删除人',
    created_on datetime COMMENT '创建时间',
    updated_on datetime COMMENT '更新时间',
    deleted_on datetime COMMENT '删除时间'
) COMMENT='device表';

CREATE TABLE monitor (
    id bigint unsigned COMMENT '主键ID',
    name varchar(100) COMMENT '监测对象名称',
    area_id bigint unsigned COMMENT '区域ID',
    community_area_id bigint unsigned COMMENT '社区ID',
    estate_area_id bigint unsigned COMMENT '小区ID',
    building_area_id bigint unsigned COMMENT '楼栋ID',
    unit_area_id bigint unsigned COMMENT '单元ID',
    service_range varchar(100) COMMENT '服务范围',
    elevator_count int unsigned COMMENT '电梯数量',
    shaft_type varchar(20) COMMENT '竖井类型',
    monitor_status varchar(20) COMMENT '监测状态',
    build_date date COMMENT '建成日期',
    owner_company varchar(200) COMMENT '产权单位',
    device_id bigint unsigned COMMENT '终端ID',
    remark varchar(500) COMMENT '备注',
    deleted tinyint COMMENT '是否删除',
    creator bigint unsigned COMMENT '创建人',
    updator bigint unsigned COMMENT '更新人',
    deletor bigint unsigned COMMENT '删除人',
    created_on datetime COMMENT '创建时间',
    updated_on datetime COMMENT '更新时间',
    deleted_on datetime COMMENT '删除时间'
) COMMENT='monitor表';

CREATE TABLE shaft_floor (
    id bigint unsigned COMMENT '主键ID',
    monitor_id bigint unsigned COMMENT '监测对象ID',
    area_id bigint unsigned COMMENT '区域ID',
    name varchar(100) COMMENT '楼层名称',
    device_id bigint unsigned COMMENT '终端ID',
    start_point int unsigned COMMENT '开始点位',
    end_point int unsigned COMMENT '结束点位',
    sort int COMMENT '排序号',
    deleted tinyint COMMENT '是否删除',
    creator bigint unsigned COMMENT '创建人',
    updator bigint unsigned COMMENT '更新人',
    deletor bigint unsigned COMMENT '删除人',
    created_on datetime COMMENT '创建时间',
    updated_on datetime COMMENT '更新时间',
    deleted_on datetime COMMENT '删除时间'
) COMMENT='shaft_floor表';

CREATE TABLE monitor_device_bind (
    id bigint unsigned COMMENT '主键ID',
    monitor_id bigint unsigned COMMENT '监测对象ID',
    device_id bigint unsigned COMMENT '终端ID',
    bind_status tinyint COMMENT '绑定状态',
    bind_time datetime COMMENT '绑定时间',
    unbind_time datetime COMMENT '解绑时间',
    deleted tinyint COMMENT '是否删除',
    creator bigint unsigned COMMENT '创建人',
    updator bigint unsigned COMMENT '更新人',
    deletor bigint unsigned COMMENT '删除人',
    created_on datetime COMMENT '创建时间',
    updated_on datetime COMMENT '更新时间',
    deleted_on datetime COMMENT '删除时间'
) COMMENT='monitor_device_bind表';

CREATE TABLE monitor_partition_bind (
    id bigint unsigned COMMENT '主键ID',
    monitor_id bigint unsigned COMMENT '监测对象ID',
    device_id bigint unsigned COMMENT '终端ID',
    shaft_floor_id bigint unsigned COMMENT '楼层ID',
    partition_code varchar(100) COMMENT '分区编码',
    partition_name varchar(100) COMMENT '分区名称',
    data_reference varchar(200) COMMENT 'MQ数据引用',
    device_token varchar(100) COMMENT '设备编码片段',
    partition_no int COMMENT '分区序号',
    bind_status tinyint COMMENT '绑定状态',
    deleted tinyint COMMENT '是否删除',
    creator bigint unsigned COMMENT '创建人',
    updator bigint unsigned COMMENT '更新人',
    deletor bigint unsigned COMMENT '删除人',
    created_on datetime COMMENT '创建时间',
    updated_on datetime COMMENT '更新时间',
    deleted_on datetime COMMENT '删除时间'
) COMMENT='monitor_partition_bind表';

CREATE TABLE alarm (
    id bigint unsigned COMMENT '主键ID',
    alarm_code varchar(64) COMMENT '告警单号',
    alarm_type varchar(30) COMMENT '告警类型',
    source_type varchar(20) COMMENT '来源类型',
    monitor_id bigint unsigned COMMENT '监测对象ID',
    device_id bigint unsigned COMMENT '终端ID',
    shaft_floor_id bigint unsigned COMMENT '楼层ID',
    partition_code varchar(100) COMMENT '分区编码',
    partition_name varchar(100) COMMENT '分区名称',
    data_reference varchar(200) COMMENT 'MQ数据引用',
    device_token varchar(100) COMMENT '设备编码片段',
    partition_no int COMMENT '分区序号',
    source_format varchar(30) COMMENT '数据来源格式',
    status varchar(20) COMMENT '告警状态',
    first_alarm_time datetime COMMENT '首次告警时间',
    last_alarm_time datetime COMMENT '最近告警时间',
    merge_count int unsigned COMMENT '合并次数',
    alarm_level tinyint unsigned COMMENT '告警等级',
    title varchar(200) COMMENT '告警标题',
    content varchar(1000) COMMENT '告警内容',
    confirm_user_id bigint unsigned COMMENT '确认人ID',
    confirm_time datetime COMMENT '确认时间',
    handle_remark varchar(500) COMMENT '处理备注',
    deleted tinyint COMMENT '是否删除',
    created_on datetime COMMENT '创建时间',
    updated_on datetime COMMENT '更新时间'
) COMMENT='alarm表';

CREATE TABLE event (
    id bigint unsigned COMMENT '主键ID',
    alarm_id bigint unsigned COMMENT '告警ID',
    alarm_type varchar(30) COMMENT '告警类型',
    source_type varchar(20) COMMENT '来源类型',
    monitor_id bigint unsigned COMMENT '监测对象ID',
    device_id bigint unsigned COMMENT '终端ID',
    shaft_floor_id bigint unsigned COMMENT '楼层ID',
    partition_code varchar(100) COMMENT '分区编码',
    partition_name varchar(100) COMMENT '分区名称',
    data_reference varchar(200) COMMENT 'MQ数据引用',
    device_token varchar(100) COMMENT '设备编码片段',
    partition_no int COMMENT '分区序号',
    source_format varchar(30) COMMENT '数据来源格式',
    event_time datetime COMMENT '事件时间',
    event_no int unsigned COMMENT '事件序号',
    event_level tinyint unsigned COMMENT '事件等级',
    point_list_json json COMMENT '触发点位列表',
    detail_json json COMMENT '事件详情',
    merged_flag tinyint COMMENT '是否合并',
    deleted tinyint COMMENT '是否删除',
    created_on datetime COMMENT '创建时间',
    updated_on datetime COMMENT '更新时间'
) COMMENT='event表';

CREATE TABLE raw_data (
    id bigint unsigned COMMENT '主键ID',
    device_id bigint unsigned COMMENT '终端ID',
    iot_code varchar(100) COMMENT '物联编码',
    monitor_id bigint unsigned COMMENT '监测对象ID',
    shaft_floor_id bigint unsigned COMMENT '楼层ID',
    partition_code varchar(100) COMMENT '分区编码',
    partition_name varchar(100) COMMENT '分区名称',
    data_reference varchar(200) COMMENT 'MQ数据引用',
    device_token varchar(100) COMMENT '设备编码片段',
    partition_no int COMMENT '分区序号',
    source_format varchar(30) COMMENT '数据来源格式',
    collect_time datetime COMMENT '采集时间',
    point_count int unsigned COMMENT '点位数量',
    valid_start_point int unsigned COMMENT '有效开始点位',
    valid_end_point int unsigned COMMENT '有效结束点位',
    values_json json COMMENT '分区测量快照JSON',
    max_temp decimal(8,2) COMMENT '最大温度',
    min_temp decimal(8,2) COMMENT '最小温度',
    avg_temp decimal(8,2) COMMENT '平均温度',
    abnormal_flag tinyint COMMENT '是否异常',
    deleted tinyint COMMENT '是否删除',
    created_on datetime COMMENT '创建时间'
) COMMENT='raw_data表';

CREATE TABLE temp_stat_minute (
    id bigint unsigned COMMENT '主键ID',
    device_id bigint unsigned COMMENT '终端ID',
    monitor_id bigint unsigned COMMENT '监测对象ID',
    shaft_floor_id bigint unsigned COMMENT '楼层ID',
    partition_code varchar(100) COMMENT '分区编码',
    partition_name varchar(100) COMMENT '分区名称',
    data_reference varchar(200) COMMENT 'MQ数据引用',
    device_token varchar(100) COMMENT '设备编码片段',
    partition_no int COMMENT '分区序号',
    source_format varchar(30) COMMENT '数据来源格式',
    stat_time datetime COMMENT '统计时间',
    max_temp decimal(8,2) COMMENT '最大温度',
    min_temp decimal(8,2) COMMENT '最小温度',
    avg_temp decimal(8,2) COMMENT '平均温度',
    alarm_point_count int unsigned COMMENT '异常数量',
    deleted tinyint COMMENT '是否删除',
    created_on datetime COMMENT '创建时间'
) COMMENT='temp_stat_minute表';

CREATE TABLE device_online_log (
    id bigint unsigned COMMENT '主键ID',
    device_id bigint unsigned COMMENT '终端ID',
    status tinyint COMMENT '状态',
    change_time datetime COMMENT '状态变化时间',
    reason varchar(200) COMMENT '变化原因',
    deleted tinyint COMMENT '是否删除',
    created_on datetime COMMENT '创建时间'
) COMMENT='device_online_log表';
