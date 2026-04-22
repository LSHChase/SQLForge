# SQLForge Huawei Cloud Setup

## 适用场景

- 目标环境为华为云私有云或等效私有化环境
- 需要按目标形态部署 4 个微服务、MySQL、Redis、Kafka、OBS/MRS 与可观测组件
- 需要在本地 `DATABASE` 模式之外切换到生产 `KAFKA` 模式

## 部署目标

- 最终服务边界固定为 4 个微服务：
  - 查询执行服务
  - SQL 优化服务
  - 压测引擎服务
  - 公共管理服务
- 前端与后端独立构建、独立部署
- 管理面与压测面保持独立资源边界

## 目标拓扑

```text
华为云私有云（单区域）
├── 接入层
│   └── Spring Cloud Gateway（双实例负载均衡）
├── 计算层（CCE / Kubernetes）
│   ├── 查询执行服务（3副本）
│   ├── SQL优化服务（2副本，CPU密集）
│   ├── 压测引擎服务（1副本，独立资源池）
│   └── 公共管理服务（2副本）
├── 数据层
│   ├── MySQL/TDSQL（主从）
│   ├── Redis 7.x 集群
│   ├── Kafka 3.x 集群
│   └── OBS（归档与备份）
├── 大数据层（MRS）
│   ├── Hetu
│   ├── Hive
│   ├── Spark
│   └── Hudi（COW）
└── 可观测层
    ├── ELK
    ├── Prometheus + Grafana
    └── SkyWalking
```

## 前置条件

- 华为云私有云网络、子网、安全组和负载均衡已准备
- CCE 或等效 Kubernetes 集群已可用
- MRS 集群已开通并具备 Hetu/Hive/Spark/Hudi 运行条件
- MySQL/TDSQL、Redis、Kafka、OBS 已完成环境准备
- 已准备统一镜像仓库、制品仓库和 CI/CD 凭据

## 基础设施建议

### MySQL / TDSQL

- 主从或等效高可用
- 打开备份、binlog 和恢复演练机制
- 审计日志、历史索引和消息补偿表纳入重点恢复清单
- 恢复目标、责任分工和演练记录模板以 `backup-recovery-baseline.md` 为准

### Redis

- 建议 3 主 3 从
- 仅承载缓存与短期状态，不承载历史唯一事实

### Kafka

- 生产环境启用真实 Kafka 集群
- 审计相关 Topic 与业务 Topic 分离
- 推荐至少 3 节点、3 副本

### OBS

- 用于冷数据归档、审计归档、备份文件和离线导出
- 对象存储必须启用加密和生命周期管理

## 应用配置切换

生产环境统一使用 `KAFKA` 模式，不沿用本地数据库消息模拟：

```yaml
messaging:
  mode: KAFKA
  kafka:
    enabled: true
    bootstrap-servers: kafka-cluster:9092
  database:
    enabled: false
```

同时要求：

- `application-prod.yml` 打开真实鉴权与权限控制
- 敏感配置走环境变量、密文配置中心或独立密钥服务
- 生产环境不得保留本地调试绕过开关

## 服务部署顺序

1. 部署 MySQL/TDSQL、Redis、Kafka、OBS、MRS 与可观测组件
2. 执行数据库初始化脚本与必要增量脚本
3. 部署公共管理服务
4. 部署查询执行服务与 SQL 优化服务
5. 部署压测引擎服务到独立资源池
6. 部署前端和接入层
7. 执行健康检查、鉴权检查、租户隔离检查和审计链路检查

## 数据初始化

- 执行 `sql/init-schema.sql`
- 执行 `sql/init-data.sql`
- 若存在已发布环境，必须补充增量脚本而不是直接覆盖

## 合规与安全要求

- 所有服务必须经过后端身份鉴别
- 所有请求必须显式带租户上下文
- 所有 SQL 操作、登录登出、权限变更进入审计日志
- 数据源密码、Token、密钥不得明文落库或写入日志
- MySQL 备份恢复目标满足 `RPO < 1小时`、`RTO < 4小时`

## 验证清单

- 应用配置：
  - `application-prod.yml` 存在且非空
  - `messaging.mode=KAFKA`
  - 生产鉴权开关已开启
- 可观测：
  - 4 个后端服务的 `/actuator/prometheus` 已纳入采集
  - `logs / metrics / alerts` 基线已按 `observability-baseline.md` 接入
- 备份恢复：
  - 备份对象、`RPO/RTO`、责任人和演练模板已按 `backup-recovery-baseline.md` 接入
- 基础设施：
  - Kafka、MySQL、Redis、OBS/MRS 连通
  - 端口和服务发现配置正确
- 服务能力：
  - 健康检查通过
  - 鉴权与租户隔离通过
  - 审计日志双写链路通过
  - 前端能访问后端网关而非本地代理

## 故障排查

### Kafka 未连通

- 检查 `messaging.kafka.bootstrap-servers`
- 检查 Kafka 安全组和集群地址
- 检查 Topic 是否已创建且副本健康

### Hetu / MRS 不可用

- 检查 VPC 路由和安全组
- 检查 Hetu 对外暴露模式与 SQLForge 配置一致
- 检查查询执行服务使用的接入模式是否正确

### 鉴权或租户隔离失败

- 检查生产鉴权开关是否开启
- 检查租户、角色、数据源授权配置是否已初始化
- 检查审计日志中是否记录拒绝原因

## 关联文档

- [local-setup.md](/models/project/codex/SQLForge/docs/deployments/local-setup.md)
- [offline-setup.md](/models/project/codex/SQLForge/docs/deployments/offline-setup.md)
- [observability-baseline.md](/models/project/codex/SQLForge/docs/deployments/observability-baseline.md)
- [backup-recovery-baseline.md](/models/project/codex/SQLForge/docs/deployments/backup-recovery-baseline.md)
- [messaging-abstraction.md](/models/project/codex/SQLForge/docs/architecture/messaging-abstraction.md)
- [access-control-spec.md](/models/project/codex/SQLForge/docs/security/access-control-spec.md)
