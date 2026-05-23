# Access Control Spec

本文件补全 SQLForge 的身份鉴别、租户隔离、数据源范围控制、执行留痕和阶段化实现口径，作为 `R-111`、`R-112`、`R-113`、`R-114`、`R-115` 的专项实现依据。

自 `USER-CN-REMOVE-MULTI-ROLE-PERMISSION-CORE-20260523` 起，本文不再定义命名化岗位体系，也不再保留独立访问原则章节。旧请求头、旧岗位上下文字段、旧访问矩阵服务和旧内部授权路径已按影响分析与人工确认删除；当前边界由身份、租户、数据源范围、执行留痕和敏感字段保护承载。

## 1. Scope

- 适用对象：
  - 查询执行服务
  - SQL 优化服务
  - 压测引擎服务
  - 公共管理服务
- 适用操作：
  - 登录态请求
  - SQL 提交、压测提交、导出、访问范围变更、加速配置管理
- 不在本文件范围：
  - 第三方 IAM 产品具体采购与部署细节
  - 浏览器端存储策略的 UI 细节

## 2. Execution Safety Boundaries

- 后端权威：所有身份、租户、资源范围和执行风险判断都以服务端为准
- 默认拒绝：缺失身份、租户、资源范围或数据源范围时一律拒绝
- 显式租户：所有核心请求都必须有显式租户上下文
- 最小访问面：请求只能触达本次执行所需的最小资源范围
- 全程留痕：认证失败、越界失败、成功访问和访问范围变更都必须记录
- 敏感最小暴露：日志、错误返回和导出不泄露明文敏感信息

## 3. Identity Model

### 3.1 Required identity context

所有受保护请求必须在后端形成以下上下文：

- `tenantId`
- `userId`
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

## 4. No Product Persona Model

核心引擎只识别请求身份、租户、资源和数据源范围，不在产品规格中定义岗位、职能或审查人群差异。任何页面、接口或脚本若需要差异化行为，必须以服务端返回的执行状态、资源状态或明确错误结果为准。

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

## 6. Access Evaluation Model

### 6.1 Evaluation order

后端请求授权按以下顺序执行：

1. 校验请求是否在公开白名单
2. 解析并校验身份凭证
3. 建立 `tenantId`、`userId` 和链路上下文
4. 校验请求目标资源是否属于当前租户
5. 校验当前上下文声明是否满足该操作的后端访问规则
6. 校验数据范围或数据源范围
7. 记录成功或失败审计日志

### 6.2 Tenant isolation

- 所有核心表和核心资源都必须绑定 `tenantId`
- 跨租户查询默认拒绝
- 跨租户查看必须走显式管理接口并记录执行留痕

### 6.3 Datasource scope

数据源访问范围至少包含：

- 可见性：是否能查看数据源元数据
- 使用权：是否能使用该数据源发起查询或压测
- 管理范围：是否能更新数据源、连接池、访问范围
- 导出权：是否能导出相关结果或历史

### 6.4 Operation matrix

| Operation | Required backend checks | Additional checks |
|:---|:---|:---|
| 查询提交 | 身份、租户、数据源使用范围 | SQL 风险规则、租户配额 |
| 压测提交 | 身份、租户、数据源使用范围 | 影子环境、只读约束、额外确认 |
| 数据源管理 | 身份、租户、数据源管理范围 | 仅本租户数据源 |
| 执行历史查询 | 身份、租户、历史读取范围 | 敏感字段脱敏与范围过滤 |
| 访问范围变更 | 身份、租户、变更确认 | 强留痕、双重确认建议 |
| 平台配置管理 | 身份、配置范围 | 平台级操作留痕 |

## 7. SQL Governance Controls

访问控制不只校验“谁可以访问”，还要校验“允许做什么”：

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
- 访问范围、配额和租户配置变更
- 执行历史查询与恢复操作

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
- 导出结果中的敏感列必须遵守数据源范围与脱敏策略

## 10. Failure Handling

- 未认证：返回统一 JSON 错误，状态码和错误码明确
- 无租户上下文：直接拒绝并审计
- 越界访问：直接拒绝并审计
- 数据源访问范围缺失：拒绝并返回明确错误原因
- 鉴权系统异常：默认拒绝，不允许 fail-open

## 11. Current Implementation Baseline

当前仓库现状：

- `governance` 已删除旧访问矩阵实现，内部受保护入口改为 `/api/governance/internal/datasource-access/check` 与 `/api/governance/internal/datasource-access/scope/change`
- `query-execution`、`sql-optimization`、`benchmark-engine` 已改为调用数据源范围检查，不再调用旧授权决策路径
- 当前资源模型已覆盖 `SQL_REWRITE_RECORD`，使 SQL 历史聚合改写记录时可按后端访问规则读取，同步把创建、确认、发布、暂停、撤销和验证创建限制在优化提交访问规则与数据源 `USE` 范围内
- 数据源访问范围变更由内部受保护通道承担，必须在系统租户上下文执行并写入审计
- 当前 header-based stateless auth 已把每次受保护请求的鉴权建立/释放记录为 `LOGIN` / `LOGOUT` 审计事件；鉴权前置失败会记录失败型 `LOGIN` 审计事件
- 当前 `governance` 已通过共享 AES-256 基线把密码 / token / key 类字段接入统一持久化保护入口：
  - `system_config` 敏感键写入 `value_ciphertext`
  - `config_snapshot/result_payload/query_context/export_options` 的敏感叶子节点写入密文 envelope
  - `audit_log.request_params/response_summary` 与导出地址、错误文本仅保留脱敏内容
- 当前治理服务已固定以下失败错误码：
  - 访问规则不满足治理访问要求：`20000` `GOVERNANCE_ACCESS_DENIED`
  - 跨租户访问拒绝：`20001` `GOVERNANCE_TENANT_ACCESS_DENIED`
  - 数据源绑定或访问范围拒绝：`20002` `GOVERNANCE_DATASOURCE_ACCESS_DENIED`
- 旧矩阵和旧访问决策入口不再作为当前实现或目标契约的一部分保留

## 12. Target Completion Definition

当访问控制能力被视为“补充完整”时，至少需要满足：

- 所有受保护接口都经过后端身份鉴别
- 所有核心资源都绑定租户并执行租户隔离
- 数据源访问范围可配置且可验证
- 审计日志覆盖认证失败、越权失败和关键成功操作
- 敏感配置加密存储且不泄露到日志/导出
- 备份恢复文档和验证记录纳入交付

## 13. Validation Scenarios

- 未登录访问受保护接口被拒绝
- 缺失 `tenantId` 的请求被拒绝
- 缺失数据源管理范围的请求无法管理数据源
- 缺失历史读取范围的请求无法读取高敏感执行详情
- 跨租户资源访问被拒绝
- 已移出数据源访问范围的用户无法继续查询
- 压测任务不能直接对生产写路径执行
- 认证失败、越权失败和成功访问都有审计记录

## 14. Related Documents

- `docs/security/compliance.md`
- `docs/architecture/init.md`
- `docs/architecture/messaging-abstraction.md`
- `docs/plans/master-execution-plan.md`
