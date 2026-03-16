# AGENTS.md

## Goal
实现测温物联网系统最小可用版本，重点是：
- 实时数据接收
- Redis 实时状态缓存
- 原始数据落库
- 告警生成与合并
- 离线巡检

## Source of truth
先阅读以下文件，再开始编码：
- ./docs/测温物联网系统最终建表.sql
- ./docs/测温物联网系统数据库设计_最终版.docx
- ./docs/测温物联网系统设计说明书.docx

如果 docx 不方便读取，请至少参考同目录下的 markdown 或 txt 摘要。

## Constraints
- 这是定制项目，优先快速落地，不做过度设计
- 仅使用 MySQL + Redis
- 不引入额外中间件
- 保持实现简单、可排查、便于上线

## Confirmed design
- MySQL 负责持久化和查询
- Redis 负责实时状态、最近窗口、活跃告警
- 原始数据按一次上报一行存储
- 告警模型采用 alarm + event
- 离线告警由定时任务巡检生成

## Final table names
area
org
device
monitor
shaft_floor
monitor_device_bind
alarm
event
raw_data
temp_stat_minute
device_online_log

## Important schema rules
- area.type
- org.type
- area / org 无 level_no、code
- sort_no 统一为 sort
- 通用删除字段为 deleted
- 通用时间字段为 created_on / updated_on
- alarm、event、raw_data、temp_stat_minute、device_online_log
  不使用 creator、updator、deletor、deleted_on

## Working style
先做代码库扫描和实现计划，不要一开始大改。
优先复用现有目录结构和基础设施。
每完成一步都说明：
1. 改了哪些文件
2. 为什么这么改
3. 如何验证

## Output expectation
优先完成：
1. 消息消费入口
2. Redis 状态更新
3. raw_data 落库
4. alarm / event 生成
5. 离线巡检任务

## Commands
请根据项目实际情况自行发现并使用：
- 安装依赖命令
- 启动命令
- 测试命令
- lint / type check 命令