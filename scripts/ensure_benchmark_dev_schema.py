#!/usr/bin/env python3
"""当本地开发 benchmark 表仍是旧启动 schema 时执行升级。"""

from __future__ import annotations

import os
import subprocess
import sys


MYSQL_CONTAINER = os.environ.get("SQLFORGE_DEV_MYSQL_CONTAINER", "sqlforge-mysql")
MYSQL_DATABASE = os.environ.get("SQLFORGE_DEV_MYSQL_DATABASE", "sqlforge")
MYSQL_USER = os.environ.get("SQLFORGE_DEV_MYSQL_USER", "sqlforge")
MYSQL_PASSWORD = os.environ.get("SQLFORGE_DEV_MYSQL_PASSWORD", "sqlforge")


TASK_COLUMN_DEFINITIONS = [
    (
        "scale_target_json",
        "ADD COLUMN scale_target_json JSON DEFAULT NULL COMMENT '生产规模目标与未验证证据边界 JSON' AFTER dataset_size_label",
    ),
    (
        "template_id",
        "ADD COLUMN template_id VARCHAR(64) DEFAULT NULL COMMENT '压测模板标识符' AFTER scale_target_json",
    ),
    (
        "template_type",
        "ADD COLUMN template_type VARCHAR(32) DEFAULT NULL COMMENT '压测模板类型' AFTER template_id",
    ),
    (
        "template_version",
        "ADD COLUMN template_version VARCHAR(32) DEFAULT NULL COMMENT '压测模板契约版本' AFTER template_type",
    ),
    (
        "test_set_id",
        "ADD COLUMN test_set_id VARCHAR(64) DEFAULT NULL COMMENT '压测测试集标识符' AFTER template_version",
    ),
    (
        "test_set_source",
        "ADD COLUMN test_set_source VARCHAR(32) DEFAULT NULL COMMENT '压测测试集来源类型' AFTER test_set_id",
    ),
    (
        "test_set_labels_json",
        "ADD COLUMN test_set_labels_json JSON DEFAULT NULL COMMENT '压测测试集标签模型 JSON' AFTER test_set_source",
    ),
    (
        "test_set_source_refs_json",
        "ADD COLUMN test_set_source_refs_json JSON DEFAULT NULL COMMENT '压测测试集来源引用 JSON' AFTER test_set_labels_json",
    ),
]

TASK_INDEX_DEFINITIONS = [
    (
        "idx_benchmark_task_template_type",
        "ADD KEY idx_benchmark_task_template_type (template_type)",
    ),
    (
        "idx_benchmark_task_test_set_source",
        "ADD KEY idx_benchmark_task_test_set_source (test_set_source)",
    ),
]

REPORT_COLUMN_DEFINITIONS = [
    (
        "execution_summary_json",
        "ADD COLUMN execution_summary_json JSON DEFAULT NULL COMMENT '隔离执行摘要 JSON' AFTER recommendations_json",
    ),
    (
        "regression_summary_json",
        "ADD COLUMN regression_summary_json JSON DEFAULT NULL COMMENT '回归摘要 JSON' AFTER execution_summary_json",
    ),
    (
        "alert_linkages_json",
        "ADD COLUMN alert_linkages_json JSON DEFAULT NULL COMMENT '治理告警关联 JSON' AFTER regression_summary_json",
    ),
    (
        "export_artifacts_json",
        "ADD COLUMN export_artifacts_json JSON DEFAULT NULL COMMENT '已持久化导出制品元数据与内容 JSON' AFTER alert_linkages_json",
    ),
]


def mysql_exec(sql: str) -> str:
    command = [
        "docker",
        "exec",
        "-i",
        MYSQL_CONTAINER,
        "mysql",
        "--init-command=SET time_zone='+08:00'",
        f"-u{MYSQL_USER}",
        f"-p{MYSQL_PASSWORD}",
        MYSQL_DATABASE,
        "-N",
        "-B",
        "-e",
        sql,
    ]
    return subprocess.check_output(command, text=True).strip()


def column_exists(table_name: str, column_name: str) -> bool:
    result = mysql_exec(
        "SELECT COUNT(*) FROM information_schema.columns "
        f"WHERE table_schema = '{MYSQL_DATABASE}' "
        f"AND table_name = '{table_name}' "
        f"AND column_name = '{column_name}'"
    )
    return result == "1"


def index_exists(table_name: str, index_name: str) -> bool:
    result = mysql_exec(
        "SELECT COUNT(*) FROM information_schema.statistics "
        f"WHERE table_schema = '{MYSQL_DATABASE}' "
        f"AND table_name = '{table_name}' "
        f"AND index_name = '{index_name}'"
    )
    return result != "0"


def ensure_column(table_name: str, column_name: str, ddl_fragment: str) -> bool:
    if column_exists(table_name, column_name):
        return False
    mysql_exec(f"ALTER TABLE {table_name} {ddl_fragment}")
    return True


def ensure_index(table_name: str, index_name: str, ddl_fragment: str) -> bool:
    if index_exists(table_name, index_name):
        return False
    mysql_exec(f"ALTER TABLE {table_name} {ddl_fragment}")
    return True


def main() -> int:
    applied_task_columns = []
    applied_task_indexes = []
    applied_report_columns = []

    for column_name, ddl_fragment in TASK_COLUMN_DEFINITIONS:
        if ensure_column("benchmark_task", column_name, ddl_fragment):
            applied_task_columns.append(column_name)

    for index_name, ddl_fragment in TASK_INDEX_DEFINITIONS:
        if ensure_index("benchmark_task", index_name, ddl_fragment):
            applied_task_indexes.append(index_name)

    for column_name, ddl_fragment in REPORT_COLUMN_DEFINITIONS:
        if ensure_column("benchmark_task_report", column_name, ddl_fragment):
            applied_report_columns.append(column_name)

    if applied_task_columns or applied_task_indexes or applied_report_columns:
        details = []
        if applied_task_columns:
            details.append("benchmark_task columns: " + ", ".join(applied_task_columns))
        if applied_task_indexes:
            details.append("benchmark_task indexes: " + ", ".join(applied_task_indexes))
        if applied_report_columns:
            details.append("benchmark_task_report columns: " + ", ".join(applied_report_columns))
        print("已升级 benchmark 开发 schema:", "; ".join(details))
    else:
        print("benchmark 开发 schema 已是最新。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
