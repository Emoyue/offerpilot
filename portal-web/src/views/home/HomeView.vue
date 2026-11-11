<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchHealth, listSubjects } from '@/api/common'
import type { HealthSnapshot, Subject } from '@/types'

const router = useRouter()

const health = ref<HealthSnapshot | null>(null)
const healthError = ref('')
const subjects = ref<Subject[]>([])
const subjectsError = ref('')
const loading = ref(true)

const featureCards = [
  { icon: '📚', title: '海量题库', desc: '按学科与阶段筛选，覆盖单选、多选、判断与简答', to: '/questions' },
  { icon: '🎯', title: '刷题练习', desc: '顺序或随机，客观题即时判分，简答由 AI 批改', to: '/practice' },
  { icon: '🧠', title: 'AI 模拟面试', desc: '逐题作答与实时反馈，结束后生成能力雷达报告', to: '/interview' },
  { icon: '🗺️', title: '学习路线', desc: '按课程顺序推进，勾选完成即计入已学', to: '/routes' }
]

onMounted(async () => {
  try {
    health.value = await fetchHealth()
  } catch (error) {
    healthError.value = error instanceof Error ? error.message : '自检失败'
  }

  try {
    subjects.value = await listSubjects()
  } catch (error) {
    subjectsError.value = error instanceof Error ? error.message : '学科加载失败'
  } finally {
    loading.value = false
  }
})

function statusTagType(status?: string) {
  return status === 'UP' ? 'success' : 'danger'
}

function goInterview() {
  if (health.value?.status !== 'UP') {
    ElMessage.warning('后端环境未就绪，请先检查 /api/health')
    return
  }
  router.push('/interview')
}
</script>

<template>
  <div class="home">
    <section class="hero">
      <div class="hero__content">
        <p class="hero__eyebrow">AI 赋能的智能面试训练平台</p>
        <h1 class="hero__title">把「不会」练成「讲得清」</h1>
        <p class="hero__desc">
          八个 AI 方向，从刷题到错题，从模拟面试到能力报告——一条闭环走完，再回到最薄的那块补强。
        </p>
        <div class="hero__actions">
          <el-button type="primary" size="large" @click="router.push('/questions')">海量题库</el-button>
          <el-button size="large" @click="goInterview">开始 AI 面试</el-button>
        </div>
      </div>

      <div class="hero__panel">
        <h2 class="panel__title">环境连通性</h2>
        <p class="panel__hint">数据来自 <code>GET /api/health</code></p>

        <el-skeleton v-if="loading" :rows="3" animated />

        <el-alert v-else-if="healthError" :title="healthError" type="error" :closable="false" show-icon />

        <template v-else-if="health">
          <div class="status-row">
            <span class="status-label">MySQL</span>
            <el-tag :type="statusTagType(health.mysql.status)" size="small">
              {{ health.mysql.status }}
            </el-tag>
            <span class="status-meta">23 表模板 · {{ health.mysql.subjects ?? 0 }} 学科</span>
          </div>
          <div class="status-row">
            <span class="status-label">Redis</span>
            <el-tag :type="statusTagType(health.redis.status)" size="small">
              {{ health.redis.status }}
            </el-tag>
            <span class="status-meta">当前键数 {{ health.redis.keys ?? 0 }}</span>
          </div>
          <div class="status-row">
            <span class="status-label">MinIO</span>
            <el-tag :type="statusTagType(health.minio.status)" size="small">
              {{ health.minio.status }}
            </el-tag>
            <span class="status-meta">桶 {{ health.minio.bucket }}</span>
          </div>
          <p class="panel__time">服务端 UTC 时间：{{ health.serverTimeUtc }}</p>
        </template>
      </div>
    </section>

    <section class="features">
      <RouterLink v-for="card in featureCards" :key="card.title" :to="card.to" class="feature-card">
        <span class="feature-card__icon">{{ card.icon }}</span>
        <h3 class="feature-card__title">{{ card.title }}</h3>
        <p class="feature-card__desc">{{ card.desc }}</p>
      </RouterLink>
    </section>

    <section class="subjects">
      <header class="section-head">
        <h2>八个 AI 方向</h2>
        <p>数据取自 <code>GET /api/portal/v1/subjects</code>，即库内 subject 表。</p>
      </header>

      <el-alert v-if="subjectsError" :title="subjectsError" type="warning" :closable="false" show-icon />

      <div v-else class="subject-grid">
        <article v-for="subject in subjects" :key="subject.id" class="subject-card">
          <span class="subject-card__icon">{{ subject.icon }}</span>
          <h3 class="subject-card__name">{{ subject.name }}</h3>
          <p class="subject-card__desc">{{ subject.description }}</p>
          <span class="subject-card__color" :style="{ background: subject.themeColor }" />
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 48px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(320px, 0.75fr);
  gap: 32px;
  align-items: start;
}

.hero__eyebrow {
  margin: 0 0 12px;
  color: var(--yszx-primary);
  font-size: 13px;
  letter-spacing: 0.08em;
}

.hero__title {
  margin: 0 0 16px;
  font-size: 40px;
  line-height: 1.2;
  color: var(--yszx-text);
}

.hero__desc {
  max-width: 560px;
  margin: 0 0 28px;
  color: var(--yszx-text-secondary);
  font-size: 15px;
  line-height: 1.8;
}

.hero__actions {
  display: flex;
  gap: 12px;
}

.hero__panel {
  padding: 20px;
  border: 1px solid var(--yszx-border);
  border-radius: 14px;
  background: #fff;
}

.panel__title {
  margin: 0 0 4px;
  font-size: 15px;
  color: var(--yszx-text);
}

.panel__hint {
  margin: 0 0 16px;
  font-size: 12px;
  color: var(--yszx-text-tertiary);
}

.status-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--yszx-border);
}

.status-row:last-of-type {
  border-bottom: none;
}

.status-label {
  width: 60px;
  font-size: 13px;
  color: var(--yszx-text);
}

.status-meta {
  font-size: 12px;
  color: var(--yszx-text-tertiary);
}

.panel__time {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--yszx-text-tertiary);
}

.features {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.feature-card {
  padding: 20px;
  border: 1px solid var(--yszx-border);
  border-radius: 12px;
  background: #fff;
  text-decoration: none;
  transition: border-color 0.2s, transform 0.2s;
}

.feature-card:hover {
  border-color: var(--yszx-primary);
  transform: translateY(-2px);
}

.feature-card__icon {
  font-size: 20px;
}

.feature-card__title {
  margin: 10px 0 6px;
  font-size: 15px;
  color: var(--yszx-text);
}

.feature-card__desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--yszx-text-secondary);
}

.section-head h2 {
  margin: 0 0 6px;
  font-size: 22px;
  color: var(--yszx-text);
}

.section-head p {
  margin: 0 0 20px;
  font-size: 13px;
  color: var(--yszx-text-tertiary);
}

.subject-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.subject-card {
  position: relative;
  overflow: hidden;
  padding: 18px;
  border: 1px solid var(--yszx-border);
  border-radius: 12px;
  background: #fff;
}

.subject-card__icon {
  font-size: 22px;
}

.subject-card__name {
  margin: 10px 0 6px;
  font-size: 15px;
  color: var(--yszx-text);
}

.subject-card__desc {
  margin: 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--yszx-text-secondary);
}

.subject-card__color {
  position: absolute;
  inset: 0 auto 0 0;
  width: 3px;
}

@media (max-width: 960px) {
  .hero {
    grid-template-columns: 1fr;
  }

  .features,
  .subject-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
