# UI Component Generator - Local Run Guide

This project uses JPA + MySQL.
For local setup, create the database and grant user privileges. JPA will manage tables.

## 1) Database bootstrap

Run this script in MySQL 8:

- [local_setup_mysql.sql](/d:/java/biyesheji/back/sql/local_setup_mysql.sql)

Example:

```sql
SOURCE d:/java/biyesheji/back/sql/local_setup_mysql.sql;
```

Notes:

- `spring.jpa.hibernate.ddl-auto=update` is enabled, so JPA will create/update tables automatically.
- You can inspect and manage tables in Navicat.

## 2) Manual configuration you must set

## Backend

Reference files:

- [application.yml](/d:/java/biyesheji/back/src/main/resources/application.yml)
- [back/.env.example](/d:/java/biyesheji/back/.env.example)

Required variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `LLM_API_KEY`

Recommended values:

- `DB_URL=jdbc:mysql://localhost:3306/uigen?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
- `DB_USERNAME=uigen_app`
- `DB_PASSWORD=uigen_app_123456`

Optional variables:

- `LLM_MODEL` (default `qwen3-coder-plus`)
- `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173`)
- `LLM_TIMEOUT_MS` (default `60000`)

## Frontend

Reference file:

- [front/.env.example](/d:/java/biyesheji/front/.env.example)

Required:

- `VITE_API_BASE=http://localhost:8080/api/v1`

## 3) Startup order

1. Start MySQL and run `local_setup_mysql.sql`.
2. Start backend.
3. Start frontend.

## 4) Start commands (Windows PowerShell)

Backend terminal:

```powershell
cd d:\java\biyesheji\back
$env:DB_URL="jdbc:mysql://localhost:3306/uigen?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="uigen_app"
$env:DB_PASSWORD="uigen_app_123456"
$env:LLM_API_KEY="your_dashscope_key"
mvn spring-boot:run
```

Frontend terminal:

```powershell
cd d:\java\biyesheji\front
npm install
$env:VITE_API_BASE="http://localhost:8080/api/v1"
npm run dev
```

Open:

- `http://localhost:5173`

## 5) Verification checklist

- Backend starts without schema validation errors.
- Tables are auto-created by JPA.
- Frontend can create workspace and receive SSE generation stream.
- MySQL has data in:
  - `workspace`
  - `generation_task`
  - `component_version`
  - `llm_call_log`

## 6) Common issues

- CORS error: check `CORS_ALLOWED_ORIGINS`.
- LLM auth error: check `LLM_API_KEY`.
- DB connection error: check MySQL service and DB credentials.
