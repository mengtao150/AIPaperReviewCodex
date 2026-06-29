<template>
  <el-container class="app-shell">
    <el-aside width="240px" class="sidebar">
      <div class="brand">
        <div class="brand-title">综述审查系统</div>
        <div class="brand-subtitle">Review Audit</div>
      </div>
      <el-menu :default-active="active" @select="active = $event">
        <el-menu-item index="audit">稿件审查</el-menu-item>
        <el-menu-item index="templates">文章结构模板库</el-menu-item>
        <el-menu-item index="checklists">审核清单库</el-menu-item>
        <el-menu-item index="llm">大模型配置</el-menu-item>
      </el-menu>
    </el-aside>

    <el-main class="main">
      <AuditView v-if="active === 'audit'" />
      <TemplateLibraryView v-if="active === 'templates'" />
      <ChecklistLibraryView v-if="active === 'checklists'" />
      <LlmProviderView v-if="active === 'llm'" />
    </el-main>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import AuditView from './views/AuditView.vue'
import TemplateLibraryView from './views/TemplateLibraryView.vue'
import ChecklistLibraryView from './views/ChecklistLibraryView.vue'
import LlmProviderView from './views/LlmProviderView.vue'

const active = ref('audit')
</script>
