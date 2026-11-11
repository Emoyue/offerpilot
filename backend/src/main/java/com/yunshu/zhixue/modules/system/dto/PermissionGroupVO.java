package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 权限树分组（04 A-L04）：9 组，每组下挂 34 项操作点。
 *
 * @param id          组节点主键
 * @param name        组名
 * @param code        组编码（如 question）
 * @param permissions 组内操作点
 */
@Schema(description = "权限树分组")
public record PermissionGroupVO(Long id, String name, String code, List<PermissionNodeVO> permissions) {
}
