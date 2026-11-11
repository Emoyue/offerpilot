package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 保存角色权限（04 A-L05）：全量覆盖 role_permission。
 *
 * @param permissionIds 勾选的权限主键（含组节点）；空数组表示清空该角色权限
 */
@Schema(description = "保存角色权限请求")
public record RolePermissionUpdateRequest(
        @Schema(description = "权限 ID 集合（全量）")
        @NotNull(message = "权限列表不能为空")
        @Size(max = 100, message = "权限项超出上限")
        List<Long> permissionIds
) {
}
