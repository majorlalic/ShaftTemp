# generated-persistence

自动生成目录（用于复制到目标平台工程）：

- vo: *VO
- appservice-abs: Abstract*AppService
- appservice: *AppService
- sql: *SQL.xml

说明：
- 基于当前 service/dao 与 service/entity 自动生成。
- SQL 已按达梦语法口径保留/转换（例如 RawData query 使用 rownum）。
- 该目录文件不参与当前工程运行，仅作为迁移素材。
