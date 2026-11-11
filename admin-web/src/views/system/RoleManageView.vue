<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createRole,
  deleteRole,
  fetchPermissionTree,
  listRoles,
  saveRolePermissions
} from '@/api/system'
import type { PermissionGroup, RoleItem } from '@/types'

/**
 * 角色管理（04 §5.10，A-L01~A-L05，仅超管）。
 *
 * 布局对齐 prototype/admin/roles.html：角色卡片网格 + 权限编辑弹窗。
 * 权限分组与勾选项完全来自后端 9 组 34 项（不硬编码，避免与 permission 表漂移）。
 */
const loading = ref(false)
const roles = ref<RoleItem[]>([])

const total = computed(() => roles.value.length)

async function loadRoles() {
  loading.value = true
  try {
    roles.value = await listRoles()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载角色失败')
  } finally {
    loading.value = false
  }
}

/* ---------------------------------------------------------------- 新增角色 */

const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive({ name: '', code: '', description: '' })

const createRules: FormRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 50, message: '不能超过 50 字', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]{1,49}$/,
      message: '以小写字母开头，仅含小写字母、数字与下划线（2~50 位）',
      trigger: 'blur'
    }
  ]
}

function openCreate() {
  createForm.name = ''
  createForm.code = ''
  createForm.description = ''
  createFormRef.value?.clearValidate()
  createVisible.value = true
}

async function submitCreate() {
  if (!createFormRef.value) {
    return
  }
  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  creating.value = true
  try {
    await createRole({
      name: createForm.name.trim(),
      code: createForm.code.trim(),
      description: createForm.description.trim() || undefined
    })
    ElMessage.success('角色创建成功')
    createVisible.value = false
    loadRoles()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  } finally {
    creating.value = false
  }
}

/* ---------------------------------------------------------------- 删除角色 */

async function removeRole(role: RoleItem) {
  try {
    await ElMessageBox.confirm(`确定要删除角色「${role.name}」吗？此操作不可撤销。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      confirmButtonClass: 'el-button--danger'
    })
  } catch {
    return
  }
  try {
    await deleteRole(role.id)
    ElMessage.success('角色已删除')
    loadRoles()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

/* ---------------------------------------------------------------- 权限编辑 */

const permVisible = ref(false)
const permLoading = ref(false)
const permSaving = ref(false)
const currentRole = ref<RoleItem | null>(null)
const groups = ref<PermissionGroup[]>([])
const checkedIds = ref<number[]>([])

async function openPermissions(role: RoleItem) {
  currentRole.value = role
  permVisible.value = true
  permLoading.value = true
  checkedIds.value = []
  try {
    groups.value = await fetchPermissionTree(role.id)
    checkedIds.value = groups.value.flatMap((group) =>
      group.permissions.filter((node) => node.checked).map((node) => node.id)
    )
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载权限树失败')
  } finally {
    permLoading.value = false
  }
}

function groupNodeIds(group: PermissionGroup) {
  return group.permissions.map((node) => node.id)
}

function isGroupChecked(group: PermissionGroup) {
  const ids = groupNodeIds(group)
  return ids.length > 0 && ids.every((id) => checkedIds.value.includes(id))
}

function isGroupIndeterminate(group: PermissionGroup) {
  const ids = groupNodeIds(group)
  const hit = ids.filter((id) => checkedIds.value.includes(id)).length
  return hit > 0 && hit < ids.length
}

/** 组级全选：el-checkbox 的 change 值类型为 string | number | boolean。 */
function onGroupToggle(group: PermissionGroup, value: string | number | boolean) {
  toggleGroup(group, value === true)
}

function toggleGroup(group: PermissionGroup, checked: boolean) {
  const ids = groupNodeIds(group)
  if (checked) {
    checkedIds.value = Array.from(new Set([...checkedIds.value, ...ids]))
  } else {
    checkedIds.value = checkedIds.value.filter((id) => !ids.includes(id))
  }
}

async function savePermissions() {
  if (!currentRole.value) {
    return
  }
  permSaving.value = true
  try {
    await saveRolePermissions(currentRole.value.id, checkedIds.value)
    ElMessage.success('权限已保存，相关账号将在下次登录后生效')
    permVisible.value = false
    loadRoles()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    permSaving.value = false
  }
}

onMounted(loadRoles)
</script>

<template>
  <section class="role-page">
    <div class="page-head">
      <h2>共 <strong>{{ total }}</strong> 个角色</h2>
      <el-button type="primary" @click="openCreate">+ 新增角色</el-button>
    </div>

    <div v-loading="loading" class="roles-grid">
      <article v-for="role in roles" :key="role.id" class="role-card">
        <header class="role-head">
          <div class="role-title">
            <h3>{{ role.name }}</h3>
            <span class="role-code">{{ role.code }}</span>
          </div>
          <div class="role-count"><strong>{{ role.userCount }}</strong> 用户</div>
        </header>

        <p class="role-desc">{{ role.description || '暂无描述' }}</p>

        <div class="role-perms">
          <template v-if="role.builtin">
            <span class="perm-tag all">全部权限</span>
          </template>
          <template v-else-if="role.permissionLabels.length === 0">
            <span class="perm-empty">暂无权限</span>
          </template>
          <template v-else>
            <!-- 与原型一致：全部展示、自然换行，不做截断，卡片由网格拉齐高度 -->
            <span v-for="label in role.permissionLabels" :key="label" class="perm-tag">
              {{ label }}
            </span>
          </template>
        </div>

        <footer class="role-actions">
          <el-button size="small" :disabled="role.builtin" @click="openPermissions(role)">
            {{ role.builtin ? '权限固定' : '编辑权限' }}
          </el-button>
          <el-button size="small" type="danger" plain :disabled="role.builtin" @click="removeRole(role)">
            删除
          </el-button>
        </footer>
      </article>

      <el-empty v-if="!loading && roles.length === 0" description="暂无角色，点击右上角新增角色" />
    </div>

    <!-- 新增角色 -->
    <el-dialog v-model="createVisible" title="新增角色" width="500px" destroy-on-close>
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="88px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="createForm.name" placeholder="如：内容编辑" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="createForm.code" placeholder="如：content_editor" />
        </el-form-item>
        <el-form-item label="角色描述" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="submitCreate">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限编辑 -->
    <el-dialog v-model="permVisible" :title="`编辑权限 · ${currentRole?.name ?? ''}`" width="720px">
      <div v-loading="permLoading" class="perm-body">
        <p class="perm-tip">
          勾选该角色可执行的操作；保存后按全量覆盖写入，相关账号下次登录生效。
        </p>
        <div v-for="group in groups" :key="group.id" class="perm-group">
          <label class="perm-group-title">
            <el-checkbox
              :model-value="isGroupChecked(group)"
              :indeterminate="isGroupIndeterminate(group)"
              @change="onGroupToggle(group, $event)"
            >
              {{ group.name }}
            </el-checkbox>
          </label>
          <el-checkbox-group v-model="checkedIds" class="perm-items">
            <el-checkbox v-for="node in group.permissions" :key="node.id" :value="node.id">
              {{ node.name }}
            </el-checkbox>
          </el-checkbox-group>
        </div>
        <el-empty v-if="!permLoading && groups.length === 0" description="暂无权限数据" :image-size="80" />
      </div>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSaving" @click="savePermissions">保存权限</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.role-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-head h2 {
  margin: 0;
  font-size: 15px;
  font-weight: 400;
  color: var(--yszx-text-secondary);
}

.page-head strong {
  font-size: 18px;
  color: var(--yszx-text);
}

.roles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 18px;
  min-height: 180px;
}

.role-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 22px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.role-card:hover {
  box-shadow: 0 8px 24px rgba(99, 102, 241, 0.12);
  transform: translateY(-2px);
}

.role-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.role-title h3 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
}

.role-code {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--yszx-text-tertiary);
}

.role-count {
  flex: none;
  font-size: 12px;
  color: var(--yszx-text-tertiary);
}

.role-count strong {
  margin-right: 2px;
  font-size: 18px;
  color: var(--yszx-primary);
}

.role-desc {
  min-height: 40px;
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--yszx-text-secondary);
}

.role-perms {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 52px;
  align-content: flex-start;
}

.perm-tag {
  padding: 2px 8px;
  font-size: 12px;
  color: #4b5563;
  background: #f3f4f6;
  border-radius: 999px;
}

.perm-tag.all {
  color: #6d28d9;
  background: #ede9fe;
}

.perm-empty {
  font-size: 12px;
  color: var(--yszx-text-tertiary);
}

.role-actions {
  display: flex;
  gap: 10px;
  /* 网格会把同一行卡片拉到等高，这里把操作区压到卡片底部 */
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--yszx-border);
}

.perm-body {
  max-height: 62vh;
  overflow-y: auto;
  padding-right: 4px;
}

.perm-tip {
  margin: 0 0 16px;
  font-size: 12px;
  color: var(--yszx-text-tertiary);
}

.perm-group {
  padding: 12px 16px;
  margin-bottom: 12px;
  background: #fafafa;
  border-radius: 10px;
}

.perm-group-title {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
}

.perm-items {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 18px;
}
</style>
