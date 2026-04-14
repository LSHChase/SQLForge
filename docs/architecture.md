# SQLForge 架构分析与初始化方案

本文件现在与 Harness engineering 风格的仓库知识结构配套使用：

- 导航入口见 [AGENTS.md](/models/project/codex/SQLForge/AGENTS.md)
- 顶层架构地图见 [ARCHITECTURE.md](/models/project/codex/SQLForge/ARCHITECTURE.md)
- 设计/产品/计划/质量文档见仓库根目录和 `docs/` 各索引

## 1. 对原方案的工程化收敛

原始方案覆盖面很完整，但如果直接按全部能力同时落地，工程风险会很高，主要体现在：

- 控制面与执行面职责混杂，容易让调度、分析、接入、报告耦合在一起
- 六维压测矩阵一次性全部实现，开发量大，且难以优先验证平台核心价值
- 方案强调“智能”，但缺少明确的领域对象、应用服务和基础设施边界
- 真实 Trino/Hudi/K8s 接入具有外部依赖，不适合在初始化阶段强耦合

因此 SQLForge 初始化版采用“先骨架、后连接器”的策略。

## 2. 初始化版优化原则

### 2.1 控制面 / 数据面分离

控制面负责：

- SQL 接入与治理
- 风险评估
- 压测计划编排
- 结果分析
- 报告输出
- 决策闭环

执行面负责：

- 样本构造
- 压测下发
- 影子压测
- Trino/K8s/Hudi 外部系统适配

初始化版只把控制面做成可运行骨架，并为执行面保留接口。

### 2.2 MVP 先聚焦三条主线

第一阶段只强落三条业务闭环：

1. BI SQL 上线前评估
2. 压测计划自动编排
3. 大促容量规划

这样可以优先证明平台在“是否上线、怎么压、扩多少”三个核心问题上的价值。

### 2.3 启发式算法先替代重型依赖

初始化版先用启发式规则与轻量模型替代：

- Trino AST 解析器
- 真实统计信息回放
- 真实 QueryDetail 分析
- K8s 与 Hudi 接口

后续只需替换基础设施层或领域服务实现，不需要重写应用层流程。

## 3. 初始化版领域划分

### Access & Governance

- `QueryIntentEngine`
- `AssessSqlUseCase`
- `TenantPolicy`

职责：

- SQL 指纹生成
- 风险标记
- 查询结构分类
- 资源粗估
- 租户配额读取

### Data Construction

- `SamplingEngine`
- `ExtrapolationModel`

职责：

- 生成代表性样本计划
- 根据样本结果外推全量耗时

### Execution Engine

- `PressureMatrix`
- `CreateBenchmarkPlanUseCase`

职责：

- 生成并发梯度
- 构造数据规模矩阵
- 构造执行计划枚举
- 预留影子压测和租户混压扩展点

### Intelligence Layer

- `ResultSanitizer`
- `BottleneckDetector`
- `CapacityPlanner`
- `ResourceEstimator`
- `AnalyzeBenchmarkUseCase`

职责：

- 冷启动剔除
- 异常值识别
- 瓶颈判断
- Worker/CPU/内存/磁盘/网络估算

### Output & Action

- `ReportBuilder`
- `GenerateReportUseCase`
- `EvaluateBiReleaseUseCase`
- `PlanPromotionCapacityUseCase`

职责：

- 报告生成
- 上线建议
- 容量规划输出

## 4. 推荐的后续落地顺序

1. 接入真实 SQL 解析器，替换当前启发式规则
2. 接入 Trino EXPLAIN / QueryDetail / EventListener
3. 接入 Hudi/HMS 元数据，生成真实采样计划
4. 接入 K8s 执行器和租户资源隔离
5. 接入时序指标和日志系统，构建真实结果分析闭环

## 5. 仓库结构设计

```text
src/
  app/
    create-http-server.js
    router.js
  bootstrap/
    create-container.js
  config/
    index.js
  infrastructure/
    repositories/
  modules/
    access/
    data-construction/
    execution/
    intelligence/
    reporting/
    tenancy/
    workflows/
  shared/
    utils/
test/
```

## 6. 初始化版明确保留的扩展点

- `QueryIntentEngine`：可替换为真实 Trino AST 解析实现
- `SamplingEngine`：可接入 Hudi/HMS 元数据服务
- `PressureMatrix`：可追加极端倾斜、统计信息老化、资源约束扫描
- `AnalyzeBenchmarkUseCase`：可接入真实 Trino QueryDetail
- `TenantPolicy`：可接入多租户控制台或配置中心
- `BaselineRepository`：可替换为 PostgreSQL / Redis / Elasticsearch

## 7. Harness 风格调整项

为对齐 Harness engineering 的核心实践，当前仓库补充了以下约束：

- `AGENTS.md` 只保留导航信息，不堆积全部规则
- `docs/` 作为系统事实来源，按设计、产品、计划、生成物、参考资料分区
- 顶层增加 `ARCHITECTURE.md`、`DESIGN.md`、`PLANS.md`、`QUALITY_SCORE.md`、`RELIABILITY.md`、`SECURITY.md`
- 通过测试机械校验文档结构和依赖方向，避免约束只停留在文字层

## 8. 本次初始化完成标准

本次提交达到以下目标：

- 仓库可以直接启动
- 已有清晰领域分层
- 已有 3 条主流程的初版实现
- 已有 HTTP API 和测试
- 已经能作为后续真系统接入的稳定骨架

## 9. 生产栈迁移目标

根据新的交付约束，SQLForge 的主工程方向调整为：

- 前端采用 Vue + JavaScript + CSS
- 后端采用 Java 8 + Spring Boot
- 采用前后端分离部署
- 统一 UTF-8 编码与 Unix/LF 行尾
- 运行环境支持 `arm64` 与 `amd64`
- 数据引擎连接层覆盖 MySQL、Trino、Presto、ClickHouse、MRS-Hetu、Kyligence

该目标以 `frontend/` 与 `backend/` 目录为主线推进，原 `src/` 目录仅保留为初始化阶段参考。
