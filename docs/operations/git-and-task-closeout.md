# Git And Task Closeout

## Git Rules

- 根目录 `/models/project/codex/SQLForge` 是唯一规范 Git 边界
- 一个已验证任务对应一个 commit
- commit subject 采用 `Conventional Commits + task id`：
  `type(scope): TASK-ID short title`
- 只能 stage 当前任务相关文件
- 禁止 `git add .`、`git add -A`、`git commit -a`
- 禁止 force push 或改写已发布历史
- 不自动 push

## Closeout Order

1. 实现
2. 自审与复盘
3. 验证
4. 同步文档
5. 完成上下文收缩并写回任务证据，同时写定最终 `Commit subject`
6. 将完成任务移入 `tasks-done.md`
7. 运行 `python3 scripts/task_audit.py --check`
8. 创建单任务 commit
9. 立即重新运行必要审计/知识校验，确认当前任务的 `Commit subject` 已进入 Git history
10. 完成上下文清理后再进入下一任务

## Context Contraction And Cleanup

- “上下文收缩”至少要把以下内容回写到权威来源：
  - 已完成范围
  - 验证命令与结果
  - 剩余风险、未决项或人工确认点
  - 下一步与任务状态
- pre-commit 的 `task_audit` 只允许一个例外：当前正在关闭的最新归档任务可以暂时尚未出现在 Git history；除此之外，其余 `tasks-done.md` 条目必须全部已可追溯。
- 上述例外只用于单任务 closeout 的 pre-commit 审计；commit 完成后必须立即复验，届时 `tasks-done.md` 中所有任务的 `Commit subject` 都必须已能在 Git history 中追溯。
- “上下文清理”要求在单任务 commit 后停止沿用上一任务的局部推理，并在下一任务开始前重新按项目阅读顺序建立上下文。
- `/contract`、`/clear` 或类似命令若当前 Codex 运行环境支持，可以作为辅助动作；若不支持，必须通过权威文档回写和重新建立上下文完成等价效果，不得因命令缺失而跳过 closeout。

## Audit Chain

以下内容共同构成审计链：

- `tasks.md`
- `tasks-done.md`
- `INBOX.md`
- `docs/quality/validation-log.md`
- Git history
