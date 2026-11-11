package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增角色（04 A-L02）。
 *
 * <p>需求 4.5.2 未定义角色基本信息编辑，因此只有创建，没有 PUT 基本信息接口。
 */
@Schema(description = "新增角色请求")
public record RoleCreateRequest(
        @Schema(description = "角色名称", example = "内容编辑")
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 50, message = "角色名称不能超过 50 字")
        String name,

        @Schema(description = "角色编码（小写字母开头，可含数字与下划线）", example = "content_editor")
        @NotBlank(message = "角色编码不能为空")
        @Pattern(regexp = "^[a-z][a-z0-9_]{1,49}$", message = "编码需以小写字母开头，仅含小写字母、数字与下划线（2~50 位）")
        String code,

        @Schema(description = "角色描述")
        @Size(max = 255, message = "描述不能超过 255 字")
        String description
) {
}
