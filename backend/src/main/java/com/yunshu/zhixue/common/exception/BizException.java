package com.yunshu.zhixue.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常：由 {@link GlobalExceptionHandler} 统一转换为 HTTP 状态码 + 业务码。
 */
public class BizException extends RuntimeException {

    private final int code;
    private final HttpStatus httpStatus;

    public BizException(ErrorCode errorCode) {
        super(errorCode.message());
        this.code = errorCode.code();
        this.httpStatus = errorCode.httpStatus();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.code();
        this.httpStatus = errorCode.httpStatus();
    }

    public BizException(int code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
