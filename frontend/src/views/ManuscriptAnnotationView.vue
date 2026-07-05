<template>
  <div class="annotation-view">
    <header class="view-header">
      <div>
        <p class="eyebrow">原稿全文</p>
        <h3>全文原稿与审稿批注</h3>
      </div>
      <div class="view-stats">
        <el-tag>{{ paragraphs.length }} 段原文</el-tag>
        <el-tag v-if="matchedAnnotations.length" type="warning">{{ matchedAnnotations.length }} 条已定位意见</el-tag>
        <el-tag v-if="unmatchedAnnotations.length" type="info">{{ unmatchedAnnotations.length }} 条未自动定位</el-tag>
      </div>
    </header>

    <el-empty
      v-if="paragraphs.length === 0"
      description="暂无原稿全文内容。系统会在打开历史任务时尝试从已保存原稿回填；如果原始文件已不存在，需要重新上传后查看。"
    />

    <template v-else>
      <div class="annotation-layout">
        <main class="manuscript-pane" aria-label="原稿全文">
          <article class="manuscript-document">
            <section
              v-for="paragraph in paragraphs"
              :id="paragraph.paragraphId"
              :key="paragraph.paragraphId"
              class="manuscript-paragraph"
              :class="{ highlighted: annotationsByParagraph[paragraph.paragraphId]?.length }"
            >
              <div class="paragraph-index">P{{ paragraph.orderIndex }}</div>
              <p>{{ paragraph.text }}</p>

              <div v-if="annotationsByParagraph[paragraph.paragraphId]?.length" class="inline-comments">
                <div
                  v-for="annotation in annotationsByParagraph[paragraph.paragraphId]"
                  :key="annotation.id"
                  class="inline-comment"
                >
                  <div class="inline-comment-meta">
                    <el-tag size="small">{{ annotation.source }}</el-tag>
                    <el-tag :type="statusTagType(annotation.status)" size="small">{{ annotation.status }}</el-tag>
                  </div>
                  <div class="inline-comment-title">{{ annotation.item }}</div>
                  <p v-if="annotation.reason"><strong>问题说明：</strong>{{ annotation.reason }}</p>
                  <p v-if="annotation.suggestion"><strong>修改建议：</strong>{{ annotation.suggestion }}</p>
                  <p v-if="annotation.evidence"><strong>原文依据：</strong>{{ annotation.evidence }}</p>
                </div>
              </div>
            </section>
          </article>
        </main>

        <aside class="comments-pane">
          <div class="comments-title">审稿意见导航</div>
          <p class="comments-help">点击已定位意见，可跳转到原文中被高亮的段落。</p>
          <el-empty v-if="annotations.length === 0" description="审稿完成后，问题意见会在这里汇总；原文仍可单独阅读。" />
          <button
            v-for="annotation in annotations"
            :key="annotation.id"
            class="comment-card"
            :class="{ unmatched: !annotation.paragraphId }"
            type="button"
            :disabled="!annotation.paragraphId"
            @click="scrollToParagraph(annotation.paragraphId)"
          >
            <div class="comment-card-top">
              <el-tag :type="statusTagType(annotation.status)" size="small">{{ annotation.status }}</el-tag>
              <span>{{ annotation.source }}</span>
              <span class="paragraph-target">
                {{ annotation.paragraphId ? `P${annotation.orderIndex}` : '未自动定位' }}
              </span>
            </div>
            <div class="comment-title">{{ annotation.item }}</div>
            <p v-if="annotation.location"><strong>问题位置：</strong>{{ annotation.location }}</p>
            <p v-if="annotation.reason"><strong>问题说明：</strong>{{ annotation.reason }}</p>
            <p v-if="annotation.suggestion"><strong>修改建议：</strong>{{ annotation.suggestion }}</p>
            <p v-if="annotation.evidence"><strong>原文依据短摘：</strong>{{ annotation.evidence }}</p>
          </button>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  paragraphs: {
    type: Array,
    default: () => []
  },
  items: {
    type: Array,
    default: () => []
  }
})

const annotations = computed(() => props.items.map((item, index) => {
  const paragraph = findBestParagraph(item, props.paragraphs)
  return {
    id: `${item.source || 'issue'}-${index}`,
    paragraphId: paragraph?.paragraphId || '',
    orderIndex: paragraph?.orderIndex || '',
    source: item.source || '审稿意见',
    item: item.item || '未命名问题',
    status: item.status || '无法判断',
    location: item.suggestedLocation || '',
    evidence: item.evidence || '',
    reason: item.reason || '',
    suggestion: item.suggestion || ''
  }
}))

const matchedAnnotations = computed(() => annotations.value.filter(annotation => annotation.paragraphId))
const unmatchedAnnotations = computed(() => annotations.value.filter(annotation => !annotation.paragraphId))

const annotationsByParagraph = computed(() => {
  const grouped = {}
  for (const annotation of matchedAnnotations.value) {
    if (!grouped[annotation.paragraphId]) {
      grouped[annotation.paragraphId] = []
    }
    grouped[annotation.paragraphId].push(annotation)
  }
  return grouped
})

function findBestParagraph(item, paragraphs) {
  if (!Array.isArray(paragraphs) || paragraphs.length === 0) {
    return null
  }
  const explicitParagraph = findParagraphByLocation(item.suggestedLocation, paragraphs)
  if (explicitParagraph) {
    return explicitParagraph
  }
  const terms = uniqueTerms([
    item.evidence,
    item.suggestedLocation,
    item.item,
    ...quotedSnippets(item.reason),
    ...quotedSnippets(item.suggestion)
  ])

  let best = null
  let bestScore = 0
  for (const paragraph of paragraphs) {
    const text = normalize(paragraph.text)
    const compactText = compact(text)
    let score = 0
    for (const term of terms) {
      if (text.includes(term)) {
        score = Math.max(score, term.length + 100)
        continue
      }
      const compactTerm = compact(term)
      if (compactTerm.length >= 8 && compactText.includes(compactTerm)) {
        score = Math.max(score, compactTerm.length + 50)
      }
      score = Math.max(score, tokenOverlapScore(text, term))
    }
    if (score > bestScore) {
      best = paragraph
      bestScore = score
    }
  }
  return bestScore >= 2 ? best : null
}

function findParagraphByLocation(location, paragraphs) {
  const text = String(location || '')
  const match = text.match(/(?:P|第)\s*(\d{1,4})\s*(?:段|paragraph)?/i)
  if (!match) {
    return null
  }
  const orderIndex = Number(match[1])
  return paragraphs.find(paragraph => Number(paragraph.orderIndex) === orderIndex) || null
}

function uniqueTerms(values) {
  const seen = new Set()
  const terms = []
  for (const value of values) {
    const term = normalize(value)
    if (term.length < 4 || seen.has(term)) {
      continue
    }
    seen.add(term)
    terms.push(term)
  }
  return terms
}

function quotedSnippets(value) {
  const text = String(value || '')
  const snippets = []
  for (const match of text.matchAll(/[“"']([^“”"']{4,120})[”"']/g)) {
    snippets.push(match[1])
  }
  return snippets
}

function tokenOverlapScore(text, term) {
  const tokens = term
    .split(/[\s,.;:，。；：、()（）[\]【】]+/)
    .map(token => token.trim())
    .filter(token => token.length >= 4)
  if (tokens.length === 0) return 0
  return tokens.reduce((score, token) => score + (text.includes(token) ? 1 : 0), 0)
}

function normalize(value) {
  return String(value || '')
    .replace(/<think>[\s\S]*?<\/think>/gi, '')
    .replaceAll('```json', '')
    .replaceAll('```', '')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase()
}

function compact(value) {
  return String(value || '').replace(/\s+/g, '')
}

function scrollToParagraph(paragraphId) {
  if (!paragraphId) return
  document.getElementById(paragraphId)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function statusTagType(status) {
  if (status?.includes('Major') || status?.includes('未撰写')) return 'danger'
  if (status?.includes('Minor') || status?.includes('不完整')) return 'warning'
  if (status?.includes('格式')) return 'info'
  return ''
}
</script>

<style scoped>
.annotation-view {
  display: grid;
  gap: 14px;
}

.view-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.eyebrow {
  margin: 0 0 4px;
  color: #409eff;
  font-size: 13px;
  font-weight: 700;
}

.view-header h3 {
  margin: 0;
  color: #111827;
  font-size: 18px;
}

.view-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.annotation-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
  align-items: start;
}

.manuscript-pane {
  max-height: 72vh;
  overflow: auto;
  padding: 0 10px 0 2px;
}

.manuscript-document {
  max-width: 980px;
  margin: 0 auto;
  padding: 20px 22px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.manuscript-paragraph {
  position: relative;
  margin-bottom: 10px;
  padding: 10px 12px 10px 52px;
  border: 1px solid transparent;
  border-radius: 6px;
  line-height: 1.75;
}

.manuscript-paragraph.highlighted {
  border-color: #f59e0b;
  background: #fff7ed;
}

.paragraph-index {
  position: absolute;
  left: 12px;
  top: 13px;
  color: #909399;
  font-size: 12px;
}

.manuscript-paragraph p {
  margin: 0;
  white-space: pre-wrap;
}

.inline-comments {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.inline-comment {
  padding: 10px 12px;
  border-left: 3px solid #f59e0b;
  border-radius: 6px;
  background: #fffbeb;
  font-size: 13px;
}

.inline-comment-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}

.inline-comment-title {
  margin-bottom: 4px;
  font-weight: 700;
}

.inline-comment p {
  margin: 5px 0 0;
}

.comments-pane {
  position: sticky;
  top: 12px;
  max-height: 72vh;
  overflow: auto;
}

.comments-title {
  font-weight: 700;
}

.comments-help {
  margin: 4px 0 10px;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

.comment-card {
  width: 100%;
  margin-bottom: 10px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
  color: #1f2937;
  text-align: left;
  cursor: pointer;
}

.comment-card:hover {
  border-color: #409eff;
  background: #f8fbff;
}

.comment-card:disabled {
  cursor: default;
}

.comment-card.unmatched {
  border-style: dashed;
  background: #f9fafb;
}

.comment-card-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #606266;
  font-size: 12px;
}

.paragraph-target {
  margin-left: auto;
  color: #909399;
}

.comment-title {
  margin-bottom: 8px;
  font-weight: 700;
}

.comment-card p {
  margin: 6px 0 0;
  line-height: 1.6;
}

@media (max-width: 980px) {
  .view-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .view-stats {
    justify-content: flex-start;
  }

  .annotation-layout {
    grid-template-columns: 1fr;
  }

  .comments-pane {
    position: static;
    max-height: none;
  }
}
</style>
