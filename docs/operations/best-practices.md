# Best Practices

本文件只保留可泛化、可复用、需要长期执行的工程规则。

## Canonical Rules

- 先读文档与现有实现，再改代码；不要把目标架构写成已实现事实。
- 一个 generalized rule 只保留一个 canonical 表述；新增前先审查并合并近重复规则。
- 任务必须留下可追溯证据：命令、结果、结构化 artifact 或文档落点。
- bug 或风格修复需要写 `Root Cause`、`Cure`、`Generalization`，并在需要时创建后续治理任务。
