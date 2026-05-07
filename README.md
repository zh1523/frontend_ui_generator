# UI 组件生成器 - 本地运行指南

本项目使用 JPA + MySQL。
本地搭建时，请先创建数据库并授予用户权限，JPA 会自动管理表结构。

## 1) 数据库初始化

在 MySQL 8 中运行以下脚本：

- [local_setup_mysql.sql](/d:/java/biyesheji/back/sql/local_setup_mysql.sql)

示例：

```sql
SOURCE d:/java/biyesheji/back/sql/local_setup_mysql.sql;
```

说明：

- 已启用 `spring.jpa.hibernate.ddl-auto=update`，因此 JPA 会自动创建/更新表结构。
- 可以在 Navicat 中查看和管理表。

## 2) 必须手动配置的项

### 后端

参考文件：

- [application.yml](/d:/java/biyesheji/back/src/main/resources/application.yml)
- [back/.env.example](/d:/java/biyesheji/back/.env.example)

必填变量：

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `LLM_API_KEY`

推荐值：

- `DB_URL=jdbc:mysql://localhost:3306/uigen?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
- `DB_USERNAME=uigen_app`
- `DB_PASSWORD=uigen_app_123456`

可选变量：

- `LLM_MODEL`（默认 `qwen3-coder-plus`）
- `CORS_ALLOWED_ORIGINS`（默认 `http://localhost:5173`）
- `LLM_TIMEOUT_MS`（默认 `60000`）

### 前端

参考文件：

- [front/.env.example](/d:/java/biyesheji/front/.env.example)

必填：

- `VITE_API_BASE=http://localhost:8080/api/v1`

## 3) 启动顺序

1. 启动 MySQL 并运行 `local_setup_mysql.sql`。
2. 启动后端。
3. 启动前端。

## 4) 启动命令（Windows PowerShell）

后端终端：

```powershell
cd d:\java\biyesheji\back
$env:DB_URL="jdbc:mysql://localhost:3306/uigen?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="uigen_app"
$env:DB_PASSWORD="uigen_app_123456"
$env:LLM_API_KEY="your_dashscope_key"
mvn spring-boot:run
```

前端终端：

```powershell
cd d:\java\biyesheji\front
npm install
$env:VITE_API_BASE="http://localhost:8080/api/v1"
npm run dev
```

打开：

- `http://localhost:5173`

## 5) 验证清单

- 后端启动无 schema 校验错误。
- JPA 自动创建表。
- 前端能创建工作区并接收 SSE 生成流。
- MySQL 中以下表有数据：
  - `workspace`
  - `generation_task`
  - `component_version`
  - `llm_call_log`

## 6) 常见问题

- CORS 错误：检查 `CORS_ALLOWED_ORIGINS`。
- LLM 认证错误：检查 `LLM_API_KEY`。
- 数据库连接错误：检查 MySQL 服务和数据库凭据。
