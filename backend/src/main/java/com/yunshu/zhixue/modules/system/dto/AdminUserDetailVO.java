package com.yunshu.zhixue.modules.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 后台用户详情（04 A-M08）：基本信息 + 最近操作。
 *
 * @param user        基本信息
 * @param recentLogs  最近 10 条操作日志
 */
@Schema(description = "后台用户详情")
public record AdminUserDetailVO(AdminUserVO user, List<OperationLogVO> recentLogs) {
}
