<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  batchDeleteUsers,
  batchUpdateUserStatus,
  createUser,
  deleteUser,
  fetchUserDetail,
  listRoleOptions,
  listUsers,
  resetUserPassword,
  updateUser
} from '@/api/system'
import { useAdminStore } from '@/store/user'
import { formatDateTime } from '@/utils/datetime'
import type { AdminUserDetail, AdminUserItem, RoleOption } from '@/types'

/**
 * 用户管理（04 §5.9，A-M01~A-M08）。
 *
 * 布局对齐 prototype/admin/users.html：筛选条 + 批量操作 + 表格 + 分页。
 * 内置账号（admin）的角色与状态在界面上锁定，与后端 30004 保护规则一致。
 */
const store = useAdminStore()

const loading = ref(false)
const rows = ref<AdminUserItem[]>([])
const total = ref(0)
const roleOptions = ref<RoleOption[]>([])
const selectedIds = ref<number[]>([])

const query = reactive<{ keyword: string; roleId?: number; status?: number; page: number; pageSize: number }>({
  keyword: '',
  roleId: undefined,
  status: undefined,
  page: 1,
  pageSize: 10
})

const canAdd = computed(() => store.hasPermission('user:add'))
const canEdit = computed(() => store.hasPermission('user:edit'))
const canDisable = computed(() => store.hasPermission('user:disable'))

/* ------------------------------------------------------------------ 列表 */

async function loadUsers() {
  loading.value = true
  try {
    const data = await listUsers({
      keyword: query.keyword.trim() || undefined,
      roleId: query.roleId,
      status: query.status,
      page: query.page,
      pageSize: query.pageSize
    })
    rows.value = data.list
    total.value = data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载用户列表失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  loadUsers()
}

function resetFilters() {
  query.keyword = ''
  query.roleId = undefined
  query.status = undefined
  search()
}

function onSelectionChange(selection: AdminUserItem[]) {
  selectedIds.value = selection.map((item) => item.id)
}

/** 内置账号不参与批量操作（与后端 30004 保护一致）。 */
function isSelectable(row: AdminUserItem) {
  return !row.builtin
}

/* ------------------------------------------------------------ 新增 / 编辑 */

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const editingBuiltin = ref(false)

const form = reactive({
  username: '',
  mobile: '',
  email: '',
  roleId: undefined as number | undefined,
  password: '',
  status: 1
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度需为 2~50 位', trigger: 'blur' }
  ],
  mobile: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [
    {
      validator: (_rule, value: string, callback) => {
        if (dialogMode.value === 'create' && !value) {
          callback(new Error('请输入初始密码'))
          return
        }
        if (value && (value.length < 6 || value.length > 20)) {
          callback(new Error('密码长度需为 6~20 位'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function resetForm() {
  form.username = ''
  form.mobile = ''
  form.email = ''
  form.roleId = undefined
  form.password = ''
  form.status = 1
  formRef.value?.clearValidate()
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  editingBuiltin.value = false
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: AdminUserItem) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  editingBuiltin.value = row.builtin
  resetForm()
  form.username = row.username
  form.mobile = row.mobile ?? ''
  form.email = row.email ?? ''
  form.roleId = row.roleId
  form.status = row.status
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || form.roleId === undefined) {
    return
  }

  submitting.value = true
  try {
    const payload = {
      username: form.username.trim(),
      mobile: form.mobile.trim() || undefined,
      email: form.email.trim() || undefined,
      roleId: form.roleId,
      password: form.password || undefined,
      status: form.status
    }
    if (dialogMode.value === 'create') {
      await createUser(payload)
      ElMessage.success('用户创建成功')
    } else if (editingId.value !== null) {
      await updateUser(editingId.value, payload)
      ElMessage.success('用户信息已更新')
    }
    dialogVisible.value = false
    loadUsers()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    submitting.value = false
  }
}

/* ------------------------------------------------------------ 状态 / 删除 */

async function toggleStatus(row: AdminUserItem) {
  const next = row.status === 1 ? 0 : 1
  const action = next === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户「${row.username}」吗？`, `${action}确认`, {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await batchUpdateUserStatus([row.id], next)
    ElMessage.success(`${action}成功`)
    loadUsers()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : `${action}失败`)
  }
}

async function batchStatus(status: number) {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择账号')
    return
  }
  const action = status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${action}选中的 ${selectedIds.value.length} 个账号吗？`, `${action}确认`, {
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await batchUpdateUserStatus(selectedIds.value, status)
    ElMessage.success(`${action}成功`)
    loadUsers()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : `${action}失败`)
  }
}

async function removeUser(row: AdminUserItem) {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？此操作不可撤销。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      confirmButtonClass: 'el-button--danger'
    })
  } catch {
    return
  }
  try {
    await deleteUser(row.id)
    ElMessage.success('已删除')
    loadUsers()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function batchRemove() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择账号')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 个账号吗？此操作不可撤销。`, '批量删除', {
      type: 'warning',
      confirmButtonText: '删除',
      confirmButtonClass: 'el-button--danger'
    })
  } catch {
    return
  }
  try {
    await batchDeleteUsers(selectedIds.value)
    ElMessage.success('已删除')
    loadUsers()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

/* ---------------------------------------------------------------- 重置密码 */

const resetVisible = ref(false)
const resetPasswordValue = ref('')
const resetTarget = ref('')

async function resetPassword(row: AdminUserItem) {
  try {
    await ElMessageBox.confirm(
      `确定要重置「${row.username}」的密码吗？将恢复为部署期配置的默认密码。`,
      '重置密码',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    const data = await resetUserPassword(row.id)
    resetTarget.value = row.username
    resetPasswordValue.value = data.password
    resetVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重置失败')
  }
}

async function copyPassword() {
  try {
    await navigator.clipboard.writeText(resetPasswordValue.value)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.warning('复制失败，请手动选择文本复制')
  }
}

/* ------------------------------------------------------------------ 详情 */

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<AdminUserDetail | null>(null)

async function openDetail(row: AdminUserItem) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await fetchUserDetail(row.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

/* ------------------------------------------------------------------ 初始化 */

onMounted(async () => {
  await loadUsers()
  try {
    roleOptions.value = await listRoleOptions()
  } catch {
    // 角色下拉失败不阻断列表展示，新增/编辑时会再次提示
    roleOptions.value = []
  }
})

function statusTagType(status: number) {
  return status === 1 ? 'success' : 'danger'
}
</script>

<template>
  <section class="user-page">
    <div class="panel">
      <div class="panel-header">
        <div class="filters">
          <el-input
            v-model="query.keyword"
            class="filter-keyword"
            placeholder="搜索用户名/手机号"
            clearable
            @keyup.enter="search"
            @clear="search"
          />
          <el-select v-model="query.roleId" class="filter-select" placeholder="全部角色" clearable @change="search">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
          <el-select v-model="query.status" class="filter-select" placeholder="全部状态" clearable @change="search">
            <el-option label="已启用" :value="1" />
            <el-option label="已禁用" :value="0" />
          </el-select>
          <el-button @click="resetFilters">重置</el-button>
        </div>

        <div class="panel-actions">
          <el-button v-if="canEdit" @click="batchStatus(1)">批量启用</el-button>
          <el-button v-if="canEdit" @click="batchStatus(0)">批量禁用</el-button>
          <el-button v-if="canDisable" type="danger" plain @click="batchRemove">批量删除</el-button>
          <el-button v-if="canAdd" type="primary" @click="openCreate">+ 新增用户</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        class="user-table"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="46" :selectable="isSelectable" />
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell" @click="openDetail(row)">
              <span class="user-avatar">{{ row.username.slice(0, 1).toUpperCase() }}</span>
              <div class="user-meta">
                <span class="user-name">{{ row.username }}</span>
                <span v-if="row.builtin" class="user-badge">内置</span>
                <span v-if="row.email" class="user-mail">{{ row.email }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="mobile" label="手机号" width="140">
          <template #default="{ row }">{{ row.mobile || '—' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="130">
          <template #default="{ row }">
            <el-tag :type="row.roleCode === 'super_admin' ? 'danger' : 'info'" effect="light">
              {{ row.roleName || '未分配' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="light">
              {{ row.status === 1 ? '已启用' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="180">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canEdit" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="canEdit"
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              :disabled="row.builtin"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button v-if="canEdit" link type="primary" @click="resetPassword(row)">重置密码</el-button>
            <el-button v-if="canDisable" link type="danger" :disabled="row.builtin" @click="removeUser(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有找到匹配的用户" />
        </template>
      </el-table>

      <div class="pagination">
        <span class="pagination-total">共 {{ total }} 条记录</span>
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="sizes, prev, pager, next, jumper"
          background
          @current-change="loadUsers"
          @size-change="search"
        />
      </div>
    </div>

    <!-- 新增 / 编辑 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增用户' : '编辑用户'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="mobile">
              <el-input v-model="form.mobile" placeholder="选填，可用于登录" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="选填" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="roleId">
              <el-select
                v-model="form.roleId"
                placeholder="请选择角色"
                :disabled="dialogMode === 'edit' && editingBuiltin"
              >
                <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="dialogMode === 'create' ? '初始密码' : '新密码'" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                :placeholder="dialogMode === 'create' ? '6~20 位' : '留空则不修改'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <div class="status-switch">
                <el-switch v-model="form.status" :active-value="1" :inactive-value="0" :disabled="editingBuiltin" />
                <span class="status-text">{{ form.status === 1 ? '已启用' : '已禁用' }}</span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <p v-if="editingBuiltin" class="form-tip">内置账号的角色与状态不可修改。</p>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码结果 -->
    <el-dialog v-model="resetVisible" title="密码已重置" width="420px">
      <p class="reset-tip">「{{ resetTarget }}」的密码已重置为默认密码，请转告本人并提醒尽快修改：</p>
      <div class="reset-value">
        <code>{{ resetPasswordValue }}</code>
        <el-button size="small" @click="copyPassword">复制</el-button>
      </div>
      <template #footer>
        <el-button type="primary" @click="resetVisible = false">我已记录</el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="用户详情" size="480px">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <div class="detail-head">
            <span class="user-avatar large">{{ detail.user.username.slice(0, 1).toUpperCase() }}</span>
            <div>
              <h3>{{ detail.user.username }}</h3>
              <p>
                {{ detail.user.roleName || '未分配角色' }} ·
                {{ detail.user.status === 1 ? '已启用' : '已禁用' }}
              </p>
            </div>
          </div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="手机号">{{ detail.user.mobile || '—' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ detail.user.email || '—' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(detail.user.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="最后登录">{{ formatDateTime(detail.user.lastLoginAt) }}</el-descriptions-item>
          </el-descriptions>

          <h4 class="detail-title">最近操作</h4>
          <el-table v-if="detail.recentLogs.length" :data="detail.recentLogs" size="small">
            <el-table-column prop="action" label="操作" width="90" />
            <el-table-column prop="targetName" label="对象" show-overflow-tooltip />
            <el-table-column label="时间" width="160">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="该账号暂无操作记录" :image-size="80" />
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped>
.user-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel {
  padding: 20px 24px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.panel-header {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.filter-keyword {
  width: 220px;
}

.filter-select {
  width: 150px;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.user-cell {
  display: flex;
  gap: 10px;
  align-items: center;
  cursor: pointer;
}

.user-avatar {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 50%;
}

.user-avatar.large {
  width: 52px;
  height: 52px;
  font-size: 18px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.user-name {
  font-weight: 500;
  color: var(--yszx-text);
}

.user-badge {
  display: inline-block;
  align-self: flex-start;
  padding: 0 6px;
  font-size: 11px;
  color: #b45309;
  background: #fef3c7;
  border-radius: 4px;
}

.user-mail {
  font-size: 12px;
  color: var(--yszx-text-tertiary);
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
}

.pagination-total {
  font-size: 13px;
  color: var(--yszx-text-tertiary);
}

.status-switch {
  display: flex;
  gap: 10px;
  align-items: center;
}

.status-text {
  font-size: 13px;
  color: var(--yszx-text-secondary);
}

.form-tip {
  margin: 0;
  font-size: 12px;
  color: var(--yszx-text-tertiary);
  text-align: right;
}

.reset-tip {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--yszx-text-secondary);
}

.reset-value {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f7f8fa;
  border-radius: 8px;
}

.reset-value code {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.detail-body {
  min-height: 200px;
}

.detail-head {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 18px;
}

.detail-head h3 {
  margin: 0;
  font-size: 17px;
}

.detail-head p {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--yszx-text-tertiary);
}

.detail-title {
  margin: 22px 0 12px;
  font-size: 14px;
  font-weight: 500;
}

@media (max-width: 900px) {
  .filter-keyword,
  .filter-select {
    width: 100%;
  }

  .filters {
    width: 100%;
  }
}
</style>
