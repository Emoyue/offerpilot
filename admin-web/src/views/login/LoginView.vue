<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { ApiError } from '@/api/http'
import { useAdminStore } from '@/store/user'

/**
 * 管理端登录（原型 admin/login.html）。
 *
 * 左侧品牌区 + 右侧表单的两栏结构、深色品牌底与品牌渐变均沿用原型。
 * 登录成功后将令牌与档案交给 store 持久化，再跳回被拦截的目标路由。
 */

const router = useRouter()
const route = useRoute()
const store = useAdminStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const errorMessage = ref('')

const form = reactive({
  account: '',
  password: '',
  remember: true
})

const rules: FormRules = {
  account: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6~20 位', trigger: 'blur' }
  ]
}

/** 只接受站内绝对路径，避免 redirect 参数被用作跳转外链。 */
function resolveRedirect(): string {
  const target = route.query.redirect
  if (typeof target === 'string' && target.startsWith('/') && target !== '/login') {
    return target
  }
  return '/dashboard'
}

async function handleSubmit() {
  if (!formRef.value || submitting.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    await store.login({ account: form.account.trim(), password: form.password }, form.remember)
    ElMessage.success('登录成功')
    await router.replace(resolveRedirect())
  } catch (error) {
    // 30009 账号或密码错误 / 30010 临时锁定 / 30011 已禁用 都由后端给出可读文案
    errorMessage.value = error instanceof ApiError ? error.message : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

function showForgotTip() {
  ElMessage.info('请联系超级管理员重置密码')
}

/** 生产环境由 Nginx 同域分区提供前台（D20），开发期两个工程分端口。 */
function goPortal() {
  window.location.href = import.meta.env.DEV ? 'http://localhost:5173/' : '/'
}
</script>

<template>
  <div class="login-wrapper">
    <section class="brand-section">
      <div class="brand-content">
        <div class="brand-logo">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <defs>
              <linearGradient id="loginBrandGrad" x1="0" y1="0" x2="1" y2="1">
                <stop offset="0%" stop-color="#ffffff" />
                <stop offset="100%" stop-color="#e6dcff" />
              </linearGradient>
            </defs>
            <path
              fill="url(#loginBrandGrad)"
              d="M13.05,3.55C14.79,11.21,14.79,11.21,22.45,12.95C14.79,14.69,14.79,14.69,13.05,22.35C11.31,14.69,11.31,14.69,3.65,12.95C11.31,11.21,11.31,11.21,13.05,3.55Z"
            />
            <path
              fill="url(#loginBrandGrad)"
              fill-opacity="0.72"
              d="M5.65,1.65C6.41,4.99,6.41,4.99,9.75,5.75C6.41,6.51,6.41,6.51,5.65,9.85C4.89,6.51,4.89,6.51,1.55,5.75C4.89,4.99,4.89,4.99,5.65,1.65Z"
            />
          </svg>
        </div>
        <h1 class="brand-title">OfferPilot</h1>
        <p class="brand-subtitle">AI 版 · 管理后台</p>
        <ul class="brand-features">
          <li class="brand-feature">
            <span class="icon">📚</span>
            <span>题库管理 · 面试题库 · 学科管理</span>
          </li>
          <li class="brand-feature">
            <span class="icon">👥</span>
            <span>用户管理 · 角色权限 · 操作日志</span>
          </li>
          <li class="brand-feature">
            <span class="icon">📊</span>
            <span>数据看板 · 系统配置 · AI 模型管理</span>
          </li>
        </ul>
      </div>
    </section>

    <section class="form-section">
      <div class="form-container">
        <header class="form-header">
          <h2>管理员登录</h2>
          <p>请输入管理员账号和密码登录后台</p>
        </header>

        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          show-icon
          :closable="false"
          class="login-error"
        />

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="handleSubmit"
        >
          <el-form-item label="管理员账号" prop="account">
            <el-input
              v-model="form.account"
              :prefix-icon="User"
              placeholder="请输入管理员账号"
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item label="登录密码" prop="password">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              type="password"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
            />
          </el-form-item>

          <div class="form-options">
            <el-checkbox v-model="form.remember">记住登录状态</el-checkbox>
            <el-link type="primary" :underline="false" @click="showForgotTip">忘记密码？</el-link>
          </div>

          <el-button
            type="primary"
            native-type="submit"
            class="login-btn"
            :loading="submitting"
          >
            登 录
          </el-button>
        </el-form>

        <p class="login-hint">
          内置超管账号 <code>admin</code>，初始密码见部署说明（deploy/README.md），上线前必须修改。
        </p>

        <div class="back-link">
          <el-link :underline="false" @click="goPortal">← 返回学员前台</el-link>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-wrapper {
  display: flex;
  min-height: 100vh;
  background: #1d1e2c;
}

/* ---------------------------------------------------------------- 左侧品牌 */
.brand-section {
  position: relative;
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  padding: 60px;
  overflow: hidden;
  background: linear-gradient(135deg, #1d1e2c 0%, #2d2e4c 100%);
}

.brand-section::before {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  content: '';
  background: radial-gradient(circle, rgba(102, 126, 234, 0.08) 0%, transparent 60%);
  animation: brand-pulse 10s ease-in-out infinite;
}

@keyframes brand-pulse {
  0%,
  100% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.15);
  }
}

.brand-content {
  position: relative;
  z-index: 1;
  color: #fff;
  text-align: center;
}

.brand-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  margin: 0 auto 24px;
  border-radius: 24px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.3);
}

.brand-logo svg {
  width: 52px;
  height: 52px;
}

.brand-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
}

.brand-subtitle {
  margin: 0 0 40px;
  font-size: 14px;
  opacity: 0.7;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 280px;
  margin: 0 auto;
  padding: 0;
  list-style: none;
  text-align: left;
}

.brand-feature {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  opacity: 0.8;
}

.brand-feature .icon {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  font-size: 16px;
}

/* ---------------------------------------------------------------- 右侧表单 */
.form-section {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 460px;
  min-width: 460px;
  padding: 40px;
  background: #fff;
}

.form-container {
  width: 100%;
  max-width: 360px;
}

.form-header {
  margin-bottom: 28px;
}

.form-header h2 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.form-header p {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.login-error {
  margin-bottom: 16px;
  border-radius: 8px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
}

.login-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  font-size: 16px;
  font-weight: 600;
}

.login-btn:hover,
.login-btn:focus {
  background: linear-gradient(135deg, #7589f0, #8557b4);
}

.login-hint {
  margin: 18px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: #a8abb2;
}

.login-hint code {
  padding: 1px 5px;
  border-radius: 4px;
  background: #f2f3f5;
  color: #606266;
}

.back-link {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
}

/* ---------------------------------------------------------------- 响应式 */
@media (max-width: 900px) {
  .brand-section {
    display: none;
  }

  .form-section {
    width: 100%;
    min-width: auto;
    min-height: 100vh;
    padding: 24px;
  }
}
</style>
