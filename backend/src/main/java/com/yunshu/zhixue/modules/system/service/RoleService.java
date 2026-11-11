package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.modules.system.dto.PermissionGroupVO;
import com.yunshu.zhixue.modules.system.dto.RoleCreateRequest;
import com.yunshu.zhixue.modules.system.dto.RoleVO;
import java.util.List;

/**
 * 角色与权限树（04 §5.10，仅超管）。
 */
public interface RoleService {

    List<RoleVO> listRoles();

    Long createRole(RoleCreateRequest request, String clientIp);

    void deleteRole(Long roleId, String clientIp);

    /** 权限树；传 roleId 时带上该角色的勾选状态（A-L04）。 */
    List<PermissionGroupVO> permissionTree(Long roleId);

    /** 全量覆盖角色权限（A-L05）。 */
    void savePermissions(Long roleId, List<Long> permissionIds, String clientIp);
}
