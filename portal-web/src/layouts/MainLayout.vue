<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const navItems = [
  { name: 'home', label: '首页', to: '/' },
  { name: 'routes', label: '学习路线', to: '/routes' },
  { name: 'questions', label: '题库', to: '/questions' },
  { name: 'interview', label: 'AI 面试', to: '/interview' }
]

const activeName = computed(() => (route.name as string) ?? 'home')

function handleLogout() {
  userStore.clear()
  router.push({ name: 'home' })
}
</script>

<template>
  <div class="portal-shell">
    <header class="portal-header">
      <div class="portal-header__inner">
        <RouterLink to="/" class="brand">
          <span class="brand__mark">O</span>
          <span class="brand__text">OfferPilot</span>
        </RouterLink>

        <nav class="portal-nav">
          <RouterLink
            v-for="item in navItems"
            :key="item.name"
            :to="item.to"
            class="portal-nav__link"
            :class="{ 'is-active': activeName === item.name }"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <div class="portal-actions">
          <template v-if="userStore.isLoggedIn">
            <RouterLink to="/profile" class="portal-actions__user">
              {{ userStore.nickname || '我的账号' }}
            </RouterLink>
            <el-button link type="primary" @click="handleLogout">退出</el-button>
          </template>
          <template v-else>
            <el-button text @click="router.push('/login')">登录</el-button>
            <el-button type="primary" @click="router.push('/login')">免费注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="portal-main">
      <RouterView />
    </main>

    <footer class="portal-footer">
      <span>OfferPilot · AI 赋能的智能面试训练平台</span>
      <RouterLink to="/admin/" class="portal-footer__admin">管理后台</RouterLink>
    </footer>
  </div>
</template>

<style scoped>
.portal-shell {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  background: var(--yszx-bg);
}

.portal-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid var(--yszx-border);
  backdrop-filter: blur(8px);
}

.portal-header__inner {
  display: flex;
  align-items: center;
  gap: 32px;
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--yszx-text);
  text-decoration: none;
}

.brand__mark {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: var(--yszx-primary);
  color: #fff;
  font-size: 15px;
}

.brand__text {
  font-size: 17px;
}

.portal-nav {
  display: flex;
  gap: 4px;
  flex: 1;
}

.portal-nav__link {
  padding: 8px 14px;
  border-radius: 8px;
  color: var(--yszx-text-secondary);
  font-size: 14px;
  text-decoration: none;
}

.portal-nav__link:hover {
  background: var(--yszx-primary-soft);
  color: var(--yszx-primary);
}

.portal-nav__link.is-active {
  background: var(--yszx-primary-soft);
  color: var(--yszx-primary);
  font-weight: 500;
}

.portal-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.portal-actions__user {
  font-size: 14px;
  color: var(--yszx-text);
  text-decoration: none;
}

.portal-main {
  flex: 1;
  width: 100%;
  max-width: 1180px;
  margin: 0 auto;
  padding: 32px 24px 56px;
}

.portal-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 20px;
  border-top: 1px solid var(--yszx-border);
  color: var(--yszx-text-tertiary);
  font-size: 13px;
}

.portal-footer__admin {
  color: var(--yszx-text-tertiary);
}
</style>
