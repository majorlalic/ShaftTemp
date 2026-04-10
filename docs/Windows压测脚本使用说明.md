# Windows 测试脚本说明

本文档包含两类脚本：
- `scripts/windows_pressure.ps1`：纯压测（单接口或混合接口）
- `scripts/windows_full_e2e_test.ps1`：全链路功能+压测（带阶段断言、进度、错误打印）

## 1. 全链路脚本（推荐先跑）

脚本：`scripts/windows_full_e2e_test.ps1`

用途：
- 模拟实时测温上报、告警上报
- 触发定温/差温/升温速率规则
- 验证告警合并（`mergeCount`）
- 验证离线巡检告警（可跳过）
- 执行并发压测并持续打印进度

### 1.1 参数

| 参数 | 默认值 | 说明 |
|---|---:|---|
| `BaseUrl` | `http://localhost:8090/api` | 服务地址 |
| `IotCode` | `shaft-dev-001` | 设备编码 |
| `PartitionCount` | `50` | 分区数量（`th01...thXX`） |
| `PressureTotal` | `3000` | 压测总请求数 |
| `PressureConcurrency` | `30` | 每批并发 job 数 |
| `HttpTimeoutSec` | `10` | 单请求超时（秒） |
| `OfflineWaitSec` | `75` | 离线告警等待秒数 |
| `SkipOfflineTest` | `false` | 跳过离线巡检测试 |
| `MaxErrorPrint` | `30` | 最多打印失败明细条数 |

### 1.2 运行命令

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows_full_e2e_test.ps1 `
  -BaseUrl "http://localhost:8090/api" `
  -IotCode "shaft-dev-001" `
  -PartitionCount 50 `
  -PressureTotal 3000 `
  -PressureConcurrency 30 `
  -HttpTimeoutSec 10 `
  -OfflineWaitSec 75 `
  -MaxErrorPrint 30
```

跳过离线阶段：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows_full_e2e_test.ps1 -SkipOfflineTest
```

### 1.3 阶段说明

1. baseline measure  
2. threshold trigger  
3. temp diff trigger  
4. rise rate trigger  
5. alarm raw push  
6. merge verify  
7. offline check（可跳过）  
8. pressure test  
9. filter query checks

### 1.4 输出日志说明

- `[PASS]/[FAIL]`：每个阶段断言结果
- `[HEARTBEAT]`：压测批次心跳（防止无输出）
- `[PRESSURE_PROGRESS]`：批次进度、成功率、QPS
- `[PRESSURE_FAIL]`：失败请求详情（截断输出）
- `[API_ERROR]/[API_FAIL]`：接口调用异常/业务失败
- `SUMMARY` + `[RESULT] PASS/FAIL`：最终结果（退出码 `0/1`）

## 2. 纯压测脚本

脚本：`scripts/windows_pressure.ps1`

用途：
- 对 `POST /shaft/iot/reports/measure`、`POST /shaft/iot/reports/alarm` 做并发压测
- 不依赖 MQ，不需要额外工具

### 2.1 参数（核心）

| 参数 | 默认值 | 说明 |
|---|---:|---|
| `BaseUrl` | `http://127.0.0.1:8080` | 服务地址 |
| `Mode` | `measure` | `measure` / `alarm` / `mix` |
| `Total` | `5000` | 总请求数 |
| `Concurrency` | `50` | 并发数 |
| `PartitionCount` | `50` | 分区数 |
| `IotCode` | `shaft-dev-001` | 设备编码 |
| `TimeoutSec` | `10` | 单请求超时 |
| `MixMeasurePercent` | `90` | `mix` 模式下 measure 占比 |
| `MaxErrorPrint` | `20` | 最大错误打印条数 |

### 2.2 示例

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows_pressure.ps1 `
  -BaseUrl "http://localhost:8090/api" `
  -Mode measure `
  -Total 20000 `
  -Concurrency 100 `
  -PartitionCount 50 `
  -IotCode "shaft-dev-001"
```

## 3. 执行前检查

- 服务已启动并可访问 `BaseUrl`
- 主数据完整（`device`、`monitor`、`shaft_floor`、`monitor_device_bind` 有正确映射）
- 设备分区编码与绑定一致，否则会出现：
  - `partition binding not found for partitionCode=...`

## 4. 常见报错排查

- `partition binding not found`
  - 检查 `iotCode + thXX` 对应的分区是否有绑定记录
- 请求全 200 但数据库没数据
  - 检查是否打到了错误环境（`BaseUrl` 不一致）
  - 检查数据库连接配置是否指向当前实例
- 压测输出太少或看起来“卡住”
  - 全链路脚本依赖 `[HEARTBEAT]` 与 `[PRESSURE_PROGRESS]`，如果都不输出，优先检查 PowerShell 执行策略和脚本是否为最新版本

## 5. 数据核对建议（达梦示例）

```sql
select count(*) from ODS_DWEQ_DM_RAW_DATA_D where created_on >= sysdate - 10/1440;
select count(*) from ODS_DWEQ_DM_ALARM_RAW_D where created_on >= sysdate - 10/1440;
select count(*) from ODS_DWEQ_DM_ALARM_D where created_on >= sysdate - 10/1440;
select count(*) from ODS_DWEQ_DM_EVENT_D where created_on >= sysdate - 10/1440;
```
