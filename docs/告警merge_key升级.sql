-- 告警 merge_key 升级 SQL
-- 用途：
-- 1. 为待确认告警增加数据库唯一占位键
-- 2. 保证同一 monitor + alarmType 在待确认状态下只能有一条主单

USE shaft;

ALTER TABLE alarm
    ADD COLUMN merge_key varchar(128) NULL COMMENT '待确认合并占位键' AFTER source_format;

UPDATE alarm
SET merge_key = CONCAT(monitor_id, ':', alarm_type)
WHERE status = 0
  AND (deleted IS NULL OR deleted = 0);

ALTER TABLE alarm
    ADD UNIQUE KEY uk_alarm_merge_key (merge_key);

-- 核对结果
-- SELECT id, monitor_id, alarm_type, status, merge_key FROM alarm ORDER BY id DESC LIMIT 20;
