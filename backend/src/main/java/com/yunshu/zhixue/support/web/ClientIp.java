package com.yunshu.zhixue.support.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析（operation_log.ip 审计用）。
 *
 * <p>生产由 Nginx 反代，优先取代理头中的真实客户端 IP；开发直连时回落到 remoteAddr。
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
