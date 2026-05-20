# USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520 Raw Requirement

Captured at: 2026-05-20

## Original User Requirement

```text
本项目的所有复杂审批/发布/撤销/回滚流程需要简化，折叠为“激活/暂停”两个核心动作，其如果涉及核心功能的影响，请给出问题及解决方案，等待人类决定。另外改造要彻底点，深入分析文档、规则、要求、代码、脚本、验证等内容进行改造
```

## Governance Handling

- Formal task: `USER-CN-SIMPLIFY-LIFECYCLE-ACTIVATE-PAUSE-20260520`
- Impact analysis: `docs/plans/lifecycle-activation-pause-impact-analysis-2026-05-20.md`
- Human decision item: `INBOX-006`
- Current handling: blocked pending decision because a full collapse of approval, publish, revoke, and rollback semantics affects core runtime safety, audit evidence, DB status compatibility, acceleration-plan rollback evidence, frontend contract checks, and validation scripts.
