package com.yunshu.zhixue.modules.auth.service;

import com.yunshu.zhixue.modules.auth.dto.AdminLoginRequest;
import com.yunshu.zhixue.modules.auth.dto.AdminLoginVO;

/**
 * 管理端认证（04 §5.1 A-A01 ~ A-A03）。
 *
 * <p>令牌域固定为 {@code admin}：学员端令牌无法通过管理端接口验签（D12）。
 */
public interface AdminAuthService {

    /** 账号密码登录；连续失败 5 次锁定 5 分钟（D18）。 */
    AdminLoginVO login(AdminLoginRequest request, String clientIp);

    /** 用刷新令牌换发新的一对令牌，旧刷新令牌立即失效（轮换）。 */
    AdminLoginVO refresh(String refreshToken);

    /** 登出：访问令牌进黑名单、刷新令牌作废；重复调用幂等。 */
    void logout(String accessToken, String refreshToken, String clientIp);
}
