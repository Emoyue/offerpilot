-- =============================================================================
-- OfferPilot - 种子数据
-- 依据：docs/design/03.数据库设计.md §7、docs/design/04.API接口设计.md 附录 A（9 组 34 项）
--
-- 执行方式（需在建表之后）：
--   docker exec -i yszx-mysql mysql -uroot -p<root密码> --default-character-set=utf8mb4 yszx < 03-seed.sql
--
-- 幂等性：全部使用 INSERT IGNORE + 显式主键，重复执行不会报错、不会覆盖人工修改过的数据；
--         如需重置某条种子，先 DELETE 对应行再重跑本脚本。
--
-- 权限 ID 规则（与附录 A 一致）：十位 = 所属分组，个位 = 组内序号
--   1 题库管理 / 2 面试题库 / 3 用户管理 / 4 学习路线 / 5 学员管理
--   6 系统管理 / 7 学科管理 / 8 课程库   / 9 数据看板
--
-- 角色授权策略：
--   超级管理员 = 全部 43 行（9 组 + 34 项）
--   其余角色   = 组节点 + 叶子权限点：组节点用于前端权限树渲染勾选态，校验只看叶子码
-- =============================================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

USE `yszx`;

-- ----------------------------------------------------------------- 学科（8 个）
-- 图标与主题色为占位值，上线前由运营在管理端调整（03 §9 item 4）
INSERT IGNORE INTO `subject` (`id`, `name`, `icon`, `description`, `theme_color`, `sort_order`) VALUES
(1, 'AI应用与智能体开发', '🤖', '大模型应用落地与智能体（Agent）编排，覆盖提示词工程、RAG、工具调用与多智能体协作', '#6366F1', 1),
(2, 'AI大模型开发', '🧠', '模型原理、微调与推理优化，涵盖 Transformer、LoRA、量化部署与分布式训练', '#8B5CF6', 2),
(3, 'AI具身智能机器人开发', '🦾', '感知—决策—执行闭环，机器人运动规划、仿真环境与具身智能算法', '#0EA5E9', 3),
(4, 'AI嵌入式_机器人开发', '🔧', '边缘侧算力与嵌入式部署，C/C++、ROS、芯片适配与实时控制', '#14B8A6', 4),
(5, 'AI测试', '🧪', 'AI 系统的质量保障，模型评测、数据校验、自动化测试与回归策略', '#F59E0B', 5),
(6, 'AI运维', '⚙️', '模型与推理服务的稳定性保障，监控告警、容量规划、成本与故障处理', '#10B981', 6),
(7, 'AI视觉设计', '🎨', 'AIGC 视觉创作与设计工作流，含分镜、风格控制与素材生产', '#EC4899', 7),
(8, 'AI视频与短剧创作', '🎬', 'AI 视频生成与短剧内容生产，覆盖脚本、分镜、配音与成片剪辑', '#EF4444', 8);

-- ------------------------------------------------------------------- 角色（3 个）
INSERT IGNORE INTO `role` (`id`, `name`, `code`, `description`) VALUES
(1, '超级管理员', 'super_admin', '内置唯一，拥有全部权限，不可删除、不可改角色'),
(2, '内容审核', 'content_reviewer', '只把关不生产：查看全部内容并审核题目，无增删改权限'),
(3, '运营人员', 'operator', '除系统管理外的全部操作，含内容维护与学员管理，不具备审核权限');

-- ------------------------------------------------------- 权限分组（9 组，type=1）
INSERT IGNORE INTO `permission` (`id`, `parent_id`, `name`, `code`, `type`, `sort_order`) VALUES
(1, NULL, '题库管理',   'question',      1, 1),
(2, NULL, '面试题库',   'interviewbank', 1, 2),
(3, NULL, '用户管理',   'user',          1, 3),
(4, NULL, '学习路线',   'route',         1, 4),
(5, NULL, '学员管理',   'student',       1, 5),
(6, NULL, '系统管理',   'system',        1, 6),
(7, NULL, '学科管理',   'subject',       1, 7),
(8, NULL, '课程库',     'course',        1, 8),
(9, NULL, '数据看板',   'dashboard',     1, 9);

-- --------------------------------------------------- 权限操作点（34 项，type=2）
INSERT IGNORE INTO `permission` (`id`, `parent_id`, `name`, `code`, `type`, `sort_order`) VALUES
-- 题库管理（7 项）
(11, 1, '查看',     'question:view',        2, 1),
(12, 1, '新增',     'question:add',         2, 2),
(13, 1, '编辑',     'question:edit',        2, 3),
(14, 1, '删除',     'question:delete',      2, 4),
(15, 1, '导入',     'question:import',      2, 5),
(16, 1, '导出',     'question:export',      2, 6),
(17, 1, '审核',     'question:audit',       2, 7),
-- 面试题库（5 项）
(21, 2, '查看',     'interviewbank:view',       2, 1),
(22, 2, '上传',     'interviewbank:upload',     2, 2),
(23, 2, '编辑',     'interviewbank:edit',       2, 3),
(24, 2, '删除',     'interviewbank:delete',     2, 4),
(25, 2, '检索测试', 'interviewbank:search_test',2, 5),
-- 用户管理（4 项）
(31, 3, '查看',     'user:view',     2, 1),
(32, 3, '新增',     'user:add',      2, 2),
(33, 3, '编辑',     'user:edit',     2, 3),
(34, 3, '启用禁用', 'user:disable',  2, 4),
-- 学习路线（4 项）
(41, 4, '查看', 'route:view',   2, 1),
(42, 4, '新增', 'route:add',    2, 2),
(43, 4, '编辑', 'route:edit',   2, 3),
(44, 4, '删除', 'route:delete', 2, 4),
-- 学员管理（3 项）
(51, 5, '查看', 'student:view',   2, 1),
(52, 5, '编辑', 'student:edit',   2, 2),
(53, 5, '删除', 'student:delete', 2, 3),
-- 系统管理（2 项）
(61, 6, '日志查看', 'system:log_view', 2, 1),
(62, 6, '系统设置', 'system:setting',  2, 2),
-- 学科管理（4 项，阶段随学科权限点）
(71, 7, '查看', 'subject:view',   2, 1),
(72, 7, '新增', 'subject:add',    2, 2),
(73, 7, '编辑', 'subject:edit',   2, 3),
(74, 7, '删除', 'subject:delete', 2, 4),
-- 课程库（4 项）
(75, 8, '查看', 'course:view',   2, 1),
(76, 8, '新增', 'course:add',    2, 2),
(77, 8, '编辑', 'course:edit',   2, 3),
(78, 8, '删除', 'course:delete', 2, 4),
-- 数据看板（1 项）
(79, 9, '查看', 'dashboard:view', 2, 1);

-- --------------------------------------------------- 角色授权：超级管理员 = 全部
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`)
SELECT 1, `id` FROM `permission`;

-- --------------------------------------- 角色授权：内容审核（查看内容 + 审核题目）
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`) VALUES
(2, 1), (2, 2), (2, 4), (2, 7), (2, 8), (2, 9),  -- 组：题库/面试题库/学习路线/学科/课程库/看板
(2, 11),  -- question:view
(2, 17),  -- question:audit
(2, 21),  -- interviewbank:view
(2, 41),  -- route:view
(2, 71),  -- subject:view
(2, 75),  -- course:view
(2, 79);  -- dashboard:view

-- --------------------------- 角色授权：运营人员（除系统管理与题目审核外全部）
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`) VALUES
(3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (3, 7), (3, 8), (3, 9),  -- 组：除系统管理(6)
(3, 11), (3, 12), (3, 13), (3, 14), (3, 15), (3, 16),
(3, 21), (3, 22), (3, 23), (3, 24), (3, 25),
(3, 31), (3, 32), (3, 33), (3, 34),
(3, 41), (3, 42), (3, 43), (3, 44),
(3, 51), (3, 52), (3, 53),
(3, 71), (3, 72), (3, 73), (3, 74),
(3, 75), (3, 76), (3, 77), (3, 78),
(3, 79);

-- ------------------------------------------------------- 系统设置（4 项，值待填）
INSERT IGNORE INTO `system_setting` (`id`, `config_key`, `config_value`, `remark`) VALUES
(1, 'site_name',        'OfferPilot', '站点名称（首页与管理端标题）'),
(2, 'site_description', '',         '站点描述（SEO 与首页副标题）'),
(3, 'logo_url',         '',         '平台 Logo（MinIO 对象 URL，留空用默认图标）'),
(4, 'default_avatar',   '',         '学员默认头像（MinIO 对象 URL，留空用默认图）');

-- --------------------------------------------- 内置超级管理员（默认密码见下）
-- 默认账号 admin / 密码 Offer@2026（BCrypt cost=10，已本地校验通过）
-- 上线前必须改密：管理端「用户管理 → 重置密码」，或直接改成自己的哈希后执行 UPDATE
INSERT IGNORE INTO `admin_user` (`id`, `username`, `password_hash`, `mobile`, `email`, `role_id`, `status`, `is_builtin`) VALUES
(1, 'admin', '$2b$10$4Gfn8fBC71oNR8jKUGftIOyannKtMd6mbecbUOd7LXjWGvA7.L6Re', NULL, NULL, 1, 1, 1);

-- 早期版本写入过占位哈希，INSERT IGNORE 不会覆盖已有行，这里就地纠正为可用哈希。
-- 只在仍是占位值时才更新，不会覆盖人工改过的密码。
UPDATE `admin_user`
   SET `password_hash` = '$2b$10$4Gfn8fBC71oNR8jKUGftIOyannKtMd6mbecbUOd7LXjWGvA7.L6Re',
       `updated_at`    = UTC_TIMESTAMP()
 WHERE `username` = 'admin'
   AND `password_hash` LIKE '%PLACEHOLDER%';
