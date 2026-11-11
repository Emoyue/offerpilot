package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.common.util.ShanghaiTime;
import com.yunshu.zhixue.modules.system.dto.OperationLogVO;
import com.yunshu.zhixue.modules.system.entity.OperationLog;
import com.yunshu.zhixue.modules.system.mapper.OperationLogMapper;
import com.yunshu.zhixue.security.CurrentAdmin;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 操作日志：写入 + 查询 + 导出 + 清空。
 *
 * <p>审计失败不能影响主流程（登录成功与否不应取决于日志表是否可写），
 * 因此写入路径吞掉异常并降级为告警日志；查询与清空属于显式管理操作，异常正常抛出。
 *
 * <p>「清空日志」是物理删除（03 §6，operation_log 无逻辑删除），清空本身会再记一条，
 * 保留「谁在什么时候清了日志」的线索。
 */
@Service
public class OperationLogService {

    private static final Logger log = LoggerFactory.getLogger(OperationLogService.class);
    private static final String ACTION_DELETE = "删除";
    private static final String TARGET_LOG = "操作日志";
    private static final DateTimeFormatter ISO_UTC = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final OperationLogMapper operationLogMapper;
    private final CurrentAdmin currentAdmin;

    public OperationLogService(OperationLogMapper operationLogMapper, CurrentAdmin currentAdmin) {
        this.operationLogMapper = operationLogMapper;
        this.currentAdmin = currentAdmin;
    }

    public void record(Long userId, String username, String action,
                       String targetType, String targetName, String ip) {
        try {
            OperationLog entry = new OperationLog();
            entry.setUserId(userId);
            entry.setUsername(truncate(username, 50));
            entry.setAction(truncate(action, 50));
            entry.setTargetType(truncate(targetType, 50));
            entry.setTargetName(truncate(targetName, 255));
            entry.setIp(truncate(ip, 64));
            entry.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            operationLogMapper.insert(entry);
        } catch (Exception ex) {
            log.warn("操作日志写入失败 action={} user={} reason={}", action, username, ex.getMessage());
        }
    }

    /** 分页查询（A-G01）。日期按上海自然日理解，换算成 UTC 区间。 */
    public PageResult<OperationLogVO> page(String keyword, String operationType,
                                           LocalDate startDate, LocalDate endDate,
                                           int page, int pageSize) {
        int safePage = Math.max(page, AppConstants.PAGE_DEFAULT);
        int safeSize = Math.min(Math.max(pageSize, 1), AppConstants.PAGE_SIZE_MAX);

        IPage<OperationLog> result = operationLogMapper.selectPage(new Page<>(safePage, safeSize),
                buildWrapper(keyword, operationType, startDate, endDate));
        List<OperationLogVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, result.getTotal(), safePage, safeSize);
    }

    public OperationLogVO detail(Long id) {
        OperationLog entry = operationLogMapper.selectById(id);
        if (entry == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "日志不存在");
        }
        return toVO(entry);
    }

    /** 导出用全量查询，单次上限 1 万行（04 §7.3），超出由前端缩小筛选范围。 */
    public List<OperationLogVO> listForExport(String keyword, String operationType,
                                              LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<OperationLog> wrapper = buildWrapper(keyword, operationType, startDate, endDate);
        wrapper.last("LIMIT " + AppConstants.EXPORT_MAX_ROWS);
        return operationLogMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    /** 清空日志（A-G04）：物理删除后补一条审计记录。 */
    @Transactional
    public void clearAll(String clientIp) {
        int deleted = operationLogMapper.delete(Wrappers.<OperationLog>lambdaQuery());
        record(currentAdmin.id(), currentAdmin.username(), ACTION_DELETE, TARGET_LOG,
                "清空全部日志（" + deleted + " 条）", clientIp);
    }

    private LambdaQueryWrapper<OperationLog> buildWrapper(String keyword, String operationType,
                                                          LocalDate startDate, LocalDate endDate) {
        String trimmed = keyword == null ? "" : keyword.trim();
        LambdaQueryWrapper<OperationLog> wrapper = Wrappers.<OperationLog>lambdaQuery()
                .and(!trimmed.isEmpty(), w -> w.like(OperationLog::getUsername, trimmed)
                        .or()
                        .like(OperationLog::getTargetName, trimmed)
                        .or()
                        .like(OperationLog::getAction, trimmed))
                .eq(operationType != null && !operationType.isBlank(),
                        OperationLog::getAction, operationType)
                .orderByDesc(OperationLog::getId);
        applyDateRange(wrapper, startDate, endDate);
        return wrapper;
    }

    private void applyDateRange(LambdaQueryWrapper<OperationLog> wrapper,
                                LocalDate startDate, LocalDate endDate) {
        if (startDate != null) {
            wrapper.ge(OperationLog::getCreatedAt, ShanghaiTime.toUtcStartOfDay(startDate));
        }
        if (endDate != null) {
            wrapper.lt(OperationLog::getCreatedAt, ShanghaiTime.toUtcEndExclusive(endDate));
        }
    }

    private OperationLogVO toVO(OperationLog entry) {
        return new OperationLogVO(entry.getId(), entry.getUserId(), entry.getUsername(),
                entry.getAction(), entry.getTargetType(), entry.getTargetName(), entry.getIp(),
                entry.getCreatedAt() == null ? null
                        : entry.getCreatedAt().atOffset(ZoneOffset.UTC).format(ISO_UTC));
    }

    /** 日志列为定长，超长截断比整条写入失败更可取。 */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
