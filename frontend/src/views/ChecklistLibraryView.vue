<template>
  <section>
    <div class="page-header">
      <h1 class="page-title">审核清单库</h1>
      <p class="page-desc">上传 PDF/Word 清单后，由已启用的大模型解析为可逐项审查的规则。</p>
    </div>

    <div class="panel">
      <el-form :model="form" label-width="110px">
        <el-form-item label="综述类型">
          <el-select v-model="form.reviewTypeId" placeholder="选择清单适用的综述类型" style="width: 320px">
            <el-option v-for="type in reviewTypes" :key="type.id" :label="type.name" :value="type.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="清单名称">
          <el-input v-model="form.name" placeholder="例如 AME Narrative Review 审核清单" />
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="form.version" placeholder="例如 2025.6.11 或 1.0" />
        </el-form-item>
        <el-form-item label="清单文件">
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
        <el-table-column prop="name" label="清单名称" />
        <el-table-column prop="reviewType" label="综述类型" width="220" />
        <el-table-column prop="version" label="版本" width="140" />
        <el-table-column prop="active" label="启用" width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="规则" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row.id)">查看条目</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="detailVisible" title="审核清单规则" width="980px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="清单">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="综述类型">{{ detail.reviewType }}</el-descriptions-item>
        <el-descriptions-item label="版本">{{ detail.version }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.items || []" style="margin-top: 16px" border>
        <el-table-column prop="orderIndex" label="#" width="70" />
        <el-table-column prop="category" label="维度" width="140" />
        <el-table-column prop="requirement" label="审核要求" />
        <el-table-column prop="evaluationGuidance" label="判别说明" />
        <el-table-column prop="required" label="必需" width="90">
          <template #default="{ row }">
            <el-tag :type="row.required ? 'danger' : 'info'">{{ row.required ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getChecklist, importChecklist, listChecklists, listReviewTypes } from '../api/client'

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
    const [types, checklists] = await Promise.all([listReviewTypes(), listChecklists()])
    reviewTypes.value = types
    items.value = checklists
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
    ElMessage.warning('请选择综述类型和清单文件')
    return
  }
  importing.value = true
  try {
    await importChecklist({ ...form })
    ElMessage.success('解析导入完成')
    await load()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '解析导入失败，请确认已配置并启用大模型')
  } finally {
    importing.value = false
  }
}

async function showDetail(id) {
  detail.value = await getChecklist(id)
  detailVisible.value = true
}

onMounted(load)
</script>
