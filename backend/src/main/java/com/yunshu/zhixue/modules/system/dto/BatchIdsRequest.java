package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 批量删除（04 A-M06 / A-U05 等）：只带主键集合。
 */
@Schema(description = "批量删除请求")
public record BatchIdsRequest(
        @Schema(description = "待删除的主键集合")
        @NotEmpty(message = "请至少选择一条记录")
        @Size(max = 100, message = "单次最多操作 100 条记录")
        List<Long> ids
) {
}
