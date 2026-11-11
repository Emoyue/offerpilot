package com.yunshu.zhixue.common.response;

import java.util.List;

/**
 * 统一响应结构（04 §2.3 / api-rules）。
 *
 * <p>成功：{@code {"code":0,"message":"ok","data":{}}}
 * <br>失败：使用真实 HTTP 状态码 + 5 位业务码，并可能携带 fieldErrors / fileErrors。
 *
 * <p>约定：绝不出现「HTTP 200 + 业务失败」或「HTTP 500 + 业务成功」。
 */
public record ApiResponse<T>(int code, String message, T data, List<FieldError> fieldErrors) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(0, "ok", null, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message, List<FieldError> fieldErrors) {
        return new ApiResponse<>(code, message, null, fieldErrors);
    }
}
