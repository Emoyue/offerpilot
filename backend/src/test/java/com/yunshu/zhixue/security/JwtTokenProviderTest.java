package com.yunshu.zhixue.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 令牌域隔离测试：两端令牌不可互用，是双端独立账号体系（D12）的底线。
 */
class JwtTokenProviderTest {

    private JwtTokenProvider provider() {
        JwtProperties properties = new JwtProperties(
                "offerpilot-test",
                new JwtProperties.Token("portal-test-secret-key-at-least-32-bytes-long", Duration.ofHours(2), Duration.ofDays(14)),
                new JwtProperties.Token("admin-test-secret-key-at-least-32-bytes-long", Duration.ofHours(2), Duration.ofDays(14)));
        return new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("学员令牌可解析出身份与权限")
    void portalTokenShouldBeParsed() {
        JwtTokenProvider provider = provider();
        String token = provider.createAccessToken(TokenDomain.PORTAL, 42L, "13800000000",
                List.of("subject:view"), "student");

        Optional<AuthPrincipal> principal = provider.parseAccessToken(TokenDomain.PORTAL, token);

        assertTrue(principal.isPresent());
        assertEquals(42L, principal.get().userId());
        assertEquals("13800000000", principal.get().account());
        assertEquals(TokenDomain.PORTAL, principal.get().domain());
        assertTrue(principal.get().permissions().contains("subject:view"));
    }

    @Test
    @DisplayName("学员令牌拿到管理端校验必须失败")
    void portalTokenShouldNotBeAcceptedByAdminDomain() {
        JwtTokenProvider provider = provider();
        String portalToken = provider.createAccessToken(TokenDomain.PORTAL, 1L, "13800000000",
                List.of(), "student");

        assertTrue(provider.parseAccessToken(TokenDomain.ADMIN, portalToken).isEmpty());
    }

    @Test
    @DisplayName("管理端令牌可解析出角色与权限码")
    void adminTokenShouldCarryPermissions() {
        JwtTokenProvider provider = provider();
        String adminToken = provider.createAccessToken(TokenDomain.ADMIN, 1L, "admin",
                List.of("subject:view", "subject:edit"), "super_admin");

        Optional<AuthPrincipal> principal = provider.parseAccessToken(TokenDomain.ADMIN, adminToken);

        assertTrue(principal.isPresent());
        assertTrue(principal.get().isSuperAdmin());
        assertEquals(2, principal.get().permissions().size());
    }

    @Test
    @DisplayName("签名被篡改的令牌必须解析失败")
    void tamperedTokenShouldFail() {
        JwtTokenProvider provider = provider();
        String token = provider.createAccessToken(TokenDomain.PORTAL, 1L, "13800000000", List.of(), "student");

        assertTrue(provider.parseAccessToken(TokenDomain.PORTAL, token + "x").isEmpty());
    }

    @Test
    @DisplayName("刷新令牌不能当作访问令牌使用")
    void refreshTokenShouldNotBeAcceptedAsAccessToken() {
        JwtTokenProvider provider = provider();
        String refreshToken = provider.createRefreshToken(TokenDomain.ADMIN, 1L).token();

        assertTrue(provider.parseAccessToken(TokenDomain.ADMIN, refreshToken).isEmpty());
    }

    @Test
    @DisplayName("访问令牌不能当作刷新令牌使用，且访问令牌带 jti")
    void accessTokenShouldNotBeAcceptedAsRefreshToken() {
        JwtTokenProvider provider = provider();
        String accessToken = provider.createAccessToken(TokenDomain.ADMIN, 1L, "admin", List.of(), "super_admin");

        assertTrue(provider.parseRefreshToken(TokenDomain.ADMIN, accessToken).isEmpty());
        assertTrue(provider.parseAccessToken(TokenDomain.ADMIN, accessToken).orElseThrow().jti() != null);
    }
}
