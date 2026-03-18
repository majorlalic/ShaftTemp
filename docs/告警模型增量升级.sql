-- 告警模型增量升级 SQL
-- 用途：
-- 1. 适配最新“告警状态码 + 事件类型 + 事件内容 + 待确认合并”方案
-- 2. 在已有 shaft 库上增量执行
-- 3. 不重建全库

USE shaft;

-- 1. alarm.status 从字符串状态迁移为数值状态码
-- 目标状态码：
-- 0 待确认
-- 1 已确认
-- 2 持续观察
-- 3 误报
-- 4 已关闭
-- 5 自动恢复

ALTER TABLE alarm
    ADD COLUMN status_code_tmp tinyint NULL COMMENT '告警状态码 0待确认1已确认2持续观察3误报4关闭5自动恢复';

UPDATE alarm
SET status_code_tmp = CASE status
    WHEN 'ACTIVE' THEN 0
    WHEN 'CONFIRMED' THEN 1
    WHEN 'RECOVERED' THEN 5
    WHEN 'CLOSED' THEN 4
    WHEN 'PENDING_CONFIRM' THEN 0
    WHEN 'OBSERVING' THEN 2
    WHEN 'FALSE_POSITIVE' THEN 3
    ELSE 0
END;

ALTER TABLE alarm
    DROP COLUMN status;

ALTER TABLE alarm
    CHANGE COLUMN status_code_tmp status tinyint COMMENT '告警状态码 0待确认1已确认2持续观察3误报4关闭5自动恢复';

-- 2. event 增加事件类型和事件内容

ALTER TABLE alarm
    MODIFY COLUMN source_type varchar(32) COMMENT '来源类型';

ALTER TABLE event
    MODIFY COLUMN source_type varchar(32) COMMENT '来源类型';

ALTER TABLE event
    ADD COLUMN event_type tinyint NULL COMMENT '事件类型码 0触发1合并2确认3观察4误报5恢复6关闭' AFTER source_format;

ALTER TABLE event
    ADD COLUMN content varchar(1000) NULL COMMENT '事件内容' AFTER detail_json;

-- 3. 历史 event.source_type 映射到 event_type
-- 当前映射规则：
-- REALTIME / MQ_STATUS / INSPECTION -> 0 触发
-- RECOVERY -> 5 恢复
-- MANUAL_CONFIRM -> 2 确认
-- MANUAL_OBSERVE -> 3 观察
-- MANUAL_FALSE_POSITIVE / MANUAL_FALSE -> 4 误报
-- MANUAL_CLOSE -> 6 关闭

UPDATE event
SET event_type = CASE source_type
    WHEN 'REALTIME' THEN 0
    WHEN 'MQ_STATUS' THEN 0
    WHEN 'INSPECTION' THEN 0
    WHEN 'RECOVERY' THEN 5
    WHEN 'MANUAL_CONFIRM' THEN 2
    WHEN 'MANUAL_OBSERVE' THEN 3
    WHEN 'MANUAL_FALSE_POSITIVE' THEN 4
    WHEN 'MANUAL_FALSE' THEN 4
    WHEN 'MANUAL_CLOSE' THEN 6
    ELSE 0
END
WHERE event_type IS NULL;

-- 4. 历史事件内容回填
-- 优先使用告警主单 content

UPDATE event e
JOIN alarm a ON a.id = e.alarm_id
SET e.content = a.content
WHERE e.content IS NULL OR e.content = '';

-- 5. 可选：把历史 merged_flag=1 且 event_type=0 的记录标识为合并事件
-- 如果你希望区分“首次触发”和“合并触发”，可以执行下面语句

UPDATE event
SET event_type = 1
WHERE merged_flag = 1
  AND event_type = 0;

-- 6. 核对结果用查询
-- SELECT id, alarm_type, status, first_alarm_time, last_alarm_time, merge_count, content FROM alarm ORDER BY id DESC LIMIT 20;
-- SELECT id, alarm_id, event_type, source_type, event_time, content, merged_flag FROM event ORDER BY id DESC LIMIT 50;
