-- 告警事件计数升级 SQL
-- 用途：
-- 1. 在 alarm 表增加 event_count 字段
-- 2. 保留 merge_count 作为真实累计触发次数
-- 3. 用 event_count 表示当前主单下已落库的事件条数

USE shaft;

ALTER TABLE alarm
    ADD COLUMN event_count int unsigned NULL COMMENT '已记录事件次数' AFTER merge_count;

UPDATE alarm a
LEFT JOIN (
    SELECT alarm_id, COUNT(*) AS cnt
    FROM event
    WHERE deleted IS NULL OR deleted = 0
    GROUP BY alarm_id
) e ON e.alarm_id = a.id
SET a.event_count = IFNULL(e.cnt, 0);

-- 核对结果
-- SELECT id, alarm_type, merge_count, event_count FROM alarm ORDER BY id DESC LIMIT 20;
