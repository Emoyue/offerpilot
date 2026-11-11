package com.yunshu.zhixue.modules.auth.service.impl;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginVO;
import com.yunshu.zhixue.modules.auth.dto.AdminProfileVO;
import com.yunshu.zhixue.modules.auth.service.AdminAuthService;
import com.yunshu.zhixue.modules.system.dto.AdminAccountSnapshot;
import com.yunshu.zhixue.modules.system.dto.AdminRoleSnapshot;
import com.yunshu.zhixue.modules.system.service.AdminAccountService;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import com.yunshu.zhixue.security.AuthPrincipal;
import com.yunshu.zhixue.security.JwtTokenProvider;
import com.yunshu.zhixue.security.JwtTokenProvider.IssuedToken;
import com.yunshu.zhixue.security.JwtTokenProvider.RefreshTokenClaims;
import com.yunshu.zhixue.security.RefreshTokenStore;
import com.yunshu.zhixue.security.TokenBlacklist;
import com.yunshu.zhixue.security.TokenDomain;
import java.time.Duration;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端认证实现。
 *
 * <p>登录流程：失败锁定检查 → 账号命中（用户名优先、手机号兜底）→ BCrypt 校验 → 状态检查 →
 * 查角色与权限码 → 签发双令牌（刷新令牌登记到 Redis）→ 更新最后登录时间、写审计日志。
 *
 * <p>账号与角色数据属于 system 模块，这里只经 {@link AdminAccountService} 取快照，
 * 不直接依赖对方的 Mapper/实体（02 §3.2）。
 */
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final String LOGIN_FAIL_KEY_PREFIX = "login:fail:admin:";
    private static final String ACTION_LOGIN = "登录";
    private static final String ACTION_LOGOUT = "登出";
    private static final String TARGET_AUTH = "认证";
    private static final String TARGET_ADMIN_CONSOLE = "管理后台";

    private final AdminAccountService adminAccountService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenBlacklist tokenBlacklist;
    private final StringRedisTemplate stringRedisTemplate;
    private final OperationLogService operationLogService;

    public AdminAuthServiceImpl(AdminAccountService adminAccountService,
                                PasswordEncoder passwordEncoder,
                                JwtTokenProvider tokenProvider,
                                RefreshTokenStore refreshTokenStore,
                                TokenBlacklist tokenBlacklist,
                                StringRedisTemplate stringRedisTemplate,
                                OperationLogService operationLogService) {
        this.adminAccountService = adminAccountService;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.tokenBlacklist = tokenBlacklist;
        this.stringRedisTemplate = stringRedisTemplate;
        this.operationLogService = operationLogService;
    }

    @Override
    @Transactional
    public AdminLoginVO login(AdminLoginRequest request, String clientIp) {
        String account = request.account().trim();
        String failKey = LOGIN_FAIL_KEY_PREFIX + account;

        if (currentFailCount(failKey) >= AppConstants.LOGIN_MAX_FAIL_TIMES) {
            throw new BizException(ErrorCode.ADMIN_LOGIN_LOCKED);
        }

        AdminAccountSnapshot user = adminAccountService.findByAccount(account).orElse(null);
        if (user == null || !matches(request.password(), user.passwordHash())) {
            // 账号不存在与密码错误返回同一业务码，避免账号枚举
            registerFailure(failKey);
            throw new BizException(ErrorCode.ADMIN_LOGIN_FAILED);
        }
        if (!user.enabled()) {
            throw new BizException(ErrorCode.ADMIN_DISABLED);
        }

        stringRedisTemplate.delete(failKey);
        adminAccountService.markLogin(user.id());

        AdminLoginVO result = issueTokens(user);
        operationLogService.record(user.id(), user.username(), ACTION_LOGIN,
                TARGET_AUTH, TARGET_ADMIN_CONSOLE, clientIp);
        return result;
    }

    @Override
    public AdminLoginVO refresh(String refreshToken) {
        RefreshTokenClaims claims = tokenProvider.parseRefreshToken(TokenDomain.ADMIN, refreshToken)
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        // 一次性消费：同一刷新令牌并发或重放时只有第一次成功，其余按未登录处理
        Long userId = refreshTokenStore.consume(TokenDomain.ADMIN, claims.jti())
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));

        AdminAccountSnapshot user = adminAccountService.findById(userId)
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        if (!user.enabled()) {
            throw new BizException(ErrorCode.ADMIN_DISABLED);
        }
        return issueTokens(user);
    }

    @Override
    public void logout(String accessToken, String refreshToken, String clientIp) {
        AuthPrincipal principal = tokenProvider.parseAccessToken(TokenDomain.ADMIN, accessToken).orElse(null);
        if (principal != null) {
            tokenBlacklist.add(principal.jti(), principal.expiresAt());
        }

        RefreshTokenClaims refreshClaims =
                tokenProvider.parseRefreshToken(TokenDomain.ADMIN, refreshToken).orElse(null);
        if (refreshClaims != null) {
            refreshTokenStore.remove(TokenDomain.ADMIN, refreshClaims.jti());
        }

        // 幂等：令牌已过期或缺失时不报错，仅在有明确身份时记审计
        Long userId = null;
        String username = null;
        if (principal != null) {
            userId = principal.userId();
            username = principal.account();
        } else if (refreshClaims != null) {
            userId = refreshClaims.userId();
        }
        if (userId != null) {
            if (username == null) {
                username = resolveUsername(userId);
            }
            operationLogService.record(userId, username, ACTION_LOGOUT,
                    TARGET_AUTH, TARGET_ADMIN_CONSOLE, clientIp);
        }
    }

    /**
     * 校验密码。哈希为占位符（未初始化的种子数据）时 BCrypt 会返回 false，
     * 表现为"账号或密码错误"，不会放行也不会抛异常。
     */
    private boolean matches(String rawPassword, String passwordHash) {
        return passwordHash != null && !passwordHash.isBlank()
                && passwordEncoder.matches(rawPassword, passwordHash);
    }

    private int currentFailCount(String failKey) {
        String value = stringRedisTemplate.opsForValue().get(failKey);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            stringRedisTemplate.delete(failKey);
            return 0;
        }
    }

    /** 累加失败次数；达到阈值时抛锁定错误，锁定窗口为首次失败起 5 分钟。 */
    private void registerFailure(String failKey) {
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(failKey, Duration.ofMinutes(AppConstants.LOGIN_LOCK_MINUTES));
        }
        if (count != null && count >= AppConstants.LOGIN_MAX_FAIL_TIMES) {
            throw new BizException(ErrorCode.ADMIN_LOGIN_LOCKED);
        }
    }

    private AdminLoginVO issueTokens(AdminAccountSnapshot user) {
        AdminRoleSnapshot role = adminAccountService.findRoleById(user.roleId())
                // 角色被删会导致账号无法确定权限，属于数据完整性问题，不能静默放行
                .orElseThrow(() -> new BizException(ErrorCode.SYSTEM_ERROR, "账号角色配置异常，请联系超级管理员"));
        List<String> permissions = adminAccountService.findPermissionCodes(role.id());

        String accessToken = tokenProvider.createAccessToken(TokenDomain.ADMIN, user.id(),
                user.username(), permissions, role.code());
        IssuedToken refreshToken = tokenProvider.createRefreshToken(TokenDomain.ADMIN, user.id());
        refreshTokenStore.save(TokenDomain.ADMIN, refreshToken, user.id());

        AdminProfileVO profile = new AdminProfileVO(user.id(), user.username(),
                role.code(), role.name(), permissions);
        return new AdminLoginVO(accessToken, refreshToken.token(),
                tokenProvider.accessTtl(TokenDomain.ADMIN).toSeconds(), profile);
    }

    private String resolveUsername(Long userId) {
        return adminAccountService.findById(userId).map(AdminAccountSnapshot::username).orElse("unknown");
    }
}
