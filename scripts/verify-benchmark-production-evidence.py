#!/usr/bin/env python3
"""校验外部压测生产规模证据并输出 manifest JSON。"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import sys
import tempfile
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any


MIN_PRODUCTION_CONCURRENCY = 10000
MIN_PRODUCTION_DAILY_QUERY_VOLUME = 10000000
MIN_PRODUCTION_DATASET_SIZE_BYTES = 30000000000000000
MIN_LONG_REPLAY_HOURS = Decimal("24")
REQUIRED_EVIDENCE_FILES = (
    "concurrency.json",
    "daily-query-volume.json",
    "data-layout.json",
    "workload-replay.json",
    "metrics.csv",
    "cost-bill.json",
)


class EvidenceError(ValueError):
    """Raised when an evidence artifact cannot be parsed."""


def parse_decimal(value: Any, field_name: str) -> Decimal:
    if value is None or str(value).strip() == "":
        raise EvidenceError(f"{field_name} 为必填项")
    try:
        return Decimal(str(value).strip())
    except (InvalidOperation, ValueError) as exc:
        raise EvidenceError(f"{field_name} 必须是数值：{value}") from exc


def parse_int(value: Any, field_name: str) -> int:
    parsed = parse_decimal(value, field_name)
    if parsed != parsed.to_integral_value():
        raise EvidenceError(f"{field_name} 必须是整数：{value}")
    return int(parsed)


def read_json(path: Path) -> dict[str, Any]:
    if not path.is_file():
        raise EvidenceError(f"缺少文件：{path.name}")
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise EvidenceError(f"{path.name} 不是有效 JSON：{exc}") from exc
    if not isinstance(data, dict):
        raise EvidenceError(f"{path.name} 必须包含 JSON 对象")
    return data


def read_metrics_csv(path: Path) -> dict[str, Decimal | int]:
    if not path.is_file():
        raise EvidenceError(f"缺少文件：{path.name}")
    with path.open("r", encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle)
        required = {"p95_latency_ms", "p99_latency_ms", "scanned_bytes", "cpu_usage_percent", "queue_wait_ms"}
        if reader.fieldnames is None:
            raise EvidenceError(f"{path.name} 必须包含表头行")
        missing_columns = sorted(required.difference(set(reader.fieldnames)))
        if missing_columns:
            raise EvidenceError(f"{path.name} 缺少列：{', '.join(missing_columns)}")
        rows = list(reader)
    if not rows:
        raise EvidenceError(f"{path.name} 必须至少包含一行数据")

    p95 = max(parse_decimal(row.get("p95_latency_ms"), "p95_latency_ms") for row in rows)
    p99 = max(parse_decimal(row.get("p99_latency_ms"), "p99_latency_ms") for row in rows)
    scanned = max(parse_int(row.get("scanned_bytes"), "scanned_bytes") for row in rows)
    cpu = max(parse_decimal(row.get("cpu_usage_percent"), "cpu_usage_percent") for row in rows)
    queue = max(parse_decimal(row.get("queue_wait_ms"), "queue_wait_ms") for row in rows)
    return {
        "p95LatencyMs": p95,
        "p99LatencyMs": p99,
        "scannedBytes": scanned,
        "cpuUsagePercent": cpu,
        "queueWaitMs": queue,
    }


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def collect_evidence_file_digests(evidence_dir: Path) -> dict[str, dict[str, Any]]:
    digests: dict[str, dict[str, Any]] = {}
    for file_name in REQUIRED_EVIDENCE_FILES:
        path = evidence_dir / file_name
        if path.is_file():
            digests[file_name] = {
                "sha256": sha256_file(path),
                "sizeBytes": path.stat().st_size,
            }
    return digests


def require_positive_decimal(value: Decimal, evidence_name: str, missing: list[str]) -> None:
    if value <= Decimal("0"):
        missing.append(f"{evidence_name}:required>0,actual={value}")


def evaluate_evidence_dir(evidence_dir: Path,
                          target_concurrency: int,
                          min_daily_query_volume: int,
                          min_dataset_size_bytes: int,
                          min_replay_hours: Decimal) -> dict[str, Any]:
    missing: list[str] = []
    parse_errors: list[str] = []
    evidence_file_digests = collect_evidence_file_digests(evidence_dir)
    manifest_refs: dict[str, str | None] = {
        "concurrencyProofRef": None,
        "dailyQueryVolumeProofRef": None,
        "dataLayoutProofRef": None,
        "workloadReplayProofRef": None,
        "p95P99MetricProofRef": None,
        "scanCpuQueueMetricProofRef": None,
        "costBillProofRef": None,
    }
    bundle: dict[str, Any] = {}
    workload_window = None

    try:
        concurrency = read_json(evidence_dir / "concurrency.json")
        observed_concurrency = parse_int(concurrency.get("observedConcurrency"), "observedConcurrency")
        bundle["observedConcurrency"] = observed_concurrency
        manifest_refs["concurrencyProofRef"] = str(concurrency.get("proofRef") or "concurrency.json")
        if observed_concurrency < target_concurrency:
            missing.append(f"productionEvidenceBundle.concurrency:required={target_concurrency},actual={observed_concurrency}")
    except EvidenceError as exc:
        parse_errors.append(str(exc))
        missing.append("productionEvidenceBundle.concurrency")

    try:
        daily_query_volume = read_json(evidence_dir / "daily-query-volume.json")
        observed_daily_query_volume = parse_int(
            daily_query_volume.get("observedDailyQueryVolume"),
            "observedDailyQueryVolume",
        )
        bundle["observedDailyQueryVolume"] = observed_daily_query_volume
        manifest_refs["dailyQueryVolumeProofRef"] = str(
            daily_query_volume.get("proofRef") or "daily-query-volume.json"
        )
        if observed_daily_query_volume < min_daily_query_volume:
            missing.append(
                "productionEvidenceBundle.dailyQueryVolume:"
                f"required={min_daily_query_volume},actual={observed_daily_query_volume}"
            )
    except EvidenceError as exc:
        parse_errors.append(str(exc))
        missing.append("productionEvidenceBundle.dailyQueryVolume")

    try:
        data_layout = read_json(evidence_dir / "data-layout.json")
        dataset_size = parse_int(data_layout.get("observedDatasetSizeBytes"), "observedDatasetSizeBytes")
        bundle["observedDatasetSizeBytes"] = dataset_size
        manifest_refs["dataLayoutProofRef"] = str(data_layout.get("proofRef") or "data-layout.json")
        if dataset_size < min_dataset_size_bytes:
            missing.append(
                "productionEvidenceBundle.dataLayout30Pb:"
                f"requiredBytes={min_dataset_size_bytes},actual={dataset_size}"
            )
    except EvidenceError as exc:
        parse_errors.append(str(exc))
        missing.append("productionEvidenceBundle.dataLayout30Pb")

    try:
        replay = read_json(evidence_dir / "workload-replay.json")
        replay_hours = parse_decimal(replay.get("workloadReplayDurationHours"), "workloadReplayDurationHours")
        bundle["workloadReplayDurationHours"] = replay_hours
        workload_window = str(replay.get("workloadReplayWindow") or "").strip() or None
        manifest_refs["workloadReplayProofRef"] = str(replay.get("proofRef") or "workload-replay.json")
        if replay_hours < min_replay_hours:
            missing.append(
                "productionEvidenceBundle.longReplay:"
                f"requiredHours={min_replay_hours},actual={replay_hours}"
            )
        if workload_window is None:
            missing.append("productionEvidenceBundle.workloadReplayWindow")
    except EvidenceError as exc:
        parse_errors.append(str(exc))
        missing.append("productionEvidenceBundle.longReplay")

    try:
        metrics = read_metrics_csv(evidence_dir / "metrics.csv")
        bundle.update(metrics)
        manifest_refs["p95P99MetricProofRef"] = "metrics.csv"
        manifest_refs["scanCpuQueueMetricProofRef"] = "metrics.csv"
        require_positive_decimal(metrics["p95LatencyMs"], "productionEvidenceBundle.p95Latency", missing)
        require_positive_decimal(metrics["p99LatencyMs"], "productionEvidenceBundle.p99Latency", missing)
        if int(metrics["scannedBytes"]) <= 0:
            missing.append(f"productionEvidenceBundle.scanBytes:required>0,actual={metrics['scannedBytes']}")
        require_positive_decimal(metrics["cpuUsagePercent"], "productionEvidenceBundle.cpu", missing)
        if metrics["queueWaitMs"] < Decimal("0"):
            missing.append(f"productionEvidenceBundle.queueWait:required>=0,actual={metrics['queueWaitMs']}")
    except EvidenceError as exc:
        parse_errors.append(str(exc))
        missing.append("productionEvidenceBundle.p95P99Latency")
        missing.append("productionEvidenceBundle.scanCpuQueue")

    try:
        cost_bill = read_json(evidence_dir / "cost-bill.json")
        cost_amount = parse_decimal(cost_bill.get("costBillAmount"), "costBillAmount")
        currency = str(cost_bill.get("costBillCurrency") or "").strip()
        bundle["costBillAmount"] = cost_amount
        bundle["costBillCurrency"] = currency
        manifest_refs["costBillProofRef"] = str(cost_bill.get("proofRef") or "cost-bill.json")
        require_positive_decimal(cost_amount, "productionEvidenceBundle.costBillAmount", missing)
        if not currency:
            missing.append("productionEvidenceBundle.costBillCurrency")
    except EvidenceError as exc:
        parse_errors.append(str(exc))
        missing.append("productionEvidenceBundle.costBill")

    status = "PASSED" if not missing and not parse_errors else "FAILED"
    external_status = "VERIFIED" if status == "PASSED" else "UNVERIFIED"
    verifier_ref = "benchmark-production-evidence-verifier:local-script"
    bundle["verifierRef"] = verifier_ref

    manifest = {
        "evidenceSource": "PRODUCTION_EVIDENCE_DIRECTORY",
        "concurrencyProofRef": manifest_refs["concurrencyProofRef"],
        "dailyQueryVolumeProofRef": manifest_refs["dailyQueryVolumeProofRef"],
        "dataLayoutProofRef": manifest_refs["dataLayoutProofRef"],
        "workloadReplayProofRef": manifest_refs["workloadReplayProofRef"],
        "workloadReplayWindow": workload_window,
        "p95P99MetricProofRef": manifest_refs["p95P99MetricProofRef"],
        "scanCpuQueueMetricProofRef": manifest_refs["scanCpuQueueMetricProofRef"],
        "costBillProofRef": manifest_refs["costBillProofRef"],
        "externalVerificationStatus": external_status,
        "evidenceFileDigests": evidence_file_digests,
        "verificationBundle": bundle,
    }
    return {
        "status": status,
        "externalVerificationStatus": external_status,
        "thresholds": {
            "targetConcurrency": target_concurrency,
            "minDailyQueryVolume": min_daily_query_volume,
            "minDatasetSizeBytes": min_dataset_size_bytes,
            "minReplayHours": min_replay_hours,
        },
        "evidenceFileDigests": evidence_file_digests,
        "missingEvidence": sorted(set(missing)),
        "parseErrors": parse_errors,
        "scaleTargetEvidenceManifest": manifest,
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


def write_json(payload: dict[str, Any], output_path: Path | None) -> None:
    rendered = json.dumps(json_safe(payload), ensure_ascii=False, indent=2, sort_keys=True)
    if output_path is None:
        print(rendered)
        return
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(rendered + "\n", encoding="utf-8")
    print(f"已写入压测生产证据校验结果：{output_path}")


def write_fixture(path: Path, name: str, payload: str) -> None:
    (path / name).write_text(payload, encoding="utf-8")


def run_self_test() -> int:
    with tempfile.TemporaryDirectory(prefix="benchmark-evidence-") as temp:
        evidence_dir = Path(temp)
        write_fixture(evidence_dir, "concurrency.json", '{"observedConcurrency": 10000, "proofRef": "concurrency.log"}')
        write_fixture(
            evidence_dir,
            "daily-query-volume.json",
            '{"observedDailyQueryVolume": 10000000, "proofRef": "daily-query-volume.log"}',
        )
        write_fixture(
            evidence_dir,
            "data-layout.json",
            '{"observedDatasetSizeBytes": 30000000000000000, "proofRef": "data-layout.json"}',
        )
        write_fixture(
            evidence_dir,
            "workload-replay.json",
            '{"workloadReplayDurationHours": 24, '
            '"workloadReplayWindow": "2026-05-17T00:00Z/2026-05-18T00:00Z", '
            '"proofRef": "replay.log"}',
        )
        write_fixture(
            evidence_dir,
            "metrics.csv",
            "p95_latency_ms,p99_latency_ms,scanned_bytes,cpu_usage_percent,queue_wait_ms\n"
            "120,240,9876543210,72.5,8\n",
        )
        write_fixture(
            evidence_dir,
            "cost-bill.json",
            '{"costBillAmount": 12345.67, "costBillCurrency": "USD", "proofRef": "cost-bill.csv"}',
        )
        passed = evaluate_evidence_dir(
            evidence_dir,
            MIN_PRODUCTION_CONCURRENCY,
            MIN_PRODUCTION_DAILY_QUERY_VOLUME,
            MIN_PRODUCTION_DATASET_SIZE_BYTES,
            MIN_LONG_REPLAY_HOURS,
        )
        assert passed["status"] == "PASSED", passed
        assert passed["externalVerificationStatus"] == "VERIFIED", passed
        assert set(passed["evidenceFileDigests"].keys()) == set(REQUIRED_EVIDENCE_FILES), passed
        assert len(passed["evidenceFileDigests"]["metrics.csv"]["sha256"]) == 64, passed

        write_fixture(evidence_dir, "concurrency.json", '{"observedConcurrency": 9999}')
        failed = evaluate_evidence_dir(
            evidence_dir,
            MIN_PRODUCTION_CONCURRENCY,
            MIN_PRODUCTION_DAILY_QUERY_VOLUME,
            MIN_PRODUCTION_DATASET_SIZE_BYTES,
            MIN_LONG_REPLAY_HOURS,
        )
        assert failed["status"] == "FAILED", failed
        assert failed["externalVerificationStatus"] == "UNVERIFIED", failed
        assert any("concurrency" in item for item in failed["missingEvidence"]), failed
        write_fixture(evidence_dir, "concurrency.json", '{"observedConcurrency": 10000}')
        write_fixture(evidence_dir, "daily-query-volume.json", '{"observedDailyQueryVolume": 9999999}')
        failed_daily_volume = evaluate_evidence_dir(
            evidence_dir,
            MIN_PRODUCTION_CONCURRENCY,
            MIN_PRODUCTION_DAILY_QUERY_VOLUME,
            MIN_PRODUCTION_DATASET_SIZE_BYTES,
            MIN_LONG_REPLAY_HOURS,
        )
        assert failed_daily_volume["status"] == "FAILED", failed_daily_volume
        assert any("dailyQueryVolume" in item for item in failed_daily_volume["missingEvidence"]), failed_daily_volume
        override_daily_volume = evaluate_evidence_dir(
            evidence_dir,
            MIN_PRODUCTION_CONCURRENCY,
            9999999,
            MIN_PRODUCTION_DATASET_SIZE_BYTES,
            MIN_LONG_REPLAY_HOURS,
        )
        assert override_daily_volume["status"] == "PASSED", override_daily_volume
    print("压测生产证据校验器自检通过")
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="校验外部压测生产证据，并输出 scaleTarget.evidenceManifest JSON。"
    )
    parser.add_argument("--evidence-dir", type=Path, help="包含生产证据 artifacts 的目录。")
    parser.add_argument("--output", type=Path, help="可选的 JSON 校验结果输出路径。")
    parser.add_argument("--target-concurrency", type=int, default=MIN_PRODUCTION_CONCURRENCY)
    parser.add_argument("--min-daily-query-volume", type=int, default=MIN_PRODUCTION_DAILY_QUERY_VOLUME)
    parser.add_argument("--min-dataset-size-bytes", type=int, default=MIN_PRODUCTION_DATASET_SIZE_BYTES)
    parser.add_argument("--min-replay-hours", type=Decimal, default=MIN_LONG_REPLAY_HOURS)
    parser.add_argument("--self-test", action="store_true", help="运行内置校验器自检。")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.self_test:
        return run_self_test()
    if args.evidence_dir is None:
        print("除非使用 --self-test，否则必须提供 --evidence-dir。", file=sys.stderr)
        return 2
    payload = evaluate_evidence_dir(
        args.evidence_dir,
        args.target_concurrency,
        args.min_daily_query_volume,
        args.min_dataset_size_bytes,
        args.min_replay_hours,
    )
    write_json(payload, args.output)
    return 0 if payload["status"] == "PASSED" else 1


if __name__ == "__main__":
    raise SystemExit(main())
