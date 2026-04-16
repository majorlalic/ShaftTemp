-- 告警域字段升级（终端告警/监测对象告警）
-- 适用表：ODS_DWEQ_DM_ALARM_D

ALTER TABLE ODS_DWEQ_DM_ALARM_D ADD alarm_domain INTEGER DEFAULT 1 NOT NULL;

COMMENT ON COLUMN ODS_DWEQ_DM_ALARM_D.alarm_domain IS '告警域：0终端告警，1监测对象告警';

CREATE INDEX IDX_ALARM_DOMAIN_STATUS_TIME
ON ODS_DWEQ_DM_ALARM_D(alarm_domain, status, last_alarm_time);

