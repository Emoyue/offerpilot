import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig
} from 'axios'
import type { AdminSession, ApiResponse, FieldError } from '@/types'
import {
  clearSession,
  getAccessToken,
  getRefreshToken,
  saveSession
} from '@/utils/session'

export const API_BASE = import.meta.env.VITE_API_BASE || '/api'

export class ApiError extends Error {
  readonly code: number
  readonly fieldErrors?: FieldError[]

  constructor(code: number, message: string, fieldErrors?: FieldError[]) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

const instance = axios.create({
  baseURL: API_BASE,
  timeout: 20000
})

/** 带重试标记的请求配置：同一个请求只允许自动刷新一次，防止 401 死循环。 */
interface RetriableConfig extends InternalAxiosRequestConfig {
  _retried?: boolean
}

instance.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 令牌刷新（A-A02）。
 *
 * 用裸 axios 而不是上面的 instance：刷新接口本身返回 401 时不能再触发刷新拦截器。
 * 并发请求共用同一个 Promise，避免多个 401 同时换发导致刷新令牌被提前消费。
 */
let refreshing: Promise<string> | null = null

async function refreshAccessToken(): Promise<string> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new ApiError(10001, '登录已过期，请重新登录')
  }
  const { data } = await axios.post<ApiResponse<AdminSession>>(
    `${API_BASE}/admin/v1/auth/refresh`,
    null,
    { headers: { 'X-Refresh-Token': refreshToken }, timeout: 10000 }
  )
  if (data.code !== 0 || !data.data) {
    throw new ApiError(data.code, data.message || '登录已过期，请重新登录')
  }
  saveSession(data.data)
  return data.data.accessToken
}

function redirectToLogin() {
  clearSession()
  const { pathname, search } = window.location
  if (pathname.endsWith('/login')) {
    return
  }
  const base = import.meta.env.BASE_URL || '/admin/'
  const redirect = encodeURIComponent(pathname + search)
  window.location.href = `${base}login?redirect=${redirect}`
}

instance.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const config = error.config as RetriableConfig | undefined
    const status = error.response?.status
    const isAuthEndpoint =
      config?.url?.includes('/auth/login') || config?.url?.includes('/auth/refresh')

    // 访问令牌过期时先静默续期再重放原请求；刷新也失败才跳登录页
    if (status === 401 && config && !config._retried && !isAuthEndpoint) {
      config._retried = true
      try {
        refreshing = refreshing ?? refreshAccessToken().finally(() => {
          refreshing = null
        })
        const token = await refreshing
        config.headers.Authorization = `Bearer ${token}`
        return instance.request(config)
      } catch {
        redirectToLogin()
      }
    }

    return Promise.reject(error)
  }
)

/**
 * 统一请求入口：成功返回 data，失败抛 ApiError（含业务码与字段级错误）。
 *
 * 业务码非 0 时也走异常分支——调用方只需要处理一种失败形态。
 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  try {
    const { data } = await instance.request<ApiResponse<T>>(config)
    if (data.code !== 0) {
      throw new ApiError(data.code, data.message, data.fieldErrors)
    }
    return data.data
  } catch (error) {
    if (error instanceof ApiError) {
      throw error
    }
    const axiosError = error as AxiosError<ApiResponse<unknown>>
    const payload = axiosError.response?.data
    throw new ApiError(
      payload?.code ?? -1,
      payload?.message ?? '网络异常，请稍后重试',
      payload?.fieldErrors
    )
  }
}

export default instance
