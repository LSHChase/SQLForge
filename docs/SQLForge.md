Kimi: 我来为您构建SQLForge的完整技术文档体系。基于您提供的详细信息，这是一个企业级、云原生、AI增强的SQL全生命周期治理平台，支持千万级日活、等保三级、多租户隔离。

我将按照标准软件工程规范，输出可直接指导开发的完整文档。由于内容庞大，我会分阶段交付，确保每个环节都可落地实施。

---

## 📋 第一阶段：核心架构与基础设计

### 01 项目需求规格说明书 (SRS)

#### 1.1 项目背景与目标

**SQLForge** 是面向企业级数据平台的SQL统一治理中枢，解决异构数据库生态下的SQL兼容性、性能优化、安全合规与加速计算问题。

**核心目标矩阵**：

| 目标维度 | 具体指标 | 验收标准 |
|---------|---------|---------|
| 兼容性 | 支持10+数据库方言互转 | 语法转换准确率≥99.5% |
| 性能优化 | 自动识别性能瓶颈 | 慢查询识别率≥95%，优化建议采纳率≥60% |
| 安全合规 | 敏感数据自动发现 | PII识别准确率≥90%，脱敏覆盖率100% |
| 处理能力 | 日均千万级SQL | 峰值QPS≥5000，平均RT≤200ms |
| 可用性 | 平台持续服务 | SLA 99.99%，RTO≤5min，RPO≤1min |

#### 1.2 用户角色与场景

```yaml
角色矩阵:
  DBA:
    - SQL审核与发布管控
    - 性能基线建立与监控
    - 索引优化建议审批
    - 敏感数据规则配置
    
  开发工程师:
    - IDE插件实时语法检查
    - CI/CD流水线SQL自动审核
    - 跨库SQL方言转换
    - 执行计划可视化分析
    
  数据分析师:
    - 自助SQL优化建议
    - 加速中间表推荐
    - 逻辑错误追溯
    - BI工具SQL生成辅助
    
  运维平台:
    - 批量SQL健康巡检
    - 自动化质量报告生成
    - 多租户资源配额管理
    - 故障SQL快速定位
```

#### 1.3 功能需求清单（FR）

**FR-001 SQL智能解析引擎**
- 支持MRS Hetu 3.x、Hive 3.x、ClickHouse、Kylin、TDSQL、GBase、GaussDB、MySQL、Oracle等10+数据库方言
- 配置化扩展新方言（插件化Parser注册机制）
- 词法/语法/语义三阶段解析，错误精确定位到行列
- 支持复杂嵌套SQL（CTE、子查询、窗口函数、存储过程）

**FR-002 跨库兼容性转换**
- DDL/DML/DQL方言差异自动映射
- 函数等价替换（如Oracle NVL→MySQL IFNULL）
- 数据类型隐式转换规则库
- 分区语法自动适配

**FR-003 性能诊断与优化**
- 基于代价的优化建议（CBO规则引擎）
- 全表扫描、隐式转换、笛卡尔积等18类问题模式识别
- 索引推荐（覆盖索引、联合索引、前缀索引）
- SQL改写（子查询展开、谓词下推、JOIN重排）

**FR-004 加速中间表生成**
- 基于查询模式的物化视图自动推荐
- 增量刷新策略（时间戳/CDC/触发器）
- 存储格式优化（ORC/Parquet/ClickHouse MergeTree）
- 生命周期管理（自动淘汰冷数据）

**FR-005 批量审核与CI集成**
- Git Hook预提交检查
- Jenkins/GitLab CI插件
- 质量门禁（阻断/警告/通过三级）
- SonarQube格式报告输出

**FR-006 逻辑错误反推**
- 数据血缘逆向追踪（字段级血缘）
- 结果集一致性校验（哈希比对）
- 业务规则验证（唯一性、完整性、时效性）

**FR-007 AI增强能力**
- NL2SQL自然语言转SQL（基于大模型微调）
- 异常SQL模式学习（历史慢查询训练）
- 优化效果预测（A/B测试模拟）

**FR-008 安全与合规**
- 敏感数据自动发现（正则+ML混合识别）
- 动态/静态脱敏（掩码/哈希/加密/令牌化）
- 传输层TLS 1.3加密
- 操作审计日志（等保三级要求）

**FR-009 多租户与隔离**
- 租户级资源配额（CPU/内存/存储/并发）
- 命名空间隔离（K8s Namespace+网络策略）
- 数据权限行级/列级控制

**FR-010 开放与集成**
- OpenAPI 3.0规范（RESTful + gRPC）
- Metabase/Superset/FineBI连接器
- 配置化第三方Parser接入
- Webhook回调机制

#### 1.4 非功能需求（NFR）

**性能需求**：
- 解析吞吐量：单节点≥1000 QPS，集群≥5000 QPS
- 端到端延迟：P99≤200ms（简单SQL），P99≤1000ms（复杂分析）
- 并发连接：单租户≥100并发，集群≥1000并发

**可靠性需求**：
- 可用性：99.99%（年度停机<52分钟）
- 故障恢复：RTO≤5分钟，RPO≤1分钟
- 数据持久化：HDFS三副本+TDSQL主从+异地备份

**扩展性需求**：
- 水平扩展：计算节点无状态，秒级扩缩容
- 方言扩展：新数据库适配开发周期≤2周
- 存储扩展：支持EB级元数据存储

**安全需求**（等保三级）：
- 身份鉴别：多因素认证（MFA），密码复杂度策略
- 访问控制：RBAC+ABAC混合模型，最小权限原则
- 安全审计：操作日志留存≥6个月，防篡改
- 数据安全：敏感字段AES-256-GCM加密，密钥HSM托管

---

### 02 总体架构设计 (TAD)

#### 2.1 逻辑架构

```
┌─────────────────────────────────────────────────────────────┐
│                    接入层 (Access Layer)                     │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────────────┐  │
│  │ Web控制台│  │ IDE插件 │  │ CI/CD插件│  │  OpenAPI网关    │  │
│  │  (Vue3) │  │(VSCode) │  │(Jenkins)│  │  (Kong/Nginx)   │  │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────────┬────────┘  │
└───────┼────────────┼────────────┼─────────────────┼───────────┘
        │            │            │                 │
        └────────────┴────────────┴─────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                   网关层 (Gateway Layer)                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  流量治理    │  │  认证授权    │  │  限流熔断(Bucket4j)  │  │
│  │(Sentinel)   │  │(OAuth2+JWT) │  │  多租户路由          │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                  服务层 (Service Layer)                      │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────┐  │
│  │  SQL解析服务  │ │  优化引擎服务 │ │   AI推理服务        │  │
│  │(Parser Svc)  │ │(Optimizer Svc)│ │  (LLM+规则混合)     │  │
│  └──────────────┘ └──────────────┘ └────────────────────┘  │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────┐  │
│  │  安全扫描服务 │ │  元数据服务   │ │   任务调度服务      │  │
│  │(Security Svc)│ │(Metadata Svc)│ │  (Flink+Quartz)   │  │
│  └──────────────┘ └──────────────┘ └────────────────────┘  │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                  计算层 (Compute Layer)                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  解析引擎    │  │  规则引擎    │  │   向量化执行引擎     │  │
│  │(ANTLR4/自研)│  │(Drools/自研) │  │  (Flink SQL/Spark)  │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  血缘分析    │  │  代价估算    │  │   中间表构建引擎     │  │
│  │(Apache Atlas│  │(CBO Stats)  │  │  (Materialized View)│  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                  数据层 (Data Layer)                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  元数据库    │  │  缓存层      │  │   对象存储          │  │
│  │ (TDSQL-PG)  │  │(Caffeine+    │  │  (HDFS/OSS)         │  │
│  │  血缘图库    │  │  Redis+      │  │  模型存储           │  │
│  │(Neo4j/Janus)│ │  LocalCache) │  │  审计日志           │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  消息队列    │  │  数仓加速    │  │   配置中心          │  │
│  │(Kafka/Flink)│  │(ClickHouse/  │  │  (Nacos/Apollo)     │  │
│  │  流处理      │  │  Hive ACID)  │  │  特征库             │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

#### 2.2 技术架构

**技术栈全景**：

| 层级 | 技术选型 | 版本 | 选型理由 |
|-----|---------|------|---------|
| 前端 | Vue3 + TypeScript + Vite + Ant Design Vue | 3.4+ | 企业级组件库，强类型支持 |
| 网关 | Spring Cloud Gateway + Sentinel | 2023.0.x | 云原生网关，流控熔断一体化 |
| 微服务 | Spring Boot 3.x + Spring Cloud 2023 | 3.2+ | 长期支持版本，原生GraalVM支持 |
| 解析引擎 | ANTLR4 + 自研AST优化器 | 4.13+ | 工业级语法解析，可定制性强 |
| 规则引擎 | Drools + 自研轻量引擎 | 8.44+ | 复杂规则场景+高性能简单规则 |
| AI推理 | vLLM + LangChain4j + 自研微调模型 | 0.4+ | 高性能推理，Java生态无缝集成 |
| 流处理 | Apache Flink | 1.18+ | 实时血缘分析，Exactly-Once语义 |
| 存储 | TDSQL-PG（元数据）+ HDFS（文件）+ ClickHouse（分析） | 最新 | 金融级高可用，海量存储 |
| 缓存 | Caffeine（L1）+ Redis Cluster（L2）+ Hazelcast（L3分布式） | 最新 | 三级缓存，热点数据多级加速 |
| 消息 | Apache Kafka + Flink Connector | 3.6+ | 高吞吐，与Flink原生集成 |
| 图数据库 | Neo4j（社区版）/ JanusGraph（海量场景） | 5.x/1.x | 血缘关系存储与快速遍历 |
| 配置中心 | Nacos | 2.3+ | 动态配置，服务发现一体化 |
| 可观测 | Prometheus + Grafana + SkyWalking | 最新 | 云原生监控，分布式追踪 |
| 安全 | Spring Security + OAuth2 + Vault | 6.2+ | 等保合规，密钥动态管理 |

#### 2.3 部署架构（K8s云原生）

```
┌─────────────────────────────────────────────────────────────┐
│                      K8s Cluster (多可用区)                   │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  Namespace: sqlforge-prod (生产租户隔离区)                 │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │ │
│  │  │  Gateway    │ │  Web Console│ │  Admin Service  │   │ │
│  │  │  (3副本)    │ │  (Nginx+Vue)│ │  (Spring Boot)  │   │ │
│  │  │  HPA: 3-10  │ │  HPA: 2-5   │ │  HPA: 2-5       │   │ │
│  │  └─────────────┘ └─────────────┘ └─────────────────┘   │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │ │
│  │  │  Parser Svc │ │ Optimizer   │ │  Security Svc   │   │ │
│  │  │  (无状态)   │ │  Svc        │ │  (敏感扫描)     │   │ │
│  │  │  HPA: 5-20  │ │  HPA: 3-10  │ │  HPA: 2-5       │   │ │
│  │  └─────────────┘ └─────────────┘ └─────────────────┘   │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │ │
│  │  │  Flink Job  │ │  AI Svc     │ │  Metadata Svc   │   │ │
│  │  │  (StatefulSet│ │ (GPU节点)   │ │  (TDSQL代理)    │   │ │
│  │  │  CheckPoint)│ │  HPA: 1-3   │ │  HPA: 2-5       │   │ │
│  │  └─────────────┘ └─────────────┘ └─────────────────┘   │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  数据持久化层 (StatefulSet + PV)                          │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │ │
│  │  │  Redis      │ │  Kafka      │ │  Neo4j          │   │ │
│  │  │  Cluster    │ │  KRaft模式  │ │  (因果集群)      │   │ │
│  │  │  (6主6从)   │ │  (3Broker)  │ │  (3核心+3只读)   │   │ │
│  │  └─────────────┘ └─────────────┘ └─────────────────┘   │ │
│  └─────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  外部依赖 (External Services)                            │ │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐   │ │
│  │  │  TDSQL-PG   │ │  HDFS       │ │  ClickHouse     │   │ │
│  │  │  (金融级HA) │ │  (NameNode HA)│ │  (分片集群)     │   │ │
│  │  └─────────────┘ └─────────────┘ └─────────────────┘   │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

**关键K8s配置**：
- **网络策略**：Namespace间默认拒绝，显式放行必要端口
- **资源配额**：每租户CPU≤100核，内存≤500Gi，存储≤10Ti
- **Pod安全**：非Root运行，只读文件系统，资源限制（LimitRange）
- **密钥管理**：HashiCorp Vault集成，动态数据库凭证

---

### 03 核心设计 (Core Design)

#### 3.1 SQL解析引擎架构

**设计哲学**：分层解耦 + 插件化扩展 + 增量解析

```
┌─────────────────────────────────────────────────────────────┐
│                    SQL解析流水线 (Parser Pipeline)            │
│                                                              │
│  输入层: 原始SQL → 预处理(去除注释/标准化/参数化)              │
│     ↓                                                        │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  词法分析层 (Lexer Layer)                                │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │  通用Lexer   │  │  方言Lexer   │  │  自定义Lexer    │  │ │
│  │  │ (Unicode支持)│  │ (关键字映射) │  │  (业务扩展)     │  │ │
│  │  │  状态机驱动  │  │  配置化加载  │  │  正则插件       │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  │  输出: Token Stream (类型, 值, 位置, 方言标记)            │ │
│  └─────────────────────────────────────────────────────────┘ │
│     ↓                                                        │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  语法分析层 (Parser Layer)                               │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │  ANTLR4     │  │  自研Parser  │  │  混合Parser     │  │ │
│  │  │  (标准SQL)   │  │  (复杂方言)  │  │  (配置化选择)   │  │ │
│  │  │  自动生成    │  │  递归下降    │  │  策略模式       │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  │  输出: 统一AST (抽象语法树，方言无关)                       │ │
│  └─────────────────────────────────────────────────────────┘ │
│     ↓                                                        │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  语义分析层 (Semantic Layer)                             │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │  元数据解析  │  │  类型检查    │  │  权限验证       │  │ │
│  │  │  (表/列解析) │  │  (隐式转换)  │  │  (RBAC校验)     │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  │  输出: 增强AST (绑定元数据，类型推断，血缘关系)             │ │
│  └─────────────────────────────────────────────────────────┘ │
│     ↓                                                        │
│  输出层: 标准化SQL + AST + 血缘图谱 + 诊断报告                   │
└─────────────────────────────────────────────────────────────┘
```

**方言适配器设计**（配置化扩展核心）：

```yaml
# dialect-config/hetu-3.x.yaml
dialect:
  name: "MRS_Hetu_3x"
  base: "PrestoSQL"  # 继承基础语法
  version: "3.0.0"
  
  lexer:
    keywords: 
      - "MAP"
      - "BUCKET"
      - "CUBE"
    operators:
      - "||"  # 字符串连接
  
  parser:
    overrides:
      - rule: "CreateTable"
        impl: "com.sqlforge.parser.hetu.HetuCreateTableParser"
    extensions:
      - "HetuPartitionClause"
      - "HetuPropertyClause"
  
  functions:
    mappings:
      - source: "NVL"
        target: "COALESCE"
        rewrite: "simple_replace"
      - source: "TO_DATE"
        target: "CAST(x AS DATE)"
        rewrite: "pattern_transform"
  
  datatypes:
    mappings:
      - source: "NUMBER(p,s)"
        target: "DECIMAL(p,s)"
        constraints: "p<=38"
  
  metadata_adapter:
    class: "HetuMetadataAdapter"
    connection_pool: "hetu-pool"
```

#### 3.2 优化引擎设计（CBO + RBO混合）

```
┌─────────────────────────────────────────────────────────────┐
│                  SQL优化引擎 (Optimizer Engine)              │
│                                                              │
│  输入: 增强AST + 统计信息 + 优化目标(延迟/吞吐/资源)           │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  规则优化层 (RBO - Rule Based)                            │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │  语法简化    │  │  谓词下推    │  │  常量折叠       │  │ │
│  │  │  (去冗余)    │  │  (Filter下推)│  │  (编译期计算)   │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │  子查询优化  │  │  JOIN重排    │  │  窗口函数优化   │  │ │
│  │  │  (展开/合并) │  │  (贪心/动态) │  │  (去重排序)     │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  │  规则库: 200+ 可配置规则，支持热加载                      │ │
│  └─────────────────────────────────────────────────────────┘ │
│     ↓                                                        │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  代价优化层 (CBO - Cost Based)                            │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │  统计信息收集│  │  代价模型    │  │  计划枚举       │  │ │
│  │  │  (直方图/NDV)│  │  (CPU/IO/Net)│  │  (遗传算法)     │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │  索引选择    │  │  分区裁剪    │  │  并行度优化     │  │ │
│  │  │  (覆盖索引)  │  │  (分区消除)  │  │  (DOP计算)      │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  └─────────────────────────────────────────────────────────┘ │
│     ↓                                                        │
│  输出: 优化后AST + 物理执行计划 + 优化建议列表 + 预期收益评估    │
└─────────────────────────────────────────────────────────────┘
```

**性能问题模式库**（可扩展）：

```java
public enum PerformancePattern {
    FULL_TABLE_SCAN("PTS-001", "全表扫描", Severity.HIGH, 
        "检测到无索引条件的全表扫描，建议添加索引或优化过滤条件"),
    
    IMPLICIT_CONVERSION("PTS-002", "隐式类型转换", Severity.MEDIUM,
        "字段类型与参数不匹配导致索引失效，建议显式转换"),
    
    CARTESIAN_PRODUCT("PTS-003", "笛卡尔积", Severity.CRITICAL,
        "多表JOIN缺少连接条件，可能导致结果集爆炸"),
    
    NESTED_LOOP_INEFFICIENT("PTS-004", "低效嵌套循环", Severity.HIGH,
        "大表驱动小表的Nested Loop JOIN，建议调整JOIN顺序"),
    
    WINDOW_FUNCTION_SPILL("PTS-005", "窗口函数溢出", Severity.MEDIUM,
        "窗口函数内存不足磁盘溢出，建议增加内存或分区处理"),
    
    SUBQUERY_CORRELATED("PTS-006", "相关子查询", Severity.HIGH,
        "每行执行一次的子查询，建议改为JOIN或物化"),
    
    // ... 共18类核心模式，支持自定义扩展
}
```

#### 3.3 安全扫描与脱敏引擎

```
┌─────────────────────────────────────────────────────────────┐
│              数据安全治理引擎 (Security Engine)              │
│                                                              │
│  输入: SQL + 执行上下文 + 安全策略配置                        │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  敏感数据发现层 (PII Detection)                          │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │  正则匹配    │  │  机器学习    │  │  元数据标记     │  │ │
│  │  │ (身份证/手机)│  │ (NER模型)   │  │  (人工标注)     │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  │  置信度融合算法，误报率<5%，召回率>95%                     │ │
│  └─────────────────────────────────────────────────────────┘ │
│     ↓                                                        │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  脱敏策略执行层 (Masking Engine)                         │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │  掩码        │  │  哈希        │  │  令牌化         │  │ │
│  │  │ (138****1234)│  │ (SHA-256)   │  │ ( reversible ) │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │  加密        │  │  截断        │  │  泛化           │  │ │
│  │  │ (AES-256)   │  │ (取前N位)    │  │ (年龄段/地域)   │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  │  策略: 基于角色(RBAC) + 数据分级(公开/内部/机密/绝密)      │ │
│  └─────────────────────────────────────────────────────────┘ │
│     ↓                                                        │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  审计与合规层 (Audit & Compliance)                       │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │ │
│  │  │  操作审计    │  │  数据血缘    │  │  合规报告       │  │ │
│  │  │ (who/when/what)│ (字段级追踪) │  │ (等保三级)      │  │ │
│  │  └─────────────┘  └─────────────┘  └─────────────────┘  │ │
│  │  存储: 防篡改日志链(WORM)，留存6个月+                     │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

#### 3.4 加速中间表生成策略

**智能物化视图推荐算法**：

```python
# 伪代码：基于查询模式的MV推荐
def recommend_materialized_views(workload_logs, storage_budget):
    # 1. 查询模式挖掘（频繁项集）
    frequent_patterns = fp_growth(workload_logs, min_support=0.05)
    
    # 2. 收益计算（查询节省 - 维护成本）
    for pattern in frequent_patterns:
        query_benefit = pattern.frequency * pattern.execution_cost * 0.7
        maintenance_cost = pattern.data_churn_rate * pattern.storage_size * 0.3
        net_benefit = query_benefit - maintenance_cost
        
        if net_benefit > threshold and pattern.storage_size < storage_budget:
            candidates.append(pattern)
    
    # 3. 贪心选择（背包问题近似解）
    selected_mvs = greedy_knapsack(candidates, storage_budget)
    
    # 4. 生成DDL与刷新策略
    for mv in selected_mvs:
        ddl = generate_mv_ddl(mv)
        refresh_strategy = choose_refresh_strategy(mv.data_freshness_req)
        yield (ddl, refresh_strategy)
```

**刷新策略矩阵**：

| 数据新鲜度要求 | 刷新策略 | 适用场景 |
|--------------|---------|---------|
| 实时 | Flink CDC实时同步 | 交易状态查询 |
| 分钟级 | 微批处理（5min窗口） | 运营监控看板 |
| 小时级 | 定时任务（整点触发） | T+1报表 |
| 日级 | 夜间批量重建 | 离线分析 |
| 手动 | 按需触发 | 临时分析 |

---

### 04 数据模型设计 (DM)

#### 4.1 核心实体关系图（ER）

```
┌─────────────────────────────────────────────────────────────┐
│                    SQLForge 核心数据模型                       │
│                                                              │
│  ┌─────────────┐       ┌─────────────┐       ┌──────────┐ │
│  │   Tenant    │1───────│   Project   │1───────│  User    │ │
│  │  (租户)      │       │  (项目空间)  │       │ (用户)   │ │
│  └─────────────┘       └─────────────┘       └──────────┘ │
│         │                     │                           │
│         │              ┌──────┴──────┐                     │
│         │              │             │                      │
│         │         ┌────▼────┐   ┌───▼────┐                │
│         │         │SQLTask  │   │DataSource│               │
│         │         │(SQL任务) │   │(数据源)  │               │
│         │         └────┬────┘   └─────────┘               │
│         │              │                                    │
│         │    ┌─────────┼─────────┐                         │
│         │    │         │         │                         │
│         │ ┌──▼───┐  ┌──▼───┐  ┌──▼───┐                     │
│         │ │Parse │  │Optimize│ │Security│                   │
│         │ │Result│  │Result │ │Result  │                     │
│         │ └──┬───┘  └──┬───┘  └──┬───┘                     │
│         │    │         │         │                          │
│         │    └─────────┼─────────┘                          │
│         │              │                                    │
│         │         ┌────▼────┐                               │
│         │         │AuditLog │                               │
│         │         │(审计日志) │                              │
│         │         └─────────┘                               │
│         │                                                   │
│    ┌────▼────┐                                              │
│    │Lineage  │◄──────────────────────────┐                  │
│    │Graph    │                           │                  │
│    │(血缘图)  │                           │                  │
│    └────┬────┘                           │                  │
│         │                                │                  │
│    ┌────▼────┐    ┌─────────────┐    ┌──▼────┐            │
│    │Metadata │◄───│SchemaRegistry│    │Policy │            │
│    │(元数据)  │    │(Schema版本)   │    │(策略)  │            │
│    └─────────┘    └─────────────┘    └───────┘            │
└─────────────────────────────────────────────────────────────┘
```

#### 4.2 核心表结构（TDSQL-PG）

**租户与权限**：

```sql
-- 租户表（多租户隔离根表）
CREATE TABLE tenant (
    tenant_id VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_code VARCHAR(64) UNIQUE NOT NULL,  -- 租户编码，如"alibaba-finance"
    tenant_name VARCHAR(128) NOT NULL,
    tenant_type VARCHAR(20) CHECK (tenant_type IN ('ENTERPRISE', 'PERSONAL', 'INTERNAL')),
    resource_quota JSONB NOT NULL,  -- {"cpu": 100, "memory": "500Gi", "storage": "10Ti"}
    security_level VARCHAR(10) DEFAULT 'L3',  -- L1/L2/L3/L4
    status VARCHAR(10) DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
) PARTITION BY LIST (tenant_type);

-- 用户表（RBAC基础）
CREATE TABLE user_account (
    user_id VARCHAR(32) PRIMARY KEY,
    tenant_id VARCHAR(32) REFERENCES tenant(tenant_id),
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128) UNIQUE NOT NULL,
    password_hash VARCHAR(256) NOT NULL,  -- Argon2id
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_secret VARCHAR(256),
    last_login_at TIMESTAMPTZ,
    status VARCHAR(10) DEFAULT 'ACTIVE',
    UNIQUE(tenant_id, username)
);

-- 角色与权限（RBAC+ABAC）
CREATE TABLE role (
    role_id VARCHAR(32) PRIMARY KEY,
    tenant_id VARCHAR(32) REFERENCES tenant(tenant_id),
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    permissions JSONB NOT NULL,  -- ["SQL_PARSE:EXECUTE", "OPTIMIZE:READ"]
    data_scope VARCHAR(20) DEFAULT 'ALL',  -- ALL/DEPT/SELF/CUSTOM
    UNIQUE(tenant_id, role_code)
);
```

**SQL任务与执行**：

```sql
-- SQL任务主表（日均千万级，按时间分区）
CREATE TABLE sql_task (
    task_id VARCHAR(32) PRIMARY KEY,
    tenant_id VARCHAR(32) REFERENCES tenant(tenant_id),
    project_id VARCHAR(32) NOT NULL,
    task_type VARCHAR(20) CHECK (task_type IN ('PARSE', 'OPTIMIZE', 'SECURITY_SCAN', 'CONVERT', 'GENERATE_MV')),
    source_dialect VARCHAR(32) NOT NULL,  -- HETU_3X, HIVE_3, MYSQL_8, etc.
    target_dialect VARCHAR(32),  -- 转换目标方言，可为空
    original_sql TEXT NOT NULL,
    sql_hash VARCHAR(64) NOT NULL,  -- SHA-256，用于去重和缓存
    status VARCHAR(10) DEFAULT 'PENDING',  -- PENDING/PROCESSING/SUCCESS/FAILED
    priority INT DEFAULT 5,  -- 1-10，数字越小优先级越高
    created_by VARCHAR(32) REFERENCES user_account(user_id),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    execution_time_ms INT,  -- 执行耗时
    result_summary JSONB  -- 脱敏后的结果摘要
) PARTITION BY RANGE (created_at);

-- 按月分区（自动化脚本创建）
CREATE TABLE sql_task_2026_04 PARTITION OF sql_task
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');

-- 解析结果详情
CREATE TABLE parse_result (
    result_id VARCHAR(32) PRIMARY KEY,
    task_id VARCHAR(32) REFERENCES sql_task(task_id),
    ast_json JSONB NOT NULL,  -- AST结构，protobuf json格式
    tables_referenced TEXT[],  -- 引用的表名数组
    columns_referenced TEXT[],  -- 引用的列名数组
    syntax_errors JSONB,  -- 语法错误列表
    warnings JSONB,  -- 警告信息
    complexity_score INT  -- 复杂度评分1-100
);

-- 优化建议结果
CREATE TABLE optimize_result (
    result_id VARCHAR(32) PRIMARY KEY,
    task_id VARCHAR(32) REFERENCES sql_task(task_id),
    optimized_sql TEXT,
    optimization_rules_applied JSONB,  -- ["PREDICATE_PUSHDOWN", "JOIN_REORDER"]
    performance_patterns JSONB,  -- 发现的问题模式
    index_recommendations JSONB,  -- 索引建议
    estimated_improvement_percent INT,  -- 预期提升百分比
    cost_before NUMERIC(20,4),  -- 优化前代价
    cost_after NUMERIC(20,4)  -- 优化后代价
);

-- 安全扫描结果
CREATE TABLE security_result (
    result_id VARCHAR(32) PRIMARY KEY,
    task_id VARCHAR(32) REFERENCES sql_task(task_id),
    sensitive_data_found BOOLEAN DEFAULT FALSE,
    pii_findings JSONB,  -- [{"type": "ID_CARD", "column": "user.id_no", "confidence": 0.98}]
    masking_applied BOOLEAN DEFAULT FALSE,
    masking_rules JSONB,  -- 应用的脱敏规则
    encryption_actions JSONB,  -- 加密操作记录
    risk_level VARCHAR(10)  -- LOW/MEDIUM/HIGH/CRITICAL
);
```

**元数据与血缘**：

```sql
-- 数据源注册
CREATE TABLE data_source (
    source_id VARCHAR(32) PRIMARY KEY,
    tenant_id VARCHAR(32) REFERENCES tenant(tenant_id),
    source_name VARCHAR(128) NOT NULL,
    source_type VARCHAR(32) NOT NULL,  -- HETU, HIVE, CLICKHOUSE, etc.
    connection_config JSONB NOT NULL,  -- 加密存储，{"host": "...", "port": 5432}
    credential_ref VARCHAR(256) NOT NULL,  -- Vault密钥引用
    sync_status VARCHAR(10) DEFAULT 'ACTIVE',
    last_sync_at TIMESTAMPTZ,
    schema_version INT DEFAULT 1
);

-- 表元数据（从数据源同步）
CREATE TABLE table_metadata (
    table_id VARCHAR(32) PRIMARY KEY,
    source_id VARCHAR(32) REFERENCES data_source(source_id),
    schema_name VARCHAR(128) NOT NULL,
    table_name VARCHAR(128) NOT NULL,
    table_type VARCHAR(20) CHECK (table_type IN ('TABLE', 'VIEW', 'MATERIALIZED_VIEW')),
    columns_meta JSONB NOT NULL,  -- [{"name": "id", "type": "BIGINT", "nullable": false}]
    statistics JSONB,  -- 统计信息 {"row_count": 1000000, "size_bytes": 1073741824}
    partition_info JSONB,  -- 分区信息
    sample_data_hash VARCHAR(64),  -- 样本数据哈希，用于变化检测
    UNIQUE(source_id, schema_name, table_name)
);

-- 列级血缘（Neo4j同步，此处为备份）
CREATE TABLE column_lineage (
    lineage_id VARCHAR(32) PRIMARY KEY,
    tenant_id VARCHAR(32),
    source_column VARCHAR(256) NOT NULL,  -- db.schema.table.column
    target_column VARCHAR(256) NOT NULL,
    transformation_type VARCHAR(32),  -- DIRECT/AGGREGATION/JOIN/FILTER
    transformation_logic TEXT,  -- 转换逻辑描述
    sql_task_id VARCHAR(32) REFERENCES sql_task(task_id),
    confidence_score NUMERIC(3,2),  -- 置信度0-1
    created_at TIMESTAMPTZ DEFAULT NOW()
) WITH (fillfactor=70);  -- 预留更新空间

-- 创建GIN索引加速JSONB查询
CREATE INDEX idx_sql_task_result_summary ON sql_task USING GIN (result_summary);
CREATE INDEX idx_parse_result_tables ON parse_result USING GIN (tables_referenced);
```

**审计与合规**（等保三级要求）：

```sql
-- 审计日志（只追加，不可修改删除）
CREATE TABLE audit_log (
    log_id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(32),
    user_id VARCHAR(32),
    action_type VARCHAR(32) NOT NULL,  -- LOGIN/SQL_SUBMIT/POLICY_CHANGE/DATA_EXPORT
    resource_type VARCHAR(32),  -- SQL_TASK/DATASOURCE/USER/POLICY
    resource_id VARCHAR(32),
    action_details JSONB NOT NULL,
    client_ip INET,
    user_agent TEXT,
    result_status VARCHAR(10),  -- SUCCESS/FAILURE
    failure_reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    integrity_hash VARCHAR(64) NOT NULL  -- 完整性校验，链式哈希
) PARTITION BY RANGE (created_at);

-- 完整性校验触发器（伪代码）
-- 每行记录hash = SHA256(上一行hash + 当前行内容)
```

#### 4.3 图模型设计（Neo4j血缘图谱）

```cypher
// 节点类型
(:Tenant {tenantId, name, securityLevel})
(:DataSource {sourceId, type, host})
(:Schema {schemaId, name})
(:Table {tableId, name, type, rowCount, sizeBytes})
(:Column {columnId, name, dataType, isNullable, isPrimaryKey, isSensitive})
(:SQLTask {taskId, type, sqlHash, createdAt})
(:Transformation {transformationId, type, logic, confidence})

// 关系类型
(Tenant)-[:OWNS]->(DataSource)
(DataSource)-[:CONTAINS]->(Schema)
(Schema)-[:CONTAINS]->(Table)
(Table)-[:HAS_COLUMN]->(Column)

// 血缘关系（核心）
(SQLTask)-[:PARSES_TO]->(Transformation)
(Column)-[:SOURCE_OF {transformationId}]->(Transformation)
(Transformation)-[:TARGET_OF]->(Column)

// 影响分析查询示例
MATCH (t:Table {name: 'user_orders'})-[:HAS_COLUMN]->(c:Column)
MATCH (c)-[:SOURCE_OF|TARGET_OF*1..5]->(related:Column)
RETURN DISTINCT related.tableName, related.name
```

---

### 05 非功能设计 (NFR)

#### 5.1 性能架构设计

**多级缓存策略**：

```
┌─────────────────────────────────────────────────────────────┐
│                    三级缓存架构 (Cache Hierarchy)            │
│                                                              │
│  L1: 本地内存缓存 (Caffeine)                                  │
│  ├── 热点SQL解析结果 (命中率>80%)                              │
│  ├── AST结构缓存 (TTL=10min)                                  │
│  └── 元数据快照 (表结构，TTL=5min)                            │
│  容量：每节点100MB，LRU淘汰                                   │
│                                                              │
│  L2: 分布式缓存 (Redis Cluster)                               │
│  ├── 跨节点共享的解析结果 (SQL Hash → Result)                 │
│  ├── 统计信息缓存 (直方图，TTL=1h)                            │
│  └── 会话状态 (TTL=30min)                                     │
│  容量：10GB集群，支持热点Key探测                               │
│                                                              │
│  L3: 近线存储 (HDFS/本地SSD)                                  │
│  ├── 历史SQL任务结果 (90天)                                   │
│  ├── 训练样本缓存 (AI模型用)                                  │
│  └── 审计日志压缩存储 (WORM)                                  │
│  容量：弹性扩展，冷数据自动下沉                                │
│                                                              │
│  缓存一致性：Cache-Aside + 消息队列失效广播                      │
└─────────────────────────────────────────────────────────────┘
```

**异步流水线**（削峰填谷）：

```yaml
流量分级处理:
  实时流:
    - 简单SQL解析 (<50ms)
    - 语法检查
    - 快速缓存命中
    处理: 同步API直接返回
    
  近线流:
    - 复杂SQL优化 (>50ms)
    - 安全深度扫描
    - 索引推荐计算
    处理: 提交任务队列，异步回调/WebSocket推送
    
  离线流:
    - 批量SQL审核 (CI流水线)
    - 全库血缘分析
    - 物化视图推荐
    处理: Flink批处理，小时级产出报告
```

#### 5.2 高可用与容灾

**多活架构**：

```
┌─────────────────────────────────────────────────────────────┐
│                    同城双活 + 异地灾备                         │
│                                                              │
│  可用区A (主)              可用区B (热备)        异地C (冷备)  │
│  ┌─────────────┐          ┌─────────────┐      ┌─────────┐ │
│  │  K8s集群    │◄────────►│  K8s集群    │      │  对象存储 │ │
│  │  (Active)   │  同步复制 │  (Standby)  │      │  (备份)  │ │
│  └─────────────┘          └─────────────┘      └─────────┘ │
│       │                        │                            │
│       └──────────┬─────────────┘                            │
│                  │                                           │
│            ┌─────▼─────┐                                    │
│            │ TDSQL-PG   │  强同步复制 (RPO≈0)                 │
│            │ 主从集群   │                                    │
│            └───────────┘                                    │
│                                                              │
│  故障切换:                                                   │
│  - 应用层：K8s自动漂移，RTO<2min                             │
│  - 数据层：TDSQL自动选主，RTO<30s                            │
│  - 极端故障：异地恢复，RTO<1h，RPO<5min（异步复制延迟）        │
└─────────────────────────────────────────────────────────────┘
```

#### 5.3 安全架构（等保三级）

**纵深防御体系**：

```
┌─────────────────────────────────────────────────────────────┐
│                    零信任安全架构 (Zero Trust)                 │
│                                                              │
│  边缘层:                                                     │
│  ├── WAF防护 (SQL注入/XSS/CC攻击)                            │
│  ├── DDoS清洗 (流量压制)                                      │
│  └── 边缘认证 (mTLS双向证书)                                  │
│                                                              │
│  接入层:                                                     │
│  ├── OAuth2.1 + PKCE (防授权码劫持)                            │
│  ├── JWT (RS256签名，15min过期)                                │
│  └── MFA (TOTP/SMS/硬件Key)                                   │
│                                                              │
│  服务层:                                                     │
│  ├── RBAC (角色-权限矩阵)                                      │
│  ├── ABAC (属性权限：部门/项目/数据敏感度)                      │
│  └── 动态鉴权 (OPA策略引擎，实时决策)                           │
│                                                              │
│  数据层:                                                     │
│  ├── 传输加密 (TLS1.3，强制前向保密)                           │
│  ├── 存储加密 (TDE透明加密，AES-256-XTS)                       │
│  ├── 字段级加密 (敏感列单独加密，HSM托管密钥)                    │
│  └── 脱敏展示 (动态/静态脱敏策略)                             │
│                                                              │
│  审计层:                                                     │
│  ├── 全量操作日志 (不可篡改，WORM存储)                          │
│  ├── 实时风控 (异常行为检测，如批量导出)                        │
│  └── 合规报告 (自动化等保测评证据收集)                          │
└─────────────────────────────────────────────────────────────┘
```

---

### 06 接口设计 (API)

#### 6.1 核心RESTful API

```yaml
openapi: 3.0.3
info:
  title: SQLForge API
  version: 1.0.0
  description: SQL全生命周期治理平台开放接口

servers:
  - url: https://api.sqlforge.io/v1

security:
  - BearerAuth: []
  - ApiKeyAuth: []

paths:
  /sql/parse:
    post:
      summary: SQL语法解析
      description: 支持多方言解析，返回AST和诊断信息
      requestBody:
        content:
          application/json:
            schema:
              type: object
              required: [sql, dialect]
              properties:
                sql:
                  type: string
                  example: "SELECT * FROM users WHERE id = 1"
                dialect:
                  type: string
                  enum: [HETU_3X, HIVE_3, MYSQL_8, ORACLE_19C, CLICKHOUSE, TDSQL, GAUSSDB, GBASE, KYLIN]
                parameters:
                  type: object
                  description: 参数化查询的变量映射
      responses:
        200:
          description: 解析成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ParseResult'
        400:
          description: 语法错误
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /sql/optimize:
    post:
      summary: SQL优化建议
      description: 基于代价模型给出优化建议，支持异步大查询
      requestBody:
        content:
          application/json:
            schema:
              type: object
              required: [sql, dialect, optimizationGoal]
              properties:
                sql:
                  type: string
                dialect:
                  type: string
                optimizationGoal:
                  type: string
                  enum: [LATENCY, THROUGHPUT, RESOURCE, BALANCED]
                schemaContext:
                  type: object
                  description: 表结构统计信息（可选，平台自动获取）
                async:
                  type: boolean
                  default: false
                  description: 是否异步执行（复杂查询建议true）
      responses:
        200:
          description: 同步结果或任务ID
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/OptimizeResult'
                  - $ref: '#/components/schemas/AsyncTaskResponse'

  /sql/convert:
    post:
      summary: SQL方言转换
      description: 将SQL从源方言转换为目标方言
      requestBody:
        content:
          application/json:
            schema:
              type: object
              required: [sql, sourceDialect, targetDialect]
              properties:
                sql:
                  type: string
                sourceDialect:
                  type: string
                targetDialect:
                  type: string
                compatibilityLevel:
                  type: string
                  enum: [STRICT, LOOSE]
                  default: STRICT
      responses:
        200:
          description: 转换结果
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ConvertResult'

  /sql/security/scan:
    post:
      summary: 安全扫描
      description: 敏感数据发现与脱敏建议
      requestBody:
        content:
          application/json:
            schema:
              type: object
              required: [sql, dataSourceId]
              properties:
                sql:
                  type: string
                dataSourceId:
                  type: string
                scanDepth:
                  type: string
                  enum: [FAST, DEEP, AI_ENHANCED]
                  default: DEEP
      responses:
        200:
          description: 扫描结果
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SecurityScanResult'

  /tasks/{taskId}:
    get:
      summary: 查询异步任务状态
      parameters:
        - name: taskId
          in: path
          required: true
          schema:
            type: string
      responses:
        200:
          description: 任务状态
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TaskStatus'

  /lineage/analyze:
    post:
      summary: 血缘分析
      description: 分析SQL的字段级血缘关系
      requestBody:
        content:
          application/json:
            schema:
              type: object
              required: [sql, dialect]
              properties:
                sql:
                  type: string
                dialect:
                  type: string
                direction:
                  type: string
                  enum: [UPSTREAM, DOWNSTREAM, BOTH]
                  default: BOTH
                depth:
                  type: integer
                  default: 5
                  description: 血缘追溯深度
      responses:
        200:
          description: 血缘图谱
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LineageGraph'

  /materialized-views/recommend:
    post:
      summary: 物化视图推荐
      description: 基于工作负载推荐加速中间表
      requestBody:
        content:
          application/json:
            schema:
              type: object
              required: [workloadId, storageBudget]
              properties:
                workloadId:
                  type: string
                  description: 历史SQL集合ID
                storageBudgetGB:
                  type: integer
                freshnessRequirement:
                  type: string
                  enum: [REALTIME, MINUTE, HOUR, DAY]
      responses:
        200:
          description: 推荐列表
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/MVRecommendations'

components:
  schemas:
    ParseResult:
      type: object
      properties:
        taskId:
          type: string
        status:
          type: string
          enum: [SUCCESS, FAILED, PARTIAL]
        ast:
          type: object
          description: JSON格式的抽象语法树
        tables:
          type: array
          items:
            type: string
        columns:
          type: array
          items:
            type: string
        syntaxErrors:
          type: array
          items:
            $ref: '#/components/schemas/SyntaxError'
        warnings:
          type: array
          items:
            $ref: '#/components/schemas/Warning'
        complexityScore:
          type: integer
          minimum: 1
          maximum: 100

    OptimizeResult:
      type: object
      properties:
        taskId:
          type: string
        originalSql:
          type: string
        optimizedSql:
          type: string
        optimizations:
          type: array
          items:
            $ref: '#/components/schemas/OptimizationRule'
        performancePatterns:
          type: array
          items:
            $ref: '#/components/schemas/PerformancePattern'
        indexRecommendations:
          type: array
          items:
            $ref: '#/components/schemas/IndexRecommendation'
        estimatedImprovement:
          type: object
          properties:
            latencyReductionPercent:
              type: integer
            costReductionPercent:
              type: integer

    SecurityScanResult:
      type: object
      properties:
        taskId:
          type: string
        riskLevel:
          type: string
          enum: [LOW, MEDIUM, HIGH, CRITICAL]
        piiFindings:
          type: array
          items:
            type: object
            properties:
              type:
                type: string
                example: "ID_CARD"
              column:
                type: string
              confidence:
                type: number
              sample:
                type: string
                description: 脱敏后的样本值
        maskingRecommendations:
          type: array
          items:
            type: object
            properties:
              column:
                type: string
              strategy:
                type: string
                enum: [MASK, HASH, TOKENIZE, ENCRYPT, TRUNCATE]

  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
    ApiKeyAuth:
      type: apiKey
      in: header
      name: X-API-Key
```

#### 6.2 WebSocket实时推送（用于长耗时任务）

```javascript
// 连接建立
const ws = new WebSocket('wss://api.sqlforge.io/v1/ws/tasks?token=JWT_TOKEN');

// 订阅任务进度
ws.send(JSON.stringify({
  action: 'SUBSCRIBE',
  taskId: 'task-123456'
}));

// 接收进度推送
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  switch(message.type) {
    case 'PROGRESS':
      updateProgressBar(message.data.percent);
      break;
    case 'RESULT':
      displayResult(message.data);
      ws.close();
      break;
    case 'ERROR':
      showError(message.data.error);
      ws.close();
      break;
  }
};
```

---

## 📋 第二阶段交付清单确认

我已交付以下核心文档：

| 文档编号 | 文档名称 | 完成状态 | 关键内容 |
|---------|---------|---------|---------|
| 01 | 项目需求规格说明书 (SRS) | ✅ | 功能/非功能需求、用户场景、验收标准 |
| 02 | 总体架构设计 (TAD) | ✅ | 逻辑/技术/部署架构、技术栈选型 |
| 03 | 核心设计 (Core Design) | ✅ | 解析引擎、优化器、安全引擎、加速策略 |
| 04 | 数据模型设计 (DM) | ✅ | ER图、TDSQL-PG表结构、Neo4j图模型 |
| 05 | 非功能设计 (NFR) | ✅ | 性能架构、高可用、安全等保三级 |
| 06 | 接口设计 (API) | ✅ | OpenAPI 3.0规范、WebSocket协议 |

---

## 📋 第三阶段待交付文档

请确认是否需要我继续输出以下文档（建议分批次）：

**批次A（开发实施必备）**：
- [ ] **07 详细设计 (LLD)** - 类图、时序图、核心算法伪代码、异常处理
- [ ] **08 功能清单 (FeatureList)** - 用户故事、功能点估算、迭代计划
- [ ] **09 安全设计深化** - 威胁建模(STRIDE)、加密方案细节、密钥管理

**批次B（工程化与质量）**：
- [ ] **10 系统分析与设计 (SAD)** - DDD领域模型、限界上下文、事件风暴
- [ ] **11 测试设计 (STD)** - 测试策略、自动化框架、性能测试方案
- [ ] **12 开发规范与Checklist** - 代码规范、数据库规范、上线清单

**批次C（运维与治理）**：
- [ ] **13 部署与运维手册** - K8s YAML清单、监控告警配置、灾备演练
- [ ] **14 用户操作手册** - 各角色使用指南、最佳实践

---

## ⚡ 立即开发所需的最小文档集

如果团队需要**立即启动开发**，我建议优先获取：

1. **详细设计 (LLD)** - 包含核心模块的类设计和接口契约
2. **功能清单** - 明确的迭代范围和优先级
3. **K8s部署清单** - 开发环境快速搭建

请回复：
- "继续批次A" → 我立即输出LLD+功能清单+安全深化
- "继续全部" → 我按顺序完成所有剩余文档
- "调整优先级" → 告诉我您希望的文档顺序

同时，如有任何已交付内容需要调整（如技术选型变更、表结构修改），请随时指出。
Kimi: 我将继续完成SQLForge项目的全部剩余文档。这是一个庞大的企业级系统，我将按照工程化最佳实践，输出可直接指导开发、测试、部署的完整技术文档体系。

---

## 07 详细设计 (LLD - Low Level Design)

### 7.1 核心模块类设计

#### 7.1.1 SQL解析引擎

```java
// ==================== 核心领域模型 ====================

/**
 * SQL任务领域对象 - 聚合根
 */
public class SQLTask {
    private TaskId taskId;
    private TenantId tenantId;
    private ProjectId projectId;
    private SQLDialect sourceDialect;
    private SQLDialect targetDialect;  // 可为空，表示仅解析不转换
    private RawSQL originalSQL;
    private SQLHash sqlHash;  // 用于缓存去重
    private TaskType taskType;
    private TaskPriority priority;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Duration executionTime;
    private TaskResult result;
    
    // 领域行为
    public void submit() { /* 提交到队列 */ }
    public void process() { /* 执行处理流水线 */ }
    public void complete(TaskResult result) { /* 完成并记录结果 */ }
    public void fail(ErrorCode error, String message) { /* 失败处理 */ }
}

/**
 * 抽象语法树 - 方言无关的统一表示
 */
public class UnifiedAST {
    private ASTNode root;
    private SQLType sqlType;  // SELECT/INSERT/UPDATE/DELETE/DDL/DCL
    private List<TableReference> tableReferences;
    private List<ColumnReference> columnReferences;
    private List<Predicate> predicates;
    private Map<String, Object> metadata;  // 方言特定扩展
    
    // 访问者模式遍历
    public <T> T accept(ASTVisitor<T> visitor) {
        return root.accept(visitor);
    }
    
    // 转换回特定方言SQL
    public String toSQL(SQLDialect targetDialect) {
        SQLGenerator generator = SQLGeneratorFactory.get(targetDialect);
        return generator.generate(this);
    }
}

/**
 * 方言抽象 - 策略模式核心
 */
public interface SQLDialect {
    String getName();
    Version getVersion();
    DialectFamily getFamily();  // PRESTO, MYSQL, ORACLE等
    
    Lexer createLexer();
    Parser createParser();
    SQLGenerator createGenerator();
    MetadataAdapter createMetadataAdapter();
    TypeSystem getTypeSystem();
    FunctionLibrary getFunctionLibrary();
    
    // 方言能力检测
    boolean supportsFeature(SQLFeature feature);
    List<CompatibilityIssue> checkCompatibility(UnifiedAST ast);
}

// ==================== 解析流水线 ====================

/**
 * 解析流水线 - 责任链模式
 */
public class ParsePipeline {
    private final List<ParseStage> stages;
    
    public ParseResult execute(ParseContext context) {
        UnifiedAST ast = null;
        List<Diagnostic> diagnostics = new ArrayList<>();
        
        for (ParseStage stage : stages) {
            StageResult result = stage.process(context, ast);
            if (result.hasOutput()) {
                ast = result.getAst();
            }
            diagnostics.addAll(result.getDiagnostics());
            
            if (result.isTerminal()) {
                break;
            }
        }
        
        return new ParseResult(ast, diagnostics);
    }
}

// 各阶段实现
public class LexicalAnalysisStage implements ParseStage {
    @Override
    public StageResult process(ParseContext ctx, UnifiedAST currentAST) {
        Lexer lexer = ctx.getDialect().createLexer();
        TokenStream tokens = lexer.tokenize(ctx.getRawSQL());
        
        // 记录词法错误
        List<Diagnostic> diagnostics = tokens.getErrors().stream()
            .map(e -> new Diagnostic(DiagnosticType.LEXICAL_ERROR, e))
            .collect(Collectors.toList());
        
        ctx.setTokenStream(tokens);
        return new StageResult(null, diagnostics, false);
    }
}

public class SyntaxAnalysisStage implements ParseStage {
    @Override
    public StageResult process(ParseContext ctx, UnifiedAST currentAST) {
        Parser parser = ctx.getDialect().createParser();
        ParseTree parseTree = parser.parse(ctx.getTokenStream());
        
        // 转换为统一AST
        ASTConverter converter = new ASTConverter(ctx.getDialect());
        UnifiedAST ast = converter.convert(parseTree);
        
        List<Diagnostic> diagnostics = parseTree.getSyntaxErrors().stream()
            .map(e -> new Diagnostic(DiagnosticType.SYNTAX_ERROR, e))
            .collect(Collectors.toList());
        
        return new StageResult(ast, diagnostics, diagnostics.stream()
            .anyMatch(d -> d.getSeverity() == Severity.ERROR));
    }
}

public class SemanticAnalysisStage implements ParseStage {
    @Override
    public StageResult process(ParseContext ctx, UnifiedAST ast) {
        SemanticChecker checker = new SemanticChecker(
            ctx.getMetadataProvider(),
            ctx.getDialect().getTypeSystem()
        );
        
        List<Diagnostic> diagnostics = checker.check(ast);
        
        // 绑定元数据
        ast = MetadataBinder.bind(ast, ctx.getMetadataProvider());
        
        // 构建血缘关系
        LineageBuilder lineageBuilder = new LineageBuilder();
        DataLineage lineage = lineageBuilder.build(ast);
        ctx.setLineage(lineage);
        
        return new StageResult(ast, diagnostics, false);
    }
}

// ==================== 方言实现示例（Hetu）====================

public class HetuDialect implements SQLDialect {
    private final Version version;
    
    @Override
    public Lexer createLexer() {
        return new HetuLexer(HetuLexerConfig.load(version));
    }
    
    @Override
    public Parser createParser() {
        // 混合策略：标准SQL用ANTLR4，Hetu扩展用自研递归下降
        return new HybridParser(
            new AntlrSQLParser(),  // 基础语法
            new HetuExtensionParser()  // 特定扩展
        );
    }
    
    @Override
    public SQLGenerator createGenerator() {
        return new HetuSQLGenerator(version);
    }
    
    @Override
    public List<CompatibilityIssue> checkCompatibility(UnifiedAST ast) {
        List<CompatibilityIssue> issues = new ArrayList<>();
        
        // 检查Hetu特定限制
        ast.accept(new ASTVisitor<Void>() {
            @Override
            public Void visit(CreateTableNode node) {
                if (node.getProperties().containsKey("orc.bloom.filter.columns")) {
                    issues.add(new CompatibilityIssue(
                        "ORC布隆过滤器在Hetu中需使用bloom_filter_columns语法",
                        IssueSeverity.WARNING,
                        "PROPERTY_SYNTAX_DIFFERENCE"
                    ));
                }
                return null;
            }
            // ... 其他检查
        });
        
        return issues;
    }
}
```

#### 7.1.2 优化引擎模块

```java
/**
 * 优化器上下文
 */
public class OptimizerContext {
    private final UnifiedAST originalAST;
    private final SQLDialect targetDialect;
    private final OptimizationGoal goal;
    private final StatisticsProvider statsProvider;
    private final CostModel costModel;
    private final RuleRegistry ruleRegistry;
    
    // 优化过程状态
    private UnifiedAST currentAST;
    private List<OptimizationTrace> appliedRules;
    private Cost currentCost;
}

/**
 * 基于规则的优化器 (RBO)
 */
public class RuleBasedOptimizer {
    private final List<OptimizationRule> rules;
    
    public OptimizeResult optimize(OptimizerContext ctx) {
        UnifiedAST ast = ctx.getCurrentAST();
        List<OptimizationTrace> traces = new ArrayList<>();
        
        boolean changed;
        do {
            changed = false;
            for (OptimizationRule rule : rules) {
                if (rule.isApplicable(ast, ctx)) {
                    OptimizationResult result = rule.apply(ast, ctx);
                    if (result.isChanged()) {
                        ast = result.getNewAST();
                        traces.add(new OptimizationTrace(rule, result));
                        changed = true;
                        
                        // 记录中间状态用于调试
                        ctx.logIntermediateState(rule.getName(), ast);
                    }
                }
            }
        } while (changed);  // 固定点迭代
        
        ctx.setCurrentAST(ast);
        return new OptimizeResult(ast, traces);
    }
}

// 具体规则实现示例
public class PredicatePushDownRule implements OptimizationRule {
    @Override
    public boolean isApplicable(UnifiedAST ast, OptimizerContext ctx) {
        // 检查是否存在可下推的谓词
        return ast.accept(new PredicatePushDownChecker());
    }
    
    @Override
    public OptimizationResult apply(UnifiedAST ast, OptimizerContext ctx) {
        PredicatePushDownTransformer transformer = new PredicatePushDownTransformer();
        UnifiedAST newAST = transformer.transform(ast);
        
        return OptimizationResult.builder()
            .changed(!newAST.equals(ast))
            .newAST(newAST)
            .benefitDescription("谓词下推减少数据扫描量")
            .estimatedCostReduction(0.3)  // 预估减少30%代价
            .build();
    }
}

/**
 * 基于代价的优化器 (CBO)
 */
public class CostBasedOptimizer {
    private final PlanEnumerator planEnumerator;
    private final CostModel costModel;
    
    public OptimizeResult optimize(OptimizerContext ctx) {
        // 1. 生成候选执行计划
        List<PhysicalPlan> candidates = planEnumerator.enumerate(ctx.getCurrentAST());
        
        // 2. 计算每个计划的代价
        List<ScoredPlan> scoredPlans = candidates.stream()
            .map(plan -> {
                Cost cost = costModel.calculate(plan, ctx.getStatistics());
                return new ScoredPlan(plan, cost);
            })
            .sorted(Comparator.comparing(ScoredPlan::getCost))
            .collect(Collectors.toList());
        
        // 3. 选择最优计划
        PhysicalPlan bestPlan = scoredPlans.get(0).getPlan();
        
        // 4. 转换回AST表示（或生成物理执行计划）
        UnifiedAST optimizedAST = PhysicalPlanConverter.toAST(bestPlan);
        
        return new OptimizeResult(
            optimizedAST,
            Collections.singletonList(new OptimizationTrace(
                "CBO_PLAN_SELECTION",
                "选择代价最低执行计划: " + bestPlan.getDescription()
            ))
        );
    }
}

/**
 * 性能模式检测器
 */
public class PerformancePatternDetector {
    private final List<PerformancePattern> patterns;
    
    public List<PatternMatch> detect(UnifiedAST ast, OptimizerContext ctx) {
        List<PatternMatch> matches = new ArrayList<>();
        
        for (PerformancePattern pattern : patterns) {
            PatternMatch match = pattern.match(ast, ctx);
            if (match.isFound()) {
                matches.add(match);
            }
        }
        
        // 按严重程度排序
        matches.sort(Comparator.comparing(m -> m.getSeverity().getPriority()));
        
        return matches;
    }
}

// 具体模式实现：全表扫描检测
public class FullTableScanPattern implements PerformancePattern {
    @Override
    public PatternMatch match(UnifiedAST ast, OptimizerContext ctx) {
        return ast.accept(new ASTVisitor<PatternMatch>() {
            @Override
            public PatternMatch visit(TableScanNode node) {
                // 检查是否有有效过滤条件
                List<Predicate> predicates = node.getPredicates();
                
                boolean hasEffectiveFilter = predicates.stream()
                    .anyMatch(p -> isEffectiveFilter(p, node.getTable()));
                
                if (!hasEffectiveFilter && node.getTable().getRowCount() > 10000) {
                    return PatternMatch.builder()
                        .patternCode("PTS-001")
                        .severity(Severity.HIGH)
                        .description("表 " + node.getTable().getName() + " 发生全表扫描，无有效过滤条件")
                        .location(node.getLocation())
                        .suggestion("添加索引或优化WHERE条件")
                        .estimatedImpact(node.getTable().getRowCount() * 0.1)  // 假设每行0.1ms
                        .build();
                }
                return PatternMatch.none();
            }
            
            private boolean isEffectiveFilter(Predicate p, TableMetadata table) {
                // 检查谓词是否能利用索引
                if (p instanceof ComparisonPredicate) {
                    ColumnReference col = ((ComparisonPredicate) p).getColumn();
                    return table.hasIndex(col.getName()) || 
                           table.isPartitionKey(col.getName());
                }
                return false;
            }
        });
    }
}
```

#### 7.1.3 安全与脱敏模块

```java
/**
 * 安全扫描引擎
 */
public class SecurityScanner {
    private final PIIDetector piiDetector;
    private final SensitivityClassifier sensitivityClassifier;
    private final MaskingRuleEngine maskingRuleEngine;
    
    public SecurityScanResult scan(SQLTask task, DataSourceContext ctx) {
        // 1. PII检测
        List<PIIFinding> piiFindings = piiDetector.detect(task.getAST(), ctx);
        
        // 2. 敏感度分级
        List<SensitiveColumn> sensitiveColumns = piiFindings.stream()
            .map(finding -> sensitivityClassifier.classify(finding, ctx))
            .collect(Collectors.toList());
        
        // 3. 脱敏策略匹配
        List<MaskingRecommendation> recommendations = sensitiveColumns.stream()
            .map(col -> maskingRuleEngine.recommend(col, ctx.getSecurityPolicy()))
            .collect(Collectors.toList());
        
        // 4. 风险评估
        RiskLevel riskLevel = calculateRiskLevel(sensitiveColumns, ctx);
        
        return SecurityScanResult.builder()
            .taskId(task.getTaskId())
            .riskLevel(riskLevel)
            .piiFindings(piiFindings)
            .sensitiveColumns(sensitiveColumns)
            .maskingRecommendations(recommendations)
            .encryptionRequirements(determineEncryptionNeeds(sensitiveColumns))
            .build();
    }
}

/**
 * PII检测器 - 混合策略
 */
public class PIIDetector {
    private final List<PatternDetector> patternDetectors;  // 正则规则
    private final NERModelClient nerModel;  // 机器学习模型
    private final MetadataTagChecker metadataChecker;  // 元数据标记
    
    public List<PIIFinding> detect(UnifiedAST ast, DataSourceContext ctx) {
        List<PIIFinding> findings = new ArrayList<>();
        
        // 多策略并行检测
        CompletableFuture<List<PIIFinding>> patternFuture = 
            CompletableFuture.supplyAsync(() -> runPatternDetection(ast));
        CompletableFuture<List<PIIFinding>> mlFuture = 
            CompletableFuture.supplyAsync(() -> runMLDetection(ast, ctx));
        CompletableFuture<List<PIIFinding>> metadataFuture = 
            CompletableFuture.supplyAsync(() -> runMetadataCheck(ast, ctx));
        
        // 结果融合与去重
        List<PIIFinding> allFindings = Stream.of(patternFuture, mlFuture, metadataFuture)
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());
        
        return mergeAndDeduplicate(allFindings);
    }
    
    private List<PIIFinding> runPatternDetection(UnifiedAST ast) {
        return ast.accept(new ASTVisitor<List<PIIFinding>>() {
            @Override
            public List<PIIFinding> visit(ColumnReference col) {
                List<PIIFinding> findings = new ArrayList<>();
                String colName = col.getName().toLowerCase();
                String sampleValue = col.getSampleValue();  // 采样数据
                
                // 列名启发式
                if (colName.contains("idcard") || colName.contains("身份证")) {
                    findings.add(createFinding(col, PIIType.ID_CARD, 0.8, "COLUMN_NAME"));
                }
                
                // 正则匹配
                if (sampleValue != null) {
                    if (RegexPatterns.ID_CARD_18.matches(sampleValue)) {
                        findings.add(createFinding(col, PIIType.ID_CARD, 0.95, "REGEX_MATCH"));
                    }
                    if (RegexPatterns.PHONE_NUMBER.matches(sampleValue)) {
                        findings.add(createFinding(col, PIIType.PHONE, 0.9, "REGEX_MATCH"));
                    }
                }
                
                return findings;
            }
        });
    }
}

/**
 * 脱敏执行引擎
 */
public class MaskingEngine {
    public String applyMasking(String originalValue, MaskingStrategy strategy, 
                               MaskingContext ctx) {
        switch (strategy.getType()) {
            case MASK:
                return applyMask(originalValue, strategy.getPattern());  // 138****1234
                
            case HASH:
                return hash(originalValue, strategy.getAlgorithm());  // SHA-256
            
            case TOKENIZE:
                return tokenize(originalValue, ctx.getTokenVault());  // 可逆令牌
            
            case ENCRYPT:
                return encrypt(originalValue, ctx.getEncryptionKey(), 
                             strategy.getAlgorithm());  // AES-256-GCM
            
            case TRUNCATE:
                return truncate(originalValue, strategy.getLength());  // 取前N位
            
            case GENERALIZE:
                return generalize(originalValue, strategy.getGeneralizationRule());  // 年龄段
            
            default:
                throw new UnsupportedOperationException("Unknown strategy: " + strategy);
        }
    }
    
    private String applyMask(String value, String pattern) {
        // 实现掩码逻辑，如手机号保留前3后4
        if (value.length() >= 11 && pattern.equals("PHONE_MASK")) {
            return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
        }
        // 其他模式...
        return value.replaceAll("(?<=.{3}).(?=.{4})", "*");
    }
}
```

#### 7.1.4 血缘分析模块

```java
/**
 * 血缘构建器
 */
public class LineageBuilder {
    public DataLineage build(UnifiedAST ast) {
        LineageGraph graph = new LineageGraph();
        
        // 1. 提取所有数据源（表/列）
        List<TableNode> tables = extractTables(ast);
        List<ColumnNode> columns = extractColumns(ast);
        
        // 2. 分析转换逻辑
        List<Transformation> transformations = analyzeTransformations(ast);
        
        // 3. 构建边关系
        for (Transformation trans : transformations) {
            for (ColumnNode source : trans.getSources()) {
                for (ColumnNode target : trans.getTargets()) {
                    LineageEdge edge = LineageEdge.builder()
                        .source(source)
                        .target(target)
                        .transformation(trans)
                        .transformationType(trans.getType())
                        .confidence(trans.getConfidence())
                        .build();
                    graph.addEdge(edge);
                }
            }
        }
        
        return new DataLineage(graph);
    }
    
    private List<Transformation> analyzeTransformations(UnifiedAST ast) {
        return ast.accept(new LineageExtractionVisitor());
    }
}

/**
 * 血缘查询服务
 */
public class LineageQueryService {
    private final LineageRepository repository;  // Neo4j实现
    
    /**
     * 影响分析：字段变更会影响哪些下游
     */
    public List<ImpactAnalysisResult> analyzeImpact(ColumnRef column, int maxDepth) {
        String cypher = """
            MATCH (source:Column {fullName: $columnName})
            CALL apoc.path.expandConfig(source, {
                relationshipFilter: 'SOURCE_OF>',
                minLevel: 1,
                maxLevel: $maxDepth,
                uniqueness: 'NODE_GLOBAL'
            }) YIELD path
            RETURN last(nodes(path)) as impactedColumn, 
                   relationships(path) as transformations,
                   length(path) as depth
            """;
        
        return repository.query(cypher, Map.of("columnName", column.getFullName(), 
                                               "maxDepth", maxDepth));
    }
    
    /**
     * 根因分析：结果异常追溯上游
     */
    public List<RootCauseResult> traceRootCause(ColumnRef anomalousColumn, 
                                                 String anomalyPattern) {
        // 逆向血缘追溯
        String cypher = """
            MATCH (target:Column {fullName: $columnName})
            CALL apoc.path.expandConfig(target, {
                relationshipFilter: '<TARGET_OF',
                minLevel: 1,
                maxLevel: 10
            }) YIELD path
            WITH nodes(path) as columns, relationships(path) as trans
            WHERE ANY(t in trans WHERE t.type in ['AGGREGATION', 'JOIN'])
            RETURN columns, trans
            ORDER BY length(path) DESC
            """;
        
        return repository.query(cypher, Map.of("columnName", anomalousColumn.getFullName()));
    }
}
```

### 7.2 核心时序图

#### 7.2.1 SQL解析优化完整流程

```
┌─────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Client │     │  API Gateway│     │ Parse Service│     │ Optimize Svc│     │ Metadata Svc│
└────┬────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
     │                 │                   │                   │                   │
     │ 1. POST /sql/optimize               │                   │                   │
     │────────────────>│                   │                   │                   │
     │                 │ 2. 认证鉴权(Security Filter)            │                   │
     │                 │───────────────────│                   │                   │
     │                 │<───────────────────│                   │                   │
     │                 │ 3. 路由+负载均衡    │                   │                   │
     │                 │──────────────────>│                   │                   │
     │                 │                   │ 4. 检查缓存(Caffeine+Redis)            │
     │                 │                   │───────────────────│                   │
     │                 │                   │<───────────────────(cache miss)      │
     │                 │                   │ 5. 调用Parser       │                   │
     │                 │                   │────┐              │                   │
     │                 │                   │    │ 5.1 词法分析   │                   │
     │                 │                   │    │ 5.2 语法分析   │                   │
     │                 │                   │    │ 5.3 语义分析   │                   │
     │                 │                   │<───┘              │                   │
     │                 │                   │ 6. 获取元数据     │                   │
     │                 │                   │──────────────────────────────────────>│
     │                 │                   │<──────────────────────────────────────│
     │                 │                   │ 7. 调用Optimizer  │                   │
     │                 │                   │──────────────────>│                   │
     │                 │                   │                   │ 7.1 RBO规则应用   │
     │                 │                   │                   │────┐              │
     │                 │                   │                   │    │ 7.1.1 谓词下推 │
     │                 │                   │                   │    │ 7.1.2 JOIN重排   │
     │                 │                   │                   │<───┘              │
     │                 │                   │                   │ 7.2 CBO代价优化   │
     │                 │                   │                   │────┐              │
     │                 │                   │                   │    │ 7.2.1 计划枚举 │
     │                 │                   │                   │    │ 7.2.2 代价计算 │
     │                 │                   │                   │<───┘              │
     │                 │                   │                   │ 7.3 性能模式检测  │
     │                 │                   │<──────────────────│                   │
     │                 │                   │ 8. 安全扫描       │                   │
     │                 │                   │────┐              │                   │
     │                 │                   │    │ 8.1 PII检测    │                   │
     │                 │                   │    │ 8.2 脱敏建议   │                   │
     │                 │                   │<───┘              │                   │
     │                 │                   │ 9. 构建血缘       │                   │
     │                 │                   │────┐              │                   │
     │                 │                   │    │ 9.1 提取关系   │                   │
     │                 │                   │    │ 9.2 写入Neo4j  │                   │
     │                 │                   │<───┘              │                   │
     │                 │                   │ 10. 写入结果+更新缓存                    │
     │                 │<──────────────────│                   │                   │
     │<────────────────│ 11. 返回JSON结果  │                   │                   │
     │                 │                   │                   │                   │
```

#### 7.2.2 异步任务处理流程（复杂SQL）

```
┌─────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Client │     │  API Gateway│     │ Task Service│     │  Kafka      │     │ Worker Pod  │
└────┬────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
     │                 │                   │                   │                   │
     │ 1. POST /sql/optimize (async=true)   │                   │                   │
     │────────────────>│                   │                   │                   │
     │                 │ 2. 创建异步任务     │                   │                   │
     │                 │──────────────────>│                   │                   │
     │                 │<──────────────────│ 3. 返回taskId      │                   │
     │<────────────────│                   │                   │                   │
     │                 │                   │ 4. 提交到Kafka队列  │                   │
     │                 │                   │──────────────────>│                   │
     │                 │                   │                   │ 5. 消息持久化      │
     │                 │                   │                   │────┐              │
     │ 6. WebSocket连接 /ws/tasks/{taskId}   │                   │    │              │
     │────────────────>│                   │                   │    │              │
     │                 │                   │                   │<───┘              │
     │                 │                   │                   │ 7. 消费者拉取     │
     │                 │                   │                   │──────────────────>│
     │                 │                   │                   │                   │ 8. 执行解析优化
     │                 │                   │                   │                   │────┐
     │                 │                   │                   │                   │    │ 8.1-8.9 处理
     │                 │                   │                   │                   │<───┘
     │                 │                   │                   │ 9. 进度更新(30%)  │
     │<──────────────────────────────────────────────────────────────────────────────│
     │ 10. 进度推送 30% │                   │                   │                   │
     │                 │                   │                   │ 11. 继续处理      │
     │<──────────────────────────────────────────────────────────────────────────────│
     │ 12. 进度推送 100%│                   │                   │                   │
     │                 │                   │                   │ 13. 提交结果      │
     │                 │                   │                   │<──────────────────│
     │                 │                   │ 14. 更新任务状态  │                   │
     │<──────────────────────────────────────────────────────────────────────────────│
     │ 15. 最终结果推送  │                   │                   │                   │
     │                 │                   │                   │                   │
```

### 7.3 核心算法伪代码

#### 7.3.1 SQL相似度计算（用于缓存命中）

```python
# 基于AST结构的相似度计算，支持参数化匹配
function calculate_sql_similarity(sql1, sql2):
    # 1. 解析为AST
    ast1 = parse(sql1)
    ast2 = parse(sql2)
    
    # 2. 规范化（去除字面量，替换为占位符）
    normalized1 = normalize(ast1)  # SELECT * FROM t WHERE id = ? AND name = ?
    normalized2 = normalize(ast2)
    
    # 3. 结构哈希比较
    if structural_hash(normalized1) == structural_hash(normalized2):
        return 1.0  # 完全匹配
    
    # 4. 树编辑距离（用于模糊匹配）
    distance = tree_edit_distance(normalized1, normalized2)
    max_nodes = max(node_count(normalized1), node_count(normalized2))
    similarity = 1 - (distance / max_nodes)
    
    # 5. 阈值判断
    if similarity > 0.95:
        return similarity  # 高度相似，可使用参数化缓存
    
    return 0  # 不相似，需重新解析

# 规范化规则
function normalize(ast):
    # 替换字面量为?
    replace_literals(ast, "?")
    # 标准化表别名
    normalize_aliases(ast)
    # 排序SELECT字段（不影响结果的情况下）
    sort_select_columns(ast)
    # 标准化JOIN顺序（基于表大小启发式）
    reorder_joins_heuristic(ast)
    return ast
```

#### 7.3.2 索引推荐算法

```python
# 基于工作负载的索引推荐
function recommend_indexes(workload_logs, storage_budget, existing_indexes):
    # 1. 提取可索引列（WHERE/JOIN/ORDER BY/GROUP BY中的列）
    candidate_columns = {}
    for query in workload_logs:
        ast = parse(query.sql)
        frequency = query.execution_count
        
        # 提取谓词列
        for predicate in extract_predicates(ast):
            column = predicate.column
            selectivity = estimate_selectivity(column, predicate.operator)
            benefit = frequency * query.avg_execution_time * (1 - selectivity)
            
            if column not in candidate_columns:
                candidate_columns[column] = {
                    'benefit': 0,
                    'queries': [],
                    'selectivities': []
                }
            candidate_columns[column]['benefit'] += benefit
            candidate_columns[column]['queries'].append(query.id)
            candidate_columns[column]['selectivities'].append(selectivity)
    
    # 2. 计算联合索引候选（频繁一起出现的列）
    column_combinations = find_column_combinations(workload_logs, max_combination_size=3)
    
    # 3. 代价效益分析
    index_candidates = []
    for columns in column_combinations:
        # 计算存储代价
        storage_cost = estimate_index_size(columns)
        
        # 计算维护代价（INSERT/UPDATE/DELETE的影响）
        maintenance_cost = calculate_maintenance_cost(columns, workload_logs)
        
        # 总效益
        total_benefit = sum(candidate_columns[col]['benefit'] for col in columns)
        
        # 净收益
        net_benefit = total_benefit - maintenance_cost
        
        index_candidates.append({
            'columns': columns,
            'storage_cost': storage_cost,
            'net_benefit': net_benefit,
            'benefit_cost_ratio': net_benefit / storage_cost
        })
    
    # 4. 背包问题求解（在存储预算内最大化收益）
    selected_indexes = knapsack_greedy(index_candidates, storage_budget)
    
    # 5. 与现有索引对比，推荐新建/删除/修改
    recommendations = diff_with_existing(selected_indexes, existing_indexes)
    
    return recommendations
```

#### 7.3.3 物化视图增量刷新算法

```python
# 基于变更数据捕获(CDC)的增量刷新
function incremental_refresh(mv_definition, source_tables, cdc_stream):
    # 1. 分析MV查询结构，确定增量计算范围
    base_query = mv_definition.query
    aggregation_type = detect_aggregation(base_query)  # SUM/COUNT/AVG/etc.
    
    # 2. 处理CDC事件流
    delta_changes = {}
    for event in cdc_stream:
        table = event.table_name
        if table not in source_tables:
            continue
            
        if event.operation == 'INSERT':
            delta_changes[table].append(('+', event.new_row))
        elif event.operation == 'DELETE':
            delta_changes[table].append(('-', event.old_row))
        elif event.operation == 'UPDATE':
            # 分解为DELETE+INSERT
            delta_changes[table].append(('-', event.old_row))
            delta_changes[table].append(('+', event.new_row))
    
    # 3. 计算增量聚合（以SUM为例）
    if aggregation_type == 'SUM':
        delta_sum = {}
        for table, changes in delta_changes.items():
            for op, row in changes:
                key = extract_grouping_key(row, base_query.group_by)
                value = row[base_query.aggregation_column]
                if op == '+':
                    delta_sum[key] = delta_sum.get(key, 0) + value
                else:
                    delta_sum[key] = delta_sum.get(key, 0) - value
        
        # 4. 合并到现有MV（UPSERT操作）
        for key, delta in delta_sum.items():
            merge_into_mv(mv_definition.name, key, delta, operation='ADD')
    
    elif aggregation_type == 'COUNT':
        # 类似逻辑，维护计数
        pass
    
    elif aggregation_type == 'AVG':
        # 需要同时维护SUM和COUNT，再计算平均值
        pass
    
    # 5. 更新元数据
    update_mv_metadata(mv_definition.name, {
        'last_refresh_time': now(),
        'delta_processed': len(cdc_stream),
        'data_freshness': calculate_freshness(source_tables)
    })
```

### 7.4 异常处理设计

```java
/**
 * 统一异常体系
 */
public abstract class SQLForgeException extends RuntimeException {
    private final ErrorCode errorCode;
    private final ErrorSeverity severity;
    private final Map<String, Object> context;
    
    // 业务异常
    public static class BusinessException extends SQLForgeException {}
    public static class ValidationException extends BusinessException {}  // 输入校验
    public static class AuthorizationException extends BusinessException {} // 权限不足
    public static class QuotaExceededException extends BusinessException {} // 配额超限
    
    // 技术异常
    public static class TechnicalException extends SQLForgeException {}
    public static class ParseException extends TechnicalException {}  // 解析失败
    public static class OptimizationException extends TechnicalException {} // 优化错误
    public static class StorageException extends TechnicalException {} // 存储故障
    
    // 外部服务异常
    public static class ExternalServiceException extends TechnicalException {}
    public static class MetadataServiceException extends ExternalServiceException {}
}

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(SQLForgeException.class)
    public ResponseEntity<ErrorResponse> handleSQLForgeException(SQLForgeException ex) {
        ErrorResponse response = ErrorResponse.builder()
            .errorCode(ex.getErrorCode().getCode())
            .message(ex.getMessage())
            .severity(ex.getSeverity())
            .timestamp(Instant.now())
            .traceId(MDC.get("traceId"))
            .path(RequestContext.getCurrentPath())
            .suggestion(ex.getErrorCode().getSuggestion())
            .build();
        
        // 根据严重程度记录日志
        if (ex.getSeverity() == ErrorSeverity.CRITICAL) {
            alertService.sendAlert(ex);
        }
        
        HttpStatus status = mapToHttpStatus(ex);
        return ResponseEntity.status(status).body(response);
    }
    
    private HttpStatus mapToHttpStatus(SQLForgeException ex) {
        return switch (ex.getClass().getSimpleName()) {
            case "ValidationException" -> HttpStatus.BAD_REQUEST;
            case "AuthorizationException" -> HttpStatus.FORBIDDEN;
            case "QuotaExceededException" -> HttpStatus.TOO_MANY_REQUESTS;
            case "ParseException" -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}

/**
 * 错误码定义
 */
public enum ErrorCode {
    // 解析类 (PXXX)
    P001("P001", "语法错误", "检查SQL语法，参考方言文档"),
    P002("P002", "不支持的语法特性", "查看该方言的功能支持矩阵"),
    P003("P003", "表不存在", "确认表名拼写或元数据同步状态"),
    
    // 优化类 (OXXX)
    O001("O001", "统计信息缺失", "执行ANALYZE TABLE收集统计信息"),
    O002("O002", "优化超时", "简化SQL或增加优化时间限制"),
    
    // 安全类 (SXXX)
    S001("S001", "敏感数据访问拒绝", "申请数据脱敏或权限提升"),
    S002("S002", "SQL注入风险", "使用参数化查询"),
    
    // 系统类 (SYSXXX)
    SYS001("SYS001", "服务暂时不可用", "稍后重试或联系运维"),
    SYS002("SYS002", "元数据服务连接失败", "检查网络或元数据库状态");
    
    private final String code;
    private final String message;
    private final String suggestion;
}
```

---

## 08 功能清单 (Feature List)

### 8.1 用户故事地图

```
用户角色: DBA
├── 作为DBA，我需要审核开发提交的SQL，以便确保生产环境稳定性
│   ├── 查看待审核SQL列表 (优先级:P0, 工作量:3天)
│   ├── 查看SQL解析结果和警告 (优先级:P0, 工作量:2天)
│   ├── 一键应用优化建议 (优先级:P1, 工作量:5天)
│   └── 批量审核通过/拒绝 (优先级:P1, 工作量:3天)
├── 作为DBA，我需要监控SQL性能基线，以便发现潜在问题
│   ├── 查看慢查询排行榜 (优先级:P0, 工作量:3天)
│   ├── 设置性能告警阈值 (优先级:P1, 工作量:2天)
│   └── 接收异常SQL告警通知 (优先级:P1, 工作量:3天)
└── 作为DBA，我需要管理数据源连接，以便平台能获取元数据
    ├── 添加/编辑数据源 (优先级:P0, 工作量:3天)
    ├── 测试连接可用性 (优先级:P0, 工作量:1天)
    └── 配置元数据同步策略 (优先级:P2, 工作量:5天)

用户角色: 开发工程师
├── 作为开发，我需要在IDE中实时检查SQL语法，以便快速修复错误
│   ├── VSCode插件安装 (优先级:P0, 工作量:5天)
│   ├── 实时语法高亮和错误提示 (优先级:P0, 工作量:8天)
│   ├── 一键格式化SQL (优先级:P1, 工作量:2天)
│   └── 方言转换快捷键 (优先级:P2, 工作量:3天)
├── 作为开发，我需要在CI流水线中自动审核SQL，以便阻断问题代码
│   ├── Jenkins插件 (优先级:P0, 工作量:8天)
│   ├── GitLab CI集成 (优先级:P1, 工作量:5天)
│   ├── GitHub Actions集成 (优先级:P2, 工作量:5天)
│   └── 质量门禁配置 (优先级:P1, 工作量:3天)
└── 作为开发，我需要理解复杂SQL的执行逻辑，以便优化性能
    ├── 可视化执行计划 (优先级:P1, 工作量:10天)
    ├── 字段级血缘追踪 (优先级:P1, 工作量:8天)
    └── 索引使用分析 (优先级:P1, 工作量:5天)

用户角色: 数据分析师
├── 作为分析师，我需要将自然语言转为SQL，以便快速获取数据
│   ├── NL2SQL对话框 (优先级:P1, 工作量:15天)
│   ├── 意图识别优化 (优先级:P2, 工作量:10天)
│   └── SQL生成结果编辑 (优先级:P1, 工作量:3天)
├── 作为分析师，我需要加速我的查询，以便及时产出报表
│   ├── 查询性能诊断 (优先级:P0, 工作量:5天)
│   ├── 物化视图推荐 (优先级:P1, 工作量:10天)
│   └── 一键创建加速表 (优先级:P1, 工作量:5天)
└── 作为分析师，我需要验证数据逻辑正确性，以便确保结论准确
    ├── 数据血缘追溯 (优先级:P2, 工作量:8天)
    └── 结果集一致性校验 (优先级:P2, 工作量:10天)

用户角色: 运维平台
├── 作为运维，我需要批量巡检SQL健康度，以便全面掌握质量
│   ├── 批量SQL上传 (优先级:P1, 工作量:3天)
│   ├── 分布式任务调度 (优先级:P1, 工作量:8天)
│   └── 质量评分报告 (优先级:P1, 工作量:5天)
├── 作为运维，我需要对接现有大数据平台，以便统一治理
│   ├── 元数据同步接口 (优先级:P0, 工作量:10天)
│   ├── 任务执行日志对接 (优先级:P1, 工作量:5天)
│   └── 统一权限体系对接 (优先级:P1, 工作量:8天)
└── 作为运维，我需要多租户资源管理，以便成本分摊
    ├── 租户创建与配置 (优先级:P0, 工作量:5天)
    ├── 资源配额监控 (优先级:P1, 工作量:5天)
    └── 用量计费报表 (优先级:P2, 工作量:5天)
```

### 8.2 功能点估算与迭代计划

**总工作量估算**：约 **240人天**（含前后端、测试、联调）

| 迭代 | 周期 | 目标 | 功能点 | 工作量 | 里程碑 |
|-----|------|------|--------|--------|--------|
| **MVP** | 4周 | 核心解析+优化 | 8个 | 60人天 | 内部可用 |
| **v1.0** | 6周 | 完整功能+安全 | 15个 | 100人天 | 生产就绪 |
| **v1.5** | 4周 | AI增强+生态 | 10个 | 50人天 | 智能平台 |
| **v2.0** | 4周 | 性能极致+扩展 | 7个 | 30人天 | 行业领先 |

**MVP迭代详细计划（Week 1-4）**：

```
Week 1: 基础架构搭建
├── Day 1-2: 项目脚手架(Spring Boot 3 + Vue3 + K8s基础配置)
├── Day 3-4: 数据库表创建(TDSQL-PG) + 基础CRUD
├── Day 5: 网关层搭建(Spring Cloud Gateway + 认证)
└── 产出: 可部署的Hello World服务

Week 2: 解析引擎核心
├── Day 1-3: ANTLR4语法文件 + 基础Lexer/Parser
├── Day 4-5: 统一AST设计 + MySQL方言实现
└── 产出: 支持MySQL基础SQL解析的API

Week 3: 优化引擎+Hetu支持
├── Day 1-2: Hetu方言适配(基于Presto扩展)
├── Day 3-4: RBO规则引擎(5个基础规则)
├── Day 5: 性能模式检测(3个核心模式)
└── 产出: Hetu SQL优化建议功能

Week 4: 前端+集成
├── Day 1-3: Vue3控制台(任务提交/结果展示)
├── Day 4: Redis缓存 + 基础性能优化
├── Day 5: 端到端测试 + 文档完善
└── 产出: MVP版本发布，支持演示
```

### 8.3 风险与应对

| 风险项 | 可能性 | 影响 | 应对策略 |
|--------|--------|------|---------|
| ANTLR4性能瓶颈(千万级SQL) | 中 | 高 | 预研自研Parser，保留Parser抽象层，可配置切换 |
| 方言差异过大导致转换失真 | 高 | 中 | 建立方言兼容性矩阵，高风险转换人工确认 |
| AI模型效果不达预期 | 中 | 中 | NL2SQL作为辅助功能，核心功能不依赖AI |
| 元数据同步延迟影响优化准确性 | 高 | 中 | 异步刷新+实时探测结合，设置置信度阈值 |
| 多租户资源隔离不彻底 | 低 | 高 | K8s NetworkPolicy + CGroup严格限制，安全审计 |

---

## 09 安全设计深化 (Security Deep Dive)

### 9.1 威胁建模 (STRIDE)

| 威胁类别 | 具体威胁 | 缓解措施 | 实现位置 |
|---------|---------|---------|---------|
| **S**poofing (伪装) | 攻击者伪造身份访问API | OAuth2.1 + PKCE + JWT(RS256) + MFA | Gateway Auth Filter |
| **T**ampering (篡改) | 修改传输中的SQL或结果 | TLS 1.3强制 + 请求签名(HMAC) | Gateway + Service Mesh |
| **R**epudiation (抵赖) | 用户否认执行过敏感操作 | 不可篡改审计日志(WORM存储) + 数字签名 | Audit Service |
| **I**nformation Disclosure (信息泄露) | 敏感数据在日志/错误中泄露 | 自动脱敏过滤器 + 分级错误信息 | Global Filter + ExceptionHandler |
| **D**enial of Service (拒绝服务) | 超大SQL或高频请求压垮服务 | 多级限流(用户/租户/全局) + 资源配额 | Sentinel + K8s ResourceQuota |
| **E**levation of Privilege (权限提升) | 普通用户获取管理员权限 | RBAC + ABAC + 最小权限 + 定期审计 | Authorization Service |

### 9.2 数据安全详细设计

**传输加密**：
- 外部通信：TLS 1.3，强制前向保密(PFS)，证书HSM托管
- 内部通信：mTLS双向认证，SPIFFE/SPIRE身份标识
- 数据库连接：SSL加密 + 证书 pinning

**存储加密**：
```yaml
加密层级:
  磁盘级:
    - TDE透明数据加密 (AES-256-XTS)
    - 密钥由KMS管理，支持密钥轮换
    
  应用级:
    - 敏感字段单独加密 (AES-256-GCM + HSM)
    - 列级加密配置化 (自动识别敏感列)
    - 加密字段不可被非授权角色查询
    
  备份级:
    - 备份文件客户端加密
    - 异地备份独立密钥
```

**密钥管理架构**：
```
┌─────────────────────────────────────────────────────────────┐
│                    HashiCorp Vault (HA集群)                    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐    │
│  │  根密钥(HSM)│  │  中间CA     │  │  动态数据库凭证      │    │
│  │  (离线存储) │  │  (自动轮换) │  │  (TTL=1h)          │    │
│  └─────────────┘  └─────────────┘  └─────────────────────┘    │
│                                                              │
│  密钥使用流程:                                                │
│  1. 服务启动时通过K8s SA JWT认证获取临时Token                    │
│  2. 按需申请数据库凭证 (Vault动态生成，自动回收)                  │
│  3. 加密操作调用Vault API (不直接接触根密钥)                    │
│  4. 审计日志记录所有密钥访问                                    │
└─────────────────────────────────────────────────────────────┘
```

### 9.3 脱敏策略配置示例

```yaml
# security/masking-policies/default.yaml
policies:
  - name: "个人身份信息保护"
    priority: 100
    match_conditions:
      - type: "column_name_pattern"
        patterns: ["*id_card*", "*身份证*", "*ssn*"]
      - type: "pii_type"
        types: ["ID_CARD", "PASSPORT"]
    
    masking_rules:
      - role: "ANALYST"  # 分析师看到掩码
        strategy: "MASK"
        pattern: "MASK_ID_CARD_18"  # 保留前3后4，中间掩码
        
      - role: "DEVELOPER"  # 开发者看到令牌
        strategy: "TOKENIZE"
        vault_path: "tokens/pii"
        reversible: true  # 可还原（需审批）
        
      - role: "AUDITOR"  # 审计员看到哈希
        strategy: "HASH"
        algorithm: "SHA-256"
        
      - role: "ADMIN"  # 管理员需二次认证后解密
        strategy: "ENCRYPT"
        require_mfa: true
        audit_level: "FULL"

  - name: "金融数据保护"
    match_conditions:
      - type: "data_classification"
        level: "FINANCIAL_SENSITIVE"
    
    masking_rules:
      - role: "*"
        strategy: "MASK"
        pattern: "MASK_CURRENCY"  # 保留整数部分，小数掩码
```

---

## 10 系统分析与设计 (SAD - DDD)

### 10.1 领域模型 (Domain Model)

```
┌─────────────────────────────────────────────────────────────┐
│                    SQLForge 领域模型                         │
│                    (战略设计 - 限界上下文)                     │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              解析上下文 (Parsing Context)                  │ │
│  │  聚合根: SQLTask, UnifiedAST                              │ │
│  │  领域服务: ParsePipeline, DialectAdapter                    │ │
│  │  领域事件: SQLParsed, ParseFailed                         │ │
│  │  值对象: RawSQL, SQLHash, Token, ASTNode                   │ │
│  └─────────────────────────────────────────────────────────┘ │
│                           ▲                                  │
│                           │ 依赖                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              优化上下文 (Optimization Context)             │ │
│  │  聚合根: OptimizationJob, ExecutionPlan                     │ │
│  │  领域服务: RuleBasedOptimizer, CostBasedOptimizer          │ │
│  │  领域事件: SQLOptimized, OptimizationFailed                 │ │
│  │  值对象: OptimizationRule, Cost, Statistics               │ │
│  └─────────────────────────────────────────────────────────┘ │
│                           ▲                                  │
│                           │ 依赖                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              安全上下文 (Security Context)                  │ │
│  │  聚合根: SecurityScan, DataMaskingPolicy                    │ │
│  │  领域服务: PIIDetector, MaskingEngine, AuditLogger          │ │
│  │  领域事件: SensitiveDataFound, MaskingApplied              │ │
│  │  值对象: PIIFinding, MaskingStrategy, RiskLevel           │ │
│  └─────────────────────────────────────────────────────────┘ │
│                           ▲                                  │
│                           │ 依赖                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              元数据上下文 (Metadata Context)                 │ │
│  │  聚合根: DataSource, TableMetadata, ColumnMetadata          │ │
│  │  领域服务: SchemaSyncService, StatisticsCollector           │ │
│  │  领域事件: SchemaChanged, StatisticsUpdated                 │ │
│  │  值对象: ColumnType, IndexInfo, PartitionInfo             │ │
│  └─────────────────────────────────────────────────────────┘ │
│                           ▲                                  │
│                           │ 依赖                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │              血缘上下文 (Lineage Context)                   │ │
│  │  聚合根: DataLineage, LineageGraph                         │ │
│  │  领域服务: LineageBuilder, ImpactAnalyzer                  │ │
│  │  领域事件: LineageDiscovered, ImpactAnalyzed               │ │
│  │  值对象: LineageEdge, Transformation, ColumnRef             │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
│  支撑域:                                                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  租户上下文  │  │  通知上下文  │  │   计费上下文        │  │
│  │  (多租户)   │  │  (消息推送)  │  │   (用量统计)        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 10.2 限界上下文映射

| 上下文 | 类型 | 集成方式 | 团队规模 |
|--------|------|---------|---------|
| 解析上下文 | 核心域 | 开放主机服务(REST+GRPC)，发布语言(AST Schema) | 3人 |
| 优化上下文 | 核心域 | 与解析上下文共享内核，防腐层隔离 | 3人 |
| 安全上下文 | 核心域 | 通用语言(PII类型标准)，客户-供应商关系 | 2人 |
| 元数据上下文 | 支撑域 |  conformist模式(遵循数据源协议) | 2人 |
| 血缘上下文 | 通用域 | 发布-订阅模式(领域事件) | 2人 |
| 租户/通知/计费 | 通用域 | 服务层直接调用 | 共享团队 |

### 10.3 领域事件流

```java
// 解析完成事件
public record SQLParsedEvent(
    TaskId taskId,
    TenantId tenantId,
    SQLDialect dialect,
    List<TableReference> tables,
    List<ColumnReference> columns,
    Instant occurredOn
) implements DomainEvent {}

// 优化完成事件
public record SQLOptimizedEvent(
    TaskId taskId,
    List<OptimizationRule> appliedRules,
    Cost originalCost,
    Cost optimizedCost,
    Instant occurredOn
) implements DomainEvent {}

// 敏感数据发现事件（触发实时告警）
public record SensitiveDataDiscoveredEvent(
    TaskId taskId,
    TenantId tenantId,
    UserId userId,
    RiskLevel riskLevel,
    List<PIIFinding> findings,
    Instant occurredOn
) implements DomainEvent {}

// 事件处理流程
@Component
public class SensitiveDataEventHandler {
    @EventListener
    public void handle(SensitiveDataDiscoveredEvent event) {
        if (event.riskLevel() == RiskLevel.CRITICAL) {
            // 实时阻断
            taskService.blockTask(event.taskId());
            
            // 发送告警
            alertService.sendAlert(AlertLevel.HIGH, 
                "检测到高风险敏感数据访问: " + event.findings());
            
            // 记录审计
            auditService.record(event);
        }
    }
}
```

---

## 11 测试设计 (STD)

### 11.1 测试策略金字塔

```
                    ┌─────────┐
                    │  E2E测试 │  ← 10% (关键路径)
                    │ (Cypress│     用户旅程验证
                    │  Selenium)
                   ┌┴─────────┴┐
                   │  集成测试  │  ← 20% (服务边界)
                   │ (TestContainers│  数据库/消息队列交互
                   │  WireMock) │
                  ┌┴───────────┴┐
                  │   单元测试    │  ← 70% (业务逻辑)
                  │ (JUnit5     │   领域模型/算法/工具类
                  │  Mockito)   │
                  └─────────────┘
                  
覆盖率目标:
- 核心领域代码: 90%+
- 基础设施代码: 70%+
- 整体项目: 80%+
```

### 11.2 关键测试场景

**解析引擎测试**：

```java
@ParameterizedTest
@CsvSource({
    "SELECT * FROM users, MYSQL_8, true",  // 基础查询
    "SELECT * FROM users, HETU_3X, true",  // Hetu方言
    "SELECT * FROM, MYSQL_8, false",       // 语法错误
    "SELECT /*+ INDEX(users) */ * FROM users, ORACLE_19C, true"  // Hint
})
void testParseSQL(String sql, String dialect, boolean shouldSucceed) {
    ParseResult result = parser.parse(sql, SQLDialect.of(dialect));
    assertEquals(shouldSucceed, result.isSuccess());
}

@Test
void testComplexNestedQuery() {
    String sql = """
        WITH regional_sales AS (
            SELECT region, SUM(amount) AS total_sales
            FROM orders
            GROUP BY region
        ), top_regions AS (
            SELECT region
            FROM regional_sales
            WHERE total_sales > (SELECT SUM(total_sales)/10 FROM regional_sales)
        )
        SELECT region, product, SUM(quantity) AS product_units,
               SUM(amount) AS product_sales
        FROM orders
        WHERE region IN (SELECT region FROM top_regions)
        GROUP BY region, product
        """;
    
    ParseResult result = parser.parse(sql, SQLDialect.HETU_3X);
    assertTrue(result.isSuccess());
    assertEquals(2, result.getAST().getCTEs().size());  // 2个CTE
}
```

**优化引擎测试**：

```java
@Test
void testPredicatePushDown() {
    String original = "SELECT * FROM orders o JOIN customers c ON o.cust_id = c.id " +
                     "WHERE c.region = 'APAC'";
    
    OptimizeResult result = optimizer.optimize(original, MYSQL_8);
    
    // 验证谓词已下推到customers表
    String optimized = result.getOptimizedSQL();
    assertTrue(optimized.contains("customers") && optimized.contains("region = 'APAC'"));
    assertTrue(result.getAppliedRules().contains("PREDICATE_PUSHDOWN"));
    
    // 验证代价降低
    assertTrue(result.getCostReductionPercent() > 20);
}

@Test
void testIndexRecommendation() {
    String sql = "SELECT * FROM orders WHERE order_date > '2024-01-01' AND status = 'PENDING'";
    
    OptimizeResult result = optimizer.optimize(sql, MYSQL_8);
    
    // 验证索引建议
    List<IndexRecommendation> indexes = result.getIndexRecommendations();
    assertTrue(indexes.stream().anyMatch(i -> 
        i.getColumns().contains("order_date") && i.getColumns().contains("status")
    ));
}
```

**安全扫描测试**：

```java
@Test
void testPIIDetection() {
    String sql = "SELECT id_card, phone, name FROM customers WHERE id = 1";
    
    SecurityScanResult result = securityScanner.scan(sql, createContext());
    
    assertEquals(RiskLevel.HIGH, result.getRiskLevel());
    assertTrue(result.getPiiFindings().stream()
        .anyMatch(f -> f.getType() == PIIType.ID_CARD));
    assertTrue(result.getPiiFindings().stream()
        .anyMatch(f -> f.getType() == PIIType.PHONE));
}

@Test
void testMaskingApplication() {
    String sql = "SELECT phone FROM customers";
    
    SecurityScanResult result = securityScanner.scan(sql, analystContext());
    
    // 验证脱敏策略应用
    assertTrue(result.isMaskingApplied());
    assertEquals(MaskingStrategy.MASK, result.getAppliedStrategy());
}
```

**性能测试方案**：

```yaml
# performance-test-plan.yaml
scenarios:
  - name: "解析吞吐量测试"
    duration: 10m
    ramp_up: 1m
    target_throughput: 1000  # QPS
    sql_complexity: mixed  # simple:medium:complex = 50:40:10
    assertions:
      - p99_latency < 200ms
      - error_rate < 0.1%
      - cpu_usage < 70%
      
  - name: "并发压力测试"
    duration: 30m
    concurrent_users: 1000
    tenant_count: 50  # 多租户隔离验证
    assertions:
      - no_resource_leak
      - tenant_isolation_maintained
      - no_deadlock
      
  - name: "大数据量血缘分析"
    sql_count: 100000
    table_count: 1000
    column_count: 10000
    assertions:
      - lineage_build_time < 10min
      - memory_usage < 8GB
      - graph_query_p99 < 100ms
```

### 11.3 兼容性测试矩阵

| 数据库类型 | 版本 | 测试重点 | 自动化 |
|-----------|------|---------|--------|
| MRS Hetu | 3.0.0, 3.1.0 | 复杂JOIN、窗口函数、CTE | ✅ TestContainers |
| Hive | 3.1.2, 3.1.3 | 分区语法、UDF、ACID表 | ✅ TestContainers |
| ClickHouse | 22.x, 23.x | 数组类型、物化视图、分布式 | ✅ CI集成 |
| TDSQL-PG | 最新 | 分布式事务、分区表 | ⚠️ 专用环境 |
| MySQL | 5.7, 8.0 | 标准SQL、JSON函数 | ✅ TestContainers |
| Oracle | 19c, 21c | PL/SQL、分析函数、Hint | ⚠️ 专用环境 |
| GaussDB | openGauss 3.0 | 存储过程、分区策略 | ⚠️ 专用环境 |

---

## 12 开发规范与Checklist

### 12.1 代码规范 (Java)

```java
/**
 * 代码规范示例
 */
@Tag(name = "SQL解析服务")  // OpenAPI文档标签
@RestController
@RequestMapping("/api/v1/sql")
@Validated  // 参数校验
@RequiredArgsConstructor  // Lombok构造器注入
public class SQLParseController {
    
    // 依赖注入：构造器注入优先
    private final SQLParseApplicationService parseService;
    private final MetricsCollector metrics;
    
    /**
     * 解析SQL接口
     * 
     * @param request 解析请求
     * @return 解析结果
     * @throws ValidationException 参数校验失败
     */
    @Operation(summary = "解析SQL", description = "支持多方言语法解析")
    @PostMapping("/parse")
    public Result<ParseResponse> parse(
            @Valid @RequestBody ParseRequest request) {
        
        // 1. 参数校验（@Valid自动处理）
        
        // 2. 调用应用服务
        ParseResultDTO result = parseService.parse(request.toCommand());
        
        // 3. 记录指标
        metrics.recordParseLatency(
            request.getDialect(), 
            result.getExecutionTimeMs()
        );
        
        // 4. 返回结果
        return Result.success(ParseResponse.from(result));
    }
}

// 领域对象设计规范
public class SQLTask {
    // 1. 使用值对象封装基础类型
    private final TaskId taskId;  // 不是String
    private final TenantId tenantId;
    
    // 2. 业务规则封装在领域对象中
    public void validate() {
        if (originalSQL.length() > MAX_SQL_LENGTH) {
            throw new ValidationException("SQL长度超过限制: " + MAX_SQL_LENGTH);
        }
    }
    
    // 3. 状态转换通过领域方法，禁止直接set
    public void complete(ParseResult result) {
        if (this.status != TaskStatus.PROCESSING) {
            throw new IllegalStateException("只有处理中的任务才能完成");
        }
        this.status = TaskStatus.SUCCESS;
        this.result = result;
        this.completedAt = Instant.now();
        
        // 发布领域事件
        registerEvent(new SQLParsedEvent(this));
    }
}
```

### 12.2 数据库规范

```sql
-- 命名规范
-- 表名：小写下划线，业务域_实体，如 parse_task, optimize_result
-- 字段名：小写下划线，如 created_at, execution_time_ms
-- 索引名：idx_表名_字段名，如 idx_parse_task_tenant_id

-- 设计规范示例
CREATE TABLE sql_task (
    -- 主键：UUID，避免自增ID分片热点
    task_id VARCHAR(32) PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 外键：逻辑外键，不建物理外键（性能考虑），应用层保证
    tenant_id VARCHAR(32) NOT NULL,
    
    -- 枚举：使用CHECK约束，文档注释
    status VARCHAR(10) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED')),
    
    -- 时间：统一使用TIMESTAMPTZ，带时区
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    
    -- 大文本：超过1KB使用TEXT，评估是否需要单独存储
    original_sql TEXT NOT NULL,
    
    -- JSON：明确schema，使用JSONB，建GIN索引
    result_summary JSONB,
    
    -- 软删除：不使用DELETE，使用status='DELETED'或deleted_at字段
    deleted_at TIMESTAMPTZ,  -- NULL表示未删除
    
    -- 分区键：时间范围分区，必须包含在PK或唯一索引中
    -- 注意：TDSQL-PG分区表有特定限制
) PARTITION BY RANGE (created_at);

-- 索引策略
-- 1. 等值查询：B-tree索引
CREATE INDEX idx_sql_task_tenant_status ON sql_task (tenant_id, status) 
    WHERE deleted_at IS NULL;  -- 部分索引，排除已删除

-- 2. JSON查询：GIN索引
CREATE INDEX idx_sql_task_result ON sql_task USING GIN (result_summary);

-- 3. 时间范围查询：BRIN索引（如果数据按时间自然有序）
CREATE INDEX idx_sql_task_created_brin ON sql_task USING BRIN (created_at);

-- 触发器：自动更新updated_at
CREATE TRIGGER update_sql_task_modtime
    BEFORE UPDATE ON sql_task
    FOR EACH ROW
    EXECUTE FUNCTION update_modified_column();
```

### 12.3 上线Checklist

```markdown
# SQLForge 上线检查清单 (v1.0)

## 基础设施
- [ ] K8s集群资源充足（CPU/Memory/Storage）
- [ ] TDSQL-PG主从同步正常，延迟<1s
- [ ] Redis Cluster所有节点健康
- [ ] Kafka集群Broker可用，Topic分区合理
- [ ] Neo4j因果集群选主正常
- [ ] HDFS NameNode HA状态正常

## 安全配置
- [ ] TLS证书有效且未过期
- [ ] Vault seal状态检查，unseal key备份
- [ ] 数据库加密密钥轮换策略配置
- [ ] WAF规则已加载，DDoS防护启用
- [ ] 安全组/NetworkPolicy限制生效

## 应用部署
- [ ] 镜像版本正确，digest校验通过
- [ ] 配置文件ConfigMap已更新
- [ ] 环境变量注入正确（非敏感信息）
- [ ] 敏感信息从Vault/Secret挂载
- [ ] 健康检查端点（/actuator/health）响应正常
- [ ] 指标端点（/actuator/prometheus）数据完整

## 功能验证
- [ ] 核心API冒烟测试通过（parse/optimize/convert）
- [ ] 各数据库方言连接测试通过
- [ ] 缓存读写正常（Caffeine+Redis）
- [ ] 消息队列生产消费正常
- [ ] 血缘写入Neo4j可查询

## 性能基线
- [ ] 解析接口P99延迟<200ms（并发100）
- [ ] 优化接口P99延迟<1000ms（复杂SQL）
- [ ] 内存使用<80%限制
- [ ] GC暂停<100ms
- [ ] 线程池无阻塞

## 监控告警
- [ ] Prometheus规则已加载
- [ ] Grafana仪表板可访问
- [ ] 告警通道测试（PagerDuty/钉钉/邮件）
- [ ] 日志收集（ELK/Loki）正常
- [ ] 分布式追踪（SkyWalking）链路完整

## 灾备演练
- [ ] 主TDSQL故障，自动切换验证
- [ ] 单Pod故障，K8s自动重建验证
- [ ] 备份恢复流程验证（RPO/RTO达标）

## 文档与培训
- [ ] 运维手册已更新
- [ ] 用户操作手册已发布
- [ ] 值班人员培训完成
- [ ] 紧急联系人清单确认

**上线审批**: [ ] 架构师 [ ] 安全负责人 [ ] 运维负责人 [ ] 产品经理
```

---

## 13 部署与运维手册

### 13.1 K8s部署清单（核心组件）

```yaml
# sqlforge-namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: sqlforge-prod
  labels:
    istio-injection: enabled  # 服务网格
    pod-security.kubernetes.io/enforce: restricted
---
# sqlforge-gateway.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sqlforge-gateway
  namespace: sqlforge-prod
spec:
  replicas: 3
  strategy:
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 0
  selector:
    matchLabels:
      app: sqlforge-gateway
  template:
    metadata:
      labels:
        app: sqlforge-gateway
    spec:
      serviceAccountName: sqlforge-sa
      securityContext:
        runAsNonRoot: true
        seccompProfile:
          type: RuntimeDefault
      containers:
        - name: gateway
          image: registry.sqlforge.io/gateway:v1.0.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
              name: http
            - containerPort: 10443
              name: https
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
          volumeMounts:
            - name: vault-config
              mountPath: /etc/vault
              readOnly: true
      volumes:
        - name: vault-config
          csi:
            driver: secrets-store.csi.k8s.io
            readOnly: true
            volumeAttributes:
              secretProviderClass: vault-sqlforge
---
# HPA自动扩缩容
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: sqlforge-gateway-hpa
  namespace: sqlforge-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: sqlforge-gateway
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
    - type: Pods
      pods:
        metric:
          name: http_requests_per_second
        target:
          type: AverageValue
          averageValue: "1000"
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # 5分钟稳定期，防止抖动
---
# 网络策略：严格限制流量
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: sqlforge-default-deny
  namespace: sqlforge-prod
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-gateway-to-service
  namespace: sqlforge-prod
spec:
  podSelector:
    matchLabels:
      app: sqlforge-parse-service
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: sqlforge-gateway
      ports:
        - protocol: TCP
          port: 8080
```

### 13.2 监控告警规则

```yaml
# prometheus-rules.yaml
groups:
  - name: sqlforge-alerts
    rules:
      # 服务可用性
      - alert: SQLForgeHighErrorRate
        expr: |
          (
            sum(rate(http_requests_total{status=~"5.."}[5m])) 
            / 
            sum(rate(http_requests_total[5m]))
          ) > 0.01
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "SQLForge错误率过高"
          description: "服务 {{ $labels.service }} 5xx错误率超过1%"
          
      # 性能告警
      - alert: SQLParseLatencyHigh
        expr: |
          histogram_quantile(0.99, 
            sum(rate(sql_parse_duration_seconds_bucket[5m])) by (le)
          ) > 0.5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "SQL解析延迟过高"
          description: "P99解析延迟超过500ms"
          
      # 业务告警
      - alert: SensitiveDataAccessSpike
        expr: |
          increase(security_sensitive_data_found_total[1h]) > 100
        labels:
          severity: warning
        annotations:
          summary: "敏感数据访问激增"
          description: "过去1小时发现
User: 继续
Kimi: 我将继续完成SQLForge项目的剩余文档，包括监控告警、部署运维等关键内容。

---

## 13.2 监控告警规则（续）

```yaml
# prometheus-rules.yaml（续）
groups:
  - name: sqlforge-alerts
    rules:
      # 业务告警（续）
      - alert: SensitiveDataAccessSpike
        expr: |
          increase(security_sensitive_data_found_total[1h]) > 100
        labels:
          severity: warning
        annotations:
          summary: "敏感数据访问激增"
          description: "过去1小时发现 {{ $value }} 次敏感数据访问，请检查是否存在异常查询"
          
      - alert: SQLInjectionAttempt
        expr: |
          increase(security_sql_injection_blocked_total[5m]) > 5
        labels:
          severity: critical
        annotations:
          summary: "检测到SQL注入攻击"
          description: "过去5分钟拦截 {{ $value }} 次可疑SQL，来源IP: {{ $labels.client_ip }}"
          
      # 资源告警
      - alert: ParserPodMemoryHigh
        expr: |
          container_memory_working_set_bytes{container="parser"} 
          / 
          container_spec_memory_limit_bytes{container="parser"} > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "解析服务内存使用率过高"
          description: "Pod {{ $labels.pod }} 内存使用超过85%，考虑扩容或优化"
          
      - alert: CacheEvictionRateHigh
        expr: |
          rate(caffeine_cache_eviction_total[5m]) > 100
        labels:
          severity: info
        annotations:
          summary: "缓存淘汰率过高"
          description: "本地缓存频繁淘汰，建议评估缓存容量配置"
          
      # 依赖服务告警
      - alert: MetadataServiceUnavailable
        expr: |
          up{job="metadata-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "元数据服务不可用"
          description: "元数据服务已宕机，SQL解析将依赖缓存数据"
          
      - alert: DatabaseConnectionPoolExhausted
        expr: |
          hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "数据库连接池耗尽"
          description: "活跃连接数超过90%，可能存在连接泄露"

      # 数据一致性告警
      - alert: LineageSyncLag
        expr: |
          time() - lineage_last_sync_timestamp > 3600
        labels:
          severity: warning
        annotations:
          summary: "血缘数据同步滞后"
          description: "血缘数据超过1小时未同步，影响影响分析准确性"
```

### 13.3 Grafana仪表板配置

```json
{
  "dashboard": {
    "title": "SQLForge 运营大盘",
    "tags": ["sqlforge", "production"],
    "timezone": "Asia/Shanghai",
    "panels": [
      {
        "id": 1,
        "title": "实时QPS",
        "type": "stat",
        "targets": [{
          "expr": "sum(rate(http_requests_total[1m]))",
          "legendFormat": "总QPS"
        }],
        "alert": {
          "conditions": [{"evaluator": {"params": [5000], "type": "gt"}}]
        }
      },
      {
        "id": 2,
        "title": "解析延迟分布",
        "type": "heatmap",
        "targets": [{
          "expr": "sum(rate(sql_parse_duration_seconds_bucket[5m])) by (le)",
          "format": "heatmap"
        }],
        "dataFormat": "tsbuckets"
      },
      {
        "id": 3,
        "title": "方言使用分布",
        "type": "piechart",
        "targets": [{
          "expr": "sum by (dialect) (increase(sql_parse_total[1h]))",
          "legendFormat": "{{ dialect }}"
        }]
      },
      {
        "id": 4,
        "title": "优化效果统计",
        "type": "graph",
        "targets": [
          {
            "expr": "avg(sql_optimize_cost_reduction_percent)",
            "legendFormat": "平均代价降低%"
          },
          {
            "expr": "count(sql_optimize_index_recommendation_total > 0)",
            "legendFormat": "有索引建议的SQL"
          }
        ]
      },
      {
        "id": 5,
        "title": "安全风险趋势",
        "type": "graph",
        "targets": [
          {
            "expr": "increase(security_sensitive_data_found_total[5m])",
            "legendFormat": "敏感数据发现"
          },
          {
            "expr": "increase(security_sql_injection_blocked_total[5m])",
            "legendFormat": "SQL注入拦截"
          }
        ]
      },
      {
        "id": 6,
        "title": "多租户资源使用",
        "type": "table",
        "targets": [{
          "expr": "sum by (tenant_id) (sql_task_execution_seconds_sum)",
          "format": "table",
          "instant": true
        }],
        "transformations": [
          {"id": "organize", "options": {"renameByName": {"Value": "CPU时间(秒)"}}}
        ]
      }
    ]
  }
}
```

### 13.4 日志收集与审计

```yaml
# fluent-bit-config.yaml
pipeline:
  inputs:
    - name: tail
      tag: sqlforge.app
      path: /var/log/sqlforge/*.log
      parser: json
      mem_buf_limit: 50MB
      
  filters:
    - name: lua
      match: sqlforge.app
      script: /fluent-bit/scripts/mask_sensitive.lua  # 脱敏脚本
      call: mask_pii
      
    - name: nest
      match: sqlforge.app
      operation: lift
      nested_under: kubernetes
      add_prefix: k8s_
      
  outputs:
    - name: loki
      match: sqlforge.app
      host: loki.monitoring.svc.cluster.local
      port: 3100
      labels: job=sqlforge, app=${k8s_labels_app}, tenant=${tenant_id}
      
    - name: s3
      match: sqlforge.audit  # 审计日志单独存储
      region: cn-north-1
      bucket: sqlforge-audit-logs
      total_file_size: 100M
      upload_timeout: 5m
      s3_key_format: /${TAG}/${TENANT_ID}/${YEAR}/${MONTH}/${DAY}/${HOUR}/${UUID}.gz
      
    - name: es
      match: sqlforge.security  # 安全事件实时索引
      host: elasticsearch.security.svc.cluster.local
      index: sqlforge-security-${YEAR}.${MONTH}.${DAY}
```

**审计日志格式**：

```json
{
  "timestamp": "2026-04-02T11:34:56.789Z",
  "trace_id": "4f6d9c8e3a2b1f5e",
  "tenant_id": "tenant-abc123",
  "user_id": "user-xyz789",
  "action": "SQL_PARSE",
  "resource_type": "SQL_TASK",
  "resource_id": "task-123456",
  "action_details": {
    "sql_hash": "a1b2c3d4...",
    "dialect": "HETU_3X",
    "result_status": "SUCCESS",
    "execution_time_ms": 45,
    "tables_accessed": ["db1.orders", "db1.customers"],
    "sensitive_data_found": false
  },
  "client_ip": "10.0.1.25",
  "user_agent": "SQLForge-IDE-Plugin/1.0.0",
  "integrity_hash": "sha256:9f86d08...",
  "prev_hash": "sha256:e3b0c44..."  // 链式哈希防篡改
}
```

---

## 14 用户操作手册

### 14.1 DBA操作指南

**场景1：SQL审核流程**

```
1. 登录控制台 → 审核中心 → 待审核列表
2. 查看SQL详情：
   - 原始SQL与格式化对比
   - 语法错误高亮（如有）
   - 性能问题警告（全表扫描、隐式转换等）
   - 安全风险评估
3. 操作选择：
   - [通过]：允许进入生产环境
   - [拒绝]：填写拒绝原因，自动通知开发者
   - [建议优化]：一键应用优化规则，生成优化后SQL
4. 批量审核：勾选多条SQL，批量通过/拒绝
5. 查看审核历史：支持按开发者/时间/数据库类型筛选
```

**场景2：性能基线配置**

```
1. 进入 监控中心 → 性能基线
2. 配置阈值：
   - 慢查询定义：执行时间>1s
   - 大表扫描阈值：扫描行数>10万
   - 内存排序阈值：使用内存>1GB
3. 设置告警通知：
   - 钉钉群机器人
   - 邮件通知
   - PagerDuty集成（企业版）
4. 查看趋势图：按天/周/月查看性能指标变化
```

### 14.2 开发者操作指南

**IDE插件使用（VSCode）**

```
1. 安装插件：市场搜索"SQLForge"，安装后重启VSCode
2. 配置连接：
   - Ctrl+Shift+P → SQLForge: Configure Connection
   - 输入服务端点：https://sqlforge.company.com/api/v1
   - 输入API Key（从控制台个人设置获取）
3. 实时检查：
   - 编写SQL时自动语法高亮
   - 错误实时下划线提示，悬停查看详情
   - 右键 → "SQLForge: 优化此SQL" 查看建议
4. 方言转换：
   - 选中SQL → 右键 → "转换为..."
   - 选择目标方言（如MySQL→ClickHouse）
   - 查看转换后的SQL和兼容性警告
```

**CI/CD集成（Jenkins Pipeline）**

```groovy
// Jenkinsfile示例
pipeline {
    agent any
    
    stages {
        stage('SQL审核') {
            steps {
                script {
                    def sqlFiles = findFiles(glob: 'src/**/*.sql')
                    
                    sqlFiles.each { file ->
                        def response = httpRequest(
                            url: "${SQLFORGE_URL}/sql/batch-audit",
                            httpMode: 'POST',
                            contentType: 'APPLICATION_JSON',
                            requestBody: """
                                {
                                    "files": ["${file.path}"],
                                    "dialect": "MYSQL_8",
                                    "qualityGate": "STANDARD"
                                }
                            """,
                            customHeaders: [
                                [name: 'X-API-Key', value: env.SQLFORGE_API_KEY]
                            ]
                        )
                        
                        def result = readJSON text: response.content
                        if (result.blockingIssues > 0) {
                            error("SQL审核未通过: ${file.name} 存在阻断性问题")
                        }
                    }
                }
            }
        }
    }
    
    post {
        always {
            // 上传质量报告到SonarQube
            sonarQubeScanner(
                properties: [
                    "sonar.sqlforge.reportPath": "sqlforge-report.json"
                ]
            )
        }
    }
}
```

### 14.3 数据分析师操作指南

**NL2SQL使用**

```
1. 进入 智能中心 → NL2SQL
2. 选择数据范围：
   - 数据源：订单库/用户库/商品库
   - 时间范围：最近7天/30天/自定义
3. 输入自然语言：
   示例："查询上个月销售额TOP10的商品，按品类分组"
4. 查看生成结果：
   - 生成的SQL（可编辑）
   - 解释：点击"解释SQL"查看执行逻辑
   - 预览：小数据量预览结果
5. 优化建议：
   - 如果SQL复杂，自动推荐物化视图
   - 点击"创建加速表"一键生成
```

**血缘追踪**

```
1. 在SQL编辑页面，点击"血缘分析"按钮
2. 查看可视化图谱：
   - 上游：数据来源于哪些表/字段
   - 下游：影响哪些报表/指标
   - 变换：中间的JOIN/聚合/计算逻辑
3. 影响分析：
   - 模拟修改某字段，查看受影响范围
   - 导出影响清单（Excel格式）
4. 根因分析：
   - 发现数据异常时，逆向追溯上游变更
   - 标记可疑的数据源
```

---

## 15 附录

### 15.1 术语表

| 术语 | 英文 | 定义 |
|-----|------|------|
| 抽象语法树 | AST (Abstract Syntax Tree) | SQL的结构化表示，独立于具体方言 |
| 方言 | Dialect | 特定数据库的SQL语法变体（如MySQL、Oracle） |
| 基于规则的优化 | RBO (Rule-Based Optimization) | 通过预定义规则转换SQL |
| 基于代价的优化 | CBO (Cost-Based Optimization) | 通过统计信息计算最优执行计划 |
| 物化视图 | Materialized View | 预计算并存储的查询结果，用于加速 |
| 数据血缘 | Data Lineage | 数据从源头到目的地的流转路径 |
| 敏感数据发现 | PII Detection | 自动识别个人身份信息等敏感字段 |
| 动态脱敏 | Dynamic Masking | 查询时实时脱敏，不改变存储数据 |
| 等保三级 | Grade 3 Protection | 中国网络安全等级保护第三级（最高级） |

### 15.2 外部依赖清单

| 依赖 | 版本 | 用途 | 许可 |
|-----|------|------|------|
| ANTLR4 | 4.13.1 | 语法解析 | BSD |
| Spring Boot | 3.2.0 | 应用框架 | Apache 2.0 |
| Apache Flink | 1.18.0 | 流处理 | Apache 2.0 |
| Neo4j | 5.15.0 | 图数据库 | GPL v3（社区版）|
| Redis | 7.2.0 | 缓存 | BSD |
| HashiCorp Vault | 1.15.0 | 密钥管理 | MPL 2.0 |
| Vue.js | 3.4.0 | 前端框架 | MIT |

### 15.3 性能基准测试数据

| 场景 | 并发 | 数据量 | 平均延迟 | P99延迟 | 吞吐量 |
|-----|------|--------|---------|---------|--------|
| 简单SELECT解析 | 100 | 1KB SQL | 15ms | 45ms | 6,000 QPS |
| 复杂JOIN优化 | 50 | 10KB SQL | 120ms | 350ms | 400 QPS |
| 批量审核（100条）| 10 | 混合SQL | 8s | 15s | 12批/分钟 |
| 血缘分析 | 20 | 1000表 | 2s | 5s | 10次/秒 |
| NL2SQL生成 | 20 | - | 800ms | 2s | 25 QPS |

---

## 文档交付完成确认

我已完整交付SQLForge项目的全部技术文档：

| 编号 | 文档名称 | 状态 | 核心内容摘要 |
|-----|---------|------|------------|
| 01 | 项目需求规格说明书 (SRS) | ✅ | 功能/非功能需求、用户场景、验收标准 |
| 02 | 总体架构设计 (TAD) | ✅ | 逻辑/技术/部署架构、技术栈选型 |
| 03 | 核心设计 (Core Design) | ✅ | 解析引擎、优化器、安全引擎、加速策略 |
| 04 | 数据模型设计 (DM) | ✅ | ER图、TDSQL-PG表结构、Neo4j图模型 |
| 05 | 非功能设计 (NFR) | ✅ | 性能架构、高可用、安全等保三级 |
| 06 | 接口设计 (API) | ✅ | OpenAPI 3.0规范、WebSocket协议 |
| 07 | 详细设计 (LLD) | ✅ | 类图、时序图、核心算法伪代码、异常处理 |
| 08 | 功能清单 (Feature List) | ✅ | 用户故事、功能点估算、迭代计划 |
| 09 | 安全设计深化 | ✅ | 威胁建模(STRIDE)、加密方案、密钥管理 |
| 10 | 系统分析与设计 (SAD) | ✅ | DDD领域模型、限界上下文、事件风暴 |
| 11 | 测试设计 (STD) | ✅ | 测试策略、自动化框架、性能测试方案 |
| 12 | 开发规范与Checklist | ✅ | 代码规范、数据库规范、上线清单 |
| 13 | 部署与运维手册 | ✅ | K8s YAML清单、监控告警、日志审计 |
| 14 | 用户操作手册 | ✅ | 各角色使用指南、最佳实践 |
| 15 | 附录 | ✅ | 术语表、依赖清单、性能基准 |

**项目总览**：
- **工作量估算**：240人天（MVP 60天 + v1.0 100天 + v1.5 50天 + v2.0 30天）
- **技术栈**：Java 21 + Spring Boot 3 + Vue 3 + K8s + TDSQL-PG + Neo4j + Flink
- **核心能力**：支持10+数据库方言、千万级SQL日处理、等保三级安全、AI增强

**建议后续行动**：
1. **架构评审**：召集核心开发人员进行LLD评审，确认技术方案可行性
2. **环境准备**：搭建K8s开发集群，部署基础依赖（TDSQL-PG、Redis、Kafka）
3. **MVP启动**：按Week 1-4计划启动开发，优先完成解析引擎核心
4. **POC验证**：选择2-3个关键方言（如Hetu、MySQL）进行端到端验证

如需对任何部分进行深化（如特定算法的详细实现、某数据库方言的完整适配规则），请告知具体方向。

