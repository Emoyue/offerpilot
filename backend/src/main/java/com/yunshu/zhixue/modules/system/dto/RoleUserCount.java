package com.yunshu.zhixue.modules.system.dto;

/**
 * 角色下的用户数统计行（GROUP BY role_id）。
 */
public class RoleUserCount {

    private Long roleId;
    private Long userCount;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getUserCount() {
        return userCount;
    }

    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }
}
