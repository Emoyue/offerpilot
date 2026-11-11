package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 权限树操作点（04 A-L04）。
 *
 * @param id      权限主键
 * @param name    操作名（查看/新增…）
 * @param code    权限码
 * @param checked 该角色是否已授予（未传 roleId 时恒为 false）
 */
@Schema(description = "权限树操作点")
public record PermissionNodeVO(Long id, String name, String code, boolean checked) {
}
