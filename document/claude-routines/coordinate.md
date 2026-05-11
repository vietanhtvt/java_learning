# TaskFlow Project Coordinator

Bạn là **coordinator** của dự án TaskFlow API (Java Spring Boot 3.3). Nhiệm vụ: phân tích tiến độ, xác định session hiện tại, và giao task cụ thể + prompt mẫu sẵn copy-paste.

---

## Thông tin Dự án

**Deadline:** 14/5/2026 | **Start:** 11/5/2026 | **Hôm nay:** $CURRENT_DATE  
**Stack:** Java 21, Spring Boot 3.3, PostgreSQL, Redis, Kafka, Docker, GitHub Actions  
**Repo:** `~/java_learning/taskflow/`  
**Branch dev:** `claude/sharp-darwin-Gy838`

> **Quan trọng:** Scaffold code ĐÃ TỒN TẠI trong `taskflow/src/`. Các sessions tập trung vào **VERIFY + COMPLETE + TEST**, không phải viết từ đầu.

---

## Lịch 8 Sessions (2 sessions/ngày)

| Session | Ngày | Ca | Chủ đề |
|---------|------|----|--------|
| S1 | 11/5 | Sáng | Verify Foundation: compile, entities, Flyway |
| S2 | 11/5 | Chiều | CRUD APIs: Task, Project, Comment controllers |
| S3 | 12/5 | Sáng | Spring Security + JWT auth flow |
| S4 | 12/5 | Chiều | Exception handling, Validation, Pagination |
| S5 | 13/5 | Sáng | Redis Cache + AOP Logging + Virtual Threads |
| S6 | 13/5 | Chiều | Kafka Events + Notification flow |
| S7 | 14/5 | Sáng | Testing: JUnit5, MockMvc, Testcontainers |
| S8 | 14/5 | Chiều | Docker Compose + CI/CD + Actuator + Final README |

---

## Hành động của bạn

1. Đọc file: `~/.claude/taskflow_progress.json`
2. Xác định session hiện tại dựa theo ngày + ca (sáng/chiều)
3. Hỏi user: "Session trước đã xong những task nào?"
4. Giao **đúng 3–5 task** cho session này (ưu tiên task unblock task tiếp theo)
5. Cung cấp **prompt mẫu copy-paste** cho từng task
6. Cảnh báo nếu có nguy cơ trễ deadline
7. Nhắc mẹo usage limit phù hợp với session hôm nay

---

## Prompt Mẫu Theo Session

### S1 — Verify Foundation (11/5 sáng)

**Task 1: Kiểm tra compile**
```
Tôi đang ở thư mục ~/java_learning/taskflow. 
Kiểm tra project Spring Boot 3.3 / Java 21 có compile được không.
Chạy: mvn compile -q
Nếu lỗi, fix từng lỗi một, ưu tiên missing imports và type mismatches.
Sau khi compile xanh, báo cáo danh sách các package đã có.
```

**Task 2: Verify Entities + Flyway**
```
Review các entity trong com.taskflow.entity: User, Task, Project, Comment, Notification.
Kiểm tra:
- @Entity, @Table đúng chưa?
- Relationships (@OneToMany, @ManyToOne) có fetchType LAZY chưa?
- BaseEntity có @CreatedDate, @LastModifiedDate chưa?
- Flyway V1__init.sql có đủ tables chưa?
Fix và commit bất kỳ vấn đề nào tìm thấy.
```

**Task 3: Chạy Docker Compose kiểm tra PostgreSQL + Redis**
```
Chạy: docker-compose up -d postgres redis
Kiểm tra application start được với profile dev:
  cd ~/java_learning/taskflow
  mvn spring-boot:run -Dspring-boot.run.profiles=dev &
Đợi 15s, kiểm tra http://localhost:8080/actuator/health
Báo cáo kết quả và fix nếu có lỗi startup.
```

---

### S2 — CRUD APIs (11/5 chiều)

**Task 1: Verify TaskController CRUD**
```
Review TaskController.java và TaskService.java trong dự án taskflow.
Đảm bảo có đủ endpoints:
  GET    /api/tasks?projectId=&status=&page=&size=  (pagination)
  POST   /api/tasks
  GET    /api/tasks/{id}
  PUT    /api/tasks/{id}
  DELETE /api/tasks/{id}
  PATCH  /api/tasks/{id}/status
  POST   /api/tasks/{id}/assign/{userId}
Test thủ công bằng curl sau khi start app. Fix bất kỳ 404/500 nào.
```

**Task 2: Verify ProjectController**
```
Review ProjectController.java và ProjectService.java.
Đảm bảo có:
  GET  /api/projects (my projects)
  POST /api/projects
  GET  /api/projects/{id}
  PUT  /api/projects/{id}
  POST /api/projects/{id}/members (add member)
  GET  /api/projects/{id}/members
Kiểm tra @PreAuthorize("@projectSecurity.isOwner(#id, authentication)") đúng chưa.
```

**Task 3: Verify CommentController + NotificationController**
```
Đảm bảo CommentController có:
  GET  /api/tasks/{taskId}/comments
  POST /api/tasks/{taskId}/comments
  DELETE /api/comments/{id}

NotificationController có:
  GET  /api/notifications (paged, cho current user)
  PATCH /api/notifications/{id}/read
  PATCH /api/notifications/read-all

Test toàn bộ với curl. Báo cáo response mẫu.
```

---

### S3 — Spring Security + JWT (12/5 sáng)

**Task 1: Verify Auth flow end-to-end**
```
Test toàn bộ auth flow trong dự án taskflow:
1. POST /api/auth/register — tạo user mới
2. POST /api/auth/login — nhận accessToken + refreshToken
3. GET /api/tasks (dùng Bearer token) — phải 200
4. GET /api/tasks (không có token) — phải 401
5. POST /api/auth/refresh — renew token
Review JwtUtil.java, JwtAuthFilter.java, SecurityConfig.java.
Fix bất kỳ lỗi nào trong flow.
```

**Task 2: Kiểm tra SecurityFilterChain config**
```
Trong SecurityConfig.java, verify:
- /api/auth/** được permit all
- Tất cả endpoints khác require authentication
- CSRF disabled (REST API)
- CORS config cho localhost:3000 (dev)
- SessionManagement: STATELESS
- JwtAuthFilter được add trước UsernamePasswordAuthenticationFilter
Nếu thiếu, bổ sung và test lại.
```

**Task 3: Method-level security**
```
Kiểm tra @PreAuthorize annotations trong TaskController và ProjectController:
- Chỉ OWNER của project mới được xóa project
- Chỉ MEMBER của project mới thấy tasks
- Chỉ người tạo comment mới được xóa comment
Thêm test case kiểm tra unauthorized access → 403.
```

---

### S4 — Exception Handling, Validation, Pagination (12/5 chiều)

**Task 1: Verify GlobalExceptionHandler**
```
Kiểm tra GlobalExceptionHandler.java xử lý đủ các case:
- ResourceNotFoundException → 404
- AccessDeniedException → 403
- BusinessException → 400
- MethodArgumentNotValidException → 400 với field errors
- Exception (fallback) → 500
Format response phải là: { "status": 400, "error": "...", "message": "...", "timestamp": "..." }
Test mỗi loại exception bằng curl. Fix nếu format không đúng.
```

**Task 2: Verify Validation annotations**
```
Kiểm tra validation trong các Request DTOs:
- CreateTaskRequest: title @NotBlank, dueDate @FutureDueDate (custom), priority @NotNull
- CreateProjectRequest: name @NotBlank @Size(max=100), description @Size(max=500)
- RegisterRequest: email @Email @UniqueEmail, username @UniqueUsername, password @Size(min=8)
Test POST với body thiếu field → phải nhận 400 với danh sách lỗi rõ ràng.
```

**Task 3: Verify Pagination**
```
Kiểm tra GET /api/tasks?page=0&size=10&sort=createdAt,desc
Response phải là PageResponse<TaskResponse> gồm:
  { "content": [...], "page": 0, "size": 10, "totalElements": N, "totalPages": M, "last": false }
Kiểm tra filter theo: projectId, status, assigneeId, priority.
Đảm bảo không có N+1 query (dùng @EntityGraph hoặc JOIN FETCH trong TaskRepository).
```

---

### S5 — Redis Cache + AOP Logging + Virtual Threads (13/5 sáng)

**Task 1: Verify Redis Caching**
```
Trong ProjectService.java, kiểm tra:
- getProjectById() có @Cacheable("projects") chưa?
- updateProject() có @CacheEvict(key = "#id") chưa?
- deleteProject() có @CacheEvict chưa?
Test:
  1. GET /api/projects/1 (lần 1 — hit DB)
  2. GET /api/projects/1 (lần 2 — hit cache, log phải show "cache hit")
  3. PUT /api/projects/1 — cache phải bị evict
Dùng Redis CLI để kiểm tra key: redis-cli keys "projects::*"
```

**Task 2: Verify AOP Logging**
```
Kiểm tra LoggingAspect.java:
- @Around trên tất cả methods trong package service
- Log: method name, arguments, execution time (ms)
- Log level: DEBUG cho normal, WARN nếu > 1000ms
Kiểm tra AuditAspect.java:
- @Around với @Auditable annotation
- Lưu AuditLog vào DB với: action, entityType, entityId, userId, timestamp
Gọi 1 số APIs, kiểm tra logs có xuất hiện không.
```

**Task 3: Enable Virtual Threads (Java 21)**
```
Trong application.yml, thêm:
  spring:
    threads:
      virtual:
        enabled: true

Kiểm tra TaskflowApplication.java có cấu hình virtual thread executor chưa.
Nếu chưa, thêm:
  @Bean
  public TomcatProtocolHandlerCustomizer<?> virtualThreadCustomizer() {
      return handler -> handler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
  }
Verify bằng cách log thread name trong request — phải thấy "VirtualThread".
```

---

### S6 — Kafka Events + Notification Flow (13/5 chiều)

**Task 1: Verify Kafka Producer**
```
Kiểm tra KafkaConfig.java có đúng topics chưa:
  - task-assigned
  - task-completed  
  - comment-added
Kiểm tra NotificationProducer.java:
  - sendTaskAssigned(TaskAssignedEvent event)
  - sendTaskCompleted(TaskCompletedEvent event)
  - sendCommentAdded(CommentAddedEvent event)
Mỗi method dùng kafkaTemplate.send(topic, event).
Test bằng cách assign task — Kafka console consumer phải nhận được message.
```

**Task 2: Verify Kafka Consumer + Notification**
```
Kiểm tra NotificationConsumer.java:
- @KafkaListener(topics = "task-assigned") → tạo Notification entity + lưu DB
- @KafkaListener(topics = "task-completed") → gửi thông báo cho team
- @KafkaListener(topics = "comment-added") → notify task owner
Kiểm tra toàn bộ flow:
  1. Assign task cho user B
  2. Kafka consumer nhận event
  3. Notification được tạo trong DB
  4. GET /api/notifications (user B) → phải thấy notification mới
```

**Task 3: Error handling + Dead Letter Queue**
```
Thêm error handling cho Kafka consumer:
- @RetryableTopic(attempts = 3, backoff = @Backoff(delay = 1000))
- @DltHandler method để handle failed messages
Thêm vào application.yml:
  spring.kafka.consumer.enable-auto-commit: false
  spring.kafka.listener.ack-mode: manual
Test bằng cách throw exception trong consumer — verify retry + DLT.
```

---

### S7 — Testing (14/5 sáng)

**Task 1: Fix và chạy Unit Tests**
```
Chạy: cd ~/java_learning/taskflow && mvn test -Dtest=TaskServiceTest,ProjectServiceTest -q
Fix tất cả failing tests. Đảm bảo:
- TaskServiceTest.java: cover createTask, updateTaskStatus, assignTask, deleteTask
- ProjectServiceTest.java: cover createProject, addMember, getProjectById (cache)
- Mỗi test dùng @ExtendWith(MockitoExtension.class), @Mock, @InjectMocks
- Dùng assertThat từ AssertJ
```

**Task 2: Fix và chạy Integration Tests**
```
Start Docker: docker-compose up -d postgres redis kafka
Chạy: mvn test -Dtest=AuthControllerIT,TaskControllerIT -q
Fix failing tests. Đảm bảo:
- AuthControllerIT: test register → login → access protected → refresh → logout
- TaskControllerIT: test CRUD với JWT token thật
- AbstractIntegrationTest dùng @Testcontainers với PostgreSQL + Redis containers
```

**Task 3: Test Coverage Report**
```
Chạy: mvn test jacoco:report
Mở: target/site/jacoco/index.html
Đảm bảo coverage:
  - Service layer: >= 80%
  - Controller layer: >= 70%
  - Overall: >= 65%
Nếu thiếu, bổ sung test cases cho các branch chưa cover.
Báo cáo số liệu coverage cuối cùng.
```

---

### S8 — Docker + CI/CD + Actuator + Final (14/5 chiều)

**Task 1: Verify Dockerfile + Docker Compose**
```
Kiểm tra taskflow/Dockerfile:
- Multi-stage build (builder + runtime)
- Base image: eclipse-temurin:21-jre-alpine
- EXPOSE 8080
- Non-root user

Kiểm tra docker-compose.yml (root level):
- services: postgres, redis, kafka, zookeeper, app
- Health checks cho postgres và redis
- app depends_on postgres, redis, kafka

Chạy: docker-compose up --build -d
Test: curl http://localhost:8080/actuator/health → {"status":"UP"}
```

**Task 2: GitHub Actions CI/CD**
```
Kiểm tra .github/workflows/ci.yml (tạo nếu chưa có):
  name: CI
  on: [push, pull_request]
  jobs:
    test:
      runs-on: ubuntu-latest
      services:
        postgres: postgres:16-alpine (env vars, health check)
        redis: redis:7-alpine
      steps:
        - checkout
        - setup-java 21
        - cache maven deps
        - mvn test
        - upload jacoco report
    build:
      needs: test
      steps:
        - docker build + push to ghcr.io (chỉ on main branch)
Commit file này vào branch.
```

**Task 3: Actuator + Structured Logging + Final README**
```
Verify application.yml có:
  management:
    endpoints.web.exposure.include: health,info,metrics,prometheus
    endpoint.health.show-details: when-authorized

Verify logback-spring.xml có structured JSON logging cho prod profile.

Cập nhật README.md trong thư mục taskflow:
  - Project description
  - Tech stack với versions
  - Architecture diagram (text/ASCII)
  - API endpoints table
  - Cách chạy local (docker-compose up)
  - Cách chạy tests

Commit tất cả, push lên branch, tạo final summary.
```

---

## Format Output (dùng mỗi lần gọi /coordinate)

```
📅 Hôm nay: [DATE] — Session [S?]: [Tên session]
⏱  Còn [N] ngày đến deadline (14/5)

✅ Đã xong (sessions trước):
- S?: [title]

🎯 Task session này ([S?]):
1. [Task] (~Xphút)
   → Prompt: copy đoạn trong IMPLEMENTATION_ROUTINES.md#S?-Task?

2. [Task] (~Xphút)
   → Prompt: ...

⚠️  Rủi ro: [nếu có]
💡 Usage tip: [mẹo tránh limit cho session này]
```

---

## Mẹo Quản lý Usage Limit Pro

| Quy tắc | Chi tiết |
|---------|----------|
| 1 session = 1 topic | Không gộp 2 chủ đề vào 1 lần hỏi |
| Dùng `/compact` khi > 20 messages | Giải phóng context window |
| Copy code ra IDE ngay | Không cần giữ Claude mở để test |
| Chuẩn bị prompt trước | Dùng prompt mẫu bên trên, paste ngay vào Claude mới |
| 2 sessions/ngày | Sáng 1 session, chiều 1 session — đủ dư usage |
| Fix one error at a time | Đừng paste toàn bộ stack trace dài vào 1 message |
