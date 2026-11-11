package com.yunshu.zhixue.modules.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.AdminUserCreateRequest;
import com.yunshu.zhixue.modules.system.dto.AdminUserDetailVO;
import com.yunshu.zhixue.modules.system.dto.AdminUserUpdateRequest;
import com.yunshu.zhixue.modules.system.dto.AdminUserVO;
import com.yunshu.zhixue.modules.system.dto.BatchStatusRequest;
import com.yunshu.zhixue.modules.system.dto.OperationLogVO;
import com.yunshu.zhixue.modules.system.dto.ResetPasswordVO;
import com.yunshu.zhixue.modules.system.dto.RoleOptionVO;
import com.yunshu.zhixue.modules.system.entity.AdminUser;
import com.yunshu.zhixue.modules.system.entity.OperationLog;
import com.yunshu.zhixue.modules.system.entity.Role;
import com.yunshu.zhixue.modules.system.mapper.AdminUserMapper;
import com.yunshu.zhixue.modules.system.mapper.OperationLogMapper;
import com.yunshu.zhixue.modules.system.mapper.RoleMapper;
import com.yunshu.zhixue.modules.system.service.AdminUserService;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import com.yunshu.zhixue.security.CurrentAdmin;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 后台用户管理实现。
 *
 * <p>内置账号（is_builtin=1）受保护：角色只读、不可禁用、不可删除（30004），
 * 避免误操作把唯一的超级管理员锁在系统外。当前登录账号同样不可禁用/删除。
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final String TARGET_USER = "后台用户";
    private static final String ACTION_ADD = "新增";
    private static final String ACTION_UPDATE = "修改";
    private static final String ACTION_DELETE = "删除";
    private static final String ACTION_STATUS = "禁用启用";
    private static final DateTimeFormatter ISO_UTC = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final AdminUserMapper adminUserMapper;
    private final RoleMapper roleMapper;
    private final OperationLogMapper operationLogMapper;
    private final OperationLogService operationLogService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentAdmin currentAdmin;
    private final String defaultPassword;

    public AdminUserServiceImpl(AdminUserMapper adminUserMapper,
                                RoleMapper roleMapper,
                                OperationLogMapper operationLogMapper,
                                OperationLogService operationLogService,
                                PasswordEncoder passwordEncoder,
                                CurrentAdmin currentAdmin,
                                @Value("${app.default-password}") String defaultPassword) {
        this.adminUserMapper = adminUserMapper;
        this.roleMapper = roleMapper;
        this.operationLogMapper = operationLogMapper;
        this.operationLogService = operationLogService;
        this.passwordEncoder = passwordEncoder;
        this.currentAdmin = currentAdmin;
        this.defaultPassword = defaultPassword;
    }

    @Override
    public PageResult<AdminUserVO> listUsers(String keyword, Long roleId, Integer status,
                                             int page, int pageSize) {
        int safePage = Math.max(page, AppConstants.PAGE_DEFAULT);
        int safeSize = Math.min(Math.max(pageSize, 1), AppConstants.PAGE_SIZE_MAX);

        String trimmed = keyword == null ? "" : keyword.trim();
        var wrapper = Wrappers.<AdminUser>lambdaQuery()
                .and(!trimmed.isEmpty(), w -> w.like(AdminUser::getUsername, trimmed)
                        .or()
                        .like(AdminUser::getMobile, trimmed))
                .eq(roleId != null, AdminUser::getRoleId, roleId)
                .eq(status != null, AdminUser::getStatus, status)
                .orderByAsc(AdminUser::getId);

        IPage<AdminUser> result = adminUserMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        Map<Long, Role> roles = loadRoles(result.getRecords());
        List<AdminUserVO> list = result.getRecords().stream()
                .map(user -> toVO(user, roles.get(user.getRoleId())))
                .toList();
        return PageResult.of(list, result.getTotal(), safePage, safeSize);
    }

    @Override
    @Transactional
    public Long createUser(AdminUserCreateRequest request, String clientIp) {
        Role role = resolveRole(request.roleId(), null);
        String username = normalizeUsername(request.username());
        String mobile = normalizeMobile(request.mobile());

        ensureUsernameAvailable(username, null);
        ensureMobileAvailable(mobile, null);

        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setMobile(mobile);
        user.setEmail(normalize(request.email()));
        user.setRoleId(role.getId());
        user.setStatus(request.status() == null ? AppConstants.ACCOUNT_ENABLED : request.status());
        user.setIsBuiltin(0);
        adminUserMapper.insert(user);

        operationLogService.record(currentAdmin.id(), currentAdmin.username(), ACTION_ADD,
                TARGET_USER, username + "（" + role.getName() + "）", clientIp);
        return user.getId();
    }

    @Override
    @Transactional
    public void updateUser(Long id, AdminUserUpdateRequest request, String clientIp) {
        AdminUser existing = adminUserMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        boolean builtin = isBuiltin(existing);
        if (builtin && !Objects.equals(existing.getRoleId(), request.roleId())) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED, "内置账号的角色不可修改");
        }
        if (builtin && request.status() != null && request.status() == AppConstants.ACCOUNT_DISABLED) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED, "内置账号不可禁用");
        }

        Role role = resolveRole(request.roleId(), existing);
        String username = normalizeUsername(request.username());
        String mobile = normalizeMobile(request.mobile());
        ensureUsernameAvailable(username, id);
        ensureMobileAvailable(mobile, id);

        AdminUser update = new AdminUser();
        update.setId(id);
        update.setUsername(username);
        update.setMobile(mobile);
        update.setEmail(normalize(request.email()));
        update.setRoleId(role.getId());
        if (request.status() != null) {
            update.setStatus(request.status());
        }
        if (request.password() != null && !request.password().isBlank()) {
            update.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        adminUserMapper.updateById(update);

        operationLogService.record(currentAdmin.id(), currentAdmin.username(), ACTION_UPDATE,
                TARGET_USER, username, clientIp);
    }

    @Override
    @Transactional
    public void updateStatus(BatchStatusRequest request, String clientIp) {
        List<AdminUser> users = loadUsers(request.ids());
        if (users.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        boolean disabling = request.status() == AppConstants.ACCOUNT_DISABLED;
        for (AdminUser user : users) {
            if (disabling && (isBuiltin(user) || Objects.equals(user.getId(), currentAdmin.id()))) {
                throw new BizException(ErrorCode.BUILTIN_PROTECTED, "内置账号与当前登录账号不可禁用");
            }
        }

        AdminUser update = new AdminUser();
        update.setStatus(request.status());
        adminUserMapper.update(update, Wrappers.<AdminUser>lambdaUpdate()
                .in(AdminUser::getId, request.ids()));

        operationLogService.record(currentAdmin.id(), currentAdmin.username(), ACTION_STATUS,
                TARGET_USER, "批量" + (disabling ? "禁用" : "启用") + " " + users.size() + " 个账号", clientIp);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, String clientIp) {
        AdminUser user = adminUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        assertDeletable(user);
        adminUserMapper.deleteById(id);
        operationLogService.record(currentAdmin.id(), currentAdmin.username(), ACTION_DELETE,
                TARGET_USER, user.getUsername(), clientIp);
    }

    @Override
    @Transactional
    public void deleteUsers(List<Long> ids, String clientIp) {
        List<AdminUser> users = loadUsers(ids);
        if (users.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        users.forEach(this::assertDeletable);
        adminUserMapper.deleteByIds(ids);
        operationLogService.record(currentAdmin.id(), currentAdmin.username(), ACTION_DELETE,
                TARGET_USER, "批量删除 " + users.size() + " 个账号", clientIp);
    }

    @Override
    @Transactional
    public ResetPasswordVO resetPassword(Long id, String clientIp) {
        AdminUser user = adminUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        AdminUser update = new AdminUser();
        update.setId(id);
        update.setPasswordHash(passwordEncoder.encode(defaultPassword));
        adminUserMapper.updateById(update);

        operationLogService.record(currentAdmin.id(), currentAdmin.username(), ACTION_UPDATE,
                TARGET_USER, user.getUsername() + "·重置密码", clientIp);
        return new ResetPasswordVO(defaultPassword);
    }

    @Override
    public AdminUserDetailVO detail(Long id) {
        AdminUser user = adminUserMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        Map<Long, Role> roles = loadRoles(List.of(user));
        List<OperationLog> logs = operationLogMapper.selectList(Wrappers.<OperationLog>lambdaQuery()
                .eq(OperationLog::getUserId, id)
                .orderByDesc(OperationLog::getId)
                .last("LIMIT 10"));
        List<OperationLogVO> recentLogs = logs.stream()
                .map(log -> new OperationLogVO(log.getId(), log.getUserId(), log.getUsername(),
                        log.getAction(), log.getTargetType(), log.getTargetName(), log.getIp(),
                        toIso(log.getCreatedAt())))
                .toList();
        return new AdminUserDetailVO(toVO(user, roles.get(user.getRoleId())), recentLogs);
    }

    @Override
    public List<RoleOptionVO> listAssignableRoles() {
        return roleMapper.selectList(Wrappers.<Role>lambdaQuery().orderByAsc(Role::getId)).stream()
                .filter(role -> !AppConstants.ROLE_SUPER_ADMIN.equals(role.getCode()))
                .map(role -> new RoleOptionVO(role.getId(), role.getName(), role.getCode()))
                .toList();
    }

    private Map<Long, Role> loadRoles(List<AdminUser> users) {
        List<Long> roleIds = users.stream().map(AdminUser::getRoleId).filter(Objects::nonNull).distinct().toList();
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return roleMapper.selectByIds(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));
    }

    /**
     * 校验角色可分配性。
     *
     * <p>超管是内置唯一角色，新增时一律不可选（04 A-M02）；编辑内置账号时沿用原角色属于合法情况。
     */
    private Role resolveRole(Long roleId, AdminUser existing) {
        Role role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "角色不存在");
        }
        boolean unchanged = existing != null && Objects.equals(existing.getRoleId(), roleId);
        if (AppConstants.ROLE_SUPER_ADMIN.equals(role.getCode()) && !unchanged) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED, "不可分配超级管理员角色");
        }
        return role;
    }

    /**
     * 用户名归一化：去掉首尾空白后校验长度。
     *
     * <p>字符不限（中文、符号均可），但 {@code @Size} 校验的是原始值，
     * 形如 " ab " 的输入 trim 后会不足 2 位，故按 trim 后的实际长度再兜一次。
     */
    private String normalizeUsername(String raw) {
        String username = raw == null ? "" : raw.trim();
        if (username.length() < 2 || username.length() > 50) {
            throw new BizException(ErrorCode.PARAM_INVALID, "用户名长度需为 2~50 位");
        }
        return username;
    }

    private void ensureUsernameAvailable(String username, Long excludeId) {
        var wrapper = Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(AdminUser::getId, excludeId);
        }
        if (adminUserMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.NAME_EXISTS, "用户名已存在");
        }
    }

    private void ensureMobileAvailable(String mobile, Long excludeId) {
        if (mobile == null) {
            return;
        }
        var wrapper = Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getMobile, mobile);
        if (excludeId != null) {
            wrapper.ne(AdminUser::getId, excludeId);
        }
        if (adminUserMapper.selectCount(wrapper) > 0) {
            throw new BizException(ErrorCode.MOBILE_EXISTS);
        }
    }

    private List<AdminUser> loadUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return adminUserMapper.selectByIds(ids.stream().distinct().toList());
    }

    /** 内置账号与当前登录账号都不允许删除。 */
    private void assertDeletable(AdminUser user) {
        if (isBuiltin(user)) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED, "内置账号不可删除");
        }
        if (Objects.equals(user.getId(), currentAdmin.id())) {
            throw new BizException(ErrorCode.BUILTIN_PROTECTED, "不能删除当前登录账号");
        }
    }

    private boolean isBuiltin(AdminUser user) {
        return user.getIsBuiltin() != null && user.getIsBuiltin() == 1;
    }

    private AdminUserVO toVO(AdminUser user, Role role) {
        return new AdminUserVO(user.getId(), user.getUsername(), user.getMobile(), user.getEmail(),
                user.getRoleId(), role == null ? "" : role.getName(), role == null ? "" : role.getCode(),
                user.getStatus(), isBuiltin(user), toIso(user.getLastLoginAt()), toIso(user.getCreatedAt()));
    }

    private String toIso(LocalDateTime utc) {
        return utc == null ? null : utc.atOffset(ZoneOffset.UTC).format(ISO_UTC);
    }

    /** 空串按「未设置」处理：admin_user.mobile 有唯一键，空串会互相冲突。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeMobile(String mobile) {
        return normalize(mobile);
    }
}
