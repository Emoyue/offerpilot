package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.AdminUserCreateRequest;
import com.yunshu.zhixue.modules.system.dto.AdminUserDetailVO;
import com.yunshu.zhixue.modules.system.dto.AdminUserUpdateRequest;
import com.yunshu.zhixue.modules.system.dto.AdminUserVO;
import com.yunshu.zhixue.modules.system.dto.BatchIdsRequest;
import com.yunshu.zhixue.modules.system.dto.BatchStatusRequest;
import com.yunshu.zhixue.modules.system.dto.ResetPasswordVO;
import com.yunshu.zhixue.modules.system.dto.RoleOptionVO;
import java.util.List;
import com.yunshu.zhixue.modules.system.service.AdminUserService;
import com.yunshu.zhixue.security.RequirePermission;
import com.yunshu.zhixue.support.web.ClientIp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理（04 §5.9，A-M01~A-M08）。
 *
 * <p>权限：查看/详情 user:view；新增 user:add；编辑/重置密码 user:edit；删除 user:disable（附录 A）。
 */
@RestController
@RequestMapping("/api/admin/v1/users")
@Tag(name = "管理端-用户管理", description = "后台用户增改、批量启停、删除与重置密码")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @RequirePermission("user:view")
    @Operation(summary = "用户列表（A-M01）", description = "支持用户名/手机号关键词、角色与状态筛选")
    public ApiResponse<PageResult<AdminUserVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "" + AppConstants.PAGE_DEFAULT) int page,
            @RequestParam(defaultValue = "" + AppConstants.PAGE_SIZE_DEFAULT) int pageSize) {
        return ApiResponse.ok(adminUserService.listUsers(keyword, roleId, status, page, pageSize));
    }

    @GetMapping("/role-options")
    @RequirePermission("user:view")
    @Operation(summary = "可分配角色选项", description = "用户新增/编辑的角色下拉，排除内置超管角色")
    public ApiResponse<List<RoleOptionVO>> roleOptions() {
        return ApiResponse.ok(adminUserService.listAssignableRoles());
    }

    @PostMapping
    @RequirePermission("user:add")
    @Operation(summary = "新增用户（A-M02）", description = "用户名唯一 30001、手机号唯一 30002；角色不可为超管")
    public ApiResponse<Long> create(@Valid @RequestBody AdminUserCreateRequest request,
                                    HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminUserService.createUser(request, ClientIp.resolve(servletRequest)));
    }

    @PutMapping("/{id}")
    @RequirePermission("user:edit")
    @Operation(summary = "编辑用户（A-M03）", description = "内置账号角色只读；密码留空表示不修改")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody AdminUserUpdateRequest request,
                                    HttpServletRequest servletRequest) {
        adminUserService.updateUser(id, request, ClientIp.resolve(servletRequest));
        return ApiResponse.ok();
    }

    @PutMapping("/batch-status")
    @RequirePermission("user:edit")
    @Operation(summary = "批量启用/禁用（A-M04）", description = "内置账号与当前登录账号不可禁用")
    public ApiResponse<Void> batchStatus(@Valid @RequestBody BatchStatusRequest request,
                                         HttpServletRequest servletRequest) {
        adminUserService.updateStatus(request, ClientIp.resolve(servletRequest));
        return ApiResponse.ok();
    }

    @DeleteMapping("/batch")
    @RequirePermission("user:disable")
    @Operation(summary = "批量删除（A-M06）", description = "内置账号与当前登录账号不可删除")
    public ApiResponse<Void> batchDelete(@Valid @RequestBody BatchIdsRequest request,
                                         HttpServletRequest servletRequest) {
        adminUserService.deleteUsers(request.ids(), ClientIp.resolve(servletRequest));
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("user:disable")
    @Operation(summary = "删除用户（A-M05）", description = "逻辑删除；内置账号返回 30004")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest servletRequest) {
        adminUserService.deleteUser(id, ClientIp.resolve(servletRequest));
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/reset-password")
    @RequirePermission("user:edit")
    @Operation(summary = "重置密码（A-M07）", description = "重置为部署期配置的默认密码（app.default-password），仅此响应明文返回")
    public ApiResponse<ResetPasswordVO> resetPassword(@PathVariable Long id,
                                                      HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminUserService.resetPassword(id, ClientIp.resolve(servletRequest)));
    }

    @GetMapping("/{id}/detail")
    @RequirePermission("user:view")
    @Operation(summary = "用户详情（A-M08）", description = "基本信息 + 最近 10 条操作日志")
    public ApiResponse<AdminUserDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminUserService.detail(id));
    }
}
