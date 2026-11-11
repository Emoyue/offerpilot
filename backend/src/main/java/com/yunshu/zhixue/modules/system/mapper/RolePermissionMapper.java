package com.yunshu.zhixue.modules.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 角色-权限关联写入（role_permission 无实体、无逻辑删除）。
 *
 * <p>授权是覆盖式：保存时先按角色删除旧行再批量插入，同一事务内完成（04 A-L05）。
 */
@Mapper
public interface RolePermissionMapper {

    @Delete("DELETE FROM role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Insert("""
            <script>
            INSERT INTO role_permission (role_id, permission_id) VALUES
            <foreach collection="permissionIds" item="permissionId" separator=",">
                (#{roleId}, #{permissionId})
            </foreach>
            </script>
            """)
    int insertBatch(@Param("roleId") Long roleId, @Param("permissionIds") List<Long> permissionIds);
}
