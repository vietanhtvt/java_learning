# Implementation Routines — TaskFlow API

Tài liệu này dành cho **implementing routine** — session Claude chuyên viết/fix/verify code.  
Coordinator routine (file `coordinate.md`) sẽ điều phối và trỏ đến các section bên dưới.

---

## Tổng quan

- **Dự án:** TaskFlow API — Team Task Management System
- **Repo local:** `~/java_learning/taskflow/`
- **Branch:** `claude/sharp-darwin-Gy838`
- **Timeline:** 11/5 → 14/5/2026 (4 ngày, 8 sessions)
- **Trạng thái scaffold:** Đã có `src/` đầy đủ — nhiệm vụ là VERIFY + COMPLETE + TEST

---

## Kỹ thuật & Chủ đề từ README.md được cover trong dự án

| README Topic | TaskFlow Implementation |
|-------------|------------------------|
| OOP, Encapsulation | Entity hierarchy, BaseEntity, service interfaces |
| Generics | `PageResponse<T>`, Repository generics |
| Exception Handling | `GlobalExceptionHandler`, custom exceptions |
| Collections & Stream API | Service layer: filter/map tasks, Stream processing |
| Concurrency / Virtual Threads | Java 21 VirtualThread executor cho Tomcat |
| Functional Interfaces | Predicate filters trong TaskRepository queries |
| JPA / Hibernate | Entity mapping, relationships, LAZY fetch |
| N+1 Problem | `@EntityGraph` trong TaskRepository |
| Transaction | `@Transactional` propagation trong services |
| Spring IoC / DI | Constructor injection throughout |
| Spring AOP | `LoggingAspect`, `AuditAspect` |
| Spring Security + JWT | `SecurityConfig`, `JwtUtil`, `JwtAuthFilter` |
| Redis Cache | `@Cacheable` / `@CacheEvict` trong ProjectService |
| Kafka Messaging | `NotificationProducer` + `NotificationConsumer` |
| Spring MVC REST | 4 controllers, `ResponseEntity`, `@ControllerAdvice` |
| Validation | Custom validators: `@UniqueEmail`, `@FutureDueDate` |
| Pagination | `Pageable`, `PageRequest`, `PageResponse<T>` |
| JUnit 5 + Mockito | Unit tests: `TaskServiceTest`, `ProjectServiceTest` |
| MockMvc Integration Test | `AuthControllerIT`, `TaskControllerIT` |
| Testcontainers | `AbstractIntegrationTest` với PostgreSQL + Redis |
| Docker | Multi-stage Dockerfile, docker-compose |
| CI/CD | GitHub Actions workflow |
| Actuator | Health, metrics, prometheus endpoints |
| Java 21 Records | DTO Records: `LoginRequest`, `AuthResponse`, etc. |
| Sealed Classes | `TaskStatus`, `Priority` enums (sealed pattern) |

---

## Hướng dẫn chung cho Implementing Session

Mỗi khi bắt đầu session mới:

```
Tôi đang implement dự án TaskFlow API tại ~/java_learning/taskflow.
Branch: claude/sharp-darwin-Gy838
Stack: Java 21, Spring Boot 3.3, PostgreSQL, Redis, Kafka

Scaffold đã có — nhiệm vụ của session này: [PASTE TASK TỪ coordinate.md]

Sau khi xong mỗi task: git add + git commit với message rõ ràng.
```

---

## S1 — Verify Foundation (11/5 sáng) ~2h

**Mục tiêu:** Đảm bảo project compile và start được.

### Task 1.1 — Compile check (~20 phút)
```
cd ~/java_learning/taskflow
mvn compile -q 2>&1 | tail -30

# Nếu có lỗi compile, fix theo thứ tự:
# 1. Missing imports
# 2. Type mismatches  
# 3. Missing methods/constructors
# Sau khi xanh: báo cáo số class đã compile
```

### Task 1.2 — Entity & Flyway review (~30 phút)
```
Review các files sau và đảm bảo đúng:

1. src/main/java/com/taskflow/entity/BaseEntity.java
   - @MappedSuperclass, @EntityListeners(AuditingEntityListener.class)
   - @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy

2. src/main/java/com/taskflow/entity/Task.java
   - @ManyToOne(fetch = LAZY) Project project
   - @ManyToOne(fetch = LAZY) User assignee
   - @OneToMany(mappedBy = "task", cascade = ALL) List<Comment> comments
   - @Enumerated(STRING) TaskStatus status, Priority priority

3. src/main/resources/db/migration/V1__init.sql
   - Có đủ tables: users, roles, user_roles, projects, user_projects,
     tasks, comments, labels, task_labels, notifications, audit_logs

4. src/main/resources/db/migration/V2__seed_roles.sql
   - INSERT ROLE_USER, ROLE_ADMIN

Fix nếu thiếu. Commit: "fix: complete entity mapping and Flyway migrations"
```

### Task 1.3 — Start application (~30 phút)
```
# Start infra
docker-compose up -d postgres redis

# Start app
cd ~/java_learning/taskflow
mvn spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/app.log 2>&1 &
APP_PID=$!
sleep 20

# Health check
curl -s http://localhost:8080/actuator/health | python3 -m json.tool

# Check Flyway ran
curl -s http://localhost:8080/actuator/flyway 2>/dev/null | python3 -m json.tool

kill $APP_PID

# Báo cáo: UP hay lỗi gì?
```

**Commit cuối S1:** `feat(s1): verify foundation — compile + entities + Flyway + startup`

---

## S2 — CRUD APIs (11/5 chiều) ~2h

**Mục tiêu:** Toàn bộ CRUD endpoints hoạt động đúng.

### Task 2.1 — Task CRUD endpoints (~40 phút)
```
Verify TaskController.java có đúng endpoints:

GET    /api/tasks              - list with Pageable + filters
POST   /api/tasks              - create task
GET    /api/tasks/{id}         - get by id
PUT    /api/tasks/{id}         - update
DELETE /api/tasks/{id}         - delete (chỉ project owner/creator)
PATCH  /api/tasks/{id}/status  - change status
POST   /api/tasks/{id}/assign/{userId} - assign

Và TaskService.java implement đúng logic:
- createTask: validate projectId tồn tại, set creator = current user
- updateTaskStatus: check state machine (TODO→IN_PROGRESS→DONE)
- assignTask: publish TaskAssignedEvent sau khi assign

Test bằng curl (cần JWT token — dùng hardcoded test token nếu chưa có auth):
  curl -X POST http://localhost:8080/api/tasks \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer TEST_TOKEN" \
    -d '{"title":"Test task","projectId":1,"priority":"HIGH"}'

Fix 404/500 errors. Commit từng fix.
```

### Task 2.2 — Project + Comment endpoints (~40 phút)
```
Verify ProjectController endpoints:
  GET  /api/projects           - my projects (current user)
  POST /api/projects           - create
  GET  /api/projects/{id}      - get detail
  PUT  /api/projects/{id}      - update (owner only)
  DELETE /api/projects/{id}    - delete (owner only)
  POST /api/projects/{id}/members - add member
  DELETE /api/projects/{id}/members/{userId} - remove member

Verify CommentController:
  GET    /api/tasks/{taskId}/comments - list comments
  POST   /api/tasks/{taskId}/comments - add comment
  DELETE /api/comments/{id}           - delete (author only)

Đảm bảo PageResponse<T> trả về đúng format:
  { "content": [...], "page": 0, "size": 10, 
    "totalElements": N, "totalPages": M, "last": false }
```

### Task 2.3 — Notification endpoints (~20 phút)
```
Verify NotificationController:
  GET   /api/notifications              - paged, for current user
  PATCH /api/notifications/{id}/read    - mark read
  PATCH /api/notifications/read-all     - mark all read
  GET   /api/notifications/unread-count - count badge

NotificationService phải query: WHERE user = currentUser ORDER BY createdAt DESC
```

**Commit cuối S2:** `feat(s2): complete CRUD APIs — task, project, comment, notification`

---

## S3 — Spring Security + JWT (12/5 sáng) ~2h

**Mục tiêu:** Auth flow hoàn chỉnh, endpoint protection đúng.

### Task 3.1 — Auth flow end-to-end (~50 phút)
```
Test toàn bộ auth flow:

# 1. Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"Test1234!"}'

# 2. Login → lấy tokens
RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!"}')
ACCESS_TOKEN=$(echo $RESPONSE | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")

# 3. Truy cập protected endpoint
curl -H "Authorization: Bearer $ACCESS_TOKEN" http://localhost:8080/api/tasks

# 4. Truy cập không có token → phải 401
curl http://localhost:8080/api/tasks

# 5. Refresh token
REFRESH=$(echo $RESPONSE | python3 -c "import sys,json; print(json.load(sys.stdin)['refreshToken'])")
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"

Fix bất kỳ lỗi nào. JwtUtil phải:
- Dùng HS256 algorithm, secret key >= 256 bits
- accessToken: 15 phút
- refreshToken: 7 ngày
- Validate: signature, expiry, subject
```

### Task 3.2 — SecurityConfig review (~30 phút)
```
Verify SecurityConfig.java:
  @Bean SecurityFilterChain:
    - csrf().disable()
    - cors() configured
    - sessionManagement: STATELESS
    - authorizeHttpRequests:
        /api/auth/** → permitAll
        /actuator/health → permitAll
        /swagger-ui/** → permitAll
        /v3/api-docs/** → permitAll
        anyRequest → authenticated
    - addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

  @Bean CorsConfigurationSource:
    - allowedOrigins: ["http://localhost:3000"]
    - allowedMethods: GET, POST, PUT, DELETE, PATCH, OPTIONS
    - allowedHeaders: *
    - allowCredentials: true

Fix và test lại. Commit: "fix(security): complete SecurityFilterChain config"
```

### Task 3.3 — ProjectSecurity method-level (~20 phút)
```
Verify ProjectSecurity.java:
  @Component("projectSecurity")
  public class ProjectSecurity {
    boolean isOwner(Long projectId, Authentication auth) { ... }
    boolean isMember(Long projectId, Authentication auth) { ... }
  }

Kiểm tra @PreAuthorize trong ProjectController:
  @PreAuthorize("@projectSecurity.isOwner(#id, authentication)")
  DELETE /api/projects/{id}

Test: user không phải owner cố xóa project → phải nhận 403.
```

**Commit cuối S3:** `feat(s3): complete Spring Security + JWT auth — end-to-end verified`

---

## S4 — Exception Handling, Validation, Pagination (12/5 chiều) ~2h

### Task 4.1 — GlobalExceptionHandler (~30 phút)
```
Verify GlobalExceptionHandler.java handle đủ:

1. ResourceNotFoundException (404):
   { "status": 404, "error": "Not Found", "message": "Task not found: 99", "timestamp": "..." }

2. AccessDeniedException (403):
   { "status": 403, "error": "Forbidden", "message": "Access denied", "timestamp": "..." }

3. BusinessException (400):
   { "status": 400, "error": "Bad Request", "message": "Task already completed", "timestamp": "..." }

4. MethodArgumentNotValidException (400):
   { "status": 400, "error": "Validation Failed", 
     "fieldErrors": [{"field":"title","message":"must not be blank"}],
     "timestamp": "..." }

5. Exception fallback (500): không leak stack trace, chỉ log ERROR

Test từng case bằng curl. Fix format nếu sai.
```

### Task 4.2 — Custom Validators (~30 phút)
```
Verify các custom validators:

1. @UniqueEmail / UniqueEmailValidator:
   - Query UserRepository.existsByEmail(email)
   - Nếu tồn tại: "Email already registered"

2. @UniqueUsername / UniqueUsernameValidator:
   - Query UserRepository.existsByUsername(username)

3. @FutureDueDate / FutureDueDateValidator:
   - dueDate phải sau LocalDate.now()
   - Message: "Due date must be in the future"

Test:
  POST /api/auth/register với email đã tồn tại → 400 "Email already registered"
  POST /api/tasks với dueDate = yesterday → 400 "Due date must be in the future"
```

### Task 4.3 — N+1 fix + Pagination verify (~40 phút)
```
Kiểm tra TaskRepository.java:
  @EntityGraph(attributePaths = {"assignee", "project", "labels"})
  Page<Task> findByProjectIdAndFilters(
    Long projectId, TaskStatus status, Long assigneeId, Pageable pageable);

Nếu chưa có @EntityGraph, thêm vào — tránh N+1 khi load tasks với relationships.

Enable SQL logging để verify (application.yml dev profile):
  logging.level.org.hibernate.SQL: DEBUG
  logging.level.org.hibernate.orm.jdbc.bind: TRACE

Gọi GET /api/tasks?page=0&size=5 và đếm số SQL queries — phải <= 2 queries.
Fix nếu > 2 (thêm JOIN FETCH hoặc @EntityGraph).

Commit: "fix(jpa): add @EntityGraph to prevent N+1 on task queries"
```

**Commit cuối S4:** `feat(s4): complete exception handling, custom validation, N+1 fix`

---

## S5 — Redis Cache + AOP + Virtual Threads (13/5 sáng) ~2h

### Task 5.1 — Redis Cache verification (~40 phút)
```
Verify RedisConfig.java:
  @Bean RedisCacheConfiguration: TTL 10 phút, JSON serializer
  @Bean CacheManager: dùng RedisCacheManager

Verify ProjectService.java annotations:
  @Cacheable(value = "projects", key = "#id")
  public ProjectResponse getProjectById(Long id) { ... }

  @CacheEvict(value = "projects", key = "#id")
  public ProjectResponse updateProject(Long id, UpdateProjectRequest req) { ... }

  @CacheEvict(value = "projects", key = "#id")
  public void deleteProject(Long id) { ... }

Test cache:
  # Lần 1: phải hit DB (xem SQL log)
  curl http://localhost:8080/api/projects/1 -H "Authorization: Bearer $TOKEN"
  
  # Lần 2: phải hit cache (không có SQL)
  curl http://localhost:8080/api/projects/1 -H "Authorization: Bearer $TOKEN"
  
  # Kiểm tra Redis
  redis-cli keys "projects::*"
  
  # Update → evict
  curl -X PUT http://localhost:8080/api/projects/1 ...
  redis-cli keys "projects::*"  # phải rỗng
```

### Task 5.2 — AOP Logging verification (~30 phút)
```
Verify LoggingAspect.java:
  @Around("execution(* com.taskflow.service.*.*(..))")
  Object logMethodCall(ProceedingJoinPoint pjp) {
    long start = System.currentTimeMillis();
    Object result = pjp.proceed();
    long duration = System.currentTimeMillis() - start;
    log.debug("[{}] {}ms", pjp.getSignature().getName(), duration);
    if (duration > 1000) log.warn("SLOW METHOD: {} took {}ms", ...);
    return result;
  }

Verify AuditAspect.java lưu AuditLog khi method có @Auditable:
  entity: Task, action: CREATE/UPDATE/DELETE, userId: current user

Gọi vài APIs, check logs:
  tail -f /tmp/app.log | grep -E "(DEBUG|WARN).*ms"
```

### Task 5.3 — Virtual Threads (Java 21) (~20 phút)
```
Trong application.yml (dev + prod):
  spring:
    threads:
      virtual:
        enabled: true

Nếu cần config thủ công, thêm vào TaskflowApplication.java:
  @Bean
  public TomcatProtocolHandlerCustomizer<?> virtualThreadCustomizer() {
    return handler -> handler.setExecutor(
      Executors.newVirtualThreadPerTaskExecutor());
  }

Verify bằng cách thêm log trong JwtAuthFilter:
  log.debug("Thread: {}", Thread.currentThread());
  
Restart app và gọi API → log phải show "VirtualThread[#N]/runnable@..."
Commit: "feat(s5): enable Java 21 virtual threads + verify Redis cache + AOP"
```

**Commit cuối S5:** `feat(s5): Redis caching + AOP logging + Java 21 virtual threads`

---

## S6 — Kafka Events + Notifications (13/5 chiều) ~2h

### Task 6.1 — Kafka infrastructure verify (~20 phút)
```
docker-compose up -d zookeeper kafka
sleep 10

# Tạo topics nếu chưa có
docker exec -it kafka kafka-topics.sh --create \
  --bootstrap-server localhost:9092 --topic task-assigned --partitions 3 --replication-factor 1

docker exec -it kafka kafka-topics.sh --create \
  --bootstrap-server localhost:9092 --topic task-completed --partitions 3 --replication-factor 1

docker exec -it kafka kafka-topics.sh --create \
  --bootstrap-server localhost:9092 --topic comment-added --partitions 3 --replication-factor 1

# Verify topics
docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Task 6.2 — Producer + Consumer flow (~50 phút)
```
Verify KafkaConfig.java:
  - ProducerFactory: StringSerializer + JsonSerializer (value)
  - ConsumerFactory: StringDeserializer + JsonDeserializer (value)
  - @EnableKafka

Verify NotificationProducer.java:
  kafkaTemplate.send("task-assigned", event.getTaskId().toString(), event)

Verify NotificationConsumer.java:
  @KafkaListener(topics = "task-assigned", groupId = "notification-group")
  void handleTaskAssigned(TaskAssignedEvent event) {
    // Tạo Notification entity
    // Lưu DB qua NotificationRepository
    // Log: "Notification created for user {}"
  }

Test end-to-end:
  1. Assign task qua API
  2. Check Kafka consumer log: "Notification created for user X"
  3. GET /api/notifications → phải thấy notification mới
  4. GET /api/notifications/unread-count → phải > 0
```

### Task 6.3 — Error handling + Retry (~20 phút)
```
Thêm retry + DLT vào NotificationConsumer.java:
  @RetryableTopic(
    attempts = "3",
    backoff = @Backoff(delay = 1000, multiplier = 2),
    dltTopicSuffix = "-dlt"
  )
  @KafkaListener(topics = "task-assigned", ...)
  void handleTaskAssigned(TaskAssignedEvent event) { ... }

  @DltHandler
  void handleDlt(TaskAssignedEvent event, Exception ex) {
    log.error("DLT: failed to process event {} after retries: {}", event, ex.getMessage());
  }

Test: throw RuntimeException trong consumer → verify log shows 3 retries → DLT.
Commit: "feat(s6): Kafka events + notification flow + retry/DLT"
```

**Commit cuối S6:** `feat(s6): complete Kafka notification pipeline with retry and DLT`

---

## S7 — Testing (14/5 sáng) ~2h

### Task 7.1 — Unit Tests (~50 phút)
```
Chạy: mvn test -Dtest="TaskServiceTest,ProjectServiceTest" -q
Fix tất cả failures.

TaskServiceTest phải cover:
  - createTask_success
  - createTask_projectNotFound → throws ResourceNotFoundException
  - updateTaskStatus_invalidTransition → throws BusinessException
  - assignTask_success → verify kafkaProducer.sendTaskAssigned() called
  - deleteTask_notOwner → throws AccessDeniedException

ProjectServiceTest phải cover:
  - createProject_success
  - getProjectById_cacheHit (verify repo NOT called second time)
  - addMember_alreadyMember → throws BusinessException
  - deleteProject_success → verify cacheEvict

Pattern cho mỗi test:
  @Test
  void methodName_scenario_expectedResult() {
    // given
    when(repo.findById(1L)).thenReturn(Optional.of(entity));
    // when
    var result = service.doSomething(1L);
    // then
    assertThat(result).isNotNull();
    verify(repo, times(1)).findById(1L);
  }
```

### Task 7.2 — Integration Tests (~50 phút)
```
Chạy: mvn test -Dtest="AuthControllerIT,TaskControllerIT" -q
Fix tất cả failures.

AuthControllerIT flow:
  1. POST /api/auth/register → 201
  2. POST /api/auth/login → 200, có accessToken
  3. GET /api/tasks với token → 200
  4. GET /api/tasks không token → 401
  5. POST /api/auth/refresh → 200, token mới

TaskControllerIT flow:
  1. Login → get token
  2. Create project
  3. Create task trong project
  4. Get task list → verify pagination
  5. Update task status
  6. Assign task
  7. Delete task

AbstractIntegrationTest dùng @Testcontainers:
  @Container PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
  @Container GenericContainer redis = new GenericContainer("redis:7-alpine")
  
  @DynamicPropertySource
  static void configure(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.redis.host", redis::getHost);
    r.add("spring.redis.port", () -> redis.getMappedPort(6379));
  }
```

### Task 7.3 — Coverage Report (~20 phút)
```
mvn test jacoco:report -q
echo "Report: target/site/jacoco/index.html"

# Check tổng coverage
cat target/site/jacoco/index.html | grep -A2 "Total"

# Mục tiêu:
# - Service layer: >= 80%
# - Controller layer: >= 70%  
# - Overall: >= 65%

# Nếu thiếu coverage, thêm test cho:
# - Edge cases (null input, empty list)
# - Error paths (exception thrown)
# - Authorization checks (403 cases)

Commit: "test(s7): complete unit + integration tests, coverage >= 65%"
```

**Commit cuối S7:** `test(s7): JUnit5 + MockMvc + Testcontainers — all tests passing`

---

## S8 — Docker + CI/CD + Actuator + Final (14/5 chiều) ~2h

### Task 8.1 — Docker build & compose (~40 phút)
```
Verify taskflow/Dockerfile:
  # Stage 1: Build
  FROM eclipse-temurin:21-jdk-alpine AS builder
  WORKDIR /app
  COPY pom.xml .
  COPY src ./src
  RUN mvn package -DskipTests -q

  # Stage 2: Runtime
  FROM eclipse-temurin:21-jre-alpine
  RUN addgroup -S appgroup && adduser -S appuser -G appgroup
  USER appuser
  WORKDIR /app
  COPY --from=builder /app/target/*.jar app.jar
  EXPOSE 8080
  ENTRYPOINT ["java", "-jar", "app.jar"]

Build & test Docker image:
  cd ~/java_learning/taskflow
  docker build -t taskflow-api:latest .
  
Verify docker-compose.yml (root ~/java_learning/):
  - app service dùng build: ./taskflow
  - depends_on: postgres (condition: service_healthy), redis, kafka
  - environment: SPRING_PROFILES_ACTIVE=prod, DB/Redis/Kafka URLs

Full stack test:
  docker-compose up --build -d
  sleep 20
  curl http://localhost:8080/actuator/health
```

### Task 8.2 — GitHub Actions CI/CD (~30 phút)
```
Tạo file .github/workflows/ci.yml:

name: CI
on:
  push:
    branches: [master, claude/*]
  pull_request:
    branches: [master]

jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: taskflow_test
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports: ['5432:5432']
      redis:
        image: redis:7-alpine
        ports: ['6379:6379']
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: Build and Test
        run: cd taskflow && mvn verify -Dspring.profiles.active=test
      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          file: taskflow/target/site/jacoco/jacoco.xml

Commit file này.
```

### Task 8.3 — Actuator + Logging + Final README (~30 phút)
```
Verify application.yml có:
  management:
    endpoints:
      web:
        exposure:
          include: health,info,metrics,prometheus,flyway
    endpoint:
      health:
        show-details: when-authorized

Tạo taskflow/src/main/resources/logback-spring.xml cho prod profile:
  <springProfile name="prod">
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
      <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>
  </springProfile>

Cập nhật README.md trong ~/java_learning/taskflow/:
  # TaskFlow API
  Team task management REST API — Java 21 + Spring Boot 3.3

  ## Tech Stack
  | Layer | Technology |
  | Web | Spring MVC, Spring Boot 3.3 |
  | Security | Spring Security 6, JWT (jjwt 0.12) |
  | Database | PostgreSQL 16, Spring Data JPA, Flyway |
  | Cache | Redis 7, Spring Cache |
  | Messaging | Apache Kafka |
  | Testing | JUnit 5, Mockito, MockMvc, Testcontainers |
  | Observability | Spring Actuator, Micrometer |
  | Build | Maven, Docker, GitHub Actions |

  ## Quick Start
  docker-compose up -d
  curl http://localhost:8080/actuator/health

  ## API Docs
  http://localhost:8080/swagger-ui.html

Final commit: "docs(s8): complete Docker, CI/CD, Actuator, README — project done"
```

**Commit cuối S8:** `feat(s8): Docker multi-stage + GitHub Actions CI + Actuator — DONE`

---

## Checklist Hoàn thành Dự án

### Java Core (từ README)
- [ ] OOP — Entity hierarchy, service interfaces
- [ ] Generics — `PageResponse<T>`, Repository
- [ ] Exception — custom exceptions, GlobalExceptionHandler
- [ ] Collections/Stream — service filter/map logic
- [ ] Concurrency — Virtual Threads Java 21
- [ ] JVM features — Java 21 records, sealed enums

### Spring Boot (từ README)
- [ ] IoC/DI — Constructor injection khắp nơi
- [ ] AOP — LoggingAspect, AuditAspect
- [ ] Spring MVC REST — 4 controllers, pagination
- [ ] Spring Data JPA — entity mapping, N+1 fix
- [ ] Spring Security — JWT, SecurityFilterChain
- [ ] Redis Cache — @Cacheable, @CacheEvict
- [ ] Kafka — producer, consumer, DLT
- [ ] Testing — unit + integration + coverage
- [ ] Docker — multi-stage Dockerfile
- [ ] CI/CD — GitHub Actions
- [ ] Actuator — health, metrics, prometheus

---

## Ghi chú Usage Limit

| Session | Độ phức tạp | Ước tính tokens | Mẹo |
|---------|------------|----------------|-----|
| S1 Sáng | Thấp | ~20k | Chỉ verify + fix compile |
| S2 Chiều | Trung bình | ~35k | Test từng endpoint, không paste full code |
| S3 Sáng | Trung bình | ~30k | Focus vào auth flow, không rewrite từ đầu |
| S4 Chiều | Thấp | ~25k | Chủ yếu config + test |
| S5 Sáng | Trung bình | ~30k | Redis + AOP chủ yếu verify config |
| S6 Chiều | Cao | ~40k | Kafka phức tạp, có thể cần 2 lần hỏi |
| S7 Sáng | Cao | ~45k | Testing nhiều code, dùng /compact thường xuyên |
| S8 Chiều | Trung bình | ~30k | Chủ yếu config files + docs |
