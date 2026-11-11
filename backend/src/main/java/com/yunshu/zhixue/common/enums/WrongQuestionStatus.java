package com.yunshu.zhixue.common.enums;

/**
 * 错题状态（数据库 wrong_question.status，03 §5）。
 */
public enum WrongQuestionStatus {

    TO_REVIEW(1, "待复习"),
    MASTERED(2, "已掌握");

    private final int code;
    private final String label;

    WrongQuestionStatus(int code, String label) {
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
