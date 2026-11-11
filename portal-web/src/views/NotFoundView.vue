<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <p class="not-found__code">404</p>
    <h1>这条路线还没修好</h1>
    <p class="not-found__desc">页面不存在，或者已经被移走了。</p>
    <el-button type="primary" @click="router.push('/')">回到首页</el-button>
  </div>
</template>

<style scoped>
.not-found {
  display: grid;
  place-items: center;
  gap: 8px;
  min-height: 60vh;
  text-align: center;
}

.not-found__code {
  margin: 0;
  font-size: 64px;
  font-weight: 500;
  color: var(--yszx-primary);
}

.not-found h1 {
  margin: 0;
  font-size: 20px;
  color: var(--yszx-text);
}

.not-found__desc {
  margin: 0 0 12px;
  color: var(--yszx-text-secondary);
}
</style>
