<template>
  <section v-if="job">
    <el-progress
      :percentage="job.progressPercent || 0"
      :status="progressStatus"
      :stroke-width="16"
      striped
      striped-flow
    />
    <div class="progress-caption">{{ progressLabel }}</div>

    <el-descriptions title="审查结果" :column="2" border>
      <el-descriptions-item label="任务 ID">{{ job.id }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ humanJobStatus(job.status) }}</el-descriptions-item>
      <el-descriptions-item label="稿件">{{ job.originalFilename }}</el-descriptions-item>
      <el-descriptions-item label="综述类型">{{ job.reviewType || '未判定' }}</el-descriptions-item>
      <el-descriptions-item v-if="job.createdAt" label="创建时间">{{ formatDateTime(job.createdAt) }}</el-descriptions-item>
    </el-descriptions>

    <el-tabs style="margin-top: 18px">
      <el-tab-pane label="原稿全文">
        <ManuscriptAnnotationView :paragraphs="manuscriptParagraphs" :items="allProblemItems" />
      </el-tab-pane>

      <el-tab-pane label="审查报告">
        <div class="report-actions">
          <el-button type="primary" :disabled="job.status !== 'COMPLETED'" @click="downloadWordReport">
            下载 Word 报告
          </el-button>
        </div>
        <article class="review-report" v-html="renderedReport"></article>
      </el-tab-pane>

      <el-tab-pane label="综述类型判定">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="判定类型">{{ classificationView.reviewType }}</el-descriptions-item>
          <el-descriptions-item label="判定依据">{{ classificationView.reason }}</el-descriptions-item>
          <el-descriptions-item label="补充说明">{{ classificationView.evidence }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane label="文章结构审查">
        <ReadableAuditItems :items="structureItems" empty-text="暂无可读的结构审查结果" />
      </el-tab-pane>

      <el-tab-pane label="审核清单审查">
        <ReadableAuditItems :items="checklistItems" empty-text="暂无可读的清单审查结果" />
      </el-tab-pane>

      <el-tab-pane v-if="ragflowEnhancedAuditItems.length" label="人工经验增强审查">
        <ReadableAuditItems :items="ragflowEnhancedAuditItems" empty-text="暂无可读的人工经验增强审查结果" />
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { auditReportDownloadUrl } from '../api/client'
import ManuscriptAnnotationView from './ManuscriptAnnotationView.vue'
import ReadableAuditItems from './ReadableAuditItems.vue'

const props = defineProps({
  job: {
    type: Object,
    required: true
  }
})

const progressStatus = computed(() => {
  if (props.job?.status === 'FAILED') return 'exception'
  if (props.job?.status === 'COMPLETED') return 'success'
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
    FAILED: props.job?.errorMessage || '审查失败'
  }
  return labels[props.job?.status] || '等待开始'
})

const renderedReport = computed(() => renderMarkdownReport(buildProblemFocusedReport()))
const classificationView = computed(() => normalizeClassification(props.job?.classificationResult))
const structureItems = computed(() => problemOnly(normalizeAuditItems(props.job?.structureAuditResult)))
const checklistItems = computed(() => problemOnly(normalizeAuditItems(props.job?.checklistAuditResult)))
const ragflowEnhancedAuditItems = computed(() => problemOnly(normalizeAuditItems(props.job?.ragflowEnhancedAuditResult)))
const manuscriptParagraphs = computed(() => Array.isArray(props.job?.manuscriptParagraphs) ? props.job.manuscriptParagraphs : [])
const allProblemItems = computed(() => [
  ...structureItems.value.map(item => ({ ...item, source: '文章结构审查' })),
  ...checklistItems.value.map(item => ({ ...item, source: '审核清单审查' })),
  ...ragflowEnhancedAuditItems.value.map(item => ({ ...item, source: '人工经验增强审查' }))
])

function downloadWordReport() {
  if (!props.job?.id) return
  window.open(auditReportDownloadUrl(props.job.id), '_blank')
}

function normalizeClassification(value) {
  const parsed = parseJson(value)
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return {
      reviewType: props.job?.reviewType || '暂未判定',
      reason: '系统暂未生成可读的类型判定说明。',
      evidence: '请以审查报告为准。'
    }
  }
  return {
    reviewType: shortText(parsed.review_type || parsed.reviewType || props.job?.reviewType || '暂未判定', 120),
    reason: shortText(parsed.reason || parsed.rationale || parsed.explanation || parsed.decision_path || '系统未返回明确判定依据。', 320),
    evidence: shortText(parsed.evidence || parsed.key_evidence || parsed.confidence || '无补充说明。', 320)
  }
}

function normalizeAuditItems(value) {
  const parsed = parseCompleteJson(value)
  if (!parsed) {
    return []
  }
  if (Array.isArray(parsed)) {
    return parsed.filter(isNonEmptyAuditObject).map(toReadableAuditItem)
  }
  if (typeof parsed === 'object' && parsed.message) {
    return [{
      item: '系统提示',
      status: parsed.status || '无法判断',
      evidence: '',
      reason: parsed.message,
      suggestion: '请先完善相应模板或清单后重新审查。',
      suggestedLocation: ''
    }]
  }
  if (typeof parsed === 'object') {
    if (!isNonEmptyAuditObject(parsed)) return []
    return [toReadableAuditItem(parsed)]
  }
  return []
}

function isNonEmptyAuditObject(item) {
  if (!item || typeof item !== 'object') return false
  return Boolean(item.hierarchy_path || item.item || item.requirement || item.status || item.evidence || item.reason || item.comment || item.suggestion || item.message)
}

function toReadableAuditItem(item) {
  return {
    item: shortText(item.hierarchy_path || item.item || item.requirement || '未命名检查项', 160),
    status: shortText(item.status || '无法判断', 40),
    evidence: shortText(item.evidence || '', 180),
    reason: shortText(item.reason || item.comment || '', 260),
    suggestion: shortText(item.suggestion || '', 260),
    suggestedLocation: shortText(item.suggested_location || item.suggestedLocation || '', 120)
  }
}

function problemOnly(items) {
  return items.filter(isProblemItem)
}

function isProblemItem(item) {
  const status = item.status || ''
  return !status.includes('已撰写')
    && !status.includes('不适用')
    && !status.includes('no_ragflow_matches')
    && !status.includes('ragflow_error')
}

function buildProblemFocusedReport() {
  if (props.job?.status === 'FAILED') {
    return `# 综述稿件审查报告\n\n## 审查失败\n\n${shortText(props.job?.errorMessage || '审查失败', 300)}`
  }
  const lines = [
    '# 综述稿件审查报告',
    '',
    '## 稿件基本信息',
    '',
    `稿件文件：${props.job?.originalFilename || '未命名稿件'}`,
    '',
    `综述类型：${classificationView.value.reviewType}`,
    '',
    `重点问题数量：${allProblemItems.value.length} 项`,
    '',
    '## 重点问题概览',
    ''
  ]
  if (allProblemItems.value.length === 0) {
    lines.push('当前未发现需要优先处理的问题项。')
  } else {
    allProblemItems.value.forEach((item, index) => {
      lines.push(`${index + 1}. [${item.source}] ${item.item}（${item.status}）`)
    })
  }
  lines.push('', '## 修改建议说明', '')
  if (allProblemItems.value.length === 0) {
    lines.push('建议审稿人结合专业判断复核核心结论、证据链和正文逻辑。')
  } else {
    lines.push('详细问题请查看下方“文章结构审查”“审核清单审查”和“人工经验增强审查”页签。页面已隐藏已撰写项、原始 JSON、模型推理过程和长篇全文复述。')
  }
  return lines.join('\n')
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
    } else if (/^\\d+\\.\\s/.test(trimmed) || trimmed.startsWith('- ')) {
      html.push(`<p class="report-list-line">${escapeHtml(trimmed)}</p>`)
    } else {
      html.push(`<p>${escapeHtml(trimmed)}</p>`)
    }
  }
  if (codeLines.length) {
    html.push(`<pre>${escapeHtml(codeLines.join('\n'))}</pre>`)
  }
  return html.join('')
}

function parseJson(value) {
  if (!value) return null
  const candidates = [value.trim()]
  const fencedMatches = value.matchAll(/```(?:json)?\s*([\s\S]*?)```/gi)
  for (const match of fencedMatches) {
    candidates.push(match[1].trim())
  }
  const balanced = extractBalancedJson(value)
  if (balanced) {
    candidates.push(balanced)
  }
  for (const candidate of candidates) {
    try {
      return JSON.parse(candidate)
    } catch {
      // Try the next likely JSON payload in the model response.
    }
  }
  return null
}

function parseCompleteJson(value) {
  if (!value) return null
  const candidates = [value.trim()]
  const fencedMatches = value.matchAll(/```(?:json)?\s*([\s\S]*?)```/gi)
  for (const match of fencedMatches) {
    candidates.push(match[1].trim())
  }
  for (const candidate of candidates) {
    try {
      return JSON.parse(candidate)
    } catch {
      // Keep audit parsing strict; mixed text falls back to readable raw output.
    }
  }
  const objects = extractObjectCandidates(value)
  if (objects.length) {
    return objects
  }
  return null
}

function extractObjectCandidates(value) {
  const objects = []
  if (!value) return objects
  for (let start = 0; start < value.length; start++) {
    if (value[start] !== '{') continue
    const candidate = balancedCandidate(value, start)
    if (!candidate) continue
    try {
      const parsed = JSON.parse(candidate)
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        objects.push(parsed)
      }
      start += candidate.length - 1
    } catch {
      // Keep looking for the next balanced object.
    }
  }
  return objects
}

function extractBalancedJson(value) {
  for (let start = 0; start < value.length; start++) {
    if (value[start] !== '{' && value[start] !== '[') continue
    const candidate = balancedCandidate(value, start)
    if (candidate) return candidate
    if (value[start] === '[') return ''
  }
  return ''
}

function balancedCandidate(value, start) {
  let depth = 0
  let inString = false
  let escaped = false
  for (let index = start; index < value.length; index++) {
    const char = value[index]
    if (inString) {
      if (escaped) {
        escaped = false
      } else if (char === '\\') {
        escaped = true
      } else if (char === '"') {
        inString = false
      }
      continue
    }
    if (char === '"') {
      inString = true
    } else if (char === '{' || char === '[') {
      depth++
    } else if (char === '}' || char === ']') {
      depth--
      if (depth === 0) {
        return value.slice(start, index + 1)
      }
    }
  }
  return ''
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function shortText(value, maxLength) {
  const text = Array.isArray(value)
    ? value.map(item => typeof item === 'string' ? item : JSON.stringify(item)).join('；')
    : String(value ?? '')
  const cleaned = text
    .replace(/<think>[\s\S]*?<\/think>/gi, '')
    .replaceAll('```json', '')
    .replaceAll('```', '')
    .replace(/\s+/g, ' ')
    .trim()
  if (cleaned.length <= maxLength) return cleaned
  return `${cleaned.slice(0, maxLength).trim()}……`
}

function formatDateTime(value) {
  return new Date(value).toLocaleString()
}

function humanJobStatus(status) {
  const labels = {
    UPLOADED: '已上传',
    EXTRACTED: '已解析稿件',
    CLASSIFIED: '已完成类型判定',
    STRUCTURE_AUDITED: '已完成结构审查',
    CHECKLIST_AUDITED: '已完成审核清单审查',
    COMPLETED: '审查完成',
    FAILED: '审查失败'
  }
  return labels[status] || status || '未记录'
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
  font-family: "Microsoft YaHei", sans-serif;
  line-height: 1.6;
}

.review-report :deep(.report-list-line) {
  padding-left: 16px;
}
</style>
