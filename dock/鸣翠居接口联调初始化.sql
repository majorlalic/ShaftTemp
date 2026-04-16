-- 鸣翠居接口联调初始化（达梦）
-- 前置：请先执行 dock/鸣翠居主数据初始化.sql
-- 目标：
-- 1) org 与 area 保持同一套层级数据
-- 2) 补齐接口联调数据（rule/raw_data/alarm_raw/alarm/event/device_online_log）
-- 3) 单表数据量控制在 1~2000

-- =========================
-- 0) 清理本脚本ID段
-- =========================
DELETE FROM ODS_DWEQ_DM_DEVICE_ONLINE_LOG_D WHERE id BETWEEN 9940001 AND 9941200;
DELETE FROM ODS_DWEQ_DM_RAW_DATA_D WHERE id BETWEEN 9960001 AND 9961800;
DELETE FROM ODS_DWEQ_DM_ALARM_RAW_D WHERE id BETWEEN 9970001 AND 9971200;
DELETE FROM ODS_DWEQ_DM_EVENT_D WHERE id BETWEEN 9990001 AND 9991600;
DELETE FROM ODS_DWEQ_DM_ALARM_D WHERE id BETWEEN '9980001' AND '9980800';
DELETE FROM ODS_DWEQ_DM_ALARM_RULE_D WHERE id BETWEEN 9950001 AND 9950005;

-- org 镜像清理（与 area 同ID段）
DELETE FROM ODS_DWEQ_DM_ORG_D WHERE id BETWEEN 9100001 AND 9400580;

COMMIT;

-- =========================
-- 1) org 与 area 保持一致（615）
-- =========================
INSERT INTO ODS_DWEQ_DM_ORG_D (
    id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on
)
SELECT
    a.id,
    a.parent_id,
    a.name,
    a.type,
    a.path_ids,
    a.path_names,
    0 AS deleted,
    a.sort,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM ODS_DWEQ_DM_AREA_D a
WHERE a.id BETWEEN 9100001 AND 9400580
  AND (a.deleted IS NULL OR a.deleted = 0);

-- =========================
-- 2) 告警规则（5）
-- =========================
INSERT INTO ODS_DWEQ_DM_ALARM_RULE_D (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    9950001, '温度阈值', 'MONITOR', 'TEMP_THRESHOLD', 'GLOBAL', NULL,
    2, 70.00, NULL, NULL, 1, '鸣翠居联调默认规则', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ODS_DWEQ_DM_ALARM_RULE_D (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    9950002, '差温阈值', 'MONITOR', 'TEMP_DIFFERENCE', 'GLOBAL', NULL,
    2, 20.00, NULL, NULL, 1, '鸣翠居联调默认规则', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ODS_DWEQ_DM_ALARM_RULE_D (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    9950003, '升温速率', 'MONITOR', 'TEMP_RISE_RATE', 'GLOBAL', NULL,
    2, 10.00, NULL, 60, 1, '鸣翠居联调默认规则', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ODS_DWEQ_DM_ALARM_RULE_D (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    9950004, '设备离线', 'DEVICE', 'DEVICE_OFFLINE', 'GLOBAL', NULL,
    1, 30.00, NULL, NULL, 1, '鸣翠居联调默认规则', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ODS_DWEQ_DM_ALARM_RULE_D (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    9950005, '分区断纤', 'DEVICE', 'PARTITION_FAULT', 'GLOBAL', NULL,
    1, 0.00, NULL, NULL, 1, '鸣翠居联调默认规则', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- =========================
-- 3) 原始测温 raw_data（1800）
-- =========================
INSERT INTO ODS_DWEQ_DM_RAW_DATA_D (
    id, device_id, iot_code, topic, partition_id, monitor_id, shaft_floor_id,
    data_reference, ied_full_path, collect_time,
    max_temp, min_temp, avg_temp, max_temp_position, min_temp_position,
    max_temp_channel, min_temp_channel, payload_json, deleted, created_on
)
SELECT
    9960000 + t.rn AS id,
    t.device_id,
    t.device_token AS iot_code,
    t.data_reference || '/Measure' AS topic,
    t.partition_id,
    t.monitor_id,
    t.shaft_floor_id,
    t.data_reference,
    '/IED/' || t.device_token AS ied_full_path,
    CURRENT_TIMESTAMP - (t.rn / 1440) AS collect_time,
    62 + MOD(t.partition_id, 15) + MOD(t.rn, 4) * 0.1 AS max_temp,
    40 + MOD(t.partition_id, 10) + MOD(t.rn, 3) * 0.1 AS min_temp,
    52 + MOD(t.partition_id, 12) + MOD(t.rn, 5) * 0.1 AS avg_temp,
    80 + MOD(t.partition_id, 20) AS max_temp_position,
    20 + MOD(t.partition_id, 15) AS min_temp_position,
    MOD(t.partition_id, 4) + 1 AS max_temp_channel,
    MOD(t.partition_id + 1, 4) + 1 AS min_temp_channel,
    '{"scene":"dock-init","partitionId":' || TO_CHAR(t.partition_id) || '}' AS payload_json,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY id) rn,
        monitor_id, device_id, shaft_floor_id, partition_id, data_reference, device_token
    FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D
    WHERE id BETWEEN 9900001 AND 9900580
      AND (deleted IS NULL OR deleted = 0)
) t
WHERE t.rn <= 1800;

-- =========================
-- 4) 告警原始数据 alarm_raw（1200）
-- =========================
INSERT INTO ODS_DWEQ_DM_ALARM_RAW_D (
    id, iot_code, topic, partition_id, alarm_status, fault_status,
    ied_full_path, data_reference, collect_time, payload_json, deleted, created_on
)
SELECT
    9970000 + t.rn AS id,
    t.device_token AS iot_code,
    t.data_reference || '/Alarm' AS topic,
    t.partition_id,
    CASE WHEN MOD(t.rn, 4) = 0 THEN 1 ELSE 0 END AS alarm_status,
    CASE WHEN MOD(t.rn, 13) = 0 THEN 1 ELSE 0 END AS fault_status,
    '/IED/' || t.device_token AS ied_full_path,
    t.data_reference,
    CURRENT_TIMESTAMP - (t.rn / 1800) AS collect_time,
    '{"scene":"dock-init-alarm","partitionId":' || TO_CHAR(t.partition_id) || '}' AS payload_json,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY id) rn,
        partition_id, data_reference, device_token
    FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D
    WHERE id BETWEEN 9900001 AND 9900580
      AND (deleted IS NULL OR deleted = 0)
) t
WHERE t.rn <= 1200;

-- =========================
-- 5) 告警 alarm（800）
-- - 监测对象告警 + 终端告警混合
-- - alarm_domain: 0终端，1监测对象
-- - status 覆盖 0/1/2/3/4/5
-- =========================
INSERT INTO ODS_DWEQ_DM_ALARM_D (
    id, alarm_code, alarm_type, source_type, monitor_id, device_id, shaft_floor_id,
    partition_code, partition_name, data_reference, device_token, partition_no, source_format,
    merge_key, status, first_alarm_time, last_alarm_time, merge_count, event_count,
    alarm_level, title, content, handler, handle_time, handle_remark, push_status,
    alarm_type_big, alarm_domain, area_name, monitor_name, device_name, handler_name, manufacturer, device_model, push_time,
    deleted, created_on, updated_on
)
SELECT
    TO_CHAR(9980000 + t.rn) AS id,
    'MCJ-ALM-' || TO_CHAR(9980000 + t.rn) AS alarm_code,
    t.alarm_type,
    CASE WHEN t.alarm_type IN ('DEVICE_OFFLINE', 'PARTITION_FAULT') THEN 'DEVICE_REPORT' ELSE 'MEASURE_REPORT' END AS source_type,
    TO_CHAR(t.monitor_id) AS monitor_id,
    TO_CHAR(t.device_id) AS device_id,
    t.shaft_floor_id,
    t.partition_code,
    t.partition_name,
    t.data_reference,
    t.device_token,
    t.partition_id AS partition_no,
    'PARTITION' AS source_format,
    CASE
        WHEN t.rn <= 220 THEN
            'M:' || TO_CHAR(t.monitor_id) || ':' || t.alarm_type || ':' || TO_CHAR(t.rn)
        ELSE NULL
    END AS merge_key,
    CASE
        WHEN t.rn <= 220 THEN 0
        WHEN MOD(t.rn, 5) = 0 THEN 1
        WHEN MOD(t.rn, 5) = 1 THEN 2
        WHEN MOD(t.rn, 5) = 2 THEN 3
        WHEN MOD(t.rn, 5) = 3 THEN 4
        ELSE 5
    END AS status,
    CURRENT_TIMESTAMP - (t.rn / 900) AS first_alarm_time,
    CURRENT_TIMESTAMP - (t.rn / 1600) AS last_alarm_time,
    MOD(t.rn, 7) + 1 AS merge_count,
    MOD(t.rn, 7) + 1 AS event_count,
    CASE WHEN t.alarm_type IN ('DEVICE_OFFLINE', 'PARTITION_FAULT') THEN 1 ELSE 2 END AS alarm_level,
    '鸣翠居联调告警-' || TO_CHAR(t.rn) AS title,
    t.partition_name || ' 触发 ' || t.alarm_type || '，样例#' || TO_CHAR(t.rn) AS content,
    CASE WHEN t.rn <= 220 THEN NULL ELSE 'handler-001' END AS handler,
    CASE WHEN t.rn <= 220 THEN NULL ELSE CURRENT_TIMESTAMP - (t.rn / 2200) END AS handle_time,
    CASE WHEN t.rn <= 220 THEN NULL ELSE '联调处警' END AS handle_remark,
    CASE WHEN MOD(t.rn, 3) = 0 THEN 1 ELSE 0 END AS push_status,
    0 AS alarm_type_big,
    CASE WHEN t.alarm_type IN ('DEVICE_OFFLINE', 'PARTITION_FAULT') THEN 0 ELSE 1 END AS alarm_domain,
    t.area_name,
    t.monitor_name,
    t.device_name,
    CASE WHEN t.rn <= 220 THEN NULL ELSE '运维值班员A' END AS handler_name,
    t.manufacturer,
    t.device_model,
    CASE WHEN MOD(t.rn, 3) = 0 THEN CURRENT_TIMESTAMP - (t.rn / 2000) ELSE NULL END AS push_time,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY b.id) rn,
        b.monitor_id, b.device_id, b.shaft_floor_id, b.partition_id,
        b.partition_code, b.partition_name, b.data_reference, b.device_token,
        m.area_name, m.name AS monitor_name, d.name AS device_name,
        d.manufacturer, d.model AS device_model,
        CASE MOD(ROW_NUMBER() OVER (ORDER BY b.id), 5)
            WHEN 0 THEN 'TEMP_THRESHOLD'
            WHEN 1 THEN 'TEMP_DIFFERENCE'
            WHEN 2 THEN 'TEMP_RISE_RATE'
            WHEN 3 THEN 'DEVICE_OFFLINE'
            ELSE 'PARTITION_FAULT'
        END AS alarm_type
    FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D b
    JOIN ODS_DWEQ_DM_MONITOR_D m ON m.id = b.monitor_id AND (m.deleted IS NULL OR m.deleted = 0)
    JOIN ODS_DWEQ_DM_DEVICE_D d ON d.id = b.device_id AND (d.deleted IS NULL OR d.deleted = 0)
    WHERE b.id BETWEEN 9900001 AND 9900580
      AND (b.deleted IS NULL OR b.deleted = 0)
) t
WHERE t.rn <= 800;

-- =========================
-- 6) 事件 event（1600）
-- 每条告警生成2条事件：触发 + 合并/处警
-- =========================
INSERT INTO ODS_DWEQ_DM_EVENT_D (
    id, alarm_id, alarm_type, source_type, monitor_id, device_id, shaft_floor_id,
    partition_code, partition_name, data_reference, device_token, partition_no, source_format,
    event_type, event_time, event_no, event_level, point_list_json, detail_json, content,
    merged_flag, deleted, created_on, updated_on
)
SELECT
    9990000 + x.rn AS id,
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
    '[]' AS point_list_json,
    '{"scene":"dock-event","eventNo":' || TO_CHAR(x.event_no) || '}' AS detail_json,
    x.content || ' 事件' || TO_CHAR(x.event_no) AS content,
    CASE WHEN x.event_type = 1 THEN 1 ELSE 0 END AS merged_flag,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY t.rn, e.seq_no) rn,
        9980000 + t.rn AS alarm_id,
        t.alarm_type,
        CASE WHEN t.alarm_type IN ('DEVICE_OFFLINE', 'PARTITION_FAULT') THEN 'DEVICE_REPORT' ELSE 'MEASURE_REPORT' END AS source_type,
        t.monitor_id,
        t.device_id,
        t.shaft_floor_id,
        t.partition_code,
        t.partition_name,
        t.data_reference,
        t.device_token,
        t.partition_id AS partition_no,
        'PARTITION' AS source_format,
        CASE
            WHEN e.seq_no = 1 THEN 0
            WHEN t.status = 0 THEN 1
            WHEN t.status = 1 THEN 2
            WHEN t.status = 2 THEN 3
            WHEN t.status = 3 THEN 4
            ELSE 6
        END AS event_type,
        CURRENT_TIMESTAMP - (t.rn / 1800) - (e.seq_no / 86400) AS event_time,
        e.seq_no AS event_no,
        CASE WHEN t.alarm_type IN ('DEVICE_OFFLINE', 'PARTITION_FAULT') THEN 1 ELSE 2 END AS event_level,
        t.partition_name || ' 触发 ' || t.alarm_type || '，样例#' || TO_CHAR(t.rn) AS content
    FROM (
        SELECT
            ROW_NUMBER() OVER (ORDER BY b.id) rn,
            b.monitor_id, b.device_id, b.shaft_floor_id, b.partition_id,
            b.partition_code, b.partition_name, b.data_reference, b.device_token,
            CASE MOD(ROW_NUMBER() OVER (ORDER BY b.id), 5)
                WHEN 0 THEN 'TEMP_THRESHOLD'
                WHEN 1 THEN 'TEMP_DIFFERENCE'
                WHEN 2 THEN 'TEMP_RISE_RATE'
                WHEN 3 THEN 'DEVICE_OFFLINE'
                ELSE 'PARTITION_FAULT'
            END AS alarm_type,
            CASE
                WHEN ROW_NUMBER() OVER (ORDER BY b.id) <= 220 THEN 0
                WHEN MOD(ROW_NUMBER() OVER (ORDER BY b.id), 5) = 0 THEN 1
                WHEN MOD(ROW_NUMBER() OVER (ORDER BY b.id), 5) = 1 THEN 2
                WHEN MOD(ROW_NUMBER() OVER (ORDER BY b.id), 5) = 2 THEN 3
                WHEN MOD(ROW_NUMBER() OVER (ORDER BY b.id), 5) = 3 THEN 4
                ELSE 5
            END AS status
        FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D b
        WHERE b.id BETWEEN 9900001 AND 9900580
          AND (b.deleted IS NULL OR b.deleted = 0)
    ) t
    CROSS JOIN (
        SELECT 1 AS seq_no FROM dual
        UNION ALL
        SELECT 2 AS seq_no FROM dual
    ) e
    WHERE t.rn <= 800
) x
WHERE x.rn <= 1600;

-- =========================
-- 7) 设备在线日志（1200）
-- =========================
INSERT INTO ODS_DWEQ_DM_DEVICE_ONLINE_LOG_D (
    id, device_id, status, change_time, reason, deleted, created_on
)
SELECT
    9940000 + LEVEL AS id,
    9500000 + MOD(LEVEL - 1, 20) + 1 AS device_id,
    CASE WHEN MOD(LEVEL, 6) = 0 THEN 0 ELSE 1 END AS status,
    CURRENT_TIMESTAMP - (LEVEL / 1600) AS change_time,
    CASE WHEN MOD(LEVEL, 6) = 0 THEN 'offline inspection' ELSE 'report heartbeat' END AS reason,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on
FROM dual
CONNECT BY LEVEL <= 1200;

COMMIT;

-- =========================
-- 8) 数据量检查
-- =========================
SELECT 'AREA' AS t, COUNT(*) AS c FROM ODS_DWEQ_DM_AREA_D WHERE id BETWEEN 9100001 AND 9400580
UNION ALL SELECT 'ORG', COUNT(*) FROM ODS_DWEQ_DM_ORG_D WHERE id BETWEEN 9100001 AND 9400580
UNION ALL SELECT 'DEVICE', COUNT(*) FROM ODS_DWEQ_DM_DEVICE_D WHERE id BETWEEN 9500001 AND 9500020
UNION ALL SELECT 'MONITOR', COUNT(*) FROM ODS_DWEQ_DM_MONITOR_D WHERE id BETWEEN 9600001 AND 9600020
UNION ALL SELECT 'SHAFT_FLOOR', COUNT(*) FROM ODS_DWEQ_DM_SHAFT_FLOOR_D WHERE id BETWEEN 9800001 AND 9800580
UNION ALL SELECT 'MONITOR_DEVICE_BIND', COUNT(*) FROM ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D WHERE id BETWEEN 9700001 AND 9700020
UNION ALL SELECT 'MONITOR_PARTITION_BIND', COUNT(*) FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D WHERE id BETWEEN 9900001 AND 9900580
UNION ALL SELECT 'ALARM_RULE', COUNT(*) FROM ODS_DWEQ_DM_ALARM_RULE_D WHERE id BETWEEN 9950001 AND 9950005
UNION ALL SELECT 'RAW_DATA', COUNT(*) FROM ODS_DWEQ_DM_RAW_DATA_D WHERE id BETWEEN 9960001 AND 9961800
UNION ALL SELECT 'ALARM_RAW', COUNT(*) FROM ODS_DWEQ_DM_ALARM_RAW_D WHERE id BETWEEN 9970001 AND 9971200
UNION ALL SELECT 'ALARM', COUNT(*) FROM ODS_DWEQ_DM_ALARM_D WHERE id BETWEEN '9980001' AND '9980800'
UNION ALL SELECT 'EVENT', COUNT(*) FROM ODS_DWEQ_DM_EVENT_D WHERE id BETWEEN 9990001 AND 9991600
UNION ALL SELECT 'DEVICE_ONLINE_LOG', COUNT(*) FROM ODS_DWEQ_DM_DEVICE_ONLINE_LOG_D WHERE id BETWEEN 9940001 AND 9941200;

-- 告警类型覆盖检查（应包含监测对象告警 + 终端告警）
SELECT alarm_type, alarm_domain, COUNT(*) AS cnt
FROM ODS_DWEQ_DM_ALARM_D
WHERE id BETWEEN '9980001' AND '9980800'
GROUP BY alarm_type, alarm_domain
ORDER BY alarm_domain, alarm_type;
