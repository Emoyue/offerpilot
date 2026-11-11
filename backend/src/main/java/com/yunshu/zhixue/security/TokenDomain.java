package com.yunshu.zhixue.security;

/**
 * 令牌域（D7、D12）：学员端与管理端账号体系完全独立，令牌互不通用。
 */
public enum TokenDomain {

    PORTAL("portal"),
    ADMIN("admin");

    private final String audience;

    TokenDomain(String audience) {
        this.audience = audience;
    }

    /** JWT 的 aud 声明值，用于服务端校验令牌属于哪一端。 */
    public String audience() {
        return audience;
    }

    /** 按请求路径判断令牌域：/api/admin/** 归管理端，其余归学员端。 */
    public static TokenDomain ofPath(String uri) {
        return uri != null && uri.startsWith("/api/admin") ? ADMIN : PORTAL;
    }
}
