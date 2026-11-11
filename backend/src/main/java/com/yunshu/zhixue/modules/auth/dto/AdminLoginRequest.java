package com.yunshu.zhixue.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理端登录入参（04 A-A01）。
 *
 * @param account  用户名或手机号：先按 username 命中，未命中再按 mobile（Q4 定稿）
 * @param password 明文密码，服务端与 BCrypt 哈希比对
 */
@Schema(description = "管理端登录入参")
public record AdminLoginRequest(
        @Schema(description = "用户名或手机号", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "请输入用户名或手机号")
        @Size(max = 50, message = "账号长度不能超过 50 个字符")
        String account,

        @Schema(description = "登录密码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "请输入密码")
        @Size(min = 6, max = 20, message = "密码长度需为 6~20 位")
        String password
) {
}
