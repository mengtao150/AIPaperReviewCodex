# 综述稿件审查系统 MVP

技术栈：

- 前端：Vue 3 + Element Plus + Vite
- 后端：Spring Boot 3 + Spring Data JPA
- 数据库：MySQL
- 文档解析：Apache POI
- 大模型：OpenAI-compatible 可配置供应商

## 目录

- `backend/`：Spring Boot 后端
- `frontend/`：Vue 3 前端
- `docx/`：当前规则来源文件
- `docs/superpowers/specs/`：需求和设计文档
- `docs/superpowers/plans/`：实施计划

## 本机 MySQL

当前项目按本机 Windows MySQL 运行，不使用 Docker。

默认连接参数：

- host: `localhost`
- port: `3306`
- database: `aipaper_review`
- username: `root`
- password: `root`

本机已验证 `MySQL80` 服务可用，并已创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS aipaper_review
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

也可以通过环境变量覆盖：

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

## 启动后端

需要 Maven 3.9+ 和 JDK 17+。

```bash
run-backend.cmd
```

接口默认地址：

```text
http://localhost:8080/api
```

## 启动前端

```bash
run-frontend.cmd
```

前端默认使用：

```text
http://localhost:8080/api
```

如需修改：

```bash
set VITE_API_BASE_URL=http://localhost:8080/api
npm run dev
```

## RAGFlow 人工审稿经验增强

默认不开启 RAGFlow。开启后，新审稿任务会在完成结构审查和审核清单审查后，检索历史人工审稿意见，并额外生成“人工经验增强审查”结果。

需要先在 RAGFlow 中建立人工审稿意见知识库，并取得 API Key 和 Dataset ID。然后设置：

```bash
set RAGFLOW_ENABLED=true
set RAGFLOW_BASE_URL=http://localhost:9380
set RAGFLOW_API_KEY=你的RAGFlowApiKey
set RAGFLOW_DATASET_IDS=dataset-id-1,dataset-id-2
```

可选参数：

- `RAGFLOW_TOP_K`：默认 `8`
- `RAGFLOW_PAGE_SIZE`：默认 `8`
- `RAGFLOW_SIMILARITY_THRESHOLD`：默认 `0.2`
- `RAGFLOW_VECTOR_SIMILARITY_WEIGHT`：默认 `0.3`
- `RAGFLOW_TIMEOUT_SECONDS`：默认 `30`

## 使用流程

1. 确认本机 `MySQL80` 服务已启动。
2. 运行 `run-backend.cmd` 启动后端。
3. 运行 `run-frontend.cmd` 启动前端。
4. 进入“大模型配置”，填写 OpenAI-compatible 服务的 `Base URL`、`API Key`、`Model Name` 并启用。
5. 进入“稿件审查”，上传 `.docx` 综述稿件。
6. 查看类型判别、结构审查、清单审查和最终 Markdown 报告。

## 验证命令

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run build
```
