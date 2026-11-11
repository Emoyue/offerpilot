<script setup lang="ts">
import PlaceholderView from '@/components/PlaceholderView.vue'
</script>

<template>
  <PlaceholderView
    title="错题本"
    description="错题自动归集，按错误次数分级；连续答对 3 次自动转为「已掌握」。"
    endpoint="GET /api/portal/v1/wrong-questions"
    :todos="[
      '顶部统计卡：总数 / 待复习 / 已掌握 / 掌握率',
      '错题等级筛选（高频易错 ≥3、多次 =2、偶尔 =1）',
      '重做错题与掌握状态回退规则',
      '高频错题 Top3'
    ]"
  />
</template>
