package com.yunshu.zhixue.modules.system.dto;

import com.yunshu.zhixue.common.constant.AppConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 编辑后台用户（04 A-M03）。
 *
 * <p>{@code password} 为空表示不修改；内置账号的角色只读（30004）。
 */
@Schema(description = "编辑后台用户请求")
public record AdminUserUpdateRequest(
        @Schema(description = "用户名（不限字符，仅校验长度与唯一性）")
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 50, message = "用户名长度需为 2~50 位")
        String username,

        @Schema(description = "手机号（留空表示不设置）")
        @Pattern(regexp = AppConstants.MOBILE_PATTERN, message = "手机号格式不正确")
        String mobile,

        @Schema(description = "邮箱")
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱长度不能超过 100 位")
        String email,

        @Schema(description = "角色 ID")
        @NotNull(message = "请选择角色")
        Long roleId,

        @Schema(description = "新密码，留空则不修改")
        @Size(min = AppConstants.PASSWORD_MIN_LENGTH, max = AppConstants.PASSWORD_MAX_LENGTH,
                message = "密码长度需为 6~20 位")
        String password,

        @Schema(description = "状态：1 启用 / 0 禁用")
        @Min(value = 0, message = "状态值不合法")
        @Max(value = 1, message = "状态值不合法")
        Integer status
) {
}
