import { request } from './http'
import type { HealthSnapshot, Subject } from '@/types'

/** 学科列表（题库筛选与 AI 面试方向共用）。 */
export function listSubjects() {
  return request<Subject[]>({ url: '/portal/v1/subjects', method: 'get' })
}

/** 环境自检：用于首页连通性卡片与本地排障。 */
export function fetchHealth() {
  return request<HealthSnapshot>({ url: '/health', method: 'get' })
}
