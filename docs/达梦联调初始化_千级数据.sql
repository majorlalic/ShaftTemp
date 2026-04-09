-- 达梦联调初始化数据（千级）
-- 适用：ODS_DWEQ_DM_*_D 表结构
-- 目标：一键准备接口联调数据（组织/区域/设备/监测对象/楼层/绑定/规则/原始数据/告警/事件）

-- =========================
-- 0) 清理历史数据
-- =========================
DELETE FROM ODS_DWEQ_DM_DEVICE_ONLINE_LOG_D;
DELETE FROM ODS_DWEQ_DM_RAW_DATA_D;
DELETE FROM ODS_DWEQ_DM_EVENT_D;
DELETE FROM ODS_DWEQ_DM_ALARM_D;
DELETE FROM ODS_DWEQ_DM_ALARM_RAW_D;
DELETE FROM ODS_DWEQ_DM_ALARM_RULE_D;
DELETE FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D;
DELETE FROM ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D;
DELETE FROM ODS_DWEQ_DM_SHAFT_FLOOR_D;
DELETE FROM ODS_DWEQ_DM_MONITOR_D;
DELETE FROM ODS_DWEQ_DM_DEVICE_D;
DELETE FROM ODS_DWEQ_DM_ORG_D;
DELETE FROM ODS_DWEQ_DM_AREA_D;
COMMIT;

-- =========================
-- 1) 区域 area（50）
-- =========================
INSERT INTO ODS_DWEQ_DM_AREA_D (
    id, parent_id, name, type, path_ids, path_names, deleted, sort,
    creator, updator, created_on, updated_on
)
SELECT
    1000000 + LEVEL AS id,
    CASE WHEN LEVEL <= 10 THEN NULL ELSE 1000000 + MOD(LEVEL - 1, 10) + 1 END AS parent_id,
    '区域' || TO_CHAR(LEVEL) AS name,
    CASE
        WHEN LEVEL <= 10 THEN 'PARK'
        WHEN LEVEL <= 30 THEN 'BUILDING'
        ELSE 'FLOOR'
    END AS type,
    '/1000001/' || TO_CHAR(1000000 + LEVEL) AS path_ids,
    '/园区1/区域' || TO_CHAR(LEVEL) AS path_names,
    0 AS deleted,
    LEVEL AS sort,
    1 AS creator,
    1 AS updator,
    SYSDATE AS created_on,
    SYSDATE AS updated_on
FROM dual
CONNECT BY LEVEL <= 50;

-- =========================
-- 2) 机构 org（30）
-- =========================
INSERT INTO ODS_DWEQ_DM_ORG_D (
    id, parent_id, name, type, path_ids, path_names, deleted, sort,
    creator, updator, created_on, updated_on
)
SELECT
    2000000 + LEVEL AS id,
    CASE WHEN LEVEL <= 6 THEN NULL ELSE 2000000 + MOD(LEVEL - 1, 6) + 1 END AS parent_id,
    '机构' || TO_CHAR(LEVEL) AS name,
    CASE
        WHEN LEVEL <= 6 THEN 'GROUP'
        WHEN LEVEL <= 18 THEN 'COMPANY'
        ELSE 'TEAM'
    END AS type,
    '/2000001/' || TO_CHAR(2000000 + LEVEL) AS path_ids,
    '/集团/机构' || TO_CHAR(LEVEL) AS path_names,
    0 AS deleted,
    LEVEL AS sort,
    1 AS creator,
    1 AS updator,
    SYSDATE AS created_on,
    SYSDATE AS updated_on
FROM dual
CONNECT BY LEVEL <= 30;

-- =========================
-- 3) 设备 device（100）
-- iot_code 形如 shaft-dev-001 ... shaft-dev-100
-- =========================
INSERT INTO ODS_DWEQ_DM_DEVICE_D (
    id, device_type, name, area_id, iot_code, model, manufacturer,
    factory_date, run_date, asset_status, org_id, remark,
    online_status, last_report_time, last_offline_time, deleted,
    creator, updator, created_on, updated_on
)
SELECT
    3000000 + LEVEL AS id,
    'TEMP_HOST' AS device_type,
    '测温终端-' || LPAD(TO_CHAR(LEVEL), 3, '0') AS name,
    1000000 + MOD(LEVEL - 1, 50) + 1 AS area_id,
    'shaft-dev-' || LPAD(TO_CHAR(LEVEL), 3, '0') AS iot_code,
    'X' || TO_CHAR(100 + MOD(LEVEL, 5)) AS model,
    '厂商A' AS manufacturer,
    TO_DATE('2024-01-01', 'YYYY-MM-DD') + MOD(LEVEL, 300) AS factory_date,
    TO_DATE('2024-06-01', 'YYYY-MM-DD') + MOD(LEVEL, 250) AS run_date,
    CASE MOD(LEVEL, 3)
        WHEN 0 THEN '待接入'
        WHEN 1 THEN '已接入'
        ELSE '停用'
    END AS asset_status,
    2000000 + MOD(LEVEL - 1, 30) + 1 AS org_id,
    '联调数据' AS remark,
    CASE WHEN MOD(LEVEL, 10) = 0 THEN 0 ELSE 1 END AS online_status,
    SYSDATE - (LEVEL / 1440) AS last_report_time,
    CASE WHEN MOD(LEVEL, 10) = 0 THEN SYSDATE - (LEVEL / 1200) ELSE NULL END AS last_offline_time,
    0 AS deleted,
    1 AS creator,
    1 AS updator,
    SYSDATE AS created_on,
    SYSDATE AS updated_on
FROM dual
CONNECT BY LEVEL <= 100;

-- =========================
-- 4) 监测对象 monitor（100）
-- 一设备对应一监测对象，便于联调
-- =========================
INSERT INTO ODS_DWEQ_DM_MONITOR_D (
    id, name, area_id, area_name, elevator_count, shaft_type, monitor_status,
    build_date, owner_company, device_id, remark, deleted,
    creator, updator, created_on, updated_on
)
SELECT
    4000000 + LEVEL AS id,
    '监测对象-' || LPAD(TO_CHAR(LEVEL), 3, '0') AS name,
    1000000 + MOD(LEVEL - 1, 50) + 1 AS area_id,
    '区域路径/区域' || TO_CHAR(MOD(LEVEL - 1, 50) + 1) AS area_name,
    MOD(LEVEL, 8) + 1 AS elevator_count,
    CASE MOD(LEVEL, 3)
        WHEN 0 THEN 'VERTICAL_SHAFT'
        WHEN 1 THEN 'TUNNEL'
        ELSE 'PIPE_GALLERY'
    END AS shaft_type,
    CASE MOD(LEVEL, 3)
        WHEN 0 THEN 'NORMAL'
        WHEN 1 THEN 'FOCUS'
        ELSE 'MAINTENANCE'
    END AS monitor_status,
    TO_DATE('2020-01-01', 'YYYY-MM-DD') + MOD(LEVEL, 1000) AS build_date,
    '产权单位A' AS owner_company,
    3000000 + LEVEL AS device_id,
    '联调监测对象' AS remark,
    0 AS deleted,
    1 AS creator,
    1 AS updator,
    SYSDATE AS created_on,
    SYSDATE AS updated_on
FROM dual
CONNECT BY LEVEL <= 100;

-- =========================
-- 5) 楼层 shaft_floor（400）
-- 每监测对象 4 个楼层：B2/B1/F1/F2
-- =========================
INSERT INTO ODS_DWEQ_DM_SHAFT_FLOOR_D (
    id, monitor_id, area_id, name, device_id, start_point, end_point, sort,
    deleted, creator, updator, created_on, updated_on
)
SELECT
    5000000 + LEVEL AS id,
    4000000 + TRUNC((LEVEL - 1) / 4) + 1 AS monitor_id,
    1000000 + MOD(TRUNC((LEVEL - 1) / 4), 50) + 1 AS area_id,
    CASE MOD(LEVEL - 1, 4)
        WHEN 0 THEN 'B2'
        WHEN 1 THEN 'B1'
        WHEN 2 THEN 'F1'
        ELSE 'F2'
    END AS name,
    3000000 + TRUNC((LEVEL - 1) / 4) + 1 AS device_id,
    MOD(LEVEL, 10) * 10 + 1 AS start_point,
    MOD(LEVEL, 10) * 10 + 10 AS end_point,
    MOD(LEVEL - 1, 4) + 1 AS sort,
    0 AS deleted,
    1 AS creator,
    1 AS updator,
    SYSDATE AS created_on,
    SYSDATE AS updated_on
FROM dual
CONNECT BY LEVEL <= 400;

-- =========================
-- 6) 监测对象-设备绑定（100）
-- =========================
INSERT INTO ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D (
    id, monitor_id, device_id, bind_status, bind_time, unbind_time,
    deleted, creator, updator, created_on, updated_on
)
SELECT
    6000000 + LEVEL AS id,
    4000000 + LEVEL AS monitor_id,
    3000000 + LEVEL AS device_id,
    1 AS bind_status,
    SYSDATE - 10 AS bind_time,
    NULL AS unbind_time,
    0 AS deleted,
    1 AS creator,
    1 AS updator,
    SYSDATE AS created_on,
    SYSDATE AS updated_on
FROM dual
CONNECT BY LEVEL <= 100;

-- =========================
-- 7) 分区绑定 monitor_partition_bind（5000）
-- 100 台设备 * 每台 50 分区
-- =========================
INSERT INTO ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D (
    id, monitor_id, device_id, shaft_floor_id,
    partition_id, partition_code, partition_name,
    data_reference, device_token, partition_no, bind_status, deleted,
    creator, updator, created_on, updated_on
)
SELECT
    7000000 + LEVEL AS id,
    4000000 + TRUNC((LEVEL - 1) / 50) + 1 AS monitor_id,
    3000000 + TRUNC((LEVEL - 1) / 50) + 1 AS device_id,
    5000000
        + (TRUNC((LEVEL - 1) / 50) * 4)
        + CASE
            WHEN MOD(LEVEL - 1, 50) + 1 <= 12 THEN 1
            WHEN MOD(LEVEL - 1, 50) + 1 <= 25 THEN 2
            WHEN MOD(LEVEL - 1, 50) + 1 <= 38 THEN 3
            ELSE 4
          END AS shaft_floor_id,
    MOD(LEVEL - 1, 50) + 1 AS partition_id,
    'shaft-dev-' || LPAD(TO_CHAR(TRUNC((LEVEL - 1) / 50) + 1), 3, '0')
        || '_TMP_th' || LPAD(TO_CHAR(MOD(LEVEL - 1, 50) + 1), 2, '0') AS partition_code,
    '分区-' || LPAD(TO_CHAR(MOD(LEVEL - 1, 50) + 1), 2, '0') AS partition_name,
    '/TMP/shaft-dev-' || LPAD(TO_CHAR(TRUNC((LEVEL - 1) / 50) + 1), 3, '0')
        || '_TMP_th' || LPAD(TO_CHAR(MOD(LEVEL - 1, 50) + 1), 2, '0') AS data_reference,
    'shaft-dev-' || LPAD(TO_CHAR(TRUNC((LEVEL - 1) / 50) + 1), 3, '0') AS device_token,
    MOD(LEVEL - 1, 50) + 1 AS partition_no,
    1 AS bind_status,
    0 AS deleted,
    1 AS creator,
    1 AS updator,
    SYSDATE AS created_on,
    SYSDATE AS updated_on
FROM dual
CONNECT BY LEVEL <= 5000;

-- =========================
-- 8) 告警规则（5）
-- =========================
INSERT INTO ODS_DWEQ_DM_ALARM_RULE_D (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    8100001, '温度阈值', 'MONITOR', 'TEMP_THRESHOLD', 'GLOBAL', NULL,
    2, 70.00, NULL, NULL, 1, '默认规则', 0, SYSDATE, SYSDATE
);

INSERT INTO ODS_DWEQ_DM_ALARM_RULE_D (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    8100002, '差温阈值', 'MONITOR', 'TEMP_DIFFERENCE', 'GLOBAL', NULL,
    2, 20.00, NULL, NULL, 1, '默认规则', 0, SYSDATE, SYSDATE
);

INSERT INTO ODS_DWEQ_DM_ALARM_RULE_D (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    8100003, '升温速率', 'MONITOR', 'TEMP_RISE_RATE', 'GLOBAL', NULL,
    2, 10.00, NULL, 60, 1, '默认规则', 0, SYSDATE, SYSDATE
);

INSERT INTO ODS_DWEQ_DM_ALARM_RULE_D (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    8100004, '设备离线', 'DEVICE', 'DEVICE_OFFLINE', 'GLOBAL', NULL,
    1, 30.00, NULL, NULL, 1, '默认规则', 0, SYSDATE, SYSDATE
);

INSERT INTO ODS_DWEQ_DM_ALARM_RULE_D (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    8100005, '分区断纤', 'DEVICE', 'PARTITION_FAULT', 'GLOBAL', NULL,
    1, 0.00, NULL, NULL, 1, '默认规则', 0, SYSDATE, SYSDATE
);

-- =========================
-- 9) 原始测温 raw_data（5000）
-- =========================
INSERT INTO ODS_DWEQ_DM_RAW_DATA_D (
    id, device_id, iot_code, topic, partition_id, monitor_id, shaft_floor_id,
    data_reference, ied_full_path, collect_time,
    max_temp, min_temp, avg_temp, max_temp_position, min_temp_position, max_temp_channel, min_temp_channel,
    payload_json, deleted, created_on
)
SELECT
    9000000 + t.rn AS id,
    t.device_id,
    t.device_token AS iot_code,
    t.data_reference || '/Measure' AS topic,
    t.partition_id,
    t.monitor_id,
    t.shaft_floor_id,
    t.data_reference,
    '/IED/' || t.device_token AS ied_full_path,
    SYSDATE - (t.rn / 1440) AS collect_time,
    65 + MOD(t.partition_id, 15) + MOD(t.rn, 5) * 0.1 AS max_temp,
    45 + MOD(t.partition_id, 10) + MOD(t.rn, 3) * 0.1 AS min_temp,
    55 + MOD(t.partition_id, 12) + MOD(t.rn, 4) * 0.1 AS avg_temp,
    100 + MOD(t.partition_id, 20) AS max_temp_position,
    20 + MOD(t.partition_id, 15) AS min_temp_position,
    MOD(t.partition_id, 4) + 1 AS max_temp_channel,
    MOD(t.partition_id + 1, 4) + 1 AS min_temp_channel,
    '{"source":"init","partitionId":' || TO_CHAR(t.partition_id) || '}' AS payload_json,
    0 AS deleted,
    SYSDATE AS created_on
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY id) rn,
        monitor_id, device_id, shaft_floor_id, partition_id, data_reference, device_token
    FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D
    WHERE deleted = 0
) t
WHERE t.rn <= 5000;

-- =========================
-- 10) 告警原始报警 alarm_raw（1200）
-- =========================
INSERT INTO ODS_DWEQ_DM_ALARM_RAW_D (
    id, iot_code, topic, partition_id, alarm_status, fault_status,
    ied_full_path, data_reference, collect_time, payload_json, deleted, created_on
)
SELECT
    9100000 + t.rn AS id,
    t.device_token AS iot_code,
    t.data_reference || '/Alarm' AS topic,
    t.partition_id,
    CASE WHEN MOD(t.rn, 5) = 0 THEN 1 ELSE 0 END AS alarm_status,
    CASE WHEN MOD(t.rn, 17) = 0 THEN 1 ELSE 0 END AS fault_status,
    '/IED/' || t.device_token AS ied_full_path,
    t.data_reference,
    SYSDATE - (t.rn / 1800) AS collect_time,
    '{"source":"init-alarm","partitionId":' || TO_CHAR(t.partition_id) || '}' AS payload_json,
    0 AS deleted,
    SYSDATE AS created_on
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY id) rn,
        partition_id, data_reference, device_token
    FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D
    WHERE deleted = 0
) t
WHERE t.rn <= 1200;

-- =========================
-- 11) 告警 alarm（600）
-- - 前 200 条待确认（merge_key 唯一）
-- - 其余按新状态流转（持续观察/待消缺/待复测/已确认/闭环）
-- =========================
INSERT INTO ODS_DWEQ_DM_ALARM_D (
    id, alarm_code, alarm_type, source_type, monitor_id, device_id, shaft_floor_id,
    partition_code, partition_name, data_reference, device_token, partition_no, source_format,
    merge_key, status, first_alarm_time, last_alarm_time, merge_count, event_count,
    alarm_level, title, content, handler, handle_time, handle_remark, push_status,
    alarm_type_big, area_name, monitor_name, device_name, handler_name, manufacturer, device_model, push_time,
    deleted, created_on, updated_on
)
SELECT
    TO_CHAR(9200000 + t.rn) AS id,
    'ALM-' || TO_CHAR(9200000 + t.rn) AS alarm_code,
    CASE MOD(t.rn, 5)
        WHEN 0 THEN 'TEMP_THRESHOLD'
        WHEN 1 THEN 'TEMP_DIFFERENCE'
        WHEN 2 THEN 'TEMP_RISE_RATE'
        WHEN 3 THEN 'DEVICE_OFFLINE'
        ELSE 'PARTITION_FAULT'
    END AS alarm_type,
    CASE WHEN MOD(t.rn, 5) IN (3, 4) THEN 'DEVICE_REPORT' ELSE 'MEASURE_REPORT' END AS source_type,
    TO_CHAR(t.monitor_id),
    TO_CHAR(t.device_id),
    t.shaft_floor_id,
    t.partition_code,
    t.partition_name,
    t.data_reference,
    t.device_token,
    t.partition_id AS partition_no,
    'PARTITION' AS source_format,
    CASE
        WHEN t.rn <= 200 THEN
            'M:' || TO_CHAR(t.monitor_id) || ':'
            || CASE MOD(t.rn, 5)
                WHEN 0 THEN 'TEMP_THRESHOLD'
                WHEN 1 THEN 'TEMP_DIFFERENCE'
                WHEN 2 THEN 'TEMP_RISE_RATE'
                WHEN 3 THEN 'DEVICE_OFFLINE'
                ELSE 'PARTITION_FAULT'
               END
            || ':' || TO_CHAR(t.rn)
        ELSE NULL
    END AS merge_key,
    CASE
        WHEN t.rn <= 200 THEN 0
        WHEN MOD(t.rn, 5) = 0 THEN 1
        WHEN MOD(t.rn, 5) = 1 THEN 2
        WHEN MOD(t.rn, 5) = 2 THEN 3
        WHEN MOD(t.rn, 5) = 3 THEN 4
        ELSE 5
    END AS status,
    SYSDATE - (t.rn / 1000) AS first_alarm_time,
    SYSDATE - (t.rn / 2000) AS last_alarm_time,
    MOD(t.rn, 9) + 1 AS merge_count,
    MOD(t.rn, 9) + 1 AS event_count,
    CASE WHEN MOD(t.rn, 3) = 0 THEN 1 ELSE 2 END AS alarm_level,
    '联调告警-' || TO_CHAR(t.rn) AS title,
    t.partition_name || ' 温度告警，联调样例 #' || TO_CHAR(t.rn) AS content,
    CASE WHEN t.rn <= 200 THEN NULL ELSE '10001' END AS handler,
    CASE WHEN t.rn <= 200 THEN NULL ELSE SYSDATE - (t.rn / 3000) END AS handle_time,
    CASE WHEN t.rn <= 200 THEN NULL ELSE '联调处理' END AS handle_remark,
    CASE WHEN MOD(t.rn, 3) = 0 THEN 1 ELSE 0 END AS push_status,
    0 AS alarm_type_big,
    t.area_name,
    t.monitor_name,
    t.device_name,
    CASE WHEN t.rn <= 200 THEN NULL ELSE '运维值班员A' END AS handler_name,
    t.manufacturer,
    t.device_model,
    CASE WHEN MOD(t.rn, 3) = 0 THEN SYSDATE - (t.rn / 2500) ELSE NULL END AS push_time,
    0 AS deleted,
    SYSDATE AS created_on,
    SYSDATE AS updated_on
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY b.id) rn,
        b.monitor_id, b.device_id, b.shaft_floor_id, b.partition_id,
        b.partition_code, b.partition_name, b.data_reference, b.device_token,
        m.area_name AS area_name,
        m.name AS monitor_name,
        d.name AS device_name,
        d.manufacturer AS manufacturer,
        d.model AS device_model
    FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D b
    JOIN ODS_DWEQ_DM_MONITOR_D m ON m.id = b.monitor_id AND m.deleted = 0
    JOIN ODS_DWEQ_DM_DEVICE_D d ON d.id = b.device_id AND d.deleted = 0
    WHERE b.deleted = 0
) t
WHERE t.rn <= 600;

-- =========================
-- 12) 事件 event（1800）
-- 每个告警约 3 条事件
-- =========================
INSERT INTO ODS_DWEQ_DM_EVENT_D (
    id, alarm_id, alarm_type, source_type, monitor_id, device_id, shaft_floor_id,
    partition_code, partition_name, data_reference, device_token, partition_no, source_format,
    event_type, event_time, event_no, event_level, point_list_json, detail_json, content,
    merged_flag, deleted, created_on, updated_on
)
SELECT
    9300000 + x.rn AS id,
    x.alarm_id,
    x.alarm_type,
    x.source_type,
    x.monitor_id,
    x.device_id,
    x.shaft_floor_id,
    x.partition_code,
    x.partition_name,
    x.data_reference,
    x.device_token,
    x.partition_no,
    x.source_format,
    x.event_type,
    x.event_time,
    x.event_no,
    x.event_level,
    x.point_list_json,
    x.detail_json,
    x.content,
    x.merged_flag,
    0 AS deleted,
    SYSDATE AS created_on,
    SYSDATE AS updated_on
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY TO_NUMBER(a.id), e.seq_no) rn,
        TO_NUMBER(a.id) AS alarm_id,
        a.alarm_type,
        a.source_type,
        TO_NUMBER(a.monitor_id) AS monitor_id,
        TO_NUMBER(a.device_id) AS device_id,
        a.shaft_floor_id,
        a.partition_code,
        a.partition_name,
        a.data_reference,
        a.device_token,
        a.partition_no,
        a.source_format,
        CASE e.seq_no
            WHEN 1 THEN 0
            WHEN 2 THEN 1
            ELSE CASE WHEN a.status IN (1,2,3,4,5) THEN 2 ELSE 0 END
        END AS event_type,
        a.last_alarm_time - (e.seq_no / 86400) AS event_time,
        e.seq_no AS event_no,
        a.alarm_level AS event_level,
        '[]' AS point_list_json,
        '{"source":"init-event","seq":' || TO_CHAR(e.seq_no) || '}' AS detail_json,
        a.content || ' 事件' || TO_CHAR(e.seq_no) AS content,
        CASE WHEN e.seq_no = 2 THEN 1 ELSE 0 END AS merged_flag
    FROM ODS_DWEQ_DM_ALARM_D a
    CROSS JOIN (
        SELECT 1 AS seq_no FROM dual
        UNION ALL SELECT 2 AS seq_no FROM dual
        UNION ALL SELECT 3 AS seq_no FROM dual
    ) e
    WHERE a.deleted = 0
) x
WHERE x.rn <= 1800;

-- =========================
-- 13) 在线状态日志（1000）
-- =========================
INSERT INTO ODS_DWEQ_DM_DEVICE_ONLINE_LOG_D (
    id, device_id, status, change_time, reason, deleted, created_on
)
SELECT
    9400000 + LEVEL AS id,
    3000000 + MOD(LEVEL - 1, 100) + 1 AS device_id,
    CASE WHEN MOD(LEVEL, 6) = 0 THEN 0 ELSE 1 END AS status,
    SYSDATE - (LEVEL / 1200) AS change_time,
    CASE WHEN MOD(LEVEL, 6) = 0 THEN 'offline inspection' ELSE 'received report' END AS reason,
    0 AS deleted,
    SYSDATE AS created_on
FROM dual
CONNECT BY LEVEL <= 1000;

COMMIT;

-- =========================
-- 14) 数据量检查
-- =========================
SELECT 'AREA' AS t, COUNT(*) AS c FROM ODS_DWEQ_DM_AREA_D
UNION ALL SELECT 'ORG', COUNT(*) FROM ODS_DWEQ_DM_ORG_D
UNION ALL SELECT 'DEVICE', COUNT(*) FROM ODS_DWEQ_DM_DEVICE_D
UNION ALL SELECT 'MONITOR', COUNT(*) FROM ODS_DWEQ_DM_MONITOR_D
UNION ALL SELECT 'SHAFT_FLOOR', COUNT(*) FROM ODS_DWEQ_DM_SHAFT_FLOOR_D
UNION ALL SELECT 'MONITOR_DEVICE_BIND', COUNT(*) FROM ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D
UNION ALL SELECT 'MONITOR_PARTITION_BIND', COUNT(*) FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D
UNION ALL SELECT 'ALARM_RULE', COUNT(*) FROM ODS_DWEQ_DM_ALARM_RULE_D
UNION ALL SELECT 'RAW_DATA', COUNT(*) FROM ODS_DWEQ_DM_RAW_DATA_D
UNION ALL SELECT 'ALARM_RAW', COUNT(*) FROM ODS_DWEQ_DM_ALARM_RAW_D
UNION ALL SELECT 'ALARM', COUNT(*) FROM ODS_DWEQ_DM_ALARM_D
UNION ALL SELECT 'EVENT', COUNT(*) FROM ODS_DWEQ_DM_EVENT_D
UNION ALL SELECT 'DEVICE_ONLINE_LOG', COUNT(*) FROM ODS_DWEQ_DM_DEVICE_ONLINE_LOG_D;
