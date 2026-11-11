package com.yunshu.zhixue.common.enums;

/**
 * 题目状态（数据库 question.status，03 §5）。
 *
 * <p>注意：{@link #DRAFT} 为 DB 预留值，当前不启用（04 Q6）。
 */
public enum QuestionStatus {

    PUBLISHED(1, "已上架"),
    OFFLINE(2, "已下架"),
    PENDING(3, "待审核"),
    /** 预留值，接口不接受、列表不展示 */
    DRAFT(4, "草稿"),
    REJECTED(5, "已驳回");

    private final int code;
    private final String label;

    QuestionStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }
}
