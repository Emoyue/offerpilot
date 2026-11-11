package com.yunshu.zhixue.modules.system.dto;

/**
 * 供 auth 模块签发令牌使用的角色快照。
 *
 * @param id   角色主键
 * @param name 角色名称（前端展示）
 * @param code 角色编码（超管判定）
 */
public record AdminRoleSnapshot(Long id, String name, String code) {
}
