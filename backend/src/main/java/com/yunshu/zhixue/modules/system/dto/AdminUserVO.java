package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 后台用户列表项（04 A-M01）。
 *
 * @param id          主键
 * @param username    用户名
 * @param mobile      手机号
 * @param email       邮箱
 * @param roleId      角色主键
 * @param roleName    角色名称
 * @param roleCode    角色编码（前端据此标记超管）
 * @param status      1 启用 / 0 禁用
 * @param builtin     是否内置账号（不可删除、角色只读）
 * @param lastLoginAt 最后登录时间（UTC ISO-8601）
 * @param createdAt   创建时间（UTC ISO-8601）
 */
@Schema(description = "后台用户列表项")
public record AdminUserVO(
        Long id,
        String username,
        String mobile,
        String email,
        Long roleId,
        String roleName,
        String roleCode,
        Integer status,
        boolean builtin,
        String lastLoginAt,
        String createdAt
) {
}
