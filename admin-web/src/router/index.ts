import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAdminStore } from '@/store/user'

/**
 * 管理端路由。
 *
 * base 为 /admin/，与 Nginx 的 /admin/ 静态分区一致（D20）。
 * 菜单项按权限码动态显隐由 AdminLayout 负责；此处的 meta.permission 供守卫与菜单共用。
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'admin-login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '数据看板', permission: 'dashboard:view' }
      },
      {
        path: 'content/subjects',
        name: 'subjects',
        component: () => import('@/views/PlaceholderPage.vue'),
        props: {
          title: '学科管理',
          description: '学科的增删改查，以及学科下阶段（一对多）的维护与启用停用。',
          endpoint: 'A-S01 ~ A-S11'
        },
        meta: { title: '学科管理', permission: 'subject:view' }
      },
      {
        path: 'content/courses',
        name: 'courses',
        component: () => import('@/views/PlaceholderPage.vue'),
        props: {
          title: 'B站课程库',
          description: 'bvid 唯一的课程录入、多学科关联、上下架与批量操作。',
          endpoint: 'A-C01 ~ A-C07'
        },
        meta: { title: '课程库', permission: 'course:view' }
      },
      {
        path: 'content/questions',
        name: 'questions',
        component: () => import('@/views/PlaceholderPage.vue'),
        props: {
          title: '题库管理',
          description: '题目 CRUD、审核流转（待审核→上架/驳回）、批量导入导出（≤500 行）。',
          endpoint: 'A-Q01 ~ A-Q15'
        },
        meta: { title: '题库管理', permission: 'question:view' }
      },
      {
        path: 'content/interviewbank',
        name: 'interviewbank',
        component: () => import('@/views/PlaceholderPage.vue'),
        props: {
          title: '面试题库',
          description: 'Markdown 解析切题（异步任务）、手工录入、主题维护与上架下架。',
          endpoint: 'A-I01 ~ A-I14'
        },
        meta: { title: '面试题库', permission: 'interviewbank:view' }
      },
      {
        path: 'content/routes',
        name: 'routes',
        component: () => import('@/views/PlaceholderPage.vue'),
        props: {
          title: '学习路线管理',
          description: '路线与选课顺序维护、发布下线；课程顺序即学习顺序。',
          endpoint: 'A-R01 ~ A-R09'
        },
        meta: { title: '学习路线', permission: 'route:view' }
      },
      {
        path: 'students',
        name: 'students',
        component: () => import('@/views/PlaceholderPage.vue'),
        props: {
          title: '学员管理',
          description: '学员列表、详情（学习数据 / 已学课程 / 面试记录）、禁用与导出。',
          endpoint: 'A-U01 ~ A-U08'
        },
        meta: { title: '学员管理', permission: 'student:view' }
      },
      {
        path: 'system/users',
        name: 'system-users',
        component: () => import('@/views/system/UserManageView.vue'),
        meta: { title: '用户管理', permission: 'user:view' }
      },
      {
        path: 'system/roles',
        name: 'system-roles',
        component: () => import('@/views/system/RoleManageView.vue'),
        meta: { title: '角色管理', superAdminOnly: true }
      },
      {
        path: 'system/logs',
        name: 'system-logs',
        component: () => import('@/views/system/OperationLogView.vue'),
        meta: { title: '操作日志', permission: 'system:log_view' }
      },
      {
        path: 'system/settings',
        name: 'system-settings',
        component: () => import('@/views/system/SettingsView.vue'),
        meta: { title: '系统设置', permission: 'system:setting' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory('/admin/'),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · OfferPilot 管理后台` : 'OfferPilot 管理后台'
  const store = useAdminStore()

  if (to.name === 'admin-login') {
    // 已登录时不再展示登录页，直接回到工作区
    return store.isLoggedIn ? { name: 'dashboard' } : true
  }
  if (!store.isLoggedIn) {
    return { name: 'admin-login', query: { redirect: to.fullPath } }
  }
  // 菜单已按权限过滤，这里再拦一次直接输入 URL 的情况；后端仍会独立校验
  const requiredPermission = to.meta.permission as string | undefined
  if (requiredPermission && !store.hasPermission(requiredPermission)) {
    return { name: 'dashboard' }
  }
  if (to.meta.superAdminOnly && !store.isSuperAdmin) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
