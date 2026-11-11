package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.dto.RoleUserCount;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 后台用户 Mapper。登录时按用户名、手机号分别命中（04 Q4 定稿）。
 */
@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {

    /** 角色卡片上的「N 用户」：一次分组统计，避免逐个角色 count 造成 N+1。 */
    @Select("""
            SELECT role_id AS roleId, COUNT(*) AS userCount
              FROM admin_user
             WHERE delete_flag = 0
             GROUP BY role_id
            """)
    List<RoleUserCount> countGroupByRole();
}
