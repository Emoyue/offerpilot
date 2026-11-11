package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 批量启用/禁用（04 A-M04）。
 *
 * @param ids    账号主键集合
 * @param status 1 启用 / 0 禁用
 */
@Schema(description = "批量启用/禁用请求")
public record BatchStatusRequest(
        @Schema(description = "账号 ID 集合")
        @NotEmpty(message = "请至少选择一个账号")
        @Size(max = 100, message = "单次最多操作 100 个账号")
        List<Long> ids,

        @Schema(description = "目标状态：1 启用 / 0 禁用", example = "0")
        @NotNull(message = "请指定目标状态")
        @Min(value = 0, message = "状态值不合法")
        @Max(value = 1, message = "状态值不合法")
        Integer status
) {
}
