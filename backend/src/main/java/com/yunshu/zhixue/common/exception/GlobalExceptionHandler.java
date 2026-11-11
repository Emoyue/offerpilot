package com.yunshu.zhixue.common.exception;

import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理（04 §2.7）。
 *
 * <p>原则：错误信息面向用户可读；不返回堆栈、SQL、内部主机信息（api-rules）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBiz(BizException ex, HttpServletRequest request) {
        log.warn("业务异常 {} {} -> {} {}", request.getMethod(), request.getRequestURI(), ex.code(), ex.getMessage());
        return ResponseEntity.status(ex.httpStatus())
                .body(ApiResponse.fail(ex.code(), ex.getMessage()));
    }

    /**
     * 唯一索引冲突兜底：应用层已先查重，这里的漏网之鱼（并发写入等）也要给出可读错误码，而不是 10006。
     *
     * <p>按约束名判断是手机号还是名称/用户名/编码冲突。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException ex, HttpServletRequest request) {
        String detail = ex.getMostSpecificCause().getMessage();
        log.warn("唯一约束冲突 {} {} -> {}", request.getMethod(), request.getRequestURI(), detail);
        boolean mobile = detail != null && detail.contains("mobile");
        ErrorCode code = mobile ? ErrorCode.MOBILE_EXISTS : ErrorCode.NAME_EXISTS;
        return ResponseEntity.status(code.httpStatus()).body(ApiResponse.fail(code.code(), code.message()));
    }

    /** Bean Validation 失败：一次返回全部字段错误 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidation(BindException ex) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID.code(), ErrorCode.PARAM_INVALID.message(), errors));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(Exception ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID.code(), "缺少必要参数或参数类型不正确"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.JSON_PARSE_ERROR.code(), ErrorCode.JSON_PARSE_ERROR.message()));
    }

    /** 方法级权限注解鉴权失败 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.message()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ErrorCode.UNAUTHORIZED.code(), ErrorCode.UNAUTHORIZED.message()));
    }

    /** 路径不存在：不能让静态资源未命中落到兜底 500（api-rules：状态码须表达真实语义）。 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ErrorCode.NOT_FOUND.code(), ErrorCode.NOT_FOUND.message()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("未捕获异常 {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.SYSTEM_ERROR.code(), ErrorCode.SYSTEM_ERROR.message()));
    }
}
