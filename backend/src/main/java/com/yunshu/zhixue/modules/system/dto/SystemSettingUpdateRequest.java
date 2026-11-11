package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 保存系统设置（04 A-T02）：平台名称必填，其余可空。
 */
@Schema(description = "保存系统设置请求")
public record SystemSettingUpdateRequest(
        @Schema(description = "平台名称", example = "OfferPilot")
        @NotBlank(message = "平台名称不能为空")
        @Size(max = 50, message = "平台名称不能超过 50 字")
        String siteName,

        @Schema(description = "平台描述")
        @Size(max = 255, message = "平台描述不能超过 255 字")
        String siteDescription,

        @Schema(description = "Logo 直链（先经 /files/image 上传）")
        @Size(max = 500, message = "Logo 地址过长")
        String logoUrl,

        @Schema(description = "默认头像直链")
        @Size(max = 500, message = "头像地址过长")
        String defaultAvatar
) {
}
