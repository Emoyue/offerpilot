import { request } from './http'
import type { AdminSession } from '@/types'

export interface AdminLoginPayload {
  /** 用户名或手机号（04 Q4） */
  account: string
  password: string
}

/** 管理端登录（A-A01）：成功后返回双令牌与管理员档案。 */
export function login(payload: AdminLoginPayload) {
  return request<AdminSession>({
    url: '/admin/v1/auth/login',
    method: 'post',
    data: payload
  })
}

/** 刷新令牌（A-A02）。正常由 http 拦截器自动调用，这里保留显式入口便于排障。 */
export function refresh(refreshToken: string) {
  return request<AdminSession>({
    url: '/admin/v1/auth/refresh',
    method: 'post',
    headers: { 'X-Refresh-Token': refreshToken }
  })
}

/** 登出（A-A03）：访问令牌进黑名单、刷新令牌作废；接口幂等。 */
export function logout(refreshToken?: string | null) {
  return request<void>({
    url: '/admin/v1/auth/logout',
    method: 'post',
    headers: refreshToken ? { 'X-Refresh-Token': refreshToken } : undefined
  })
}
