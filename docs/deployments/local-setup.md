# SQLForge Local Setup

## 前置条件

- Docker Desktop 或 Docker Engine + Docker Compose
- JDK 8
- Maven 3.8+
- Node.js 18+

## 一键启动

在仓库根目录执行：

```bash
./scripts/local-start.sh
```

Windows PowerShell：

```powershell
./scripts/local-start.ps1
```

启动脚本会完成以下动作：

1. 检查 Docker 与 Docker Compose 是否已安装
2. 检查 `3306`、`6379`、`9000` 端口是否被占用
3. 启动 MySQL、Redis、MinIO
4. 等待 MySQL 就绪
5. 自动执行 `sql/init-schema.sql` 与 `sql/init-data.sql`
6. 检查 `kafka_message_queue` 表是否存在

## 手动启动

先启动基础依赖：

```bash
docker compose up -d
```

如果本机使用旧版 Compose，也可以执行：

```bash
docker-compose up -d
```

启动后端：

```bash
cd governance-service
mvn spring-boot:run
```

启动前端：

```bash
npm run dev
```

## 停止与数据保留

默认停止服务但保留卷数据：

```bash
./scripts/local-stop.sh
```

PowerShell：

```powershell
./scripts/local-stop.ps1
```

如需同时删除卷数据：

```bash
./scripts/local-stop.sh -v
```

```powershell
./scripts/local-stop.ps1 -v
```

## 访问地址

- MySQL：`localhost:3306`（默认本地账号 `sqlforge/sqlforge`）
- Redis：`localhost:6379`
- MinIO API：`http://localhost:9000`
- MinIO Console：`http://localhost:9001`
- Governance Service：`http://localhost:8080/api/governance/health`
- Frontend：`http://localhost:3000`

## 端口占用排查

macOS / Linux：

```bash
lsof -i :3306
lsof -i :6379
lsof -i :9000
```

Windows：

```powershell
netstat -ano | findstr 3306
netstat -ano | findstr 6379
netstat -ano | findstr 9000
```

## 消息队列本地开发

本地开发使用 R-144 `DATABASE` 模式，无需启动 Kafka。消息发送、消费、重试与待处理状态统一通过 `kafka_message_queue` 表模拟。

验证命令：

```sql
SELECT * FROM kafka_message_queue;
```

管理接口：

```bash
curl -X POST http://localhost:8080/admin/messages/retry
curl http://localhost:8080/admin/messages/stats
```

如需检查待处理消息数量，也可以执行：

```sql
SELECT COUNT(*) FROM kafka_message_queue WHERE status = 'PENDING';
```

## 生产环境切换

生产环境切换到真实 Kafka 时：

1. 将 `messaging.mode` 修改为 `KAFKA`
2. 配置 `messaging.kafka.bootstrap-servers`
3. 使用 `docker compose --profile optional up -d kafka` 或生产编排启用 Kafka 服务

## 数据重置

删除容器并清理卷后重建：

```bash
./scripts/local-stop.sh -v
./scripts/local-start.sh
```

如果只想重新导入初始化数据，也可以：

```bash
docker compose exec -T mysql mysql -usqlforge -psqlforge sqlforge < sql/init-schema.sql
docker compose exec -T mysql mysql -usqlforge -psqlforge sqlforge < sql/init-data.sql
```

## 健康检查

执行统一健康检查脚本：

```bash
./scripts/health-check.sh
```

## 网络故障排查

### Docker 镜像拉取失败

- 优先使用国内镜像版启动脚本：

```bash
./scripts/local-start-cn.sh
```

- 或直接使用国内镜像 compose：

```bash
docker-compose -f docker-compose-cn.yml up -d
```

- 如果阶段 0 或阶段 1 初期开发只需要 MySQL 与 Redis，优先使用简化版：

```bash
docker-compose -f docker-compose-simple.yml up -d
```

### 离线环境部署

- 参考 [offline-setup.md](/models/project/codex/SQLForge/docs/deployments/offline-setup.md)
- 在可联网机器先执行 `docker pull` 和 `docker save`
- 在目标机器执行 `docker load -i sqlforge-images.tar`

### 孤儿容器与脏卷清理

```bash
./scripts/cleanup-orphans.sh
```

### 启动前端口检查

```bash
./scripts/check-ports.sh
```

## 常见问题

### MySQL 连接拒绝

- 确认 `docker compose ps` 中 `mysql` 为 `healthy`
- 确认本机 `3306` 端口未被其他 MySQL 占用
- 确认本地开发配置使用 `jdbc:mysql://localhost:3306/sqlforge?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true`

### 消息队列表不存在

- 重新执行 `sql/init-schema.sql`
- 确认 `docker compose exec -T mysql mysql -usqlforge -psqlforge sqlforge < sql/init-schema.sql` 执行成功
- 确认本地配置为 `messaging.mode=DATABASE`

### 前端代理失效

- 确认后端已在 `localhost:8080` 启动
- 确认 [vite.config.js](/models/project/codex/SQLForge/vite.config.js) 代理目标仍为 `http://localhost:8080`
- 如修改端口，需同步更新前端代理与健康检查脚本
