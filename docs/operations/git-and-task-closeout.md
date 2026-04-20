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
5. 写回任务证据
6. 运行 `python3 scripts/task_audit.py --check`
7. 将完成任务移入 `tasks-done.md`
8. 创建单任务 commit

## Audit Chain

以下内容共同构成审计链：

- `tasks.md`
- `tasks-done.md`
- `INBOX.md`
- `docs/quality/validation-log.md`
- Git history
