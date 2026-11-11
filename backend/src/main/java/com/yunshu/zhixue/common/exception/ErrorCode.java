package com.yunshu.zhixue.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码（首位标识归属：1 通用 / 2 学员端 / 3 管理端）。
 *
 * <p>完整错误码表见 {@code docs/design/04.API接口设计.md} 附录 B，
 * 此处仅收录骨架阶段确定会用到的部分，后续按模块增量补齐。
 */
public enum ErrorCode {

    /** 参数校验失败 */
    PARAM_INVALID(10000, "参数校验失败", HttpStatus.BAD_REQUEST),
    /** 未登录或登录态已失效 */
    UNAUTHORIZED(10001, "未登录或登录已过期", HttpStatus.UNAUTHORIZED),
    /** 已登录但无该操作权限 */
    FORBIDDEN(10002, "无访问权限", HttpStatus.FORBIDDEN),
    /** 请求的资源不存在 */
    NOT_FOUND(10003, "资源不存在", HttpStatus.NOT_FOUND),
    /** 未捕获的系统异常（原始信息不返回给前端） */
    SYSTEM_ERROR(10006, "系统繁忙，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR),
    /** 请求体解析失败 */
    JSON_PARSE_ERROR(10007, "请求体格式不正确", HttpStatus.BAD_REQUEST),
    /** 上传文件类型或尺寸不符 */
    FILE_INVALID(10008, "文件类型或大小不符", HttpStatus.BAD_REQUEST),

    /** 学员端：手机号已注册 */
    STUDENT_MOBILE_EXISTS(20001, "手机号已注册", HttpStatus.BAD_REQUEST),

    /** 管理端：名称/用户名/编码已存在 */
    NAME_EXISTS(30001, "名称或编码已存在", HttpStatus.CONFLICT),
    /** 管理端：手机号已被占用 */
    MOBILE_EXISTS(30002, "手机号已存在", HttpStatus.CONFLICT),
    /** 管理端：角色下仍有用户，不可删除 */
    ROLE_HAS_USERS(30003, "该角色下存在用户，无法删除", HttpStatus.CONFLICT),
    /** 管理端：内置账号或超级管理员角色不可操作 */
    BUILTIN_PROTECTED(30004, "内置账号或超级管理员角色不可操作", HttpStatus.CONFLICT),
    /** 管理端：批量导入行级校验失败 */
    IMPORT_ROW_INVALID(30005, "导入数据校验未通过", HttpStatus.BAD_REQUEST),
    /** 管理端：权限保存失败 */
    PERMISSION_SAVE_FAILED(30008, "权限保存失败，请重试", HttpStatus.BAD_REQUEST),
    /** 管理端：账号或密码错误（附录 B：HTTP 400） */
    ADMIN_LOGIN_FAILED(30009, "账号或密码错误", HttpStatus.BAD_REQUEST),
    /** 管理端：连续登录失败导致账号临时锁定 */
    ADMIN_LOGIN_LOCKED(30010, "账号已临时锁定，请 5 分钟后再试", HttpStatus.LOCKED),
    /** 管理端：账号已被禁用 */
    ADMIN_DISABLED(30011, "账号已被禁用，请联系超级管理员", HttpStatus.FORBIDDEN);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
