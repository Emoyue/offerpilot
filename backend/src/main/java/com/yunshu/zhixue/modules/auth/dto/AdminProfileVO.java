package com.yunshu.zhixue.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 登录返回的管理员档案（04 A-A01 出参）。
 *
 * <p>权限码用于前端菜单显隐与按钮级控制；后端的真正校验仍以令牌 + {@code @RequirePermission} 为准。
 *
 * @param id          管理员主键
 * @param username    登录用户名
 * @param roleCode    角色编码，超管为 super_admin
 * @param roleName    角色显示名
 * @param permissions 权限码集合（含组节点）
 */
@Schema(description = "管理员档案与权限码")
public record AdminProfileVO(
        @Schema(description = "管理员 ID", example = "1") Long id,
        @Schema(description = "用户名", example = "admin") String username,
        @Schema(description = "角色编码", example = "super_admin") String roleCode,
        @Schema(description = "角色名称", example = "超级管理员") String roleName,
        @Schema(description = "权限码集合") List<String> permissions
) {
}
