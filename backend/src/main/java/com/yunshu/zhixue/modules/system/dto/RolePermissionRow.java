package com.yunshu.zhixue.modules.system.dto;

/**
 * 角色-权限明细行（角色卡片上的权限标签用）。
 *
 * <p>只包含操作点（type=2），组节点由前端权限树单独渲染。
 */
public class RolePermissionRow {

    private Long roleId;
    private String code;
    private String name;
    private String groupName;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
