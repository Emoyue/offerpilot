package com.yunshu.zhixue.modules.subject.dto;

import com.yunshu.zhixue.modules.subject.entity.Subject;

/**
 * 学科出参（学员端/管理端通用）。不直接暴露实体，避免 deleteFlag 等内部字段外泄。
 */
public record SubjectVO(
        Long id,
        String name,
        String icon,
        String description,
        String themeColor,
        Integer sortOrder
) {
    public static SubjectVO from(Subject entity) {
        return new SubjectVO(
                entity.getId(),
                entity.getName(),
                entity.getIcon(),
                entity.getDescription(),
                entity.getThemeColor(),
                entity.getSortOrder());
    }
}
