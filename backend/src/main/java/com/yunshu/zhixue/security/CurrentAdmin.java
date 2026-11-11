package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 当前登录管理员。
 *
 * <p>用于业务层写审计日志（operation_log 的 user_id / username 不能为空），
 * 避免每个 Controller 都手工解析令牌。仅在管理端受保护接口内调用，此时主体必然存在。
 */
@Component
public class CurrentAdmin {

    public AuthPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal) {
            return principal;
        }
        throw new BizException(ErrorCode.UNAUTHORIZED);
    }

    public Long id() {
        return principal().userId();
    }

    public String username() {
        return principal().account();
    }
}
