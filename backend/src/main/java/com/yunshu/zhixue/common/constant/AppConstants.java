package com.yunshu.zhixue.common.constant;

/**
 * 业务固定阈值（D13：全部为编译期常量，不做后台可配置）。
 *
 * <p>来源：需求文档 §5、03 §7、04 §7。改动需同步修改文档，不允许散落在业务代码里。
 */
public final class AppConstants {

    private AppConstants() {
    }

    /** 分页默认值与上限（04 §2.4） */
    public static final int PAGE_DEFAULT = 1;
    public static final int PAGE_SIZE_DEFAULT = 10;
    public static final int PAGE_SIZE_MAX = 100;

    /** AI 面试：可选题量 3/5/8/10/15，默认 10 */
    public static final int INTERVIEW_QUESTION_DEFAULT = 10;
    public static final int INTERVIEW_QUESTION_MIN = 3;
    public static final int INTERVIEW_QUESTION_MAX = 15;

    /** 单题作答超时：5 分钟 */
    public static final int INTERVIEW_ANSWER_TIMEOUT_SECONDS = 300;

    /** 每日 AI 面试配额：5 次，按 Asia/Shanghai 自然日 0 点重置（D17） */
    public static final int INTERVIEW_DAILY_QUOTA = 5;

    /** 简答 AI 批改通过阈值：≥6/10 */
    public static final int SHORT_ANSWER_PASS_SCORE = 6;

    /** 错题掌握判定：连续答对 3 次 */
    public static final int WRONG_QUESTION_MASTER_STREAK = 3;

    /** 题库导入单次上限（行） */
    public static final int IMPORT_MAX_ROWS = 500;

    /** 同步导出单次上限（行），超出提示缩小筛选范围（04 §7.3） */
    public static final int EXPORT_MAX_ROWS = 10_000;

    /** 短信验证码：有效期 5 分钟、重发间隔 60 秒、每日上限 10 次 */
    public static final int SMS_CODE_TTL_SECONDS = 300;
    public static final int SMS_RESEND_INTERVAL_SECONDS = 60;
    public static final int SMS_DAILY_LIMIT = 10;

    /** 登录失败锁定：连续 5 次错误锁定 5 分钟（D18） */
    public static final int LOGIN_MAX_FAIL_TIMES = 5;
    public static final int LOGIN_LOCK_MINUTES = 5;

    /** 时区：业务日界按上海，存储统一 UTC（D17） */
    public static final String BUSINESS_ZONE = "Asia/Shanghai";

    /** 内置超级管理员角色编码（role.code）：拥有全部权限，不可删除、不可改权限 */
    public static final String ROLE_SUPER_ADMIN = "super_admin";

    /** 账号状态：1 启用 / 0 禁用（附录 C） */
    public static final int ACCOUNT_ENABLED = 1;
    public static final int ACCOUNT_DISABLED = 0;

    /** 后台账号密码长度（04 §2.6：6~20 位） */
    public static final int PASSWORD_MIN_LENGTH = 6;
    public static final int PASSWORD_MAX_LENGTH = 20;

    /** 手机号正则（04 §2.6） */
    public static final String MOBILE_PATTERN = "^1[3-9]\\d{9}$";
}
