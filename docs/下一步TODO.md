# 下一步 TODO

## P0 已完成

- 分区 MQ 模型接入
- Redis 实时状态
- `raw_data` 落库
- 告警主单与事件
- 离线巡检
- 告警状态机
- `merge_key` 待确认唯一性
- `event_count` 与 `merge_count` 拆分
- 建库脚本、初始化脚本整理

## 下一阶段建议

### 1. 原始数据分月表

目标：

- 避免 `raw_data` 单表长期膨胀
- 为每天数百万条数据做好归档准备

建议：

- 先做 `raw_data_yyyyMM`
- 后续再考虑 `event_yyyyMM`

### 2. 长时间压测

目标：

- 验证 10 分钟以上持续写入
- 验证 Redis 分钟刷盘、离线巡检并行时的稳定性

建议指标：

- 5000 到 20000 条连续 `Measure`
- 20 到 50 并发
- 观察错误率、主单数量、`merge_count`、`event_count`

### 3. 规则配置化

目标：

- 当前阈值从代码硬编码迁到配置或规则表

建议：

- 先做数据库规则表
- 再补最小 CRUD 接口

### 4. MQ 真实联调

目标：

- 用真实 broker 和真实 topic/payload 验证

重点检查：

- `dataReference` 是否与绑定表一致
- 现场设备命名规则是否稳定
- `Measure / Alarm` 两类消息是否完整覆盖

### 5. 查询层冷热分离

目标：

- 实时查 Redis
- 趋势查 `temp_stat_minute`
- 追溯才查 `raw_data`

这样可以避免后续页面直接压 `raw_data`

### 6. 文档与交接

目标：

- 保持 `docs` 只存最新交付物
- 后续只更新这 4 份文档，不再堆历史方案稿
