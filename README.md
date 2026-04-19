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

当前仓库处于阶段 0 初始化中，后续任务会逐步补齐父 POM、公共模块、服务骨架、前端框架、SQL 初始化脚本与检查脚本。

## 文档索引

- 文档入口：`docs/README.md`
- 架构初始化总文档：`docs/architecture/init.md`
- 规则库：`docs/rules/codex-rules.md`
- Java 规范治理：`docs/quality/alibaba-java-guidelines.md`
- 前后端分离基线：`docs/quality/frontend-backend-separation-baseline.md`
- ADR 目录：`docs/adr/README.md`
- 合规说明：`docs/security/compliance.md`
- 阶段计划：`docs/plans/phase-0-plan.md`
