# SQLForge Document Governance Repair Retrospective 2026-04-20

## 1. Batch Metadata

- Batch / Phase: `DOC-GOV-002`
- Date: `2026-04-20`
- Owner: Codex
- Related plan: `docs/plans/master-execution-plan.md`
- Related ADR: `ADR-002`
- Related validation rules: `R-131`, `R-133`, `R-156`, `R-160`, `R-161`

## 2. Intended Outcome

- Planned scope:
  - 关闭严格核验中剩余的 7 个未完全闭口项
  - 把冲突/缺失矩阵、阶段前置条件矩阵、接口契约基线、逐 Task 扩展治理字段显式成文
  - 补齐 `DOC-GOV-001` / `DOC-GOV-002` 的任务归档、repair 复盘和 git closeout 一致性
- Success criteria:
  - 上述 7 项全部从“部分完成/未完成”变为显式闭口
  - 新增治理文档进入主索引、主计划、coverage 与 lint
  - task audit、文档 lint、构建与测试全部通过
- Non-goals:
  - 不实现新的业务服务代码
  - 不把后续实现缺口误报为本轮文档治理缺口

## 3. Actual Outcome

- What was completed:
  - 新增 `document-gap-matrix.md`
  - 新增 `phase-prerequisite-matrix.md`
  - 新增 `service-interface-contract-baseline.md`
  - 新增 `task-governance-extension-matrix.md`
  - 新增本 repair 复盘文档
  - 追加 `DOC-GOV-001` 与 `DOC-GOV-002` 的任务归档与 git closeout
  - 将新增文档接入主入口、主计划、coverage matrix、repo map 与 lint
- What remained pending:
  - 仅剩实现层缺口：`sqlforge-common` 共享层、查询执行服务、SQL 优化服务、压测引擎服务、访问控制完整代码化
- What changed in the plan:
  - 原计划中的隐式治理要求被进一步拆成显式矩阵和契约表
  - Task 治理字段从核心 10 字段扩展为“10 字段 + 3 个治理补充字段”的双层结构

## 4. Evidence

- Commands executed:
  - `node scripts/lint-repository-knowledge.js`
  - `node scripts/check-frontend-backend-separation.js`
  - `mvn -B test`
  - `npm run build`
  - `npm run lint`
  - `python3 scripts/task_audit.py --check`
- Validation results:
  - 上述命令全部通过
- Key files changed:
  - `docs/plans/document-gap-matrix.md`
  - `docs/plans/phase-prerequisite-matrix.md`
  - `docs/architecture/service-interface-contract-baseline.md`
  - `docs/plans/task-governance-extension-matrix.md`
  - `docs/plans/document-governance-repair-retrospective-2026-04-20.md`
  - `docs/README.md`
  - `docs/plans/README.md`
  - `docs/plans/master-execution-plan.md`
  - `docs/plans/document-coverage-matrix.md`
  - `docs/plans/task-spec-matrix.md`
  - `docs/generated/repo-map.md`
  - `docs/quality/validation-log.md`
  - `docs/references/human-constraint-history.md`
  - `docs/rules/codex-rules.md`
  - `scripts/lint-repository-knowledge.js`
  - `tasks-done.md`
- Delivery artifacts:
  - 7 项严格核验缺口闭口文档
  - 补充治理矩阵和契约基线
  - 完整批次归档与 closeout 证据链

## 5. Drift And Gaps

- Rule drift found:
  - 任务治理只依赖核心 10 字段时，严格治理需求仍有隐式信息
- Document drift found:
  - 冲突/缺失矩阵、阶段前置条件矩阵和接口契约表原先没有单独成文
- Implementation drift found:
  - 服务实现仍明显落后于 4 微服务目标，但已被显式保留为实现缺口
- Security/compliance gaps:
  - 合规文档已完整，代码仍待阶段化落地
- Test gaps:
  - 本次仍属治理收口，没有新增业务行为测试

## 6. Root Cause / Cure / Generalization

- Root Cause:
  - 主计划与专项文档已经较完整，但严格核验要求的“矩阵化闭口”与“批次一致性”仍停留在分散表达
- Cure:
  - 新增矩阵型文档和契约基线，并把治理批次直接接入任务归档与 git history
- Generalization:
  - 以后复杂治理批次结束后，必须同时检查：
    - 是否有显式 gap matrix
    - 是否有 phase prerequisite matrix
    - 是否有 task governance extension
    - 是否有 retro / validation / tasks-done / git history 一致性

## 7. Follow-Up Items

- Next batch candidates:
  - `sqlforge-common` 公共层做实
  - 公共管理服务继续收敛
  - 查询执行 / SQL 优化 / 压测引擎服务骨架落地
- Human confirmations still needed:
  - 若后续实施需要删除历史规则、废弃文档语义或调整错误码归属区间，仍需人工确认
- Tooling or script updates needed:
  - 新增治理文档后继续同步维护 coverage completeness 与 required docs lint
- Docs or ADR updates needed:
  - 后续进入服务实现后，应继续细化真实 DTO 命名、事件版本化和错误码枚举清单
