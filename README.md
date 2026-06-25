# Blog Server

Spring Boot 3.x backend for the personal blog platform.

## Requirements

- JDK 17+
- Maven 3.8+
- MySQL 8.0
- Redis

## Setup

1. Create database:

```sql
CREATE DATABASE blog_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Update `src/main/resources/application.yml` with your MySQL and Redis credentials.

3. Run:

```bash
mvn spring-boot:run
```

## Default Admin

- Username: `admin`
- Password: `123456`

## API

- Swagger UI: http://localhost:8080/swagger-ui.html
- Public articles: `GET /api/articles`
- Article detail: `GET /api/articles/{slug}`
- Admin login: `POST /api/admin/login`
- Admin CRUD: `/api/admin/articles`

## OSS

Set `blog.oss.enabled=true` and configure Aliyun OSS credentials in `application.yml`.
