package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 操作日志（04 A-G01/A-G02）。
 *
 * @param id         主键
 * @param userId     操作人主键
 * @param username   操作人用户名快照
 * @param action     操作类型中文枚举（登录/登出/新增/修改/删除/禁用启用/导出/其他）
 * @param targetType 操作对象类型
 * @param targetName 操作对象描述
 * @param ip         客户端 IP
 * @param createdAt  操作时间（UTC ISO-8601）
 */
@Schema(description = "操作日志")
public record OperationLogVO(
        Long id,
        Long userId,
        String username,
        String action,
        String targetType,
        String targetName,
        String ip,
        String createdAt
) {
}
