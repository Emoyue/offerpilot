<script setup lang="ts">
import PlaceholderView from '@/components/PlaceholderView.vue'
</script>

<template>
  <PlaceholderView
    title="个人中心"
    description="资料维护与学习数据展示；修改密码后旧令牌全部拉黑，需重新登录。"
    endpoint="GET /api/portal/v1/profile"
    :todos="[
      '基本信息查看与编辑（头像上传走 MinIO）',
      '学习数据：练习统计、答题统计、连续天数',
      '修改密码（旧密码校验 + 令牌黑名单）'
    ]"
  />
</template>
