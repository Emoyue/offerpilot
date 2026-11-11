package com.yunshu.zhixue.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 刷新令牌登记簿。
 *
 * <p>刷新令牌虽然自带签名，但"是否还能用"必须由服务端裁决，否则无法做到登出即失效、
 * 也无法防止同一令牌被重复使用（04 §2.2「刷新时旧 refreshToken 立即失效」）。
 * 键 {@code token:refresh:{domain}:{jti}} 的值为账号 ID，TTL 与令牌有效期一致。
 */
@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "token:refresh:";

    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void save(TokenDomain domain, JwtTokenProvider.IssuedToken token, long userId) {
        Duration ttl = Duration.between(Instant.now(), token.expiresAt());
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(key(domain, token.jti()), Long.toString(userId), ttl);
    }

    /**
     * 取出并删除刷新令牌（一次性消费）。
     *
     * <p>用 GETDEL 保证并发下同一个刷新令牌只会成功一次，避免重放换出多组令牌。
     */
    public Optional<Long> consume(TokenDomain domain, String jti) {
        if (jti == null || jti.isBlank()) {
            return Optional.empty();
        }
        String value = stringRedisTemplate.opsForValue().getAndDelete(key(domain, jti));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /** 登出时作废刷新令牌；令牌本就不存在也视为成功（幂等）。 */
    public void remove(TokenDomain domain, String jti) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        stringRedisTemplate.delete(key(domain, jti));
    }

    private String key(TokenDomain domain, String jti) {
        return KEY_PREFIX + domain.audience() + ":" + jti;
    }
}
