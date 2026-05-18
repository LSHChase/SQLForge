# Benchmark Production Evidence Runbook

本文定义生产规模压测证据目录的最小格式和校验入口，用于把真实外部 artifacts 转换为 `scaleTarget.evidenceManifest.verificationBundle`。该流程只做校验和 JSON 生成，不会创造生产证据；缺失或不达标时必须保持 `UNVERIFIED`。

## Evidence Directory

外部环境 owner 需要在同一目录保存以下文件：

| File | Required fields | Purpose |
|:---|:---|:---|
| `concurrency.json` | `observedConcurrency`, optional `proofRef` | 证明真实并发数达到目标，默认目标为 `10000`。 |
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
  --output /path/to/production-evidence/verification-result.json
```

自检脚本本身：

```bash
python3 scripts/verify-benchmark-production-evidence.py --self-test
```

通过时输出 `status=PASSED`、`externalVerificationStatus=VERIFIED`，并在 `scaleTargetEvidenceManifest` 下生成可提交到 benchmark task 的 manifest 片段。失败时输出 `status=FAILED`、`externalVerificationStatus=UNVERIFIED`，并列出 `missingEvidence` 和 `parseErrors`；失败输出不得用于声明 READY。

## Submission Boundary

只有满足以下条件，才能把输出 manifest 提交到 `scaleTarget.evidenceManifest`：

- `status` 为 `PASSED`。
- `externalVerificationStatus` 为 `VERIFIED`。
- `missingEvidence` 和 `parseErrors` 均为空。
- 证据目录由外部生产或准生产环境产生，并已按环境留存策略归档。

即使脚本通过，仓库内测试、manifest 或 verifier 也只是验证边界；没有外部 artifacts 时，不得把 10000 并发、30PB、长 replay、P95/P99、扫描/CPU/队列等待或成本账单写成已达成事实。
