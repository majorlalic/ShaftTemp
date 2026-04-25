-- 福田区台账抽取主数据初始化（达梦）
-- 口径：
-- 1) area / org 结构一致，且不包含楼层节点
-- 2) 楼层数据仅落在 ECMS_D_ST_SHAFT_FLOOR
-- 3) 每栋按 1 个竖井（1 单元）生成 1 台设备 + 1 个监测对象

-- =========================
-- 0) 清理本脚本 ID 段
-- =========================
DELETE FROM ECMS_D_ST_MONITOR_PARTITION_BIND WHERE id BETWEEN 8890001 AND 8899999;
DELETE FROM ECMS_D_ST_SHAFT_FLOOR WHERE id BETWEEN 8880001 AND 8889999;
DELETE FROM ECMS_D_ST_MONITOR_DEVICE_BIND WHERE id BETWEEN 8870001 AND 8879999;
DELETE FROM ECMS_D_ST_MONITOR WHERE id BETWEEN 8860001 AND 8869999;
DELETE FROM ECMS_D_ST_SHAFT_DEVICE WHERE id BETWEEN 8850001 AND 8859999;
DELETE FROM ECMS_D_ST_ORG WHERE id BETWEEN 8800001 AND 8849999;
DELETE FROM ECMS_D_ST_AREA WHERE id BETWEEN 8800001 AND 8849999;

COMMIT;

-- =========================
-- 1) area：基础层级（4 条）
-- =========================
INSERT INTO ECMS_D_ST_AREA (
    id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on
) VALUES (
    8800001, 0, '深圳供电局', 'ORG', '8800001', '深圳供电局', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
INSERT INTO ECMS_D_ST_AREA (
    id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on
) VALUES (
    8800002, 8800001, '福田区', 'DISTRICT', '8800001/8800002', '深圳供电局/福田区', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
INSERT INTO ECMS_D_ST_AREA (
    id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on
) VALUES (
    8800003, 8800002, '香蜜湖街道', 'STREET', '8800001/8800002/8800003', '深圳供电局/福田区/香蜜湖街道', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
INSERT INTO ECMS_D_ST_AREA (
    id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on
) VALUES (
    8800004, 8800003, '香蜜社区', 'COMMUNITY', '8800001/8800002/8800003/8800004', '深圳供电局/福田区/香蜜湖街道/香蜜社区', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);

-- =========================
-- 2) area：小区（3 条）
-- =========================
INSERT INTO ECMS_D_ST_AREA (
    id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on
)
SELECT
    8810000 + e.e AS id,
    8800004 AS parent_id,
    CASE e.e WHEN 1 THEN '听水居' WHEN 2 THEN '润雨居' ELSE '鸣翠居' END AS name,
    'ESTATE' AS type,
    '8800001/8800002/8800003/8800004/' || TO_CHAR(8810000 + e.e) AS path_ids,
    '深圳供电局/福田区/香蜜湖街道/香蜜社区/' ||
    CASE e.e WHEN 1 THEN '听水居' WHEN 2 THEN '润雨居' ELSE '鸣翠居' END AS path_names,
    0 AS deleted,
    e.e AS sort,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (SELECT LEVEL AS e FROM dual CONNECT BY LEVEL <= 3) e;

-- =========================
-- 3) area：楼栋（12 条）
-- 每个小区 4 栋
-- =========================
INSERT INTO ECMS_D_ST_AREA (
    id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on
)
SELECT
    8820000 + ((e.e - 1) * 4 + b.b) AS id,
    8810000 + e.e AS parent_id,
    TO_CHAR(b.b) || '栋' AS name,
    'BUILDING' AS type,
    '8800001/8800002/8800003/8800004/' || TO_CHAR(8810000 + e.e) || '/' || TO_CHAR(8820000 + ((e.e - 1) * 4 + b.b)) AS path_ids,
    '深圳供电局/福田区/香蜜湖街道/香蜜社区/' ||
    CASE e.e WHEN 1 THEN '听水居' WHEN 2 THEN '润雨居' ELSE '鸣翠居' END || '/' || TO_CHAR(b.b) || '栋' AS path_names,
    0 AS deleted,
    b.b AS sort,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (SELECT LEVEL AS e FROM dual CONNECT BY LEVEL <= 3) e
CROSS JOIN (SELECT LEVEL AS b FROM dual CONNECT BY LEVEL <= 4) b;

-- =========================
-- 4) area：竖井（12 条）
-- 每栋 1 单元，按 SHAFT 节点
-- =========================
INSERT INTO ECMS_D_ST_AREA (
    id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on
)
SELECT
    8830000 + s.s AS id,
    8820000 + s.s AS parent_id,
    '1单元' AS name,
    'SHAFT' AS type,
    a.path_ids || '/' || TO_CHAR(8830000 + s.s) AS path_ids,
    a.path_names || '/1单元' AS path_names,
    0 AS deleted,
    1 AS sort,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (SELECT LEVEL AS s FROM dual CONNECT BY LEVEL <= 12) s
JOIN ECMS_D_ST_AREA a ON a.id = 8820000 + s.s
WHERE (a.deleted IS NULL OR a.deleted = 0);

-- =========================
-- 5) org：镜像 area（不含楼层）
-- =========================
INSERT INTO ECMS_D_ST_ORG (
    id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on
)
SELECT
    a.id, a.parent_id, a.name, a.type, a.path_ids, a.path_names,
    0 AS deleted, a.sort, CURRENT_TIMESTAMP AS created_on, CURRENT_TIMESTAMP AS updated_on
FROM ECMS_D_ST_AREA a
WHERE a.id BETWEEN 8800001 AND 8839999
  AND (a.deleted IS NULL OR a.deleted = 0);

-- =========================
-- 6) device：每个竖井 1 台（12 条）
-- =========================
INSERT INTO ECMS_D_ST_SHAFT_DEVICE (
    id, device_type, name, area_id, iot_code, model, manufacturer, asset_status, org_id,
    online_status, deleted, created_on, updated_on, remark
)
SELECT
    8850000 + s.s AS id,
    'SHAFT_TEMP' AS device_type,
    a.path_names || '测温终端' AS name,
    a.id AS area_id,
    'ft-shaft-dev-' || LPAD(TO_CHAR(s.s), 4, '0') AS iot_code,
    'TMP-V1' AS model,
    'LEDGER_IMPORT' AS manufacturer,
    '待接入' AS asset_status,
    a.id AS org_id,
    0 AS online_status,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on,
    '福田区台账抽取' AS remark
FROM (SELECT LEVEL AS s FROM dual CONNECT BY LEVEL <= 12) s
JOIN ECMS_D_ST_AREA a ON a.id = 8830000 + s.s
WHERE (a.deleted IS NULL OR a.deleted = 0);

-- =========================
-- 7) monitor：每个竖井 1 个（12 条）
-- =========================
INSERT INTO ECMS_D_ST_MONITOR (
    id, name, area_id, area_name, elevator_count, shaft_type, monitor_status, owner_company, device_id,
    remark, deleted, created_on, updated_on
)
SELECT
    8860000 + s.s AS id,
    a.path_names || '竖井' AS name,
    a.id AS area_id,
    a.path_names AS area_name,
    1 AS elevator_count,
    'POWER' AS shaft_type,
    'RUNNING' AS monitor_status,
    '深圳供电局' AS owner_company,
    8850000 + s.s AS device_id,
    '福田区台账抽取' AS remark,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (SELECT LEVEL AS s FROM dual CONNECT BY LEVEL <= 12) s
JOIN ECMS_D_ST_AREA a ON a.id = 8830000 + s.s
WHERE (a.deleted IS NULL OR a.deleted = 0);

-- =========================
-- 8) monitor-device 绑定（12 条）
-- =========================
INSERT INTO ECMS_D_ST_MONITOR_DEVICE_BIND (
    id, monitor_id, device_id, bind_status, bind_time, deleted, created_on, updated_on
)
SELECT
    8870000 + s.s AS id,
    8860000 + s.s AS monitor_id,
    8850000 + s.s AS device_id,
    1 AS bind_status,
    CURRENT_TIMESTAMP AS bind_time,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (SELECT LEVEL AS s FROM dual CONNECT BY LEVEL <= 12) s;

-- =========================
-- 9) 楼层（shaft_floor，240 条）
-- 楼层范围：10~29 层
-- =========================
INSERT INTO ECMS_D_ST_SHAFT_FLOOR (
    id, monitor_id, area_id, name, device_id, start_point, end_point, sort, deleted, created_on, updated_on
)
SELECT
    8880000 + ((s.s - 1) * 20 + f.f) AS id,
    8860000 + s.s AS monitor_id,
    8830000 + s.s AS area_id,
    TO_CHAR(9 + f.f) || '层' AS name,
    8850000 + s.s AS device_id,
    (f.f - 1) * 3 + 1 AS start_point,
    f.f * 3 AS end_point,
    9 + f.f AS sort,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (SELECT LEVEL AS s FROM dual CONNECT BY LEVEL <= 12) s
CROSS JOIN (SELECT LEVEL AS f FROM dual CONNECT BY LEVEL <= 20) f;

-- =========================
-- 10) 分区绑定（monitor_partition_bind，240 条）
-- =========================
INSERT INTO ECMS_D_ST_MONITOR_PARTITION_BIND (
    id, monitor_id, device_id, shaft_floor_id, partition_id, partition_code, partition_name, data_reference,
    device_token, partition_no, bind_status, deleted, created_on, updated_on
)
SELECT
    8890000 + ((s.s - 1) * 20 + f.f) AS id,
    8860000 + s.s AS monitor_id,
    8850000 + s.s AS device_id,
    8880000 + ((s.s - 1) * 20 + f.f) AS shaft_floor_id,
    9 + f.f AS partition_id,
    ('ft-shaft-dev-' || LPAD(TO_CHAR(s.s), 4, '0') || '_TMP_th' || LPAD(TO_CHAR(9 + f.f), 2, '0')) AS partition_code,
    (TO_CHAR(9 + f.f) || '层分区') AS partition_name,
    ('/TMP/ft-shaft-dev-' || LPAD(TO_CHAR(s.s), 4, '0') || '_TMP_th' || LPAD(TO_CHAR(9 + f.f), 2, '0')) AS data_reference,
    ('ft-shaft-dev-' || LPAD(TO_CHAR(s.s), 4, '0')) AS device_token,
    9 + f.f AS partition_no,
    1 AS bind_status,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (SELECT LEVEL AS s FROM dual CONNECT BY LEVEL <= 12) s
CROSS JOIN (SELECT LEVEL AS f FROM dual CONNECT BY LEVEL <= 20) f;

COMMIT;

-- =========================
-- 11) 校验
-- =========================
SELECT 'AREA' AS tag, COUNT(*) AS cnt FROM ECMS_D_ST_AREA WHERE id BETWEEN 8800001 AND 8839999
UNION ALL SELECT 'ORG', COUNT(*) FROM ECMS_D_ST_ORG WHERE id BETWEEN 8800001 AND 8839999
UNION ALL SELECT 'SHAFT_DEVICE', COUNT(*) FROM ECMS_D_ST_SHAFT_DEVICE WHERE id BETWEEN 8850001 AND 8859999
UNION ALL SELECT 'MONITOR', COUNT(*) FROM ECMS_D_ST_MONITOR WHERE id BETWEEN 8860001 AND 8869999
UNION ALL SELECT 'MONITOR_DEVICE_BIND', COUNT(*) FROM ECMS_D_ST_MONITOR_DEVICE_BIND WHERE id BETWEEN 8870001 AND 8879999
UNION ALL SELECT 'SHAFT_FLOOR', COUNT(*) FROM ECMS_D_ST_SHAFT_FLOOR WHERE id BETWEEN 8880001 AND 8889999
UNION ALL SELECT 'MONITOR_PARTITION_BIND', COUNT(*) FROM ECMS_D_ST_MONITOR_PARTITION_BIND WHERE id BETWEEN 8890001 AND 8899999;
