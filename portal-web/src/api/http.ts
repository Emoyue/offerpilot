import axios, { type AxiosRequestConfig, type AxiosError } from 'axios'
import type { ApiResponse, FieldError } from '@/types'

export const TOKEN_KEY = 'yszx_portal_token'

/** 业务异常：携带 5 位业务码与字段级错误，便于表单回显。 */
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
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 20000
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      const redirect = encodeURIComponent(window.location.pathname + window.location.search)
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = `/login?redirect=${redirect}`
      }
    }
    return Promise.reject(error)
  }
)

/**
 * 统一请求入口：拆掉响应信封，返回 data 本体；业务码非 0 抛 ApiError。
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
