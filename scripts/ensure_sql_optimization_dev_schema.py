#!/usr/bin/env python3
"""当本地开发 sql-optimization 表仍是旧启动 schema 时执行升级。"""

from __future__ import annotations

import os
import subprocess
import sys


MYSQL_CONTAINER = os.environ.get("SQLFORGE_DEV_MYSQL_CONTAINER", "sqlforge-mysql")
MYSQL_DATABASE = os.environ.get("SQLFORGE_DEV_MYSQL_DATABASE", "sqlforge")
MYSQL_USER = os.environ.get("SQLFORGE_DEV_MYSQL_USER", "sqlforge")
MYSQL_PASSWORD = os.environ.get("SQLFORGE_DEV_MYSQL_PASSWORD", "sqlforge")


RECOMMENDATION_COLUMN_DEFINITIONS = [
    (
        "history_id",
        "ADD COLUMN history_id VARCHAR(64) DEFAULT NULL COMMENT '关联查询历史标识符' AFTER source_sql_id",
    ),
    (
        "parse_task_id",
        "ADD COLUMN parse_task_id VARCHAR(64) DEFAULT NULL COMMENT '关联parse task标识符' AFTER history_id",
    ),
    (
        "batch_id",
        "ADD COLUMN batch_id VARCHAR(64) DEFAULT NULL COMMENT '关联parse/报表批次标识符' AFTER parse_task_id",
    ),
    (
        "route_decision_id",
        "ADD COLUMN route_decision_id VARCHAR(64) DEFAULT NULL COMMENT '关联路由决策标识符' AFTER batch_id",
    ),
    (
        "alert_id",
        "ADD COLUMN alert_id VARCHAR(64) DEFAULT NULL COMMENT '关联告警标识符' AFTER route_decision_id",
    ),
    (
        "source_type",
        "ADD COLUMN source_type VARCHAR(32) DEFAULT NULL COMMENT '用于推荐可追踪性的 PARSE/QUERY 来源类型' AFTER status",
    ),
    (
        "source_kind",
        "ADD COLUMN source_kind VARCHAR(64) DEFAULT NULL COMMENT '治理来源类型，例如 STRUCTURE_PARSE/QUERY_HISTORY' AFTER source_type",
    ),
    (
        "source_id",
        "ADD COLUMN source_id VARCHAR(128) DEFAULT NULL COMMENT '与来源类型/种类配对的规范来源标识符' AFTER source_kind",
    ),
    (
        "evidence_level",
        "ADD COLUMN evidence_level VARCHAR(32) DEFAULT NULL COMMENT '证据level：STATIC_PARSE/ACCESS_PARSE/EXPLAIN_PLAN/RUNTIME_HISTORY/BENCHMARK/MIXED' AFTER source_id",
    ),
    (
        "schema_version",
        "ADD COLUMN schema_version VARCHAR(64) NOT NULL DEFAULT 'SQL_RECOMMENDATION_RULE_MODEL_V1' COMMENT '推荐规则输出模式版本' AFTER evidence_level",
    ),
    (
        "rule_chain_json",
        "ADD COLUMN rule_chain_json JSON DEFAULT NULL COMMENT '已应用或已选择的推荐规则链证据' AFTER schema_version",
    ),
    (
        "unapplied_rules_json",
        "ADD COLUMN unapplied_rules_json JSON DEFAULT NULL COMMENT '已考虑但未应用的规则及原因证据' AFTER rule_chain_json",
    ),
    (
        "preconditions_json",
        "ADD COLUMN preconditions_json JSON DEFAULT NULL COMMENT '校验或后续应用前所需的前置条件' AFTER unapplied_rules_json",
    ),
    (
        "semantic_risks_json",
        "ADD COLUMN semantic_risks_json JSON DEFAULT NULL COMMENT '需要校验或人工评审的语义风险' AFTER preconditions_json",
    ),
    (
        "acceleration_artifact_json",
        "ADD COLUMN acceleration_artifact_json JSON DEFAULT NULL COMMENT '高级 MV accelerationArtifact 持久快照，响应仅返回允许的高级 MV 类型' AFTER semantic_risks_json",
    ),
    (
        "expected_benefit_json",
        "ADD COLUMN expected_benefit_json JSON DEFAULT NULL COMMENT '静态预估收益证据，非真实执行收益' AFTER acceleration_artifact_json",
    ),
    (
        "estimated_cost_json",
        "ADD COLUMN estimated_cost_json JSON DEFAULT NULL COMMENT '静态预估成本与治理跟进证据' AFTER expected_benefit_json",
    ),
    (
        "confidence",
        "ADD COLUMN confidence INT DEFAULT NULL COMMENT '静态模型置信分 0-100' AFTER estimated_cost_json",
    ),
    (
        "validation_method",
        "ADD COLUMN validation_method VARCHAR(64) DEFAULT NULL COMMENT '批准或后续应用前要求的校验方法' AFTER confidence",
    ),
    (
        "validation_status",
        "ADD COLUMN validation_status VARCHAR(32) NOT NULL DEFAULT 'NOT_VALIDATED' COMMENT '校验状态：NOT_VALIDATED/VALIDATING/EQUIVALENT/DIVERGED/FAILED/EXPIRED' AFTER validation_method",
    ),
    (
        "auto_apply_allowed",
        "ADD COLUMN auto_apply_allowed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '校验后是否允许后续自动应用' AFTER validation_status",
    ),
    (
        "manual_review_required",
        "ADD COLUMN manual_review_required TINYINT(1) NOT NULL DEFAULT 1 COMMENT '推荐是否需要人工评审' AFTER auto_apply_allowed",
    ),
    (
        "source_problems_json",
        "ADD COLUMN source_problems_json JSON DEFAULT NULL COMMENT '改写试算来源问题证据，不代表生产已改写' AFTER manual_review_required",
    ),
    (
        "issue_rule_links_json",
        "ADD COLUMN issue_rule_links_json JSON DEFAULT NULL COMMENT '来源问题到改写规则链路证据' AFTER source_problems_json",
    ),
]

RECOMMENDATION_INDEX_DEFINITIONS = [
    (
        "idx_acc_reco_history",
        "ADD KEY idx_acc_reco_history (tenant_id, history_id)",
    ),
    (
        "idx_acc_reco_parse_task",
        "ADD KEY idx_acc_reco_parse_task (tenant_id, parse_task_id)",
    ),
    (
        "idx_acc_reco_batch",
        "ADD KEY idx_acc_reco_batch (tenant_id, batch_id)",
    ),
    (
        "idx_acc_reco_route",
        "ADD KEY idx_acc_reco_route (tenant_id, route_decision_id)",
    ),
    (
        "idx_acc_reco_alert",
        "ADD KEY idx_acc_reco_alert (tenant_id, alert_id)",
    ),
    (
        "idx_acc_reco_source",
        "ADD KEY idx_acc_reco_source (tenant_id, source_type, source_kind, source_id)",
    ),
    (
        "idx_acc_reco_validation",
        "ADD KEY idx_acc_reco_validation (tenant_id, validation_status, manual_review_required)",
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
    applied_columns = []
    applied_indexes = []

    for column_name, ddl_fragment in RECOMMENDATION_COLUMN_DEFINITIONS:
        if ensure_column("acceleration_recommendation", column_name, ddl_fragment):
            applied_columns.append(column_name)

    for index_name, ddl_fragment in RECOMMENDATION_INDEX_DEFINITIONS:
        if ensure_index("acceleration_recommendation", index_name, ddl_fragment):
            applied_indexes.append(index_name)

    if applied_columns or applied_indexes:
        details = []
        if applied_columns:
            details.append("acceleration_recommendation columns: " + ", ".join(applied_columns))
        if applied_indexes:
            details.append("acceleration_recommendation indexes: " + ", ".join(applied_indexes))
        print("已升级 sql-optimization 开发 schema:", "; ".join(details))
    else:
        print("sql-optimization 开发 schema 已是最新。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
