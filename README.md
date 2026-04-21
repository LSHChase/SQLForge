# SQLForge

SQLForge 是一个面向企业 BI 与数据中台场景的 SQL 生命周期治理平台，覆盖 SQL 查询入口、解析改写、压测验证、执行治理、审计合规、数据血缘与加速配置等能力。

## 项目简介

- 后端技术栈：Java 8、Spring Boot 2.7.x、MyBatis XML、Maven、Lombok、MapStruct
- 前端技术栈：Vue 3、JavaScript、Element Plus 2.4+、Pinia、Vue Router 4、Vite
- 基础设施：Nacos 2.2.x、Spring Cloud Gateway 3.1.x、Sentinel 1.8.x、XXL-JOB 2.4
- 数据与中间件：MySQL 8.0、Redis 7.x、Kafka 3.6+、Hudi 0.14.0(COW)
- 部署目标：华为云私有云，兼容 ARM64 与 AMD64

## 快速启动

1. 启动基础依赖：`docker compose up -d`
2. 查看可用命令：`make help`
3. 阅读架构与执行规范：`docs/README.md`
4. 查看当前任务台账：`tasks.md`

当前仓库已经具备父 POM、根级前端、公共治理服务基线、查询执行服务骨架、SQL 优化服务提交/轮询 API skeleton、压测引擎服务提交/轮询/报告查询 API skeleton、任务台账、operations 文档和仓库审计脚本；后续工作按主执行计划继续补齐目标服务能力、实现深度与生产就绪项。

## 文档索引

- 文档入口：`docs/README.md`
- 运维协作：`docs/operations/README.md`
- 计划索引：`docs/plans/README.md`
- 主执行计划：`docs/plans/master-execution-plan.md`
- Codex 集成治理蓝图：`docs/plans/codex-governance-integration-blueprint.md`
- 架构初始化总文档：`docs/architecture/init.md`
- 规则库：`docs/rules/codex-rules.md`
- Java 规范治理：`docs/quality/alibaba-java-guidelines.md`
- 前后端分离基线：`docs/quality/frontend-backend-separation-baseline.md`
- ADR 目录：`docs/adr/README.md`
- 合规说明：`docs/security/compliance.md`
- 华为云部署：`docs/deployments/huawei-cloud-setup.md`
- 可观测基线：`docs/deployments/observability-baseline.md`
- 阶段计划：`docs/plans/phase-0-plan.md`
- 任务台账：`tasks.md`
- 完成归档：`tasks-done.md`
