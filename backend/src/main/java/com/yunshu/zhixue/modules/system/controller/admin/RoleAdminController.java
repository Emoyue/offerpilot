package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.PermissionGroupVO;
import com.yunshu.zhixue.modules.system.dto.RoleCreateRequest;
import com.yunshu.zhixue.modules.system.dto.RolePermissionUpdateRequest;
import com.yunshu.zhixue.modules.system.dto.RoleVO;
import com.yunshu.zhixue.modules.system.service.RoleService;
import com.yunshu.zhixue.security.RequireSuperAdmin;
import com.yunshu.zhixue.support.web.ClientIp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
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
 * 角色管理与权限树（04 §5.10，A-L01~A-L05）。
 *
 * <p>全部接口仅超管可访问（不设权限码，D4-5）；需求未定义角色基本信息编辑，故不提供 PUT /roles/{id}。
 */
@RestController
@RequestMapping("/api/admin/v1")
@Tag(name = "管理端-角色权限", description = "角色卡片、权限树与授权保存（仅超管）")
public class RoleAdminController {

    private final RoleService roleService;

    public RoleAdminController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/roles")
    @RequireSuperAdmin
    @Operation(summary = "角色列表（A-L01）", description = "含用户数与已授权权限标签")
    public ApiResponse<List<RoleVO>> listRoles() {
        return ApiResponse.ok(roleService.listRoles());
    }

    @PostMapping("/roles")
    @RequireSuperAdmin
    @Operation(summary = "新增角色（A-L02）", description = "编码唯一，重复返回 30001")
    public ApiResponse<Long> createRole(@Valid @RequestBody RoleCreateRequest request,
                                        HttpServletRequest servletRequest) {
        return ApiResponse.ok(roleService.createRole(request, ClientIp.resolve(servletRequest)));
    }

    @DeleteMapping("/roles/{id}")
    @RequireSuperAdmin
    @Operation(summary = "删除角色（A-L03）", description = "角色下有用户返回 30003，超管角色返回 30004")
    public ApiResponse<Void> deleteRole(@PathVariable Long id, HttpServletRequest servletRequest) {
        roleService.deleteRole(id, ClientIp.resolve(servletRequest));
        return ApiResponse.ok();
    }

    @GetMapping("/permissions/tree")
    @RequireSuperAdmin
    @Operation(summary = "权限树（A-L04）", description = "9 组 34 项；传 roleId 时返回该角色的勾选状态")
    public ApiResponse<List<PermissionGroupVO>> permissionTree(
            @RequestParam(required = false) Long roleId) {
        return ApiResponse.ok(roleService.permissionTree(roleId));
    }

    @PutMapping("/roles/{id}/permissions")
    @RequireSuperAdmin
    @Operation(summary = "保存角色权限（A-L05）", description = "全量覆盖；超管角色不可编辑（30004）")
    public ApiResponse<Void> savePermissions(@PathVariable Long id,
                                             @Valid @RequestBody RolePermissionUpdateRequest request,
                                             HttpServletRequest servletRequest) {
        roleService.savePermissions(id, request.permissionIds(), ClientIp.resolve(servletRequest));
        return ApiResponse.ok();
    }
}
