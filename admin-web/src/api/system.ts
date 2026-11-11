import instance, { ApiError, request } from './http'
import type {
  AdminUserDetail,
  AdminUserItem,
  ApiResponse,
  OperationLogItem,
  PageResult,
  PermissionGroup,
  RoleItem,
  RoleOption,
  SystemSettings
} from '@/types'

/**
 * 系统管理接口封装（04 §5.9 ~ §5.13）。
 *
 * 统一在这里声明路径与入参，页面不直接拼 URL；失败一律抛 ApiError，由页面给出提示。
 */

/* ------------------------------------------------------------------ 角色权限 */

export function listRoles() {
  return request<RoleItem[]>({ url: '/admin/v1/roles', method: 'get' })
}

export interface RoleCreatePayload {
  name: string
  code: string
  description?: string
}

export function createRole(payload: RoleCreatePayload) {
  return request<number>({ url: '/admin/v1/roles', method: 'post', data: payload })
}

export function deleteRole(id: number) {
  return request<void>({ url: `/admin/v1/roles/${id}`, method: 'delete' })
}

export function fetchPermissionTree(roleId?: number) {
  return request<PermissionGroup[]>({
    url: '/admin/v1/permissions/tree',
    method: 'get',
    params: roleId ? { roleId } : undefined
  })
}

export function saveRolePermissions(id: number, permissionIds: number[]) {
  return request<void>({
    url: `/admin/v1/roles/${id}/permissions`,
    method: 'put',
    data: { permissionIds }
  })
}

/* ------------------------------------------------------------------ 用户管理 */

export interface UserQuery {
  keyword?: string
  roleId?: number
  status?: number
  page?: number
  pageSize?: number
}

export function listUsers(params: UserQuery) {
  return request<PageResult<AdminUserItem>>({ url: '/admin/v1/users', method: 'get', params })
}

/** 补充接口：用户表单的角色下拉（角色卡片接口仅超管可访问）。 */
export function listRoleOptions() {
  return request<RoleOption[]>({ url: '/admin/v1/users/role-options', method: 'get' })
}

export interface UserPayload {
  username: string
  mobile?: string
  email?: string
  roleId: number
  /** 新增必填；编辑时留空表示不修改 */
  password?: string
  status?: number
}

export function createUser(payload: UserPayload) {
  return request<number>({ url: '/admin/v1/users', method: 'post', data: payload })
}

export function updateUser(id: number, payload: UserPayload) {
  return request<void>({ url: `/admin/v1/users/${id}`, method: 'put', data: payload })
}

export function batchUpdateUserStatus(ids: number[], status: number) {
  return request<void>({ url: '/admin/v1/users/batch-status', method: 'put', data: { ids, status } })
}

export function deleteUser(id: number) {
  return request<void>({ url: `/admin/v1/users/${id}`, method: 'delete' })
}

export function batchDeleteUsers(ids: number[]) {
  return request<void>({ url: '/admin/v1/users/batch', method: 'delete', data: { ids } })
}

export function resetUserPassword(id: number) {
  return request<{ password: string }>({
    url: `/admin/v1/users/${id}/reset-password`,
    method: 'post'
  })
}

export function fetchUserDetail(id: number) {
  return request<AdminUserDetail>({ url: `/admin/v1/users/${id}/detail`, method: 'get' })
}

/* ------------------------------------------------------------------ 操作日志 */

export interface LogQuery {
  keyword?: string
  operationType?: string
  startDate?: string
  endDate?: string
  page?: number
  pageSize?: number
}

export function listLogs(params: LogQuery) {
  return request<PageResult<OperationLogItem>>({ url: '/admin/v1/logs', method: 'get', params })
}

export function fetchLogDetail(id: number) {
  return request<OperationLogItem>({ url: `/admin/v1/logs/${id}`, method: 'get' })
}

export function clearLogs() {
  return request<void>({ url: '/admin/v1/logs', method: 'delete' })
}

/** 导出 CSV：后端返回带 BOM 的下载流，这里转成浏览器下载。 */
export async function exportLogs(params: Omit<LogQuery, 'page' | 'pageSize'>) {
  const response = await instance.get('/admin/v1/logs/export', {
    params,
    responseType: 'blob'
  })
  const blob = response.data as Blob

  // 鉴权失败等场景后端仍返回 JSON，需要还原成可读错误而不是下载一个坏文件
  if (blob.type.includes('application/json')) {
    const payload = JSON.parse(await blob.text()) as ApiResponse<unknown>
    throw new ApiError(payload.code, payload.message)
  }

  const stamp = new Date().toISOString().slice(0, 10).replace(/-/g, '')
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `操作日志-${stamp}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/* ---------------------------------------------------------------- 系统设置 */

export function fetchSettings() {
  return request<SystemSettings>({ url: '/admin/v1/settings', method: 'get' })
}

export function saveSettings(payload: SystemSettings) {
  return request<void>({ url: '/admin/v1/settings', method: 'put', data: payload })
}

export function resetSettings() {
  return request<void>({ url: '/admin/v1/settings/reset', method: 'post' })
}

/** 图片上传（A-F01）：Logo 裁 256×256，头像 200×200，JPG/PNG ≤2MB。 */
export function uploadImage(file: File, scene: 'logo' | 'avatar') {
  const form = new FormData()
  form.append('file', file)
  form.append('scene', scene)
  return request<{ url: string }>({
    url: '/admin/v1/files/image',
    method: 'post',
    data: form,
    timeout: 30000
  })
}
