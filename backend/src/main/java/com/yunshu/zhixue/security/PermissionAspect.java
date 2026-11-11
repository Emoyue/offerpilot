package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * {@link RequirePermission} 的切面实现：按权限码校验当前管理员。
 *
 * <p>权限码在登录时按角色查库并写入令牌，避免每次请求查库。若后续需要"改权限即时生效"，
 * 改为查 Redis 缓存的权限集合即可，调用方无需改动。
 */
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requirePermission)")
    public void checkPermission(RequirePermission requirePermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.domain() != TokenDomain.ADMIN) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        // 超级管理员拥有全部权限
        if (principal.isSuperAdmin()) {
            return;
        }
        String required = requirePermission.value();
        boolean granted = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(code -> "*".equals(code) || required.equals(code));
        if (!granted) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }

    /** 角色管理接口：不设权限码，仅超管（D4-5）。 */
    @Before("@annotation(com.yunshu.zhixue.security.RequireSuperAdmin)")
    public void checkSuperAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (principal.domain() != TokenDomain.ADMIN || !principal.isSuperAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
    }
}
