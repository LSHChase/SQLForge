# Benchmark Production Evidence Runbook

本文定义生产规模压测证据目录的最小格式和校验入口，用于把真实外部 artifacts 转换为 `scaleTarget.evidenceManifest.verificationBundle`。该流程只做校验和 JSON 生成，不会创造生产证据；缺失或不达标时必须保持 `UNVERIFIED`。

## Evidence Directory

外部环境 owner 需要在同一目录保存以下文件：

| File | Required fields | Purpose |
|:---|:---|:---|
| `provenance.json` | `environmentId`, `environmentType`, `evidenceOwner`, `artifactArchiveRef`, `verifierOperator` | 证明证据来自已确认的生产或准生产环境、归档位置和执行人；`environmentType` 只接受 `PRODUCTION` 或 `PRE_PRODUCTION`。 |
| `concurrency.json` | `observedConcurrency`, optional `proofRef` | 证明真实并发数达到目标，默认目标为 `10000`。 |
| `daily-query-volume.json` | `observedDailyQueryVolume`, optional `proofRef` | 证明真实日查询量达到千万级，默认目标为 `10000000`。 |
| `data-layout.json` | `observedDatasetSizeBytes`, optional `proofRef` | 证明数据布局达到 30PB 字节级规模，默认阈值为 `30000000000000000`。 |
| `workload-replay.json` | `workloadReplayDurationHours`, `workloadReplayWindow`, optional `proofRef` | 证明长期 workload replay，默认窗口不少于 24 小时。 |
| `metrics.csv` | `p95_latency_ms`, `p99_latency_ms`, `scanned_bytes`, `cpu_usage_percent`, `queue_wait_ms` | 证明 P95/P99、扫描字节、CPU 和队列等待。多行时校验入口使用最大值作为保守证据。 |
| `cost-bill.json` | `costBillAmount`, `costBillCurrency`, optional `proofRef` | 证明成本账单存在且金额大于 0。 |

`proofRef` 应指向外部证据归档位置或同目录文件名。若未提供，脚本使用对应文件名作为引用。真实日志、账单、截图或对象存储 URI 不应直接改写为测试值。

## Verify

执行：

```bash
python3 scripts/verify-benchmark-production-evidence.py \
  --evidence-dir /path/to/production-evidence \
  --target-concurrency 10000 \
  --min-daily-query-volume 10000000 \
  --min-dataset-size-bytes 30000000000000000 \
  --min-replay-hours 24 \
  --output /path/to/production-evidence/verification-result.json
```

本目标必须使用上述默认生产阈值。`--min-daily-query-volume` 只让外部 owner 在命令行上显式确认千万级日查询要求；SQL 推荐改写完成度审计仍固定要求 `observedDailyQueryVolume >= 10000000`。

自检脚本本身：

```bash
python3 scripts/verify-benchmark-production-evidence.py --self-test
```

通过时输出 `status=PASSED`、`externalVerificationStatus=VERIFIED`，在 `scaleTargetEvidenceManifest` 下生成可提交到 benchmark task 的 manifest 片段，并在顶层和 manifest 内的 `evidenceFileDigests` 记录每个必需证据文件的 `sha256` 与 `sizeBytes`。manifest 还必须携带 `environmentId`、`environmentType`、`evidenceOwner`、`artifactArchiveRef` 和 `verifierOperator`，用于说明外部证据来源与归档责任。失败时输出 `status=FAILED`、`externalVerificationStatus=UNVERIFIED`，并列出 `missingEvidence` 和 `parseErrors`；失败输出不得用于声明 READY。

验证 SQL 推荐改写目标的完整完成度时，再执行：

```bash
python3 scripts/audit-rewrite-production-readiness.py \
  --verification-result /path/to/production-evidence/verification-result.json \
  --evidence-dir /path/to/production-evidence \
  --output /path/to/production-evidence/rewrite-readiness-audit.json
```

该审计会同时检查推荐改写调研归档、50+ SELECT 规则覆盖、`productionScaleGate` 和外部 `VERIFIED` 证据。没有 `verification-result.json`、没有原始 `--evidence-dir`、缺少 `provenance.json` 或千万级日查询等任一外部证据、缺少来源元数据、manifest 来源元数据与原始 `provenance.json` 不一致、顶层与 manifest 内的 `evidenceFileDigests` 不一致，或复算原始文件 SHA-256/sizeBytes 不匹配时，输出 `overallStatus=BLOCKED`，不得把目标标记为完成。

输出中的 `completionAudit.promptToArtifactChecklist` 是最终完成度复核清单，会把用户目标逐项映射到调研文档、核心改写逻辑、覆盖测试、生产规模 gate 和外部证据 artifacts。只有 `completionAudit.completionDecision=ACHIEVED` 且 `missingOrWeakEvidence` 为空时，才允许声明该 SQL 推荐改写目标已完成。

归档 `verification-result.json` 时必须同时保存原始 evidence directory。评审者可用 `evidenceFileDigests` 对照原始文件重新计算 SHA-256；若缺少任一必需文件摘要，完成度审计必须保持 `BLOCKED`。

## Submission Boundary

只有满足以下条件，才能把输出 manifest 提交到 `scaleTarget.evidenceManifest`：

- `status` 为 `PASSED`。
- `externalVerificationStatus` 为 `VERIFIED`。
- `missingEvidence` 和 `parseErrors` 均为空。
- `provenance.json` 已归档，且 manifest 保留 `environmentId`、`environmentType`、`evidenceOwner`、`artifactArchiveRef`、`verifierOperator`。
- 证据目录由外部生产或准生产环境产生，并已按环境留存策略归档。

即使脚本通过，仓库内测试、manifest 或 verifier 也只是验证边界；没有外部 artifacts 时，不得把 10000 并发、千万级日查询、30PB、长 replay、P95/P99、扫描/CPU/队列等待或成本账单写成已达成事实。
