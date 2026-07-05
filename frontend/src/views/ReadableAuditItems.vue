<template>
  <div class="reader-section">
    <el-empty v-if="items.length === 0" :description="emptyText" />
    <div v-for="(item, index) in items" :key="index" class="review-item">
      <div class="review-item-title">{{ index + 1 }}. {{ item.item }}</div>
      <el-tag :type="statusTagType(item.status)">{{ item.status }}</el-tag>
      <p v-if="item.suggestedLocation"><strong>问题位置：</strong>{{ item.suggestedLocation }}</p>
      <p v-if="item.reason"><strong>问题说明：</strong>{{ item.reason }}</p>
      <p v-if="item.suggestion"><strong>修改建议：</strong>{{ item.suggestion }}</p>
      <p v-if="item.evidence"><strong>原文依据短摘：</strong>{{ item.evidence }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  items: {
    type: Array,
    default: () => []
  },
  emptyText: {
    type: String,
    default: '暂无可读审查结果'
  }
})

function statusTagType(status) {
  if (status?.includes('Major')) return 'danger'
  if (status?.includes('Minor')) return 'warning'
  if (status?.includes('格式')) return 'info'
  if (status?.includes('已撰写')) return 'success'
  if (status?.includes('未撰写')) return 'danger'
  if (status?.includes('不完整')) return 'warning'
  if (status?.includes('不适用')) return 'info'
  return ''
}
</script>

<style scoped>
.reader-section {
  line-height: 1.7;
}

.review-item {
  padding: 14px 0;
  border-bottom: 1px solid #e5e7eb;
}

.review-item:last-child {
  border-bottom: 0;
}

.review-item-title {
  margin-bottom: 8px;
  font-weight: 700;
}

.review-item p {
  margin: 8px 0 0;
}
</style>
