# Windows 压测脚本使用说明

脚本路径：`scripts/windows_pressure.ps1`  
用途：对 `service` 的 HTTP 接口做并发压测（不依赖 MQ，不安装额外工具）。

## 1. 支持的接口

- 实时上报：`POST /shaft/iot/reports/measure`
- 告警上报：`POST /shaft/iot/reports/alarm`

## 2. 参数说明

| 参数 | 默认值 | 说明 |
|---|---:|---|
| `BaseUrl` | `http://127.0.0.1:8080` | 服务地址 |
| `Mode` | `measure` | 压测模式：`measure` / `alarm` / `mix` |
| `Total` | `5000` | 总请求数 |
| `Concurrency` | `50` | 并发数（每批并发 job 数） |
| `PartitionCount` | `50` | 分区数量（按 `th01...thXX` 轮询） |
| `IotCode` | `shaft-dev-01` | 设备编码 |
| `MaxTemp` | `82.0` | 测温请求字段（仅 measure） |
| `MinTemp` | `75.0` | 测温请求字段（仅 measure） |
| `AvgTemp` | `78.0` | 测温请求字段（仅 measure） |
| `TimeoutSec` | `10` | 单请求超时时间（秒） |
| `MixMeasurePercent` | `90` | `mix` 模式下 measure 占比（0-100） |
| `MaxErrorPrint` | `20` | 最多打印多少条失败明细（防止刷屏） |

## 3. 运行方式

在 Windows PowerShell 执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows_pressure.ps1
```

### 3.1 仅测全链路实时上报（推荐先跑）

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows_pressure.ps1 `
  -BaseUrl "http://127.0.0.1:8080" `
  -Mode measure `
  -Total 20000 `
  -Concurrency 100 `
  -PartitionCount 50 `
  -IotCode "shaft-dev-01"
```

### 3.2 仅测告警上报

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows_pressure.ps1 `
  -Mode alarm `
  -Total 5000 `
  -Concurrency 50
```

### 3.3 混合压测（measure + alarm）

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\windows_pressure.ps1 `
  -Mode mix `
  -Total 30000 `
  -Concurrency 100 `
  -MixMeasurePercent 85
```

## 4. 输出说明

脚本会输出：
- 每批进度：`[PROGRESS] batch=x/y done=a/b(%) ok=n fail=m successRate=% batchCostMs=... qps=...`
- 最终统计：
  - `total`
  - `ok`
  - `fail`
  - `sentMeasure`
  - `sentAlarm`
  - `approxQps`
- 失败明细（最多 `MaxErrorPrint` 条）：
  - `[ERROR] idx=... mode=... part=... status=... elapsedMs=... msg=...`
- 结束态：
  - 全成功打印 `[RESULT] PASS`
  - 有失败打印 `[RESULT] FAIL`

## 5. 压测后建议校验

在数据库中核对数据增量（表名按你当前库实际为准）：

```sql
select count(*) from ODS_DWEQ_DM_RAW_DATA_D where created_on >= now() - interval 10 minute;
select count(*) from ODS_DWEQ_DM_ALARM_RAW_D where created_on >= now() - interval 10 minute;
select count(*) from ODS_DWEQ_DM_ALARM_D where created_on >= now() - interval 10 minute;
select count(*) from ODS_DWEQ_DM_EVENT_D where created_on >= now() - interval 10 minute;
```

## 6. 注意事项

- `measure` 模式才会走完整实时链路（Redis、规则判断、alarm/event）。
- `alarm` 模式只压告警上报入口，不代表实时测温链路性能。
- 如果失败率高，先下调 `Concurrency` 再观察。
