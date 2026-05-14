-- 福田区台账抽取接口联调初始化（达梦）
-- 前置：先执行 dock/福田区台账抽取_主数据初始化_ECMS.sql
-- 口径：
-- 1) alarm 状态仅生成 3 种：1(待确认)、3(已确认)、11(持续观察)
-- 2) alarm.merge_count、alarm.event_count 与 event 实际条数一致
-- 3) alarm 冗余字段 device_name / monitor_name 直接取主表 name（不使用路径）
-- 4) 设备接入状态不在本脚本改写（由主数据脚本保证 0/1）

DELETE FROM ECMS_D_ST_DEVICE_ONLINE_LOG WHERE id BETWEEN 9940001 AND 9943000;
DELETE FROM ECMS_D_ST_RAW_DATA WHERE id BETWEEN 9960001 AND 9966000;
DELETE FROM ECMS_D_ST_ALARM_RAW WHERE id BETWEEN 9970001 AND 9973000;
DELETE FROM ECMS_D_ST_EVENT WHERE id BETWEEN 9990001 AND 9995000;
DELETE FROM ECMS_D_ST_ALARM WHERE id BETWEEN '9980001' AND '9985000';
DELETE FROM ECMS_D_ST_ALARM_RULE WHERE id BETWEEN 9950001 AND 9950005;

COMMIT;

-- 规则
INSERT INTO ECMS_D_ST_ALARM_RULE (id,rule_name,biz_type,alarm_type,scope_type,scope_id,level,threshold_value,threshold_value2,duration_seconds,enabled,remark,deleted,created_on,updated_on)
VALUES (9950001,'温度阈值','MONITOR','TEMP_THRESHOLD','GLOBAL',NULL,2,70.00,NULL,NULL,1,'福田区联调默认规则',0,TO_DATE('2026-04-01 09:00:00','YYYY-MM-DD HH24:MI:SS'),TO_DATE('2026-04-01 09:00:00','YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ECMS_D_ST_ALARM_RULE (id,rule_name,biz_type,alarm_type,scope_type,scope_id,level,threshold_value,threshold_value2,duration_seconds,enabled,remark,deleted,created_on,updated_on)
VALUES (9950002,'差温阈值','MONITOR','TEMP_DIFFERENCE','GLOBAL',NULL,2,20.00,NULL,NULL,1,'福田区联调默认规则',0,TO_DATE('2026-04-01 09:00:00','YYYY-MM-DD HH24:MI:SS'),TO_DATE('2026-04-01 09:00:00','YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ECMS_D_ST_ALARM_RULE (id,rule_name,biz_type,alarm_type,scope_type,scope_id,level,threshold_value,threshold_value2,duration_seconds,enabled,remark,deleted,created_on,updated_on)
VALUES (9950003,'升温速率','MONITOR','TEMP_RISE_RATE','GLOBAL',NULL,2,10.00,NULL,60,1,'福田区联调默认规则',0,TO_DATE('2026-04-01 09:00:00','YYYY-MM-DD HH24:MI:SS'),TO_DATE('2026-04-01 09:00:00','YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ECMS_D_ST_ALARM_RULE (id,rule_name,biz_type,alarm_type,scope_type,scope_id,level,threshold_value,threshold_value2,duration_seconds,enabled,remark,deleted,created_on,updated_on)
VALUES (9950004,'设备离线','DEVICE','DEVICE_OFFLINE','GLOBAL',NULL,1,30.00,NULL,NULL,1,'福田区联调默认规则',0,TO_DATE('2026-04-01 09:00:00','YYYY-MM-DD HH24:MI:SS'),TO_DATE('2026-04-01 09:00:00','YYYY-MM-DD HH24:MI:SS'));
INSERT INTO ECMS_D_ST_ALARM_RULE (id,rule_name,biz_type,alarm_type,scope_type,scope_id,level,threshold_value,threshold_value2,duration_seconds,enabled,remark,deleted,created_on,updated_on)
VALUES (9950005,'分区断纤','DEVICE','PARTITION_FAULT','GLOBAL',NULL,1,0.00,NULL,NULL,1,'福田区联调默认规则',0,TO_DATE('2026-04-01 09:00:00','YYYY-MM-DD HH24:MI:SS'),TO_DATE('2026-04-01 09:00:00','YYYY-MM-DD HH24:MI:SS'));

-- raw_data: 348分区 * 8 = 2784
INSERT INTO ECMS_D_ST_RAW_DATA (
  id,device_id,iot_code,topic,partition_id,monitor_id,shaft_floor_id,data_reference,ied_full_path,collect_time,
  max_temp,min_temp,avg_temp,max_temp_position,min_temp_position,max_temp_channel,min_temp_channel,payload_json,deleted,created_on
)
SELECT
  9960000 + t.rn,
  t.device_id,
  t.device_token,
  t.data_reference || '/Measure',
  t.partition_id,
  t.monitor_id,
  t.shaft_floor_id,
  t.data_reference,
  '/IED/' || t.device_token,
  TO_DATE('2026-05-13 23:40:00','YYYY-MM-DD HH24:MI:SS') - ((8 - t.seq_no) * 3) - (MOD(t.rn,120) / 1440),
  58 + MOD(t.partition_id,15) + MOD(t.rn,7) * 0.2,
  35 + MOD(t.partition_id,9) + MOD(t.rn,5) * 0.1,
  46 + MOD(t.partition_id,11) + MOD(t.rn,6) * 0.1,
  60 + MOD(t.partition_id,25),
  10 + MOD(t.partition_id,18),
  MOD(t.partition_id,4) + 1,
  MOD(t.partition_id + 1,4) + 1,
  '{"scene":"ft-ledger-measure","seq":' || TO_CHAR(t.seq_no) || ',"partitionId":' || TO_CHAR(t.partition_id) || '}',
  0,
  TO_DATE('2026-05-13 23:50:00','YYYY-MM-DD HH24:MI:SS')
FROM (
  SELECT ROW_NUMBER() OVER (ORDER BY b.id,s.seq_no) rn,s.seq_no,b.monitor_id,b.device_id,b.shaft_floor_id,b.partition_id,b.data_reference,b.device_token
  FROM ECMS_D_ST_MONITOR_PARTITION_BIND b
  CROSS JOIN (SELECT LEVEL seq_no FROM dual CONNECT BY LEVEL <= 8) s
  WHERE NVL(b.deleted,0)=0 AND b.bind_status=1 AND b.device_token LIKE 'ft-shaft-dev-%'
) t
WHERE t.rn <= 2784;

-- alarm_raw: 348分区 * 5 = 1740
INSERT INTO ECMS_D_ST_ALARM_RAW (
  id,iot_code,topic,partition_id,alarm_status,fault_status,ied_full_path,data_reference,collect_time,payload_json,deleted,created_on
)
SELECT
  9970000 + t.rn,
  t.device_token,
  t.data_reference || '/Alarm',
  t.partition_id,
  CASE WHEN MOD(t.rn,4)=0 THEN 1 ELSE 0 END,
  CASE WHEN MOD(t.rn,9)=0 THEN 1 ELSE 0 END,
  '/IED/' || t.device_token,
  t.data_reference,
  TO_DATE('2026-05-13 21:00:00','YYYY-MM-DD HH24:MI:SS') - ((5 - t.seq_no) * 4) - (MOD(t.rn,180) / 1440),
  '{"scene":"ft-ledger-alarm-raw","seq":' || TO_CHAR(t.seq_no) || ',"partitionId":' || TO_CHAR(t.partition_id) || '}',
  0,
  TO_DATE('2026-05-13 21:10:00','YYYY-MM-DD HH24:MI:SS')
FROM (
  SELECT ROW_NUMBER() OVER (ORDER BY b.id,s.seq_no) rn,s.seq_no,b.partition_id,b.data_reference,b.device_token
  FROM ECMS_D_ST_MONITOR_PARTITION_BIND b
  CROSS JOIN (SELECT LEVEL seq_no FROM dual CONNECT BY LEVEL <= 5) s
  WHERE NVL(b.deleted,0)=0 AND b.bind_status=1 AND b.device_token LIKE 'ft-shaft-dev-%'
) t
WHERE t.rn <= 1740;

-- alarm: 348分区 * 3状态 = 1044；每条 event_count=2, merge_count=2
INSERT INTO ECMS_D_ST_ALARM (
  id,alarm_code,alarm_type,source_type,monitor_id,device_id,shaft_floor_id,partition_code,partition_name,data_reference,device_token,partition_no,source_format,
  merge_key,status,first_alarm_time,last_alarm_time,merge_count,event_count,alarm_level,title,content,handler,handle_time,handle_remark,push_status,
  alarm_type_big,alarm_domain,area_name,monitor_name,device_name,handler_name,manufacturer,device_model,push_time,deleted,created_on,updated_on
)
SELECT
  TO_CHAR(9980000 + t.rn),
  SUBSTR('FT-ALM-' || TO_CHAR(9980000 + t.rn),1,64),
  SUBSTR(t.alarm_type,1,32),
  SUBSTR(CASE WHEN t.alarm_type IN ('DEVICE_OFFLINE','PARTITION_FAULT') THEN 'DEVICE_REPORT' ELSE 'MEASURE_REPORT' END,1,32),
  TO_CHAR(t.monitor_id),
  TO_CHAR(t.device_id),
  t.shaft_floor_id,
  SUBSTR(t.partition_code,1,128),
  SUBSTR(t.partition_name,1,100),
  SUBSTR(t.data_reference,1,255),
  SUBSTR(t.device_token,1,64),
  t.partition_id,
  'PARTITION',
  CASE WHEN t.status = 1 THEN SUBSTR('M:' || TO_CHAR(t.monitor_id) || ':' || t.alarm_type || ':' || TO_CHAR(t.rn),1,64) ELSE NULL END,
  t.status,
  TO_DATE('2026-05-13 20:00:00','YYYY-MM-DD HH24:MI:SS') - (MOD(t.rn,20)) - (MOD(t.rn,240) / 1440),
  TO_DATE('2026-05-13 21:30:00','YYYY-MM-DD HH24:MI:SS') - (MOD(t.rn,10)) - (MOD(t.rn,180) / 1440),
  2,
  2,
  CASE WHEN t.alarm_type IN ('DEVICE_OFFLINE','PARTITION_FAULT') THEN 1 ELSE 2 END,
  SUBSTR('福田区联调告警-' || TO_CHAR(t.rn),1,120),
  SUBSTR(
    t.partition_name || '发生' ||
    CASE t.alarm_type
      WHEN 'TEMP_THRESHOLD' THEN '温度阈值告警'
      WHEN 'TEMP_DIFFERENCE' THEN '差温告警'
      WHEN 'TEMP_RISE_RATE' THEN '升温速率告警'
      WHEN 'DEVICE_OFFLINE' THEN '设备离线告警'
      WHEN 'PARTITION_FAULT' THEN '分区断纤告警'
      ELSE '异常告警'
    END,
    1,500
  ),
  CASE WHEN t.status IN (3,11) THEN 'handler-001' ELSE NULL END,
  CASE WHEN t.status IN (3,11) THEN TO_DATE('2026-05-13 22:00:00','YYYY-MM-DD HH24:MI:SS') - (MOD(t.rn,120) / 1440) ELSE NULL END,
  CASE WHEN t.status IN (3,11) THEN '联调处警' ELSE NULL END,
  CASE WHEN MOD(t.rn,3)=0 THEN 1 ELSE 0 END,
  0,
  CASE WHEN t.alarm_type IN ('DEVICE_OFFLINE','PARTITION_FAULT') THEN 0 ELSE 1 END,
  SUBSTR(t.area_name,1,100),
  SUBSTR(t.monitor_name,1,100),
  SUBSTR(t.device_name,1,100),
  CASE WHEN t.status IN (3,11) THEN '运维值班员A' ELSE NULL END,
  SUBSTR(t.manufacturer,1,100),
  SUBSTR(t.device_model,1,100),
  CASE WHEN MOD(t.rn,3)=0 THEN TO_DATE('2026-05-13 23:00:00','YYYY-MM-DD HH24:MI:SS') - (MOD(t.rn,90) / 1440) ELSE NULL END,
  0,
  TO_DATE('2026-05-13 23:10:00','YYYY-MM-DD HH24:MI:SS'),
  TO_DATE('2026-05-13 23:10:00','YYYY-MM-DD HH24:MI:SS')
FROM (
  SELECT
    ROW_NUMBER() OVER (ORDER BY b.id,s.status_sort) rn,
    b.monitor_id,b.device_id,b.shaft_floor_id,b.partition_id,b.partition_code,b.partition_name,b.data_reference,b.device_token,
    m.area_name,m.name monitor_name,d.name device_name,d.manufacturer,d.model device_model,
    CASE MOD(ROW_NUMBER() OVER (ORDER BY b.id,s.status_sort),5)
      WHEN 0 THEN 'TEMP_THRESHOLD'
      WHEN 1 THEN 'TEMP_DIFFERENCE'
      WHEN 2 THEN 'TEMP_RISE_RATE'
      WHEN 3 THEN 'DEVICE_OFFLINE'
      ELSE 'PARTITION_FAULT'
    END alarm_type,
    s.status_code status
  FROM ECMS_D_ST_MONITOR_PARTITION_BIND b
  JOIN ECMS_D_ST_MONITOR m ON m.id=b.monitor_id AND NVL(m.deleted,0)=0
  JOIN ECMS_D_ST_SHAFT_DEVICE d ON d.id=b.device_id AND NVL(d.deleted,0)=0
  CROSS JOIN (
    SELECT 1 status_sort, 1 status_code FROM dual
    UNION ALL SELECT 2,3 FROM dual
    UNION ALL SELECT 3,11 FROM dual
  ) s
  WHERE NVL(b.deleted,0)=0 AND b.bind_status=1 AND b.device_token LIKE 'ft-shaft-dev-%'
) t
WHERE t.rn <= 1044;

-- event: 每条alarm生成2条 => 与 alarm.event_count 对齐
INSERT INTO ECMS_D_ST_EVENT (
  id,alarm_id,alarm_type,source_type,monitor_id,device_id,shaft_floor_id,partition_code,partition_name,data_reference,device_token,partition_no,source_format,
  event_type,event_time,event_no,event_level,point_list_json,detail_json,content,merged_flag,deleted,created_on,updated_on
)
SELECT
  9990000 + x.rn,
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
  '[]',
  '{"scene":"ft-ledger-event","eventNo":' || TO_CHAR(x.event_no) || '}',
  SUBSTR(x.content || ' 事件' || TO_CHAR(x.event_no),1,500),
  CASE WHEN x.event_no=2 THEN 1 ELSE 0 END,
  0,
  TO_DATE('2026-05-13 23:20:00','YYYY-MM-DD HH24:MI:SS'),
  TO_DATE('2026-05-13 23:20:00','YYYY-MM-DD HH24:MI:SS')
FROM (
  SELECT
    ROW_NUMBER() OVER (ORDER BY a.id,e.seq_no) rn,
    TO_NUMBER(a.id) alarm_id,
    a.alarm_type,
    a.source_type,
    TO_NUMBER(a.monitor_id) monitor_id,
    TO_NUMBER(a.device_id) device_id,
    a.shaft_floor_id,
    a.partition_code,
    a.partition_name,
    a.data_reference,
    a.device_token,
    a.partition_no,
    a.source_format,
    CASE WHEN e.seq_no = 1 THEN 0 ELSE
      CASE
        WHEN a.status = 1 THEN 1
        WHEN a.status = 11 THEN 3
        WHEN a.status = 3 THEN 2
        ELSE 3
      END
    END event_type,
    a.last_alarm_time + (e.seq_no / 1440) event_time,
    e.seq_no event_no,
    a.alarm_level event_level,
    a.content
  FROM ECMS_D_ST_ALARM a
  CROSS JOIN (SELECT 1 seq_no FROM dual UNION ALL SELECT 2 FROM dual) e
WHERE a.id BETWEEN '9980001' AND '9985000' AND NVL(a.deleted,0)=0
) x
WHERE x.rn <= 2088;

-- 在线日志：14设备 * 120 = 1680
INSERT INTO ECMS_D_ST_DEVICE_ONLINE_LOG (
  id,device_id,status,change_time,reason,deleted,created_on
)
SELECT
  9940000 + t.rn,
  t.device_id,
  CASE WHEN MOD(t.rn,7)=0 THEN 0 ELSE 1 END,
  TO_DATE('2026-05-13 23:30:00','YYYY-MM-DD HH24:MI:SS') - (MOD(t.rn,30)) - (MOD(t.rn,1440) / 1440),
  CASE WHEN MOD(t.rn,7)=0 THEN 'offline inspection' ELSE 'report heartbeat' END,
  0,
  TO_DATE('2026-05-13 23:35:00','YYYY-MM-DD HH24:MI:SS')
FROM (
  SELECT ROW_NUMBER() OVER (ORDER BY d.id,x.n) rn,d.id device_id
  FROM ECMS_D_ST_SHAFT_DEVICE d
  CROSS JOIN (SELECT LEVEL n FROM dual CONNECT BY LEVEL <= 120) x
  WHERE NVL(d.deleted,0)=0 AND d.iot_code LIKE 'ft-shaft-dev-%'
) t
WHERE t.rn <= 1680;

COMMIT;

-- 数据量
SELECT 'ALARM' t, COUNT(*) c FROM ECMS_D_ST_ALARM WHERE id BETWEEN '9980001' AND '9985000'
UNION ALL SELECT 'EVENT', COUNT(*) FROM ECMS_D_ST_EVENT WHERE id BETWEEN 9990001 AND 9995000
UNION ALL SELECT 'RAW_DATA', COUNT(*) FROM ECMS_D_ST_RAW_DATA WHERE id BETWEEN 9960001 AND 9966000
UNION ALL SELECT 'ALARM_RAW', COUNT(*) FROM ECMS_D_ST_ALARM_RAW WHERE id BETWEEN 9970001 AND 9973000
UNION ALL SELECT 'DEVICE_ONLINE_LOG', COUNT(*) FROM ECMS_D_ST_DEVICE_ONLINE_LOG WHERE id BETWEEN 9940001 AND 9943000;

-- 校验1：event_count / merge_count 与 event表一致
SELECT COUNT(*) mismatch_cnt
FROM (
  SELECT a.id, a.event_count, a.merge_count, NVL(e.cnt,0) real_cnt
  FROM ECMS_D_ST_ALARM a
  LEFT JOIN (
    SELECT alarm_id, COUNT(*) cnt
    FROM ECMS_D_ST_EVENT
    WHERE NVL(deleted,0)=0
    GROUP BY alarm_id
  ) e ON e.alarm_id = TO_NUMBER(a.id)
  WHERE a.id BETWEEN '9980001' AND '9985000'
    AND NVL(a.deleted,0)=0
) t
WHERE NVL(t.event_count,0) <> NVL(t.real_cnt,0)
   OR NVL(t.merge_count,0) <> NVL(t.real_cnt,0);

-- 校验2：alarm.device_name / monitor_name 与主表一致
SELECT COUNT(*) mismatch_name_cnt
FROM ECMS_D_ST_ALARM a
JOIN ECMS_D_ST_SHAFT_DEVICE d ON d.id = TO_NUMBER(a.device_id)
JOIN ECMS_D_ST_MONITOR m ON m.id = TO_NUMBER(a.monitor_id)
WHERE a.id BETWEEN '9980001' AND '9985000'
  AND NVL(a.deleted,0)=0
  AND (NVL(a.device_name,'') <> NVL(d.name,'') OR NVL(a.monitor_name,'') <> NVL(m.name,''));

-- 校验3：告警状态只有 1/3/11
SELECT status, COUNT(*) cnt
FROM ECMS_D_ST_ALARM
WHERE id BETWEEN '9980001' AND '9985000'
  AND NVL(deleted,0)=0
GROUP BY status
ORDER BY status;
