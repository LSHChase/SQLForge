# Human Constraint History

本文件为 append-only 历史账本，用于记录人类明确表达的长期规则、边界和执行约束。

## 2026-04-18T00:00:00-06:00

- 初始化导入 SQLForge 架构初始化总文档，建立仓库内文档体系基线。
- 锁定当前规则索引范围为 `R-001` 至 `R-115`，以 `docs/architecture/init.md` 为权威来源。
- 锁定阶段0任务按顺序执行，单任务完成后必须停下等待下一条指令。
- 锁定技术栈：
  - 后端：Java 8、Spring Boot 2.7.x、MyBatis XML、Maven、Lombok、MapStruct
  - 前端：Vue 3、JavaScript、Element Plus 2.4+、Pinia、Vue Router 4、Vite
  - 基础设施：Nacos 2.2.x、Spring Cloud Gateway 3.1.x、Sentinel 1.8.x、XXL-JOB 2.4
  - 数据：MySQL 8.0/TDSQL、Redis 7.x、Kafka 3.6+、Hudi 0.14.0(COW)
  - 部署：华为云私有云，兼容 ARM64 + AMD64，UTF-8 + LF
- 锁定后端强制分层：
  - `controller -> service -> domain -> infrastructure`
  - 领域层无 Spring 注解
  - 包结构为“领域目录 + 分层子目录”
- 规则索引入口：`docs/rules/codex-rules.md`

## 2026-04-18T16:02:46-05:00

- 追加验证规则 `R-116` 至 `R-135` 到 `docs/rules/codex-rules.md`。
- 新增独立查阅文档 `docs/quality/validation-rules.md`，同步收录 `R-116` 至 `R-135`。
- 追加原因：将验证行为固化为规则，防止人工遗漏。

## 2026-04-18T16:02:47-05:00

- 事件：将“人类手动发送验证话术”固化为“Codex自动触发验证规则”。
- 新增规则：`R-136` 至 `R-143`。
- 影响：人类无需记忆验证话术，Codex 完成任务后自动输出验证报告。

## 2026-04-19T00:00:00-05:00

- 事件：追加 Kafka 本地开发抽象模式规则，固化 Kafka 相关开发默认不依赖本地 Kafka 服务。
- 新增规则：`R-144`。
- 约束：
  - Kafka 相关任务必须优先采用数据库模拟模式，保留接口转发与内存队列模式作为特定场景补充。
  - 业务层必须依赖消息抽象接口，禁止直接耦合 Kafka 具体实现。
  - 多环境配置必须支持 `messaging.mode` 切换，并同步维护消息抽象文档与接口契约。
- 文档落点：`docs/architecture/messaging-abstraction.md`。
