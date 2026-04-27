#!/usr/bin/env python3
"""Upgrade the local dev execution_result table when an older bootstrap schema exists."""

from __future__ import annotations

import os
import subprocess
import sys


MYSQL_CONTAINER = os.environ.get("SQLFORGE_DEV_MYSQL_CONTAINER", "sqlforge-mysql")
MYSQL_DATABASE = os.environ.get("SQLFORGE_DEV_MYSQL_DATABASE", "sqlforge")
MYSQL_USER = os.environ.get("SQLFORGE_DEV_MYSQL_USER", "sqlforge")
MYSQL_PASSWORD = os.environ.get("SQLFORGE_DEV_MYSQL_PASSWORD", "sqlforge")


COLUMN_DEFINITIONS = [
    (
        "access_channel",
        "ADD COLUMN access_channel VARCHAR(32) DEFAULT NULL COMMENT 'Access channel such as PAGE/API/JDBC_AGENT/SDK/CLIENT' AFTER saga_id",
    ),
    (
        "target_engine",
        "ADD COLUMN target_engine VARCHAR(64) DEFAULT NULL COMMENT 'Selected execution engine or routed engine' AFTER access_channel",
    ),
    (
        "returned_row_count",
        "ADD COLUMN returned_row_count BIGINT DEFAULT NULL COMMENT 'Returned row count when known' AFTER target_engine",
    ),
    (
        "cache_hit",
        "ADD COLUMN cache_hit TINYINT(1) DEFAULT NULL COMMENT 'Whether cache was hit' AFTER returned_row_count",
    ),
    (
        "rewrite_applied",
        "ADD COLUMN rewrite_applied TINYINT(1) DEFAULT NULL COMMENT 'Whether lightweight rewrite was applied' AFTER cache_hit",
    ),
    (
        "acceleration_applied",
        "ADD COLUMN acceleration_applied TINYINT(1) DEFAULT NULL COMMENT 'Whether acceleration path was applied' AFTER rewrite_applied",
    ),
    (
        "hit_table_summary",
        "ADD COLUMN hit_table_summary JSON DEFAULT NULL COMMENT 'Structured hit table summary JSON' AFTER acceleration_applied",
    ),
    (
        "route_summary",
        "ADD COLUMN route_summary JSON DEFAULT NULL COMMENT 'Structured route summary JSON' AFTER hit_table_summary",
    ),
    (
        "cache_summary",
        "ADD COLUMN cache_summary JSON DEFAULT NULL COMMENT 'Structured cache summary JSON' AFTER route_summary",
    ),
]

INDEX_DEFINITIONS = [
    (
        "idx_execution_result_access_channel",
        "ADD KEY idx_execution_result_access_channel (tenant_id, access_channel, create_time)",
    ),
    (
        "idx_execution_result_target_engine",
        "ADD KEY idx_execution_result_target_engine (tenant_id, target_engine, create_time)",
    ),
    (
        "idx_execution_result_cache_hit",
        "ADD KEY idx_execution_result_cache_hit (tenant_id, cache_hit, create_time)",
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
        "AND table_name = 'execution_result' "
        f"AND column_name = '{column_name}'"
    )
    return result == "1"


def index_exists(index_name: str) -> bool:
    result = mysql_exec(
        "SELECT COUNT(*) FROM information_schema.statistics "
        f"WHERE table_schema = '{MYSQL_DATABASE}' "
        "AND table_name = 'execution_result' "
        f"AND index_name = '{index_name}'"
    )
    return result != "0"


def ensure_column(column_name: str, ddl_fragment: str) -> bool:
    if column_exists(column_name):
        return False
    mysql_exec(f"ALTER TABLE execution_result {ddl_fragment}")
    return True


def ensure_index(index_name: str, ddl_fragment: str) -> bool:
    if index_exists(index_name):
        return False
    mysql_exec(f"ALTER TABLE execution_result {ddl_fragment}")
    return True


def main() -> int:
    applied_columns = []
    applied_indexes = []

    for column_name, ddl_fragment in COLUMN_DEFINITIONS:
        if ensure_column(column_name, ddl_fragment):
            applied_columns.append(column_name)

    for index_name, ddl_fragment in INDEX_DEFINITIONS:
        if ensure_index(index_name, ddl_fragment):
            applied_indexes.append(index_name)

    if applied_columns or applied_indexes:
        details = []
        if applied_columns:
            details.append("columns: " + ", ".join(applied_columns))
        if applied_indexes:
            details.append("indexes: " + ", ".join(applied_indexes))
        print("Upgraded execution_result dev schema:", "; ".join(details))
    else:
        print("execution_result dev schema already up to date.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
