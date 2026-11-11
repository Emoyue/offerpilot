package com.yunshu.zhixue.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 解析 Authorization: Bearer 令牌并建立安全上下文。
 *
 * <p>令牌域由路径决定：/api/admin/** 用管理端密钥校验，其余用学员端密钥（D12）。
 * 解析失败或令牌已登出（命中 Redis 黑名单）不直接返回错误，
 * 交给 SecurityConfig 的 EntryPoint 统一输出 401 + 10001。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklist tokenBlacklist;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, TokenBlacklist tokenBlacklist) {
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            TokenDomain domain = TokenDomain.ofPath(request.getRequestURI());
            tokenProvider.parseAccessToken(domain, token)
                    .filter(principal -> !tokenBlacklist.contains(principal.jti()))
                    .ifPresent(principal -> {
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        principal.permissions().forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));
                        if (principal.roleCode() != null && !principal.roleCode().isBlank()) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + principal.roleCode().toUpperCase()));
                        }
                        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        chain.doFilter(request, response);
    }
}
