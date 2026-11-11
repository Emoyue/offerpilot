package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 角色卡片（04 A-L01）。
 *
 * @param id               角色主键
 * @param name             角色名称
 * @param code             角色编码
 * @param description      描述
 * @param userCount        该角色下的用户数（删除前置校验用）
 * @param permissions      操作点权限码
 * @param permissionLabels 权限标签「组名·操作」，供卡片直接展示
 * @param builtin          是否内置超管角色（不可删除、不可改权限）
 */
@Schema(description = "角色卡片")
public record RoleVO(
        Long id,
        String name,
        String code,
        String description,
        long userCount,
        List<String> permissions,
        List<String> permissionLabels,
        boolean builtin
) {
}
