-- 启动前主数据自检 SQL（达梦）
-- 用途：排查 “partition binding not found ...” 等联调常见问题
-- 建议：每次导数后、服务启动前执行一遍

-- 1) 基础表数据量检查
SELECT 'DEVICE' AS t, COUNT(*) AS c FROM ODS_DWEQ_DM_DEVICE_D WHERE deleted = 0
UNION ALL
SELECT 'MONITOR', COUNT(*) FROM ODS_DWEQ_DM_MONITOR_D WHERE deleted = 0
UNION ALL
SELECT 'SHAFT_FLOOR', COUNT(*) FROM ODS_DWEQ_DM_SHAFT_FLOOR_D WHERE deleted = 0
UNION ALL
SELECT 'MONITOR_DEVICE_BIND', COUNT(*) FROM ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D WHERE deleted = 0
UNION ALL
SELECT 'MONITOR_PARTITION_BIND', COUNT(*) FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D WHERE deleted = 0
UNION ALL
SELECT 'ALARM_RULE', COUNT(*) FROM ODS_DWEQ_DM_ALARM_RULE_D WHERE deleted = 0;

-- 2) 设备是否存在（替换为实际 iotCode）
-- 预期：返回 1 条
SELECT id, iot_code, name, deleted
FROM ODS_DWEQ_DM_DEVICE_D
WHERE iot_code = 'shaft-dev-001';

-- 3) 指定设备 + 分区ID 的绑定是否存在（替换 iotCode/partitionId）
-- 预期：返回 1 条
SELECT
    d.id AS device_id,
    d.iot_code,
    b.partition_id,
    b.partition_code,
    b.data_reference,
    b.shaft_floor_id
FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D b
JOIN ODS_DWEQ_DM_DEVICE_D d ON d.id = b.device_id
WHERE d.iot_code = 'shaft-dev-001'
  AND b.partition_id = 10
  AND b.deleted = 0
  AND d.deleted = 0;

-- 4) 指定 partitionCode 的绑定是否存在（替换为实际 partitionCode）
-- 预期：返回 1 条
SELECT
    b.id,
    b.device_id,
    d.iot_code,
    b.partition_id,
    b.partition_code,
    b.data_reference
FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D b
LEFT JOIN ODS_DWEQ_DM_DEVICE_D d ON d.id = b.device_id
WHERE b.partition_code = 'shaft-dev-001_TMP_th10'
  AND b.deleted = 0;

-- 5) 命名规范检查：iot_code 是否符合 shaft-dev-三位数
-- 预期：返回 0 条
SELECT id, iot_code
FROM ODS_DWEQ_DM_DEVICE_D
WHERE deleted = 0
  AND REGEXP_LIKE(iot_code, '^shaft-dev-[0-9]{3}$') = 0;

-- 6) 命名规范检查：partition_code 是否符合 shaft-dev-三位数_TMP_th两位数
-- 预期：返回 0 条
SELECT id, partition_code
FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D
WHERE deleted = 0
  AND REGEXP_LIKE(partition_code, '^shaft-dev-[0-9]{3}_TMP_th[0-9]{2}$') = 0;

-- 7) 规则配置检查：核心规则是否存在且启用
-- 预期：每种 alarm_type 至少 1 条 enabled=1
SELECT alarm_type, enabled, COUNT(*) AS c
FROM ODS_DWEQ_DM_ALARM_RULE_D
WHERE deleted = 0
  AND alarm_type IN ('TEMP_THRESHOLD', 'TEMP_DIFFERENCE', 'TEMP_RISE_RATE', 'DEVICE_OFFLINE', 'PARTITION_FAULT')
GROUP BY alarm_type, enabled
ORDER BY alarm_type, enabled;

-- 8) 绑定完整性：绑定了 device 但 monitor/device_bind 缺失
-- 预期：返回 0 条
SELECT b.id, b.device_id, b.monitor_id, b.partition_id, b.partition_code
FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D b
LEFT JOIN ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D md
       ON md.monitor_id = b.monitor_id
      AND md.device_id = b.device_id
      AND md.deleted = 0
WHERE b.deleted = 0
  AND md.id IS NULL;

-- 9) 绑定完整性：shaft_floor 缺失
-- 预期：返回 0 条
SELECT b.id, b.shaft_floor_id, b.partition_code
FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D b
LEFT JOIN ODS_DWEQ_DM_SHAFT_FLOOR_D f
       ON f.id = b.shaft_floor_id
      AND f.deleted = 0
WHERE b.deleted = 0
  AND f.id IS NULL;

-- 10) 快速定位“partition binding not found”问题（替换实际 partitionCode）
-- 示例报错：partitionCode=shaft-dev-01_TMP_th10
-- 可一次性看到“接近但不一致”的候选数据
SELECT
    b.id,
    d.iot_code,
    b.partition_id,
    b.partition_code,
    b.data_reference
FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D b
JOIN ODS_DWEQ_DM_DEVICE_D d ON d.id = b.device_id
WHERE b.deleted = 0
  AND (
       b.partition_code LIKE '%_TMP_th10'
    OR b.partition_code LIKE 'shaft-dev-01%'
    OR b.partition_code LIKE 'shaft-dev-001%'
  )
ORDER BY b.partition_code;

-- 11) 可选：联调前检查目标接口关键样例（按你实际联调目标替换）
-- 目标：确认以下组合在绑定表存在
-- iotCode=shaft-dev-001, PartitionId=10, dataReference=/TMP/shaft-dev-001_TMP_th10
SELECT
    CASE WHEN COUNT(*) > 0 THEN 'OK' ELSE 'MISSING' END AS check_result
FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D b
JOIN ODS_DWEQ_DM_DEVICE_D d ON d.id = b.device_id
WHERE d.iot_code = 'shaft-dev-001'
  AND b.partition_id = 10
  AND b.data_reference = '/TMP/shaft-dev-001_TMP_th10'
  AND b.deleted = 0
  AND d.deleted = 0;
