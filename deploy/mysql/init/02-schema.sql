-- =============================================================================
-- OfferPilot - 表结构（23 张）
-- 依据：docs/design/03.数据库设计.md §4
-- 规范：docs/rules/database-rules.md
--
-- 执行方式：
--   容器内：docker exec -i yszx-mysql mysql -uroot -p<密码> yszx < 02-schema.sql
--   或客户端：mysql> use yszx; source 02-schema.sql;
--
-- 关键约定：
--   1. 无任何物理外键，关联字段 *_id 由应用层校验（database-rules）
--   2. 时间统一 UTC 存储（D17）：默认值用 UTC_TIMESTAMP() 表达式，
--      不随连接会话时区漂移；展示层再转 Asia/Shanghai
--   3. updated_at 无法使用 ON UPDATE (UTC_TIMESTAMP())（MySQL 仅接受 CURRENT_TIMESTAMP 系列，
--      而 CURRENT_TIMESTAMP 取会话时区，会写入 +08 时间），故由 MyBatis-Plus 自动填充写入 UTC；
--      手工改数据时请自行更新 updated_at
--   4. 业务表统一 delete_flag 逻辑删除；operation_log、role_permission、system_setting 为例外
--   5. 幂等：全部 CREATE TABLE IF NOT EXISTS，可重复执行不报错
-- =============================================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

USE `yszx`;

-- ---------------------------------------------------------------- 4.2 内容管理

CREATE TABLE IF NOT EXISTS `subject` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(50) NOT NULL COMMENT '学科名称',
  `icon` VARCHAR(50) NOT NULL COMMENT '图标（Emoji）',
  `description` VARCHAR(255) NOT NULL COMMENT '描述',
  `theme_color` VARCHAR(20) NOT NULL DEFAULT '#000000' COMMENT '主题色',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序（越小越靠前）',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_name` (`name`),
  KEY `idx_subject_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学科表';

CREATE TABLE IF NOT EXISTS `stage` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id` BIGINT UNSIGNED NOT NULL COMMENT '学科ID -> subject.id',
  `name` VARCHAR(50) NOT NULL COMMENT '阶段名称',
  `icon` VARCHAR(50) NULL COMMENT '图标（留空用默认）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stage_subject_name` (`subject_id`, `name`),
  KEY `idx_stage_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='阶段表';

CREATE TABLE IF NOT EXISTS `course` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '课程名称',
  `cover_url` VARCHAR(500) NULL COMMENT '封面URL（外链）',
  `duration_minutes` INT NOT NULL DEFAULT 0 COMMENT '时长（分钟）',
  `source` TINYINT NOT NULL DEFAULT 1 COMMENT '1 B站 / 2 其他',
  `link` VARCHAR(500) NULL COMMENT '课程链接',
  `bvid` VARCHAR(20) NULL COMMENT 'BV号（非B站来源为空）',
  `subject_id` BIGINT UNSIGNED NULL COMMENT '主学科ID，NULL=其他学科',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_bvid` (`bvid`),
  KEY `idx_course_subject` (`subject_id`),
  KEY `idx_course_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='B站课程库表';

CREATE TABLE IF NOT EXISTS `course_subject` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID -> course.id',
  `subject_id` BIGINT UNSIGNED NOT NULL COMMENT '学科ID -> subject.id（额外学科）',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_subject` (`course_id`, `subject_id`),
  KEY `idx_course_subject_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程-学科多学科关联表';

CREATE TABLE IF NOT EXISTS `question` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stage_id` BIGINT UNSIGNED NOT NULL COMMENT '阶段ID -> stage.id',
  `type` TINYINT NOT NULL COMMENT '1 单选 / 2 多选 / 3 判断 / 4 简答',
  `difficulty` TINYINT NOT NULL DEFAULT 2 COMMENT '1 简单 / 2 中等 / 3 困难',
  `content` TEXT NOT NULL COMMENT '题干',
  `answer` TEXT NULL COMMENT '判断题为正确/错误文本；简答为参考答案；选择题为NULL',
  `analysis` TEXT NULL COMMENT '解析',
  `tags` VARCHAR(255) NULL COMMENT '标签，逗号分隔（仅展示）',
  `status` TINYINT NOT NULL DEFAULT 3 COMMENT '1 已上架 / 2 已下架 / 3 待审核 / 4 草稿 / 5 已驳回',
  `reject_reason` VARCHAR(255) NULL COMMENT '驳回原因',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_question_stage` (`stage_id`),
  KEY `idx_question_status` (`status`),
  KEY `idx_question_type_difficulty` (`type`, `difficulty`),
  KEY `idx_question_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题库题目表';

CREATE TABLE IF NOT EXISTS `question_option` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `question_id` BIGINT UNSIGNED NOT NULL COMMENT '题目ID -> question.id',
  `option_key` VARCHAR(4) NOT NULL COMMENT '选项标识 A/B/C/D',
  `option_text` VARCHAR(500) NOT NULL COMMENT '选项内容',
  `is_correct` TINYINT NOT NULL DEFAULT 0 COMMENT '1 正确 / 0 否',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_option` (`question_id`, `option_key`),
  KEY `idx_question_option_qid` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目选项表（判断/简答无选项行）';

CREATE TABLE IF NOT EXISTS `interview_topic` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id` BIGINT UNSIGNED NULL COMMENT '学科ID，NULL=全局主题',
  `name` VARCHAR(50) NOT NULL COMMENT '主题名称',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_interview_topic_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题主题词表';

CREATE TABLE IF NOT EXISTS `interview_question` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id` BIGINT UNSIGNED NOT NULL COMMENT '方向ID -> subject.id',
  `content` TEXT NOT NULL COMMENT '题干',
  `reference_answer` TEXT NULL COMMENT '参考答案',
  `topic` VARCHAR(50) NULL COMMENT '主题，NULL=未分类',
  `difficulty` TINYINT NOT NULL DEFAULT 2 COMMENT '1 简单 / 2 中等 / 3 困难',
  `tags` VARCHAR(255) NULL COMMENT '标签，逗号分隔',
  `source_type` TINYINT NOT NULL DEFAULT 1 COMMENT '1 文档解析 / 2 手工录入 / 3 手动导入',
  `source_doc_name` VARCHAR(255) NULL COMMENT '来源文档名（文件不入库）',
  `source_seq` INT NULL COMMENT '文档内第N题',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 已上架 / 2 已下架',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_interview_question_subject_status` (`subject_id`, `status`),
  KEY `idx_interview_question_topic` (`topic`),
  KEY `idx_interview_question_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题库表';

CREATE TABLE IF NOT EXISTS `learning_route` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT '路线名称',
  `subject_id` BIGINT UNSIGNED NOT NULL COMMENT '关联学科ID（单学科）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 禁用',
  `icon` VARCHAR(50) NULL COMMENT '路线图标',
  `target_education` VARCHAR(100) NULL COMMENT '适合学历',
  `description` TEXT NULL COMMENT '路线描述',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_learning_route_subject` (`subject_id`),
  KEY `idx_learning_route_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习路线表';

CREATE TABLE IF NOT EXISTS `route_course` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `route_id` BIGINT UNSIGNED NOT NULL COMMENT '路线ID -> learning_route.id',
  `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID -> course.id',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '课程顺序（即学习顺序）',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_route_course` (`route_id`, `course_id`),
  KEY `idx_route_course_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='路线-课程关联表';

CREATE TABLE IF NOT EXISTS `route_course_completion` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` BIGINT UNSIGNED NOT NULL COMMENT '学员ID -> student.id',
  `route_course_id` BIGINT UNSIGNED NOT NULL COMMENT '路线课程ID -> route_course.id',
  `completed_at` DATETIME NOT NULL COMMENT '完成时间（UTC）',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_route_course_completion` (`student_id`, `route_course_id`),
  KEY `idx_route_course_completion_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员课程完成表';

-- ---------------------------------------------------------------- 4.1 学员账号

CREATE TABLE IF NOT EXISTS `student` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `mobile` VARCHAR(11) NOT NULL COMMENT '手机号（11位），登录账号',
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
  `nickname` VARCHAR(50) NOT NULL COMMENT '用户名',
  `avatar` VARCHAR(500) NULL COMMENT '头像URL（MinIO）',
  `email` VARCHAR(100) NULL COMMENT '邮箱（可空，非空值唯一）',
  `gender` TINYINT NOT NULL DEFAULT 0 COMMENT '0 保密 / 1 男 / 2 女',
  `preferred_subject_id` BIGINT UNSIGNED NULL COMMENT '首选技术方向 -> subject.id',
  `guide_finished` TINYINT NOT NULL DEFAULT 0 COMMENT '新手引导是否看过 1 是 0 否',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 已启用 / 0 已禁用',
  `last_login_at` DATETIME NULL COMMENT '最后登录时间（UTC）',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  -- 逻辑删除与唯一约束的共存（02 §R4）：只约束 delete_flag=0 的行，注销后手机号/邮箱可重新注册
  UNIQUE KEY `uk_student_mobile` ((IF(`delete_flag` = 0, `mobile`, NULL))),
  UNIQUE KEY `uk_student_email` ((IF(`delete_flag` = 0, `email`, NULL))),
  KEY `idx_student_created_at` (`created_at`),
  KEY `idx_student_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员表';

-- ---------------------------------------------------------------- 4.3 学习训练

CREATE TABLE IF NOT EXISTS `practice_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` BIGINT UNSIGNED NOT NULL COMMENT '学员ID -> student.id',
  `question_id` BIGINT UNSIGNED NOT NULL COMMENT '题目ID -> question.id',
  `mode` TINYINT NOT NULL DEFAULT 1 COMMENT '1 顺序 / 2 随机',
  `is_correct` TINYINT NOT NULL DEFAULT 0 COMMENT '1 对 / 0 错',
  `duration_seconds` INT NULL COMMENT '该题用时（秒）',
  `practiced_at` DATETIME NOT NULL COMMENT '作答时间（UTC）',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_practice_record_student_time` (`student_id`, `practiced_at`),
  KEY `idx_practice_record_student_question` (`student_id`, `question_id`),
  KEY `idx_practice_record_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='刷题作答明细表';

CREATE TABLE IF NOT EXISTS `wrong_question` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` BIGINT UNSIGNED NOT NULL COMMENT '学员ID -> student.id',
  `question_id` BIGINT UNSIGNED NOT NULL COMMENT '题目ID -> question.id',
  `error_count` INT NOT NULL DEFAULT 1 COMMENT '错误次数（≥3 高频 / =2 多次 / <2 偶尔）',
  `corrected_count` INT NOT NULL DEFAULT 0 COMMENT '已纠正次数（连续答对+1，达3置已掌握）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 待复习 / 2 已掌握',
  `last_wrong_at` DATETIME NULL COMMENT '最近答错时间（UTC）',
  `last_answer` TEXT NULL COMMENT '最近一次错误作答快照',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wrong_question` (`student_id`, `question_id`),
  KEY `idx_wrong_question_student_status` (`student_id`, `status`),
  KEY `idx_wrong_question_last_wrong` (`last_wrong_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='错题本表（每人每题一行）';

-- ---------------------------------------------------------------- 4.4 AI 面试

CREATE TABLE IF NOT EXISTS `interview_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id` BIGINT UNSIGNED NOT NULL COMMENT '学员ID -> student.id',
  `subject_id` BIGINT UNSIGNED NOT NULL COMMENT '面试方向 -> subject.id',
  `difficulty` TINYINT NOT NULL COMMENT '1 简单 / 2 中等 / 3 困难',
  `question_count` INT NOT NULL COMMENT '题数（3/5/8/10/15）',
  `overall_score` DECIMAL(3,1) NULL COMMENT '综合评分 0.0-10.0',
  `total_duration` INT NULL COMMENT '累计时长（秒）',
  `started_at` DATETIME NOT NULL COMMENT '开始时间（UTC）',
  `completed_at` DATETIME NOT NULL COMMENT '完成时间（UTC）',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_interview_record_student_time` (`student_id`, `created_at`),
  KEY `idx_interview_record_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试记录表（仅已完成会话落库）';

CREATE TABLE IF NOT EXISTS `interview_question_score` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `interview_record_id` BIGINT UNSIGNED NOT NULL COMMENT '面试记录ID -> interview_record.id',
  `question_order` INT NOT NULL COMMENT '第N题（1起）',
  `question_id` BIGINT UNSIGNED NULL COMMENT '面试题ID，NULL=快照题',
  `question_text` TEXT NOT NULL COMMENT '题干快照',
  `student_answer` TEXT NULL COMMENT '学员作答',
  `score` DECIMAL(3,1) NOT NULL COMMENT '本题得分 0.0-10.0',
  `ai_feedback` TEXT NULL COMMENT 'AI 评分理由/反馈',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_interview_score_record` (`interview_record_id`, `question_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试逐题评分表';

CREATE TABLE IF NOT EXISTS `interview_report` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `interview_record_id` BIGINT UNSIGNED NOT NULL COMMENT '面试记录ID -> interview_record.id',
  `radar_json` TEXT NULL COMMENT '6维能力分析（JSON）',
  `strengths` TEXT NULL COMMENT '优点',
  `weaknesses` TEXT NULL COMMENT '不足',
  `suggestions` TEXT NULL COMMENT '改进建议',
  `improvement_cards_json` TEXT NULL COMMENT '3张能力提升卡（JSON）',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_interview_report_record` (`interview_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试报告表（与记录 1:1）';

-- ---------------------------------------------------------------- 4.5 系统管理

CREATE TABLE IF NOT EXISTS `role` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(255) NULL COMMENT '描述',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `permission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` BIGINT UNSIGNED NULL COMMENT '父节点ID，NULL=组节点',
  `name` VARCHAR(50) NOT NULL COMMENT '组名/权限点名',
  `code` VARCHAR(100) NOT NULL COMMENT '权限编码，如 question:view',
  `type` TINYINT NOT NULL COMMENT '1 组（菜单级）/ 2 操作点',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`code`),
  KEY `idx_permission_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表（权限树，两层）';

CREATE TABLE IF NOT EXISTS `role_permission` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID -> role.id',
  `permission_id` BIGINT UNSIGNED NOT NULL COMMENT '权限ID -> permission.id',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_role_permission_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-权限关联表（无逻辑删除，覆盖式授权）';

CREATE TABLE IF NOT EXISTS `admin_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录账号）',
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
  `mobile` VARCHAR(11) NULL COMMENT '手机号',
  `email` VARCHAR(100) NULL COMMENT '邮箱',
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID -> role.id（单角色）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 已启用 / 0 已禁用',
  `is_builtin` TINYINT NOT NULL DEFAULT 0 COMMENT '1 内置（超级管理员，不可删除/改角色）',
  `last_login_at` DATETIME NULL COMMENT '最后登录时间（UTC）',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  `delete_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 未删 1 已删',
  PRIMARY KEY (`id`),
  -- 逻辑删除与唯一约束的共存（02 §R4）：函数索引只约束 delete_flag=0 的行，
  -- 已删除账号不再占用用户名/手机号，可重新创建同名账号，且可累积多条历史删除记录。
  UNIQUE KEY `uk_admin_user_username` ((IF(`delete_flag` = 0, `username`, NULL))),
  UNIQUE KEY `uk_admin_user_mobile` ((IF(`delete_flag` = 0, `mobile`, NULL))),
  KEY `idx_admin_user_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台用户表';

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '操作人ID -> admin_user.id',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名快照',
  `action` VARCHAR(50) NOT NULL COMMENT '登录/登出/新增/修改/删除/禁用启用/导出/其他',
  `target_type` VARCHAR(50) NOT NULL COMMENT '操作对象类型',
  `target_name` VARCHAR(255) NOT NULL COMMENT '操作对象描述',
  `ip` VARCHAR(64) NOT NULL COMMENT 'IP 地址',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '操作时间（UTC）',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_user` (`user_id`),
  KEY `idx_operation_log_time` (`created_at`),
  KEY `idx_operation_log_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表（审计，无逻辑删除）';

CREATE TABLE IF NOT EXISTS `system_setting` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` VARCHAR(50) NOT NULL COMMENT '配置键',
  `config_value` TEXT NULL COMMENT '配置值（文本/JSON）',
  `remark` VARCHAR(255) NULL COMMENT '说明',
  `created_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '创建时间（UTC）',
  `updated_at` DATETIME NOT NULL DEFAULT (UTC_TIMESTAMP()) COMMENT '更新时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_setting_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统设置表（key-value）';
