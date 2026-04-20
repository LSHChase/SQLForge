# SQLForge Plans

本目录用于承载 SQLForge 的执行计划、阶段计划、确认台账与后续阶段验收记录。

## 计划阅读顺序

1. [主执行计划](./master-execution-plan.md)
   当前全量执行控制文档，负责汇总事实基线、阶段拆解、Epic/Story/Task 结构、验证矩阵与人工确认项。
2. [阶段0执行计划](./phase-0-plan.md)
   初始化阶段历史计划，保留原任务顺序、依赖关系与验收口径，供阶段真值对齐时引用。
3. [文档覆盖矩阵](./document-coverage-matrix.md)
   用于证明 `docs/` 目录全部文档与归档资料已被主计划纳入，不遗漏。
4. [Task 字段矩阵](./task-spec-matrix.md)
   为主计划中的全部 Task 补齐 Harness Engineering 所要求的 10 项字段。

## 计划治理要求

- 所有计划、阶段、Epic、Story、Task 必须符合 Harness Engineering 的“先固化意图、约束、接口、验证，再执行”要求。
- 所有跨步骤、跨前后端、跨领域工作必须先落到计划文档，再进入实现。
- 所有 Task 必须具备独立验证计划，且验证内容必须真实执行，不得以推测替代。
- 已存在的阶段计划作为历史记录保留；新的全局计划统一追加在本目录，不覆盖历史语义。

## 当前状态

- 当前主控计划文档：`master-execution-plan.md`
- 当前历史阶段文档：`phase-0-plan.md`
- 当前文档全量覆盖证明：`document-coverage-matrix.md`
- 当前 Task 字段补全集：`task-spec-matrix.md`
- 当前未闭合事项：以主执行计划中的 `Human Confirmation Ledger` 为准

## 2026-04-20 新增治理文档

- `document-truth-baseline.md`
  用于隔离当前仓库事实、已确认目标、历史记录和归档原文。
- `implementation-readiness.md`
  用于定义任务开始前的文档消费顺序、主题权威、冲突处理和波次执行顺序。
- `retrospective-template.md`
  用于阶段或复杂批次结束后的标准复盘。
- `document-governance-retrospective-2026-04-20.md`
  本轮文档治理专项的实际复盘记录。

使用方式：

- 判断“现在仓库里到底有什么”时，先读 `document-truth-baseline.md`
- 判断“接下来这轮编码按什么顺序推进”时，读 `implementation-readiness.md`
- 需要在交付后沉淀复盘时，使用 `retrospective-template.md`
- 需要追溯本轮治理专项的实际结果时，读 `document-governance-retrospective-2026-04-20.md`
