#!/usr/bin/env python3
"""Upgrade the local dev query_history table when an older bootstrap schema exists."""

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
        "sql_template_cipher",
        "ADD COLUMN sql_template_cipher MEDIUMBLOB DEFAULT NULL COMMENT 'Encrypted template SQL payload for parameterized queries' AFTER sql_text_cipher",
    ),
    (
        "bound_sql_text_cipher",
        "ADD COLUMN bound_sql_text_cipher MEDIUMBLOB DEFAULT NULL COMMENT 'Encrypted bound SQL payload after parameter binding' AFTER sql_template_cipher",
    ),
    (
        "datasource_code",
        "ADD COLUMN datasource_code VARCHAR(128) DEFAULT NULL COMMENT 'Datasource business identifier or alias' AFTER bound_sql_text_cipher",
    ),
    (
        "report_code",
        "ADD COLUMN report_code VARCHAR(128) DEFAULT NULL COMMENT 'Report code parsed from SQL comment context' AFTER datasource_type",
    ),
    (
        "stage_code",
        "ADD COLUMN stage_code VARCHAR(32) DEFAULT NULL COMMENT 'Execution stage parsed from SQL comment context' AFTER report_code",
    ),
    (
        "biz_date",
        "ADD COLUMN biz_date DATE DEFAULT NULL COMMENT 'Execution date parsed from SQL comment context' AFTER stage_code",
    ),
    (
        "query_date_start",
        "ADD COLUMN query_date_start DATE DEFAULT NULL COMMENT 'Query date lower bound parsed from SQL body' AFTER biz_date",
    ),
    (
        "query_date_end",
        "ADD COLUMN query_date_end DATE DEFAULT NULL COMMENT 'Query date upper bound parsed from SQL body' AFTER query_date_start",
    ),
    (
        "query_date_status",
        "ADD COLUMN query_date_status VARCHAR(32) DEFAULT NULL COMMENT 'Query date extraction status such as RESOLVED/UNRESOLVED/PARTIAL' AFTER query_date_end",
    ),
    (
        "access_channel",
        "ADD COLUMN access_channel VARCHAR(32) DEFAULT NULL COMMENT 'Access channel such as PAGE/API/JDBC_AGENT/SDK/CLIENT' AFTER query_date_status",
    ),
    (
        "parameterized_sql_flag",
        "ADD COLUMN parameterized_sql_flag TINYINT(1) DEFAULT NULL COMMENT 'Whether the SQL was parameterized before binding' AFTER access_channel",
    ),
    (
        "binding_mode",
        "ADD COLUMN binding_mode VARCHAR(16) DEFAULT NULL COMMENT 'Binding mode such as POSITIONAL/NAMED' AFTER parameterized_sql_flag",
    ),
    (
        "binding_render_status",
        "ADD COLUMN binding_render_status VARCHAR(16) DEFAULT NULL COMMENT 'Binding render status such as SUCCESS/PARTIAL/FAILED/MASKED' AFTER binding_mode",
    ),
    (
        "sql_template_fingerprint",
        "ADD COLUMN sql_template_fingerprint CHAR(32) DEFAULT NULL COMMENT 'Template SQL fingerprint before binding' AFTER binding_render_status",
    ),
    (
        "bound_sql_fingerprint",
        "ADD COLUMN bound_sql_fingerprint CHAR(32) DEFAULT NULL COMMENT 'Bound SQL fingerprint after binding' AFTER sql_template_fingerprint",
    ),
    (
        "comment_context",
        "ADD COLUMN comment_context JSON DEFAULT NULL COMMENT 'Structured SQL comment context JSON' AFTER saga_id",
    ),
    (
        "binding_summary",
        "ADD COLUMN binding_summary JSON DEFAULT NULL COMMENT 'Structured parameter binding summary JSON' AFTER comment_context",
    ),
    (
        "rewrite_record_id",
        "ADD COLUMN rewrite_record_id VARCHAR(64) DEFAULT NULL COMMENT 'SQL rewrite record identifier applied or evaluated for this execution' AFTER binding_summary",
    ),
    (
        "runtime_binding_id",
        "ADD COLUMN runtime_binding_id VARCHAR(64) DEFAULT NULL COMMENT 'Runtime rewrite binding identifier used by query-execution' AFTER rewrite_record_id",
    ),
    (
        "rewrite_rule_version",
        "ADD COLUMN rewrite_rule_version BIGINT DEFAULT NULL COMMENT 'Numeric runtime rewrite rule version snapshot' AFTER runtime_binding_id",
    ),
    (
        "runtime_rule_version",
        "ADD COLUMN runtime_rule_version VARCHAR(64) DEFAULT NULL COMMENT 'Runtime rewrite rule version label snapshot' AFTER rewrite_rule_version",
    ),
    (
        "runtime_rewrite_status",
        "ADD COLUMN runtime_rewrite_status VARCHAR(32) DEFAULT NULL COMMENT 'Runtime rewrite binding resolution status snapshot' AFTER runtime_rule_version",
    ),
    (
        "rewrite_publish_status_snapshot",
        "ADD COLUMN rewrite_publish_status_snapshot VARCHAR(32) DEFAULT NULL COMMENT 'Publish status snapshot derived from backend runtime evidence' AFTER runtime_rewrite_status",
    ),
    (
        "rewrite_fallback_reason",
        "ADD COLUMN rewrite_fallback_reason VARCHAR(128) DEFAULT NULL COMMENT 'Reason automatic rewrite fell back to original SQL' AFTER rewrite_publish_status_snapshot",
    ),
    (
        "logical_object_hits",
        "ADD COLUMN logical_object_hits JSON DEFAULT NULL COMMENT 'Structured logical object and table hit summary JSON' AFTER rewrite_fallback_reason",
    ),
    (
        "route_summary",
        "ADD COLUMN route_summary JSON DEFAULT NULL COMMENT 'Structured route decision summary JSON' AFTER logical_object_hits",
    ),
    (
        "cache_summary",
        "ADD COLUMN cache_summary JSON DEFAULT NULL COMMENT 'Structured cache decision summary JSON' AFTER route_summary",
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


def main() -> int:
    applied = []
    for column_name, ddl_fragment in COLUMN_DEFINITIONS:
        if ensure_column(column_name, ddl_fragment):
            applied.append(column_name)

    if applied:
        print("Upgraded query_history dev schema:", ", ".join(applied))
    else:
        print("query_history dev schema already up to date.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
