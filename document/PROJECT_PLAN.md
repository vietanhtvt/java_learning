# TaskFlow API — Kế hoạch Dự án Java Spring Boot

> Dự án thực hành toàn bộ kỹ thuật trong learning roadmap, hoàn thành trong 4 ngày.

## Tổng quan

| Mục | Nội dung |
|-----|----------|
| Tên dự án | TaskFlow API |
| Mô tả | Hệ thống quản lý công việc nhóm (Task Management) |
| Deadline | 14/5/2026 |
| Thời gian | 4 ngày × 2 sessions/ngày = 8 sessions |
| **Trạng thái** | **✅ Hoàn thành (10/5/2026)** |

## Stack Công Nghệ

| Layer | Công nghệ | Trạng thái |
|-------|-----------|-----------|
| Language | Java 21 (Virtual Threads, Records, Pattern Matching) | ✅ |
| Framework | Spring Boot 3.3.x | ✅ |
| Database | PostgreSQL 16 + Spring Data JPA / Hibernate 6 | ✅ |
| Cache | Redis 7 + Spring Cache | ✅ |
| Security | Spring Security 6 + JWT (JJWT 0.12) | ✅ |
| Messaging | Apache Kafka | ✅ |
| Testing | JUnit 5 + Mockito + Testcontainers | ✅ |
| Monitoring | Spring Actuator + Micrometer | ✅ |
| Container | Docker + Docker Compose | ✅ |
| CI/CD | GitHub Actions | ✅ |
| Docs | SpringDoc OpenAPI 3 (Swagger UI) | ✅ |

## Domain Model

```
User ──< UserProject >── Project ──< Task
                                      │
                                      ├── Comment
                                      └── Label
                                      
Task ──> Notification (via Kafka)
AuditLog (via AOP @Auditable)
```

**Entities:** `User`, `Project`, `Task`, `Comment`, `Label`, `Notification`, `AuditLog`

---

## Lịch 8 Sessions

### Ngày 1 — 10/5 (Foundation) ✅

#### Session 1 (Sáng) — Project Bootstrap

Checklist:
- [x] `spring init` với dependencies: Web, JPA, PostgreSQL, Security, Redis, Kafka, Actuator, Validation, Lombok, Flyway
- [x] Package structure: `controller / service / repository / entity / dto / config / exception / aop / kafka / security / validation`
- [x] `BaseEntity` với `@EntityListeners(AuditingEntityListener.class)` — UUID PK, createdAt, updatedAt, createdBy, updatedBy
- [x] Entities: `User`, `Role`, `Project`, `UserProject`
- [x] Flyway: `V1__init.sql` (full schema + indexes), `V2__seed_roles.sql`

#### Session 2 (Chiều) — Task Domain + CRUD APIs

Checklist:
- [x] Entities: `Task` (status, priority, dueDate, assignee), `Comment`, `Label`
- [x] Repository với custom JPQL queries (findByProjectAndStatus, findOverdueTasks, findByProjectWithFilters, findByIdWithDetails)
- [x] Service layer: `ProjectService`, `TaskService`, `CommentService`
- [x] DTOs (Request/Response) — Java Records
- [x] Controllers: `GET/POST /api/projects`, `GET/POST/PUT/DELETE /api/tasks/{id}`

---

### Ngày 2 — 11/5 (Security & API) ✅

#### Session 3 (Sáng) — Spring Security + JWT

Checklist:
- [x] `SecurityConfig` (STATELESS, filterChain, `DaoAuthenticationProvider`)
- [x] `JwtUtil`: generate/validate/extract — JJWT 0.12 new API (`Jwts.builder().claims()`, `Jwts.parser().verifyWith()`)
- [x] `JwtAuthFilter` — `OncePerRequestFilter`, blocks refresh tokens
- [x] `AuthController`: register, login (`/api/auth/login`), refresh token (`/api/auth/refresh`)
- [x] `@PreAuthorize("@projectSecurity.isOwner(#projectId, authentication)")` — SpEL + custom bean
- [x] CORS — `allowedOriginPatterns("*")` (tránh conflict với `allowCredentials`)

#### Session 4 (Chiều) — Exception Handling + Validation

Checklist:
- [x] `GlobalExceptionHandler` → RFC 7807 `ProblemDetail` — `ResourceNotFoundException` (404), `AccessDeniedException` (403), `BusinessException` (custom status), validation errors (422 với field map)
- [x] Custom exception hierarchy: `ResourceNotFoundException`, `AccessDeniedException`, `BusinessException`
- [x] Bean Validation trên tất cả Request DTOs (`@NotBlank`, `@Size`, `@NotNull`, `@Email`)
- [x] Custom validators: `@UniqueEmail` (UniqueEmailValidator), `@UniqueUsername` (UniqueUsernameValidator), `@FutureDueDate` (FutureDueDateValidator)
- [x] Pagination: `Pageable` + `@PageableDefault` + `PageResponse<T>` wrapper

---

### Ngày 3 — 12/5 (Advanced Features) ✅

#### Session 5 (Sáng) — Redis Cache + AOP

Checklist:
- [x] `RedisConfig` với `RedisCacheManager`, TTL per cache (projects: 10m, tasks: 5m, users: 30m), JSON serialization với type info
- [x] Cache: `@Cacheable` trên getProject/getTask, `@CacheEvict` trên update/delete
- [x] `LoggingAspect`: `@Around` service methods — log method name, args, execution time, exceptions
- [x] `AuditAspect`: `@Around @Auditable` — ghi vào bảng `audit_log` với `@Async` + `Propagation.REQUIRES_NEW`

#### Session 6 (Chiều) — Kafka + Notifications

Checklist:
- [x] `KafkaConfig` (producer + consumer, topics với partitions)
- [x] Event classes (Java Records): `TaskAssignedEvent`, `TaskCompletedEvent`, `CommentAddedEvent`
- [x] `NotificationProducer` — `KafkaTemplate.send()` + `CompletableFuture.whenComplete` callback, taskId làm partition key
- [x] `NotificationConsumer` — `@KafkaListener`, `@Transactional`, Virtual Threads (`spring.threads.virtual.enabled=true`)
- [x] `Notification` entity + API:
  - `GET /api/notifications` — danh sách có pagination
  - `GET /api/notifications/unread-count` — đếm chưa đọc
  - `PATCH /api/notifications/{id}/read` — đánh dấu đã đọc
  - `PATCH /api/notifications/read-all` — đánh dấu tất cả đã đọc

---

### Ngày 4 — 13/5 (Testing + DevOps) ✅

#### Session 7 (Sáng) — Testing

Checklist:
- [x] `TaskServiceTest` — Mockito unit tests, BDD `given/then`, `@Nested` classes, `ArgumentCaptor<TaskAssignedEvent>`
- [x] `ProjectServiceTest` — Mockito unit tests (bonus, không có trong plan gốc)
- [x] `AbstractIntegrationTest` — Testcontainers shared static containers (PostgreSQL, Redis, Kafka), `@DynamicPropertySource`
- [x] `AuthControllerIT` — end-to-end auth flow: register → login → access, duplicate email (422), wrong password (401), refresh token
- [x] `TaskControllerIT` — CRUD qua HTTP, pagination, add comment, non-member (403)
- [x] `CacheIT` — Redis cache hit/miss/eviction verification
- [x] `application-test.yml` — Testcontainers-compatible config

#### Session 8 (Chiều) — Docker + CI/CD

Checklist:
- [x] `Dockerfile` multi-stage (eclipse-temurin:21-jdk-alpine → 21-jre-alpine), non-root user, `--mount=type=cache` cho Maven, `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`
- [x] `docker-compose.yml` — app + postgres + redis + kafka + zookeeper + kafka-ui (`--profile dev`), `condition: service_healthy`, named volumes
- [x] `.github/workflows/ci.yml` — 2 jobs: (1) compile → unit tests → integration tests → upload artifacts; (2) Docker build & push GHCR với tags `sha-*`, `master`, `latest`
- [x] Custom Micrometer metrics: `taskflow.tasks.created` (Counter), `taskflow.tasks.completed` (Counter), `taskflow.tasks.in_progress` (Gauge)
- [x] Swagger UI — SpringDoc OpenAPI 3, `OpenApiConfig` với JWT Bearer `SecurityScheme`, `@Operation`/`@Tag` trên tất cả controllers
- [x] Jacoco coverage plugin (70% minimum line coverage cho `com.taskflow.service.*`)

---

## Topics Coverage (từ README)

| Topic | Session | Trạng thái |
|-------|---------|-----------|
| Records, Virtual Threads, Pattern Matching | S6 | ✅ |
| Collections, Stream API | S1-S2 | ✅ |
| CompletableFuture | S6 | ✅ |
| IoC, DI, Bean Lifecycle | S1 | ✅ |
| Spring AOP | S5 | ✅ |
| Spring MVC, REST, Validation | S2, S4 | ✅ |
| Spring Data JPA, Relations, Fetch strategies | S1-S2 | ✅ |
| Transactions | S2 | ✅ |
| Pagination | S4 | ✅ |
| Spring Security, JWT | S3 | ✅ |
| Method-level security | S3 | ✅ |
| CSRF, CORS | S3 | ✅ |
| Redis Cache | S5 | ✅ |
| JUnit 5, Mockito, SpringBootTest | S7 | ✅ |
| Testcontainers | S7 | ✅ |
| Actuator | S8 | ✅ |
| Docker | S8 | ✅ |
| CI/CD | S8 | ✅ |
| Kafka | S6 | ✅ |
| SLF4J, Logback | S5 | ✅ |

---

## Tổng kết Implementation

### Files chính

| Layer | Files |
|-------|-------|
| **Entity** | `User`, `Role`, `Project`, `UserProject`, `Task`, `Comment`, `Label`, `Notification`, `AuditLog`, `BaseEntity` |
| **Repository** | `UserRepository`, `ProjectRepository`, `TaskRepository`, `CommentRepository`, `LabelRepository`, `NotificationRepository`, `AuditLogRepository`, `RoleRepository`, `UserProjectRepository` |
| **Service** | `AuthService`, `ProjectService`, `TaskService`, `CommentService`, `NotificationService` |
| **Controller** | `AuthController`, `ProjectController`, `TaskController`, `NotificationController` |
| **Security** | `SecurityConfig`, `JwtUtil`, `JwtAuthFilter`, `UserDetailsServiceImpl`, `ProjectSecurity` |
| **Config** | `RedisConfig`, `KafkaConfig`, `MetricsConfig`, `OpenApiConfig`, `JwtProperties`, `JpaAuditingConfig` |
| **AOP** | `LoggingAspect`, `AuditAspect`, `@Auditable` |
| **Kafka** | `NotificationProducer`, `NotificationConsumer`, `TaskAssignedEvent`, `TaskCompletedEvent`, `CommentAddedEvent` |
| **Validation** | `@UniqueEmail`, `@UniqueUsername`, `@FutureDueDate` + validators |
| **Exception** | `GlobalExceptionHandler`, `ResourceNotFoundException`, `AccessDeniedException`, `BusinessException` |
| **Tests** | `TaskServiceTest`, `ProjectServiceTest`, `AbstractIntegrationTest`, `AuthControllerIT`, `TaskControllerIT`, `CacheIT` |
| **DevOps** | `Dockerfile`, `docker-compose.yml`, `.github/workflows/ci.yml` |

### REST API Endpoints

| Method | URL | Mô tả | Auth |
|--------|-----|-------|------|
| POST | `/api/auth/register` | Đăng ký tài khoản | Public |
| POST | `/api/auth/login` | Đăng nhập, lấy JWT | Public |
| POST | `/api/auth/refresh` | Làm mới access token | Public |
| GET | `/api/projects` | Danh sách project của user | Required |
| POST | `/api/projects` | Tạo project | Required |
| GET | `/api/projects/{id}` | Chi tiết project | Required |
| PUT | `/api/projects/{id}` | Cập nhật project | Owner only |
| DELETE | `/api/projects/{id}` | Xóa project | Owner only |
| POST | `/api/projects/{id}/members` | Thêm thành viên | Owner only |
| DELETE | `/api/projects/{id}/members/{userId}` | Xóa thành viên | Owner only |
| GET | `/api/projects/{id}/tasks` | Danh sách task (có filter + pagination) | Member |
| POST | `/api/projects/{id}/tasks` | Tạo task | Member |
| GET | `/api/tasks/{id}` | Chi tiết task | Member |
| PUT | `/api/tasks/{id}` | Cập nhật task | Member |
| DELETE | `/api/tasks/{id}` | Xóa task | Member |
| GET | `/api/tasks/overdue` | Danh sách task quá hạn | Required |
| GET | `/api/tasks/my` | Task được assign cho tôi | Required |
| POST | `/api/tasks/{id}/comments` | Thêm comment | Member |
| GET | `/api/notifications` | Danh sách thông báo | Required |
| GET | `/api/notifications/unread-count` | Số thông báo chưa đọc | Required |
| PATCH | `/api/notifications/{id}/read` | Đánh dấu đã đọc | Required |
| PATCH | `/api/notifications/read-all` | Đánh dấu tất cả đã đọc | Required |
