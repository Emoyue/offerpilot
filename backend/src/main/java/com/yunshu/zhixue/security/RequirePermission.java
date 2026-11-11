package com.yunshu.zhixue.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理端接口权限校验（04 §2.8）。
 *
 * <p>用法：{@code @RequirePermission("question:edit")}，权限码与 permission 表 code 一一对应。
 * 超级管理员（role.code = super_admin）默认放行；角色管理接口不设权限码，仅校验超管身份。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** 权限码，如 subject:view */
    String value();
}
