package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.AdminUserCreateRequest;
import com.yunshu.zhixue.modules.system.dto.AdminUserDetailVO;
import com.yunshu.zhixue.modules.system.dto.AdminUserUpdateRequest;
import com.yunshu.zhixue.modules.system.dto.AdminUserVO;
import com.yunshu.zhixue.modules.system.dto.BatchStatusRequest;
import com.yunshu.zhixue.modules.system.dto.ResetPasswordVO;
import com.yunshu.zhixue.modules.system.dto.RoleOptionVO;
import java.util.List;

/**
 * 后台用户管理（04 §5.9，A-M01~A-M08）。
 */
public interface AdminUserService {

    PageResult<AdminUserVO> listUsers(String keyword, Long roleId, Integer status, int page, int pageSize);

    Long createUser(AdminUserCreateRequest request, String clientIp);

    void updateUser(Long id, AdminUserUpdateRequest request, String clientIp);

    void updateStatus(BatchStatusRequest request, String clientIp);

    void deleteUser(Long id, String clientIp);

    void deleteUsers(List<Long> ids, String clientIp);

    ResetPasswordVO resetPassword(Long id, String clientIp);

    AdminUserDetailVO detail(Long id);

    /** 可分配角色选项（排除内置超管角色）。 */
    List<RoleOptionVO> listAssignableRoles();
}
