-- ODS_DWEQ_DM_ALARM_D 升级脚本（达梦）
-- 目标：
-- 1) id / monitor_id / device_id / handler 改为字符串
-- 2) 新增业务字段
-- 3) 支持分页查询的筛选索引（status / alarm_type_big / 告警时间 / area_name）

-- =========================
-- 1. 字段类型调整
-- =========================
ALTER TABLE ODS_DWEQ_DM_ALARM_D MODIFY id VARCHAR(64);
ALTER TABLE ODS_DWEQ_DM_ALARM_D MODIFY monitor_id VARCHAR(64);
ALTER TABLE ODS_DWEQ_DM_ALARM_D MODIFY device_id VARCHAR(64);
ALTER TABLE ODS_DWEQ_DM_ALARM_D MODIFY handler VARCHAR(64);

-- =========================
-- 2. 新增字段
-- =========================
ALTER TABLE ODS_DWEQ_DM_ALARM_D ADD alarm_type_big INT DEFAULT 0;
ALTER TABLE ODS_DWEQ_DM_ALARM_D ADD area_name VARCHAR(255);
ALTER TABLE ODS_DWEQ_DM_ALARM_D ADD monitor_name VARCHAR(255);
ALTER TABLE ODS_DWEQ_DM_ALARM_D ADD device_name VARCHAR(255);
ALTER TABLE ODS_DWEQ_DM_ALARM_D ADD handler_name VARCHAR(255);
ALTER TABLE ODS_DWEQ_DM_ALARM_D ADD manufacturer VARCHAR(255);
ALTER TABLE ODS_DWEQ_DM_ALARM_D ADD device_model VARCHAR(255);
ALTER TABLE ODS_DWEQ_DM_ALARM_D ADD push_time DATETIME;

-- =========================
-- 3. 字段注释
-- =========================
COMMENT ON COLUMN ODS_DWEQ_DM_ALARM_D.id IS '告警ID（字符串）';
COMMENT ON COLUMN ODS_DWEQ_DM_ALARM_D.monitor_id IS '监测对象ID（字符串）';
COMMENT ON COLUMN ODS_DWEQ_DM_ALARM_D.device_id IS '设备ID（字符串）';
COMMENT ON COLUMN ODS_DWEQ_DM_ALARM_D.handler IS '处理人ID（字符串）';
COMMENT ON COLUMN ODS_DWEQ_DM_ALARM_D.alarm_type_big IS '告警大类 0竖井测温 1配电柜局放 2行波定位 3蓄电池 4主变振动 5铁芯接地电流';
COMMENT ON COLUMN ODS_DWEQ_DM_ALARM_D.area_name IS '小区名称';
COMMENT ON COLUMN ODS_DWEQ_DM_ALARM_D.monitor_name IS '监测对象名称';
COMMENT ON COLUMN ODS_DWEQ_DM_ALARM_D.device_name IS '设备名称';
COMMENT ON COLUMN ODS_DWEQ_DM_ALARM_D.handler_name IS '处理人名称';
COMMENT ON COLUMN ODS_DWEQ_DM_ALARM_D.manufacturer IS '设备厂家';
COMMENT ON COLUMN ODS_DWEQ_DM_ALARM_D.device_model IS '设备型号';
COMMENT ON COLUMN ODS_DWEQ_DM_ALARM_D.push_time IS '推送时间';

-- =========================
-- 4. 查询索引
-- =========================
-- 说明：area_name 模糊查询通常无法高效命中普通索引，但仍可作为前缀命中场景优化
CREATE INDEX idx_alarm_typebig_status_time ON ODS_DWEQ_DM_ALARM_D (alarm_type_big, status, last_alarm_time);
CREATE INDEX idx_alarm_area_name ON ODS_DWEQ_DM_ALARM_D (area_name);

-- =========================
-- 5. 存量数据回填（可选）
-- =========================
-- 统一设置大类：现有流程全部按竖井测温
UPDATE ODS_DWEQ_DM_ALARM_D SET alarm_type_big = 0 WHERE alarm_type_big IS NULL;

COMMIT;
