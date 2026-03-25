# 下一步 TODO

### 1. 接口收口

目标：

- 避免两套告警接口长期并存

### 2. 原始数据方案收敛

目标：

- 明确当前分区版 `raw_data` 的长期策略
- 为未来设备数组消息预留独立原始表方案
- 决定是否移除 `temp_stat_minute`

建议：

- 当前继续保留分区版 `raw_data`
- 后续如拿到设备数组消息，新增独立表承载，不与当前 `raw_data` 混存
- 如果维持当前模型，至少尽快做分月表和保留周期

### 3. 原始数据分月表

目标：

- 避免 `raw_data` 单表长期膨胀
- 为每天数百万条数据做好归档准备

建议指标：

- 先做 `raw_data_yyyyMM`
- 后续再考虑 `event_yyyyMM`

### 4. 更长时间压测

目标：

- 在已完成 5 分钟压测基础上，继续验证 10 到 30 分钟持续写入
- 验证 Redis 分钟刷盘、离线巡检并行时的稳定性

建议：

- 74 req/s 左右持续压测
- 20 到 50 并发
- 观察错误率、主单数量、`merge_count`、`event_count`
- 观察 `minute:stat:*` 键数量和刷盘延迟

### 5. MQ 真实联调

目标：

- 用真实 broker 和真实 topic/payload 验证

重点检查：

- `dataReference` 是否与绑定表一致
- 现场设备命名规则是否稳定
- `Measure / Alarm` 两类消息是否完整覆盖

### 6. 查询层冷热分离

目标：

- 实时查 Redis
- 趋势查 `temp_stat_minute`
- 追溯才查 `raw_data`

### 7. 上线前配置核对

上线前至少确认以下配置和数据：

- `spring.datasource.*`
- `spring.redis.*`
- `shaft.mq.enabled`
- `shaft.mq.queue`
- `shaft.alarm.window-size`
- `shaft.alarm.event-throttle-seconds`
- `shaft.inspection.enabled`
- `shaft.inspection.fixed-delay-ms`
- `shaft.stat.flush-delay-ms`
- `alarm_rule` 表是否已初始化
- `monitor_partition_bind` 是否与现场 `dataReference` 一致

### 8. 文档与交接

目标：

- 保持 `docs` 只存最新交付物
- 后续只更新这 4 份文档，不再堆历史方案稿
