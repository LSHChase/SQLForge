# SQLForge Offline Setup

## 适用场景

- 目标机器无法访问 Docker Hub
- 目标机器网络不稳定，镜像拉取经常失败
- 需要在内网或隔离环境完成本地开发部署

## 预下载镜像命令

在可联网机器执行：

```bash
docker pull mysql:8.0
docker pull redis:7
docker pull bitnami/kafka:3.6.2
docker pull minio/minio:latest
docker save -o sqlforge-images.tar mysql:8.0 redis:7 bitnami/kafka:3.6.2 minio/minio:latest
```

如需国内镜像源，也可以在联网机器先拉取镜像源版本，再重新打上标准标签：

```bash
docker pull docker.mirrors.sjtug.sjtu.edu.cn/library/mysql:8.0
docker pull docker.mirrors.sjtug.sjtu.edu.cn/library/redis:7
docker pull docker.mirrors.sjtug.sjtu.edu.cn/bitnami/kafka:3.6.2
docker pull docker.mirrors.sjtug.sjtu.edu.cn/minio/minio:latest

docker tag docker.mirrors.sjtug.sjtu.edu.cn/library/mysql:8.0 mysql:8.0
docker tag docker.mirrors.sjtug.sjtu.edu.cn/library/redis:7 redis:7
docker tag docker.mirrors.sjtug.sjtu.edu.cn/bitnami/kafka:3.6.2 bitnami/kafka:3.6.2
docker tag docker.mirrors.sjtug.sjtu.edu.cn/minio/minio:latest minio/minio:latest
```

## 离线加载命令

在目标机器执行：

```bash
docker load -i sqlforge-images.tar
```

加载完成后可用以下命令确认镜像已存在：

```bash
docker images | grep -E 'mysql|redis|bitnami/kafka|minio/minio'
```

## 使用本地镜像启动

默认 [docker-compose.yml](/models/project/codex/SQLForge/docker-compose.yml) 使用标准镜像标签；只要离线加载后的标签保持一致，`docker compose up -d` 就会直接使用本地镜像而不需要重新拉取。

```bash
docker compose up -d
```

如果你的离线镜像标签和默认配置不一致，请修改 `docker-compose.yml` 中的 `image` 字段为本地已有标签，再执行：

```bash
docker compose up -d
```

## 简化版离线启动

如果当前只需要阶段 0 或阶段 1 初期开发，推荐优先使用简化版：

```bash
docker compose -f docker-compose-simple.yml up -d
```

该方案只启动 MySQL 和 Redis，可绕过 Kafka 与 MinIO 的镜像问题。

如果当前环境仍停留在旧版 Compose，也可以把上面的 `docker compose` 等价替换为 `docker-compose`。
