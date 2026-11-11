package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 用户表单里的角色选项。
 *
 * <p>04 文档未定义该接口：角色卡片接口（A-L01）仅超管可访问，而用户新增/编辑（A-M02/M03）
 * 需要角色下拉，故补一个最小只读选项接口，排除不可分配的超管角色。
 */
@Schema(description = "可分配角色选项")
public record RoleOptionVO(Long id, String name, String code) {
}
