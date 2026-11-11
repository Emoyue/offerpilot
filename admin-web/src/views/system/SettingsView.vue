<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
  type UploadRequestOptions
} from 'element-plus'
import { fetchSettings, resetSettings, saveSettings, uploadImage } from '@/api/system'
import { useAdminStore } from '@/store/user'
import type { SystemSettings } from '@/types'

/**
 * 系统设置（04 §5.12 / §5.13，A-T01~A-T03、A-F01）。
 *
 * 布局对齐 prototype/admin/settings.html：单个「基本信息设置」面板。
 * 图片先上传拿直链，点「保存设置」才落库，避免上传即生效产生未保存的脏数据。
 * 阈值类参数是编译期常量（D13），不在本页暴露。
 */
const store = useAdminStore()
const canSave = computed(() => store.hasPermission('system:setting'))

const loading = ref(false)
const saving = ref(false)
const uploadingLogo = ref(false)
const uploadingAvatar = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<SystemSettings>({
  siteName: '',
  siteDescription: '',
  logoUrl: '',
  defaultAvatar: ''
})

const rules: FormRules = {
  siteName: [
    { required: true, message: '请输入平台名称', trigger: 'blur' },
    { max: 50, message: '不能超过 50 字', trigger: 'blur' }
  ],
  siteDescription: [{ max: 255, message: '不能超过 255 字', trigger: 'blur' }]
}

async function loadSettings() {
  loading.value = true
  try {
    const data = await fetchSettings()
    form.siteName = data.siteName
    form.siteDescription = data.siteDescription
    form.logoUrl = data.logoUrl
    form.defaultAvatar = data.defaultAvatar
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载系统设置失败')
  } finally {
    loading.value = false
  }
}

function validateImage(file: File) {
  const allowed = ['image/png', 'image/jpeg'].includes(file.type)
  if (!allowed) {
    ElMessage.warning('仅支持 JPG / PNG 图片')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 2MB')
    return false
  }
  return true
}

async function handleLogoUpload(options: UploadRequestOptions) {
  if (!validateImage(options.file)) {
    return
  }
  uploadingLogo.value = true
  try {
    const data = await uploadImage(options.file, 'logo')
    form.logoUrl = data.url
    ElMessage.success('Logo 已上传，记得点击「保存设置」')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败')
  } finally {
    uploadingLogo.value = false
  }
}

async function handleAvatarUpload(options: UploadRequestOptions) {
  if (!validateImage(options.file)) {
    return
  }
  uploadingAvatar.value = true
  try {
    const data = await uploadImage(options.file, 'avatar')
    form.defaultAvatar = data.url
    ElMessage.success('默认头像已上传，记得点击「保存设置」')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败')
  } finally {
    uploadingAvatar.value = false
  }
}

function clearLogo() {
  form.logoUrl = ''
}

function clearAvatar() {
  form.defaultAvatar = ''
}

async function handleSave() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    await saveSettings({
      siteName: form.siteName.trim(),
      siteDescription: form.siteDescription.trim(),
      logoUrl: form.logoUrl,
      defaultAvatar: form.defaultAvatar
    })
    ElMessage.success('设置已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleReset() {
  try {
    await ElMessageBox.confirm('确定要恢复默认设置吗？当前配置会被覆盖。', '恢复默认', {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await resetSettings()
    await loadSettings()
    ElMessage.success('已恢复默认设置')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '恢复失败')
  }
}

onMounted(loadSettings)
</script>

<template>
  <section class="settings-page">
    <div v-loading="loading" class="panel">
      <div class="panel-body">
        <h3 class="panel-title">基本信息设置</h3>
        <p class="panel-desc">配置平台的基本信息与展示内容</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="settings-form">
          <el-form-item prop="siteName">
            <template #label>
              <span class="form-label">平台名称</span>
            </template>
            <el-input v-model="form.siteName" placeholder="请输入平台名称" :disabled="!canSave" />
            <p class="form-hint">显示在浏览器标题与页面 Logo 旁</p>
          </el-form-item>

          <el-form-item prop="siteDescription">
            <template #label>
              <span class="form-label">平台描述</span>
            </template>
            <el-input
              v-model="form.siteDescription"
              type="textarea"
              :rows="3"
              placeholder="请输入平台描述"
              :disabled="!canSave"
            />
          </el-form-item>

          <el-form-item>
            <template #label>
              <span class="form-label">
                Logo
                <span class="label-desc">建议 256×256，JPG/PNG ≤2MB</span>
              </span>
            </template>
            <div class="upload-row">
              <div class="upload-preview">
                <img v-if="form.logoUrl" :src="form.logoUrl" alt="平台 Logo" />
                <span v-else class="preview-placeholder">✦</span>
              </div>
              <div class="upload-actions">
                <el-upload
                  :show-file-list="false"
                  :http-request="handleLogoUpload"
                  accept="image/png,image/jpeg"
                  :disabled="!canSave || uploadingLogo"
                >
                  <el-button :loading="uploadingLogo" :disabled="!canSave">选择文件</el-button>
                </el-upload>
                <el-button v-if="form.logoUrl" link type="danger" @click="clearLogo">移除</el-button>
              </div>
            </div>
          </el-form-item>

          <el-form-item>
            <template #label>
              <span class="form-label">
                默认头像
                <span class="label-desc">用户未设置头像时显示</span>
              </span>
            </template>
            <div class="upload-row">
              <div class="upload-preview round">
                <img v-if="form.defaultAvatar" :src="form.defaultAvatar" alt="默认头像" />
                <span v-else class="preview-placeholder">👤</span>
              </div>
              <div class="upload-actions">
                <el-upload
                  :show-file-list="false"
                  :http-request="handleAvatarUpload"
                  accept="image/png,image/jpeg"
                  :disabled="!canSave || uploadingAvatar"
                >
                  <el-button :loading="uploadingAvatar" :disabled="!canSave">选择文件</el-button>
                </el-upload>
                <el-button v-if="form.defaultAvatar" link type="danger" @click="clearAvatar">移除</el-button>
              </div>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <div class="panel-footer">
        <el-button :disabled="!canSave" @click="handleReset">重置</el-button>
        <el-button type="primary" :loading="saving" :disabled="!canSave" @click="handleSave">
          保存设置
        </el-button>
      </div>
    </div>

    <p v-if="!canSave" class="readonly-tip">当前角色仅有查看权限，修改需 system:setting 权限。</p>
  </section>
</template>

<style scoped>
.settings-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.panel-body {
  padding: 28px 32px 8px;
}

.panel-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.panel-desc {
  margin: 6px 0 24px;
  font-size: 13px;
  color: var(--yszx-text-tertiary);
}

.settings-form {
  max-width: 620px;
}

.form-label {
  font-weight: 500;
  color: var(--yszx-text);
}

.label-desc {
  margin-left: 8px;
  font-size: 12px;
  font-weight: 400;
  color: var(--yszx-text-tertiary);
}

.form-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--yszx-text-tertiary);
}

.upload-row {
  display: flex;
  gap: 18px;
  align-items: center;
}

.upload-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 84px;
  height: 84px;
  overflow: hidden;
  background: linear-gradient(135deg, #eef2ff, #f5f3ff);
  border: 1px solid var(--yszx-border);
  border-radius: 12px;
}

.upload-preview.round {
  border-radius: 50%;
}

.upload-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-placeholder {
  font-size: 26px;
  color: var(--yszx-primary);
}

.upload-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.panel-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 18px 32px;
  border-top: 1px solid var(--yszx-border);
}

.readonly-tip {
  margin: 0;
  font-size: 12px;
  color: var(--yszx-text-tertiary);
  text-align: right;
}

@media (max-width: 768px) {
  .panel-body {
    padding: 20px;
  }

  .panel-footer {
    padding: 16px 20px;
  }
}
</style>
