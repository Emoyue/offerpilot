# 环境变量说明

`deploy/env/.env.example` 是模板，使用时复制到 `deploy/.env`（与 `docker-compose.yml` 同级）：

```bash
cd deploy
cp env/.env.example .env
```

| 变量 | 默认值 | 说明 | 修改后需 |
|---|---|---|---|
| `MYSQL_PORT` | `3307` | 宿主机访问 MySQL 的端口。默认避开本机 MySQL80 的 3306 | 重建容器 |
| `MYSQL_DATABASE` | `yszx` | 启动时自动创建的业务库名 | 重建容器 + 清卷 |
| `MYSQL_USER` | `yszx` | 应用账号，对 `MYSQL_DATABASE` 拥有全部权限 | 重建容器 |
| `MYSQL_PASSWORD` | `yszx_2024` | 应用账号密码 | 重建容器 |
| `MYSQL_ROOT_PASSWORD` | `yszx_root_2024` | root 密码，仅本地运维用 | 重建容器 |
| `REDIS_PORT` | `6379` | 宿主机访问 Redis 的端口 | 重建容器 |
| `REDIS_PASSWORD` | `yszx_redis_2024` | Redis 密码（`requirepass`） | 重建容器 |
| `REDIS_MAXMEMORY` | `512mb` | 内存上限，超出按 `allkeys-lru` 淘汰 | 重建容器 |
| `MINIO_API_PORT` | `9000` | S3 API 端口 | 重建容器 |
| `MINIO_CONSOLE_PORT` | `9001` | Web 控制台端口 | 重建容器 |
| `MINIO_ROOT_USER` | `yszx_minio` | MinIO 管理员账号（即 S3 AccessKey） | 重建容器 |
| `MINIO_ROOT_PASSWORD` | `yszx_minio_2024` | MinIO 管理员密码（即 S3 SecretKey），长度须 ≥ 8 | 重建容器 |
| `MINIO_BUCKET` | `yszx` | 初始化任务自动创建的桶名，设为公开读 | 重跑 `minio-init` |
| `MINIO_BROWSER_REDIRECT_URL` | `http://localhost:9001` | 控制台重定向地址，仅影响浏览器登录跳转 | 重建容器 |
| `MINIO_IMAGE` | `bitnamilegacy/minio:2024.12.18-debian-12-r0` | MinIO 镜像。官方 `minio/minio` 已下架（拉取返回 denied），故用 Bitnami 归档镜像：2024.12 版带内置控制台 | 重建容器 |

## 注意

1. **只影响容器创建**：`.env` 在 `docker compose up -d` 时读取，改完要重建容器；`docker compose restart` 不会重新读取。
2. **改密码不会改已有数据**：MySQL 的 root/应用密码只在**数据卷为空时**由环境变量初始化；卷已有数据后改 `MYSQL_PASSWORD` 不会修改库内账号，需进容器用 `ALTER USER` 改。
3. **`.env` 不要提交**：里面是明文密码。生产环境改由 CI/CD 变量或密钥管理注入，并把密码与 `.env.example` 分离。
4. 生产部署时同时关闭宿主机端口暴露（只保留 compose 内部网络），由 Nginx 统一入口访问。
