# generated-persistence/service（迁移副本）

这个目录是从根目录 `service` 复制出来的迁移版本，目标是给你移植时直接用。

## 已处理内容

- 保留了原 `service` 业务代码结构（controller / service / dao / vo / entity）。
- 在迁移副本中，`com.example.demo.service` 下的持久层引用已改为模板生成持久层：
  - `*Repository` 引用改为 `com.csg.dgri.szsiom.sysmanage.appservice.*AppService`
  - `*Entity` 类型改为 `com.csg.dgri.szsiom.sysmanage.model.*VO`
- 模板生成文件已放入副本：
  - `src/main/java/com/csg/dgri/szsiom/sysmanage/model`
  - `src/main/java/com/csg/dgri/szsiom/sysmanage/appservice`
  - `src/main/java/com/csg/dgri/szsiom/sysmanage/appservice/abs`
  - `src/main/resources/mybatis`

## 说明

- 根目录 `service` 源码未修改。
- 该目录用于“复制到目标平台工程做二次落地”，不保证在本仓库可直接编译运行（依赖目标平台基类和运行环境）。
