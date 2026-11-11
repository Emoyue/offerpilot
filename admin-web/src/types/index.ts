/**
 * 管理端契约类型。
 *
 * 业务 DTO 以 openapi.json 生成物为准（src/types/generated/），
 * 这里只放响应信封与骨架阶段用到的手写类型。
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

export interface HealthSnapshot {
  status: 'UP' | 'DOWN'
  mysql: { status: string; tables?: number; subjects?: number; error?: string }
  redis: { status: string; keys?: number; error?: string }
  minio: { status: string; bucket?: string; endpoint?: string; error?: string }
  serverTimeUtc: string
}

export interface AdminTokenPair {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

/** 登录后返回的管理员档案与权限码（对应 04 文档 A-A01 出参） */
export interface AdminProfile {
  id: number
  username: string
  nickname?: string
  roleCode: string
  roleName: string
  permissions: string[]
}

/** 登录/刷新接口的完整返回：令牌对 + 档案 */
export interface AdminSession extends AdminTokenPair {
  profile: AdminProfile
}

/* ------------------------------------------------------------------ 系统管理 */

/** 角色卡片（A-L01） */
export interface RoleItem {
  id: number
  name: string
  code: string
  description: string
  userCount: number
  permissions: string[]
  permissionLabels: string[]
  /** 内置超管角色：不可删除、不可改权限 */
  builtin: boolean
}

/** 权限树操作点（A-L04） */
export interface PermissionNode {
  id: number
  name: string
  code: string
  checked: boolean
}

/** 权限树分组（9 组 34 项） */
export interface PermissionGroup {
  id: number
  name: string
  code: string
  permissions: PermissionNode[]
}

/** 用户表单的角色下拉选项（补充接口，排除超管角色） */
export interface RoleOption {
  id: number
  name: string
  code: string
}

/** 后台用户列表项（A-M01） */
export interface AdminUserItem {
  id: number
  username: string
  mobile?: string | null
  email?: string | null
  roleId: number
  roleName: string
  roleCode: string
  /** 1 启用 / 0 禁用 */
  status: number
  builtin: boolean
  lastLoginAt?: string | null
  createdAt?: string | null
}

/** 操作日志（A-G01 / A-G02） */
export interface OperationLogItem {
  id: number
  userId: number
  username: string
  action: string
  targetType: string
  targetName: string
  ip: string
  createdAt: string
}

/** 用户详情：基本信息 + 最近操作（A-M08） */
export interface AdminUserDetail {
  user: AdminUserItem
  recentLogs: OperationLogItem[]
}

/** 系统设置（A-T01） */
export interface SystemSettings {
  siteName: string
  siteDescription: string
  logoUrl: string
  defaultAvatar: string
}
