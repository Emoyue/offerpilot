/**
 * 与后端共享的契约类型。
 *
 * 说明：D21 要求两端类型由后端 openapi.json 生成，生成物放 src/types/generated/。
 * 本文件只放"生成物之外的手写补充"（如统一响应信封、分页信封），业务 DTO 不在此手写。
 */

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  fieldErrors?: FieldError[]
}

export interface FieldError {
  field: string
  reason: string
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

/** 学员端：学科（对应后端 SubjectVO） */
export interface Subject {
  id: number
  name: string
  icon: string
  description: string
  themeColor: string
  sortOrder: number
}

/** 环境自检结果（对应后端 /api/health） */
export interface HealthSnapshot {
  status: 'UP' | 'DOWN'
  mysql: { status: string; tables?: number; subjects?: number; error?: string }
  redis: { status: string; keys?: number; error?: string }
  minio: { status: string; bucket?: string; endpoint?: string; error?: string }
  serverTimeUtc: string
}

/** 令牌对（登录成功后返回） */
export interface TokenPair {
  accessToken: string
  refreshToken: string
  expiresIn: number
}
