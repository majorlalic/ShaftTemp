-- 福田区台账抽取：清理接口联调数据（保留基础主数据）
-- 适用表名规则：ECMS_D_ST_*
-- 保留：AREA/ORG/SHAFT_DEVICE/MONITOR/SHAFT_FLOOR/MONITOR_DEVICE_BIND/MONITOR_PARTITION_BIND
-- 清理：ALARM_RULE(联调段)、RAW_DATA、ALARM_RAW、ALARM、EVENT、DEVICE_ONLINE_LOG

-- =========================
-- 0) 先删事件，再删告警（避免外键/逻辑依赖）
-- =========================
DELETE FROM ECMS_D_ST_EVENT
WHERE id BETWEEN 9990001 AND 9999999
   OR alarm_id IN (
        SELECT TO_NUMBER(a.id)
        FROM ECMS_D_ST_ALARM a
        WHERE (a.device_token LIKE 'ft-shaft-dev-%')
           OR (a.id BETWEEN '9980001' AND '9989999')
   );

DELETE FROM ECMS_D_ST_ALARM
WHERE id BETWEEN '9980001' AND '9989999'
   OR device_token LIKE 'ft-shaft-dev-%';

-- =========================
-- 1) 清理上游告警原文与测温原始
-- =========================
DELETE FROM ECMS_D_ST_ALARM_RAW
WHERE id BETWEEN 9970001 AND 9979999
   OR iot_code LIKE 'ft-shaft-dev-%';

DELETE FROM ECMS_D_ST_RAW_DATA
WHERE id BETWEEN 9960001 AND 9969999
   OR iot_code LIKE 'ft-shaft-dev-%';

-- =========================
-- 2) 清理设备在线日志（仅联调设备）
-- =========================
DELETE FROM ECMS_D_ST_DEVICE_ONLINE_LOG
WHERE id BETWEEN 9940001 AND 9949999
   OR device_id IN (
        SELECT d.id
        FROM ECMS_D_ST_SHAFT_DEVICE d
        WHERE d.iot_code LIKE 'ft-shaft-dev-%'
   );

-- =========================
-- 3) 清理联调规则（仅清联调ID段）
-- =========================
DELETE FROM ECMS_D_ST_ALARM_RULE
WHERE id BETWEEN 9950001 AND 9950005;

-- =========================
-- 4) 可选：把联调设备状态复位为“待接入+离线”
-- =========================
UPDATE ECMS_D_ST_SHAFT_DEVICE
SET asset_status = '待接入',
    online_status = 0,
    last_report_time = NULL,
    last_offline_time = NULL,
    updated_on = CURRENT_TIMESTAMP
WHERE iot_code LIKE 'ft-shaft-dev-%';

COMMIT;

-- =========================
-- 5) 清理结果检查
-- =========================
SELECT 'ALARM_RULE' AS t, COUNT(*) AS c
FROM ECMS_D_ST_ALARM_RULE
WHERE id BETWEEN 9950001 AND 9950005
UNION ALL
SELECT 'RAW_DATA', COUNT(*) FROM ECMS_D_ST_RAW_DATA WHERE iot_code LIKE 'ft-shaft-dev-%'
UNION ALL
SELECT 'ALARM_RAW', COUNT(*) FROM ECMS_D_ST_ALARM_RAW WHERE iot_code LIKE 'ft-shaft-dev-%'
UNION ALL
SELECT 'ALARM', COUNT(*) FROM ECMS_D_ST_ALARM WHERE device_token LIKE 'ft-shaft-dev-%'
UNION ALL
SELECT 'EVENT', COUNT(*) FROM ECMS_D_ST_EVENT e
WHERE e.alarm_id IN (
    SELECT TO_NUMBER(a.id) FROM ECMS_D_ST_ALARM a WHERE a.device_token LIKE 'ft-shaft-dev-%'
)
UNION ALL
SELECT 'DEVICE_ONLINE_LOG', COUNT(*) FROM ECMS_D_ST_DEVICE_ONLINE_LOG l
WHERE l.device_id IN (
    SELECT d.id FROM ECMS_D_ST_SHAFT_DEVICE d WHERE d.iot_code LIKE 'ft-shaft-dev-%'
);

