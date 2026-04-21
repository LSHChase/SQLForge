# SQLForge Document Governance Retrospective 2026-04-20

## 1. Batch Metadata

- Batch / Phase: `DOC-GOV-001`
- Date: `2026-04-20`
- Owner: Codex
- Related plan: `docs/plans/master-execution-plan.md`
- Related ADR: `ADR-002`
- Related validation rules: `R-131`, `R-133`, `R-156`, `R-160`, `R-161`

## 2. Intended Outcome

- Planned scope:
  - 为本轮治理增加当前真值基线
  - 为后续编码增加实现就绪规范
  - 为 4 微服务目标增加能力分配图
  - 为后续交付增加复盘模板
  - 把新增治理文档接入入口索引、主执行计划、覆盖矩阵、历史账本与验证日志
- Success criteria:
  - 文档体系不再混写“当前事实”和“目标架构”
  - 后续编码前具备明确文档消费顺序
  - `governance` 与 `sqlforge-shared` 的过渡边界被明确写清
  - 所有相关检查命令通过
- Non-goals:
  - 不实现新的业务服务代码
  - 不删除或重写历史规则语义

## 3. Actual Outcome

- What was completed:
  - 新增 `document-truth-baseline.md`
  - 新增 `implementation-readiness.md`
  - 新增 `service-capability-map.md`
  - 新增 `retrospective-template.md`
  - 把新增文档接入 `docs/README.md`、`master-execution-plan.md`、`document-coverage-matrix.md`、`repo-map.md`
  - 在严格复验后补齐 `docs/README.md` 的阅读顺序截断问题
  - 在严格复验后补齐 coverage matrix 对 `operations/`、`generated/`、`quality/validation-log.md` 与 `exec-plans/` 占位文件的缺失收录
  - 在严格复验后扩展 `scripts/lint-repository-knowledge.js`，让新增治理文档与 coverage 完整性进入脚本校验
  - 为本批次补写本复盘文档
- What remained pending:
  - `sqlforge-shared` 仍未承载真实公共源码
  - 查询执行服务、SQL 优化服务、压测引擎服务仍未建立独立代码模块
  - 访问控制完整实现仍待后续服务化落地
- What changed in the plan:
  - 原先只新增复盘模板，现补充了一份本次治理的实际复盘实例
  - 原先只在附录说明新增治理文档，现补充到主阅读顺序、主计划证据集和追踪矩阵
  - 原先未把新增治理文档完全纳入脚本校验，现已补齐脚本覆盖

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
  - `docs/README.md`
  - `docs/architecture/init.md`
  - `docs/architecture/service-capability-map.md`
  - `docs/plans/document-truth-baseline.md`
  - `docs/plans/implementation-readiness.md`
  - `docs/plans/document-coverage-matrix.md`
  - `docs/plans/master-execution-plan.md`
  - `docs/plans/retrospective-template.md`
  - `docs/plans/document-governance-retrospective-2026-04-20.md`
  - `docs/references/human-constraint-history.md`
  - `docs/quality/validation-log.md`
  - `scripts/lint-repository-knowledge.js`
- Delivery artifacts:
  - 当前真值基线
  - 实现就绪规范
  - 服务能力分配图
  - 复盘模板与本批次复盘记录

## 5. Drift And Gaps

- Rule drift found:
  - 初始化文档中的部分目标落点路径与当前仓库真实权威文档不一致
- Document drift found:
  - 新增治理文档最初只在增量附录中声明，尚未完全进入主阅读顺序与主证据集；该问题已在严格复验后补齐
- Implementation drift found:
  - 当前模块结构仍落后于已确认的 4 微服务目标
- Security/compliance gaps:
  - 访问控制仍停留在最小租户校验基线
- Test gaps:
  - 本轮只有文档治理验证，没有新增业务行为测试

## 6. Root Cause / Cure / Generalization

- Root Cause:
  - 初始化文档承担了太多“规则基线 + 目标落点 + 历史要求”，随着后续文档拆分，容易出现消费路径漂移
- Cure:
  - 增加真值基线文档和实现就绪文档，并把它们接入主索引和主计划
- Generalization:
  - 以后新增治理文档时，不能只追加“增量说明”，必须同步更新：
    - 主阅读顺序
    - 主计划证据集
    - 文档覆盖矩阵
    - 验证日志
    - 必要时的复盘实例

## 7. Follow-Up Items

- Next batch candidates:
  - `sqlforge-shared` 公共层做实
  - `governance` 向公共管理服务边界继续收敛
  - 新建查询执行服务骨架
- Human confirmations still needed:
  - 若后续要删除、废弃或重命名历史规则落点，仍需人工确认
- Tooling or script updates needed:
  - 后续新增治理文档时，继续同步维护 `lint-repository-knowledge.js` 的检查集合与 coverage 完整性校验
- Docs or ADR updates needed:
  - 后续服务拆分开始时，应继续细化服务级接口契约和模块边界
