import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 180000
})

export async function uploadAudit(file) {
  const form = new FormData()
  form.append('file', file)
  const { data } = await api.post('/audits', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return data
}

export async function getAudit(id) {
  const { data } = await api.get(`/audits/${id}`)
  return data
}

export async function listAudits() {
  const { data } = await api.get('/audits')
  return data
}

export function auditReportDownloadUrl(id) {
  return `${api.defaults.baseURL}/audits/${id}/report.docx`
}

export async function listStructureTemplates() {
  const { data } = await api.get('/library/structure-templates')
  return data
}

export async function getStructureTemplate(id) {
  const { data } = await api.get(`/library/structure-templates/${id}`)
  return data
}

export async function importStructureTemplate(payload) {
  const form = new FormData()
  form.append('reviewTypeId', payload.reviewTypeId)
  form.append('name', payload.name || '')
  form.append('version', payload.version || '')
  form.append('file', payload.file)
  const { data } = await api.post('/library/structure-templates/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return data
}

export async function listChecklists() {
  const { data } = await api.get('/library/checklists')
  return data
}

export async function getChecklist(id) {
  const { data } = await api.get(`/library/checklists/${id}`)
  return data
}

export async function importChecklist(payload) {
  const form = new FormData()
  form.append('reviewTypeId', payload.reviewTypeId)
  form.append('name', payload.name || '')
  form.append('version', payload.version || '')
  form.append('file', payload.file)
  const { data } = await api.post('/library/checklists/import', form, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return data
}

export async function listReviewTypes() {
  const { data } = await api.get('/library/review-types')
  return data
}

export async function listLlmProviders() {
  const { data } = await api.get('/llm-providers')
  return data
}

export async function saveLlmProvider(payload) {
  const { data } = await api.post('/llm-providers', payload)
  return data
}

export async function activateLlmProvider(id) {
  const { data } = await api.post(`/llm-providers/${id}/activate`)
  return data
}

export async function testLlmProvider(id) {
  const { data } = await api.post(`/llm-providers/${id}/test`)
  return data
}
