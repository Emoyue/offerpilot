package com.yunshu.zhixue.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 管理端登录/刷新结果（04 A-A01、A-A02）。
 *
 * @param accessToken  访问令牌，放入 {@code Authorization: Bearer <token>}
 * @param refreshToken 刷新令牌，放入 {@code X-Refresh-Token}，仅用于刷新与登出
 * @param expiresIn    访问令牌有效期（秒）
 * @param profile      管理员档案与权限码
 */
@Schema(description = "管理端登录结果")
public record AdminLoginVO(
        @Schema(description = "访问令牌") String accessToken,
        @Schema(description = "刷新令牌") String refreshToken,
        @Schema(description = "访问令牌有效期（秒）", example = "7200") long expiresIn,
        @Schema(description = "管理员档案") AdminProfileVO profile
) {
}
