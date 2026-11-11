package com.yunshu.zhixue.modules.system.dto;

/**
 * 供 auth 模块登录使用的账号快照。
 *
 * <p>按 02 §3.2「不跨模块直连数据库」，auth 只能经 system 的 Service 取数，
 * 因此这里只暴露登录必需的字段，不把 admin_user 实体透出去。
 *
 * @param id           账号主键
 * @param username     登录用户名
 * @param passwordHash BCrypt 哈希
 * @param roleId       角色主键
 * @param status       1 启用 / 0 禁用
 */
public record AdminAccountSnapshot(Long id, String username, String passwordHash, Long roleId, Integer status) {

    public boolean enabled() {
        return status != null && status == 1;
    }
}
