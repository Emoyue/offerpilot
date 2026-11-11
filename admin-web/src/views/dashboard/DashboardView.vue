<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchHealth } from '@/api/common'
import type { HealthSnapshot } from '@/types'

/**
 * 数据看板。
 *
 * 布局骨架按原型 prototype/admin/index.html：统计卡片区 + 内容面板。
 * 目前 stats 聚合接口未实现，统计数值统一显示「—」，只保留真实可用的环境连通性面板。
 */
const health = ref<HealthSnapshot | null>(null)
const error = ref('')
const loading = ref(true)

const statCards = [
  { label: '学科数量', icon: '🎓', tone: 'purple', value: '—' },
  { label: '题目数量', icon: '📚', tone: 'green', value: '—' },
  { label: '注册用户', icon: '👥', tone: 'blue', value: '—' },
  { label: 'AI 面试次数', icon: '🤖', tone: 'orange', value: '—' }
]

onMounted(async () => {
  try {
    health.value = await fetchHealth()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '自检失败'
  } finally {
    loading.value = false
  }
})

function tagType(status?: string) {
  return status === 'UP' ? 'success' : 'danger'
}
</script>

<template>
  <section class="dashboard">
    <div class="stats-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <div class="stat-icon" :class="card.tone">{{ card.icon }}</div>
        <div class="stat-info">
          <h3>{{ card.value }}</h3>
          <p>{{ card.label }}</p>
          <span class="stat-hint">待 stats 接口接入</span>
        </div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h3 class="panel-title">环境连通性</h3>
        <el-tag v-if="health" :type="tagType(health.status)" size="small">{{ health.status }}</el-tag>
      </div>

      <div class="panel-body">
        <el-skeleton v-if="loading" :rows="3" animated />
        <el-alert v-else-if="error" :title="error" type="error" :closable="false" show-icon />
        <template v-else-if="health">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="MySQL">
              <el-tag :type="tagType(health.mysql.status)" size="small">
                {{ health.mysql.status }}
              </el-tag>
              <span class="meta">{{ health.mysql.tables ?? 0 }} 表 / {{ health.mysql.subjects ?? 0 }} 学科</span>
            </el-descriptions-item>
            <el-descriptions-item label="Redis">
              <el-tag :type="tagType(health.redis.status)" size="small">
                {{ health.redis.status }}
              </el-tag>
              <span class="meta">{{ health.redis.keys ?? 0 }} 键</span>
            </el-descriptions-item>
            <el-descriptions-item label="MinIO">
              <el-tag :type="tagType(health.minio.status)" size="small">
                {{ health.minio.status }}
              </el-tag>
              <span class="meta">桶 {{ health.minio.bucket }}</span>
            </el-descriptions-item>
          </el-descriptions>
          <p class="panel-time">服务端 UTC：{{ health.serverTimeUtc }}</p>
        </template>
      </div>
    </div>

    <div class="panel">
      <div class="panel-header">
        <h3 class="panel-title">趋势与分布</h3>
      </div>
      <div class="panel-body">
        <el-empty description="ECharts 已就位，待 stats 模块提供聚合接口后接入（缓存 5 分钟）" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* -------------------------------------------------- 统计卡片（对齐原型） */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 56px;
  height: 56px;
  border-radius: 14px;
  font-size: 24px;
}

.stat-icon.purple {
  background: #eef0ff;
}

.stat-icon.green {
  background: #e8f8ef;
}

.stat-icon.blue {
  background: #e8f2ff;
}

.stat-icon.orange {
  background: #fff3e6;
}

.stat-info h3 {
  margin: 0 0 4px;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
  color: #303133;
}

.stat-info p {
  margin: 0 0 4px;
  font-size: 13px;
  color: #909399;
}

.stat-hint {
  font-size: 12px;
  color: #c0c4cc;
}

/* -------------------------------------------------- 内容面板 */
.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.panel-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.panel-body {
  padding: 24px;
}

.meta {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}

.panel-time {
  margin: 12px 0 0;
  font-size: 12px;
  color: #c0c4cc;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
