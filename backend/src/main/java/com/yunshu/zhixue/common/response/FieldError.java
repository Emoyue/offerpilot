package com.yunshu.zhixue.common.response;

/**
 * 字段级校验错误（04 §2.3/§2.6）：参数校验失败（10000）时随响应返回。
 *
 * @param field  字段名，如 mobile
 * @param reason 可读原因，如 手机号格式不正确
 */
public record FieldError(String field, String reason) {
}
