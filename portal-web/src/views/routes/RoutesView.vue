<script setup lang="ts">
import PlaceholderView from '@/components/PlaceholderView.vue'
</script>

<template>
  <PlaceholderView
    title="学习路线"
    description="学科下的课程按顺序学习，跳转 B 站观看，手动勾选「已完成」即计入已学课程。"
    endpoint="GET /api/portal/v1/routes"
    :todos="[
      '路线列表与阶段进度',
      '课程卡片（封面/标题/时长）与外链跳转',
      '勾选完成（route_course_completion 落库）'
    ]"
  />
</template>
