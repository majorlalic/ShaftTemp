#!/usr/bin/env python3
import os
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SERVICE_DAO_DIR = ROOT / "service/src/main/java/com/example/demo/dao"
SERVICE_ENTITY_DIR = ROOT / "service/src/main/java/com/example/demo/entity"
OUT_DIR = ROOT / "generated-persistence"

VO_PKG = "com.csg.dgri.szsiom.sysmanage.model"
ABS_PKG = "com.csg.dgri.szsiom.sysmanage.appservice.abs"
APP_PKG = "com.csg.dgri.szsiom.sysmanage.appservice"

TABLES = [
    {"name": "Alarm", "entity": "AlarmEntity", "table": "ODS_DWEQ_DM_ALARM_D", "repos": ["AlarmRepository"]},
    {"name": "AlarmRaw", "entity": "AlarmRawEntity", "table": "ODS_DWEQ_DM_ALARM_RAW_D", "repos": ["AlarmRawRepository"]},
    {"name": "AlarmRule", "entity": "AlarmRuleEntity", "table": "ODS_DWEQ_DM_ALARM_RULE_D", "repos": ["AlarmRuleRepository"]},
    {"name": "Area", "entity": "AreaEntity", "table": "ODS_DWEQ_DM_AREA_D", "repos": ["AreaRepository"]},
    {"name": "Device", "entity": "DeviceEntity", "table": "ODS_DWEQ_DM_DEVICE_D", "repos": ["DeviceRepository"]},
    {"name": "DeviceOnlineLog", "entity": "DeviceOnlineLogEntity", "table": "ODS_DWEQ_DM_DEVICE_ONLINE_LOG_D", "repos": ["DeviceOnlineLogRepository"]},
    {"name": "Event", "entity": "EventEntity", "table": "ODS_DWEQ_DM_EVENT_D", "repos": ["EventRepository"]},
    {"name": "Monitor", "entity": "MonitorEntity", "table": "ODS_DWEQ_DM_MONITOR_D", "repos": ["MonitorRepository"]},
    {"name": "MonitorDeviceBind", "entity": "MonitorDeviceBindEntity", "table": "ODS_DWEQ_DM_MONITOR_DEVICE_BIND_D", "repos": ["MonitorDeviceBindRepository"]},
    {"name": "MonitorPartitionBind", "entity": "MonitorPartitionBindEntity", "table": "ODS_DWEQ_DM_MONITOR_PARTITION_BIND_D", "repos": ["MonitorPartitionBindRepository"]},
    {"name": "Org", "entity": "OrgEntity", "table": "ODS_DWEQ_DM_ORG_D", "repos": ["OrgRepository"]},
    {"name": "RawData", "entity": "RawDataEntity", "table": "ODS_DWEQ_DM_RAW_DATA_D", "repos": ["RawDataRepository"]},
    {"name": "ShaftFloor", "entity": "ShaftFloorEntity", "table": "ODS_DWEQ_DM_SHAFT_FLOOR_D", "repos": ["ShaftFloorRepository"]},
]


def ensure_dirs():
    for sub in ["vo", "appservice-abs", "appservice", "sql"]:
        (OUT_DIR / sub).mkdir(parents=True, exist_ok=True)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write_text(path: Path, content: str):
    path.write_text(content, encoding="utf-8")


def extract_imports_and_body(entity_text: str):
    lines = entity_text.splitlines()
    imports = []
    class_idx = -1
    for i, line in enumerate(lines):
        s = line.strip()
        if s.startswith("import "):
            if "javax.persistence.Entity" in s:
                continue
            imports.append(s)
        if "public class " in line:
            class_idx = i
            break
    if class_idx < 0:
        raise RuntimeError("class declaration not found")
    # body between first { of class and final }
    body_start = class_idx + 1
    body_end = len(lines) - 1
    body = "\n".join(lines[body_start:body_end]).strip("\n")
    return imports, body


def build_vo(table_conf):
    entity_name = table_conf["entity"]
    vo_name = entity_name.replace("Entity", "VO")
    entity_path = SERVICE_ENTITY_DIR / f"{entity_name}.java"
    entity_text = read_text(entity_path)
    imports, body = extract_imports_and_body(entity_text)

    imports_set = list(dict.fromkeys(imports + ["import java.io.Serializable;"]))
    if not any("javax.persistence.Table" in x for x in imports_set):
        imports_set.append("import javax.persistence.Table;")

    head = [
        f"package {VO_PKG};",
        "",
        *imports_set,
        "",
        "/**",
        f" * {table_conf['table']} 对应 VO（自动生成）",
        " */",
        f'@Table(name = "{table_conf["table"]}")',
        f"public class {vo_name} extends CapBaseVO implements Serializable " + "{",
        "",
        "    private static final long serialVersionUID = 1L;",
        "",
        f"    public {vo_name}() " + "{",
        "    }",
        "",
    ]
    tail = ["}", ""]
    content = "\n".join(head) + body + "\n" + "\n".join(tail)
    write_text(OUT_DIR / "vo" / f"{vo_name}.java", content)


def extract_methods(repo_text: str):
    # (annotation_type, sql, method_signature)
    methods = []
    pattern = re.compile(r"@(Select|Insert|Update)\s*(\(\s*(?:\{.*?\}|\".*?\")\s*\))\s*(.*?)\s*;", re.S)
    for m in pattern.finditer(repo_text):
        ann_type = m.group(1).lower()
        ann_payload = m.group(2)
        signature = m.group(3).strip()
        sql_parts = re.findall(r"\"(.*?)\"", ann_payload, re.S)
        sql = "\n".join(p.strip() for p in sql_parts if p.strip())
        sql = sql.replace('\\"', '"')
        sql = re.sub(r"^\s*<script>\s*$", "", sql, flags=re.M)
        sql = re.sub(r"^\s*</script>\s*$", "", sql, flags=re.M)
        sql = "\n".join([line for line in sql.splitlines() if line.strip() != ""])
        methods.append((ann_type, sql, signature))
    return methods


def parse_signature(signature: str):
    signature = re.sub(r"\s+", " ", signature).strip()
    m = re.match(r"([A-Za-z0-9_<>, ?\.]+)\s+([A-Za-z0-9_]+)\((.*)\)$", signature)
    if not m:
        raise RuntimeError(f"cannot parse signature: {signature}")
    return_type = m.group(1).strip()
    method_name = m.group(2).strip()
    params = m.group(3).strip()
    return return_type, method_name, params


def split_params(param_text: str):
    if not param_text:
        return []
    parts = []
    cur = []
    depth = 0
    for ch in param_text:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth = max(depth - 1, 0)
        if ch == "," and depth == 0:
            parts.append("".join(cur).strip())
            cur = []
        else:
            cur.append(ch)
    if cur:
        parts.append("".join(cur).strip())
    return parts


def parse_param(param_decl: str):
    # returns: decl, var_name, map_key
    decl = re.sub(r"\s+", " ", param_decl).strip()
    ann = re.search(r'@Param\("([^"]+)"\)\s*([A-Za-z0-9_<>\[\]\.]+)\s+([A-Za-z0-9_]+)', decl)
    if ann:
        key = ann.group(1)
        type_name = ann.group(2)
        var_name = ann.group(3)
        return f"{type_name} {var_name}", var_name, key
    m = re.search(r"([A-Za-z0-9_<>\[\]\.]+)\s+([A-Za-z0-9_]+)$", decl)
    if not m:
        raise RuntimeError(f"cannot parse param: {param_decl}")
    type_name = m.group(1)
    var_name = m.group(2)
    return f"{type_name} {var_name}", var_name, var_name


def to_vo_type(type_name: str):
    return type_name.replace("Entity", "VO")


def fq_vo(name: str):
    return f"{VO_PKG}.{name}"


def build_app_and_xml(table_conf, methods):
    name = table_conf["name"]
    vo_name = f"{name}VO"
    abs_name = f"Abstract{name}AppService"
    app_name = f"{name}AppService"
    namespace = fq_vo(vo_name)

    # extra: raw_data query method from RawDataQueryRepository (JDBC -> Mapper SQL), DM syntax
    if name == "RawData":
        methods.append((
            "select",
            "\n".join([
                "select * from (",
                "select id, device_id, iot_code, topic, partition_id, monitor_id, shaft_floor_id, data_reference, ied_full_path,",
                "collect_time, max_temp, min_temp, avg_temp, max_temp_position, min_temp_position, max_temp_channel, min_temp_channel,",
                "payload_json, deleted, created_on",
                "from ODS_DWEQ_DM_RAW_DATA_D",
                "where (deleted is null or deleted = 0)",
                "and collect_time >= #{from} and collect_time <= #{to}",
                "and (#{monitorId} is null or monitor_id = #{monitorId})",
                "and (#{deviceId} is null or device_id = #{deviceId})",
                "and (#{shaftFloorId} is null or shaft_floor_id = #{shaftFloorId})",
                "and (#{partitionId} is null or partition_id = #{partitionId})",
                "order by collect_time desc, id desc",
                ") t",
                "where rownum &lt;= #{limit}",
            ]),
            "List<RawDataEntity> query(Long monitorId, Long deviceId, Long shaftFloorId, Integer partitionId, java.time.LocalDateTime from, java.time.LocalDateTime to, int limit)"
        ))

    # Abstract AppService
    abs_content = "\n".join([
        f"package {ABS_PKG};",
        "",
        f"import {VO_PKG}.{vo_name};",
        "",
        "/**",
        f" * {table_conf['table']} 抽象业务类（自动生成）",
        " */",
        f"public abstract class {abs_name}<T extends {vo_name}> extends CapBaseAppService<T> " + "{",
        "}",
        "",
    ])
    write_text(OUT_DIR / "appservice-abs" / f"{abs_name}.java", abs_content)

    app_lines = [
        f"package {APP_PKG};",
        "",
        "import java.util.HashMap;",
        "import java.util.List;",
        "import java.util.Map;",
        "import java.util.Optional;",
        "",
        "import org.springframework.stereotype.Service;",
        "",
        f"import {ABS_PKG}.{abs_name};",
        f"import {VO_PKG}.{vo_name};",
        "",
        "/**",
        f" * {table_conf['table']} 业务类（自动生成）",
        " */",
        f'@Service(value = "{name[0].lower() + name[1:]}AppService")',
        f"public class {app_name}<T extends {vo_name}> extends {abs_name}<{vo_name}> " + "{",
        "",
        f'    private static final String NS = "{namespace}.";',
        "",
    ]

    xml_lines = [
        '<?xml version="1.0" encoding="UTF-8"?>',
        '<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">',
        f'<mapper namespace="{namespace}">',
        "",
    ]

    for ann_type, sql, signature in methods:
        ret, method_name, params_txt = parse_signature(signature)
        ret_vo = to_vo_type(ret)
        raw_params = split_params(params_txt)
        parsed_params = [parse_param(p) for p in raw_params]
        conv_params = []
        for d, var_name, map_key in parsed_params:
            d_conv = to_vo_type(d)
            type_name_conv = d_conv.split()[0]
            conv_params.append((d_conv, type_name_conv, var_name, map_key))
        param_decl_list = [x[0] for x in conv_params]
        method_param_decl = ", ".join(param_decl_list)
        single_vo_param = len(conv_params) == 1 and conv_params[0][1].endswith("VO")

        # Build parameter object for DAO call
        if len(conv_params) == 0:
            call_param = "null"
        elif single_vo_param:
            call_param = conv_params[0][2]
        else:
            app_lines.append("    @SuppressWarnings(\"unchecked\")")
            call_param = "params"

        app_lines.append(f"    public {ret_vo} {method_name}({method_param_decl}) " + "{")
        if len(conv_params) > 1 or (len(conv_params) == 1 and not single_vo_param):
            app_lines.append("        Map<String, Object> params = new HashMap<String, Object>();")
            for _, _, var_name, map_key in conv_params:
                app_lines.append(f'        params.put("{map_key}", {var_name});')

        if ann_type == "select":
            if ret_vo.startswith("List<"):
                app_lines.append(f'        return this.getCapBaseCommonDAO().queryList(NS + "{method_name}", {call_param});')
            elif ret_vo.startswith("Optional<"):
                inner = re.sub(r"Optional<(.+)>", r"\1", ret_vo)
                app_lines.append(f"        {inner} one = ({inner}) this.getCapBaseCommonDAO().selectOne(NS + \"{method_name}\", {call_param});")
                app_lines.append("        return Optional.ofNullable(one);")
            else:
                app_lines.append(f"        return ({ret_vo}) this.getCapBaseCommonDAO().selectOne(NS + \"{method_name}\", {call_param});")
        else:
            app_lines.append(f'        return this.getCapBaseCommonDAO().update(NS + "{method_name}", {call_param});')
        app_lines.append("    }")
        app_lines.append("")

        # XML statement
        stmt_tag = ann_type
        parameter_type = "map"
        if single_vo_param:
            parameter_type = fq_vo(conv_params[0][1])
        if len(conv_params) == 0:
            parameter_type = "map"

        result_type = None
        if ann_type == "select":
            if ret_vo.startswith("List<"):
                inner = re.sub(r"List<(.+)>", r"\1", ret_vo)
                result_type = fq_vo(inner) if inner.endswith("VO") else f"java.lang.{inner}"
            elif ret_vo.startswith("Optional<"):
                inner = re.sub(r"Optional<(.+)>", r"\1", ret_vo)
                result_type = fq_vo(inner) if inner.endswith("VO") else f"java.lang.{inner}"
            else:
                if ret_vo.endswith("VO"):
                    result_type = fq_vo(ret_vo)
                elif ret_vo in ("int", "Integer", "Long", "String"):
                    result_type = "java.lang.Integer" if ret_vo in ("int", "Integer") else (
                        "java.lang.Long" if ret_vo == "Long" else "java.lang.String"
                    )
                else:
                    result_type = "java.util.Map"

        if result_type:
            xml_lines.append(f'    <{stmt_tag} id="{method_name}" parameterType="{parameter_type}" resultType="{result_type}">')
        else:
            xml_lines.append(f'    <{stmt_tag} id="{method_name}" parameterType="{parameter_type}">')
        xml_lines.append("        <![CDATA[")
        for ln in sql.splitlines():
            xml_lines.append(f"        {ln}")
        xml_lines.append("        ]]>")
        xml_lines.append(f"    </{stmt_tag}>")
        xml_lines.append("")

    app_lines.append("}")
    app_lines.append("")
    xml_lines.append("</mapper>")
    xml_lines.append("")

    write_text(OUT_DIR / "appservice" / f"{app_name}.java", "\n".join(app_lines))
    write_text(OUT_DIR / "sql" / f"{vo_name}SQL.xml", "\n".join(xml_lines))


def collect_repo_methods(repo_name: str):
    repo_path = SERVICE_DAO_DIR / f"{repo_name}.java"
    if not repo_path.exists():
        return []
    txt = read_text(repo_path)
    methods = extract_methods(txt)
    return methods


def main():
    ensure_dirs()
    for t in TABLES:
        build_vo(t)
        all_methods = []
        for repo in t["repos"]:
            all_methods.extend(collect_repo_methods(repo))
        build_app_and_xml(t, all_methods)

    readme = "\n".join([
        "# generated-persistence",
        "",
        "自动生成目录（用于复制到目标平台工程）：",
        "",
        "- vo: *VO",
        "- appservice-abs: Abstract*AppService",
        "- appservice: *AppService",
        "- sql: *SQL.xml",
        "",
        "说明：",
        "- 基于当前 service/dao 与 service/entity 自动生成。",
        "- SQL 已按达梦语法口径保留/转换（例如 RawData query 使用 rownum）。",
        "- 该目录文件不参与当前工程运行，仅作为迁移素材。",
        "",
    ])
    write_text(OUT_DIR / "README.md", readme)


if __name__ == "__main__":
    main()
