<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  mobile: '',
  password: ''
})

const rules: FormRules = {
  mobile: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6~20 位', trigger: 'blur' }
  ]
}

async function handleSubmit() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  // auth 模块尚未落地：不伪造登录态，明确提示，避免"看起来能登进去"的假象
  ElMessage.warning('登录接口（auth 模块）尚未落地，当前仅完成前端表单与校验')
  submitting.value = false
}

function backToHome() {
  const redirect = route.query.redirect
  router.push(typeof redirect === 'string' ? redirect : '/')
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <header class="auth-card__head">
        <span class="auth-card__mark">O</span>
        <h1>登录 OfferPilot</h1>
        <p>未注册的手机号将自动创建账号（注册接口待落地）</p>
      </header>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" placeholder="请输入 11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6~20 位" />
        </el-form-item>
        <el-button type="primary" class="auth-card__submit" :loading="submitting" @click="handleSubmit">
          登录
        </el-button>
      </el-form>

      <footer class="auth-card__foot">
        <el-button link @click="backToHome">返回首页</el-button>
        <el-button link disabled>找回密码（待落地）</el-button>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--yszx-bg);
}

.auth-card {
  width: 100%;
  max-width: 400px;
  padding: 32px;
  border: 1px solid var(--yszx-border);
  border-radius: 16px;
  background: #fff;
}

.auth-card__head {
  margin-bottom: 24px;
  text-align: center;
}

.auth-card__mark {
  display: inline-grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: var(--yszx-primary);
  color: #fff;
  font-size: 18px;
}

.auth-card__head h1 {
  margin: 14px 0 6px;
  font-size: 20px;
  color: var(--yszx-text);
}

.auth-card__head p {
  margin: 0;
  font-size: 12.5px;
  color: var(--yszx-text-tertiary);
}

.auth-card__submit {
  width: 100%;
  margin-top: 8px;
}

.auth-card__foot {
  display: flex;
  justify-content: space-between;
  margin-top: 18px;
}
</style>
