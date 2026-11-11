package com.yunshu.zhixue.modules.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.system.dto.PermissionGroupVO;
import com.yunshu.zhixue.modules.system.dto.PermissionNodeVO;
import com.yunshu.zhixue.modules.system.dto.RoleCreateRequest;
import com.yunshu.zhixue.modules.system.dto.RolePermissionRow;
import com.yunshu.zhixue.modules.system.dto.RoleUserCount;
import com.yunshu.zhixue.modules.system.dto.RoleVO;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.Permission;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.PermissionMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.modules.system.mapper.RolePermissionMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import com.yunshu.zhixue.modules.system.service.RoleService;
import com.yunshu.zhixue.security.CurrentAdmin;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色管理实现。
 *
 * <p>角色卡片需要的用户数与权限标签各用一次批量查询取回，避免按角色逐条统计。
 * 角色权限保存为覆盖式（先删后插，单事务），超管角色不允许改权限（A-L05）。
 */
@Service
public class RoleServiceImpl implements RoleService {

    private static final String TARGET_ROLE = "角色";
    private static final String ACTION_ADD = "新增";
    private static final String ACTION_UPDATE = "修改";
    private static final String ACTION_DELETE = "删除";

    private final RoleMapper roleMapper;
    private final AdminUserMapper adminUserMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final OperationLogService operationLogService;
    private final CurrentAdmin currentAdmin;

    public RoleServiceImpl(RoleMapper roleMapper,
                           AdminUserMapper adminUserMapper,
                           PermissionMapper permissionMapper,
                           RolePermissionMapper rolePermissionMapper,
                           OperationLogService operationLogService,
                           CurrentAdmin currentAdmin) {
        this.roleMapper = roleMapper;
        this.adminUserMapper = adminUserMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.operationLogService = operationLogService;
        this.currentAdmin = currentAdmin;
    }

    @Override
    public List<RoleVO> listRoles() {
        List<Role> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery().orderByAsc(Role::getId));
        if (roles.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> userCounts = adminUserMapper.countGroupByRole().stream()
                .collect(Collectors.toMap(RoleUserCount::getRoleId, RoleUserCount::getUserCount, (a, b) -> a));
        Map<Long, List<RolePermissionRow>> permissionRows = permissionMapper.selectRolePermissionRows().stream()
                .collect(Collectors.groupingBy(RolePermissionRow::getRoleId, LinkedHashMap::new, Collectors.toList()));

        return roles.stream().map(role -> {
            List<RolePermissionRow> rows = permissionRows.getOrDefault(role.getId(), List.of());
            List<String> codes = rows.stream().map(RolePermissionRow::getCode).toList();
            List<String> labels = rows.stream()
                    .map(row -> (row.getGroupName() == null ? "" : row.getGroupName() + "·") + row.getName())
                    .toList();
            return new RoleVO(role.getId(), role.getName(), role.getCode(), role.getDescription(),
                    userCounts.getOrDefault(role.getId(), 0L), codes, labels,
                    AppConstants.ROLE_SUPER_ADMIN.equals(role.getCode()));
        }).toList();
    }

    @Override
    @Transactional
    public Long createRole(RoleCreateRequest request, String clientIp) {
        String code = request.code().trim();
        long exists = roleMapper.selectCount(Wrappers.<Role>lambdaQuery().eq(Role::getCode, code));
        if (exists > 0) {
            throw new BizException(ErrorCode.NAME_EXISTS);
        }

        Role role = new Role();
        role.setName(request.name().trim());
        role.setCode(code);
        role.setDescription(request.description() == null ? "" : request.description().trim());
        roleMapper.insert(role);

        operationLogService.record(currentAdmin.id(), currentAdmin.username(), ACTION_ADD, TARGET_ROLE,
                role.getName() + "（" + role.getCode() + "）", clientIp);
        return role.getId();
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId, String clientIp) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (AppConstants.ROLE_SUPER_ADMIN.equals(role.getCode())) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED);
        }
        Long userCount = adminUserMapper.selectCount(Wrappers.<AdminUser>lambdaQuery()
                .eq(AdminUser::getRoleId, roleId));
        if (userCount != null && userCount > 0) {
            throw new BizException(ErrorCode.ROLE_HAS_USERS);
        }

        roleMapper.deleteById(roleId);
        operationLogService.record(currentAdmin.id(), currentAdmin.username(), ACTION_DELETE, TARGET_ROLE, role.getName(), clientIp);
    }

    @Override
    public List<PermissionGroupVO> permissionTree(Long roleId) {
        List<Permission> all = permissionMapper.selectList(Wrappers.<Permission>lambdaQuery()
                .orderByAsc(Permission::getSortOrder)
                .orderByAsc(Permission::getId));
        Set<Long> checkedIds = roleId == null ? Set.of()
                : new HashSet<>(permissionMapper.selectPermissionIdsByRoleId(roleId));

        Map<Long, List<Permission>> childrenByParent = all.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(Permission::getParentId, LinkedHashMap::new, Collectors.toList()));

        List<PermissionGroupVO> groups = new ArrayList<>();
        for (Permission group : all) {
            if (group.getParentId() != null) {
                continue;
            }
            List<PermissionNodeVO> nodes = childrenByParent.getOrDefault(group.getId(), List.of()).stream()
                    .map(child -> new PermissionNodeVO(child.getId(), child.getName(), child.getCode(),
                            checkedIds.contains(child.getId())))
                    .toList();
            groups.add(new PermissionGroupVO(group.getId(), group.getName(), group.getCode(), nodes));
        }
        return groups;
    }

    @Override
    @Transactional
    public void savePermissions(Long roleId, List<Long> permissionIds, String clientIp) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (AppConstants.ROLE_SUPER_ADMIN.equals(role.getCode())) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED);
        }

        List<Long> distinctIds = permissionIds == null ? List.of()
                : permissionIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (!distinctIds.isEmpty()) {
            long validCount = permissionMapper.selectCount(Wrappers.<Permission>lambdaQuery()
                    .in(Permission::getId, distinctIds));
            if (validCount != distinctIds.size()) {
                throw new BizException(ErrorCode.PARAM_INVALID, "存在无效的权限项，请刷新后重试");
            }
        }

        try {
            rolePermissionMapper.deleteByRoleId(roleId);
            if (!distinctIds.isEmpty()) {
                rolePermissionMapper.insertBatch(roleId, distinctIds);
            }
        } catch (Exception ex) {
            // 覆盖式授权要么全成功要么保持原状，失败统一 30008，具体原因只进服务端日志
            throw new BizException(ErrorCode.PERMISSION_SAVE_FAILED);
        }

        operationLogService.record(currentAdmin.id(), currentAdmin.username(), ACTION_UPDATE, TARGET_ROLE,
                role.getName() + "·权限授权（" + distinctIds.size() + " 项）", clientIp);
    }
}
