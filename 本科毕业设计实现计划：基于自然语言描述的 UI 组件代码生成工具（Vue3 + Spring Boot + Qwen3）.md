# 本科毕业设计实现计划：基于自然语言描述的 UI 组件代码生成工具（Vue3 + Spring Boot + Qwen3）

## Summary
- 目标闭环：`自然语言输入 -> Qwen3 流式生成 -> 在线预览 -> 历史管理 -> .vue 下载`。
- 技术选型已锁定：`Vue3 + Vite + Element Plus`、`Spring Boot`、`MySQL 8`、`DashScope OpenAI 兼容接口`、`SSE 流式输出`、`script setup + JS`。
- 安全策略：`严格过滤 + 前后端双重沙箱`，默认禁止外部依赖导入。
- 范围：V1 仅做核心闭环，不做多模型切换和版本 diff 可视化。

## Implementation Changes（前后端功能、数据库、接口）
- 前端功能：输入描述与生成参数、SSE 实时输出、代码编辑器展示、Vue SFC 沙箱预览、历史列表与详情、单文件下载、错误提示与重试。
- 后端功能：匿名工作区管理、生成任务管理、Qwen3 流式调用、代码抽取与结构化（template/script/style）、安全扫描、版本持久化、下载接口、调用日志与限流。
- 公共接口/类型新增：
  - Header：`X-Workspace-Key`（匿名隔离）。
  - 生成请求体 `GenerateRequest`：`prompt`, `componentName`, `constraints`（可选：styleHint/layoutHint/maxLines）。
  - SSE 事件类型：`started`, `token`, `partial_code`, `final_code`, `error`, `done`。
  - 版本实体 `ComponentVersionDTO`：`id`, `taskId`, `versionNo`, `vueCode`, `templateCode`, `scriptCode`, `styleCode`, `safetyResult`, `createdAt`。

### 数据库表设计（MySQL 8）
| 表名                | 关键字段                                                     | 说明               |
| ------------------- | ------------------------------------------------------------ | ------------------ |
| `workspace`         | `id`, `workspace_key(unique)`, `created_at`, `last_active_at`, `ip_hash`, `ua_hash`, `status` | 匿名用户隔离空间   |
| `generation_task`   | `id`, `workspace_id`, `prompt`, `status(PENDING/GENERATING/SUCCEEDED/FAILED)`, `model`, `error_message`, `started_at`, `finished_at` | 一次生成任务主记录 |
| `component_version` | `id`, `task_id`, `version_no`, `vue_code`, `template_code`, `script_code`, `style_code`, `safety_level`, `compile_ok`, `created_at`, `download_count` | 生成结果版本化存储 |
| `llm_call_log`      | `id`, `task_id`, `provider`, `model`, `request_tokens`, `response_tokens`, `latency_ms`, `finish_reason`, `created_at` | 调用审计与性能分析 |

### 前后端对接接口（V1）
| 方法   | 路径                                      | 说明                                |
| ------ | ----------------------------------------- | ----------------------------------- |
| `POST` | `/api/v1/workspaces/anonymous`            | 创建匿名工作区，返回 `workspaceKey` |
| `POST` | `/api/v1/generations`                     | 创建生成任务，返回 `taskId`         |
| `GET`  | `/api/v1/generations/{taskId}/stream`     | SSE 流式生成（EventSource）         |
| `GET`  | `/api/v1/generations/{taskId}`            | 查询任务状态与最新版本摘要          |
| `GET`  | `/api/v1/workspaces/me/tasks?page=&size=` | 历史任务分页列表                    |
| `GET`  | `/api/v1/generations/{taskId}/versions`   | 查看任务下版本列表                  |
| `GET`  | `/api/v1/versions/{versionId}`            | 获取某版本完整代码                  |
| `GET`  | `/api/v1/versions/{versionId}/download`   | 下载 `.vue` 文件                    |
| `POST` | `/api/v1/generations/{taskId}/regenerate` | 基于原描述再生成一个新版本          |

## 子任务拆分与 CodePlan
| 子任务                 | CodePlan（实现内容）                                         | 完成标准                                   |
| ---------------------- | ------------------------------------------------------------ | ------------------------------------------ |
| CP1 项目脚手架         | 初始化 `front`（Vue3/Vite/Element Plus/Pinia/Router）与 `back`（Spring Boot Web/JPA/Validation/SSE/MySQL/Flyway）基础工程；统一 `.env` 配置。 | 前后端可本地启动，健康检查通过。           |
| CP2 领域模型与数据库   | 落地 4 张表、索引、状态枚举、JPA 实体、Repository、Flyway migration。 | 自动建表成功，核心 CRUD 可用。             |
| CP3 生成任务与 SSE     | 实现创建任务、状态流转、SSE 通道、Qwen3 流式调用适配、超时与重试。 | 前端可实时收到 token 与结束事件。          |
| CP4 代码抽取与安全治理 | 从 LLM 输出中抽取单个 `.vue` SFC；解析 `template/script/style`；执行危险模式拦截（`eval`、远程 import、敏感 API 等）。 | 危险代码被拦截并标记失败，安全代码可入库。 |
| CP5 前端核心页面       | 实现输入页、流式输出区、代码编辑器、沙箱预览（禁外链依赖）、任务状态与错误处理。 | 用户可从描述直接看到可运行预览。           |
| CP6 历史与下载         | 历史分页、版本详情、再生成、`.vue` 下载、下载计数。          | 可追溯查看历史并下载任意版本。             |
| CP7 稳定性与可观测     | 限流、防抖、请求幂等、全局异常处理、日志追踪（taskId 贯穿）、基础性能压测。 | 并发场景下无明显阻塞，错误可定位。         |
| CP8 测试与验收         | 后端单测/集成测（任务流转、安全过滤、接口契约）；前端组件与 E2E 主流程测试。 | 主流程自动化测试通过，可用于论文演示。     |

## Test Plan
- 功能用例：描述生成成功、历史查询、版本切换、下载 `.vue`、再生成。
- 异常用例：Qwen 超时、Qwen 5xx、SSE 中断重连、非法 `taskId`、无效 `workspaceKey`。
- 安全用例：输出包含 `eval`、远程 `import`、`document.cookie`、网络请求脚本时应拦截。
- 稳定性用例：连续高频生成、并发 20+ SSE 连接、MySQL 慢查询场景下接口退化行为。
- 验收标准：核心闭环成功率高、页面可流畅预览、拦截策略有效、日志可追踪。

## Assumptions
- 仅匿名工作区，不做注册登录。
- 仅支持生成 Vue3 SFC（`script setup + JS`），下载格式仅 `.vue`。
- 预览默认禁用第三方 npm 依赖与远程脚本加载。
- Qwen3 使用 DashScope OpenAI 兼容接口，模型与密钥通过环境变量配置。
- 本计划完成后，按 CP1->CP8 顺序进入编码实现阶段。

## todo

后端接口日志打印

前端切换页面时，原来输入的内容任然保存

历史版本页面，不显示渲染页面和代码，在版本出提供一个按钮或者点击版本框触发，跳转到生成页面并显示代码和渲染页面