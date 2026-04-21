# Operations Docs

本目录承载 SQLForge 的 foreman 工作流、人类协作约束、本地开发规则和任务关闭要求。

## 阅读顺序

1. [Foreman Workflow](./foreman-workflow.md)
   任务入口、阅读顺序、任务选择、开发循环、上下文收缩/清理、归档和提交闭环。
2. [Human Collaboration](./human-collaboration.md)
   人类与 foreman 的职责边界、`stop` / `continue`、脏工作树、冲突处理和命令可用性约束。
3. [Local Development](./local-development.md)
   当前仓库可用的本地命令、脚本、环境约束和验证入口。
4. [Git And Task Closeout](./git-and-task-closeout.md)
   根 Git 边界、单任务单 commit、task audit、上下文收缩/清理和关闭顺序。
5. [Best Practices](./best-practices.md)
   可泛化工程规则的 canonical 账本。

## 相关台账

- 根级任务台账：`tasks.md`
- 已完成归档：`tasks-done.md`
- 人工决策入口：`INBOX.md`
- 机器参数：`.agent/config.json`
- 验证日志：`docs/quality/validation-log.md`
