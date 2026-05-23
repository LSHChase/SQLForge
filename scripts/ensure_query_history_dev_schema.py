#!/usr/bin/env python3
"""当本地开发 query_history 表仍是旧启动 schema 时执行升级。"""

from __future__ import annotations

import os
import subprocess
import sys


MYSQL_CONTAINER = os.environ.get("SQLFORGE_DEV_MYSQL_CONTAINER", "sqlforge-mysql")
MYSQL_DATABASE = os.environ.get("SQLFORGE_DEV_MYSQL_DATABASE", "sqlforge")
MYSQL_USER = os.environ.get("SQLFORGE_DEV_MYSQL_USER", "sqlforge")
MYSQL_PASSWORD = os.environ.get("SQLFORGE_DEV_MYSQL_PASSWORD", "sqlforge")


LEGACY_COLUMN_RENAMES = [
    (
        "rewrite_publish_status_snapshot",
        "rewrite_activation_status_snapshot",
        "VARCHAR(32) DEFAULT NULL COMMENT '根据后端运行时证据生成的激活状态快照'",
    ),
]

COLUMN_DEFINITIONS = [
    (
        "sql_template_cipher",
        "ADD COLUMN sql_template_cipher MEDIUMBLOB DEFAULT NULL COMMENT '参数化查询的加密模板 SQL 载荷' AFTER sql_text_cipher",
    ),
    (
        "bound_sql_text_cipher",
        "ADD COLUMN bound_sql_text_cipher MEDIUMBLOB DEFAULT NULL COMMENT '参数绑定后的加密绑定 SQL 载荷' AFTER sql_template_cipher",
    ),
    (
        "datasource_code",
        "ADD COLUMN datasource_code VARCHAR(128) DEFAULT NULL COMMENT '数据源业务标识符或别名' AFTER bound_sql_text_cipher",
    ),
    (
        "report_code",
        "ADD COLUMN report_code VARCHAR(128) DEFAULT NULL COMMENT '从 SQL 注释上下文解析出的报表编码' AFTER datasource_type",
    ),
    (
        "stage_code",
        "ADD COLUMN stage_code VARCHAR(32) DEFAULT NULL COMMENT '从 SQL 注释上下文解析出的执行阶段' AFTER report_code",
    ),
    (
        "biz_date",
        "ADD COLUMN biz_date DATE DEFAULT NULL COMMENT '从 SQL 注释上下文解析出的执行日期' AFTER stage_code",
    ),
    (
        "query_date_start",
        "ADD COLUMN query_date_start DATE DEFAULT NULL COMMENT '从 SQL 正文解析出的查询日期下界' AFTER biz_date",
    ),
    (
        "query_date_end",
        "ADD COLUMN query_date_end DATE DEFAULT NULL COMMENT '从 SQL 正文解析出的查询日期上界' AFTER query_date_start",
    ),
    (
        "query_date_status",
        "ADD COLUMN query_date_status VARCHAR(32) DEFAULT NULL COMMENT '查询日期提取状态，例如 RESOLVED/UNRESOLVED/PARTIAL' AFTER query_date_end",
    ),
    (
        "access_channel",
        "ADD COLUMN access_channel VARCHAR(32) DEFAULT NULL COMMENT '访问渠道，例如 PAGE/API/JDBC_AGENT/SDK/CLIENT' AFTER query_date_status",
    ),
    (
        "parameterized_sql_flag",
        "ADD COLUMN parameterized_sql_flag TINYINT(1) DEFAULT NULL COMMENT '绑定前 SQL 是否已参数化' AFTER access_channel",
    ),
    (
        "binding_mode",
        "ADD COLUMN binding_mode VARCHAR(16) DEFAULT NULL COMMENT 'Binding 模式，例如 POSITIONAL/NAMED' AFTER parameterized_sql_flag",
    ),
    (
        "binding_render_status",
        "ADD COLUMN binding_render_status VARCHAR(16) DEFAULT NULL COMMENT '绑定渲染状态，例如 SUCCESS/PARTIAL/FAILED/MASKED' AFTER binding_mode",
    ),
    (
        "sql_template_fingerprint",
        "ADD COLUMN sql_template_fingerprint CHAR(32) DEFAULT NULL COMMENT '绑定前模板 SQL 指纹' AFTER binding_render_status",
    ),
    (
        "bound_sql_fingerprint",
        "ADD COLUMN bound_sql_fingerprint CHAR(32) DEFAULT NULL COMMENT '绑定后 SQL 指纹' AFTER sql_template_fingerprint",
    ),
    (
        "comment_context",
        "ADD COLUMN comment_context JSON DEFAULT NULL COMMENT '结构化 SQL 注释上下文 JSON' AFTER saga_id",
    ),
    (
        "binding_summary",
        "ADD COLUMN binding_summary JSON DEFAULT NULL COMMENT '结构化参数绑定摘要 JSON' AFTER comment_context",
    ),
    (
        "rewrite_record_id",
        "ADD COLUMN rewrite_record_id VARCHAR(64) DEFAULT NULL COMMENT '本次执行已应用或评估的 SQL 改写记录标识符' AFTER binding_summary",
    ),
    (
        "runtime_binding_id",
        "ADD COLUMN runtime_binding_id VARCHAR(64) DEFAULT NULL COMMENT 'query-execution 使用的运行时改写绑定标识符' AFTER rewrite_record_id",
    ),
    (
        "rewrite_rule_version",
        "ADD COLUMN rewrite_rule_version BIGINT DEFAULT NULL COMMENT '数值型运行时改写规则版本快照' AFTER runtime_binding_id",
    ),
    (
        "runtime_rule_version",
        "ADD COLUMN runtime_rule_version VARCHAR(64) DEFAULT NULL COMMENT '运行时改写规则版本标签快照' AFTER rewrite_rule_version",
    ),
    (
        "runtime_rewrite_status",
        "ADD COLUMN runtime_rewrite_status VARCHAR(32) DEFAULT NULL COMMENT '运行时改写绑定解析状态快照' AFTER runtime_rule_version",
    ),
    (
        "rewrite_activation_status_snapshot",
        "ADD COLUMN rewrite_activation_status_snapshot VARCHAR(32) DEFAULT NULL COMMENT '根据后端运行时证据生成的激活状态快照' AFTER runtime_rewrite_status",
    ),
    (
        "rewrite_fallback_reason",
        "ADD COLUMN rewrite_fallback_reason VARCHAR(128) DEFAULT NULL COMMENT '自动改写回退到原始 SQL 的原因' AFTER rewrite_activation_status_snapshot",
    ),
    (
        "logical_object_hits",
        "ADD COLUMN logical_object_hits JSON DEFAULT NULL COMMENT '结构化逻辑对象与表命中摘要 JSON' AFTER rewrite_fallback_reason",
    ),
    (
        "route_summary",
        "ADD COLUMN route_summary JSON DEFAULT NULL COMMENT '结构化路由决策摘要 JSON' AFTER logical_object_hits",
    ),
    (
        "cache_summary",
        "ADD COLUMN cache_summary JSON DEFAULT NULL COMMENT '结构化缓存决策摘要 JSON' AFTER route_summary",
    ),
]


def mysql_exec(sql: str) -> str:
    command = [
        "docker",
        "exec",
        "-i",
        MYSQL_CONTAINER,
        "mysql",
        f"-u{MYSQL_USER}",
        f"-p{MYSQL_PASSWORD}",
        MYSQL_DATABASE,
        "-N",
        "-B",
        "-e",
        sql,
    ]
    return subprocess.check_output(command, text=True).strip()


def column_exists(column_name: str) -> bool:
    result = mysql_exec(
        "SELECT COUNT(*) FROM information_schema.columns "
        f"WHERE table_schema = '{MYSQL_DATABASE}' "
        "AND table_name = 'query_history' "
        f"AND column_name = '{column_name}'"
    )
    return result == "1"


def ensure_column(column_name: str, ddl_fragment: str) -> bool:
    if column_exists(column_name):
        return False
    mysql_exec(f"ALTER TABLE query_history {ddl_fragment}")
    return True


def ensure_legacy_column_renamed(old_column_name: str, new_column_name: str, column_definition: str) -> bool:
    if column_exists(new_column_name) or not column_exists(old_column_name):
        return False
    mysql_exec(
        "ALTER TABLE query_history "
        f"CHANGE COLUMN {old_column_name} {new_column_name} {column_definition}"
    )
    return True


def main() -> int:
    applied = []
    renamed = []

    for old_column_name, new_column_name, column_definition in LEGACY_COLUMN_RENAMES:
        if ensure_legacy_column_renamed(old_column_name, new_column_name, column_definition):
            renamed.append(f"{old_column_name}->{new_column_name}")

    for column_name, ddl_fragment in COLUMN_DEFINITIONS:
        if ensure_column(column_name, ddl_fragment):
            applied.append(column_name)

    if renamed or applied:
        details = []
        if renamed:
            details.append("renamed: " + ", ".join(renamed))
        if applied:
            details.append("columns: " + ", ".join(applied))
        print("已升级 query_history 开发 schema:", "; ".join(details))
    else:
        print("query_history 开发 schema 已是最新。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
