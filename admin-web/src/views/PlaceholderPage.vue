<script setup lang="ts">
/**
 * 空页面占位组件。
 *
 * 管理端业务页面尚未开发，统一用本组件占位，只呈现该页的职责说明与对应接口编号。
 * 页面标题由布局顶栏统一显示——对齐原型：内容区不重复标题。
 */
defineProps<{
  /** 页面标题；仅接收不渲染，避免作为 attribute 泄漏到根元素 */
  title: string
  /** 页面职责说明 */
  description: string
  /** 对应接口编号，便于开发时快速定位 04 文档章节 */
  endpoint?: string
}>()
</script>

<template>
  <section class="page">
    <div class="page-card">
      <el-empty description="页面骨架已就位，业务待开发">
        <p class="page-desc">{{ description }}</p>
        <el-tag v-if="endpoint" type="info" size="small" effect="plain">
          接口：{{ endpoint }}
        </el-tag>
      </el-empty>
    </div>
  </section>
</template>

<style scoped>
.page-card {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.page-desc {
  max-width: 560px;
  margin: 0 auto 12px;
  font-size: 13px;
  line-height: 1.7;
  color: #909399;
}
</style>
