package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 系统基本信息设置（04 A-T01）。
 *
 * <p>对应 system_setting 的 4 个 config_key；阈值类参数是固定常量，不在此配置（D13）。
 */
@Schema(description = "系统设置")
public record SystemSettingVO(
        @Schema(description = "平台名称") String siteName,
        @Schema(description = "平台描述") String siteDescription,
        @Schema(description = "Logo 直链") String logoUrl,
        @Schema(description = "默认头像直链") String defaultAvatar
) {
}
