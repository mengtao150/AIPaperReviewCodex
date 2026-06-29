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
      <el-progress
        :percentage="progressPercent"
        :status="progressStatus"
        :stroke-width="16"
        striped
        striped-flow
      />
      <div class="progress-caption">{{ progressLabel }}</div>

      <el-descriptions title="审查结果" :column="2" border>
        <el-descriptions-item label="任务 ID">{{ job.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ job.status }}</el-descriptions-item>
        <el-descriptions-item label="稿件">{{ job.originalFilename }}</el-descriptions-item>
        <el-descriptions-item label="综述类型">{{ job.reviewType || '未判定' }}</el-descriptions-item>
      </el-descriptions>

      <el-tabs style="margin-top: 18px">
        <el-tab-pane label="最终报告">
          <div class="report-actions">
            <el-button type="primary" :disabled="job.status !== 'COMPLETED'" @click="downloadWordReport">
              下载 Word 报告
            </el-button>
          </div>
          <article class="review-report" v-html="renderedReport"></article>
        </el-tab-pane>
        <el-tab-pane label="类型判别 JSON">
          <pre class="report">{{ job.classificationResult }}</pre>
        </el-tab-pane>
        <el-tab-pane label="结构审查 JSON">
          <pre class="report">{{ job.structureAuditResult }}</pre>
        </el-tab-pane>
        <el-tab-pane label="清单审查 JSON">
          <pre class="report">{{ job.checklistAuditResult }}</pre>
        </el-tab-pane>
      </el-tabs>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { auditReportDownloadUrl, getAudit, uploadAudit } from '../api/client'

const selectedFile = ref(null)
const loading = ref(false)
const job = ref(null)

const progressPercent = computed(() => job.value?.progressPercent || 0)
const progressStatus = computed(() => {
  if (job.value?.status === 'FAILED') return 'exception'
  if (job.value?.status === 'COMPLETED') return 'success'
  return undefined
})
const progressLabel = computed(() => {
  const labels = {
    UPLOADED: '已上传，等待解析稿件',
    EXTRACTED: '已提取稿件文本，正在判别综述类型',
    CLASSIFIED: '已完成类型判别，正在进行结构审查',
    STRUCTURE_AUDITED: '已完成结构审查，正在进行清单审查',
    CHECKLIST_AUDITED: '已完成清单审查，正在生成最终报告',
    COMPLETED: '审查完成',
    FAILED: job.value?.errorMessage || '审查失败'
  }
  return labels[job.value?.status] || '等待开始'
})
const renderedReport = computed(() => renderMarkdownReport(job.value?.finalReport || job.value?.errorMessage || '暂无报告'))

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

function downloadWordReport() {
  if (!job.value?.id) return
  window.open(auditReportDownloadUrl(job.value.id), '_blank')
}

function renderMarkdownReport(markdown) {
  const lines = markdown.split(/\r?\n/)
  const html = []
  let inCode = false
  let codeLines = []
  for (const line of lines) {
    const trimmed = line.trim()
    if (trimmed.startsWith('```')) {
      if (inCode) {
        html.push(`<pre>${escapeHtml(codeLines.join('\n'))}</pre>`)
        codeLines = []
      }
      inCode = !inCode
      continue
    }
    if (inCode) {
      codeLines.push(line)
      continue
    }
    if (!trimmed) continue
    if (trimmed.startsWith('# ')) {
      html.push(`<h2>${escapeHtml(trimmed.slice(2))}</h2>`)
    } else if (trimmed.startsWith('## ')) {
      html.push(`<h3>${escapeHtml(trimmed.slice(3))}</h3>`)
    } else {
      html.push(`<p>${escapeHtml(trimmed)}</p>`)
    }
  }
  if (codeLines.length) {
    html.push(`<pre>${escapeHtml(codeLines.join('\n'))}</pre>`)
  }
  return html.join('')
}

function escapeHtml(value) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}
</script>

<style scoped>
.progress-caption {
  margin: 10px 0 18px;
  color: #606266;
}

.report-actions {
  margin-bottom: 14px;
}

.review-report {
  padding: 4px 2px;
  line-height: 1.75;
  color: #1f2937;
}

.review-report :deep(h2) {
  margin: 0 0 18px;
  text-align: center;
  font-size: 22px;
}

.review-report :deep(h3) {
  margin: 22px 0 10px;
  padding-left: 10px;
  border-left: 4px solid #409eff;
  font-size: 17px;
}

.review-report :deep(p) {
  margin: 8px 0;
}

.review-report :deep(pre) {
  margin: 10px 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
  white-space: pre-wrap;
  font-family: Consolas, "Microsoft YaHei", monospace;
  line-height: 1.6;
}
</style>
