<template>
  <section>
    <div class="page-header">
      <h1 class="page-title">审稿历史</h1>
      <p class="page-desc">查看历史审稿任务，并回看任意一次任务的审查结果和 Word 报告。</p>
    </div>

    <div class="panel">
      <el-button :loading="loading" @click="load">刷新历史</el-button>
      <el-table :data="items" style="margin-top: 16px" border>
        <el-table-column prop="id" label="任务 ID" width="90" />
        <el-table-column prop="originalFilename" label="稿件" min-width="220" />
        <el-table-column prop="status" label="状态" width="150" />
        <el-table-column prop="reviewType" label="综述类型" width="190">
          <template #default="{ row }">{{ row.reviewType || '未判定' }}</template>
        </el-table-column>
        <el-table-column label="进度" width="160">
          <template #default="{ row }">
            <el-progress :percentage="row.progressPercent || 0" :stroke-width="10" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="190">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="showDetail(row.id)">查看结果</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="selectedJob" class="panel" style="margin-top: 18px">
      <AuditResultView :job="selectedJob" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAudit, listAudits } from '../api/client'
import AuditResultView from './AuditResultView.vue'

const items = ref([])
const selectedJob = ref(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    items.value = await listAudits()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '加载历史失败')
  } finally {
    loading.value = false
  }
}

async function showDetail(id) {
  selectedJob.value = await getAudit(id)
}

function formatDateTime(value) {
  if (!value) return ''
  return new Date(value).toLocaleString()
}

onMounted(load)
</script>
