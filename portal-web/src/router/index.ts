import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/home/HomeView.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'questions',
        name: 'questions',
        component: () => import('@/views/questions/QuestionsView.vue'),
        meta: { title: '题库', requiresAuth: true }
      },
      {
        path: 'practice',
        name: 'practice',
        component: () => import('@/views/practice/PracticeView.vue'),
        meta: { title: '刷题练习', requiresAuth: true }
      },
      {
        path: 'wrong-book',
        name: 'wrong-book',
        component: () => import('@/views/wrong-book/WrongBookView.vue'),
        meta: { title: '错题本', requiresAuth: true }
      },
      {
        path: 'routes',
        name: 'routes',
        component: () => import('@/views/routes/RoutesView.vue'),
        meta: { title: '学习路线', requiresAuth: true }
      },
      {
        path: 'interview',
        name: 'interview',
        component: () => import('@/views/interview/InterviewView.vue'),
        meta: { title: 'AI 面试', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/profile/ProfileView.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · OfferPilot` : 'OfferPilot'
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
