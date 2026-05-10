# TaskFlow API — Kế hoạch Dự án Java Spring Boot

> Dự án thực hành toàn bộ kỹ thuật trong learning roadmap, hoàn thành trong 4 ngày.

## Tổng quan

| Mục | Nội dung |
|-----|----------|
| Tên dự án | TaskFlow API |
| Mô tả | Hệ thống quản lý công việc nhóm (Task Management) |
| Deadline | 14/5/2026 |
| Thời gian | 4 ngày × 2 sessions/ngày = 8 sessions |

## Stack Công Nghệ

| Layer | Công nghệ |
|-------|-----------|
| Language | Java 21 (Virtual Threads, Records, Pattern Matching) |
| Framework | Spring Boot 3.3.x |
| Database | PostgreSQL 16 + Spring Data JPA / Hibernate 6 |
| Cache | Redis 7 + Spring Cache |
| Security | Spring Security 6 + JWT (JJWT 0.12) |
| Messaging | Apache Kafka |
| Testing | JUnit 5 + Mockito + Testcontainers |
| Monitoring | Spring Actuator + Micrometer |
| Container | Docker + Docker Compose |
| CI/CD | GitHub Actions |
| Docs | SpringDoc OpenAPI 3 (Swagger UI) |

## Domain Model

```
User ──< UserProject >── Project ──< Task
                                      │
                                      ├── Comment
                                      └── Label
                                      
Task ──> Notification (via Kafka)
```

**Entities:** `User`, `Project`, `Task`, `Comment`, `Label`, `Notification`

---

## Lịch 8 Sessions

### Ngày 1 — 10/5 (Foundation)

#### Session 1 (Sáng) — Project Bootstrap
**Prompt mẫu:**
```
Init Spring Boot 3.3 project TaskFlow API. Setup PostgreSQL + Flyway migration,
BaseEntity với auditing (createdAt, updatedAt, createdBy).
Tạo User + Role + Project + UserProject entities với JPA relations đầy đủ.
Package: com.taskflow
```

Checklist:
- [ ] `spring init` với dependencies: Web, JPA, PostgreSQL, Security, Redis, Kafka, Actuator, Validation, Lombok, Flyway
- [ ] Package structure: `controller / service / repository / entity / dto / config / exception`
- [ ] `BaseEntity` với `@EntityListeners(AuditingEntityListener.class)`
- [ ] Entities: `User`, `Role`, `Project`, `UserProject`
- [ ] Flyway: `V1__init.sql`, `V2__seed_roles.sql`

#### Session 2 (Chiều) — Task Domain + CRUD APIs
**Prompt mẫu:**
```
Implement Task entity với @ManyToOne Project, @OneToMany Comments, enum TaskStatus/Priority.
Repository với custom JPQL (findByProjectAndStatus, findOverdueTasks).
ProjectService + TaskService với full CRUD. DTO mapping. REST Controllers.
```

Checklist:
- [ ] Entities: `Task` (status, priority, dueDate, assignee), `Comment`, `Label`
- [ ] Repository với custom queries
- [ ] Service layer: `ProjectService`, `TaskService`
- [ ] DTOs (Request/Response)
- [ ] Controllers: `/api/projects`, `/api/tasks`

---

### Ngày 2 — 11/5 (Security & API)

#### Session 3 (Sáng) — Spring Security + JWT
**Prompt mẫu:**
```
Spring Security 6 STATELESS với JWT (JJWT 0.12 API mới).
UserDetailsService, JwtAuthFilter extends OncePerRequestFilter.
AuthController: /auth/register, /auth/login, /auth/refresh.
@PreAuthorize custom expression trên TaskController.
CORS config cho frontend.
```

Checklist:
- [ ] `SecurityConfig` (STATELESS, filterChain)
- [ ] `JwtUtil`: generate/validate/extract (JJWT 0.12)
- [ ] `JwtAuthFilter`
- [ ] `AuthController`: register, login, refresh token
- [ ] `@PreAuthorize("@projectSecurity.isOwner(#id)")`
- [ ] CORS configuration

#### Session 4 (Chiều) — Exception Handling + Validation
**Prompt mẫu:**
```
@RestControllerAdvice global handler trả về RFC 7807 Problem Details format.
Custom exceptions: ResourceNotFoundException, AccessDeniedException, BusinessException.
@Valid + custom constraints: @UniqueEmail, @FutureDueDate.
Pagination Pageable + Page<TaskResponse> cho task list API.
```

Checklist:
- [ ] `GlobalExceptionHandler` → RFC 7807 Problem Details
- [ ] Custom exception hierarchy
- [ ] Bean Validation trên tất cả Request DTOs
- [ ] Custom validators
- [ ] Pagination: `Pageable` + `Page<TaskResponse>`

---

### Ngày 3 — 12/5 (Advanced Features)

#### Session 5 (Sáng) — Redis Cache + AOP
**Prompt mẫu:**
```
Redis caching: RedisCacheManager với custom TTL per cache.
@Cacheable trên project/task details, @CacheEvict khi update/delete.
AOP @Aspect: LoggingAspect (execution time), AuditAspect (@Around ghi audit_log).
```

Checklist:
- [ ] `RedisConfig` với `RedisCacheManager`, TTL config
- [ ] Cache: `projects::id`, `tasks::projectId`
- [ ] `LoggingAspect`: log method + args + execution time
- [ ] `AuditAspect`: ghi vào bảng `audit_log`

#### Session 6 (Chiều) — Kafka + Notifications
**Prompt mẫu:**
```
Kafka producer/consumer. Events: TaskAssignedEvent, TaskCompletedEvent, CommentAddedEvent.
NotificationProducer publish khi task thay đổi.
NotificationConsumer (Virtual Threads Java 21) tạo Notification entity.
API: /notifications (unread count, mark as read).
spring.threads.virtual.enabled=true
```

Checklist:
- [ ] `KafkaConfig` (producer + consumer)
- [ ] Event classes (records)
- [ ] `NotificationProducer`
- [ ] `NotificationConsumer` với Virtual Threads
- [ ] `Notification` entity + API

---

### Ngày 4 — 13/5 (Testing + DevOps)

#### Session 7 (Sáng) — Testing
**Prompt mẫu:**
```
Unit tests TaskService với Mockito (mock repository).
Integration tests @SpringBootTest + Testcontainers (PostgreSQL, Redis, Kafka).
AuthControllerIT: register→login→access flow.
CacheIT: verify Redis cache hit/miss.
Target: 70% coverage service layer.
```

Checklist:
- [ ] `TaskServiceTest` — Mockito unit tests
- [ ] `AuthControllerIT` — end-to-end auth flow
- [ ] `TaskControllerIT` — CRUD với Testcontainers
- [ ] `CacheIT` — Redis cache verification

#### Session 8 (Chiều) — Docker + CI/CD
**Prompt mẫu:**
```
Dockerfile multi-stage build (build → JRE 21-slim).
docker-compose.yml: app + postgres + redis + kafka + zookeeper.
GitHub Actions CI/CD: compile → test → docker build → push GHCR.
Actuator health + custom Micrometer metric (task completion rate).
SpringDoc OpenAPI Swagger UI.
```

Checklist:
- [ ] `Dockerfile` multi-stage
- [ ] `docker-compose.yml` full stack
- [ ] `.github/workflows/ci.yml`
- [ ] Custom Micrometer metric
- [ ] Swagger UI đầy đủ

---

## Topics Coverage (từ README)

| Topic | Session |
|-------|---------|
| Records, Virtual Threads, Pattern Matching | S6 |
| Collections, Stream API | S1-S2 |
| CompletableFuture | S6 |
| IoC, DI, Bean Lifecycle | S1 |
| Spring AOP | S5 |
| Spring MVC, REST, Validation | S2, S4 |
| Spring Data JPA, Relations, Fetch strategies | S1-S2 |
| Transactions | S2 |
| Pagination | S4 |
| Spring Security, JWT | S3 |
| Method-level security | S3 |
| CSRF, CORS | S3 |
| Redis Cache | S5 |
| JUnit 5, Mockito, SpringBootTest | S7 |
| Testcontainers | S7 |
| Actuator | S8 |
| Docker | S8 |
| CI/CD | S8 |
| Kafka | S6 |
| SLF4J, Logback | S5 |
