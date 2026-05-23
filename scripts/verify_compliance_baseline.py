#!/usr/bin/env python3
"""Minimal machine-checkable compliance baseline verification for R-118."""

from __future__ import annotations

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent


def require_file(path: str, errors: list[str]) -> Path:
    target = ROOT / path
    if not target.is_file():
        errors.append(f"Missing required file: {path}")
    return target


def require_text(path: str, pattern: str, errors: list[str]) -> None:
    target = require_file(path, errors)
    if errors and not target.exists():
        return
    content = target.read_text(encoding="utf-8")
    if pattern not in content:
        errors.append(f"Missing required marker in {path}: {pattern}")


def main() -> int:
    errors: list[str] = []

    require_text("docs/security/compliance.md", "## R-115 备份恢复", errors)
    require_text("docs/security/compliance.md", "数据库中不存在明文密码、Token、密钥。", errors)
    require_text("docs/security/access-control-spec.md", "Execution Safety Boundaries", errors)
    require_text("docs/deployments/backup-recovery-baseline.md", "## Recovery Objectives", errors)
    require_text("docs/deployments/backup-recovery-baseline.md", "Restore Validation Checklist", errors)
    require_text("docs/deployments/observability-baseline.md", "SQLForge Observability Baseline", errors)
    require_text("docs/architecture/messaging-abstraction.md", "scripts/run-kafka-runtime-gate.sh", errors)
    require_text("sql/init-schema.sql", "CREATE TABLE IF NOT EXISTS audit_log", errors)
    require_text("sql/init-schema.sql", "CREATE TABLE IF NOT EXISTS system_config", errors)
    require_text("sql/migrations/V20260421_013__sensitive_data_encryption_baseline.sql", "ADD COLUMN value_ciphertext", errors)
    require_file("scripts/verify_kafka_runtime_config.py", errors)
    require_file("scripts/run-kafka-runtime-gate.sh", errors)
    require_file("scripts/verify-db-scripts.sh", errors)

    if errors:
        sys.stderr.write("Compliance baseline verification failed:\n")
        for item in errors:
            sys.stderr.write(f"- {item}\n")
        return 1

    sys.stdout.write(
        "Compliance baseline verification passed: auth/access-control, audit schema, encryption baseline, backup-recovery, observability, Kafka runtime gate, and DB script gate evidence are present.\n"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
