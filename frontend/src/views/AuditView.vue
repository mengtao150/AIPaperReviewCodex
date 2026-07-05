<template>
  <section>
    <div class="page-header">
      <h1 class="page-title">稿件审查</h1>
      <p class="page-desc">上传 .docx 或可复制文本的 .pdf 稿件后，系统会调用已启用的大模型进行类型判别、模板匹配和清单审查。</p>
    </div>

    <div class="panel">
      <el-upload
        drag
        :auto-upload="false"
        :limit="1"
        :on-change="onFileChange"
        :on-remove="onFileRemove"
        accept=".docx,.pdf"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽稿件到这里，或点击选择 .docx / .pdf 文件</div>
      </el-upload>

      <div style="margin-top: 16px">
        <el-button type="primary" :loading="loading" :disabled="!selectedFile" @click="submit">
          开始审查
        </el-button>
      </div>
    </div>

    <div v-if="job" class="panel" style="margin-top: 18px">
      <AuditResultView :job="job" />
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getAudit, uploadAudit } from '../api/client'
import AuditResultView from './AuditResultView.vue'

const selectedFile = ref(null)
const loading = ref(false)
const job = ref(null)

function onFileChange(uploadFile) {
  selectedFile.value = uploadFile.raw
}

function onFileRemove() {
  selectedFile.value = null
}

async function submit() {
  if (!selectedFile.value) return
  loading.value = true
  try {
    job.value = await uploadAudit(selectedFile.value)
    await pollAudit(job.value.id)
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '审查请求失败')
  } finally {
    loading.value = false
  }
}

async function pollAudit(id) {
  while (true) {
    if (job.value?.status === 'COMPLETED') {
      ElMessage.success('审查完成')
      return
    }
    if (job.value?.status === 'FAILED') {
      ElMessage.warning(job.value.errorMessage || '审查失败')
      return
    }
    await delay(2000)
    job.value = await getAudit(id)
  }
}

function delay(ms) {
  return new Promise(resolve => window.setTimeout(resolve, ms))
}
</script>
