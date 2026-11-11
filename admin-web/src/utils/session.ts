import type { AdminProfile, AdminSession } from '@/types'

/**
 * 管理端登录态的本地持久化。
 *
 * 单独抽出来的原因：axios 拦截器（令牌刷新）与 Pinia store（页面状态）都要读写同一份数据，
 * 放在 store 里会造成 store ←→ http 的循环依赖。这里只依赖类型，不依赖任何运行时模块。
 *
 * 登录页「记住登录状态」勾选时写 localStorage（关浏览器仍保留），
 * 未勾选时写 sessionStorage（关闭标签页即失效）。读取时两者都查，刷新令牌时写回原存储。
 *
 * ⚠️ 前端存储只影响页面体验，真正的鉴权由后端完成（CLAUDE.md 阶段六 第 8 条）。
 */

export const ACCESS_TOKEN_KEY = 'yszx_admin_token'
export const REFRESH_TOKEN_KEY = 'yszx_admin_refresh_token'
export const PROFILE_KEY = 'yszx_admin_profile'

const KEYS = [ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY, PROFILE_KEY]

function read(key: string): string | null {
  return localStorage.getItem(key) ?? sessionStorage.getItem(key)
}

/** 当前会话落在哪个存储：以刷新令牌所在位置为准。 */
function currentStorage(): Storage {
  if (localStorage.getItem(REFRESH_TOKEN_KEY)) {
    return localStorage
  }
  if (sessionStorage.getItem(REFRESH_TOKEN_KEY)) {
    return sessionStorage
  }
  return localStorage
}

export function getAccessToken(): string | null {
  return read(ACCESS_TOKEN_KEY)
}

export function getRefreshToken(): string | null {
  return read(REFRESH_TOKEN_KEY)
}

export function getStoredProfile(): AdminProfile | null {
  const raw = read(PROFILE_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as AdminProfile
  } catch {
    // 旧版本或手工篡改导致解析失败时直接丢弃，避免应用启动即报错
    KEYS.forEach((key) => {
      localStorage.removeItem(key)
      sessionStorage.removeItem(key)
    })
    return null
  }
}

/**
 * 写入会话。
 *
 * @param remember 显式指定存储位置；省略时沿用当前令牌所在存储（令牌刷新场景）。
 */
export function saveSession(session: AdminSession, remember?: boolean): void {
  const target = remember === undefined ? currentStorage() : remember ? localStorage : sessionStorage
  const other = target === localStorage ? sessionStorage : localStorage

  other.removeItem(ACCESS_TOKEN_KEY)
  other.removeItem(REFRESH_TOKEN_KEY)
  other.removeItem(PROFILE_KEY)

  target.setItem(ACCESS_TOKEN_KEY, session.accessToken)
  target.setItem(REFRESH_TOKEN_KEY, session.refreshToken)
  target.setItem(PROFILE_KEY, JSON.stringify(session.profile))
}

/** 登出或刷新失败时清空：两个存储都要清，避免残留半套登录态。 */
export function clearSession(): void {
  KEYS.forEach((key) => {
    localStorage.removeItem(key)
    sessionStorage.removeItem(key)
  })
}
