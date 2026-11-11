-- =============================================================================
-- OfferPilot - MySQL 初始化脚本
-- 执行时机：数据卷为空时，容器首次启动自动执行（mysql/init 下按文件名顺序）
-- 已有数据后修改本文件不会重跑；如需重跑必须清空数据卷（会丢数据）
-- =============================================================================

-- 业务库统一 utf8mb4 + utf8mb4_0900_ai_ci（与 docs/design/03.数据库设计.md 一致）
CREATE DATABASE IF NOT EXISTS `yszx`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

ALTER DATABASE `yszx`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

-- 应用账号的库权限由容器环境变量 MYSQL_USER / MYSQL_PASSWORD 自动授予，
-- 这里仅做兜底（重复授权无副作用，便于手工创建账号时复用）
-- GRANT ALL PRIVILEGES ON `yszx`.* TO 'yszx'@'%';
-- FLUSH PRIVILEGES;

USE `yszx`;

-- 建表脚本请放到本目录后续文件（如 02-schema.sql）或由应用启动时 Flyway 初始化，
-- 避免初始化脚本与迭代中的 DDL 互相覆盖。
