package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.modules.system.dto.AdminAccountSnapshot;
import com.yunshu.zhixue.modules.system.dto.AdminRoleSnapshot;
import java.util.List;
import java.util.Optional;

/**
 * 后台账号查询能力（供 auth 模块登录/刷新使用）。
 *
 * <p>只读接口：账号写操作属于用户管理，由 {@link AdminUserService} 负责。
 */
public interface AdminAccountService {

    /** 按登录账号命中：先用户名、未命中再手机号（04 Q4）。 */
    Optional<AdminAccountSnapshot> findByAccount(String account);

    Optional<AdminAccountSnapshot> findById(Long userId);

    Optional<AdminRoleSnapshot> findRoleById(Long roleId);

    /** 某角色被授予的权限码（含组节点），按权限树顺序返回。 */
    List<String> findPermissionCodes(Long roleId);

    /** 登录成功后刷新最后登录时间（UTC），失败只影响展示，不阻断登录。 */
    void markLogin(Long userId);
}
