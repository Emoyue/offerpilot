# OfferPilot - 本地基础设施（Docker Compose）

一键拉起 **MySQL 8.0 + Redis 7 + MinIO**，与生产同构。目录约定见 `docs/design/02.整体架构设计.md` §4.6。

```
deploy/
├── docker-compose.yml      # 三个中间件 + MinIO 建桶初始化任务
├── .env                    # 运行时变量（由 env/.env.example 复制，勿提交）
├── env/
│   ├── .env.example        # 变量模板
│   └── README.md           # 变量含义说明
└── mysql/init/01-init.sql  # 首次启动自动建库（utf8mb4_0900_ai_ci）
```

## 一、快速开始

```bash
cd deploy
cp env/.env.example .env      # 首次执行；已有 .env 跳过
docker compose up -d          # 拉镜像 + 启动全部服务
docker compose ps             # 等三个服务都是 (healthy)
```

看到 `yszx-mysql`、`yszx-redis`、`yszx-minio` 均 `healthy`，且 `docker compose ps -a` 中 `yszx-minio-init` 为 `Exited (0)` 即部署完成（建桶任务执行完就退出，属正常）。

## 二、连接信息（默认值，可在 .env 修改）

| 组件 | 地址 | 账号 / 密码 | 备注 |
|---|---|---|---|
| MySQL | `127.0.0.1:3307` | `yszx` / `yszx_2024` | 库名 `yszx`，root 密码 `yszx_root_2024` |
| Redis | `127.0.0.1:6379` | 密码 `yszx_redis_2024` | AOF 已开启，无用户名 |
| MinIO API | `http://127.0.0.1:9000` | `yszx_minio` / `yszx_minio_2024` | S3 兼容端点 |
| MinIO 控制台 | `http://127.0.0.1:9001` | 同上 | 浏览器打开，可看桶与对象 |

**为什么 MySQL 是 3307**：本机已安装 Windows 服务 `MySQL80` 占用 3306。容器映射到 3307，两者可共存互不干扰。

## 三、后端接入示例（application-dev.yml）

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3307/yszx?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true&rewriteBatchedStatements=true
    username: yszx
    password: yszx_2024
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: yszx_redis_2024
      database: 0
      timeout: 3s

# MinIO / S3（走 StorageService 抽象，后续可换云 OSS）
storage:
  endpoint: http://127.0.0.1:9000
  access-key: yszx_minio
  secret-key: yszx_minio_2024
  bucket: yszx
  public-url: http://127.0.0.1:9000   # 图片对外访问前缀
```

> `yszx` 桶在初始化时已设「匿名可读」（`mc anonymous set download`），业务只需上传并返回
> `http://127.0.0.1:9000/yszx/<objectKey>` 即可直接展示；写操作仍需 AK/SK 签名。

## 四、本机已有同类服务时怎么办

### 1) 复用本机已装的 MySQL80（3306），不再起容器

```bash
docker compose up -d redis minio      # 跳过 mysql 服务
```

然后用本机 MySQL 的 root 账号执行一次建库，效果与容器版完全一致（字符集、账号权限相同）：

```sql
CREATE DATABASE IF NOT EXISTS `yszx`
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'yszx'@'%' IDENTIFIED BY 'yszx_2024';
GRANT ALL PRIVILEGES ON `yszx`.* TO 'yszx'@'%';
FLUSH PRIVILEGES;
```

命令行执行（把 `-p` 后的密码换成本机 MySQL 的 root 密码）：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS \`yszx\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER IF NOT EXISTS 'yszx'@'%' IDENTIFIED BY 'yszx_2024'; GRANT ALL PRIVILEGES ON \`yszx\`.* TO 'yszx'@'%'; FLUSH PRIVILEGES;"
```

对应后端配置只需把端口从 `3307` 改回 `3306`。**注意**：本机 MySQL80 与容器版二者只能选其一作为开发库，不要同时往两个库里写数据。

### 2) 复用已在运行的 Redis

容器以固定名 `yszx-redis` 和固定密码启动。若你已有一个可用 Redis 且不想再起一个：

```bash
docker compose up -d mysql minio       # 跳过 redis
```

后端把 `spring.data.redis.password` 改成你现有 Redis 的密码即可。业务用到的键都是
`auth:*`、`sms:*`、`interview:quota:*`、`cache:*` 前缀，不会与其它项目冲突。

### 3) 复用已在运行的 MinIO / 其它 S3

```bash
docker compose up -d mysql redis       # 跳过 minio
```

在现有 MinIO 控制台手工新建桶 `yszx` 并设置匿名读（Access Policy → Custom → 允许 `s3:GetObject`），
后端把 `storage.endpoint / access-key / secret-key` 指向现有实例即可。S3 兼容协议下也可直接换成阿里云 OSS、腾讯云 COS，只需改配置。

## 五、常用命令

```bash
docker compose ps                        # 状态
docker compose logs -f mysql             # 跟日志（redis / minio 同理）
docker compose restart redis             # 单服务重启
docker compose stop                      # 全部停止，数据保留
docker compose down                      # 删除容器与网络，数据卷保留
docker compose down -v                   # 连数据卷一起删（会丢库，慎用）

docker exec -it yszx-mysql mysql -uroot -pyszx_root_2024 -e "show databases;"
docker exec -it yszx-redis redis-cli -a yszx_redis_2024 --no-auth-warning ping
```

### 表结构（23 张表）

`mysql/init/02-schema.sql` 依据 `docs/design/03.数据库设计.md` 生成，覆盖 4.1–4.5 全部 23 张表：
统一 `id BIGINT UNSIGNED` 自增主键、`utf8mb4_0900_ai_ci`、**无任何物理外键**、业务表统一 `delete_flag`
（`operation_log` / `role_permission` / `system_setting` 按文档例外处理）、`uk_` 唯一约束与 `idx_` 索引按文档命名。

时间字段默认值用 `UTC_TIMESTAMP()` 表达式（不随会话时区漂移，符合 D17「UTC 存储」）；
`updated_at` 无法用 `ON UPDATE (UTC_TIMESTAMP())`（MySQL 只接受 `CURRENT_TIMESTAMP` 系列），
因此**由 MyBatis-Plus 自动填充写入**，手工改数据时需自行更新。

⚠️ `init/` 下的脚本**只在数据卷为空、容器首次启动时执行**。数据卷已存在时新增/修改脚本不会自动生效，需手动执行：

```bash
# 在宿主机 deploy 目录执行（脚本幂等，可重复跑）
docker exec -i yszx-mysql mysql -uroot -pyszx_root_2024 yszx < mysql/init/02-schema.sql

# 或进客户端
docker exec -it yszx-mysql mysql -uroot -pyszx_root_2024 yszx -e "show tables;"
```

种子数据见下一节；建表与种子都可重复执行（幂等）。

### 种子数据（`03-seed.sql`）

依据 `03.数据库设计.md §7` 与 `04.API接口设计.md 附录 A`，已生成并导入：

| 内容 | 数量 | 说明 |
|---|---|---|
| 学科 | 8 | 含「AI应用与智能体开发」默认第一，图标/主题色为占位值，管理端可改 |
| 角色 | 3 | `super_admin` / `content_reviewer` / `operator` |
| 权限 | 43 | 9 组（type=1）+ 34 项（type=2），ID 规则：十位=分组、个位=序号 |
| 角色授权 | 43 / 13 / 39 | 超管全部；审核=6 组+7 项；运营=8 组+31 项（除系统管理与题目审核） |
| 系统设置 | 4 | `site_name` 已填「OfferPilot」，其余三项待运营填写 |
| 内置管理员 | 1 | `admin` / 初始密码 `Offer@2026`（BCrypt cost=10） |

```bash
docker exec -i yszx-mysql mysql -uroot -pyszx_root_2024 --default-character-set=utf8mb4 yszx < mysql/init/03-seed.sql
```

⚠️ 两条上线前必须处理：
1. **改默认密码**：`Offer@2026` 仅用于本地初始化，上线前在管理端「用户管理 → 重置密码」改掉。
2. **`site_description` / `logo_url` / `default_avatar` 仍为空**，按需求 4.5.4 由运营在系统设置里填写。

角色授权策略为「组节点 + 叶子权限点」：组节点供前端权限树渲染勾选态，后端校验只看叶子码（如 `question:edit`）。

## 六、Nginx（统一入口）

`nginx/nginx.conf` + `nginx/conf.d/site.conf` 实现 02 文档 2.4 的同域三分区（D20）：

| 路径 | 去向 |
|---|---|
| `/` | 学员端静态产物 `/usr/share/nginx/portal`（SPA history 回退到 `index.html`） |
| `/admin/` | 管理端静态产物 `/usr/share/nginx/admin`（同样 history 回退） |
| `/api/` | 后端 `127.0.0.1:8080`，保留 `/api/portal/v1`、`/api/admin/v1` 原前缀 |
| `/api/portal/v1/interview/sessions/{id}/answers` | **SSE 专用 location**：`proxy_buffering off`、`gzip off`、读超时 360s（> 单题 5 分钟） |

其它已配置的点：`client_max_body_size 50m`（Excel/文档导入）、带哈希的静态资源 30 天强缓存、`index.html` 不缓存、`server_tokens off`、安全响应头、`/healthz` 探活。

本机验证语法（无需装 Nginx）：

```bash
cd deploy/nginx
docker run --rm -v "$(pwd)/nginx.conf:/etc/nginx/nginx.conf:ro" \
  -v "$(pwd)/conf.d:/etc/nginx/conf.d:ro" nginx:1.25-alpine nginx -t
```

多域名部署时把 `conf.d/site.conf` 拆成 `portal.conf` / `admin.conf`，各自写 `server_name` 即可（`upstream` 只需保留一处）。

## 七、构建与部署脚本

```bash
./scripts/build.sh                 # 后端 mvn package + 两端 pnpm build → deploy/dist/
./scripts/build.sh --only backend  # 只构建后端
DEPLOY_HOST=1.2.3.4 ./scripts/deploy.sh          # 推到远程并重启
./scripts/deploy.sh --local                      # 本机部署
./scripts/deploy.sh --dry-run                    # 只打印命令，不动文件
```

- `build.sh`：工程还没创建时会明确提示并跳过，不会报错中断；产物为 `dist/backend.jar`、`dist/portal/`、`dist/admin/`。
- `deploy.sh`：`rsync --delete` 同步静态目录 → Nginx 先 `nginx -t` 再 `reload` → 后端优先 `systemctl restart yszx-backend`，无该单元时回退 `nohup java -jar`。默认有二次确认，`--yes` 可跳过。
- 远程部署需先配免密：`ssh-copy-id user@host`。

进入容器内部排查：

```bash
docker exec -it yszx-mysql bash
docker exec -it yszx-redis sh
```

## 八、排障

| 现象 | 原因 / 处理 |
|---|---|
| `bind: address already in use` | 端口被占用：改 `.env` 里的 `MYSQL_PORT` / `REDIS_PORT` / `MINIO_API_PORT` 后 `docker compose up -d` |
| MySQL 起不来且日志提示 `lower_case_table_names` | 数据目录已按别的参数初始化过：`docker compose down -v` 后重来（会清空数据） |
| mysql 一直 unhealthy | 首次初始化约需 30–60 秒，等 `start_period` 结束；仍失败看 `docker compose logs mysql` |
| `minio-init` 反复重启 | 检查 MinIO 账号密码是否符合要求（长度 ≥ 8）；`docker compose logs minio-init` |
| 拉镜像超时 | 已配镜像加速仍失败时，可临时换源；MinIO 若拉取报 `denied`，说明依赖的 `MINIO_IMAGE` 不可用，参见下方说明 |
| 修改了 `.env` 不生效 | `.env` 只影响容器创建参数，需 `docker compose up -d` 重建（`restart` 不重新读取） |

## 九、关于 MinIO 镜像（重要）

MinIO 官方在 2025 年已下架 Docker Hub 上的 `minio/minio` 镜像，同时停止提供社区版免费二进制（`dl.min.io` 现返回 `410 Gone`）。本机实测：

```
docker pull minio/minio:latest              → denied
docker pull minio/minio:RELEASE.2024-12-18  → denied
docker pull quay.io/minio/minio:latest      → 401 UNAUTHORIZED
```

因此这里改用 **Bitnami 归档镜像** `bitnamilegacy/minio:2024.12.18-debian-12-r0`（内置 minio `RELEASE.2024-12-18` + mc + curl，符合架构文档 RELEASE.2024+ 的要求，**带内置 Web 控制台**）。

注意：Bitnami 的 `latest` 标签对应 MinIO `2025.5.24`，该版本已移除内置控制台（`9001` 会返回空响应），所以**不要**改成 `latest`。

若日后拿到官方的 `minio/minio` 镜像，改 `.env` 的 `MINIO_IMAGE` 即可，但需同步两处：

| 项 | Bitnami 镜像 | 官方镜像 |
|---|---|---|
| 数据目录（卷挂载点） | `/bitnami/minio/data` | `/data` |
| 启动方式 | 镜像自带 entrypoint，端口由 `MINIO_API_PORT_NUMBER` / `MINIO_CONSOLE_PORT_NUMBER` 控制 | 需 `command: server /data --address :9000 --console-address :9001` |
| mc 客户端 | 镜像内 `/opt/bitnami/minio-client/bin/mc` | 需单独用 `minio/mc` 镜像跑 `minio-init` |

日常用控制台管理即可：浏览器打开 <http://127.0.0.1:9001>，用 `yszx_minio` / `yszx_minio_2024` 登录，桶 `yszx` 已建好且为匿名可读。

不用控制台时，也可用容器内的 `mc` 命令行：

```bash
docker exec -it yszx-minio mc alias set local http://127.0.0.1:9000 yszx_minio yszx_minio_2024
docker exec -it yszx-minio mc ls local/yszx
```
