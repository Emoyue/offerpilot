package com.yunshu.zhixue.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JWT 签发与校验（D7）。
 *
 * <p>每个域独立密钥：学员端令牌拿去调管理端接口会在验签阶段失败，反之亦然。
 *
 * <p>令牌分两类，用 {@code typ} 声明区分：{@code access} 用于接口鉴权，
 * {@code refresh} 仅用于换发新令牌。两类令牌都带 {@code jti}，
 * 访问令牌的 jti 在登出后进入 Redis 黑名单（{@code token:blacklist:{jti}}），
 * 刷新令牌的 jti 则在 {@code token:refresh:{jti}} 落库，换取时一次性消费，实现轮换。
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final int MIN_SECRET_BYTES = 32;

    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey portalKey;
    private final SecretKey adminKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.portalKey = buildKey(properties.portal().secret(), TokenDomain.PORTAL);
        this.adminKey = buildKey(properties.admin().secret(), TokenDomain.ADMIN);
    }

    public String createAccessToken(TokenDomain domain, long userId, String account,
                                    Collection<String> permissions, String roleCode) {
        return build(domain, userId, account, permissions, roleCode,
                properties.of(domain).accessTtl(), TYPE_ACCESS);
    }

    /**
     * 签发刷新令牌。
     *
     * <p>返回 jti 是为了让调用方把「哪个刷新令牌还有效」写进 Redis：
     * 令牌本身无状态，必须靠服务端记录才能做到登出即时失效与轮换。
     */
    public IssuedToken createRefreshToken(TokenDomain domain, long userId) {
        String jti = UUID.randomUUID().toString();
        Duration ttl = properties.of(domain).refreshTtl();
        String token = build(domain, userId, null, List.of(), null, ttl, TYPE_REFRESH, jti);
        return new IssuedToken(token, jti, Instant.now().plus(ttl));
    }

    private String build(TokenDomain domain, long userId, String account,
                         Collection<String> permissions, String roleCode, Duration ttl, String type) {
        return build(domain, userId, account, permissions, roleCode, ttl, type, UUID.randomUUID().toString());
    }

    private String build(TokenDomain domain, long userId, String account, Collection<String> permissions,
                         String roleCode, Duration ttl, String type, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti)
                .issuer(properties.issuer())
                .subject(Long.toString(userId))
                .audience().add(domain.audience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .claim(CLAIM_TYPE, type)
                .claim("account", account)
                .claim("perms", permissions == null ? List.of() : List.copyOf(permissions))
                .claim("role", roleCode)
                .signWith(keyOf(domain))
                .compact();
    }

    /**
     * 校验并解析访问令牌。任何异常（验签失败、过期、aud 不符、把刷新令牌当访问令牌用）都返回空，
     * 由过滤器交给 401 处理。
     */
    public Optional<AuthPrincipal> parseAccessToken(TokenDomain domain, String token) {
        return parse(domain, token, TYPE_ACCESS).flatMap(claims -> {
            try {
                long userId = Long.parseLong(claims.getSubject());
                String account = claims.get("account", String.class);
                String roleCode = claims.get("role", String.class);
                return Optional.of(new AuthPrincipal(userId, account, domain, roleCode,
                        readPermissions(claims), claims.getId(), claims.getExpiration().toInstant()));
            } catch (RuntimeException ex) {
                log.debug("访问令牌声明不完整 domain={} reason={}", domain, ex.getMessage());
                return Optional.empty();
            }
        });
    }

    /** 校验并解析刷新令牌，只取身份与 jti，不信任其中的权限信息。 */
    public Optional<RefreshTokenClaims> parseRefreshToken(TokenDomain domain, String token) {
        return parse(domain, token, TYPE_REFRESH).flatMap(claims -> {
            try {
                long userId = Long.parseLong(claims.getSubject());
                return Optional.of(new RefreshTokenClaims(userId, claims.getId(),
                        claims.getExpiration().toInstant()));
            } catch (RuntimeException ex) {
                log.debug("刷新令牌声明不完整 domain={} reason={}", domain, ex.getMessage());
                return Optional.empty();
            }
        });
    }

    private Optional<Claims> parse(TokenDomain domain, String token, String expectedType) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(keyOf(domain))
                    .requireIssuer(properties.issuer())
                    .requireAudience(domain.audience())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                log.debug("令牌类型不匹配 domain={} expected={}", domain, expectedType);
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("令牌校验失败 domain={} reason={}", domain, ex.getMessage());
            return Optional.empty();
        }
    }

    public Duration accessTtl(TokenDomain domain) {
        return properties.of(domain).accessTtl();
    }

    private Set<String> readPermissions(Claims claims) {
        Object raw = claims.get("perms");
        if (raw instanceof Collection<?> collection) {
            Set<String> result = new LinkedHashSet<>();
            collection.forEach(item -> result.add(String.valueOf(item)));
            return result;
        }
        return Set.of();
    }

    private SecretKey keyOf(TokenDomain domain) {
        return domain == TokenDomain.ADMIN ? adminKey : portalKey;
    }

    private static SecretKey buildKey(String secret, TokenDomain domain) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt." + domain.audience() + ".secret 长度不足 " + MIN_SECRET_BYTES + " 字节，请检查配置");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 已签发的令牌与其服务端标识。 */
    public record IssuedToken(String token, String jti, Instant expiresAt) {
    }

    /** 刷新令牌解析结果。 */
    public record RefreshTokenClaims(long userId, String jti, Instant expiresAt) {
    }
}
