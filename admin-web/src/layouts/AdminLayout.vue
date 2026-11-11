<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminMenu } from '@/config/admin-menu'
import { useAdminStore } from '@/store/user'

/**
 * 管理端主框架（布局壳）。
 *
 * 结构与视觉严格对齐原型 prototype/admin/index.html（CLAUDE.md 阶段六 第 4 条）：
 * - 深色固定侧边栏 240px，折叠后 64px，仅留图标
 * - 四个分组带小号标题，条目为扁平列表，选中项用品牌渐变
 * - 顶栏 64px：折叠按钮 + 页面标题 + 通知铃铛 + 用户下拉（返回前台 / 退出登录）
 * - 内容区 24px 内边距
 * - <768px 侧边栏改为抽屉，带遮罩
 *
 * 本组件只负责"壳"，各页面内容由 RouterView 渲染，当前均为空页面。
 */

const route = useRoute()
const router = useRouter()
const store = useAdminStore()

/** 桌面端：侧边栏是否折叠成图标条（240px → 64px） */
const collapsed = ref(false)
/** 移动端：抽屉是否展开 */
const drawerOpen = ref(false)
const isMobile = ref(false)

/** 前台入口：开发期两个工程分端口，生产由 Nginx 同域分区（D20） */
const portalUrl = import.meta.env.DEV ? 'http://localhost:5173/' : '/'

function syncViewport() {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) {
    drawerOpen.value = false
  }
}

onMounted(() => {
  syncViewport()
  window.addEventListener('resize', syncViewport)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncViewport)
})

function toggleSidebar() {
  if (isMobile.value) {
    drawerOpen.value = !drawerOpen.value
  } else {
    collapsed.value = !collapsed.value
  }
}

const pageTitle = computed(() => (route.meta.title as string | undefined) ?? '管理后台')

function allow(item: { permission?: string; superAdminOnly?: boolean }) {
  if (item.superAdminOnly) {
    return store.isSuperAdmin
  }
  return !item.permission || store.hasPermission(item.permission)
}

/** 按权限过滤后的菜单；空分组整体隐藏 */
const visibleGroups = computed(() =>
  adminMenu
    .map((group) => ({ ...group, items: group.items.filter(allow) }))
    .filter((group) => group.items.length > 0)
)

function isActive(path: string) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

const displayName = computed(() => store.profile?.username || '管理员')
const avatarLetter = computed(() => displayName.value.slice(0, 1).toUpperCase())

async function onUserCommand(command: string | number | object) {
  if (command === 'logout') {
    const confirmed = await ElMessageBox.confirm('退出后需要重新登录，确定退出吗？', '退出登录', {
      confirmButtonText: '退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
      .then(() => true)
      .catch(() => false)
    if (!confirmed) {
      return
    }
    // store 内部保证「后端失败也清本地」，不会把用户卡在登录态
    await store.logout()
    ElMessage.success('已退出登录')
    await router.replace({ name: 'admin-login' })
    return
  }
  if (command === 'portal') {
    window.open(portalUrl, '_blank')
  }
}
</script>

<template>
  <div class="admin-container">
    <aside class="admin-sidebar" :class="{ collapsed, show: drawerOpen }">
      <div class="sidebar-header">
        <router-link to="/dashboard" class="logo">
          <svg class="logo-mark" viewBox="0 0 24 24" aria-hidden="true">
            <defs>
              <linearGradient id="yszxBrandGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#8ea2ff" />
                <stop offset="100%" stop-color="#c9a6ff" />
              </linearGradient>
            </defs>
            <path
              fill="url(#yszxBrandGrad)"
              d="M13.05,3.55C14.79,11.21,14.79,11.21,22.45,12.95C14.79,14.69,14.79,14.69,13.05,22.35C11.31,14.69,11.31,14.69,3.65,12.95C11.31,11.21,11.31,11.21,13.05,3.55Z"
            />
            <path
              fill="url(#yszxBrandGrad)"
              fill-opacity="0.72"
              d="M5.65,1.65C6.41,4.99,6.41,4.99,9.75,5.75C6.41,6.51,6.41,6.51,5.65,9.85C4.89,6.51,4.89,6.51,1.55,5.75C4.89,4.99,4.89,4.99,5.65,1.65Z"
            />
          </svg>
          <span class="logo-text">OfferPilot 后台</span>
        </router-link>
      </div>

      <nav class="sidebar-nav">
        <div v-for="group in visibleGroups" :key="group.title" class="nav-group">
          <div class="nav-group-title">{{ group.title }}</div>
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
            :title="collapsed ? item.title : undefined"
            @click="drawerOpen = false"
          >
            <span class="icon">{{ item.icon }}</span>
            <span class="label">{{ item.title }}</span>
          </router-link>
        </div>
      </nav>
    </aside>

    <div v-if="drawerOpen" class="sidebar-mask" @click="drawerOpen = false"></div>

    <main class="admin-main" :class="{ collapsed }">
      <header class="admin-header">
        <div class="header-left">
          <button class="menu-toggle" type="button" aria-label="切换侧边栏" @click="toggleSidebar">
            ☰
          </button>
          <h1 class="page-title">{{ pageTitle }}</h1>
        </div>

        <div class="header-right">
          <div class="notification-btn" title="暂无新通知">
            <span>🔔</span>
          </div>

          <el-dropdown trigger="click" @command="onUserCommand">
            <div class="admin-user">
              <div class="user-avatar">{{ avatarLetter }}</div>
              <span class="user-name">{{ displayName }}</span>
              <span class="dropdown-arrow">▾</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ store.roleName || '管理员' }}</el-dropdown-item>
                <el-dropdown-item command="portal" divided>🏠 返回前台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>🚪 退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <div class="admin-content">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<style scoped>
.admin-container {
  min-height: 100vh;
}

/* ---------------------------------------------------------------- 侧边栏 */
.admin-sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  flex-direction: column;
  width: 240px;
  background: #1d1e2c;
  color: #fff;
  transition: width 0.3s ease, transform 0.3s ease;
}

.admin-sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #fff;
  text-decoration: none;
  overflow: hidden;
}

.logo-mark {
  flex: none;
  width: 24px;
  height: 24px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  overflow-x: hidden;
  overflow-y: auto;
}

.nav-group {
  margin-bottom: 24px;
}

.nav-group-title {
  padding: 0 12px;
  margin-bottom: 8px;
  font-size: 11px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.4);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 4px;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.nav-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.nav-item .icon {
  flex: none;
  width: 24px;
  font-size: 18px;
  text-align: center;
}

/* 折叠态：只留图标，文字与分组标题整体隐藏 */
.admin-sidebar.collapsed .logo-text,
.admin-sidebar.collapsed .nav-group-title,
.admin-sidebar.collapsed .nav-item .label {
  display: none;
}

.admin-sidebar.collapsed .sidebar-header {
  justify-content: center;
  padding: 20px 0;
}

.admin-sidebar.collapsed .sidebar-nav {
  padding: 16px 8px;
}

.admin-sidebar.collapsed .nav-item {
  justify-content: center;
  padding: 12px 0;
}

/* ---------------------------------------------------------------- 主区域 */
.admin-main {
  min-height: 100vh;
  margin-left: 240px;
  background: #f5f7fa;
  transition: margin-left 0.3s ease;
}

.admin-main.collapsed {
  margin-left: 64px;
}

.admin-header {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 64px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.menu-toggle {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: transparent;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  transition: background 0.2s;
}

.menu-toggle:hover {
  background: #f5f7fa;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.notification-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.notification-btn:hover {
  background: #f5f7fa;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  outline: none;
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.user-name {
  font-size: 14px;
  color: #606266;
}

.dropdown-arrow {
  margin-left: 2px;
  font-size: 10px;
  color: #909399;
}

.admin-content {
  padding: 24px;
}

/* ---------------------------------------------------------------- 响应式 */
.sidebar-mask {
  position: fixed;
  inset: 0;
  z-index: 99;
  background: rgba(0, 0, 0, 0.35);
}

@media (max-width: 768px) {
  .admin-sidebar {
    transform: translateX(-100%);
  }

  .admin-sidebar.show {
    transform: translateX(0);
  }

  .admin-main,
  .admin-main.collapsed {
    margin-left: 0;
  }

  .admin-content {
    padding: 16px;
  }
}
</style>
