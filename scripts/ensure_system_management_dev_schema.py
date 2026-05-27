#!/usr/bin/env python3
"""当本地开发系统管理 schema 仍是旧启动 schema 时执行升级。"""

from __future__ import annotations

import os
import subprocess
import sys


MYSQL_CONTAINER = os.environ.get("SQLFORGE_DEV_MYSQL_CONTAINER", "sqlforge-mysql")
MYSQL_DATABASE = os.environ.get("SQLFORGE_DEV_MYSQL_DATABASE", "sqlforge")
MYSQL_USER = os.environ.get("SQLFORGE_DEV_MYSQL_USER", "sqlforge")
MYSQL_PASSWORD = os.environ.get("SQLFORGE_DEV_MYSQL_PASSWORD", "sqlforge")


DATASOURCE_COLUMN_DEFINITIONS = [
    (
        "driver_source_type",
        "ADD COLUMN driver_source_type VARCHAR(32) NOT NULL DEFAULT 'CLASSPATH' COMMENT '驱动来源类型：CLASSPATH/UPLOADED' AFTER jdbc_driver_class_name",
    ),
    (
        "jdbc_driver_artifact_id",
        "ADD COLUMN jdbc_driver_artifact_id VARCHAR(64) DEFAULT NULL COMMENT '已绑定的上传驱动制品标识符' AFTER driver_source_type",
    ),
    (
        "driver_version_label",
        "ADD COLUMN driver_version_label VARCHAR(128) DEFAULT NULL COMMENT '已绑定驱动版本标签快照' AFTER jdbc_driver_artifact_id",
    ),
    (
        "driver_sha256",
        "ADD COLUMN driver_sha256 VARCHAR(64) DEFAULT NULL COMMENT '已绑定驱动 sha256 快照' AFTER driver_version_label",
    ),
    (
        "driver_load_status",
        "ADD COLUMN driver_load_status VARCHAR(32) DEFAULT NULL COMMENT '已绑定驱动加载状态快照' AFTER driver_sha256",
    ),
]

DRIVER_ARTIFACT_TABLE_DDL = """
CREATE TABLE IF NOT EXISTS jdbc_driver_artifact (
  artifact_id VARCHAR(64) NOT NULL COMMENT 'JDBC 驱动制品标识符',
  tenant_id VARCHAR(64) NOT NULL COMMENT '所属租户标识符',
  engine_type VARCHAR(32) NOT NULL COMMENT '引擎类型：TRINO/HETU/HIVE',
  driver_class_name VARCHAR(255) NOT NULL COMMENT '驱动主类名',
  version_label VARCHAR(128) NOT NULL COMMENT '版本标签',
  original_file_name VARCHAR(255) NOT NULL COMMENT '原始上传文件名',
  size_bytes BIGINT NOT NULL COMMENT '文件大小，单位字节',
  sha256 VARCHAR(64) NOT NULL COMMENT '文件 sha256 校验和',
  relative_path VARCHAR(512) NOT NULL COMMENT '共享卷相对路径',
  status VARCHAR(32) NOT NULL COMMENT '制品状态：READY/FAILED',
  uploaded_by VARCHAR(64) DEFAULT NULL COMMENT '上传操作者标识符',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间戳',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间戳',
  PRIMARY KEY (artifact_id),
  KEY idx_jdbc_driver_artifact_tenant_time (tenant_id, create_time),
  KEY idx_jdbc_driver_artifact_tenant_engine (tenant_id, engine_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Append-only uploaded JDBC driver artifacts'
""".strip()


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


def table_exists(table_name: str) -> bool:
    result = mysql_exec(
        "SELECT COUNT(*) FROM information_schema.tables "
        f"WHERE table_schema = '{MYSQL_DATABASE}' "
        f"AND table_name = '{table_name}'"
    )
    return result == "1"


def ensure_column(table_name: str, column_name: str, ddl_fragment: str) -> bool:
    if column_exists(table_name, column_name):
        return False
    mysql_exec(f"ALTER TABLE {table_name} {ddl_fragment}")
    return True


def ensure_table(table_name: str, ddl: str) -> bool:
    if table_exists(table_name):
        return False
    mysql_exec(ddl)
    return True


def main() -> int:
    applied_datasource_columns = []
    for column_name, ddl_fragment in DATASOURCE_COLUMN_DEFINITIONS:
        if ensure_column("datasource_config", column_name, ddl_fragment):
            applied_datasource_columns.append(column_name)

    created_driver_artifact_table = ensure_table("jdbc_driver_artifact", DRIVER_ARTIFACT_TABLE_DDL)

    if applied_datasource_columns or created_driver_artifact_table:
        details = []
        if applied_datasource_columns:
            details.append("datasource_config columns: " + ", ".join(applied_datasource_columns))
        if created_driver_artifact_table:
            details.append("jdbc_driver_artifact table")
        print("已升级 system-management 开发 schema:", "; ".join(details))
    else:
        print("system-management 开发 schema 已是最新。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
