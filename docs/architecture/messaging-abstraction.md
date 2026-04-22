# 消息中间件抽象架构（R-144）

本文件定义 SQLForge 在 R-144 下的消息能力抽象基线，确保本地开发不依赖 Kafka，同时保持生产环境可无缝切换到真实 Kafka。

## 设计目标：本地开发不依赖Kafka，生产环境无缝切换

1. 本地开发默认不部署 Kafka，消息链路通过数据库模拟完成验证。
2. 测试环境按场景区分 `MOCK` 与 `DATABASE`，避免单元测试和集成测试混用消息实现。
3. 生产环境统一切换为真实 Kafka，业务代码不修改调用方式。

## 三种模式：DATABASE（本地）/ KAFKA（生产）/ MOCK（测试）

- `DATABASE`
  - 用途：本地开发、联调、可补偿异步任务。
  - 存储：`kafka_message_queue` 表。
  - 行为：Producer 写库，Consumer 轮询 `PENDING` 消息并执行业务监听器。
- `KAFKA`
  - 用途：生产环境与真实消息集群。
  - 存储：真实 Kafka Topic。
  - 行为：Producer 使用 Kafka 客户端发送，Consumer 使用 `@KafkaListener` 或等价监听机制消费。
- `MOCK`
  - 用途：单元测试。
  - 存储：内存队列。
  - 行为：使用 `ConcurrentLinkedQueue` 模拟 Topic，不依赖外部中间件。

## 接口设计：MessageProducer / MessageConsumer

- 抽象接口目录：`domain/messaging/`
- `MessageProducer`
  - `send(topic, key, message, headers)`
- `MessageConsumer`
  - `poll(topic, batchSize)`
  - `listen(topic, handler)`

业务代码必须只依赖上述抽象接口，禁止直接依赖 `KafkaTemplate`、Kafka 客户端或任意数据库轮询实现。

当前 `governance` 已实现：

- `domain/messaging/` 抽象接口与 `MessageEnvelope`
- `infrastructure/messaging/` 下的 `Database` / `Mock` / `Kafka` 三种模式实现
- `MessagingConfig` 按 `messaging.mode` 选择 Bean
- `DatabaseMessagePollingJob` 轮询治理消息 Topic
- `MessageAdminController` 提供 `retry` / `stats` 管理接口，仅在 `DATABASE` 模式下可用
- 当前 `KAFKA` 模式已接入真实 Kafka 客户端基线：
  - `KafkaMessageProducer` 使用 Kafka 客户端发送消息
  - `KafkaMessageConsumer` 使用 Kafka 客户端启动后台监听循环
  - `MessagingConfig` 现已校验 `bootstrap-servers`、`security-protocol` 与 SASL/SSL 安全参数边界
  - `scripts/verify_kafka_runtime_config.py` 提供独立 bootstrap/security 参数门禁
  - `scripts/run-kafka-runtime-gate.sh` 提供真实 Kafka 连通性、成功投递、消费日志与失败回退队列验证
  - `.github/workflows/kafka-runtime-gate.yml` 提供独立 GitHub Actions 入口，保留真实 Kafka runtime evidence

## 实现映射

- `DatabaseMessageProducer` -> `kafka_message_queue` 表
- `DatabaseMessageConsumer` -> 轮询 `kafka_message_queue` 中 `PENDING` 消息
- `KafkaMessageProducer` -> Kafka 客户端 Producer
- `KafkaMessageConsumer` -> Kafka 客户端 Consumer 后台监听循环
- `MockMessageProducer` -> `ConcurrentLinkedQueue`
- `MockMessageConsumer` -> `ConcurrentLinkedQueue`

### `kafka_message_queue` 建议结构

```sql
CREATE TABLE kafka_message_queue (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  topic VARCHAR(128) NOT NULL COMMENT '模拟 Kafka Topic',
  partition_key VARCHAR(128) DEFAULT NULL COMMENT '分区键',
  message_body TEXT NOT NULL COMMENT 'JSON 格式消息体',
  headers TEXT DEFAULT NULL COMMENT 'JSON 格式消息头',
  status ENUM('PENDING','SENT','CONSUMED','FAILED') NOT NULL DEFAULT 'PENDING' COMMENT '消息状态',
  retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  consumed_at TIMESTAMP NULL DEFAULT NULL COMMENT '消费时间',
  error_log TEXT DEFAULT NULL COMMENT '错误日志',
  PRIMARY KEY (id),
  KEY idx_topic_status (topic, status),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='R-144数据库模拟模式消息队列';
```

## 配置切换：messaging.mode + MessagingConfig

```yaml
messaging:
  mode: DATABASE
  kafka:
    enabled: false
  database:
    enabled: true
    poll-interval: 5000
    max-retry: 3
```

- `application-dev.yml`：`messaging.mode=DATABASE`
- `src/main/resources/application-test.yml`：`messaging.mode=DATABASE`
- `src/test/resources/application-test.yml`：`messaging.mode=MOCK`
- `application-prod.yml`：`messaging.mode=KAFKA`

当前代码通过 `MessagingConfig` 按 `messaging.mode` 显式选择具体 Producer / Consumer Bean。

## 本地验证：查询表 + 调用重试接口

1. 调用业务接口发送消息。
2. 查询消息表：

```sql
SELECT * FROM kafka_message_queue;
```

3. 检查待处理消息数量：

```sql
SELECT COUNT(*) FROM kafka_message_queue WHERE status = 'PENDING';
```

4. 如需手动重试失败消息，调用：

```bash
./scripts/manual-message-queue-smoke.sh --cleanup
```

说明：

- `/api/governance/admin/messages/retry`
- `/api/governance/admin/messages/stats`

以上接口都属于受保护接口，必须带齐请求上下文头；本地人工验证优先使用仓库内脚本，避免手工遗漏请求头。

## 生产部署：docker-compose启用Kafka服务 + 切换mode为KAFKA

1. 在生产或生产仿真编排中启用 Kafka 服务。
2. 若使用仓库内编排参考，执行：

```bash
docker compose --profile optional up -d kafka
```

3. 将应用配置切换为：
   - `messaging.mode=KAFKA`
   - `messaging.kafka.enabled=true`
   - `messaging.kafka.bootstrap-servers=<cluster>`
   - `messaging.kafka.security-protocol=<PLAINTEXT|SSL|SASL_PLAINTEXT|SASL_SSL>`
   - 若使用 `SASL_*`，必须补齐 `messaging.kafka.sasl-mechanism` 与 `messaging.kafka.sasl-jaas-config`
   - 若使用 `*SSL`，必须补齐 `messaging.kafka.ssl-truststore-location` 与 `messaging.kafka.ssl-truststore-password`
4. 保持业务层接口不变，仅替换基础设施实现。
5. 执行真实 Kafka 验证：

```bash
python3 scripts/verify_kafka_runtime_config.py
bash scripts/run-kafka-runtime-gate.sh
```

6. 当前真实 Kafka gate 已覆盖：
   - Topic bootstrap/connectivity
   - `KAFKA` 模式下 `schedule/extensions` 状态
   - `audit/write` 成功投递与消费日志
   - Kafka 故障后的 fallback queue 恢复路径

## 维护说明

- Topic 名称、消息体 JSON 结构、Headers、分区键策略和消费顺序要求，必须同步维护到对应接口契约文档。
- 本文件负责定义全局抽象模式，不承载具体业务 Topic 细节。
- 当前仓库已完成 `DATABASE` / `MOCK` 可运行基线，以及 `KAFKA` 代码接入、真实集群 runtime gate 与安全参数校验入口。
- 相关规则：`R-066`, `R-068`, `R-121`, `R-128`, `R-144`
