# SQLForge Backup And Recovery Baseline

## Purpose

本文件作为 `F-TASK-008` 的当前权威落点，用于把 SQLForge 的备份对象、恢复目标、责任分工和演练记录模板收口为一份可执行的运维基线。

本文只写两类内容：

- 已被仓库代码、配置或文档事实证明的当前恢复相关基线
- 生产运维必须补齐的备份、恢复和演练动作

凡仓库中尚未实现的自动化备份、恢复脚本、对象存储策略或密钥托管能力，必须显式标记为缺口，不得写成既成事实。

## Scope

- 适用模块：
  - `governance`
  - `query-execution`
  - `sql-optimization`
  - `benchmark-engine`
- 适用数据域：
  - MySQL / TDSQL 主库与副本
  - `config_snapshot`
  - `execution_result`
  - `query_history`
  - `export_record`
  - `audit_log`
  - `kafka_message_queue`
  - `system_config` 敏感配置密文与密钥标识
- 关联规则：
  - `R-113`
  - `R-114`
  - `R-115`
  - `R-118`
  - `R-144`
- 关联 ADR：
  - `ADR-009`
  - `ADR-010`
  - `ADR-012`

## Current Repository Facts

| Area | Current implementation | Evidence |
|:---|:---|:---|
| Governance metadata carrier | 当前事务型治理元数据统一落在 MySQL 持久化基线，承载者是 `governance` | `docs/architecture/persistence.md` |
| Core traceability chain | 已固化 `config_snapshot -> execution_result -> query_history -> export_record -> audit_log` 主外键追溯链 | `docs/architecture/persistence.md`, `sql/init-schema.sql`, `sql/migrations/V20260421_011__core_traceability_chain.sql` |
| Audit evidence table | `audit_log` 已承接 `POST /api/governance/internal/audit/write` 与 header-based stateless auth 的 `LOGIN` / `LOGOUT` 真实落库 | `docs/architecture/persistence.md`, `governance/src/main/java/com/company/governance/application/service/GovernanceAuditTrailService.java` |
| Message fallback store | `DATABASE` 模式使用 `kafka_message_queue` 表；治理审计主路由失败时会补写 fallback message | `docs/architecture/messaging-abstraction.md`, `sql/init-schema.sql`, `governance/src/main/java/com/company/governance/application/service/GovernanceAuditTrailService.java` |
| Sensitive config storage | `system_config` 命中密码 / token / key 类键名时，当前只允许写入 `value_ciphertext/value_mask/encryption_algorithm/encryption_key_id` | `docs/architecture/persistence.md`, `docs/security/compliance.md`, `sql/migrations/V20260421_013__sensitive_data_encryption_baseline.sql` |
| Export persistence boundary | 当前仓库只固化 `export_record` 元数据；`storage_uri` 只允许保存脱敏后的地址摘要，真实导出文件位置属于外部对象存储或离线介质恢复范围 | `docs/architecture/persistence.md`, `docs/deployments/huawei-cloud-setup.md` |
| Post-restore observability baseline | `F-TASK-007` 已形成统一的 `logs / metrics / alerts` 验收基线，可直接复用到恢复演练收尾检查 | `docs/deployments/observability-baseline.md` |

## Backup Inventory And Protection Boundary

生产运维至少要把以下对象纳入备份清单，并为每项标注备份批次号、保留周期、加密方式和恢复优先级。

| Data domain | Current carrier | Mandatory backup content | Protection boundary | Restore priority |
|:---|:---|:---|:---|:---|
| MySQL schema + migrations | MySQL / TDSQL, `sql/init-schema.sql`, `sql/migrations/` | 运行中实例全量备份、binlog 或等价增量链、当前 schema 版本与已执行 migration 清单 | 备份介质必须加密；恢复时不得跳过 schema 版本核对 | `P0` |
| Core traceability metadata | `config_snapshot`, `execution_result`, `query_history`, `export_record` | 事务一致的表数据、索引、外键和最近增量日志 | 必须保持跨表一致性，不得只恢复单表快照 | `P0` |
| Audit evidence | `audit_log` | 独立可追溯备份批次、保留周期和恢复批次号 | 不可用本地滚动日志替代；恢复后不得丢失留痕连续性 | `P0` |
| Export metadata and archive pointers | `export_record`, 外部 `OBS` 或等价对象存储引用 | `export_record` 表数据、归档对象清单、离线导出批次索引 | 元数据与对象存储批次必须可交叉核对；仅恢复对象文件而不恢复元数据视为失败 | `P1` |
| Message fallback and compensation backlog | `kafka_message_queue` | `DATABASE` 模式消息表、失败重试计数、待消费状态 | 生产若启用数据库兜底或 fallback message，必须能恢复 `pending/failed` 状态并继续补偿 | `P1` |
| Sensitive config ciphertext | `system_config` 中的 `value_ciphertext/value_mask/encryption_*` | 敏感配置密文、掩码摘要、算法和 `encryption_key_id` | 只恢复 `config_value` 明文列视为错误；密文和 key metadata 必须成套保留 | `P0` |
| Key rotation boundary | 外部密钥服务、密钥登记表或离线密钥清单 | 与备份批次对应的 `keyId -> decrypt capability` 映射、密钥轮换窗口记录、退休时间 | 这是恢复可用性的硬边界；若缺少仍在保留期内的 key mapping，视为整批备份不可恢复 | `P0` |

补充要求：

1. `audit_log`、核心追溯链和 `system_config` 敏感密文字段必须使用同一恢复批次号登记，避免恢复后外键链或密钥元数据错位。
2. `kafka_message_queue` 的恢复范围必须和审计主路由故障窗口对齐，否则会出现“数据已恢复但补偿队列丢失”的假成功。
3. `export_record` 当前只保证元数据与脱敏地址摘要；真实导出文件若已归档到 `OBS`，必须把对象存储版本号或归档批次写入演练记录。

## Recovery Objectives

### Data Domain Objectives

以下目标把 `R-115` 的全局约束拆到可执行的数据域。除密钥边界外，其余 MySQL 相关对象默认继承 `RPO < 1小时`、`RTO < 4小时`。

| Service / data domain | Trigger conditions | Target RPO | Target RTO | Primary owner | Secondary owner | Acceptance evidence |
|:---|:---|:---|:---|:---|:---|:---|
| `governance` / MySQL 主库与核心追溯链 | 主库损坏、主从复制断裂、误删元数据、迁移失败后需要回退 | `< 1小时` | `< 4小时` | `DBA / Platform Ops` | `Governance service owner` | 恢复后 `config_snapshot/execution_result/query_history/export_record` 行数与抽样关联键一致；schema 版本与 migration 清单匹配 |
| `governance` / `audit_log` | 审计表损坏、审计留痕缺口、误删或归档回灌 | `< 1小时` | `< 4小时` | `DBA / Compliance Ops` | `Governance service owner` | 指定时间窗审计记录连续；`LOGIN` / `LOGOUT` 与 `audit/write` 抽样写入成功；恢复动作自身留痕 |
| `governance` / `export_record` 与归档索引 | 导出元数据损坏、归档索引丢失、导出记录与对象存储不一致 | `< 1小时` | `< 4小时` | `DBA / Storage Ops` | `Governance service owner` | 抽样 `export_record` 可追溯到 `history_id/result_id`；对象清单与脱敏 `storage_uri` 摘要可匹配 |
| `governance` / `kafka_message_queue` | 审计主路由失败后 backlog 丢失、数据库队列损坏、补偿重试中断 | `< 1小时` | `< 4小时` | `DBA / Messaging Ops` | `Governance service owner` | 恢复后 `pending/failed` 统计可读取；必要时可执行重试；恢复窗口内无未解释的 backlog 丢失 |
| `governance` / `system_config` 密文与 `keyId` 映射 | 密文列损坏、密钥轮换后无法解密、误把明文写回配置表 | 与对应备份批次同步 | 与对应备份批次同步 | `Security Ops` | `DBA / Governance service owner` | 敏感配置仍留在 `value_ciphertext`；`encryption_key_id` 可解析；无敏感值回流到 `config_value` |

### Service-Level Recovery Acceptance

当前只有 `governance` 承载真实事务型持久化，但 4 个后端服务都必须在恢复后完成服务级验收，避免“库恢复了、运行态继续漂”。

| Service | Trigger conditions | Required acceptance checks | Evidence source |
|:---|:---|:---|:---|
| `governance` | 任一 MySQL 恢复、审计回灌、密钥轮换恢复、消息补偿恢复 | `/actuator/health` 与 `/api/governance/health` 均为 `UP`；审计写入成功；如启用数据库队列则 `pending/failed` 可解释 | `observability-baseline.md`, `GovernanceAuditTrailService`, `MessageAdminApplicationService` |
| `query-execution` | 依赖治理元数据的恢复、发布切换或库回切后复测 | `/actuator/health` 为 `UP`；查询执行状态流日志恢复；无持续 `STATE_PRIMARY_TIMEOUT` 或异常失败洪峰 | `docs/deployments/observability-baseline.md` |
| `sql-optimization` | 发布切换、依赖恢复后复测 | `/actuator/health` 为 `UP`；任务提交/查询可用；无持续 `status=FAILED phase=EXCEPTION` | `docs/deployments/observability-baseline.md` |
| `benchmark-engine` | 发布切换、依赖恢复后复测 | `/actuator/health` 为 `UP`；任务与报告查询可用；无持续 `status=FAILED phase=EXCEPTION` | `docs/deployments/observability-baseline.md` |

## Restore Validation Checklist

恢复完成后，必须复用 `F-TASK-007` 的观测基线做收尾验收；只做数据库恢复、不做运行态验证，不算恢复完成。

### Mandatory Checks

1. 健康探针：
   - 4 个后端服务的 `/actuator/health` 返回 `UP`
   - `governance` 的 `/api/governance/health` 返回 `UP`
2. Metrics / scrape：
   - `/actuator/prometheus` 可被抓取
   - 恢复窗口内没有持续 scrape failure
3. 审计补偿：
   - 执行一次 `audit/write` 抽样写入
   - 核对 `LOGIN` / `LOGOUT` 审计事件仍可落库
   - 若恢复窗口涉及审计主路由失败，确认 fallback message 已消费或 backlog 可解释
4. 队列 backlog：
   - 若当前环境启用 `DATABASE` 模式，检查 `kafka_message_queue` 的 `pending/failed` 数量
   - 若当前环境为 `KAFKA` 模式但恢复了治理兜底窗口，也必须记录为什么仍需要或不需要数据库队列回放
5. 导出 / 脱敏：
   - 抽样 `export_record` 记录，确认 `history_id/result_id` 追溯键完整
   - 确认 `storage_uri` 仍为脱敏摘要，而非明文地址或凭据
6. 敏感信息泄漏：
   - 抽检日志平台，不得出现密码、Token、密钥明文
   - 抽检 `system_config`，敏感键不得回流 `config_value`

### Suggested Evidence Collection

- 恢复批次号、binlog 区间或等价增量窗口
- 备份文件校验和或对象版本号
- 演练中使用的 `keyId` 清单或密钥登记引用
- 健康检查截图、日志检索链接或命令回执
- `audit_log`、`export_record`、`kafka_message_queue` 的抽样查询结果

## Drill Baseline

默认演练基线如下；后续若要调整频率或目标窗口，按 `F-TASK-008` 的人工确认点处理。

| Drill type | Baseline frequency | Required participants | Mandatory scope |
|:---|:---|:---|:---|
| Full metadata restore drill | 每季度至少 1 次 | `DBA / Platform Ops / Governance service owner` | MySQL 备份恢复、核心追溯链、`audit_log`、`export_record`、`system_config` 密文、恢复后观测验收 |
| Queue compensation drill | 每季度至少 1 次，可与全量恢复同窗执行 | `Messaging Ops / Governance service owner` | `kafka_message_queue` 或治理 fallback backlog 的恢复、重试与统计核对 |
| Key rotation compatibility drill | 每次密钥轮换前后都要执行 | `Security Ops / DBA / Governance service owner` | `value_ciphertext`、`encryption_key_id`、历史备份可解密性抽样验证 |
| Incident-driven drill | 每次真实恢复事件后 5 个工作日内补录 | 事故责任人与恢复责任人 | 真实故障窗口、恢复动作、证据与残余风险复盘 |

## Drill Record Template

以下模板可直接复制为单次演练记录。若后续拆成独立记录文件，字段不得删减。

```md
# Backup Recovery Drill Record

## Metadata

- Drill ID:
- Date:
- Environment:
- Trigger type: scheduled / key-rotation / incident-driven
- Scenario summary:
- Human confirmation ref:
- Backup batch ID:
- Binlog / incremental window:
- Messaging mode: DATABASE / KAFKA / MOCK
- Key manifest ref:

## Roles

- Primary owner:
- Secondary owner:
- DBA:
- Security Ops:
- Governance service owner:
- Observer / approver:

## Restore Scope

- Included data domains:
- Excluded data domains and reason:
- External archive / OBS objects:
- Schema version and migrations checked:

## Execution Log

| Time | Step | Result | Evidence |
|:---|:---|:---|:---|
| hh:mm | Restore full backup | pass/fail | link / command / screenshot |
| hh:mm | Replay binlog or incremental | pass/fail | link / command / screenshot |
| hh:mm | Verify key compatibility | pass/fail | link / command / screenshot |
| hh:mm | Replay queue backlog if needed | pass/fail | link / command / screenshot |

## Acceptance Checklist

- [ ] `/actuator/health` for all 4 backend services is `UP`
- [ ] `/api/governance/health` is `UP`
- [ ] `/actuator/prometheus` scrape recovered
- [ ] `audit/write` sample succeeded
- [ ] `LOGIN` / `LOGOUT` audit sample succeeded
- [ ] `kafka_message_queue` backlog checked or explicitly not applicable
- [ ] `export_record` sample verified against `history_id/result_id`
- [ ] `system_config` sensitive keys remain ciphertext-only
- [ ] log platform leak scan found no plaintext password/token/key

## Observability Evidence

- Health links:
- Metrics / scrape evidence:
- Audit evidence:
- Queue stats evidence:
- Export metadata evidence:
- Sensitive data scan evidence:

## Result

- Result: pass / partial / fail
- RPO achieved:
- RTO achieved:
- Residual risks:
- Follow-up actions:
- Write-back targets:
```

## Current Gaps

以下能力当前仍未在仓库中内建，必须明确视为后续任务，而不是本任务已落地项：

1. 未提供 MySQL 物理备份、binlog 归档或恢复自动化脚本。
2. 未提供仓库内的 `OBS` 生命周期、归档或导出对象恢复脚本。
3. 未提供密钥托管、轮换编排或 `keyId` 清单自动校验工具。
4. 未提供恢复后自动 smoke 脚本来串联健康探针、审计写入、队列统计和脱敏抽检。
5. `query-execution`、`sql-optimization`、`benchmark-engine` 当前仍未接入各自独立的 MySQL 持久化，因此恢复验收仍以服务健康和治理元数据关联验证为主。

## Exit Criteria For F-TASK-008

`F-TASK-008` 达成完成态时，必须满足：

1. 备份对象清单明确覆盖 MySQL、`audit_log`、`export_record`、`kafka_message_queue`、`system_config` 密文字段与密钥边界。
2. 恢复目标明确写出触发条件、`RPO/RTO`、责任角色和验收证据。
3. 恢复演练模板已经复用 `F-TASK-007` 的健康、审计、队列、导出与脱敏观测项。
4. 新文档已经进入 `docs/README.md`、部署入口、真值基线与覆盖矩阵，不是孤立文件。

## Related Documents

- [Observability Baseline](/models/project/codex/SQLForge/docs/deployments/observability-baseline.md)
- [Huawei Cloud Setup](/models/project/codex/SQLForge/docs/deployments/huawei-cloud-setup.md)
- [Persistence Baseline](/models/project/codex/SQLForge/docs/architecture/persistence.md)
- [Messaging Abstraction](/models/project/codex/SQLForge/docs/architecture/messaging-abstraction.md)
- [Compliance](/models/project/codex/SQLForge/docs/security/compliance.md)
- [ADR-009 Data Retention And Destruction](/models/project/codex/SQLForge/docs/adr/ADR-009-data-retention-and-destruction.md)
