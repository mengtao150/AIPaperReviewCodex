<template>
  <section>
    <div class="page-header">
      <h1 class="page-title">大模型配置</h1>
      <p class="page-desc">支持 OpenAI-compatible 接口：填写 Base URL、API Key 和模型名称即可接入多种供应商。</p>
    </div>

    <div class="panel">
      <el-form :model="form" label-width="130px">
        <el-form-item label="配置名称">
          <el-input v-model="form.name" placeholder="例如 DeepSeek / OpenAI / 私有模型" />
        </el-form-item>
        <el-form-item label="接口类型">
          <el-input v-model="form.providerType" />
        </el-form-item>
        <el-form-item label="Base URL">
          <el-input v-model="form.baseUrl" placeholder="https://api.example.com/v1" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password />
        </el-form-item>
        <el-form-item label="Model Name">
          <el-input v-model="form.modelName" placeholder="例如 gpt-4.1 / deepseek-chat" />
        </el-form-item>
        <el-form-item label="Temperature">
          <el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" />
        </el-form-item>
        <el-form-item label="Max Tokens">
          <el-input-number v-model="form.maxTokens" :min="512" :max="200000" :step="512" />
        </el-form-item>
        <el-form-item label="Timeout 秒">
          <el-input-number v-model="form.timeoutSeconds" :min="10" :max="600" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.active">保存后启用</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
          <el-button :loading="loading" @click="load">刷新列表</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="panel" style="margin-top: 18px">
      <el-table :data="providers" border>
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="providerType" label="类型" width="180" />
        <el-table-column prop="baseUrl" label="Base URL" />
        <el-table-column prop="modelName" label="模型" width="180" />
        <el-table-column prop="active" label="启用" width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190">
          <template #default="{ row }">
            <el-button size="small" :disabled="row.active" @click="activate(row.id)">启用</el-button>
            <el-button size="small" type="primary" :loading="testing[row.id]" @click="testProvider(row.id)">
              测试
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { activateLlmProvider, listLlmProviders, saveLlmProvider, testLlmProvider } from '../api/client'

const providers = ref([])
const loading = ref(false)
const saving = ref(false)
const testing = reactive({})
const form = reactive({
  name: '',
  providerType: 'OPENAI_COMPATIBLE',
  baseUrl: '',
  apiKey: '',
  modelName: '',
  temperature: 0.1,
  maxTokens: 4096,
  timeoutSeconds: 120,
  active: true
})

async function load() {
  loading.value = true
  try {
    providers.value = await listLlmProviders()
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await saveLlmProvider({ ...form })
    ElMessage.success('已保存')
    await load()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function activate(id) {
  await activateLlmProvider(id)
  await load()
}

async function testProvider(id) {
  testing[id] = true
  try {
    const result = await testLlmProvider(id)
    if (result.success) {
      ElMessage.success(`测试成功，耗时 ${result.durationMs} ms`)
      await ElMessageBox.alert(result.content || '模型已返回空内容', '模型返回内容', {
        confirmButtonText: '知道了'
      })
    } else {
      ElMessage.error('测试失败')
      await ElMessageBox.alert(result.errorMessage || '模型未返回错误详情', '测试失败', {
        confirmButtonText: '知道了'
      })
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || error.message || '测试请求失败')
  } finally {
    testing[id] = false
  }
}

onMounted(load)
</script>
