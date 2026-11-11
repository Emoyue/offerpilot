import { request } from './http'
import type { TokenPair } from '@/types'

export interface LoginPayload {
  mobile: string
  password: string
}

export interface RegisterPayload {
  mobile: string
  code: string
  password: string
}

/**
 * 学员端认证接口（P-A01~P-A08）。
 * 后端 auth 模块尚未落地，此处先按 04 文档契约占位，联调时不需要改前端签名。
 */
export function login(payload: LoginPayload) {
  return request<TokenPair>({ url: '/portal/v1/auth/login', method: 'post', data: payload })
}

export function register(payload: RegisterPayload) {
  return request<void>({ url: '/portal/v1/auth/register', method: 'post', data: payload })
}

export function logout() {
  return request<void>({ url: '/portal/v1/auth/logout', method: 'post' })
}
