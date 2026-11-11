package com.yunshu.zhixue.security;

import java.time.Instant;
import java.util.Set;

/**
 * 已认证主体（放入 SecurityContext）。
 *
 * @param userId      账号主键
 * @param account     登录账号（学员手机号 / 管理员用户名）
 * @param domain      令牌域
 * @param roleCode    角色编码，超管为 super_admin
 * @param permissions 权限码集合，来自登录时按角色查询的结果
 * @param jti         令牌唯一标识，登出时写入 Redis 黑名单（04 §2.2）
 * @param expiresAt   令牌过期时间，用于计算黑名单键的 TTL
 */
public record AuthPrincipal(
        long userId,
        String account,
        TokenDomain domain,
        String roleCode,
        Set<String> permissions,
        String jti,
        Instant expiresAt
) {
    public boolean isSuperAdmin() {
        return "super_admin".equalsIgnoreCase(roleCode);
    }
}
