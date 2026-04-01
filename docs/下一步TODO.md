# 下一步 TODO

### 1. 原始数据方案收敛（设备数组预案）

目标：

- 明确当前分区版 `raw_data` 的长期策略
- 为未来设备数组消息预留独立原始表方案

建议：

- 当前继续保留分区版 `raw_data`
- 后续如拿到设备数组消息，新增独立表承载，不与当前 `raw_data` 混存
- 若后续接入“设备数组消息”，新增独立原始表承载，不改动现有 `raw_data` 链路

### 3. 更长时间压测

目标：

- 在已完成 5 分钟压测基础上，继续验证 10 到 30 分钟持续写入
- 验证离线巡检并行时的稳定性

建议：

- 74 req/s 左右持续压测
- 20 到 50 并发
- 观察错误率、主单数量、`merge_count`、`event_count`

### 4. MQ 真实联调

目标：

- 用真实 broker 和真实 topic/payload 验证

重点检查：

- `dataReference` 是否与绑定表一致
- 现场设备命名规则是否稳定
- `Measure / Alarm` 两类消息是否完整覆盖
- MQTT broker 地址、用户名、密码、topic 是否与现场一致

### 5. 查询层优化

目标：

- 实时查 Redis
- 历史追溯查 `raw_data`

### 6. 上线前配置核对

上线前至少确认以下配置和数据：

- `spring.datasource.*`
- `spring.redis.*`
- `shaft.mq.enabled`
- `shaft.mq.broker-url`
- `shaft.mq.client-id`
- `shaft.mq.topics`
- `shaft.alarm.window-size`
- `shaft.alarm.event-throttle-seconds`
- `shaft.inspection.enabled`
- `shaft.inspection.fixed-delay-ms`
- `alarm_rule` 表是否已初始化
- `monitor_partition_bind` 是否与现场 `dataReference` 一致

### 7. 文档与交接

目标：

- 保持 `docs` 只存最新交付物
- 后续只更新这 4 份文档，不再堆历史方案稿
