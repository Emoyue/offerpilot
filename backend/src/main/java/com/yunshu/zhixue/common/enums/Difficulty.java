package com.yunshu.zhixue.common.enums;

/**
 * 难度（数据库 question.difficulty / interview_question.difficulty，03 §5）。
 */
public enum Difficulty {

    EASY(1, "简单"),
    MEDIUM(2, "中等"),
    HARD(3, "困难");

    private final int code;
    private final String label;

    Difficulty(int code, String label) {
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
