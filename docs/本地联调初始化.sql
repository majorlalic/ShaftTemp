-- 本地联调初始化数据
-- 执行前请先运行：数据库建库.sql

USE shaft;

DELETE FROM device_online_log;
DELETE FROM temp_stat_minute;
DELETE FROM raw_data;
DELETE FROM event;
DELETE FROM alarm;
DELETE FROM monitor_partition_bind;
DELETE FROM monitor_device_bind;
DELETE FROM shaft_floor;
DELETE FROM monitor;
DELETE FROM device;
DELETE FROM org;
DELETE FROM area;

INSERT INTO area (id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on)
VALUES
    (1001, 0, '测试区域', 'COMMUNITY', '1001', '测试区域', 0, 1, NOW(), NOW());

INSERT INTO org (id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on)
VALUES
    (2001, 0, '测试机构', 'OWNER', '2001', '测试机构', 0, 1, NOW(), NOW());

INSERT INTO device (
    id, device_type, name, area_id, iot_code, model, manufacturer, asset_status, org_id, remark,
    online_status, deleted, created_on, updated_on
) VALUES (
    3001, 'SHAFT_TEMP', '竖井测温终端A', 1001, 'shaft-dev-01', 'TMP-V1', 'LOCAL', 'IN_USE', 2001, '本地联调设备',
    0, 0, NOW(), NOW()
);

INSERT INTO monitor (
    id, name, area_id, area_name, elevator_count, shaft_type, monitor_status, owner_company, device_id,
    remark, deleted, created_on, updated_on
) VALUES (
    4001, '1号竖井', 1001, '测试区域/1号竖井', 2, 'POWER', 'RUNNING', '测试单位', 3001,
    '本地联调监测对象', 0, NOW(), NOW()
);

INSERT INTO shaft_floor (
    id, monitor_id, area_id, name, device_id, start_point, end_point, sort, deleted, created_on, updated_on
) VALUES
    (5001, 4001, 1001, '1层', 3001, 0, 0, 1, 0, NOW(), NOW()),
    (5002, 4001, 1001, '2层', 3001, 0, 0, 2, 0, NOW(), NOW());

INSERT INTO monitor_device_bind (
    id, monitor_id, device_id, bind_status, bind_time, deleted, created_on, updated_on
) VALUES (
    6001, 4001, 3001, 1, NOW(), 0, NOW(), NOW()
);

INSERT INTO monitor_partition_bind (
    id, monitor_id, device_id, shaft_floor_id, partition_code, partition_name, data_reference,
    device_token, partition_no, bind_status, deleted, created_on, updated_on
) VALUES
    (7001, 4001, 3001, 5001, 'shaft-dev-01_TMP_th01', '1层分区', '/TMP/shaft-dev-01_TMP_th01', 'shaft-dev-01', 1, 1, 0, NOW(), NOW()),
    (7002, 4001, 3001, 5002, 'shaft-dev-01_TMP_th02', '2层分区', '/TMP/shaft-dev-01_TMP_th02', 'shaft-dev-01', 2, 1, 0, NOW(), NOW());
