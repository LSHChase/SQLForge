# ADR-008: 永洪 BI 对接协议与 JDBC 驱动自研范围

- Status: Accepted
- Date: 2026-04-19
- Deciders: Human Architect, Codex Implementation Lead
- Technical Story: `Phase-D / 查询接入生态`
- Tags: yonghong, bi, jdbc, integration

## Context

SQLForge 面向 BI 与数据中台场景，需要与外部 BI 工具对接。架构文档把永洪 BI 对接列为显式 ADR，意味着协议兼容、驱动边界和自研范围不能模糊处理。

## Decision Drivers

- BI 集成是项目核心使用场景之一
- 需要明确哪些能力复用标准 JDBC/HTTP 协议，哪些能力由 SQLForge 扩展
- 避免把平台能力和第三方驱动实现混在一起
- 必须控制兼容范围和后续维护成本

## Considered Options

1. 全量自研专有驱动和协议
2. 仅暴露通用 JDBC/HTTP 接口，不做适配
3. 以标准协议为主，针对治理能力提供受控扩展

## Decision

选择“标准协议优先，治理扩展受控暴露”的边界：

- 基础查询访问优先复用标准 JDBC/HTTP 兼容方式
- SQLForge 自研范围聚焦于治理增强能力的鉴权、审计、路由、压测、优化建议和配置管理
- 不将第三方 BI 的全部客户端行为都纳入自研驱动范围

## Consequences

### Positive

- 降低对接成本和学习成本
- 让 BI 工具更容易接入现有查询入口
- 减少为单一 BI 工具过度定制

### Negative

- 某些治理能力需要通过额外 API 或约定传递
- 驱动行为与治理功能之间需要额外文档说明
- 兼容性测试矩阵扩大

### Neutral

- 不改变核心 4 微服务边界
- 不改变主持久化和消息抽象策略

## Compliance Impact

- 身份鉴别：BI 接入请求仍需经过后端身份校验
- 访问控制：BI 账号只能访问授权数据源和租户资源
- 安全审计：BI 发起的查询、导出和配置读取均要审计
- 加密存储：外部连接与凭据不落明文
- 备份恢复：对接配置与鉴权元数据纳入恢复

## Implementation Notes

- 影响模块：查询执行服务、公共管理服务
- 需要定义 BI 接入协议约束和错误码映射
- 需要明确哪些治理能力通过扩展 Header 或额外 API 传递

## Validation

- 单元测试：鉴权、错误码、扩展参数解析
- 集成测试：BI 接入查询和导出场景
- 构建验证：后端模块与适配接口编译
- 文档一致性检查：对接说明与接口契约一致
- 合规自检：BI 用户越权访问被拒绝并审计

## Links

- 相关规则：`R-018`, `R-041`, `R-046`, `R-047`, `R-111`, `R-112`
- 相关计划：`docs/plans/master-execution-plan.md`
- 相关文档：`docs/architecture/init.md`, `docs/security/access-control-spec.md`
