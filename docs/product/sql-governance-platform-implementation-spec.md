# SQL Governance Platform Implementation Specification

## Summary

本规格把 SQLForge 后续新增的 SQL 治理中后台与开放接入能力统一收口为一套可实施、可拆任务、可验证的产品权威输入。本文档覆盖产品目标、角色、模块、页面、业务规则、开放接入、降级边界与分期；不把目标能力写成当前已实现事实，不新增微服务。

## 1. Product Positioning

- 产品定位：`SQL 治理中后台 + 开放接入平台`
- 目标用户：
  - `ANALYST`
  - `ARCHITECT`
  - `TENANT_ADMIN`
  - `PLATFORM_ADMIN`
  - `OPERATOR`
  - `AUDITOR`
  - 预留：`PRODUCT_VIEWER`、`BUSINESS_VIEWER`
- 服务边界：
  - 只扩展 `query-execution`
  - 只扩展 `sql-optimization`
  - 只扩展 `benchmark-engine`
  - 只扩展 `governance`
  - 新增交付物允许是 `JDBC Agent`、`Java SDK`、文档、配置、前端页面、表结构与脚本
  - 不新增微服务

## 2. Core Design Constraints

- SQL 注释协议固定为 SQL 开头连续 `--key=value` 注释行。
- 结构解析与 SQL 指纹前处理必须容忍 SQL 正文中的 `--` 行尾注释；只识别字符串、双引号标识符和反引号标识符外的行注释，不把字面量内的 `--` 当注释。
- 注释字段至少包括：
  - `report_code`
  - `stage`
  - `biz_date`
  - `tenant_id`
  - `datasource`
  - `engine_hint`
  - `priority`
- `biz_date` 代表执行日期 / 业务批次日期，不替代查询日期。
- `query_date` 必须从 SQL 正文或绑定后 SQL 中解析。
- 逻辑视图采用 `C` 模式：
  - `BUSINESS_VIEW`
  - `DB_VIEW`
- Prepared SQL / 参数化 SQL 必须支持三态：
  - 模板 SQL
  - 参数快照
  - 绑定后 SQL
- 深度解析固定拆分为：
  - `结构解析`
  - `数据访问解析`
- 结构解析不依赖数据库连接，必须可稳定返回。
- 结构解析结果至少包含：
  - `syntaxStatus`
  - `complexityLevel`
  - `sqlType`
  - `queryDateSummary`
  - `logicalObjectHits`
  - `riskTags`
  - `rewriteCandidates`
  - `issues`
  - `priorityScore`
  - `priorityLevel`
  - `important`
  - `urgent`
- `issues` 至少包含：
  - `issueCode`
  - `issueDomain`
  - `issueScene`
  - `severity`
  - `summary`
  - `detail`
  - `suggestedAction`
  - `affectedSqlCount`
  - `affectedReportCount`
  - `priorityScore`
  - `priorityLevel`
- 语法不可解析时也要返回结构解析结果，并通过 `syntaxStatus=INVALID` 与问题清单显式标识，不阻断页面显示。
- 单条 SQL 语法不可解析时，结构解析结果必须尽量返回 `failureReason`、`failureLine`、`failureColumn`、`failureOffset`、`failureToken` 和 `failureSnippet`，并在 `issues[]` 中同步暴露同类字段，便于页面指出失败原因和失败位置。
- 数据访问解析依赖数据库、引擎或元数据服务，可失败、可跳过、可异步补跑，但不能阻断结构解析结果返回。
- 结构解析命中底层数据库 View 时，应优先从目标数据源实时读取 View definition 并递归展开依赖；View 本身以 `DB_VIEW` hit 保留，最终叶子表以 `TABLE` hit 返回并写入历史 key。governance DB View 目录仅作为实时元数据不可用时的 fallback 证据。
- 默认执行策略：
  - 先执行结构解析
  - 连接可用时自动异步补跑数据访问解析
- 轻量解析必须低侵入，不得显著影响执行主链。
- 轻量解析需要同时支持：
  - 微服务内联能力
  - `JDBC Agent` / JAR 方式
  - Redis 作为规则 / 路由 / 逻辑视图 / 轻量改写来源
- 推荐改写 / 加速建议只负责治理与编排，不负责真实装数。
- 推荐结果默认写治理事件，由外部装数模块拉取。
- 装数协同固定为 `PULL_ONLY`：本项目不主动推送生产消息、不执行推荐 SQL、不装载数据、不修改底层存储；只沉淀推荐、dispatch event、ACK/FAILED 回执和审计证据。
- 告警一期只实现逻辑、审计、去重与日志模拟邮件，不接真实邮件通道。

## 3. Information Architecture

一级模块固定为：

1. `Dashboard`
2. `SQL 查询`
3. `SQL 历史`
4. `解析与加速`
5. `路由治理`
6. `数据资产`
7. `压测中心`
8. `系统管理`
9. `开放接入`

## 4. Module and Page Design

### 4.1 Dashboard

- 展示平台总览指标：
  - SQL 执行量
  - 成功率 / 失败率
  - 缓存命中率
  - 轻量改写命中率
  - 深度建议数量与采纳率
  - 高风险 SQL 数量
  - 报表问题占比
  - 路由引擎分布
  - 压测通过率
  - 告警数
  - 装数协同状态
  - 接入方式分布
- 展示待处理清单：
  - 高优先级 SQL
  - important / urgent 问题
  - 待下发推荐
  - 待关注报表批次
  - 待处理告警

### 4.2 SQL 查询

- 左侧：
  - 数据源树
  - 库 / Schema
  - 表
  - 业务逻辑视图
  - DB View
  - 收藏对象
- 中部：
  - SQL 编辑器
  - 注释模板插入
  - 参数输入
  - 格式化
  - 校验
  - 执行
  - Explain
  - 最近 SQL
  - 收藏 SQL
  - 草稿
- 右侧：
  - 注释上下文识别
  - `query_date` 提取
  - 参数绑定摘要
  - 命中逻辑对象
  - 路由决策
  - 缓存命中
  - 轻量改写预览
  - 深度解析入口
  - 压测入口
- 结果页签：
  - 执行结果
  - 执行摘要
  - 结构解析
  - 数据访问解析
  - 路由详情
  - 历史关联
  - 推荐建议

### 4.3 SQL 历史

- 必须支持：
  - 过滤
  - 筛选
  - 排序
  - 分类
  - 导出
  - 列表
  - 明细
- 核心筛选维度：
  - 时间范围
  - `report_code`
  - `tenant_id`
  - `datasource`
  - `stage`
  - `biz_date`
  - `query_date`
  - 状态
  - `SUCCESS/FAILED/WAITING`
  - 缓存命中
  - 轻量改写
  - 深度建议
  - 参数 SQL
  - `BUSINESS_VIEW`
  - `DB_VIEW`
  - 接入来源
  - 引擎
  - 用户
- 详情必须展示：
  - 原始 SQL
  - 模板 SQL
  - 绑定参数
  - 绑定后 SQL
  - `sqlText`
  - `sqlTemplateText`
  - `boundSqlText`
  - 注释上下文
  - `query_date`
  - 命中对象
  - 结构解析结果
  - 数据访问解析结果
  - 执行结果摘要
  - 路由决策链
  - 推荐记录
  - 压测记录
  - 导出记录
  - 审计记录
  - 告警记录
  - `recommendationRefs`
  - `benchmarkRefs`
  - `auditRefs`
  - `alertRefs`

### 4.4 解析与加速

二级模块固定为：

- `解析工作台`
- `批量解析中心`
- `解析结果中心`
- `加速与改写中心`

#### 4.4.1 解析工作台

- 单条 SQL 输入
- 数据源选择
- 参数绑定输入
- 结构解析结果卡
- 数据访问解析结果卡
- 解析结果主区与解析结果备注
- 综合结论与结构/访问解析结果上下排布
- `urgent=true`、`priority=P1` 等高风险字段显式颜色提示
- 代码、字段名和缩写保留英文标识，但提供帮助说明
- 解析完成记录写入解析历史，可从独立历史查询页面查看
- 服务不可用提示
- recommended action

#### 4.4.2 批量解析中心

- 导入模式：
  - SQL 文件导入
  - 表格导入
  - 报表清单解析
- 支持文件类型：
  - `xls`
  - `xlsx`
  - `et`
  - `csv`
  - `txt`
  - `sql`
- 一期稳定主格式：
  - `xlsx`
  - `csv`
  - `txt`
  - `sql`
- 二级兼容格式：
  - `xls`
  - `et`
- 兼容失败提示：
  - 若 `et` 无法兼容解析，页面与接口需显式提示改用 `xlsx/csv`
- 报表清单唯一主键：`report_code`
- 报表批次内联 SQL 导入规则：
  - `csv` / `xlsx` / `xls` / `et` 表格载荷按列位置解析，不依赖固定 SQL 列数
  - 第一列为报表代码；若首行是表头，第一列表头可为 `report_code`、`reportCode`、`报表代码` 或请求中的 `reportCodeField`
  - 第二列及之后，只要单元格 trim 后非空，均作为该行报表代码下的一条 SQL
  - 若 SQL 单元格以同一行 `--` 说明前缀开头，且该首行后续包含 `SELECT` / `WITH` 语句起点，导入侧必须提取真实 SQL 语句再进入结构解析；源行证据仍通过 `sourceFileLine` 保留，正常换行形式的 SQL 行注释继续交给结构解析器处理
  - 中间空单元格跳过，不产生 SQL；后续 100+ SQL 列必须动态遍历
  - 若表格行只有报表代码且没有内联 SQL，继续走报表 SQL resolver / txt mock source 回退
- 报表 SQL 获取策略：
  - 一期：从 txt / mock source 模拟
  - 后续：由 `governance` 配置接口，`sql-optimization` 调用
- repo-side 稳定导入执行链：
  - 先创建批次元数据
  - 再提交 `contentBase64` 批次载荷
  - 系统生成导入记录与失败记录
  - 结构解析优先执行
  - 若 `structureParseOnly=false`，再编排数据访问解析
- 批量解析页的结果与详情可见性：
  - 批量解析和报表导入的成功、部分成功、失败记录都必须保留详情入口
  - 失败记录不得只展示摘要；页面必须能点击查看 `parseTaskId`、状态、失败原因、SQL 文本、问题场景、逻辑对象与源列/行等排障字段
  - 报表导入结果按报表分组展示概览，同时 SQL 级记录必须可打开详情
- HARN-071 大批量解析与报表导入治理边界：
  - 批量解析和报表导入使用与单条 SQL 解析相同的结构解析 / Access 解析执行链；导入阶段只 trim SQL 单元格或文件语句的外层空白，不改写字符串、注释、标识符、行内空白或持久化 SQL 文本。
  - HARN-073 对上述规则增加报表导入专用窄例外：同一行 `-- 说明 -- 说明 SELECT ...` 这类历史报表清单伪注释前缀必须先提取 `SELECT` / `WITH` 起点，避免把整条输入误判为注释；该规则不改变普通批量 SQL 文件的词法切分，也不改写正常 SQL 行注释。
  - 批量解析和报表导入的 SQL 级问题明细与统计必须沿用单条 SQL 解析的细粒度 `issueCode` / `riskTags` 口径，不得只按粗粒度 `issueScene` 去重后展示，避免复杂反模式 SQL 在批量入口中少报问题。
  - 报表导入结构解析失败时，SQL 级失败原因必须包含可排障定位；当 parser 能给出位置时，至少保留 `line`、`col`、`token` 和 `near` 片段，页面结合 `reportCode`、`sqlColumnName`、`sqlOrdinalInReport` 与 `sourceFileLine` 定位源报表和源列。
  - SQL 文件导入必须按 SQL 词法边界切分语句，分号出现在字符串、引用标识符、行注释或块注释内时不得拆分；纯注释片段不生成导入记录。
  - repo 当前稳定入口仍是 `contentBase64` 载荷提交与 JVM 内解析，HARN-071 的 50M / 10 万 SQL 目标在仓库内闭环为“解析链一致、统计完整、明细预览有界、页面不一次渲染全量 SQL”；真正流式上传、外部报表系统直连和生产级 10 万 SQL 压测验收属于后续 delivery / 集成边界。
  - 批次状态接口必须返回完整总量、成功/失败/部分成功数量、成功率和统计汇总，同时对 SQL 明细返回预览元数据：普通批量明细默认返回前 500 条、失败明细默认返回前 200 条，报表导入 SQL 明细默认返回前 500 条。
  - 报表批次解析统计的 overview、问题场景、重要程度、报表视角、优先级和逻辑对象统计按完整批次汇总；SQL 清单统计只返回治理预览并暴露 `sqlStatisticLimit`、`sqlStatisticTruncated` 与 `omittedSqlStatisticCount`。
  - HARN-079 对报表批次解析统计增加同一报表多 SQL 合并候选分析：仅在同一 `report_code` 内多条 SQL 共享数据源、阶段，并命中相同逻辑对象或高度重叠问题场景时提示 `REPORT_SQL_MERGE_CANDIDATE`；该能力只输出治理问题与复核建议，不证明 SQL 语义等价，不生成或执行合并后的 SQL，不修改持久化 SQL 文本。
  - 前端必须以 summary-first 方式展示大批量结果：首屏显示完整统计卡片和 Top 问题，详情弹窗只预览有限明细，并明确提示剩余省略数量与完整统计口径。

#### 4.4.3 解析结果中心

- 统计维度：
  - 按 SQL
  - 按问题场景
  - 按报表
  - 按逻辑对象
  - 按严重度
  - 按优先级
  - 按 important / urgent
  - 按结构解析 / 数据访问解析状态
- 问题域：
  - 结构问题
  - 性能问题
  - 数据问题
  - 路由问题
  - 方言兼容问题
  - 规范问题
  - 治理问题
- 严重度：
  - `INFO`
  - `LOW`
  - `MEDIUM`
  - `HIGH`
  - `CRITICAL`
- 优先级：
  - `P1`
  - `P2`
  - `P3`
  - `P4`
- 结构解析复杂度：
  - `SIMPLE`
  - `MODERATE`
  - `COMPLEX`
  - `EXTREME`

#### 4.4.4 加速与改写中心

- 轻量改写建议
- 深度改写建议
- 加速 SQL
- 建表 SQL
- 预热 SQL
- 维护 SQL
- 推荐详情必须包含：
  - 推荐原因
  - 原 SQL
  - 推荐 SQL
  - 风险
  - 收益
  - 目标引擎
  - 适用对象
  - 是否需装数协同
- `HARN-127` 后续目标以 [加速与改写治理工作台方案](./acceleration-rewrite-governance-workbench-spec.md) 为准：解析驱动与查询驱动保留两条入口，后续候选建议、SQL 差异、计划审批、应用验证、监控告警在统一工作台承接。
- 推荐 SQL 必须从简单替换升级为 L0 安全语法改写、L1 结构改写、L2 引擎/物理协同建议三层输出，并显式标注前置条件、语义风险、未采用规则、验证方式和是否允许自动应用。
- 推荐前后 SQL 必须提供 diff 视图，覆盖文本 diff、规则级 diff、AST 摘要差异和风险提示；仅展示两个 SQL 代码块不足以满足治理要求。
- 改写记录必须能在 SQL 历史详情中查询，并支持周期性原 SQL / 推荐 SQL 结果比对；发现差异时触发 `SQL_REWRITE_RESULT_DIVERGENCE` 告警并暂停自动应用。
- `HARN-143` / `HARN-144` 明确状态边界与证据字段：`APPLIED` 只能表示配置或绑定已写入，不等同于产品态生效；默认运行时优先应用必须等待 `VERIFIED` / `ACTIVE` 且结果等价、收益有效、schema 未过期；推荐、候选、历史改写记录和告警 payload 必须保留 `sourceKind` 与 `evidenceLevel`。

#### 4.4.5 加速治理工作台

- 页面定位：真实接口 smoke 与正式加速治理流的统一 operator 页面，不替代解析工作台、推荐中心或 SQL 历史。
- 入口固定为：
  - `PARSE`：解析历史、批量解析、报表解析、解析问题场景。
  - `QUERY`：SQL 历史、慢 SQL、高 P99、高扫描量、压测回归。
- 页面必须固定展示：
  - `sourceType`
  - `sourceKind`
  - `sourceId`
  - `evidenceLevel`
  - `parseHistoryId`
  - `historyId`
  - `sqlFingerprint`
  - `datasourceCode`
  - `stage`
  - `reportCode`
- 流程图只放在该页面，节点为：入口证据、候选建议、SQL 差异、计划审批、应用验证、监控告警、回滚/废弃。
- 已实现页面能力用跳转复用：
  - 解析详情跳转解析历史。
  - 查询详情跳转 SQL 历史。
  - 推荐详情跳转推荐中心。
  - SQL 执行跳转 SQL 查询。
  - 告警详情跳转告警中心。
- 未实现独立页面的生命周期操作可在本页提供轻量真实接口按钮，响应 JSON 放入证据抽屉，不作为主视觉。
- 设计约束：
  - 工具型布局，不做营销式 hero。
  - 不使用卡片套卡片。
  - JSON 只做下钻，主区展示摘要、状态、差异、风险和下一步动作。
  - 静态解析证据必须标注 `staticOnly=true`，不得显示为真实扫描量或真实耗时。

### 4.5 SQL 历史导出与取证

- `POST /api/governance/query-history/export` 只负责单次 SQL 执行历史的取证导出基线
- SQL 解析记录由 `POST /api/sql-optimization/parse-history/export` 导出，不再复用治理 `query_history` 导出面
- 支持格式：
  - `CSV`
  - `EXCEL`
  - `JSON`
  - `SQL_TEXT`
  - `PDF_REPORT`
- 一期 `PDF_REPORT` 以 inline textual evidence payload 返回，并同步写入 `export_record + audit_log`
  - 治理事件状态

### 4.5 路由治理

- 当前路由策略
- 路由规则管理
- 历史路由记录
- 路由决策详情
- 注释协议说明
- 逻辑对象路由规则
- fallback 策略
- 引擎可达性状态

路由输入必须覆盖：

- 注释上下文
- SQL 结构特征
- 绑定后 SQL
- 逻辑对象命中
- 租户
- 数据源
- 时间窗口

### 4.6 数据资产

- 数据源
- Schema / 库
- 表
- `BUSINESS_VIEW`
- `DB_VIEW`
- 字段
- 分区
- 行数 / 体量
- 最近更新时间
- 数据到位状态
- SLA 状态
- 上游任务状态
- 下游可查询性
- 命中频次
- 逻辑视图到物理表映射

### 4.7 压测中心

二级模块固定为：

- `压测任务`
- `压测模板`
- `测试集`
- `压测报告`

支持：

- 单条压测
- 批量导入
- 模板
- 测试集
- 回归守护
- 从解析结果生成测试集
- 从推荐 SQL 生成对比压测
- 与报表 / 逻辑视图关联

### 4.8 系统管理

- 数据源管理
- 逻辑对象管理
- 报表接口配置
- 路由配置
- Redis 规则源
- 告警中心
- 装数协同配置
- 权限与审计
- 系统参数

### 4.9 开放接入

- 接入总览
- API 接入
- JDBC Agent
- SDK / Client
- 接入策略

支持的接入方式：

- `HTTP API`
- `JDBC Agent`
- `Java SDK`
- `Direct Client Mode`

JDBC Agent 三种模式：

- `Observe`
- `Governed Execute`
- `Local Rewrite + Direct JDBC`

默认模式：`Observe`

## 5. SQL Annotation and Query-Date Rules

标准模板：

```sql
--report_code=RPT_SALES_DAILY
--stage=PROD
--biz_date=2026-04-25
--tenant_id=t001
--datasource=hetu_main
--engine_hint=trino
--priority=high
SELECT ...
```

规则：

- 只解析 SQL 开头连续注释行
- 结构解析必须返回注释上下文
- 数据访问解析、路由与历史追溯都消费同一套注释字段
- 查询日期不从注释提取
- 查询日期优先从绑定后 SQL 中解析

## 6. Parsing Model

### 6.1 Lightweight Parsing

- 执行前快路径
- 不能阻断主执行
- 超时即降级
- 目标性能预算：
  - `P95 < 20ms`
  - Redis / 规则源不可用时直接 bypass
- 负责：
  - 注释解析
  - SQL 类型判断
  - 参数 SQL 识别
  - `query_date` 提取
  - 逻辑对象命中
  - 简单路由 hint / 方言轻量改写

### 6.2 Structure Parse

- 不依赖数据库
- 必须稳定可用
- 返回：
  - 语法合法性
  - 结构复杂度
  - Join / 子查询 / Union / 窗口函数等特征
  - 逻辑对象命中
  - 参数化 SQL 识别
  - 改写候选点
  - 风险标签
  - 优先级建议

`D-TASK-073` 之后，结构解析在保持上述旧字段兼容的基础上升级为查询意图理解输出：

- `sqlFingerprint`：标准化 SQL 指纹，用于 SQL 级追溯、聚合和后续 NL2SQL/推荐消费。
- `intentProfile`：静态查询意图标签，覆盖扫描模式、Join 类型、计算密度、资源类型、SLA 等级和置信度。
- `featureSummary`：parser 引擎、表数量、Join 数、谓词数、窗口函数、UDF 和重复表达式等多维结构证据。
- `riskChecklist`：结构化风险清单，至少覆盖全表扫描、大表 Join、非必要排序、重复表达式计算和结果集过大。
- `estimatedResourceCost`：CPU / IO / 内存 / 网络 / 结果集的等级化启发式估算，只能作为结构解析阶段的治理提示，不能写成真实执行计划或生产资源承诺。

Parser 边界：

- 默认保留现有 JSQLParser 路径。
- 新增 Trino parser adapter，必须通过统一 AST profile 输出结构信号，不允许业务层直接依赖单一 parser API。
- Parser adapter 失败必须降级为结构解析问题，不得阻断页面展示或改变 access parse 独立失败语义。
- NL2SQL 本阶段只预留消费字段，不实现自然语言生成 SQL。

`D-TASK-074` 对 `D-TASK-073` 复盘后补齐 SQL 指纹前处理契约：

- `sqlFingerprint` 必须基于静态文本前处理后的 SQL 形态生成：去除 SQL 注释、参数化字符串 / 数字 / 命名参数字面量、折叠空白、统一大小写并忽略末尾分号。
- 该指纹用于治理聚合、风险归并和后续推荐 / NL2SQL 消费入口，不代表完整 SQL 语义等价证明。
- 本阶段不回填历史已生成指纹，不新增持久化字段；如需跨服务缓存迁移或历史数据重算，必须另行任务化。

`D-TASK-075` 使用复杂反模式 SQL 回归补强结构解析递归 AST 信号：

- `featureSummary` 可输出子查询数、SELECT 标量子查询数、嵌套子查询深度、相关子查询数、OR 谓词数、函数包裹谓词数、前导通配符 LIKE 数、随机排序数和重复表扫描数。
- `riskChecklist/issues/riskTags` 可覆盖 `SCALAR_SUBQUERY_IN_SELECT`、`NESTED_SUBQUERY_RISK`、`CORRELATED_SUBQUERY_RISK`、`FUNCTION_WRAPPED_PREDICATE`、`NOT_EXISTS_ANTI_JOIN_RISK`、`LEADING_WILDCARD_LIKE_RISK`、`OR_PREDICATE_INDEX_RISK`、`ORDER_BY_RANDOM_RISK`、`REPEATED_TABLE_SCAN_RISK` 和 `COMPLEX_QUERY_GRAPH_RISK`。
- 上述信号全部来自静态 SQL AST，不证明真实索引存在性、对象规模或执行计划成本；涉及字段存在性、权限、分区可用性和真实计划仍属于 access parse / benchmark 边界。

`HARN-091` 继续补齐结构解析静态风险分析，但不把静态结果伪装成真实执行指标：

- `featureSummary` 可继续输出 `orderByExpressionCount`、`duplicateOrderByKeyCount`、`duplicateGroupByKeyCount`、`groupByWithoutAggregate`、`aggregateFunctionCount`、`stringProjectionCount`、`stringConcatenationCount`、`largeStringAggregateCount` 和 `repeatedSubqueryCount`。
- `riskChecklist/issues/riskTags` 可覆盖 `ORDER_BY_COMPLEXITY_RISK`、`JOIN_LATENCY_RISK`、`AGGREGATION_COMPLEXITY_RISK`、`GROUP_BY_WITHOUT_AGGREGATE_RISK`、`DUPLICATE_GROUP_OR_ORDER_KEY_RISK`、`REPEATED_SUBQUERY_RISK` 和 `LARGE_STRING_RESULT_RISK`。
- 结构解析 evidence 必须带有 `staticOnly=true`，上述计数只能表达排序、Join、聚合、字符串返回和重复子查询的静态启发式风险，不输出真实扫描行数、扫描字节数、执行耗时、返回行数或返回字节数。
- 真实执行计划摘要、扫描量、耗时和返回体积只能来自 access parse / benchmark / Hetu EXPLAIN 这类真实引擎证据；当这些证据不可用或 skipped 时，页面必须展示 unavailable / skipped 语义，不得显示为 0 成本或低风险。

`HARN-066` 补齐单条 SQL 解析失败诊断与行注释兼容：

- 结构解析前处理必须剔除字符串与标识符外的 `--` 行尾注释，同时保留换行和字符长度，以维持 parser 失败定位和原 SQL 位置的一致性。
- 单条 SQL 解析失败不得影响后续 SQL 输入或异步解析结果展示；前端必须按当前输入快照接收结果，输入变化后清理旧解析结果和旧错误信息。
- INVALID 结构解析结果必须给出可读失败原因；当 parser 或启发式定位可用时，同时给出行、列、offset、token 和附近片段。

`HARN-084` 将 SQL 执行历史与 SQL 解析记录解耦：

- SQL 执行历史继续由治理 `query_history` 与 `/api/governance/query-history` 承载，语义固定为 `QUERY_EXECUTION`。
- SQL 解析记录由 `sql-optimization.sql_parse_history` 与 `/api/sql-optimization/parse-history` 承载，语义固定为 `SQL_PARSE_RECORD`。
- 结构解析、综合解析、批量解析、报表解析和日终慢 SQL 解析不再写治理 `query_history`。
- 日终慢 SQL 解析本阶段只提供只读候选 source 端口、应用服务骨架、幂等批次键和测试，不声明真实 cron 调度已投产。

### 6.3 Access Parse

- 依赖数据库 / 引擎 / 元数据
- 返回：
  - 对象存在性
  - 字段存在性
  - 权限
  - 方言兼容
  - 执行计划摘要
  - 分区 / 数据到位 / SLA
  - 可达性
- 当服务不可用时：
  - 必须保留结构解析结果
  - 必须把 access parse 标记为 failed / unavailable / skipped
  - 页面必须提示服务不可用

## 7. Data Source and Connection Requirements

Phase 1 的 SQL 执行范围默认固定为只读查询：

- `SELECT`
- `SHOW`
- `DESC`
- `EXPLAIN`

非只读 SQL 不进入默认执行主路径，必须返回受控拒绝或后续审批扩展语义。

数据源配置必须支持：

- JDBC
- HTTP API
- Client Mode
- Gateway / Proxy
- Hetu / Trino / Presto 类执行模式抽象

配置维度至少包括：

- 基础信息
- 连接参数
- 凭证方式
- 安全选项
- 默认路由策略
- 可用引擎
- 是否允许查询
- 是否允许压测
- 健康状态
- 连通性检测
- 最近失败原因
- 租户可见范围
- 标签 / 负责人 / 环境

## 8. Recommendation and Dispatch Rules

- 本项目负责：
  - 推荐 SQL
  - 建表 SQL
  - 预热 SQL
  - 加速 SQL
  - 风险 / 收益
  - 治理事件
  - 状态追踪
- 本项目不负责：
  - 真实装数
  - 底层数据落表执行
  - 重型调度执行
- 推荐治理事件默认由外部模块拉取。

## 9. Alerting Rules

关键告警事件至少包括：

- 删除加速表
- 加速表失效
- SQL 执行大面积失败
- 数据源不可用
- 上下游服务不可用
- Redis 规则源不可用
- 报表 SQL 获取失败率过高
- 批量解析失败率过高
- 数据访问解析服务不可用
- 压测回归失败
- 装数协同失败
- 审计写入异常

通知通道一期：

- 落告警记录
- 记录模拟邮件发送日志

## 10. Phasing

### Phase 1

- SQL 查询主链
- SQL 历史
- 数据资产基础
- 数据源管理基础
- 结构解析
- 数据访问解析基础
- 批量解析基础
- 报表清单 mock
- 推荐治理事件基础
- API 接入
- JDBC Agent `Observe`

### Phase 2

- 深度加速建议
- 测试集 / 模板
- 装数协同闭环
- Redis 规则源增强
- JDBC Agent 全模式
- Java SDK

### Phase 3

- 智能诊断
- 自动治理编排
- 更多 SDK
- 平台经营分析

## Related Documents

- `docs/architecture/sql-governance-interface-extension-baseline.md`
- `docs/architecture/sql-governance-data-model-extension.md`
- `docs/operations/sql-governance-degradation-matrix.md`
- `docs/plans/master-execution-plan.md`
