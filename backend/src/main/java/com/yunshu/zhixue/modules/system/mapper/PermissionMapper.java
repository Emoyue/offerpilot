package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.dto.RolePermissionRow;
import com.yunshu.zhixue.modules.system.entity.Permission;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 权限 Mapper。
 *
 * <p>角色-授权是多对多，登录时用一条联表查询取全部权限码，避免逐条查询造成 N+1。
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /** 某角色被授予的全部权限码（含组节点），按展示顺序返回。 */
    @Select("""
            SELECT p.code
              FROM permission p
              JOIN role_permission rp ON rp.permission_id = p.id
             WHERE rp.role_id = #{roleId}
               AND p.delete_flag = 0
             ORDER BY p.sort_order, p.id
            """)
    List<String> selectCodesByRoleId(@Param("roleId") Long roleId);

    /**
     * 全部角色的操作点授权明细（角色卡片上展示「组名·操作」标签）。
     *
     * <p>只取 type=2 的操作点；组节点只在权限树里出现，不重复展示。
     */
    @Select("""
            SELECT rp.role_id AS role_id, p.code AS code, p.name AS name, pg.name AS group_name
              FROM role_permission rp
              JOIN permission p  ON p.id = rp.permission_id AND p.delete_flag = 0
              LEFT JOIN permission pg ON pg.id = p.parent_id
             WHERE p.type = 2
             ORDER BY pg.sort_order, p.sort_order, p.id
            """)
    List<RolePermissionRow> selectRolePermissionRows();

    /** 某角色已授予的权限主键（权限树回显勾选态用）。 */
    @Select("SELECT permission_id FROM role_permission WHERE role_id = #{roleId}")
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);
}
