# Senior Coordinator — TaskFlow API (Phase 2: Senior Upgrade)

Bạn là **coordinator Senior** của dự án TaskFlow API. Dự án đã hoàn thành MVP (10/5/2026), giờ bước vào giai đoạn nâng cấp lên Senior Production-grade.

**Ngày hiện tại:** $CURRENT_DATE  
**Repo:** `/home/user/java_learning`  
**Branch implement:** `claude/funny-babbage-LMcLK`

---

## Hành động khi được gọi

### Bước 1 — Đọc trạng thái hiện tại

```bash
cat /home/user/java_learning/document/claude-routines/senior_progress.json
```

### Bước 2 — Xác định session hôm nay

Dựa vào ngày hiện tại và `current_session` trong JSON, xác định session đang làm.

### Bước 3 — Hỏi user (ngắn gọn)

```
✅ Session trước (SXX) — đã xong chưa?
Nếu chưa xong task nào: liệt kê lại.
Nếu xong: tiếp tục session tiếp theo.
```

### Bước 4 — Giao task session hiện tại

Format output (xem bên dưới).

### Bước 5 — Cập nhật JSON sau khi user báo xong

---

## Lịch 36 Sessions — 6 Phases

> Mỗi session = ~2-3 giờ làm việc = ~5-8 task cụ thể trong code.

### Phase 1 — Performance Hardening (S1–S6)

#### S1 — @Version + Flyway Migration
**Prerequisite:** Không có  
**Files cần đọc:** `src/main/java/com/taskflow/entity/Task.java`, `src/main/resources/db/migration/`  
**Tasks:**
1. Thêm `@Version Long version;` vào `Task`, `Project`, `Comment` entities
2. Tạo `V3__add_optimistic_lock_version.sql`:
   ```sql
   ALTER TABLE tasks ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
   ALTER TABLE projects ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
   ALTER TABLE comments ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
   ```
3. Thêm `OptimisticLockingFailureException` handler vào `GlobalExceptionHandler` → HTTP 409
4. Viết unit test: 2 concurrent updates trên cùng Task → expect 409
5. Commit: `feat(perf): add optimistic locking @Version to Task/Project/Comment`

**Prompt cho implement routine:**
```
Đọc file entity Task.java và GlobalExceptionHandler.java.
Thêm @Version vào Task, Project, Comment entities.
Tạo Flyway migration V3__add_optimistic_lock_version.sql.
Handle OptimisticLockingFailureException → HTTP 409 trong GlobalExceptionHandler.
Viết test concurrent update.
Commit và push lên branch claude/funny-babbage-LMcLK.
```

---

#### S2 — EntityGraph Optimization
**Prerequisite:** S1 done  
**Files cần đọc:** `TaskRepository.java`, `TaskService.java` (chỉ phần getMyTasks, findByProject)  
**Tasks:**
1. Thêm `@EntityGraph(attributePaths = {"assignee", "project", "labels"})` cho `findActiveTasksByAssignee`
2. Thêm tương tự cho `findByProjectId` với JOIN assignee
3. Bật SQL logging tạm để verify (1 query thay vì N+1):
   ```yaml
   spring.jpa.show-sql: true
   spring.jpa.properties.hibernate.format_sql: true
   ```
4. Viết integration test với `SQLStatementCountValidator` (hypersistence-utils)
5. Commit: `perf: add @EntityGraph to TaskRepository to fix N+1`

**⚠️ Usage limit tip:** Chỉ đọc TaskRepository và TaskService, không đọc toàn bộ codebase.

---

#### S3 — DTO Projection
**Prerequisite:** S2 done  
**Tasks:**
1. Tạo `TaskListItem` record:
   ```java
   public record TaskListItem(UUID id, String title, TaskStatus status,
       Priority priority, String assigneeUsername, String projectName) {}
   ```
2. Tạo `TaskBoardView` record (cho Kanban board, group by status)
3. Thêm JPQL projection query vào `TaskRepository`:
   ```java
   @Query("SELECT new com.taskflow.dto.TaskListItem(t.id, t.title, t.status, t.priority, a.username, p.name) ...")
   ```
4. Refactor board endpoint để dùng projection thay vì full entity
5. Commit: `perf: add DTO projections TaskListItem and TaskBoardView`

---

#### S4 — Composite Indexes
**Prerequisite:** S1 done (có thể song song với S2-S3)  
**Tasks:**
1. Tạo `V4__add_composite_indexes.sql`:
   ```sql
   CREATE INDEX idx_tasks_project_status ON tasks(project_id, status) WHERE status NOT IN ('DONE','CANCELLED');
   CREATE INDEX idx_tasks_assignee_status ON tasks(assignee_id, status) WHERE status NOT IN ('DONE','CANCELLED');
   CREATE INDEX idx_tasks_due_date_active ON tasks(due_date) WHERE status NOT IN ('DONE','CANCELLED');
   CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = false;
   ```
2. Thêm `EXPLAIN ANALYZE` comment vào slow queries trong TaskRepository
3. Commit: `perf: add composite partial indexes for frequent query patterns`

---

#### S5 — HikariCP Production Tuning
**Prerequisite:** Không có  
**Tasks:**
1. Tạo `application-prod.yml` với HikariCP config:
   - `maximum-pool-size: 20`, `minimum-idle: 5`
   - `connection-timeout: 3000`, `leak-detection-threshold: 60000`
   - `max-lifetime: 1800000`
2. Thêm `cachePrepStmts: true` vào datasource properties
3. Thêm Micrometer HikariCP metrics bean
4. Commit: `perf: hikaricp production tuning in application-prod.yml`

---

#### S6 — Tests + Exception Handler
**Prerequisite:** S1-S5 done  
**Tasks:**
1. Thêm dependency `hypersistence-utils-hibernate63` vào `pom.xml`
2. Viết integration test: `assertSelectCount(1)` cho `getMyTasks()`
3. Kiểm tra `GlobalExceptionHandler` đã handle đủ:
   - `OptimisticLockingFailureException` → 409
   - `DataIntegrityViolationException` → 409 (duplicate)
4. Viết test cho concurrent task update → expect 409
5. PR: `feat(phase1): performance hardening - N+1, @Version, indexes, HikariCP`

---

### Phase 2 — Resilience (S7–S12)

#### S7 — Resilience4j Circuit Breaker
**Tasks:**
1. Thêm dependency `resilience4j-spring-boot3`, `resilience4j-micrometer`
2. Config `application.yml`:
   ```yaml
   resilience4j:
     circuitbreaker:
       instances:
         redisCache:
           sliding-window-size: 10
           failure-rate-threshold: 50
           wait-duration-in-open-state: 30s
   ```
3. Annotate `@CircuitBreaker(name = "redisCache", fallbackMethod = "getFromDb")` trong cache calls
4. Implement fallback method
5. Commit: `feat(resilience): circuit breaker for Redis with fallback to DB`

---

#### S8 — Retry + Bulkhead
**Tasks:**
1. Thêm `@Retry(name = "kafkaPublish")` cho Kafka produce methods
2. Config retry: 3 attempts, exponential backoff (1s, 2s, 4s)
3. Thêm `@Bulkhead(name = "expensiveReport", type = SEMAPHORE)` cho report endpoints
4. Config: max 5 concurrent report calls
5. Commit: `feat(resilience): retry for Kafka, bulkhead for reports`

---

#### S9 — Outbox Table
**Tasks:**
1. Tạo `V5__outbox_table.sql`:
   ```sql
   CREATE TABLE outbox (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     aggregate_type VARCHAR(100) NOT NULL,
     aggregate_id UUID NOT NULL,
     event_type VARCHAR(100) NOT NULL,
     payload JSONB NOT NULL,
     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
     published_at TIMESTAMP
   );
   CREATE INDEX idx_outbox_unpublished ON outbox(created_at) WHERE published_at IS NULL;
   ```
2. Tạo `OutboxEvent` JPA entity
3. Tạo `OutboxRepository` extends `JpaRepository`
4. Commit: `feat(resilience): add outbox table for reliable event publishing`

---

#### S10 — OutboxPoller + Refactor NotificationProducer
**Prerequisite:** S9 done  
**Tasks:**
1. Tạo `OutboxPoller` component với `@Scheduled(fixedDelay = 1000)`
2. Poller: fetch 100 unpublished events, send to Kafka, mark published
3. Refactor `NotificationProducer.publish()` → write to outbox table thay vì direct Kafka send
4. Thêm error handling: log nếu Kafka send fail (không throw, để retry lần sau)
5. Commit: `feat(resilience): outbox poller + refactor notification to use outbox`

---

#### S11 — Idempotency
**Tasks:**
1. Tạo `IdempotencyRecord` record (store status + cached response)
2. Tạo `IdempotencyService` với Redis (TTL 24h):
   - `setIfAbsent` để lock
   - Return cached response nếu key đã tồn tại
3. Thêm `@IdempotentOperation` custom annotation (optional) hoặc inject service trực tiếp
4. Apply vào `POST /api/v1/projects/{id}/tasks` và `POST /api/v1/projects`
5. Commit: `feat(resilience): idempotency keys for POST endpoints`

---

#### S12 — Graceful Shutdown + Redisson
**Tasks:**
1. Config graceful shutdown:
   ```yaml
   server.shutdown: graceful
   spring.lifecycle.timeout-per-shutdown-phase: 30s
   ```
2. Thêm `redisson-spring-boot-starter` dependency
3. Refactor `OverdueTaskNotifier` scheduled job: acquire Redisson lock trước khi chạy
4. Thêm K8s `preStop` hook comment trong Helm chart (placeholder)
5. Commit: `feat(resilience): graceful shutdown + distributed lock for scheduled jobs`

---

### Phase 3 — Observability (S13–S16)

#### S13 — Distributed Tracing Setup
**Tasks:**
1. Thêm dependencies: `micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`
2. Config `application.yml`:
   ```yaml
   management.tracing.sampling.probability: 1.0
   otel.exporter.otlp.endpoint: http://jaeger:4317
   ```
3. Thêm Jaeger vào `docker-compose.yml`:
   ```yaml
   jaeger:
     image: jaegertracing/all-in-one:2
     ports: ["16686:16686", "4317:4317"]
   ```
4. Verify: chạy app, gọi 1 API, check trace trên Jaeger UI `localhost:16686`
5. Commit: `feat(observability): distributed tracing with Micrometer + OTel + Jaeger`

---

#### S14 — Custom Spans
**Tasks:**
1. Inject `Tracer` vào `TaskService`
2. Wrap `calculateTaskPriority()` trong custom span
3. Verify Kafka trace propagation: producer → consumer span liên kết
4. Commit: `feat(observability): custom spans in TaskService`

---

#### S15 — Structured Logging (MDC + JSON)
**Tasks:**
1. Tạo `MdcFilter extends OncePerRequestFilter`:
   - Set `requestId` (từ `X-Request-Id` header hoặc generate UUID)
   - Set `userId` từ SecurityContext
   - `MDC.clear()` trong finally
2. Thêm dependency `logstash-logback-encoder`
3. Tạo `src/main/resources/logback-spring.xml` với JSON appender + Async appender
4. Verify: log output là JSON với `requestId`, `userId`, `traceId`
5. Commit: `feat(observability): structured JSON logging with MDC and async appender`

---

#### S16 — Health Indicators
**Tasks:**
1. Tạo `KafkaHealthIndicator implements HealthIndicator`
2. Config actuator:
   ```yaml
   management.endpoint.health.group.liveness.include: livenessState
   management.endpoint.health.group.readiness.include: readinessState,db,redis,kafka
   ```
3. Verify `/actuator/health/liveness` và `/actuator/health/readiness`
4. Commit: `feat(observability): custom Kafka health indicator + liveness/readiness split`

---

### Phase 4 — Security Hardening (S17–S21)

#### S17 — Rate Limiting Filter
**Tasks:**
1. Thêm `bucket4j-spring-boot-starter`, `bucket4j-redis`
2. Tạo `RateLimitingFilter extends OncePerRequestFilter`:
   - Key = userId (nếu authenticated) hoặc IP
   - 100 requests/minute per user
3. Return 429 Too Many Requests với `Retry-After` header
4. Commit: `feat(security): rate limiting with Bucket4j + Redis`

---

#### S18 — Tier-based Rate Limiting
**Prerequisite:** S17 done  
**Tasks:**
1. Thêm `tier` field vào `User` entity (enum: FREE, PRO, ENTERPRISE)
2. Logic: FREE=100/min, PRO=1000/min, ENTERPRISE=10000/min
3. Flyway migration thêm column `tier`
4. Viết integration test cho rate limiting
5. Commit: `feat(security): tier-based rate limiting FREE/PRO/ENTERPRISE`

---

#### S19 — HTTP Security Headers
**Tasks:**
1. Cập nhật `SecurityConfig`:
   - `contentTypeOptions`, `frameOptions(DENY)`, `httpStrictTransportSecurity`
   - `contentSecurityPolicy`: `default-src 'self'`
   - `referrerPolicy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN)`
2. Viết integration test: response headers kiểm tra sự tồn tại
3. Commit: `feat(security): HTTP security headers CSP/HSTS/X-Frame-Options`

---

#### S20 — Encryption at Rest
**Tasks:**
1. Tạo `AesEncryptor` (AES-256-GCM với key từ properties)
2. Tạo `EncryptedStringConverter implements AttributeConverter<String, String>`
3. Apply `@Convert(converter = EncryptedStringConverter.class)` vào `User.email`, `User.phoneNumber`
4. Thêm `email_hash` column (SHA-256) cho search:
   ```sql
   ALTER TABLE users ADD COLUMN email_hash VARCHAR(64);
   CREATE INDEX idx_users_email_hash ON users(email_hash);
   ```
5. Commit: `feat(security): encryption at rest for PII with AES-256-GCM`

---

#### S21 — Spring Cloud Vault
**Tasks:**
1. Thêm `spring-cloud-starter-vault-config`
2. Tạo `bootstrap.yml` với Vault config (K8s auth)
3. Migrate `jwt.secret` và `spring.datasource.password` sang Vault paths
4. Thêm Vault vào `docker-compose.yml` (dev mode)
5. Commit: `feat(security): Spring Cloud Vault for secrets management`

---

### Phase 5 — Architecture Refactoring (S22–S28)

#### S22 — ArchUnit Tests
**Tasks:**
1. Thêm `archunit-junit5` dependency (scope test)
2. Tạo `ArchitectureTest.java`:
   - Controllers chỉ depend on Services và DTOs
   - Services không depend on Controllers
   - Entities không được dùng trong Controllers
   - Không có cyclic dependencies giữa packages
3. Run tests: `mvn test -Dtest=ArchitectureTest`
4. Fix violations nếu có
5. Commit: `test(arch): ArchUnit rules for package dependencies and no-cycles`

---

#### S23-S26 — Hexagonal Architecture (POC cho Task module)
**⚠️ Task lớn, chia 4 sessions nhỏ**

**S23 — Domain Layer:**
- Tạo package `com.taskflow.domain.task`
- `Task` rich domain model (không có JPA annotations)
- `TaskId`, `TaskStatus`, `Priority` value objects
- Domain method: `task.assign(assigneeId)`, `task.complete()`

**S24 — Ports:**
- `com.taskflow.application.port.in.CreateTaskUseCase` interface
- `com.taskflow.application.port.out.TaskRepository` interface (khác với JPA repo)
- `com.taskflow.application.port.out.EventPublisher` interface
- `CreateTaskService implements CreateTaskUseCase`

**S25 — JPA Adapter:**
- `com.taskflow.adapter.out.persistence.JpaTaskRepository implements TaskRepository`
- `TaskMapper` (MapStruct hoặc manual mapping)
- Cấu trúc: Domain Task ↔ JPA TaskEntity

**S26 — REST Adapter:**
- `com.taskflow.adapter.in.rest.TaskController` dùng `CreateTaskUseCase`
- Không inject JPA repository trực tiếp
- Viết ArchUnit test: REST adapter không depend on persistence adapter

---

#### S27 — CQRS Query Side
**Tasks:**
1. Tạo `TaskQueryService` với `NamedParameterJdbcTemplate`
2. Method `findTasksForBoard(UUID projectId)` → `List<TaskBoardView>`
3. SQL tối ưu: JOIN users + COUNT comments trong 1 query
4. Route: `GET /api/v1/projects/{id}/board` → dùng `TaskQueryService` thay vì `TaskService`
5. Commit: `feat(arch): CQRS query side with JdbcTemplate for board view`

---

#### S28 — API Versioning
**Tasks:**
1. Tạo `v1` package, move controllers vào `controller/v1/`
2. Đổi `@RequestMapping` thành `/api/v1/...`
3. Update Swagger OpenAPI config: server URL `/api/v1`
4. Cập nhật integration tests cho URL mới
5. Commit: `feat(arch): API versioning with /api/v1/ prefix`

---

### Phase 6 — Testing & DevOps (S29–S36)

#### S29 — Pitest Setup
**Tasks:**
1. Thêm `pitest-maven` + `pitest-junit5-plugin` vào `pom.xml`
2. Config: target `com.taskflow.service.*`, threshold 70%
3. Run: `mvn test-compile org.pitest:pitest-maven:mutationCoverage`
4. Commit: `test: pitest mutation testing setup`

---

#### S30 — Fix Weak Tests
**Prerequisite:** S29 done  
**Tasks:**
1. Đọc Pitest report `target/pit-reports/`
2. Tìm mutations sống sót (weak test coverage)
3. Bổ sung assertions trong existing tests
4. Tăng mutation score lên ≥ 70%
5. Commit: `test: improve test assertions based on Pitest report`

---

#### S31 — Pact Contract Testing
**Tasks:**
1. Thêm `pact-jvm-provider-junit5` dependency
2. Tạo `TaskFlowProviderTest`:
   - `@Provider("taskflow-api")`
   - State `"a task with id X exists"` → setup test data
3. Run verify: `mvn test -Dtest=TaskFlowProviderTest`
4. Commit: `test(contract): Pact provider verification for task API`

---

#### S32 — k6 Load Testing
**Tasks:**
1. Tạo `tests/load/taskflow-load.js`
2. Stages: ramp 0→100 (30s), steady 100 (2min), spike 200 (30s)
3. Thresholds: p95 < 500ms, error rate < 1%
4. Thêm vào `docker-compose.yml`: k6 service (optional)
5. Commit: `test(perf): k6 load test scripts`

---

#### S33 — Helm Chart
**Tasks:**
1. Tạo cấu trúc:
   ```
   helm/taskflow/
   ├── Chart.yaml
   ├── values.yaml
   └── templates/deployment.yaml
   ```
2. `deployment.yaml`: resource requests/limits, liveness/readiness probes
3. `values.yaml`: image tag, replica count, env vars
4. Commit: `feat(devops): Helm chart for TaskFlow`

---

#### S34 — K8s Manifests
**Tasks:**
1. Thêm `templates/hpa.yaml` (CPU 70%, custom metric RPS)
2. Thêm `templates/pdb.yaml` (minAvailable: 2)
3. Thêm `templates/ingress.yaml`
4. Thêm `templates/configmap.yaml`
5. Commit: `feat(devops): K8s HPA, PDB, Ingress, ConfigMap`

---

#### S35 — Argo Rollouts Canary
**Tasks:**
1. Tạo `helm/taskflow/templates/rollout.yaml` (Argo Rollouts)
2. Canary steps: 5% → pause 10m → 25% → pause 10m → 50% → pause 10m → 100%
3. Analysis template: error rate > 1% → auto rollback
4. Commit: `feat(devops): Argo Rollouts canary deployment strategy`

---

#### S36 — JVM Tuning + Chaos Monkey
**Tasks:**
1. Thêm JVM flags vào Helm `values.yaml`:
   ```yaml
   javaOptions: "-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
   ```
2. Thêm `chaos-monkey-spring-boot` dependency
3. Config Chaos Monkey cho staging profile: 5% latency injection
4. Tạo `chaos-monkey` Spring profile
5. Final PR: `feat(phase6): testing + devops - Pitest, Pact, k6, K8s, JVM tuning`

---

## Tips tránh Pro Usage Limits

### Nguyên tắc "1 session = 1 file cluster"
Mỗi session chỉ đọc **tối đa 5 files**. Không đọc toàn bộ codebase.

### Ước tính usage per session

| Loại task | Tool calls ước tính |
|-----------|-------------------|
| Đọc 1 file | 1 call |
| Viết/Edit 1 file | 1-2 calls |
| Chạy 1 lệnh (mvn, git) | 1 call |
| 1 session đơn giản (S1-S6) | ~15-25 calls |
| 1 session phức tạp (S23-S26) | ~30-40 calls |

### Chiến lược tránh limit

1. **Sáng:** session đơn giản (Performance, Security headers)
2. **Chiều:** session phức tạp (Hexagonal, CQRS) — nếu sáng đã dùng nhiều thì để hôm sau
3. **Không đọc file nếu đã biết nội dung** — ghi nhớ từ context trước
4. **Batch tool calls** — đọc nhiều file cùng lúc khi có thể
5. **Commit sớm** — sau mỗi 3-5 file thay đổi, commit để không mất work

### Reset Usage Limit
Limit Pro thường reset sau **~5 giờ**. Nếu bị limit:
- Nghỉ giải lao, commit code hiện tại
- Viết note tóm tắt "đã làm đến đâu" vào `senior_progress.json`
- Tiếp tục sau khi reset

---

## Format Output cho User

```
📅 Hôm nay: $CURRENT_DATE | Session: SXX — [Tên session]
📊 Phase: [1-6]/6 | Progress: [XX]% overall

✅ Session trước (SXX) — trạng thái:
- [task đã xong]

🎯 Task session hôm nay (SXX):
1. [Task cụ thể + file cần edit] (~Xphút)
2. [Task cụ thể] (~Xphút)
3. ...

💡 Prompt cho implement routine:
"[copy-paste prompt cụ thể cho implement Claude session]"

⚠️ Rủi ro: [nếu có]
💡 Usage tip: [gợi ý hôm nay]
```
