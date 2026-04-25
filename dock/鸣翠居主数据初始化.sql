-- 鸣翠居主数据初始化（达梦）
-- 结构：深圳供电局-福田区-香蜜湖街道-香蜜社区-鸣翠居
-- 展开：1~10栋 -> 每栋 1/2单元（按 SHAFT 处理）
-- 关联：每个单元(竖井)1台设备、1个monitor、29条楼层、29条分区绑定

-- 可重复执行：先清理本脚本使用的 ID 段
DELETE FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D WHERE id BETWEEN 9900001 AND 9900580;
DELETE FROM ODS_DWEQ_DM_SHAFT_FLOOR_D WHERE id BETWEEN 9800001 AND 9800580;
DELETE FROM ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D WHERE id BETWEEN 9700001 AND 9700020;
DELETE FROM ODS_DWEQ_DM_MONITOR_D WHERE id BETWEEN 9600001 AND 9600020;
DELETE FROM ODS_DWEQ_DM_DEVICE_D WHERE id BETWEEN 9500001 AND 9500020;

DELETE FROM ODS_DWEQ_DM_AREA_D WHERE id BETWEEN 9300001 AND 9300020;
DELETE FROM ODS_DWEQ_DM_AREA_D WHERE id BETWEEN 9200001 AND 9200010;
DELETE FROM ODS_DWEQ_DM_AREA_D WHERE id BETWEEN 9100001 AND 9100005;

-- 1) area 基础层级（5级）
INSERT INTO ODS_DWEQ_DM_AREA_D (
    id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on
) VALUES
    (9100001, 0,       '深圳供电局',   'ORG',       '9100001',                                              '深圳供电局',                                   0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9100002, 9100001, '福田区',       'DISTRICT',  '9100001/9100002',                                      '深圳供电局/福田区',                            0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9100003, 9100002, '香蜜湖街道',   'STREET',    '9100001/9100002/9100003',                              '深圳供电局/福田区/香蜜湖街道',                 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9100004, 9100003, '香蜜社区',     'COMMUNITY', '9100001/9100002/9100003/9100004',                      '深圳供电局/福田区/香蜜湖街道/香蜜社区',        0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (9100005, 9100004, '鸣翠居',       'ESTATE',    '9100001/9100002/9100003/9100004/9100005',              '深圳供电局/福田区/香蜜湖街道/香蜜社区/鸣翠居', 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2) area 楼栋（10条）
INSERT INTO ODS_DWEQ_DM_AREA_D (
    id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on
)
SELECT
    9200000 + b.b AS id,
    9100005 AS parent_id,
    TO_CHAR(b.b) || '栋' AS name,
    'BUILDING' AS type,
    '9100001/9100002/9100003/9100004/9100005/' || TO_CHAR(9200000 + b.b) AS path_ids,
    '深圳供电局/福田区/香蜜湖街道/香蜜社区/鸣翠居/' || TO_CHAR(b.b) || '栋' AS path_names,
    0 AS deleted,
    b.b AS sort,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (SELECT LEVEL AS b FROM dual CONNECT BY LEVEL <= 10) b;

-- 3) area 单元（按 SHAFT 处理，20条）
INSERT INTO ODS_DWEQ_DM_AREA_D (
    id, parent_id, name, type, path_ids, path_names, deleted, sort, created_on, updated_on
)
SELECT
    9300000 + ((b.b - 1) * 2 + u.u) AS id,
    9200000 + b.b AS parent_id,
    TO_CHAR(u.u) || '单元' AS name,
    'SHAFT' AS type,
    '9100001/9100002/9100003/9100004/9100005/' || TO_CHAR(9200000 + b.b) || '/' || TO_CHAR(9300000 + ((b.b - 1) * 2 + u.u)) AS path_ids,
    '深圳供电局/福田区/香蜜湖街道/香蜜社区/鸣翠居/' || TO_CHAR(b.b) || '栋/' || TO_CHAR(u.u) || '单元' AS path_names,
    0 AS deleted,
    u.u AS sort,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (SELECT LEVEL AS b FROM dual CONNECT BY LEVEL <= 10) b
CROSS JOIN (SELECT LEVEL AS u FROM dual CONNECT BY LEVEL <= 2) u;

-- 4) device（20台，每个单元1台）
INSERT INTO ODS_DWEQ_DM_DEVICE_D (
    id, device_type, name, area_id, iot_code, model, manufacturer, asset_status, org_id,
    online_status, deleted, created_on, updated_on, remark
)
SELECT
    9500000 + s.s AS id,
    'SHAFT_TEMP' AS device_type,
    '鸣翠居' || TO_CHAR(TRUNC((s.s - 1) / 2) + 1) || '栋' || TO_CHAR(MOD(s.s - 1, 2) + 1) || '单元测温终端' AS name,
    9300000 + s.s AS area_id,
    'mcj-dev-b' || LPAD(TO_CHAR(TRUNC((s.s - 1) / 2) + 1), 2, '0') || '-u' || TO_CHAR(MOD(s.s - 1, 2) + 1) AS iot_code,
    'TMP-V1' AS model,
    'LOCAL' AS manufacturer,
    '已接入' AS asset_status,
    NULL AS org_id,
    0 AS online_status,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on,
    '鸣翠居批量初始化' AS remark
FROM (SELECT LEVEL AS s FROM dual CONNECT BY LEVEL <= 20) s;

-- 5) monitor（20个竖井，每个单元1个）
INSERT INTO ODS_DWEQ_DM_MONITOR_D (
    id, name, area_id, area_name, elevator_count, shaft_type, monitor_status, owner_company, device_id,
    remark, deleted, created_on, updated_on
)
SELECT
    9600000 + s.s AS id,
    '鸣翠居' || TO_CHAR(TRUNC((s.s - 1) / 2) + 1) || '栋' || TO_CHAR(MOD(s.s - 1, 2) + 1) || '单元竖井' AS name,
    9300000 + s.s AS area_id,
    '深圳供电局/福田区/香蜜湖街道/香蜜社区/鸣翠居/' || TO_CHAR(TRUNC((s.s - 1) / 2) + 1) || '栋/' || TO_CHAR(MOD(s.s - 1, 2) + 1) || '单元' AS area_name,
    1 AS elevator_count,
    'POWER' AS shaft_type,
    'RUNNING' AS monitor_status,
    '深圳供电局' AS owner_company,
    9500000 + s.s AS device_id,
    '鸣翠居批量初始化' AS remark,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (SELECT LEVEL AS s FROM dual CONNECT BY LEVEL <= 20) s;

-- 6) monitor-device 绑定（20条）
INSERT INTO ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D (
    id, monitor_id, device_id, bind_status, bind_time, deleted, created_on, updated_on
)
SELECT
    9700000 + s.s AS id,
    9600000 + s.s AS monitor_id,
    9500000 + s.s AS device_id,
    1 AS bind_status,
    CURRENT_TIMESTAMP AS bind_time,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (SELECT LEVEL AS s FROM dual CONNECT BY LEVEL <= 20) s;

-- 7) shaft_floor（580条）
INSERT INTO ODS_DWEQ_DM_SHAFT_FLOOR_D (
    id, monitor_id, area_id, name, device_id, start_point, end_point, sort, deleted, created_on, updated_on
)
SELECT
    9800000 + ((s.s - 1) * 29 + f.f) AS id,
    9600000 + s.s AS monitor_id,
    9300000 + s.s AS area_id,
    TO_CHAR(f.f) || '层' AS name,
    9500000 + s.s AS device_id,
    0 AS start_point,
    0 AS end_point,
    f.f AS sort,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (SELECT LEVEL AS s FROM dual CONNECT BY LEVEL <= 20) s
CROSS JOIN (SELECT LEVEL AS f FROM dual CONNECT BY LEVEL <= 29) f;

-- 8) monitor_partition_bind（580条，按 1层=1分区）
INSERT INTO ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D (
    id, monitor_id, device_id, shaft_floor_id, partition_id, partition_code, partition_name, data_reference,
    device_token, partition_no, bind_status, deleted, created_on, updated_on
)
SELECT
    9900000 + ((s.s - 1) * 29 + f.f) AS id,
    9600000 + s.s AS monitor_id,
    9500000 + s.s AS device_id,
    9800000 + ((s.s - 1) * 29 + f.f) AS shaft_floor_id,
    f.f AS partition_id,
    ('mcj-dev-b' || LPAD(TO_CHAR(TRUNC((s.s - 1) / 2) + 1), 2, '0') || '-u' || TO_CHAR(MOD(s.s - 1, 2) + 1) || '_TMP_th' || LPAD(TO_CHAR(f.f), 2, '0')) AS partition_code,
    TO_CHAR(f.f) || '层分区' AS partition_name,
    ('/TMP/' || 'mcj-dev-b' || LPAD(TO_CHAR(TRUNC((s.s - 1) / 2) + 1), 2, '0') || '-u' || TO_CHAR(MOD(s.s - 1, 2) + 1) || '_TMP_th' || LPAD(TO_CHAR(f.f), 2, '0')) AS data_reference,
    ('mcj-dev-b' || LPAD(TO_CHAR(TRUNC((s.s - 1) / 2) + 1), 2, '0') || '-u' || TO_CHAR(MOD(s.s - 1, 2) + 1)) AS device_token,
    f.f AS partition_no,
    1 AS bind_status,
    0 AS deleted,
    CURRENT_TIMESTAMP AS created_on,
    CURRENT_TIMESTAMP AS updated_on
FROM (SELECT LEVEL AS s FROM dual CONNECT BY LEVEL <= 20) s
CROSS JOIN (SELECT LEVEL AS f FROM dual CONNECT BY LEVEL <= 29) f;

-- 9) 校验统计
SELECT 'AREA_TOTAL' AS tag, COUNT(*) AS cnt FROM ODS_DWEQ_DM_AREA_D WHERE id BETWEEN 9100001 AND 9300020
UNION ALL
SELECT 'DEVICE_TOTAL' AS tag, COUNT(*) AS cnt FROM ODS_DWEQ_DM_DEVICE_D WHERE id BETWEEN 9500001 AND 9500020
UNION ALL
SELECT 'MONITOR_TOTAL' AS tag, COUNT(*) AS cnt FROM ODS_DWEQ_DM_MONITOR_D WHERE id BETWEEN 9600001 AND 9600020
UNION ALL
SELECT 'MONITOR_DEVICE_BIND_TOTAL' AS tag, COUNT(*) AS cnt FROM ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D WHERE id BETWEEN 9700001 AND 9700020
UNION ALL
SELECT 'SHAFT_FLOOR_TOTAL' AS tag, COUNT(*) AS cnt FROM ODS_DWEQ_DM_SHAFT_FLOOR_D WHERE id BETWEEN 9800001 AND 9800580
UNION ALL
SELECT 'MONITOR_PARTITION_BIND_TOTAL' AS tag, COUNT(*) AS cnt FROM ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D WHERE id BETWEEN 9900001 AND 9900580;
