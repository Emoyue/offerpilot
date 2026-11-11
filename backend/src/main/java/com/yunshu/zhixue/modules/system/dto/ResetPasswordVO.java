package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 重置密码结果（04 A-M07）：默认密码仅在此响应中明文返回一次。
 *
 * @param password 部署期配置的默认密码（app.default-password）
 */
@Schema(description = "重置密码结果")
public record ResetPasswordVO(@Schema(description = "重置后的默认密码") String password) {
}
