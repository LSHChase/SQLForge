#!/usr/bin/env python3
"""Audit SQL rewrite recommendation readiness against the production-scale objective."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
import tempfile
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[1]
MIN_SELECT_REWRITE_RULES = 36
AUDITED_SELECT_REWRITE_RULES = 50
MIN_PRODUCTION_CONCURRENCY = 10000
MIN_PRODUCTION_DAILY_QUERY_VOLUME = 10000000
MIN_PRODUCTION_DATASET_SIZE_BYTES = 30000000000000000
MIN_REPLAY_HOURS = Decimal("24")
REQUIRED_EVIDENCE_FILES = (
    "concurrency.json",
    "daily-query-volume.json",
    "data-layout.json",
    "workload-replay.json",
    "metrics.csv",
    "cost-bill.json",
)


def read_text(root: Path, relative_path: str) -> str:
    path = root / relative_path
    if not path.is_file():
        raise FileNotFoundError(relative_path)
    return path.read_text(encoding="utf-8")


def check_item(requirement: str,
               status: str,
               evidence: str,
               missing: list[str] | None = None,
               details: dict[str, Any] | None = None) -> dict[str, Any]:
    item: dict[str, Any] = {
        "requirement": requirement,
        "status": status,
        "evidence": evidence,
    }
    if missing:
        item["missing"] = missing
    if details:
        item["details"] = details
    return item


def check_research(root: Path) -> dict[str, Any]:
    path = "docs/references/sql-rewrite-recommendation-research-2026-05-18.md"
    try:
        text = read_text(root, path)
    except FileNotFoundError:
        return check_item(
            "latest_sql_rewrite_research_archived",
            "FAILED",
            path,
            ["research document is missing"],
        )
    required_terms = [
        "Apache Calcite",
        "Trino",
        "BigQuery",
        "Snowflake",
        "Patent",
        "Rulescript 2026",
        "Efficient Cost-Based Rewrite",
        "LASER",
        "SLER",
        "Trino 481",
        "Snowflake 2026 performance improvements",
        "E3-Rewrite 2025",
    ]
    missing = [term for term in required_terms if term not in text]
    return check_item(
        "latest_sql_rewrite_research_archived",
        "PASSED" if not missing else "FAILED",
        path,
        missing,
        {"requiredTerms": required_terms},
    )


def extract_rule_codes(test_text: str) -> list[str]:
    marker = "shouldExposeAtLeastFiftySelectRewriteRecommendationScenarios"
    start = test_text.find(marker)
    end = test_text.find("private String complexAntiPatternSql", start)
    if start < 0 or end < 0:
        return []
    method_text = test_text[start:end]
    return sorted(set(re.findall(r'"([A-Z][A-Z0-9_]{2,})"\s*\)', method_text)))


def check_rewrite_rule_coverage(root: Path) -> dict[str, Any]:
    path = "sql-optimization/src/test/java/com/company/sqloptimization/application/service/SqlOptimizationPipelineServiceTest.java"
    try:
        text = read_text(root, path)
    except FileNotFoundError:
        return check_item(
            "select_rewrite_rule_coverage_at_least_36",
            "FAILED",
            path,
            ["coverage test is missing"],
        )
    rule_codes = extract_rule_codes(text)
    missing: list[str] = []
    if len(rule_codes) < AUDITED_SELECT_REWRITE_RULES:
        missing.append("auditedRuleCount<" + str(AUDITED_SELECT_REWRITE_RULES))
    if "assertTrue(coveredRules.size() >= 50" not in text:
        missing.append("explicit >=50 assertion missing")
    if "assertFalse(model.isAutoApplyAllowed()" not in text:
        missing.append("auto-apply guard assertion missing")
    return check_item(
        "select_rewrite_rule_coverage_at_least_36",
        "PASSED" if not missing and len(rule_codes) >= MIN_SELECT_REWRITE_RULES else "FAILED",
        path,
        missing,
        {
            "minimumRequiredRules": MIN_SELECT_REWRITE_RULES,
            "auditedMinimumRules": AUDITED_SELECT_REWRITE_RULES,
            "coveredRuleCount": len(rule_codes),
            "coveredRules": rule_codes,
        },
    )


def check_recommendation_gate(root: Path) -> dict[str, Any]:
    source_path = "sql-optimization/src/main/java/com/company/sqloptimization/application/service/SqlOptimizationPipelineService.java"
    trial_test_path = "sql-optimization/src/test/java/com/company/sqloptimization/application/service/RewriteTrialApplicationServiceTest.java"
    try:
        source = read_text(root, source_path)
        trial_test = read_text(root, trial_test_path)
    except FileNotFoundError as exc:
        return check_item(
            "rewrite_recommendation_payload_carries_production_scale_gate",
            "FAILED",
            source_path,
            [str(exc)],
        )
    required_tokens = [
        "productionScaleGate",
        "EXTERNAL_EVIDENCE_REQUIRED",
        "PRODUCTION_SCALE_NOT_PROVEN_BY_STATIC_REWRITE",
        "VERIFIED_10000_CONCURRENCY",
        "VERIFIED_10M_DAILY_QUERY_VOLUME",
        "VERIFIED_30PB_DATA_LAYOUT",
        "VERIFIED_COST_BILL",
        "PRODUCTION_TARGET_DAILY_QUERY_VOLUME",
    ]
    missing = [token for token in required_tokens if token not in source]
    if "productionScaleGate" not in trial_test:
        missing.append("rewrite trial persistence test does not assert productionScaleGate")
    return check_item(
        "rewrite_recommendation_payload_carries_production_scale_gate",
        "PASSED" if not missing else "FAILED",
        source_path + "; " + trial_test_path,
        missing,
        {"requiredTokens": required_tokens},
    )


def decimal_value(value: Any, field: str, missing: list[str]) -> Decimal | None:
    if value is None or str(value).strip() == "":
        missing.append(field)
        return None
    try:
        return Decimal(str(value))
    except (InvalidOperation, ValueError):
        missing.append(field + ":notNumeric")
        return None


def require_at_least(bundle: dict[str, Any], field: str, minimum: int | Decimal, missing: list[str]) -> None:
    actual = decimal_value(bundle.get(field), field, missing)
    if actual is not None and actual < Decimal(str(minimum)):
        missing.append(field + ":required>=" + str(minimum) + ",actual=" + str(actual))


def require_positive(bundle: dict[str, Any], field: str, missing: list[str]) -> None:
    actual = decimal_value(bundle.get(field), field, missing)
    if actual is not None and actual <= Decimal("0"):
        missing.append(field + ":required>0,actual=" + str(actual))


def require_evidence_file_digests(payload: dict[str, Any], missing: list[str], field_prefix: str) -> None:
    digests = payload.get("evidenceFileDigests")
    if not isinstance(digests, dict):
        missing.append(field_prefix + "evidenceFileDigests")
        return
    for file_name in REQUIRED_EVIDENCE_FILES:
        digest_entry = digests.get(file_name)
        if not isinstance(digest_entry, dict):
            missing.append(field_prefix + "evidenceFileDigests." + file_name)
            continue
        sha256 = str(digest_entry.get("sha256") or "")
        if not re.fullmatch(r"[0-9a-f]{64}", sha256):
            missing.append(field_prefix + "evidenceFileDigests." + file_name + ".sha256")
        size_bytes = decimal_value(
            digest_entry.get("sizeBytes"),
            field_prefix + "evidenceFileDigests." + file_name + ".sizeBytes",
            missing,
        )
        if size_bytes is not None and size_bytes <= Decimal("0"):
            missing.append(
                field_prefix + "evidenceFileDigests." + file_name + ".sizeBytes:required>0,actual=" + str(size_bytes)
            )


def require_matching_evidence_file_digests(payload: dict[str, Any],
                                           manifest: dict[str, Any],
                                           missing: list[str]) -> None:
    top_level_digests = payload.get("evidenceFileDigests")
    manifest_digests = manifest.get("evidenceFileDigests")
    if not isinstance(top_level_digests, dict) or not isinstance(manifest_digests, dict):
        return
    for file_name in REQUIRED_EVIDENCE_FILES:
        top_entry = top_level_digests.get(file_name)
        manifest_entry = manifest_digests.get(file_name)
        if not isinstance(top_entry, dict) or not isinstance(manifest_entry, dict):
            continue
        if top_entry.get("sha256") != manifest_entry.get("sha256"):
            missing.append("evidenceFileDigests." + file_name + ".sha256!=scaleTargetEvidenceManifest")
        if str(top_entry.get("sizeBytes")) != str(manifest_entry.get("sizeBytes")):
            missing.append("evidenceFileDigests." + file_name + ".sizeBytes!=scaleTargetEvidenceManifest")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require_raw_evidence_file_digests(payload: dict[str, Any],
                                      evidence_dir: Path | None,
                                      missing: list[str]) -> None:
    if evidence_dir is None:
        missing.append("externalEvidenceDirectory")
        return
    if not evidence_dir.is_dir():
        missing.append("externalEvidenceDirectory:notDirectory")
        return
    digests = payload.get("evidenceFileDigests")
    if not isinstance(digests, dict):
        return
    for file_name in REQUIRED_EVIDENCE_FILES:
        digest_entry = digests.get(file_name)
        if not isinstance(digest_entry, dict):
            continue
        file_path = evidence_dir / file_name
        if not file_path.is_file():
            missing.append("externalEvidenceDirectory." + file_name)
            continue
        actual_sha256 = sha256_file(file_path)
        actual_size_bytes = file_path.stat().st_size
        if digest_entry.get("sha256") != actual_sha256:
            missing.append("externalEvidenceDirectory." + file_name + ".sha256Mismatch")
        if str(digest_entry.get("sizeBytes")) != str(actual_size_bytes):
            missing.append("externalEvidenceDirectory." + file_name + ".sizeBytesMismatch")


def collect_raw_evidence_file_digests(evidence_dir: Path) -> dict[str, dict[str, Any]]:
    digests: dict[str, dict[str, Any]] = {}
    for file_name in REQUIRED_EVIDENCE_FILES:
        file_path = evidence_dir / file_name
        if file_path.is_file():
            digests[file_name] = {
                "sha256": sha256_file(file_path),
                "sizeBytes": file_path.stat().st_size,
            }
    return digests


def check_external_verification_result(verification_result: Path | None,
                                       evidence_dir: Path | None) -> dict[str, Any]:
    requirement = "external_production_scale_evidence_verified"
    if verification_result is None:
        return check_item(
            requirement,
            "BLOCKED",
            "scripts/verify-benchmark-production-evidence.py --output <external-evidence-dir>/verification-result.json",
            ["verification-result.json not supplied"],
        )
    if not verification_result.is_file():
        return check_item(requirement, "BLOCKED", str(verification_result), ["verification-result.json missing"])
    try:
        payload = json.loads(verification_result.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return check_item(requirement, "FAILED", str(verification_result), ["invalid JSON: " + str(exc)])

    missing: list[str] = []
    if payload.get("status") != "PASSED":
        missing.append("status=PASSED")
    if payload.get("externalVerificationStatus") != "VERIFIED":
        missing.append("externalVerificationStatus=VERIFIED")
    if payload.get("missingEvidence"):
        missing.append("missingEvidence empty")
    if payload.get("parseErrors"):
        missing.append("parseErrors empty")
    require_evidence_file_digests(payload, missing, "")
    require_raw_evidence_file_digests(payload, evidence_dir, missing)

    manifest = payload.get("scaleTargetEvidenceManifest")
    if not isinstance(manifest, dict):
        missing.append("scaleTargetEvidenceManifest")
        manifest = {}
    if manifest.get("evidenceSource") != "PRODUCTION_EVIDENCE_DIRECTORY":
        missing.append("evidenceSource=PRODUCTION_EVIDENCE_DIRECTORY")
    if manifest.get("externalVerificationStatus") != "VERIFIED":
        missing.append("scaleTargetEvidenceManifest.externalVerificationStatus=VERIFIED")
    required_manifest_refs = [
        "concurrencyProofRef",
        "dailyQueryVolumeProofRef",
        "dataLayoutProofRef",
        "workloadReplayProofRef",
        "workloadReplayWindow",
        "p95P99MetricProofRef",
        "scanCpuQueueMetricProofRef",
        "costBillProofRef",
    ]
    for proof_ref in required_manifest_refs:
        if not manifest.get(proof_ref):
            missing.append(proof_ref)
    require_evidence_file_digests(manifest, missing, "scaleTargetEvidenceManifest.")
    require_matching_evidence_file_digests(payload, manifest, missing)
    bundle = manifest.get("verificationBundle")
    if not isinstance(bundle, dict):
        missing.append("verificationBundle")
        bundle = {}

    require_at_least(bundle, "observedConcurrency", MIN_PRODUCTION_CONCURRENCY, missing)
    require_at_least(bundle, "observedDailyQueryVolume", MIN_PRODUCTION_DAILY_QUERY_VOLUME, missing)
    require_at_least(bundle, "observedDatasetSizeBytes", MIN_PRODUCTION_DATASET_SIZE_BYTES, missing)
    require_at_least(bundle, "workloadReplayDurationHours", MIN_REPLAY_HOURS, missing)
    require_positive(bundle, "p95LatencyMs", missing)
    require_positive(bundle, "p99LatencyMs", missing)
    require_positive(bundle, "scannedBytes", missing)
    require_positive(bundle, "cpuUsagePercent", missing)
    queue_wait = decimal_value(bundle.get("queueWaitMs"), "queueWaitMs", missing)
    if queue_wait is not None and queue_wait < Decimal("0"):
        missing.append("queueWaitMs:required>=0,actual=" + str(queue_wait))
    require_positive(bundle, "costBillAmount", missing)
    if not bundle.get("costBillCurrency"):
        missing.append("costBillCurrency")
    if not bundle.get("verifierRef"):
        missing.append("verifierRef")

    return check_item(
        requirement,
        "PASSED" if not missing else "BLOCKED",
        str(verification_result),
        sorted(set(missing)),
        {
            "targetConcurrency": MIN_PRODUCTION_CONCURRENCY,
            "targetDailyQueryVolume": MIN_PRODUCTION_DAILY_QUERY_VOLUME,
            "targetDatasetSizeBytes": MIN_PRODUCTION_DATASET_SIZE_BYTES,
            "minReplayHours": int(MIN_REPLAY_HOURS),
        },
    )


def overall_status(checklist: list[dict[str, Any]]) -> str:
    statuses = [item["status"] for item in checklist]
    if "FAILED" in statuses:
        return "FAILED"
    if "BLOCKED" in statuses:
        return "BLOCKED"
    return "PASSED"


def audit(root: Path, verification_result: Path | None, evidence_dir: Path | None) -> dict[str, Any]:
    checklist = [
        check_research(root),
        check_rewrite_rule_coverage(root),
        check_recommendation_gate(root),
        check_external_verification_result(verification_result, evidence_dir),
    ]
    status = overall_status(checklist)
    return {
        "objective": (
            "SQL rewrite recommendation readiness: latest research, >=36 SELECT rewrite types, "
            "production-scale BI evidence for 30PB storage and 10M daily queries."
        ),
        "overallStatus": status,
        "complete": status == "PASSED",
        "checklist": checklist,
    }


def json_safe(value: Any) -> Any:
    if isinstance(value, Decimal):
        if value == value.to_integral_value():
            return int(value)
        return float(value)
    if isinstance(value, dict):
        return {key: json_safe(item) for key, item in value.items()}
    if isinstance(value, list):
        return [json_safe(item) for item in value]
    return value


def write_payload(payload: dict[str, Any], output: Path | None) -> None:
    rendered = json.dumps(json_safe(payload), ensure_ascii=False, indent=2, sort_keys=True)
    if output is None:
        print(rendered)
        return
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(rendered + "\n", encoding="utf-8")
    print(f"已写入改写生产就绪审计结果: {output}")


def write_self_test_evidence_dir(evidence_dir: Path) -> None:
    evidence_dir.mkdir(parents=True, exist_ok=True)
    (evidence_dir / "concurrency.json").write_text(
        '{"observedConcurrency":10000,"proofRef":"concurrency.log"}',
        encoding="utf-8",
    )
    (evidence_dir / "daily-query-volume.json").write_text(
        '{"observedDailyQueryVolume":10000000,"proofRef":"daily-query-volume.log"}',
        encoding="utf-8",
    )
    (evidence_dir / "data-layout.json").write_text(
        '{"observedDatasetSizeBytes":30000000000000000,"proofRef":"data-layout.json"}',
        encoding="utf-8",
    )
    (evidence_dir / "workload-replay.json").write_text(
        '{"workloadReplayDurationHours":24,'
        '"workloadReplayWindow":"2026-05-17T00:00Z/2026-05-18T00:00Z",'
        '"proofRef":"replay.log"}',
        encoding="utf-8",
    )
    (evidence_dir / "metrics.csv").write_text(
        "p95_latency_ms,p99_latency_ms,scanned_bytes,cpu_usage_percent,queue_wait_ms\n"
        "120,240,9876543210,72.5,8\n",
        encoding="utf-8",
    )
    (evidence_dir / "cost-bill.json").write_text(
        '{"costBillAmount":12345.67,"costBillCurrency":"USD","proofRef":"cost-bill.csv"}',
        encoding="utf-8",
    )


def successful_verification_payload(evidence_file_digests: dict[str, dict[str, Any]]) -> dict[str, Any]:
    payload = {
        "status": "PASSED",
        "externalVerificationStatus": "VERIFIED",
        "evidenceFileDigests": json.loads(json.dumps(evidence_file_digests)),
        "missingEvidence": [],
        "parseErrors": [],
        "scaleTargetEvidenceManifest": {
            "evidenceSource": "PRODUCTION_EVIDENCE_DIRECTORY",
            "concurrencyProofRef": "concurrency.log",
            "dailyQueryVolumeProofRef": "daily-query-volume.log",
            "dataLayoutProofRef": "data-layout.json",
            "workloadReplayProofRef": "replay.log",
            "workloadReplayWindow": "2026-05-17T00:00Z/2026-05-18T00:00Z",
            "p95P99MetricProofRef": "metrics.csv",
            "scanCpuQueueMetricProofRef": "metrics.csv",
            "costBillProofRef": "cost-bill.csv",
            "externalVerificationStatus": "VERIFIED",
            "verificationBundle": {
                "observedConcurrency": MIN_PRODUCTION_CONCURRENCY,
                "observedDailyQueryVolume": MIN_PRODUCTION_DAILY_QUERY_VOLUME,
                "observedDatasetSizeBytes": MIN_PRODUCTION_DATASET_SIZE_BYTES,
                "workloadReplayDurationHours": int(MIN_REPLAY_HOURS),
                "p95LatencyMs": 120,
                "p99LatencyMs": 240,
                "scannedBytes": 9876543210,
                "cpuUsagePercent": 72.5,
                "queueWaitMs": 8,
                "costBillAmount": 12345.67,
                "costBillCurrency": "USD",
                "verifierRef": "prod-run/verifier.json",
            },
        },
    }
    payload["scaleTargetEvidenceManifest"]["evidenceFileDigests"] = json.loads(
        json.dumps(payload["evidenceFileDigests"])
    )
    return payload


def run_self_test() -> int:
    blocked = audit(REPO_ROOT, None, None)
    assert blocked["overallStatus"] == "BLOCKED", blocked
    with tempfile.TemporaryDirectory(prefix="rewrite-readiness-") as temp:
        evidence_dir = Path(temp) / "evidence"
        write_self_test_evidence_dir(evidence_dir)
        evidence_file_digests = collect_raw_evidence_file_digests(evidence_dir)
        passed_path = Path(temp) / "verification-result.json"
        passed_path.write_text(json.dumps(successful_verification_payload(evidence_file_digests)), encoding="utf-8")
        missing_raw = audit(REPO_ROOT, passed_path, None)
        assert missing_raw["overallStatus"] == "BLOCKED", missing_raw
        passed = audit(REPO_ROOT, passed_path, evidence_dir)
        assert passed["overallStatus"] == "PASSED", passed
        failed_payload = successful_verification_payload(evidence_file_digests)
        failed_payload["scaleTargetEvidenceManifest"]["verificationBundle"].pop("observedDailyQueryVolume")
        failed_path = Path(temp) / "verification-result-missing-daily.json"
        failed_path.write_text(json.dumps(failed_payload), encoding="utf-8")
        failed = audit(REPO_ROOT, failed_path, evidence_dir)
        assert failed["overallStatus"] == "BLOCKED", failed
        failed_ref_payload = successful_verification_payload(evidence_file_digests)
        failed_ref_payload["scaleTargetEvidenceManifest"].pop("costBillProofRef")
        failed_ref_path = Path(temp) / "verification-result-missing-proof-ref.json"
        failed_ref_path.write_text(json.dumps(failed_ref_payload), encoding="utf-8")
        failed_ref = audit(REPO_ROOT, failed_ref_path, evidence_dir)
        assert failed_ref["overallStatus"] == "BLOCKED", failed_ref
        failed_digest_payload = successful_verification_payload(evidence_file_digests)
        failed_digest_payload["evidenceFileDigests"].pop("metrics.csv")
        failed_digest_path = Path(temp) / "verification-result-missing-digest.json"
        failed_digest_path.write_text(json.dumps(failed_digest_payload), encoding="utf-8")
        failed_digest = audit(REPO_ROOT, failed_digest_path, evidence_dir)
        assert failed_digest["overallStatus"] == "BLOCKED", failed_digest
        failed_manifest_digest_payload = successful_verification_payload(evidence_file_digests)
        failed_manifest_digest_payload["scaleTargetEvidenceManifest"]["evidenceFileDigests"].pop("metrics.csv")
        failed_manifest_digest_path = Path(temp) / "verification-result-missing-manifest-digest.json"
        failed_manifest_digest_path.write_text(json.dumps(failed_manifest_digest_payload), encoding="utf-8")
        failed_manifest_digest = audit(REPO_ROOT, failed_manifest_digest_path, evidence_dir)
        assert failed_manifest_digest["overallStatus"] == "BLOCKED", failed_manifest_digest
        failed_mismatch_payload = successful_verification_payload(evidence_file_digests)
        failed_mismatch_payload["scaleTargetEvidenceManifest"]["evidenceFileDigests"]["metrics.csv"]["sha256"] = "6" * 64
        failed_mismatch_path = Path(temp) / "verification-result-mismatched-digest.json"
        failed_mismatch_path.write_text(json.dumps(failed_mismatch_payload), encoding="utf-8")
        failed_mismatch = audit(REPO_ROOT, failed_mismatch_path, evidence_dir)
        assert failed_mismatch["overallStatus"] == "BLOCKED", failed_mismatch
        (evidence_dir / "metrics.csv").write_text("changed\n", encoding="utf-8")
        failed_raw_digest = audit(REPO_ROOT, passed_path, evidence_dir)
        assert failed_raw_digest["overallStatus"] == "BLOCKED", failed_raw_digest
    print("改写生产就绪审计自检通过")
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="审计 SQL 改写推荐在仓库证据与外部生产证据下的就绪状态。")
    parser.add_argument("--verification-result", type=Path, help="verify-benchmark-production-evidence.py 输出的 JSON 文件。")
    parser.add_argument("--evidence-dir", type=Path, help="原始外部生产证据目录，用于复算 evidenceFileDigests。")
    parser.add_argument("--output", type=Path, help="可选的 JSON 审计结果输出路径。")
    parser.add_argument("--self-test", action="store_true", help="运行内置通过与阻断模式检查。")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.self_test:
        return run_self_test()
    payload = audit(REPO_ROOT, args.verification_result, args.evidence_dir)
    write_payload(payload, args.output)
    if payload["overallStatus"] == "PASSED":
        return 0
    if payload["overallStatus"] == "BLOCKED":
        return 2
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
