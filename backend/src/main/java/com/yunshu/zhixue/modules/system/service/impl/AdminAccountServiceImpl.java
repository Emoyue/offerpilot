package com.yunshu.zhixue.modules.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.modules.system.dto.AdminAccountSnapshot;
import com.yunshu.zhixue.modules.system.dto.AdminRoleSnapshot;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.PermissionMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.modules.system.service.AdminAccountService;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 后台账号查询实现：把实体裁剪成快照，避免跨模块传递持久化对象。
 */
@Service
public class AdminAccountServiceImpl implements AdminAccountService {

    private final AdminUserMapper adminUserMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    public AdminAccountServiceImpl(AdminUserMapper adminUserMapper,
                                   RoleMapper roleMapper,
                                   PermissionMapper permissionMapper) {
        this.adminUserMapper = adminUserMapper;
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public Optional<AdminAccountSnapshot> findByAccount(String account) {
        AdminUser user = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getUsername, account)
                .last("LIMIT 1"));
        if (user == null) {
            user = adminUserMapper.selectOne(Wrappers.<AdminUser>lambdaQuery()
                    .eq(AdminUser::getMobile, account)
                    .last("LIMIT 1"));
        }
        return Optional.ofNullable(user).map(this::toSnapshot);
    }

    @Override
    public Optional<AdminAccountSnapshot> findById(Long userId) {
        return Optional.ofNullable(adminUserMapper.selectById(userId)).map(this::toSnapshot);
    }

    @Override
    public Optional<AdminRoleSnapshot> findRoleById(Long roleId) {
        Role role = roleMapper.selectById(roleId);
        return Optional.ofNullable(role)
                .map(item -> new AdminRoleSnapshot(item.getId(), item.getName(), item.getCode()));
    }

    @Override
    public List<String> findPermissionCodes(Long roleId) {
        return permissionMapper.selectCodesByRoleId(roleId);
    }

    @Override
    public void markLogin(Long userId) {
        AdminUser update = new AdminUser();
        update.setId(userId);
        update.setLastLoginAt(LocalDateTime.now(ZoneOffset.UTC));
        adminUserMapper.updateById(update);
    }

    private AdminAccountSnapshot toSnapshot(AdminUser user) {
        return new AdminAccountSnapshot(user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getRoleId(), user.getStatus());
    }
}
