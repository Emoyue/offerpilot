package com.yunshu.zhixue.security;

import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 访问令牌黑名单（04 §2.2）。
 *
 * <p>JWT 无状态，登出后令牌在自然过期前仍然可用。做法是把该令牌的 {@code jti} 写入
 * Redis，TTL 设为令牌剩余有效期——到期即自动清理，不会无限增长。
 * {@link JwtAuthenticationFilter} 每次请求先查这里，命中即视为未登录。
 */
@Component
public class TokenBlacklist {

    private static final String KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    public TokenBlacklist(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** 拉黑一个访问令牌；已过期或缺少 jti 时直接跳过。 */
    public void add(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank() || expiresAt == null) {
            return;
        }
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", ttl);
    }

    public boolean contains(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
