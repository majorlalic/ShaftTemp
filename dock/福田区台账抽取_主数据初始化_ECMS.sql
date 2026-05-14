-- 福田区台账抽取主数据初始化（达梦）
-- 口径：
-- 1) area / org 结构一致，且不包含楼层节点
-- 2) 楼层数据仅落在 ECMS_D_ST_SHAFT_FLOOR
-- 3) 每栋 1 个竖井（1 单元）= 1 台设备 + 1 个监测对象
-- 4) asset_status 仅使用 0(未接入) / 1(已接入)
-- 5) 楼层严格按台账清单：
--    听水居(1/2/3/5栋)=22层，润雨居(1/2/3/5栋)=22层，
--    鸣翠居1栋=28层，2栋=29层，3栋=29层，5栋=30层，6栋=28层，8栋=28层

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

-- area: 按台账楼栋清单（14栋）
INSERT INTO ECMS_D_ST_AREA (id,parent_id,name,type,path_ids,path_names,deleted,sort,created_on,updated_on)
SELECT
  8820000 + b.building_no,
  8810000 + b.estate_no,
  TO_CHAR(b.building_no_in_estate) || '栋',
  'BUILDING',
  '8800001/8800002/8800003/8800004/' || TO_CHAR(8810000 + b.estate_no) || '/' || TO_CHAR(8820000 + b.building_no),
  '深圳供电局/福田区/香蜜湖街道/香蜜社区/' || b.estate_name || '/' || TO_CHAR(b.building_no_in_estate) || '栋',
  0,b.sort_no,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM (
  SELECT 1 estate_no, '听水居' estate_name, 1 building_no, 1 building_no_in_estate, 1 sort_no FROM dual
  UNION ALL SELECT 1,'听水居',2,2,2 FROM dual
  UNION ALL SELECT 1,'听水居',3,3,3 FROM dual
  UNION ALL SELECT 1,'听水居',4,5,4 FROM dual
  UNION ALL SELECT 2,'润雨居',5,1,1 FROM dual
  UNION ALL SELECT 2,'润雨居',6,2,2 FROM dual
  UNION ALL SELECT 2,'润雨居',7,3,3 FROM dual
  UNION ALL SELECT 2,'润雨居',8,5,4 FROM dual
  UNION ALL SELECT 3,'鸣翠居',9,1,1 FROM dual
  UNION ALL SELECT 3,'鸣翠居',10,2,2 FROM dual
  UNION ALL SELECT 3,'鸣翠居',11,3,3 FROM dual
  UNION ALL SELECT 3,'鸣翠居',12,5,4 FROM dual
  UNION ALL SELECT 3,'鸣翠居',13,6,5 FROM dual
  UNION ALL SELECT 3,'鸣翠居',14,8,6 FROM dual
) b;

-- area: 每栋1单元(shaft)，共14
INSERT INTO ECMS_D_ST_AREA (id,parent_id,name,type,path_ids,path_names,deleted,sort,created_on,updated_on)
SELECT
  8830000 + b.building_no,
  8820000 + b.building_no,
  '1单元',
  'SHAFT',
  a.path_ids || '/' || TO_CHAR(8830000 + b.building_no),
  a.path_names || '/1单元',
  0,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM (
  SELECT 1 building_no FROM dual UNION ALL SELECT 2 FROM dual UNION ALL SELECT 3 FROM dual UNION ALL SELECT 4 FROM dual
  UNION ALL SELECT 5 FROM dual UNION ALL SELECT 6 FROM dual UNION ALL SELECT 7 FROM dual UNION ALL SELECT 8 FROM dual
  UNION ALL SELECT 9 FROM dual UNION ALL SELECT 10 FROM dual UNION ALL SELECT 11 FROM dual UNION ALL SELECT 12 FROM dual
  UNION ALL SELECT 13 FROM dual UNION ALL SELECT 14 FROM dual
) b
JOIN ECMS_D_ST_AREA a ON a.id = 8820000 + b.building_no
WHERE NVL(a.deleted,0)=0;

-- org 镜像 area（不含楼层）
INSERT INTO ECMS_D_ST_ORG (id,parent_id,name,type,path_ids,path_names,deleted,sort,created_on,updated_on)
SELECT id,parent_id,name,type,path_ids,path_names,0,sort,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
FROM ECMS_D_ST_AREA
WHERE id BETWEEN 8800001 AND 8839999
  AND NVL(deleted,0)=0;

-- device: 每竖井1台，共14，asset_status 0/1
INSERT INTO ECMS_D_ST_SHAFT_DEVICE (
  id,device_type,name,area_id,iot_code,model,manufacturer,asset_status,org_id,online_status,deleted,created_on,updated_on,remark
)
SELECT
  8850000 + b.building_no,
  'SHAFT_TEMP',
  estate.name || bld.name || '1单元测温终端',
  shaft.id,
  'ft-shaft-dev-' || LPAD(TO_CHAR(b.building_no),4,'0'),
  'TMP-V1',
  'LEDGER_IMPORT',
  CASE WHEN MOD(b.building_no,2)=0 THEN 1 ELSE 0 END,
  shaft.id,
  0,
  0,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP,
  '福田区台账抽取'
FROM (
  SELECT 1 building_no FROM dual UNION ALL SELECT 2 FROM dual UNION ALL SELECT 3 FROM dual UNION ALL SELECT 4 FROM dual
  UNION ALL SELECT 5 FROM dual UNION ALL SELECT 6 FROM dual UNION ALL SELECT 7 FROM dual UNION ALL SELECT 8 FROM dual
  UNION ALL SELECT 9 FROM dual UNION ALL SELECT 10 FROM dual UNION ALL SELECT 11 FROM dual UNION ALL SELECT 12 FROM dual
  UNION ALL SELECT 13 FROM dual UNION ALL SELECT 14 FROM dual
) b
JOIN ECMS_D_ST_AREA shaft ON shaft.id = 8830000 + b.building_no
JOIN ECMS_D_ST_AREA bld ON bld.id = shaft.parent_id
JOIN ECMS_D_ST_AREA estate ON estate.id = bld.parent_id
WHERE NVL(shaft.deleted,0)=0;

-- monitor: 每竖井1个，共14（名称非全路径）
INSERT INTO ECMS_D_ST_MONITOR (
  id,name,area_id,area_name,elevator_count,shaft_type,monitor_status,build_date,owner_company,device_id,remark,deleted,created_on,updated_on
)
SELECT
  8860000 + b.building_no,
  estate.name || bld.name || '1单元竖井',
  shaft.id,
  shaft.path_names,
  1,
  'POWER',
  'RUNNING',
  TO_DATE('2024-01-01','YYYY-MM-DD'),
  '深圳供电局',
  8850000 + b.building_no,
  '福田区台账抽取',
  0,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM (
  SELECT 1 building_no FROM dual UNION ALL SELECT 2 FROM dual UNION ALL SELECT 3 FROM dual UNION ALL SELECT 4 FROM dual
  UNION ALL SELECT 5 FROM dual UNION ALL SELECT 6 FROM dual UNION ALL SELECT 7 FROM dual UNION ALL SELECT 8 FROM dual
  UNION ALL SELECT 9 FROM dual UNION ALL SELECT 10 FROM dual UNION ALL SELECT 11 FROM dual UNION ALL SELECT 12 FROM dual
  UNION ALL SELECT 13 FROM dual UNION ALL SELECT 14 FROM dual
) b
JOIN ECMS_D_ST_AREA shaft ON shaft.id = 8830000 + b.building_no
JOIN ECMS_D_ST_AREA bld ON bld.id = shaft.parent_id
JOIN ECMS_D_ST_AREA estate ON estate.id = bld.parent_id
WHERE NVL(shaft.deleted,0)=0;

INSERT INTO ECMS_D_ST_MONITOR_DEVICE_BIND (id,monitor_id,device_id,bind_status,bind_time,deleted,created_on,updated_on)
SELECT 8870000 + b.building_no, 8860000 + b.building_no, 8850000 + b.building_no, 1, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (
  SELECT 1 building_no FROM dual UNION ALL SELECT 2 FROM dual UNION ALL SELECT 3 FROM dual UNION ALL SELECT 4 FROM dual
  UNION ALL SELECT 5 FROM dual UNION ALL SELECT 6 FROM dual UNION ALL SELECT 7 FROM dual UNION ALL SELECT 8 FROM dual
  UNION ALL SELECT 9 FROM dual UNION ALL SELECT 10 FROM dual UNION ALL SELECT 11 FROM dual UNION ALL SELECT 12 FROM dual
  UNION ALL SELECT 13 FROM dual UNION ALL SELECT 14 FROM dual
) b;

-- 楼层: 按台账楼栋层数清单生成，共348层
INSERT INTO ECMS_D_ST_SHAFT_FLOOR (
  id,monitor_id,area_id,name,device_id,start_point,end_point,sort,deleted,created_on,updated_on
)
SELECT
  8880000 + t.rn,
  t.monitor_id,
  t.area_id,
  TO_CHAR(t.floor_no) || '层',
  t.device_id,
  (t.floor_no - 1) * 3 + 1,
  t.floor_no * 3,
  t.floor_no,
  0,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM (
  SELECT
    ROW_NUMBER() OVER (ORDER BY b.building_no, f.f) rn,
    8860000 + b.building_no AS monitor_id,
    8830000 + b.building_no AS area_id,
    8850000 + b.building_no AS device_id,
    f.f AS floor_no
  FROM (
    SELECT 1 building_no, 22 floor_count FROM dual
    UNION ALL SELECT 2,22 FROM dual
    UNION ALL SELECT 3,22 FROM dual
    UNION ALL SELECT 4,22 FROM dual
    UNION ALL SELECT 5,22 FROM dual
    UNION ALL SELECT 6,22 FROM dual
    UNION ALL SELECT 7,22 FROM dual
    UNION ALL SELECT 8,22 FROM dual
    UNION ALL SELECT 9,28 FROM dual
    UNION ALL SELECT 10,29 FROM dual
    UNION ALL SELECT 11,29 FROM dual
    UNION ALL SELECT 12,30 FROM dual
    UNION ALL SELECT 13,28 FROM dual
    UNION ALL SELECT 14,28 FROM dual
  ) b
  CROSS JOIN (SELECT LEVEL f FROM dual CONNECT BY LEVEL <= 30) f
  WHERE f.f <= b.floor_count
) t;

-- 分区绑定: 每楼层1分区，共348
INSERT INTO ECMS_D_ST_MONITOR_PARTITION_BIND (
  id,monitor_id,device_id,shaft_floor_id,partition_id,partition_code,partition_name,data_reference,device_token,partition_no,bind_status,deleted,created_on,updated_on
)
SELECT
  8890000 + t.rn,
  t.monitor_id,
  t.device_id,
  8880000 + t.rn,
  t.floor_no,
  'ft-shaft-dev-' || LPAD(TO_CHAR(t.shaft_no),4,'0') || '_TMP_th' || LPAD(TO_CHAR(t.floor_no),2,'0'),
  TO_CHAR(t.floor_no) || '层分区',
  '/TMP/ft-shaft-dev-' || LPAD(TO_CHAR(t.shaft_no),4,'0') || '_TMP_th' || LPAD(TO_CHAR(t.floor_no),2,'0'),
  'ft-shaft-dev-' || LPAD(TO_CHAR(t.shaft_no),4,'0'),
  t.floor_no,
  1,
  0,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM (
  SELECT
    ROW_NUMBER() OVER (ORDER BY b.building_no, f.f) rn,
    b.building_no AS shaft_no,
    8860000 + b.building_no AS monitor_id,
    8850000 + b.building_no AS device_id,
    f.f AS floor_no
  FROM (
    SELECT 1 building_no, 22 floor_count FROM dual
    UNION ALL SELECT 2,22 FROM dual
    UNION ALL SELECT 3,22 FROM dual
    UNION ALL SELECT 4,22 FROM dual
    UNION ALL SELECT 5,22 FROM dual
    UNION ALL SELECT 6,22 FROM dual
    UNION ALL SELECT 7,22 FROM dual
    UNION ALL SELECT 8,22 FROM dual
    UNION ALL SELECT 9,28 FROM dual
    UNION ALL SELECT 10,29 FROM dual
    UNION ALL SELECT 11,29 FROM dual
    UNION ALL SELECT 12,30 FROM dual
    UNION ALL SELECT 13,28 FROM dual
    UNION ALL SELECT 14,28 FROM dual
  ) b
  CROSS JOIN (SELECT LEVEL f FROM dual CONNECT BY LEVEL <= 30) f
  WHERE f.f <= b.floor_count
) t;

COMMIT;

SELECT 'AREA' tag, COUNT(*) cnt FROM ECMS_D_ST_AREA WHERE id BETWEEN 8800001 AND 8839999
UNION ALL SELECT 'ORG', COUNT(*) FROM ECMS_D_ST_ORG WHERE id BETWEEN 8800001 AND 8839999
UNION ALL SELECT 'SHAFT_DEVICE', COUNT(*) FROM ECMS_D_ST_SHAFT_DEVICE WHERE id BETWEEN 8850001 AND 8859999
UNION ALL SELECT 'MONITOR', COUNT(*) FROM ECMS_D_ST_MONITOR WHERE id BETWEEN 8860001 AND 8869999
UNION ALL SELECT 'MONITOR_DEVICE_BIND', COUNT(*) FROM ECMS_D_ST_MONITOR_DEVICE_BIND WHERE id BETWEEN 8870001 AND 8879999
UNION ALL SELECT 'SHAFT_FLOOR', COUNT(*) FROM ECMS_D_ST_SHAFT_FLOOR WHERE id BETWEEN 8880001 AND 8889999
UNION ALL SELECT 'MONITOR_PARTITION_BIND', COUNT(*) FROM ECMS_D_ST_MONITOR_PARTITION_BIND WHERE id BETWEEN 8890001 AND 8899999;
