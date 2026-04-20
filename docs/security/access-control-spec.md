# Access Control Spec

本文件补全 SQLForge 的身份鉴别、角色模型、资源授权、数据范围控制、审计要求和阶段化实现口径，作为 `R-111`、`R-112`、`R-113`、`R-114`、`R-115` 的专项实现依据。

## 1. Scope

- 适用对象：
  - 查询执行服务
  - SQL 优化服务
  - 压测引擎服务
  - 公共管理服务
- 适用操作：
  - 登录态请求
  - SQL 提交、压测提交、导出、审计查询、权限变更、加速配置管理
- 不在本文件范围：
  - 第三方 IAM 产品具体采购与部署细节
  - 浏览器端存储策略的 UI 细节

## 2. Security Principles

- 后端权威：所有身份、权限、租户、资源范围判断都以服务端为准
- 默认拒绝：缺失身份、租户、角色、资源授权时一律拒绝
- 显式租户：所有核心请求都必须有显式租户上下文
- 最小权限：用户只授予完成职责所需的最小资源范围
- 全程审计：认证失败、越权失败、成功访问和权限变更都必须审计
- 敏感最小暴露：日志、错误返回和导出不泄露明文敏感信息

## 3. Identity Model

### 3.1 Required identity context

所有受保护请求必须在后端形成以下上下文：

- `tenantId`
- `userId`
- `roleCodes`
- `requestId` / `traceId`
- `authSource`
- `issuedAt` / `expiresAt`

### 3.2 Authentication sources

- 当前阶段允许通过受控 Header / Token 解析建立后端身份上下文
- 生产目标是由统一身份服务或网关前置鉴权后，把可信身份声明传入后端
- 任何情况下，后端都必须再次校验身份上下文完整性和签名/可信来源

### 3.3 Anonymous access

- 默认不允许匿名访问业务接口
- 健康检查、静态资源等极少数公开接口应单独显式列白名单

## 4. Role Model

建议的基础角色如下：

| Role | Scope | Typical capabilities |
|:---|:---|:---|
| `PLATFORM_ADMIN` | 平台级 | 管理平台级配置、全局监控、租户开通 |
| `TENANT_ADMIN` | 租户级 | 管理本租户用户、数据源、配额、加速配置审批 |
| `OPERATOR` | 租户级 | 发起查询、查看历史、提交优化和导出 |
| `ANALYST` | 租户级 | 提交查询、查看解析/优化结果、受限导出 |
| `AUDITOR` | 租户级或平台级 | 查看审计日志、合规报表、恢复记录 |
| `READONLY` | 租户级 | 只读查看授权资源和历史摘要 |

角色扩展规则：

- 平台级角色必须与租户级角色分离
- 单用户可拥有多个角色，但权限按最小并集控制
- 角色变更属于高敏感操作，必须进入审计日志

## 5. Resource Model

访问控制最小资源单元包括：

- `Tenant`
- `DataSource`
- `QueryJob`
- `ParseTask`
- `BenchmarkTask`
- `AccelerationConfig`
- `AuditLog`
- `ExportRecord`
- `LineageGraph`
- `ScheduleJob`

每个资源都必须至少绑定：

- `tenantId`
- `resourceId`
- `resourceType`
- `owner` 或 `createdBy`

## 6. Authorization Model

### 6.1 Evaluation order

后端请求授权按以下顺序执行：

1. 校验请求是否在公开白名单
2. 解析并校验身份凭证
3. 建立 `tenantId`、`userId`、`roleCodes` 上下文
4. 校验请求目标资源是否属于当前租户
5. 校验角色是否具备该操作权限
6. 校验数据范围或数据源范围
7. 记录成功或失败审计日志

### 6.2 Tenant isolation

- 所有核心表和核心资源都必须绑定 `tenantId`
- 跨租户查询默认拒绝
- 平台管理员如需跨租户查看，也必须走显式管理接口并记录审计

### 6.3 Datasource authorization

数据源授权至少包含：

- 可见性：是否能查看数据源元数据
- 使用权：是否能使用该数据源发起查询或压测
- 管理权：是否能更新数据源、连接池、权限范围
- 导出权：是否能导出相关结果或历史

### 6.4 Operation matrix

| Operation | Minimum role | Additional checks |
|:---|:---|:---|
| 查询提交 | `ANALYST` | 数据源授权、SQL 风险规则、租户配额 |
| 压测提交 | `OPERATOR` | 影子环境、只读约束、额外审批 |
| 数据源管理 | `TENANT_ADMIN` | 仅本租户数据源 |
| 审计查询 | `AUDITOR` | 敏感字段脱敏与范围过滤 |
| 角色/权限变更 | `TENANT_ADMIN` 或 `PLATFORM_ADMIN` | 强审计、双重确认建议 |
| 平台配置管理 | `PLATFORM_ADMIN` | 平台级操作审计 |

## 7. SQL Governance Controls

权限控制不只校验“谁可以访问”，还要校验“允许做什么”：

- 默认禁止 DDL、DCL 和高风险写操作进入当前治理边界
- 返回行数、超时时间、导出范围受租户配额约束
- 敏感列访问需触发脱敏或拒绝策略
- 压测必须只读、影子环境优先

## 8. Audit Requirements

以下事件必须进入审计日志：

- 登录、登出、认证失败
- SQL 提交、取消、失败、降级、导出
- 压测提交、取消、报告导出
- 数据源创建、更新、删除
- 角色、权限、配额和租户配置变更
- 审计查询与恢复操作

审计最少字段：

- 时间
- `tenantId`
- `userId`
- 操作类型
- 目标资源
- 结果状态
- 耗时
- `traceId`
- 来源 IP / User-Agent

## 9. Sensitive Data Handling

- 密码、API 密钥、Token 和连接密文禁止明文落库
- 错误响应不得返回明文凭据、数据库密码和内部堆栈细节
- 审计日志记录 SQL 时必须遵守脱敏规则
- 导出结果中的敏感列必须遵守角色与脱敏策略

## 10. Failure Handling

- 未认证：返回统一 JSON 错误，状态码和错误码明确
- 无租户上下文：直接拒绝并审计
- 越权访问：直接拒绝并审计
- 数据源授权缺失：拒绝并返回明确错误原因
- 鉴权系统异常：默认拒绝，不允许 fail-open

## 11. Current Implementation Baseline

当前仓库现状：

- `governance-service` 已通过 `TenantAccessLogic` 和请求上下文建立最小租户校验能力
- 当前占位能力已改为“显式角色门禁 + 显式数据源绑定占位配置 + 默认拒绝”
- 当前 `governance-service` 仅对治理内置数据源 `governance-tenant-config` 提供基线放行，且要求 `PLATFORM_ADMIN` 或 `TENANT_ADMIN`
- 其他数据源访问在当前阶段必须通过 `governance.access-control.placeholder.tenant-datasource-bindings` 显式配置，否则拒绝
- 当前治理服务已固定以下失败错误码：
  - 角色不满足治理访问要求：`20000` `GOVERNANCE_ACCESS_DENIED`
  - 跨租户访问拒绝：`20001` `GOVERNANCE_TENANT_ACCESS_DENIED`
  - 数据源绑定或授权拒绝：`20002` `GOVERNANCE_DATASOURCE_ACCESS_DENIED`
- 当前实现仍属于阶段性基线，不代表最终访问控制已完整交付
- 当前代码重点是“后端必须建立租户上下文”，尚未完成完整角色矩阵、数据源权限矩阵和统一身份服务接入

## 12. Target Completion Definition

当访问控制能力被视为“补充完整”时，至少需要满足：

- 所有受保护接口都经过后端身份鉴别
- 所有核心资源都绑定租户并执行租户隔离
- 角色矩阵与数据源授权矩阵可配置且可验证
- 审计日志覆盖认证失败、越权失败和关键成功操作
- 敏感配置加密存储且不泄露到日志/导出
- 备份恢复文档和演练记录纳入交付

## 13. Validation Scenarios

- 未登录访问受保护接口被拒绝
- 缺失 `tenantId` 的请求被拒绝
- 普通分析用户无法管理数据源
- 普通操作员无法读取高敏感审计详情
- 跨租户资源访问被拒绝
- 已吊销数据源权限的用户无法继续查询
- 压测任务不能直接对生产写路径执行
- 认证失败、越权失败和成功访问都有审计记录

## 14. Related Documents

- `docs/security/compliance.md`
- `docs/architecture/init.md`
- `docs/architecture/messaging-abstraction.md`
- `docs/plans/master-execution-plan.md`
