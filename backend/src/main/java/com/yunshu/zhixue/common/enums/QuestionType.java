package com.yunshu.zhixue.common.enums;

/**
 * 题目类型（数据库 question.type，03 §5）。
 */
public enum QuestionType {

    SINGLE(1, "单选"),
    MULTIPLE(2, "多选"),
    JUDGE(3, "判断"),
    SHORT_ANSWER(4, "简答");

    private final int code;
    private final String label;

    QuestionType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    /** 客观题可即时判分；简答题需走 AI 批改。 */
    public boolean isObjective() {
        return this != SHORT_ANSWER;
    }
}
