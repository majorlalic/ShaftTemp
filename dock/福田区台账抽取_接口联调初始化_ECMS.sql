-- 福田区台账抽取接口联调初始化（达梦）
-- 前置：请先执行 dock/福田区台账抽取_主数据初始化_ECMS.sql
-- 表名规则：ECMS_D_ST_*
-- 目标：生成规则+原始测温+告警原文+主告警+事件+在线日志联调数据

-- =========================
-- 0) 清理本脚本ID段
-- =========================
DELETE FROM ECMS_D_ST_DEVICE_ONLINE_LOG WHERE id BETWEEN 9940001 AND 9942000;
DELETE FROM ECMS_D_ST_RAW_DATA WHERE id BETWEEN 9960001 AND 9963000;
DELETE FROM ECMS_D_ST_ALARM_RAW WHERE id BETWEEN 9970001 AND 9972000;
DELETE FROM ECMS_D_ST_EVENT WHERE id BETWEEN 9990001 AND 9993000;
DELETE FROM ECMS_D_ST_ALARM WHERE id BETWEEN '9980001' AND '9981500';
DELETE FROM ECMS_D_ST_ALARM_RULE WHERE id BETWEEN 9950001 AND 9950005;

COMMIT;

-- =========================
-- 1) 规则（5）
-- =========================
INSERT INTO ECMS_D_ST_ALARM_RULE (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    9950001, '温度阈值', 'MONITOR', 'TEMP_THRESHOLD', 'GLOBAL', NULL,
    2, 70.00, NULL, NULL, 1, '福田区联调默认规则', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ECMS_D_ST_ALARM_RULE (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    9950002, '差温阈值', 'MONITOR', 'TEMP_DIFFERENCE', 'GLOBAL', NULL,
    2, 20.00, NULL, NULL, 1, '福田区联调默认规则', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ECMS_D_ST_ALARM_RULE (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    9950003, '升温速率', 'MONITOR', 'TEMP_RISE_RATE', 'GLOBAL', NULL,
    2, 10.00, NULL, 60, 1, '福田区联调默认规则', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ECMS_D_ST_ALARM_RULE (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    9950004, '设备离线', 'DEVICE', 'DEVICE_OFFLINE', 'GLOBAL', NULL,
    1, 30.00, NULL, NULL, 1, '福田区联调默认规则', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

INSERT INTO ECMS_D_ST_ALARM_RULE (
    id, rule_name, biz_type, alarm_type, scope_type, scope_id,
    level, threshold_value, threshold_value2, duration_seconds,
    enabled, remark, deleted, created_on, updated_on
) VALUES (
    9950005, '分区断纤', 'DEVICE', 'PARTITION_FAULT', 'GLOBAL', NULL,
    1, 0.00, NULL, NULL, 1, '福田区联调默认规则', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- =========================
-- 2) 原始测温 raw_data（最多 1500）
-- =========================
INSERT INTO ECMS_D_ST_RAW_DATA (
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
    60 + MOD(t.partition_id, 18) + MOD(t.rn, 5) * 0.1 AS max_temp,
    38 + MOD(t.partition_id, 12) + MOD(t.rn, 3) * 0.1 AS min_temp,
    49 + MOD(t.partition_id, 15) + MOD(t.rn, 4) * 0.1 AS avg_temp,
    80 + MOD(t.partition_id, 20) AS max_temp_position,
    20 + MOD(t.partition_id, 15) AS min_temp_position,
    MOD(t.partition_id, 4) + 1 AS max_temp_channel,
    MOD(t.partition_id + 1, 4) + 1 AS min_temp_channel,
    '{"scene":"ft-ledger-init","partitionId":' || TO_CHAR(t.partition_id) || '}' AS payload_json,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY b.id) rn,
        b.monitor_id, b.device_id, b.shaft_floor_id, b.partition_id, b.data_reference, b.device_token
    FROM ECMS_D_ST_MONITOR_PARTITION_BIND b
    WHERE (b.deleted IS NULL OR b.deleted = 0)
      AND b.bind_status = 1
      AND b.device_token LIKE 'ft-shaft-dev-%'
) t
WHERE t.rn <= 1500;

-- =========================
-- 3) 告警原始数据 alarm_raw（最多 1000）
-- =========================
INSERT INTO ECMS_D_ST_ALARM_RAW (
    id, iot_code, topic, partition_id, alarm_status, fault_status,
    ied_full_path, data_reference, collect_time, payload_json, deleted, created_on
)
SELECT
    9970000 + t.rn AS id,
    t.device_token AS iot_code,
    t.data_reference || '/Alarm' AS topic,
    t.partition_id,
    CASE WHEN MOD(t.rn, 4) = 0 THEN 1 ELSE 0 END AS alarm_status,
    CASE WHEN MOD(t.rn, 11) = 0 THEN 1 ELSE 0 END AS fault_status,
    '/IED/' || t.device_token AS ied_full_path,
    t.data_reference,
    CURRENT_TIMESTAMP - (t.rn / 1800) AS collect_time,
    '{"scene":"ft-ledger-alarm-raw","partitionId":' || TO_CHAR(t.partition_id) || '}' AS payload_json,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY b.id) rn,
        b.partition_id, b.data_reference, b.device_token
    FROM ECMS_D_ST_MONITOR_PARTITION_BIND b
    WHERE (b.deleted IS NULL OR b.deleted = 0)
      AND b.bind_status = 1
      AND b.device_token LIKE 'ft-shaft-dev-%'
) t
WHERE t.rn <= 1000;

-- =========================
-- 4) 主告警 alarm（最多 900）
-- =========================
INSERT INTO ECMS_D_ST_ALARM (
    id, alarm_code, alarm_type, source_type, monitor_id, device_id, shaft_floor_id,
    partition_code, partition_name, data_reference, device_token, partition_no, source_format,
    merge_key, status, first_alarm_time, last_alarm_time, merge_count, event_count,
    alarm_level, title, content, handler, handle_time, handle_remark, push_status,
    alarm_type_big, alarm_domain, area_name, monitor_name, device_name, handler_name, manufacturer, device_model, push_time,
    deleted, created_on, updated_on
)
SELECT
    TO_CHAR(9980000 + t.rn) AS id,
    'FT-ALM-' || TO_CHAR(9980000 + t.rn) AS alarm_code,
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
    CASE WHEN t.rn <= 260 THEN 'M:' || TO_CHAR(t.monitor_id) || ':' || t.alarm_type ELSE NULL END AS merge_key,
    CASE
        WHEN t.rn <= 260 THEN 0
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
    '福田区联调告警-' || TO_CHAR(t.rn) AS title,
    t.partition_name || ' 触发 ' || t.alarm_type || '，样例#' || TO_CHAR(t.rn) AS content,
    CASE WHEN t.rn <= 260 THEN NULL ELSE 'handler-001' END AS handler,
    CASE WHEN t.rn <= 260 THEN NULL ELSE CURRENT_TIMESTAMP - (t.rn / 2200) END AS handle_time,
    CASE WHEN t.rn <= 260 THEN NULL ELSE '联调处警' END AS handle_remark,
    CASE WHEN MOD(t.rn, 3) = 0 THEN 1 ELSE 0 END AS push_status,
    0 AS alarm_type_big,
    CASE WHEN t.alarm_type IN ('DEVICE_OFFLINE', 'PARTITION_FAULT') THEN 0 ELSE 1 END AS alarm_domain,
    t.area_name,
    t.monitor_name,
    t.device_name,
    CASE WHEN t.rn <= 260 THEN NULL ELSE '运维值班员A' END AS handler_name,
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
    FROM ECMS_D_ST_MONITOR_PARTITION_BIND b
    JOIN ECMS_D_ST_MONITOR m ON m.id = b.monitor_id AND (m.deleted IS NULL OR m.deleted = 0)
    JOIN ECMS_D_ST_SHAFT_DEVICE d ON d.id = b.device_id AND (d.deleted IS NULL OR d.deleted = 0)
    WHERE (b.deleted IS NULL OR b.deleted = 0)
      AND b.bind_status = 1
      AND b.device_token LIKE 'ft-shaft-dev-%'
) t
WHERE t.rn <= 900;

-- =========================
-- 5) 事件 event（最多 1800）
-- 每条告警 2 条事件
-- =========================
INSERT INTO ECMS_D_ST_EVENT (
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
    '{"scene":"ft-ledger-event","eventNo":' || TO_CHAR(x.event_no) || '}' AS detail_json,
    x.content || ' 事件' || TO_CHAR(x.event_no) AS content,
    CASE WHEN x.event_type = 1 THEN 1 ELSE 0 END AS merged_flag,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (
    SELECT
        ROW_NUMBER() OVER (ORDER BY a.id, e.seq_no) rn,
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
        CASE
            WHEN e.seq_no = 1 THEN 0
            WHEN a.status = 0 THEN 1
            WHEN a.status = 1 THEN 2
            WHEN a.status = 2 THEN 3
            WHEN a.status = 3 THEN 4
            ELSE 6
        END AS event_type,
        CURRENT_TIMESTAMP - (ROW_NUMBER() OVER (ORDER BY a.id) / 1800) - (e.seq_no / 86400) AS event_time,
        e.seq_no AS event_no,
        a.alarm_level AS event_level,
        a.content
    FROM ECMS_D_ST_ALARM a
    CROSS JOIN (
        SELECT 1 AS seq_no FROM dual
        UNION ALL
        SELECT 2 AS seq_no FROM dual
    ) e
    WHERE a.id BETWEEN '9980001' AND '9981500'
      AND (a.deleted IS NULL OR a.deleted = 0)
) x
WHERE x.rn <= 1800;

-- =========================
-- 6) 设备在线日志（最多 1200）
-- =========================
INSERT INTO ECMS_D_ST_DEVICE_ONLINE_LOG (
    id, device_id, status, change_time, reason, deleted, created_on
)
SELECT
    9940000 + t.rn AS id,
    t.device_id,
    CASE WHEN MOD(t.rn, 6) = 0 THEN 0 ELSE 1 END AS status,
    CURRENT_TIMESTAMP - (t.rn / 1600) AS change_time,
    CASE WHEN MOD(t.rn, 6) = 0 THEN 'offline inspection' ELSE 'report heartbeat' END AS reason,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on
FROM (
    SELECT ROW_NUMBER() OVER (ORDER BY d.id, x.n) rn, d.id AS device_id
    FROM ECMS_D_ST_SHAFT_DEVICE d
    CROSS JOIN (SELECT LEVEL AS n FROM dual CONNECT BY LEVEL <= 80) x
    WHERE (d.deleted IS NULL OR d.deleted = 0)
      AND d.iot_code LIKE 'ft-shaft-dev-%'
) t
WHERE t.rn <= 1200;

COMMIT;

-- =========================
-- 7) 数据量检查
-- =========================
SELECT 'AREA' AS t, COUNT(*) AS c FROM ECMS_D_ST_AREA WHERE id BETWEEN 8800001 AND 8899999
UNION ALL SELECT 'ORG', COUNT(*) FROM ECMS_D_ST_ORG WHERE id BETWEEN 8800001 AND 8899999
UNION ALL SELECT 'DEVICE', COUNT(*) FROM ECMS_D_ST_SHAFT_DEVICE WHERE id BETWEEN 8850001 AND 8859999
UNION ALL SELECT 'MONITOR', COUNT(*) FROM ECMS_D_ST_MONITOR WHERE id BETWEEN 8860001 AND 8869999
UNION ALL SELECT 'SHAFT_FLOOR', COUNT(*) FROM ECMS_D_ST_SHAFT_FLOOR WHERE id BETWEEN 8880001 AND 8889999
UNION ALL SELECT 'MONITOR_DEVICE_BIND', COUNT(*) FROM ECMS_D_ST_MONITOR_DEVICE_BIND WHERE id BETWEEN 8870001 AND 8879999
UNION ALL SELECT 'MONITOR_PARTITION_BIND', COUNT(*) FROM ECMS_D_ST_MONITOR_PARTITION_BIND WHERE id BETWEEN 8890001 AND 8899999
UNION ALL SELECT 'ALARM_RULE', COUNT(*) FROM ECMS_D_ST_ALARM_RULE WHERE id BETWEEN 9950001 AND 9950005
UNION ALL SELECT 'RAW_DATA', COUNT(*) FROM ECMS_D_ST_RAW_DATA WHERE id BETWEEN 9960001 AND 9963000
UNION ALL SELECT 'ALARM_RAW', COUNT(*) FROM ECMS_D_ST_ALARM_RAW WHERE id BETWEEN 9970001 AND 9972000
UNION ALL SELECT 'ALARM', COUNT(*) FROM ECMS_D_ST_ALARM WHERE id BETWEEN '9980001' AND '9981500'
UNION ALL SELECT 'EVENT', COUNT(*) FROM ECMS_D_ST_EVENT WHERE id BETWEEN 9990001 AND 9993000
UNION ALL SELECT 'DEVICE_ONLINE_LOG', COUNT(*) FROM ECMS_D_ST_DEVICE_ONLINE_LOG WHERE id BETWEEN 9940001 AND 9942000;

