/**
 * 认证与登录态（两端各自入口）。
 *
 * <p>职责：学员注册/登录/找回密码、管理端登录、JWT 签发与刷新、登出黑名单（Redis）、
 * 登录频控与失败锁定（D18）。接口前缀：/api/portal/v1/auth、/api/admin/v1/auth。
 */
package com.yunshu.zhixue.modules.auth;
