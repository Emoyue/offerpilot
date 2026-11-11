import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, type AdminLoginPayload } from '@/api/auth'
import type { AdminProfile } from '@/types'
import {
  clearSession,
  getAccessToken,
  getRefreshToken,
  getStoredProfile,
  saveSession
} from '@/utils/session'

/**
 * 管理员登录态与权限码。
 *
 * 令牌由 utils/session 统一持久化（http 拦截器刷新令牌时也写同一份），
 * 这里只保留响应式副本供页面使用；刷新页面后从 localStorage 恢复。
 *
 * 权限码用于菜单渲染与 v-permission 按钮级控制；超管（super_admin）默认全放行。
 * 这些都是展示层便利，真正的身份认证与权限校验在后端（CLAUDE.md 阶段六 第 8 条）。
 */
export const useAdminStore = defineStore('admin', () => {
  const accessToken = ref<string | null>(getAccessToken())
  const profile = ref<AdminProfile | null>(getStoredProfile())

  const isLoggedIn = computed(() => Boolean(accessToken.value))
  const roleCode = computed(() => profile.value?.roleCode ?? '')
  const roleName = computed(() => profile.value?.roleName ?? '')
  const displayName = computed(() => profile.value?.username ?? '管理员')
  const isSuperAdmin = computed(() => roleCode.value === 'super_admin')
  const permissions = computed(() => profile.value?.permissions ?? [])

  /** 登录：写入本地会话并刷新内存状态，异常交由调用方提示。 */
  async function login(payload: AdminLoginPayload, remember = true) {
    const session = await loginApi(payload)
    saveSession(session, remember)
    accessToken.value = session.accessToken
    profile.value = session.profile
  }

  /**
   * 登出：先通知后端拉黑令牌，再清本地。
   *
   * 后端不可达时也必须完成本地清理——否则用户会被"卡"在登录态里无法切换账号。
   */
  async function logout() {
    const refreshToken = getRefreshToken()
    try {
      await logoutApi(refreshToken)
    } catch (error) {
      console.warn('登出接口调用失败，已清理本地登录态', error)
    } finally {
      clear()
    }
  }

  function clear() {
    clearSession()
    accessToken.value = null
    profile.value = null
  }

  function hasPermission(code: string) {
    return isSuperAdmin.value || permissions.value.includes(code)
  }

  return {
    accessToken,
    profile,
    isLoggedIn,
    roleCode,
    roleName,
    displayName,
    isSuperAdmin,
    permissions,
    login,
    logout,
    clear,
    hasPermission
  }
})
