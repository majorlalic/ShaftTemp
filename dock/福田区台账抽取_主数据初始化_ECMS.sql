-- 福田区台账抽取主数据初始化（达梦）
-- 口径：
-- 1) area / org 结构一致，且不包含楼层节点
-- 2) 楼层数据仅落在 ECMS_D_ST_SHAFT_FLOOR
-- 3) 每栋 1 个竖井（1 单元）= 1 台设备 + 1 个监测对象
-- 4) asset_status 仅使用 0(未接入) / 1(已接入)

DELETE FROM ECMS_D_ST_MONITOR_PARTITION_BIND WHERE id BETWEEN 8890001 AND 8899999;
DELETE FROM ECMS_D_ST_SHAFT_FLOOR WHERE id BETWEEN 8880001 AND 8889999;
DELETE FROM ECMS_D_ST_MONITOR_DEVICE_BIND WHERE id BETWEEN 8870001 AND 8879999;
DELETE FROM ECMS_D_ST_MONITOR WHERE id BETWEEN 8860001 AND 8869999;
DELETE FROM ECMS_D_ST_SHAFT_DEVICE WHERE id BETWEEN 8850001 AND 8859999;
DELETE FROM ECMS_D_ST_ORG WHERE id BETWEEN 8800001 AND 8849999;
DELETE FROM ECMS_D_ST_AREA WHERE id BETWEEN 8800001 AND 8849999;

COMMIT;

-- area: 基础4级
INSERT INTO ECMS_D_ST_AREA (id,parent_id,name,type,path_ids,path_names,deleted,sort,created_on,updated_on)
VALUES (8800001,0,'深圳供电局','ORG','8800001','深圳供电局',0,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO ECMS_D_ST_AREA (id,parent_id,name,type,path_ids,path_names,deleted,sort,created_on,updated_on)
VALUES (8800002,8800001,'福田区','DISTRICT','8800001/8800002','深圳供电局/福田区',0,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO ECMS_D_ST_AREA (id,parent_id,name,type,path_ids,path_names,deleted,sort,created_on,updated_on)
VALUES (8800003,8800002,'香蜜湖街道','STREET','8800001/8800002/8800003','深圳供电局/福田区/香蜜湖街道',0,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO ECMS_D_ST_AREA (id,parent_id,name,type,path_ids,path_names,deleted,sort,created_on,updated_on)
VALUES (8800004,8800003,'香蜜社区','COMMUNITY','8800001/8800002/8800003/8800004','深圳供电局/福田区/香蜜湖街道/香蜜社区',0,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);

-- area: 小区3个
INSERT INTO ECMS_D_ST_AREA (id,parent_id,name,type,path_ids,path_names,deleted,sort,created_on,updated_on)
SELECT
  8810000 + e.e,
  8800004,
  CASE e.e WHEN 1 THEN '听水居' WHEN 2 THEN '润雨居' ELSE '鸣翠居' END,
  'ESTATE',
  '8800001/8800002/8800003/8800004/' || TO_CHAR(8810000 + e.e),
  '深圳供电局/福田区/香蜜湖街道/香蜜社区/' || CASE e.e WHEN 1 THEN '听水居' WHEN 2 THEN '润雨居' ELSE '鸣翠居' END,
  0,e.e,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM (SELECT LEVEL e FROM dual CONNECT BY LEVEL <= 3) e;

-- area: 每小区4栋，共12栋
INSERT INTO ECMS_D_ST_AREA (id,parent_id,name,type,path_ids,path_names,deleted,sort,created_on,updated_on)
SELECT
  8820000 + ((e.e - 1) * 4 + b.b),
  8810000 + e.e,
  TO_CHAR(b.b) || '栋',
  'BUILDING',
  '8800001/8800002/8800003/8800004/' || TO_CHAR(8810000 + e.e) || '/' || TO_CHAR(8820000 + ((e.e - 1) * 4 + b.b)),
  '深圳供电局/福田区/香蜜湖街道/香蜜社区/' || CASE e.e WHEN 1 THEN '听水居' WHEN 2 THEN '润雨居' ELSE '鸣翠居' END || '/' || TO_CHAR(b.b) || '栋',
  0,b.b,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM (SELECT LEVEL e FROM dual CONNECT BY LEVEL <= 3) e
CROSS JOIN (SELECT LEVEL b FROM dual CONNECT BY LEVEL <= 4) b;

-- area: 每栋1单元(shaft)，共12
INSERT INTO ECMS_D_ST_AREA (id,parent_id,name,type,path_ids,path_names,deleted,sort,created_on,updated_on)
SELECT
  8830000 + s.s,
  8820000 + s.s,
  '1单元',
  'SHAFT',
  a.path_ids || '/' || TO_CHAR(8830000 + s.s),
  a.path_names || '/1单元',
  0,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM (SELECT LEVEL s FROM dual CONNECT BY LEVEL <= 12) s
JOIN ECMS_D_ST_AREA a ON a.id = 8820000 + s.s
WHERE NVL(a.deleted,0)=0;

-- org 镜像 area（不含楼层）
INSERT INTO ECMS_D_ST_ORG (id,parent_id,name,type,path_ids,path_names,deleted,sort,created_on,updated_on)
SELECT id,parent_id,name,type,path_ids,path_names,0,sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM ECMS_D_ST_AREA
WHERE id BETWEEN 8800001 AND 8839999
  AND NVL(deleted,0)=0;

-- device: 每竖井1台，共12，asset_status 0/1
INSERT INTO ECMS_D_ST_SHAFT_DEVICE (
  id,device_type,name,area_id,iot_code,model,manufacturer,asset_status,org_id,online_status,deleted,created_on,updated_on,remark
)
SELECT
  8850000 + s.s,
  'SHAFT_TEMP',
  estate.name || bld.name || '1单元测温终端',
  shaft.id,
  'ft-shaft-dev-' || LPAD(TO_CHAR(s.s),4,'0'),
  'TMP-V1',
  'LEDGER_IMPORT',
  CASE WHEN MOD(s.s,2)=0 THEN 1 ELSE 0 END,
  shaft.id,
  0,
  0,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP,
  '福田区台账抽取'
FROM (SELECT LEVEL s FROM dual CONNECT BY LEVEL <= 12) s
JOIN ECMS_D_ST_AREA shaft ON shaft.id = 8830000 + s.s
JOIN ECMS_D_ST_AREA bld ON bld.id = shaft.parent_id
JOIN ECMS_D_ST_AREA estate ON estate.id = bld.parent_id
WHERE NVL(shaft.deleted,0)=0;

-- monitor: 每竖井1个，共12（名称非全路径）
INSERT INTO ECMS_D_ST_MONITOR (
  id,name,area_id,area_name,elevator_count,shaft_type,monitor_status,build_date,owner_company,device_id,remark,deleted,created_on,updated_on
)
SELECT
  8860000 + s.s,
  estate.name || bld.name || '1单元竖井',
  shaft.id,
  shaft.path_names,
  1,
  'POWER',
  'RUNNING',
  TO_DATE('2024-01-01','YYYY-MM-DD'),
  '深圳供电局',
  8850000 + s.s,
  '福田区台账抽取',
  0,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM (SELECT LEVEL s FROM dual CONNECT BY LEVEL <= 12) s
JOIN ECMS_D_ST_AREA shaft ON shaft.id = 8830000 + s.s
JOIN ECMS_D_ST_AREA bld ON bld.id = shaft.parent_id
JOIN ECMS_D_ST_AREA estate ON estate.id = bld.parent_id
WHERE NVL(shaft.deleted,0)=0;

INSERT INTO ECMS_D_ST_MONITOR_DEVICE_BIND (id,monitor_id,device_id,bind_status,bind_time,deleted,created_on,updated_on)
SELECT 8870000 + s.s, 8860000 + s.s, 8850000 + s.s, 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (SELECT LEVEL s FROM dual CONNECT BY LEVEL <= 12) s;

-- 楼层: 每竖井10~29层，共240
INSERT INTO ECMS_D_ST_SHAFT_FLOOR (
  id,monitor_id,area_id,name,device_id,start_point,end_point,sort,deleted,created_on,updated_on
)
SELECT
  8880000 + ((s.s - 1) * 20 + f.f),
  8860000 + s.s,
  8830000 + s.s,
  TO_CHAR(9 + f.f) || '层',
  8850000 + s.s,
  (f.f - 1) * 3 + 1,
  f.f * 3,
  9 + f.f,
  0,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM (SELECT LEVEL s FROM dual CONNECT BY LEVEL <= 12) s
CROSS JOIN (SELECT LEVEL f FROM dual CONNECT BY LEVEL <= 20) f;

-- 分区绑定: 每楼层1分区，共240
INSERT INTO ECMS_D_ST_MONITOR_PARTITION_BIND (
  id,monitor_id,device_id,shaft_floor_id,partition_id,partition_code,partition_name,data_reference,device_token,partition_no,bind_status,deleted,created_on,updated_on
)
SELECT
  8890000 + ((s.s - 1) * 20 + f.f),
  8860000 + s.s,
  8850000 + s.s,
  8880000 + ((s.s - 1) * 20 + f.f),
  9 + f.f,
  'ft-shaft-dev-' || LPAD(TO_CHAR(s.s),4,'0') || '_TMP_th' || LPAD(TO_CHAR(9 + f.f),2,'0'),
  TO_CHAR(9 + f.f) || '层分区',
  '/TMP/ft-shaft-dev-' || LPAD(TO_CHAR(s.s),4,'0') || '_TMP_th' || LPAD(TO_CHAR(9 + f.f),2,'0'),
  'ft-shaft-dev-' || LPAD(TO_CHAR(s.s),4,'0'),
  9 + f.f,
  1,
  0,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM (SELECT LEVEL s FROM dual CONNECT BY LEVEL <= 12) s
CROSS JOIN (SELECT LEVEL f FROM dual CONNECT BY LEVEL <= 20) f;

COMMIT;

SELECT 'AREA' tag, COUNT(*) cnt FROM ECMS_D_ST_AREA WHERE id BETWEEN 8800001 AND 8839999
UNION ALL SELECT 'ORG', COUNT(*) FROM ECMS_D_ST_ORG WHERE id BETWEEN 8800001 AND 8839999
UNION ALL SELECT 'SHAFT_DEVICE', COUNT(*) FROM ECMS_D_ST_SHAFT_DEVICE WHERE id BETWEEN 8850001 AND 8859999
UNION ALL SELECT 'MONITOR', COUNT(*) FROM ECMS_D_ST_MONITOR WHERE id BETWEEN 8860001 AND 8869999
UNION ALL SELECT 'MONITOR_DEVICE_BIND', COUNT(*) FROM ECMS_D_ST_MONITOR_DEVICE_BIND WHERE id BETWEEN 8870001 AND 8879999
UNION ALL SELECT 'SHAFT_FLOOR', COUNT(*) FROM ECMS_D_ST_SHAFT_FLOOR WHERE id BETWEEN 8880001 AND 8889999
UNION ALL SELECT 'MONITOR_PARTITION_BIND', COUNT(*) FROM ECMS_D_ST_MONITOR_PARTITION_BIND WHERE id BETWEEN 8890001 AND 8899999;
