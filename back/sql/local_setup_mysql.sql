-- Local bootstrap script for MySQL 8.x
-- Purpose: create database and app user only.
-- Application tables are created/updated by JPA (ddl-auto=update).

-- 1) Create database
CREATE DATABASE IF NOT EXISTS uigen
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 2) Create local app user (optional but recommended)
CREATE USER IF NOT EXISTS 'uigen_app'@'localhost' IDENTIFIED BY 'uigen_app_123456';
GRANT ALL PRIVILEGES ON uigen.* TO 'uigen_app'@'localhost';
FLUSH PRIVILEGES;

-- 3) Verify
SHOW DATABASES LIKE 'uigen';
SELECT user, host FROM mysql.user WHERE user = 'uigen_app';
