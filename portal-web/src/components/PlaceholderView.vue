<script setup lang="ts">
defineProps<{
  title: string
  description: string
  endpoint?: string
  todos?: string[]
}>()
</script>

<template>
  <section class="placeholder">
    <header class="placeholder__head">
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
      <el-tag v-if="endpoint" type="info" size="small" effect="plain">{{ endpoint }}</el-tag>
    </header>

    <div v-if="todos?.length" class="placeholder__todos">
      <h2>本页待落地</h2>
      <ul>
        <li v-for="item in todos" :key="item">{{ item }}</li>
      </ul>
    </div>

    <el-empty description="骨架已就位，业务待开发" />
  </section>
</template>

<style scoped>
.placeholder {
  padding: 8px 0;
}

.placeholder__head h1 {
  margin: 0 0 8px;
  font-size: 24px;
  color: var(--yszx-text);
}

.placeholder__head p {
  margin: 0 0 10px;
  color: var(--yszx-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.placeholder__todos {
  margin: 24px 0 8px;
  padding: 16px 20px;
  border: 1px dashed var(--yszx-border);
  border-radius: 12px;
  background: #fff;
}

.placeholder__todos h2 {
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--yszx-text);
}

.placeholder__todos ul {
  margin: 0;
  padding-left: 18px;
  color: var(--yszx-text-secondary);
  font-size: 13px;
  line-height: 2;
}
</style>
