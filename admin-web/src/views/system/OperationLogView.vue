<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearLogs, exportLogs, fetchLogDetail, listLogs } from '@/api/system'
import { formatDateTime } from '@/utils/datetime'
import type { OperationLogItem } from '@/types'

/**
 * 操作日志（04 §5.11，A-G01~A-G04）。
 *
 * 布局对齐 prototype/admin/logs.html：筛选条 + 表格 + 分页；行点击看详情。
 * 操作类型是中文枚举（附录 C），筛选值直接传「登录/登出/新增/修改/删除/禁用启用/导出/其他」。
 */
const ACTION_OPTIONS = ['登录', '登出', '新增', '修改', '删除', '禁用启用', '导出', '其他']

const loading = ref(false)
const exporting = ref(false)
const rows = ref<OperationLogItem[]>([])
const total = ref(0)

const filters = reactive<{ keyword: string; operationType?: string; dateRange: [string, string] | null }>({
  keyword: '',
  operationType: undefined,
  dateRange: null
})

const query = reactive({ page: 1, pageSize: 10 })

const dateParams = computed(() => ({
  startDate: filters.dateRange?.[0],
  endDate: filters.dateRange?.[1]
}))

async function loadLogs() {
  loading.value = true
  try {
    const data = await listLogs({
      keyword: filters.keyword.trim() || undefined,
      operationType: filters.operationType,
      ...dateParams.value,
      page: query.page,
      pageSize: query.pageSize
    })
    rows.value = data.list
    total.value = data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载日志失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  loadLogs()
}

function resetFilters() {
  filters.keyword = ''
  filters.operationType = undefined
  filters.dateRange = null
  search()
}

/* ---------------------------------------------------------------- 导出/清空 */

async function handleExport() {
  exporting.value = true
  try {
    await exportLogs({
      keyword: filters.keyword.trim() || undefined,
      operationType: filters.operationType,
      ...dateParams.value
    })
    ElMessage.success('导出已开始，请查看浏览器下载')
    // 导出本身也是一条审计记录，刷新列表即可看到
    loadLogs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  } finally {
    exporting.value = false
  }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm(
      '清空后所有历史日志将被物理删除且无法恢复，确定继续吗？',
      '清空日志',
      { type: 'warning', confirmButtonText: '确认清空', confirmButtonClass: 'el-button--danger' }
    )
  } catch {
    return
  }
  try {
    await clearLogs()
    ElMessage.success('日志已清空')
    search()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '清空失败')
  }
}

/* ------------------------------------------------------------------ 详情 */

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<OperationLogItem | null>(null)

async function openDetail(row: OperationLogItem) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = row
  try {
    detail.value = await fetchLogDetail(row.id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

/* ---------------------------------------------------------------- 展示映射 */

const ACTION_TAG_TYPE: Record<string, 'success' | 'primary' | 'warning' | 'danger' | 'info'> = {
  登录: 'success',
  登出: 'info',
  新增: 'primary',
  修改: 'warning',
  删除: 'danger',
  禁用启用: 'warning',
  导出: 'info',
  其他: 'info'
}

function actionTagType(action: string) {
  return ACTION_TAG_TYPE[action] ?? 'info'
}

onMounted(loadLogs)
</script>

<template>
  <section class="log-page">
    <div class="panel">
      <div class="panel-header">
        <div class="filters">
          <el-input
            v-model="filters.keyword"
            class="filter-keyword"
            placeholder="搜索用户/操作/目标"
            clearable
            @keyup.enter="search"
            @clear="search"
          />
          <el-select
            v-model="filters.operationType"
            class="filter-select"
            placeholder="全部操作"
            clearable
            @change="search"
          >
            <el-option v-for="action in ACTION_OPTIONS" :key="action" :label="action" :value="action" />
          </el-select>
          <el-date-picker
            v-model="filters.dateRange"
            class="filter-date"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            @change="search"
          />
          <el-button @click="resetFilters">重置</el-button>
        </div>

        <div class="panel-actions">
          <el-button :loading="exporting" @click="handleExport">导出日志</el-button>
          <el-button type="danger" plain @click="handleClear">清空日志</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="id" class="log-table" @row-click="openDetail">
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="log-user">
              <span class="log-avatar">{{ row.username.slice(0, 1).toUpperCase() }}</span>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-tag :type="actionTagType(row.action)" effect="light" size="small">{{ row.action }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="目标" min-width="240">
          <template #default="{ row }">
            <div class="log-target">
              <span class="log-target-name">{{ row.targetName }}</span>
              <span class="log-target-type">{{ row.targetType }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="IP 地址" width="150">
          <template #default="{ row }"><code>{{ row.ip }}</code></template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无匹配的日志记录" />
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
          @current-change="loadLogs"
          @size-change="search"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="日志详情" width="520px">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="1" border size="small">
          <el-descriptions-item label="操作人">
            {{ detail.username }}（ID {{ detail.userId }}）
          </el-descriptions-item>
          <el-descriptions-item label="操作类型">
            <el-tag :type="actionTagType(detail.action)" effect="light" size="small">{{ detail.action }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="对象类型">{{ detail.targetType }}</el-descriptions-item>
          <el-descriptions-item label="操作对象">{{ detail.targetName }}</el-descriptions-item>
          <el-descriptions-item label="IP 地址"><code>{{ detail.ip }}</code></el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ formatDateTime(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        <p v-else class="detail-empty">日志不存在或已被清空。</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.log-page {
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
  width: 200px;
}

.filter-select {
  width: 140px;
}

.filter-date {
  width: 260px;
}

.panel-actions {
  display: flex;
  gap: 8px;
}

.log-user {
  display: flex;
  gap: 10px;
  align-items: center;
}

.log-avatar {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 50%;
}

.log-target {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.log-target-name {
  color: var(--yszx-text);
}

.log-target-type {
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

.detail-empty {
  padding: 24px 0;
  color: var(--yszx-text-tertiary);
  text-align: center;
}

.log-table :deep(.el-table__row) {
  cursor: pointer;
}

@media (max-width: 900px) {
  .filter-keyword,
  .filter-select,
  .filter-date {
    width: 100%;
  }

  .filters {
    width: 100%;
  }
}
</style>
