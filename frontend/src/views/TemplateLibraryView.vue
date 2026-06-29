<template>
  <section>
    <div class="page-header">
      <h1 class="page-title">文章结构模板库</h1>
      <p class="page-desc">上传 PDF/Word 模板后，由已启用的大模型解析出标题及小标题层级、必需项和可选项。</p>
    </div>

    <div class="panel">
      <el-form :model="form" label-width="110px">
        <el-form-item label="综述类型">
          <el-select v-model="form.reviewTypeId" placeholder="选择模板适用的综述类型" style="width: 320px">
            <el-option v-for="type in reviewTypes" :key="type.id" :label="type.name" :value="type.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板名称">
          <el-input v-model="form.name" placeholder="例如 Narrative Review 文章结构模板" />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="form.version" placeholder="例如 2025.6.11 或 1.0" />
        </el-form-item>
        <el-form-item label="模板文件">
          <el-upload :auto-upload="false" :limit="1" accept=".docx,.pdf" :on-change="onFileChange" :on-remove="onFileRemove">
            <el-button>选择 PDF/Word</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="importing" @click="submitImport">用大模型解析导入</el-button>
          <el-button :loading="loading" @click="load">刷新列表</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel" style="margin-top: 18px">
      <el-button :loading="loading" @click="load">刷新</el-button>
      <el-table :data="items" style="margin-top: 16px" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="模板名称" />
        <el-table-column prop="reviewType" label="综述类型" width="220" />
        <el-table-column prop="version" label="版本" width="120" />
        <el-table-column prop="active" label="启用" width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规则" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row.id)">查看规则</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="detailVisible" title="结构模板规则" width="860px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="模板">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="综述类型">{{ detail.reviewType }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ detail.version }}</el-descriptions-item>
      </el-descriptions>
      <h3>标题及小标题层级结构</h3>
      <pre class="report">{{ pretty(detail?.parsedSections) }}</pre>
      <h3>必需项</h3>
      <pre class="report">{{ pretty(detail?.requiredItems) }}</pre>
      <h3>可选项</h3>
      <pre class="report">{{ pretty(detail?.optionalItems) }}</pre>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getStructureTemplate, importStructureTemplate, listReviewTypes, listStructureTemplates } from '../api/client'

const items = ref([])
const reviewTypes = ref([])
const loading = ref(false)
const importing = ref(false)
const detailVisible = ref(false)
const detail = ref(null)
const form = reactive({
  reviewTypeId: null,
  name: '',
  version: '1.0',
  file: null
})

async function load() {
  loading.value = true
  try {
    const [types, templates] = await Promise.all([listReviewTypes(), listStructureTemplates()])
    reviewTypes.value = types
    items.value = templates
  } finally {
    loading.value = false
  }
}

function onFileChange(uploadFile) {
  form.file = uploadFile.raw
}

function onFileRemove() {
  form.file = null
}

async function submitImport() {
  if (!form.reviewTypeId || !form.file) {
    ElMessage.warning('请选择综述类型和模板文件')
    return
  }
  importing.value = true
  try {
    await importStructureTemplate({ ...form })
    ElMessage.success('解析导入完成')
    await load()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '解析导入失败，请确认已配置并启用大模型')
  } finally {
    importing.value = false
  }
}

async function showDetail(id) {
  detail.value = await getStructureTemplate(id)
  detailVisible.value = true
}

function pretty(value) {
  if (!value) return ''
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

onMounted(load)
</script>
