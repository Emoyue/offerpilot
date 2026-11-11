package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.system.dto.ImageUploadVO;
import com.yunshu.zhixue.modules.system.dto.SystemSettingUpdateRequest;
import com.yunshu.zhixue.modules.system.dto.SystemSettingVO;
import com.yunshu.zhixue.modules.system.service.SystemSettingService;
import com.yunshu.zhixue.security.RequirePermission;
import com.yunshu.zhixue.support.web.ClientIp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 系统设置与图片上传（04 §5.12 / §5.13）。
 *
 * <p>读取放宽到「登录即可」：后台框架渲染标题与 Logo 时需要；保存与恢复默认要求 system:setting。
 */
@RestController
@RequestMapping("/api/admin/v1")
@Tag(name = "管理端-系统设置", description = "平台基本信息、Logo/头像上传与恢复默认")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    public SystemSettingController(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    @GetMapping("/settings")
    @Operation(summary = "读取系统设置（A-T01）", description = "登录即可读取，供框架渲染标题与 Logo")
    public ApiResponse<SystemSettingVO> get() {
        return ApiResponse.ok(systemSettingService.get());
    }

    @PutMapping("/settings")
    @RequirePermission("system:setting")
    @Operation(summary = "保存系统设置（A-T02）", description = "按 config_key upsert；平台名称必填")
    public ApiResponse<Void> save(@Valid @RequestBody SystemSettingUpdateRequest request,
                                  HttpServletRequest servletRequest) {
        systemSettingService.save(request, ClientIp.resolve(servletRequest));
        return ApiResponse.ok();
    }

    @PostMapping("/settings/reset")
    @RequirePermission("system:setting")
    @Operation(summary = "恢复默认设置（A-T03）", description = "四项恢复为默认值，前端需二次确认")
    public ApiResponse<Void> reset(HttpServletRequest servletRequest) {
        systemSettingService.reset(ClientIp.resolve(servletRequest));
        return ApiResponse.ok();
    }

    @PostMapping("/files/image")
    @RequirePermission("system:setting")
    @Operation(summary = "上传图片（A-F01）", description = "JPG/PNG，≤2MB；Logo 裁切为 256×256，头像 200×200")
    public ApiResponse<ImageUploadVO> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "logo") String scene,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(new ImageUploadVO(
                systemSettingService.uploadImage(file, scene, ClientIp.resolve(servletRequest))));
    }
}
