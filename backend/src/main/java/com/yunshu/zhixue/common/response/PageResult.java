package com.yunshu.zhixue.common.response;

import java.util.List;

/**
 * 统一分页响应（04 §2.4）：data = { list, total, page, pageSize }。
 *
 * @param <T> 列表元素类型
 */
public record PageResult<T>(List<T> list, long total, int page, int pageSize) {

    public static <T> PageResult<T> of(List<T> list, long total, int page, int pageSize) {
        return new PageResult<>(list, total, page, pageSize);
    }

    public static <T> PageResult<T> empty(int page, int pageSize) {
        return new PageResult<>(List.of(), 0, page, pageSize);
    }
}
