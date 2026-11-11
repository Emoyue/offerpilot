package com.yunshu.zhixue.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 仅超级管理员可访问（04 §2.8 / D4-5）。
 *
 * <p>角色管理 5 个接口不设权限码，只校验 {@code role.code = super_admin}；
 * 学员端令牌访问也会被拒绝（必须为管理端令牌域）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSuperAdmin {
}
