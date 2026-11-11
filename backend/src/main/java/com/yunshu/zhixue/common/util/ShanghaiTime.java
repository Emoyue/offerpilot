package com.yunshu.zhixue.common.util;

import com.yunshu.zhixue.common.constant.AppConstants;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * UTC 存储 ↔ Asia/Shanghai 展示的换算（04 §2.11 / D17）。
 *
 * <p>库内 datetime 一律是 UTC；接口入参的日期（前端传 yyyy-MM-dd）按上海自然日理解，
 * 因此筛选「某天」需要换算成 [当天 00:00+08, 次日 00:00+08) 对应的 UTC 区间。
 */
public final class ShanghaiTime {

    private static final ZoneId SHANGHAI = ZoneId.of(AppConstants.BUSINESS_ZONE);
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ShanghaiTime() {
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /** 上海某日 00:00 对应的 UTC 时刻。 */
    public static LocalDateTime toUtcStartOfDay(LocalDate date) {
        return date.atStartOfDay(SHANGHAI).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** 上海某日次日 00:00 对应的 UTC 时刻（右开区间上界）。 */
    public static LocalDateTime toUtcEndExclusive(LocalDate date) {
        return date.plusDays(1).atStartOfDay(SHANGHAI).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** UTC 时间格式化为上海本地可读字符串，用于 CSV 导出。 */
    public static String formatForDisplay(LocalDateTime utc) {
        if (utc == null) {
            return "";
        }
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(SHANGHAI).format(DISPLAY_FORMAT);
    }
}
