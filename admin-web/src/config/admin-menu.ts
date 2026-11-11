/**
 * 管理端侧边栏菜单定义。
 *
 * 分组与条目严格对齐原型 prototype/admin/index.html 的侧边栏结构：
 * 「概览 / 内容管理 / 学员管理 / 系统管理」四组，条目为扁平列表（不折叠成子菜单）。
 * 图标沿用原型中的 emoji，保证与已确认的原型视觉一致（CLAUDE.md 阶段六 第 4 条）。
 *
 * permission / superAdminOnly 只用于菜单显隐；真正的权限校验在后端（阶段六 第 8 条）。
 */

export interface AdminMenuItem {
  /** 菜单文案，与原型一致 */
  title: string
  /** 路由路径，需与 router/index.ts 中的 path 完全对应 */
  path: string
  /** 原型使用的 emoji 图标 */
  icon: string
  /** 所需权限码，缺省表示不校验 */
  permission?: string
  /** 仅超级管理员可见（角色管理不设权限码，见 04 文档 A-L01） */
  superAdminOnly?: boolean
}

export interface AdminMenuGroup {
  /** 分组标题，原型中为小号大写灰字 */
  title: string
  items: AdminMenuItem[]
}

export const adminMenu: AdminMenuGroup[] = [
  {
    title: '概览',
    items: [{ title: '数据看板', path: '/dashboard', icon: '📊', permission: 'dashboard:view' }]
  },
  {
    title: '内容管理',
    items: [
      { title: '学科管理', path: '/content/subjects', icon: '🎓', permission: 'subject:view' },
      { title: 'B站课程库', path: '/content/courses', icon: '🎬', permission: 'course:view' },
      { title: '题库管理', path: '/content/questions', icon: '📚', permission: 'question:view' },
      {
        title: '面试题库管理',
        path: '/content/interviewbank',
        icon: '🗂️',
        permission: 'interviewbank:view'
      },
      { title: '学习路线', path: '/content/routes', icon: '🗺️', permission: 'route:view' }
    ]
  },
  {
    title: '学员管理',
    items: [{ title: '学员管理', path: '/students', icon: '🎓', permission: 'student:view' }]
  },
  {
    title: '系统管理',
    items: [
      { title: '用户管理', path: '/system/users', icon: '👥', permission: 'user:view' },
      { title: '角色管理', path: '/system/roles', icon: '🔐', superAdminOnly: true },
      { title: '操作日志', path: '/system/logs', icon: '📝', permission: 'system:log_view' },
      { title: '系统设置', path: '/system/settings', icon: '⚙️', permission: 'system:setting' }
    ]
  }
]
