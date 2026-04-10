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

### 1.4 每一步的作用与判定

#### 第 1 步 baseline measure
- 作用：发送一条正常温度测温数据，验证实时入口可用。
- 成功代表：`/shaft/iot/reports/measure` 可正常接收并返回业务成功。
- 失败代表：实时入口不可用或请求体/主数据映射存在问题。

#### 第 2 步 threshold trigger
- 作用：发送高温数据，触发定温告警规则。
- 成功代表：高温消息已被接收，后续可用于生成/合并告警。
- 失败代表：高温消息未被服务接受，或请求参数不符合接口要求。

#### 第 3 步 temp diff trigger
- 作用：发送较大温差数据，触发差温规则。
- 成功代表：差温触发数据已进入链路。
- 失败代表：接口不可用或数据字段异常。

#### 第 4 步 rise rate trigger
- 作用：先发低值再发高值，构造升温速率场景。
- 成功代表：两条数据均接收成功，可触发速率规则判定。
- 失败代表：其中任一请求失败，速率场景无效。

#### 第 5 步 alarm raw push
- 作用：调用 `/shaft/iot/reports/alarm`，验证告警原始上报入口。
- 成功代表：原始告警报文可入库（`alarm_raw` 路径）。
- 失败代表：告警上报入口不可用或请求字段异常。

#### 第 6 步 merge verify
- 作用：连续推送同类型高温，检查是否合并到同一待确认告警。
- 成功代表：查询列表中存在 `TEMP_THRESHOLD` 且 `mergeCount >= 2`。
- 失败代表：未出现合并结果，可能是规则未命中、状态不在可合并范围、或历史数据分页干扰导致未查到目标记录。

#### 第 7 步 offline check（可跳过）
- 作用：等待巡检任务执行，检查是否生成 `DEVICE_OFFLINE` 告警。
- 成功代表：离线巡检任务生效，且告警可查询到。
- 失败代表：巡检未执行、等待时间不足，或离线阈值/设备最近上报时间不满足触发条件。
- 脚本行为：会优先读取 `DEVICE_OFFLINE` 规则阈值，并将等待时间自动提升到 `max(输入等待秒数, 规则阈值+60秒)`，降低误判概率。

#### 第 8 步 pressure test
- 作用：并发压测测温入口，观察吞吐、成功率、失败明细。
- 成功代表：`pressureFail=0`，压测请求全部成功。
- 失败代表：存在失败请求（会打印 `[PRESSURE_FAIL]` 便于排查）。

#### 第 9 步 filter query checks
- 作用：验证告警分页查询筛选参数可用（`status`、`alarmTypeBig`、`areaName`）。
- 成功代表：筛选查询接口可正常返回。
- 失败代表：查询接口异常、参数映射不一致或服务端 SQL/分页逻辑异常。

### 1.5 输出日志说明

- `[PASS]/[FAIL]`：每个阶段断言结果
- `[HEARTBEAT]`：压测批次心跳（防止无输出）
- `[PRESSURE_PROGRESS]`：批次进度、成功率、QPS
- `[PRESSURE_FAIL]`：失败请求详情（截断输出）
- `[API_ERROR]/[API_FAIL]`：接口调用异常/业务失败
- `[DEBUG] merge retry=... thresholdCount=... maxMergeCount=...`：第 6 步合并判定细节
- `[DEBUG] offline retry=... offlineAlarmCount=...`：第 7 步离线判定细节
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
- `无效的 URI: 未能分析主机名`
  - 检查启动时打印的 `[ENDPOINT] ...` 是否正确
  - 检查请求时打印的 `[HTTP] ...` / `[QUERY_ALARM_LIST] ...` 是否包含错误字符
  - 运行命令中 `-BaseUrl` 不要再包一层引号字符，例如使用：
    - 正确：`-BaseUrl http://localhost:8090/api`
    - 避免：`-BaseUrl '"http://localhost:8090/api"'`
- 请求全 200 但数据库没数据
  - 检查是否打到了错误环境（`BaseUrl` 不一致）
  - 检查数据库连接配置是否指向当前实例
- 第 6 步 `merge_check` 失败但数据库里有合并数据
  - 可能被历史数据和分页干扰，优先查看脚本的 `[DEBUG] merge retry=...]` 输出
  - 若 `maxMergeCount` 已大于等于 2，可判定合并逻辑已生效
- 压测输出太少或看起来“卡住”
  - 全链路脚本依赖 `[HEARTBEAT]` 与 `[PRESSURE_PROGRESS]`，如果都不输出，优先检查 PowerShell 执行策略和脚本是否为最新版本

## 5. 数据核对建议（达梦示例）

```sql
select count(*) from ODS_DWEQ_DM_RAW_DATA_D where created_on >= sysdate - 10/1440;
select count(*) from ODS_DWEQ_DM_ALARM_RAW_D where created_on >= sysdate - 10/1440;
select count(*) from ODS_DWEQ_DM_ALARM_D where created_on >= sysdate - 10/1440;
select count(*) from ODS_DWEQ_DM_EVENT_D where created_on >= sysdate - 10/1440;
```
