# Operations Docs

本目录承载 SQLForge 的 foreman 工作流、人类协作约束、本地开发规则和任务关闭要求。

## 阅读顺序

1. [Foreman Workflow](./foreman-workflow.md)
   任务入口、阅读顺序、任务选择、开发循环、上下文收缩/清理、归档和提交闭环。
2. [Human Collaboration](./human-collaboration.md)
   人类与 foreman 的职责边界、`stop` / `continue`、脏工作树、冲突处理和命令可用性约束。
3. [Local Development](./local-development.md)
   当前仓库可用的本地命令、脚本、环境约束、Codex `foreman.py` 入口和验证入口。
4. [Requirements-To-Task Playbook](./requirements-to-task-playbook.md)
   从无 task 开始的治理自动化入口，定义 `governed_intake`、`governed_healthcheck`、requirement normalization、candidate task pack、materialization gate 与 `governed_full_cycle` 流程。
5. [Multi-Agent Playbook](./multi-agent-playbook.md)
   多 agent 协作基础设施的角色边界、manifest/worktree 规则、`autoplan/full-auto/prepare/launch/collect` 脚本用法与 SQLForge demo runbook。
6. [Git And Task Closeout](./git-and-task-closeout.md)
   根 Git 边界、单任务单 commit、task audit、上下文收缩/清理和关闭顺序。
7. [Best Practices](./best-practices.md)
   可泛化工程规则的 canonical 账本。

## 相关台账

- 根级任务台账：`tasks.md`
- 已完成归档：`tasks-done.md`
- 人工决策入口：`INBOX.md`
- 机器参数：`.agent/config.json`
- Codex 项目级配置与 hooks：`.codex/config.toml`、`.codex/hooks.json`
- 验证日志：`docs/quality/validation-log.md`
