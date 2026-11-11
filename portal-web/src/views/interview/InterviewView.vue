<script setup lang="ts">
import PlaceholderView from '@/components/PlaceholderView.vue'
</script>

<template>
  <PlaceholderView
    title="AI 模拟面试"
    description="选择方向、难度与题量（3/5/8/10/15，默认 10），每日 5 次配额，按上海时区 0 点重置。"
    endpoint="POST /api/portal/v1/interview/sessions"
    :todos="[
      '面试配置页：方向（8 学科）/ 难度 / 题量 / 剩余次数',
      '进行中页：语音作答（Web Speech API，不支持则降级为文字）',
      '逐题评分 SSE：分析中 → 评分中 → 思考中 + 打字机',
      '报告页：逐题得分、整体评价、能力雷达、补强建议',
      '面试记录列表与「再次开始」'
    ]"
  />
</template>
