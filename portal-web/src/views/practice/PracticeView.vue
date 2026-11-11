<script setup lang="ts">
import PlaceholderView from '@/components/PlaceholderView.vue'
</script>

<template>
  <PlaceholderView
    title="刷题练习"
    description="顺序或随机模式，客观题即时判分，简答题走 AI 流式批改（≥6 分通过）。"
    endpoint="POST /api/portal/v1/practice/{sessionId}/answers"
    :todos="[
      '练习会话状态机：开始 → 作答 → 判分 → 结算',
      '客观题本地即时判分 + 服务端复核',
      '简答 SSE 批改（status / token / grade 三类事件）',
      '继续上次学习（断点续练）'
    ]"
  />
</template>
