# Vue Spring Review Audit MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a first-version web prototype using Vue 3 + Element Plus, Spring Boot, MySQL, and configurable LLM providers.

**Architecture:** The backend owns file upload, document extraction, review type classification, template/checklist matching, configurable LLM calls, and report assembly. The frontend is an admin-style Vue application for manuscript audit, template/checklist library management, LLM provider configuration, and report review.

**Tech Stack:** Vue 3, Vite, Element Plus, Axios, Spring Boot 3, Spring Web, Spring Data JPA, MySQL, Apache POI, PDFBox, Jackson, JUnit 5.

---

## File Structure

### Backend

- `backend/pom.xml`: Maven project and dependencies.
- `backend/src/main/resources/application.yml`: MySQL and file storage configuration.
- `backend/src/main/java/com/aipaperreview/AiPaperReviewApplication.java`: Spring Boot entrypoint.
- `backend/src/main/java/com/aipaperreview/domain/*`: JPA entities for review types, templates, checklists, LLM providers, and audit jobs.
- `backend/src/main/java/com/aipaperreview/repository/*`: Spring Data repositories.
- `backend/src/main/java/com/aipaperreview/service/document/*`: DOCX extraction.
- `backend/src/main/java/com/aipaperreview/service/llm/*`: configurable LLM provider abstraction and OpenAI-compatible implementation.
- `backend/src/main/java/com/aipaperreview/service/audit/*`: audit pipeline services.
- `backend/src/main/java/com/aipaperreview/web/*`: REST controllers and DTOs.
- `backend/src/test/java/com/aipaperreview/*`: unit tests for the audit pipeline core.

### Frontend

- `frontend/package.json`: Vite/Vue dependencies.
- `frontend/index.html`: app host page.
- `frontend/src/main.js`: Vue app bootstrap.
- `frontend/src/App.vue`: shell layout.
- `frontend/src/api/client.js`: Axios client.
- `frontend/src/views/AuditView.vue`: manuscript upload and report display.
- `frontend/src/views/TemplateLibraryView.vue`: structure template list.
- `frontend/src/views/ChecklistLibraryView.vue`: checklist list.
- `frontend/src/views/LlmProviderView.vue`: LLM provider configuration.
- `frontend/src/styles.css`: global styles.

## Tasks

### Task 1: Backend project scaffold

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/aipaperreview/AiPaperReviewApplication.java`

- [ ] Create the Maven Spring Boot project files with Web, JPA, Validation, MySQL, Apache POI, PDFBox, Jackson, and test dependencies.
- [ ] Configure MySQL connection through environment variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.
- [ ] Add the Spring Boot application entrypoint.

### Task 2: Backend domain and repositories

**Files:**
- Create: `backend/src/main/java/com/aipaperreview/domain/ReviewType.java`
- Create: `backend/src/main/java/com/aipaperreview/domain/StructureTemplate.java`
- Create: `backend/src/main/java/com/aipaperreview/domain/ChecklistTemplate.java`
- Create: `backend/src/main/java/com/aipaperreview/domain/ChecklistItem.java`
- Create: `backend/src/main/java/com/aipaperreview/domain/LlmProviderConfig.java`
- Create: `backend/src/main/java/com/aipaperreview/domain/AuditJob.java`
- Create: `backend/src/main/java/com/aipaperreview/repository/*.java`

- [ ] Write repository tests for matching active templates/checklists by review type.
- [ ] Implement JPA entities with version fields, active flags, JSON text fields, and audit timestamps.
- [ ] Implement Spring Data repositories.

### Task 3: Audit DTOs and status model

**Files:**
- Create: `backend/src/main/java/com/aipaperreview/web/dto/*.java`
- Create: `backend/src/main/java/com/aipaperreview/service/audit/model/*.java`

- [ ] Write tests for report status serialization.
- [ ] Add DTOs for audit creation, classification result, structure audit item, checklist audit item, and final report.
- [ ] Keep LLM-facing models separate from JPA entities.

### Task 4: Document extraction

**Files:**
- Create: `backend/src/test/java/com/aipaperreview/service/document/DocumentExtractorTest.java`
- Create: `backend/src/main/java/com/aipaperreview/service/document/ExtractedDocument.java`
- Create: `backend/src/main/java/com/aipaperreview/service/document/DocumentExtractor.java`
- Create: `backend/src/main/java/com/aipaperreview/service/document/DocxDocumentExtractor.java`

- [ ] Write a failing test that creates a small DOCX in memory and expects title/body extraction.
- [ ] Implement DOCX extraction using Apache POI.
- [ ] Preserve paragraph order and basic heading-like lines.

### Task 5: Classification rules and audit services

**Files:**
- Create: `backend/src/test/java/com/aipaperreview/service/audit/TemplateMatcherTest.java`
- Create: `backend/src/test/java/com/aipaperreview/service/audit/ReportBuilderTest.java`
- Create: `backend/src/main/java/com/aipaperreview/service/audit/ClassificationRuleProvider.java`
- Create: `backend/src/main/java/com/aipaperreview/service/audit/TemplateMatcher.java`
- Create: `backend/src/main/java/com/aipaperreview/service/audit/ChecklistMatcher.java`
- Create: `backend/src/main/java/com/aipaperreview/service/audit/ReportBuilder.java`

- [ ] Write failing tests for selecting active templates/checklists by detected review type.
- [ ] Implement the hardcoded first-version Review classification flowchart text.
- [ ] Implement template/checklist matching.
- [ ] Implement Markdown report assembly.

### Task 6: Configurable LLM provider layer

**Files:**
- Create: `backend/src/test/java/com/aipaperreview/service/llm/OpenAiCompatibleLlmClientTest.java`
- Create: `backend/src/main/java/com/aipaperreview/service/llm/LlmClient.java`
- Create: `backend/src/main/java/com/aipaperreview/service/llm/LlmRequest.java`
- Create: `backend/src/main/java/com/aipaperreview/service/llm/LlmResponse.java`
- Create: `backend/src/main/java/com/aipaperreview/service/llm/OpenAiCompatibleLlmClient.java`
- Create: `backend/src/main/java/com/aipaperreview/service/llm/LlmProviderService.java`

- [ ] Write a failing test that verifies OpenAI-compatible request payload construction.
- [ ] Implement configurable `baseUrl`, `apiKey`, `modelName`, `temperature`, `maxTokens`, and timeout.
- [ ] Keep provider configuration in database and select the active provider for audit jobs.

### Task 7: Audit orchestration and REST API

**Files:**
- Create: `backend/src/main/java/com/aipaperreview/service/audit/AuditOrchestrator.java`
- Create: `backend/src/main/java/com/aipaperreview/web/AuditController.java`
- Create: `backend/src/main/java/com/aipaperreview/web/LibraryController.java`
- Create: `backend/src/main/java/com/aipaperreview/web/LlmProviderController.java`

- [ ] Write controller tests for provider CRUD and audit job creation.
- [ ] Implement upload endpoint: `POST /api/audits`.
- [ ] Implement report endpoint: `GET /api/audits/{id}`.
- [ ] Implement library endpoints for review types, structure templates, checklists, and LLM providers.

### Task 8: Seed data

**Files:**
- Create: `backend/src/main/resources/data.sql`

- [ ] Add seed review types from the flowchart.
- [ ] Add a baseline structure template from `文章结构.docx`.
- [ ] Add Narrative Review, Mini Review, and Clinical Practice Review checklist examples from `综述清单.pdf`.

### Task 9: Frontend scaffold

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/index.html`
- Create: `frontend/src/main.js`
- Create: `frontend/src/App.vue`
- Create: `frontend/src/styles.css`
- Create: `frontend/src/api/client.js`

- [ ] Create Vue 3 + Vite + Element Plus scaffold.
- [ ] Add API base URL configuration through `VITE_API_BASE_URL`.
- [ ] Add main layout navigation.

### Task 10: Frontend pages

**Files:**
- Create: `frontend/src/views/AuditView.vue`
- Create: `frontend/src/views/TemplateLibraryView.vue`
- Create: `frontend/src/views/ChecklistLibraryView.vue`
- Create: `frontend/src/views/LlmProviderView.vue`

- [ ] Implement upload and report display page.
- [ ] Implement template library list.
- [ ] Implement checklist library list.
- [ ] Implement LLM provider configuration form.

### Task 11: Verification

**Commands:**
- Backend: `mvn test` from `backend`
- Frontend install: `npm install` from `frontend`
- Frontend build: `npm run build` from `frontend`

**Note:** This environment currently exposes Java/Javac and Node/NPM, but not Maven/Gradle. If Maven remains unavailable, backend compilation and tests cannot be executed locally until Maven or Maven Wrapper dependencies are available.

