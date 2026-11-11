package com.yunshu.zhixue.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置（app.jwt）。两个域各有独立密钥与有效期，令牌不可跨端使用。
 *
 * @param issuer 签发者
 * @param portal 学员端令牌配置
 * @param admin  管理端令牌配置
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String issuer, Token portal, Token admin) {

    /**
     * @param secret     签名密钥（HS256，至少 32 字节；生产必须走环境变量）
     * @param accessTtl  访问令牌有效期，默认 2h
     * @param refreshTtl 刷新令牌有效期，默认 14d
     */
    public record Token(String secret, Duration accessTtl, Duration refreshTtl) {
    }

    public Token of(TokenDomain domain) {
        return domain == TokenDomain.ADMIN ? admin : portal;
    }
}
