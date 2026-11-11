package com.yunshu.zhixue.modules.auth.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginVO;
import com.yunshu.zhixue.modules.auth.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端认证入口（04 §5.1）。
 *
 * <p>登录与刷新公开访问；登出幂等，令牌缺失或已过期也返回成功——
 * 前端清理本地登录态的路径不应被服务端状态卡住。
 */
@RestController
@RequestMapping("/api/admin/v1/auth")
@Tag(name = "管理端-认证", description = "后台登录、刷新令牌与登出")
public class AdminAuthController {

    private static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    @Operation(summary = "后台登录（A-A01）", description = "账号为用户名或手机号；连续失败 5 次锁定 5 分钟")
    public ApiResponse<AdminLoginVO> login(@Valid @RequestBody AdminLoginRequest request,
                                           HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminAuthService.login(request, clientIp(servletRequest)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌（A-A02）", description = "用 X-Refresh-Token 换发新的一对令牌，旧刷新令牌立即失效")
    public ApiResponse<AdminLoginVO> refresh(
            @RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String refreshToken) {
        return ApiResponse.ok(adminAuthService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出（A-A03）", description = "访问令牌进黑名单、刷新令牌作废；重复调用返回成功")
    public ApiResponse<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestHeader(value = REFRESH_TOKEN_HEADER, required = false) String refreshToken,
            HttpServletRequest servletRequest) {
        adminAuthService.logout(stripBearer(authorization), refreshToken, clientIp(servletRequest));
        return ApiResponse.ok();
    }

    private String stripBearer(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    /** 生产环境由 Nginx 反代，优先取代理头里的真实客户端 IP（operation_log.ip 审计用）。 */
    private String clientIp(HttpServletRequest request) {
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
