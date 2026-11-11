package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.common.util.ShanghaiTime;
import com.yunshu.zhixue.modules.system.dto.OperationLogVO;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import com.yunshu.zhixue.security.CurrentAdmin;
import com.yunshu.zhixue.security.RequirePermission;
import com.yunshu.zhixue.support.web.ClientIp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志（04 §5.11，A-G01~A-G04）。
 *
 * <p>导出为同步 CSV（带 BOM，Excel 直接可读），不建任务表（Q10）；清空为物理删除（03 §6）。
 */
@RestController
@RequestMapping("/api/admin/v1/logs")
@Tag(name = "管理端-操作日志", description = "日志查询、详情、CSV 导出与清空")
public class OperationLogController {

    private static final String EXPORT_FILENAME_PREFIX = "operation-log-";
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final OperationLogService operationLogService;
    private final CurrentAdmin currentAdmin;

    public OperationLogController(OperationLogService operationLogService, CurrentAdmin currentAdmin) {
        this.operationLogService = operationLogService;
        this.currentAdmin = currentAdmin;
    }

    @GetMapping
    @RequirePermission("system:log_view")
    @Operation(summary = "日志列表（A-G01）", description = "支持关键词、操作类型（中文枚举）与日期范围筛选")
    public ApiResponse<PageResult<OperationLogVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "" + AppConstants.PAGE_DEFAULT) int page,
            @RequestParam(defaultValue = "" + AppConstants.PAGE_SIZE_DEFAULT) int pageSize) {
        return ApiResponse.ok(operationLogService.page(keyword, operationType, startDate, endDate, page, pageSize));
    }

    @GetMapping("/{id}")
    @RequirePermission("system:log_view")
    @Operation(summary = "日志详情（A-G02）")
    public ApiResponse<OperationLogVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(operationLogService.detail(id));
    }

    @GetMapping("/export")
    @RequirePermission("system:log_view")
    @Operation(summary = "导出日志（A-G03）", description = "同步生成 CSV 下载流，带 BOM，单次上限 1 万行")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String operationType,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                       HttpServletResponse response,
                       HttpServletRequest servletRequest) throws IOException {
        List<OperationLogVO> rows = operationLogService.listForExport(keyword, operationType, startDate, endDate);

        String filename = EXPORT_FILENAME_PREFIX + LocalDateTime.now().format(FILE_STAMP) + ".csv";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded);

        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF');
        writer.write("操作人,操作类型,对象类型,操作对象,IP,操作时间（北京时间）\r\n");
        for (OperationLogVO row : rows) {
            writer.write(String.join(",",
                    csv(row.username()), csv(row.action()), csv(row.targetType()),
                    csv(row.targetName()), csv(row.ip()), csv(displayTime(row.createdAt()))));
            writer.write("\r\n");
        }
        writer.flush();

        // 导出也属于审计范围（01 B5）
        operationLogService.record(currentAdmin.id(), currentAdmin.username(), "导出", "操作日志",
                "导出 " + rows.size() + " 条日志", ClientIp.resolve(servletRequest));
    }

    @DeleteMapping
    @RequirePermission("system:log_view")
    @Operation(summary = "清空日志（A-G04）", description = "物理删除全部日志，清空动作本身会留下一条记录")
    public ApiResponse<Void> clearAll(HttpServletRequest servletRequest) {
        operationLogService.clearAll(ClientIp.resolve(servletRequest));
        return ApiResponse.ok();
    }

    /** UTC ISO-8601 → 北京时间可读字符串。 */
    private String displayTime(String isoUtc) {
        if (isoUtc == null || isoUtc.isBlank()) {
            return "";
        }
        return ShanghaiTime.formatForDisplay(
                OffsetDateTime.parse(isoUtc).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
    }

    /** CSV 转义：含逗号/引号/换行时加引号，内部引号翻倍。 */
    private String csv(String value) {
        String text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
