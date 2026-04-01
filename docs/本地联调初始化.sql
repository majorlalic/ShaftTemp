-- 本地联调初始化数据
-- 执行前请先运行：数据库建库.sql
-- 当前脚本只清理固定业务表

-- USE shaft;

DELETE FROM ODS_DWEQ_DM_DEVICE_ONLINE_LOG_D;
DELETE FROM ODS_DWEQ_DM_RAW_DATA_D;
DELETE FROM ODS_DWEQ_DM_ALARM_RAW_D;
DELETE FROM ODS_DWEQ_DM_EVENT_D;
DELETE FROM ODS_DWEQ_DM_ALARM_D;
DELETE FROM ODS_DWEQ_DM_ALARM_RULE_D;
DELETE FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D;
DELETE FROM ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D;
DELETE FROM ODS_DWEQ_DM_SHAFT_FLOOR_D;
DELETE FROM ODS_DWEQ_DM_MONITOR_D;
DELETE FROM ODS_DWEQ_DM_DEVICE_D;
DELETE FROM ODS_DWEQ_DM_ORG_D;
DELETE FROM ODS_DWEQ_DM_AREA_D;

INSERT INTO ODS_DWEQ_DM_AREA_D (id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on)
VALUES
    (1001, 0, '测试区域', 'COMMUNITY', '1001', '测试区域', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO ODS_DWEQ_DM_ORG_D (id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on)
VALUES
    (2001, 0, '测试机构', 'OWNER', '2001', '测试机构', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO ODS_DWEQ_DM_DEVICE_D (
    id, device_type, name, area_id, iot_code, model, manufacturer, asset_status, org_id, remark,
    online_status, deleted, created_on, updated_on
) VALUES (
    3001, 'SHAFT_TEMP', '竖井测温终端A', 1001, 'shaft-dev-01', 'TMP-V1', 'LOCAL', 'CONNECTED', 2001, '本地联调设备',
    0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ODS_DWEQ_DM_MONITOR_D (
    id, name, area_id, area_name, elevator_count, shaft_type, monitor_status, owner_company, device_id,
    remark, deleted, created_on, updated_on
) VALUES (
    4001, '1号竖井', 1001, '测试区域/1号竖井', 2, 'POWER', 'RUNNING', '测试单位', 3001,
    '本地联调监测对象', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ODS_DWEQ_DM_SHAFT_FLOOR_D (
    id, monitor_id, area_id, name, device_id, start_point, end_point, sort, deleted, created_on, updated_on
) VALUES
    (5001, 4001, 1001, '1层', 3001, 0, 0, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5002, 4001, 1001, '2层', 3001, 0, 0, 2, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D (
    id, monitor_id, device_id, bind_status, bind_time, deleted, created_on, updated_on
) VALUES (
    6001, 4001, 3001, 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D (
    id, monitor_id, device_id, shaft_floor_id, partition_id, partition_code, partition_name, data_reference,
    device_token, partition_no, bind_status, deleted, created_on, updated_on
) VALUES
    (7001, 4001, 3001, 5001, 1, 'shaft-dev-01_TMP_th01', '1层分区', '/TMP/shaft-dev-01_TMP_th01', 'shaft-dev-01', 1, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (7002, 4001, 3001, 5002, 2, 'shaft-dev-01_TMP_th02', '2层分区', '/TMP/shaft-dev-01_TMP_th02', 'shaft-dev-01', 2, 1, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO ODS_DWEQ_DM_ALARM_RULE_D
(id, rule_name, biz_type, alarm_type, scope_type, scope_id, level, threshold_value, threshold_value2, duration_seconds, enabled, remark, deleted, created_on, updated_on)
VALUES
(9001, '全局定温告警', 'MONITOR', 'TEMP_THRESHOLD', 'GLOBAL', NULL, 2, 70.00, NULL, NULL, 1, '默认定温阈值', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9002, '全局差温告警', 'MONITOR', 'TEMP_DIFFERENCE', 'GLOBAL', NULL, 2, 15.00, NULL, NULL, 1, '默认差温阈值', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9003, '全局升温速率告警', 'MONITOR', 'TEMP_RISE_RATE', 'GLOBAL', NULL, 2, 8.00, NULL, 300, 1, '默认升温速率阈值', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9004, '全局离线告警', 'DEVICE', 'DEVICE_OFFLINE', 'GLOBAL', NULL, 3, 300.00, NULL, NULL, 1, '默认离线阈值5分钟', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(9005, '全局分区故障告警', 'DEVICE', 'PARTITION_FAULT', 'GLOBAL', NULL, 3, NULL, NULL, NULL, 1, '默认分区故障启用', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
