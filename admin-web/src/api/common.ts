import { request } from './http'
import type { HealthSnapshot } from '@/types'

/** 环境自检（公开接口）。 */
export function fetchHealth() {
  return request<HealthSnapshot>({ url: '/health', method: 'get' })
}
