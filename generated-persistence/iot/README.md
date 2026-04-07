# generated-persistence/iot（迁移副本）

该目录是 `iot` 模块的独立迁移版本，不依赖根目录 `service` 模块。

## 已处理

- 从 `iot` 模块复制源码到本目录。
- 移除 `pom.xml` 中对 `shaft-temp-service` 的依赖。
- 本地补齐 `iot` 运行所需的共享类：
  - `com.example.demo.config.AppProperties`
  - `com.example.demo.service.DeviceResolverService`
  - `com.example.demo.service.IdGenerator`
  - `com.example.demo.vo.PartitionMeasureRequest`
  - `com.example.demo.vo.PartitionAlarmRequest`
- `IotRawPersistService` 已改为直接使用模板持久层：
  - `RawDataAppService + RawDataVO`
  - `AlarmRawAppService + AlarmRawVO`
- 模板持久层与 DM SQL 已内置：
  - `src/main/java/com/csg/dgri/szsiom/sysmanage/model`
  - `src/main/java/com/csg/dgri/szsiom/sysmanage/appservice`
  - `src/main/java/com/csg/dgri/szsiom/sysmanage/appservice/abs`
  - `src/main/resources/mybatis`
- `application.yml` 默认改为达梦连接配置（可按目标环境调整）。

## 说明

- 该目录用于迁移到目标平台工程，默认不保证在当前仓库可编译运行（平台基类如 `CapBaseVO/CapBaseAppService` 需目标平台提供）。
- 根目录 `iot` 与 `service` 源码均未改动。
