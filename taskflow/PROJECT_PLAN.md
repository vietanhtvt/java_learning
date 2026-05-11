# TaskFlow API — Project Plan

**Dự án:** Team Task Management REST API  
**Deadline:** 14/5/2026 | **Timeline:** 4 ngày, 8 sessions  
**Stack:** Java 21 · Spring Boot 3.3 · PostgreSQL · Redis · Kafka · Docker · GitHub Actions

---

## Mục đích

Dự án thực hành toàn bộ kỹ năng Middle Java Engineer từ README.md roadmap:
- Java Core: OOP, Generics, Collections, Concurrency, Stream API, Java 21 features
- Spring Boot: IoC/DI, AOP, MVC, Data JPA, Security, Cache, Testing
- DevOps: Docker, CI/CD, Observability

---

## Kiến trúc

```
┌─────────────────────────────────────────────────────────┐
│                    Client (curl/Swagger)                 │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP
┌─────────────────────▼───────────────────────────────────┐
│              Spring Boot 3.3 Application                 │
│                                                         │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────┐  │
│  │   Auth   │  │  Task    │  │ Project  │  │  Notif │  │
│  │Controller│  │Controller│  │Controller│  │  Ctrl  │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └───┬────┘  │
│       │             │             │             │        │
│  ┌────▼─────────────▼─────────────▼─────────────▼────┐  │
│  │              Service Layer                         │  │
│  │  AuthService · TaskService · ProjectService        │  │
│  │  CommentService · NotificationService              │  │
│  └────┬────────────────────────────┬──────────────────┘  │
│       │ JPA                        │ Kafka                │
│  ┌────▼──────────┐          ┌──────▼──────────┐          │
│  │  PostgreSQL   │          │  Apache Kafka   │          │
│  │  (Primary DB) │          │  (Events)       │          │
│  └───────────────┘          └─────────────────┘          │
│                                                         │
│  ┌────────────────┐  ┌──────────────┐                   │
│  │  Redis Cache   │  │  Spring AOP  │                   │
│  │  (projects::*) │  │  (Logging)   │                   │
│  └────────────────┘  └──────────────┘                   │
└─────────────────────────────────────────────────────────┘
```

---

## Domain Model

```
User ──────────────── UserProject ──────────────── Project
 │                   (role: OWNER/MEMBER)             │
 │                                                    │
 ├── Task (assignee) ──────────────────────── Task ◄──┘
 │    ├── Comment                             │
 │    ├── Labels                              │
 │    └── status: TODO/IN_PROGRESS/DONE       │
 │                                            │
 └── Notification ◄── Kafka ◄─ TaskAssigned ─┘
```

---

## Tech Stack & Versions

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 (LTS) |
| Framework | Spring Boot | 3.3.4 |
| Security | Spring Security + jjwt | 6.3 + 0.12.6 |
| ORM | Spring Data JPA + Hibernate | 3.3 |
| Database | PostgreSQL | 16 |
| Migration | Flyway | 10.x |
| Cache | Redis + Spring Cache | 7.x |
| Messaging | Apache Kafka | 3.7 |
| API Docs | SpringDoc OpenAPI | 2.6.0 |
| Testing | JUnit 5 + Mockito + Testcontainers | 5.11 + 5.14 + 1.20 |
| Build | Maven | 3.9 |
| Container | Docker + Compose | 27 + v2 |
| CI/CD | GitHub Actions | - |
| Observability | Spring Actuator + Micrometer | 3.3 |

---

## Package Structure

```
com.taskflow
├── TaskflowApplication.java
├── config/
│   ├── SecurityConfig.java         # SecurityFilterChain, CORS
│   ├── JwtProperties.java          # @ConfigurationProperties jwt.*
│   ├── RedisConfig.java            # CacheManager, TTL
│   ├── KafkaConfig.java            # Topics, serializers
│   ├── OpenApiConfig.java          # Swagger JWT auth
│   ├── MetricsConfig.java          # Custom Micrometer metrics
│   └── JpaAuditingConfig.java      # @EnableJpaAuditing
├── entity/
│   ├── BaseEntity.java             # @CreatedDate, @LastModifiedDate, @CreatedBy
│   ├── User.java                   # UserDetails implementation
│   ├── Project.java
│   ├── Task.java
│   ├── Comment.java
│   ├── Label.java
│   ├── UserProject.java            # Join table with role
│   ├── Notification.java
│   ├── AuditLog.java
│   ├── Role.java
│   └── enums/
│       ├── TaskStatus.java         # TODO, IN_PROGRESS, IN_REVIEW, DONE
│       ├── Priority.java           # LOW, MEDIUM, HIGH, CRITICAL
│       ├── ProjectRole.java        # OWNER, MANAGER, MEMBER, VIEWER
│       ├── ProjectStatus.java      # ACTIVE, ARCHIVED, COMPLETED
│       └── NotificationType.java
├── repository/
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   ├── TaskRepository.java         # @EntityGraph cho N+1 fix
│   ├── CommentRepository.java
│   ├── NotificationRepository.java
│   └── ...
├── service/
│   ├── AuthService.java            # register, login, refresh
│   ├── TaskService.java            # CRUD + status transitions
│   ├── ProjectService.java         # CRUD + member management (cached)
│   ├── CommentService.java
│   └── NotificationService.java
├── controller/
│   ├── AuthController.java         # /api/auth/**
│   ├── TaskController.java         # /api/tasks/**
│   ├── ProjectController.java      # /api/projects/**
│   ├── NotificationController.java # /api/notifications/**
├── security/
│   ├── JwtUtil.java                # generate, validate, parse JWT
│   ├── JwtAuthFilter.java          # OncePerRequestFilter
│   ├── UserDetailsServiceImpl.java
│   └── ProjectSecurity.java        # @PreAuthorize helper
├── dto/
│   ├── request/                    # Java Records (Java 21)
│   └── response/                   # Java Records
├── exception/
│   ├── GlobalExceptionHandler.java # @ControllerAdvice
│   ├── ResourceNotFoundException.java
│   ├── BusinessException.java
│   └── AccessDeniedException.java
├── kafka/
│   ├── NotificationProducer.java
│   ├── NotificationConsumer.java   # @RetryableTopic + @DltHandler
│   └── event/
│       ├── TaskAssignedEvent.java
│       ├── TaskCompletedEvent.java
│       └── CommentAddedEvent.java
├── aop/
│   ├── LoggingAspect.java          # method timing, slow query warn
│   ├── AuditAspect.java            # @Auditable → AuditLog
│   └── Auditable.java              # custom annotation
└── validation/
    ├── UniqueEmail.java + UniqueEmailValidator.java
    ├── UniqueUsername.java + UniqueUsernameValidator.java
    └── FutureDueDate.java + FutureDueDateValidator.java
```

---

## API Endpoints

### Auth
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | /api/auth/register | Đăng ký | No |
| POST | /api/auth/login | Đăng nhập → JWT | No |
| POST | /api/auth/refresh | Refresh access token | No |
| POST | /api/auth/logout | Blacklist refresh token | Yes |

### Tasks
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | /api/tasks | List (paginated, filterable) | Yes |
| POST | /api/tasks | Create task | Yes |
| GET | /api/tasks/{id} | Get detail | Yes |
| PUT | /api/tasks/{id} | Update | Yes |
| DELETE | /api/tasks/{id} | Delete | Owner/Creator |
| PATCH | /api/tasks/{id}/status | Change status | Member |
| POST | /api/tasks/{id}/assign/{userId} | Assign | Owner/Manager |
| GET | /api/tasks/{id}/comments | Get comments | Member |
| POST | /api/tasks/{id}/comments | Add comment | Member |

### Projects
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | /api/projects | My projects | Yes |
| POST | /api/projects | Create | Yes |
| GET | /api/projects/{id} | Detail (cached) | Member |
| PUT | /api/projects/{id} | Update | Owner |
| DELETE | /api/projects/{id} | Delete | Owner |
| POST | /api/projects/{id}/members | Add member | Owner |
| DELETE | /api/projects/{id}/members/{uid} | Remove | Owner |

### Notifications
| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | /api/notifications | My notifications (paged) | Yes |
| PATCH | /api/notifications/{id}/read | Mark read | Yes |
| PATCH | /api/notifications/read-all | Mark all read | Yes |
| GET | /api/notifications/unread-count | Unread count | Yes |

---

## Kafka Event Flow

```
User assigns Task
       │
       ▼
TaskService.assignTask()
       │
       ▼
NotificationProducer.sendTaskAssigned(TaskAssignedEvent)
       │ topic: task-assigned
       ▼
NotificationConsumer.handleTaskAssigned()   (Virtual Thread)
       │
       ├── Create Notification entity → PostgreSQL
       └── Log: "Notification sent to user {assigneeId}"

User completes Task
       │
       ▼
TaskService.updateTaskStatus(DONE)
       │
       ▼
NotificationProducer.sendTaskCompleted(TaskCompletedEvent)
       │ topic: task-completed
       ▼
NotificationConsumer.handleTaskCompleted()
       └── Notify project owner + team members
```

---

## Lịch Implementation

| Ngày | Ca | Session | Focus | Commit mục tiêu |
|------|----|---------|-------|----------------|
| 11/5 | Sáng | S1 | Verify compile + entities + startup | `feat(s1): verify foundation` |
| 11/5 | Chiều | S2 | CRUD APIs + test endpoints | `feat(s2): complete CRUD APIs` |
| 12/5 | Sáng | S3 | Spring Security + JWT end-to-end | `feat(s3): complete auth flow` |
| 12/5 | Chiều | S4 | Exception + Validation + N+1 fix | `feat(s4): exception + validation` |
| 13/5 | Sáng | S5 | Redis cache + AOP + Virtual Threads | `feat(s5): cache + aop + vthreads` |
| 13/5 | Chiều | S6 | Kafka events + notification flow | `feat(s6): kafka notifications` |
| 14/5 | Sáng | S7 | Testing (unit + integration + coverage) | `test(s7): full test suite` |
| 14/5 | Chiều | S8 | Docker + CI/CD + Actuator + README | `feat(s8): devops + done` |

---

## Cách chạy Local

```bash
# 1. Start infrastructure
docker-compose up -d postgres redis kafka zookeeper

# 2. Start app (dev profile)
cd taskflow
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 3. API docs
open http://localhost:8080/swagger-ui.html

# 4. Health check
curl http://localhost:8080/actuator/health

# 5. Quick auth test
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","email":"alice@example.com","password":"Alice1234!"}'
```

## Cách chạy Tests

```bash
# Unit tests
mvn test -Dtest="TaskServiceTest,ProjectServiceTest"

# Integration tests (cần Docker)
mvn test -Dtest="AuthControllerIT,TaskControllerIT"

# Full suite + coverage
mvn verify jacoco:report
open target/site/jacoco/index.html
```

## Docker (full stack)

```bash
docker-compose up --build -d
curl http://localhost:8080/actuator/health
# {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}
```
