<script setup lang="ts">
import PlaceholderView from '@/components/PlaceholderView.vue'
</script>

<template>
  <PlaceholderView
    title="题库中心"
    description="按学科与阶段筛选已上架题目，支持单选、多选、判断、简答四类。"
    endpoint="GET /api/portal/v1/questions"
    :todos="[
      '学科/阶段/题型/难度 四维筛选栏（复用 /subjects 接口）',
      '题目列表与预览抽屉',
      '按知识点进入练习'
    ]"
  />
</template>
