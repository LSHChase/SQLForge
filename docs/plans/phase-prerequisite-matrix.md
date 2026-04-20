# SQLForge Phase Prerequisite Matrix

## Summary

本矩阵把各阶段进入执行前必须具备的输入文档、ADR、规则、验证规则和人工确认点显式列出，用于补齐“实现前置条件矩阵”的治理缺口。

## Matrix

| Phase | Required input docs | Required ADRs | Governing rules | Validation anchors | Human confirmation points |
|:---|:---|:---|:---|:---|:---|
| `Phase-A` | `docs/README.md`, `docs/plans/document-truth-baseline.md`, `docs/architecture/init.md`, `docs/rules/codex-rules.md`, `docs/plans/master-execution-plan.md`, `docs/plans/document-gap-matrix.md` | N/A | `R-006`,`R-007`,`R-009`,`R-010`,`R-048`,`R-049`,`R-050`,`R-156`,`R-162`,`R-163` | `R-116`,`R-131`,`R-133`,`R-156`,`R-161` | 若发现需要删除/废弃/重命名历史规则或主文档语义，先进入 `HC` 台账 |
| `Phase-B` | `docs/plans/document-truth-baseline.md`, `docs/plans/phase-0-plan.md`, `docs/deliveries/init-completion.md`, `docs/security/access-control-spec.md`, `docs/plans/phase-prerequisite-matrix.md` | `ADR-001`~`ADR-013` | `R-040`,`R-048`,`R-049`,`R-050`,`R-111`~`R-115`,`R-155`,`R-163` | `R-117`,`R-118`,`R-119`,`R-124`,`R-131`,`R-133` | `HC-002` 阶段0真值、`HC-003` ADR 缺口、`HC-005` 部署文档、`HC-006` 合规差距 |
| `Phase-C` | `docs/architecture/service-capability-map.md`, `docs/architecture/service-interface-contract-baseline.md`, `docs/plans/implementation-readiness.md`, `docs/plans/task-spec-matrix.md`, `docs/plans/task-governance-extension-matrix.md` | `ADR-002`,`ADR-009`,`ADR-012` | `R-018`,`R-020`,`R-021`,`R-022`,`R-067`,`R-068`,`R-111`,`R-112`,`R-126`,`R-144`,`R-163`,`R-164` | `R-116`,`R-117`,`R-118`,`R-119`,`R-120`,`R-121`,`R-128`,`R-144` | 若 `governance-service` 承载跨域过渡实现，必须确认目标服务与迁移边界 |
| `Phase-D` | `docs/architecture/service-capability-map.md`, `docs/architecture/service-interface-contract-baseline.md`, `docs/security/access-control-spec.md`, `docs/plans/implementation-readiness.md`, `docs/plans/task-governance-extension-matrix.md` | `ADR-002`,`ADR-004`,`ADR-005`,`ADR-007`,`ADR-009`,`ADR-012`,`ADR-013` | `R-018`,`R-020`,`R-034`,`R-041`,`R-045`,`R-046`,`R-057`,`R-068`,`R-111`~`R-115`,`R-163`,`R-164` | `R-117`,`R-118`,`R-119`,`R-121`,`R-123`,`R-127`,`R-128`,`R-129`,`R-144` | 涉及服务边界重切、错误码区间变更、共享契约破坏式变更时先进入 `HC` 台账 |
| `Phase-E` | `docs/frontend/design-system.md`, `docs/quality/frontend-backend-separation-baseline.md`, `docs/plans/implementation-readiness.md`, `docs/architecture/service-interface-contract-baseline.md` | `ADR-002` | `R-015`,`R-018`,`R-019`,`R-023`~`R-030`,`R-063`,`R-124`,`R-164` | `R-117`,`R-119`,`R-124`,`R-131`,`R-133` | 若页面移除历史已承诺能力或改变主任务流，需人工确认 |
| `Phase-F` | `docs/deployments/local-setup.md`, `docs/deployments/offline-setup.md`, `docs/deployments/huawei-cloud-setup.md`, `docs/security/compliance.md`, `docs/plans/retrospective-template.md` | `ADR-004`,`ADR-009`,`ADR-010`,`ADR-012` | `R-004`,`R-012`,`R-039`,`R-060`,`R-075`,`R-076`,`R-108`,`R-109`,`R-110`,`R-115`,`R-144`,`R-148`,`R-162`,`R-163` | `R-117`,`R-118`,`R-130`,`R-141`,`R-148`,`R-151`,`R-152` | 涉及生产部署语义、备份恢复目标或跨境/多区域策略调整时先人工确认 |

## Usage Rule

1. 进入阶段前先核对本矩阵，不得只依赖主计划中的自然语言描述。
2. 若阶段依赖文档缺失，应先补文档，不得跳过。
3. 若阶段依赖的 `HC` 项未解决，不得把该阶段标记为 ready。

## Related Documents

- `docs/plans/master-execution-plan.md`
- `docs/plans/implementation-readiness.md`
- `docs/plans/task-spec-matrix.md`
- `docs/plans/task-governance-extension-matrix.md`
