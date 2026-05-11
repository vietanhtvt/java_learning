# TaskFlow API — Senior Implementation Plan

> Sprint 48h: nâng TaskFlow từ Mid-level MVP lên **Senior Production-grade**.
>
> Mỗi task = **2 tiếng** (viết code + document).  
> Tổng: **24 tasks × 2h = 48h**.  
> Nhịp khuyến nghị: **3 tasks/ngày** (6h) → hoàn thành trong **8 ngày**.  
> Giới hạn Claude Pro: không quá 3 sessions/ngày.

---

## Tổng quan

| Phase | Số task | Giờ | Ưu tiên | Mục tiêu |
|-------|---------|-----|---------|---------|
| **Phase 1 — Performance** | 7 | 14h | 🔴 Cao | Loại N+1, concurrency safe, index tối ưu |
| **Phase 2 — Resilience** | 9 | 18h | 🔴 Cao | Không mất event, chịu được Redis/Kafka down |
| **Phase 3 — Observability** | 8 | 16h | 🔴 Cao | Trace end-to-end, log có cấu trúc |
| **Phase 4–6** | 20 | ~40h | 🟡 Next | Security, Architecture, DevOps — sprint sau |
| **Tổng sprint này** | **24** | **48h** | | |

---

## Lịch 8 ngày (3 tasks/ngày)

| Ngày | Tasks | Focus |
|------|-------|-------|
| Day 1 | T1, T2, T3 | Performance: @Version, Indexes, EntityGraph |
| Day 2 | T4, T5, T6 | Performance: DTO Projection, HikariCP, N+1 test |
| Day 3 | T7, T8, T9 | Performance wrap-up + Resilience: Resilience4j setup |
| Day 4 | T10, T11, T12 | Resilience: Outbox table, OutboxPoller |
| Day 5 | T13, T14, T15 | Resilience: Idempotency, Graceful Shutdown, Redisson |
| Day 6 | T16, T17, T18 | Observability: Tracing, Jaeger, Custom Spans |
| Day 7 | T19, T20, T21 | Observability: MDC, JSON Logging, Async Appender |
| Day 8 | T22, T23, T24 | Observability: Health, Readiness, Final Docs |

---

## Trạng thái tasks

| ID | Task | Phase | Giờ | Status | Ngày |
|----|------|-------|-----|--------|------|
| T1 | `@Version` optimistic locking | P1 | 2h | ⬜ pending | Day 1 |
| T2 | Composite indexes + EXPLAIN | P1 | 2h | ⬜ pending | Day 1 |
| T3 | `@EntityGraph` fix N+1 | P1 | 2h | ⬜ pending | Day 1 |
| T4 | DTO Projection TaskListItem | P1 | 2h | ⬜ pending | Day 2 |
| T5 | HikariCP production tuning | P1 | 2h | ⬜ pending | Day 2 |
| T6 | SQL assertion test (hypersistence) | P1 | 2h | ⬜ pending | Day 2 |
| T7 | Read Replica routing (doc + POC) | P1 | 2h | ⬜ pending | Day 3 |
| T8 | Resilience4j dependency + config | P2 | 2h | ⬜ pending | Day 3 |
| T9 | `@CircuitBreaker` Redis cache | P2 | 2h | ⬜ pending | Day 3 |
| T10 | `@Retry` Kafka producer | P2 | 2h | ⬜ pending | Day 4 |
| T11 | Outbox table + entity (Flyway V5) | P2 | 2h | ⬜ pending | Day 4 |
| T12 | OutboxPoller implementation | P2 | 2h | ⬜ pending | Day 4 |
| T13 | Refactor NotificationProducer → outbox | P2 | 2h | ⬜ pending | Day 5 |
| T14 | IdempotencyService + Redis | P2 | 2h | ⬜ pending | Day 5 |
| T15 | Apply idempotency + Graceful Shutdown + Redisson | P2 | 2h | ⬜ pending | Day 5 |
| T16 | Micrometer Tracing + OTel + Jaeger | P3 | 2h | ⬜ pending | Day 6 |
| T17 | Trace propagation qua Kafka | P3 | 2h | ⬜ pending | Day 6 |
| T18 | Custom spans trong service methods | P3 | 2h | ⬜ pending | Day 6 |
| T19 | MDC Filter (requestId, userId) | P3 | 2h | ⬜ pending | Day 7 |
| T20 | JSON Logging (LogstashEncoder) | P3 | 2h | ⬜ pending | Day 7 |
| T21 | Async Appender + log pattern | P3 | 2h | ⬜ pending | Day 7 |
| T22 | Custom KafkaHealthIndicator | P3 | 2h | ⬜ pending | Day 8 |
| T23 | Liveness vs Readiness probe split | P3 | 2h | ⬜ pending | Day 8 |
| T24 | Architecture docs + ADR files | P3 | 2h | ⬜ pending | Day 8 |

---

# Phase 1 — Performance & Scalability (14h)

---

## T1 — `@Version` Optimistic Locking (2h)

**Mục tiêu:** Ngăn Lost Update khi 2 user cùng sửa task.

**Deliverables:**
- `Task.java`, `Project.java`, `Comment.java` — thêm `@Version`
- `V3__add_version_column.sql` — Flyway migration
- `GlobalExceptionHandler.java` — handle `OptimisticLockingFailureException` → 409
- `doc/adr/ADR-001-optimistic-locking.md` — giải thích quyết định

**Implementing prompt:**
```
Trong dự án ~/java_learning/taskflow:

1. Thêm @Version vào 3 entities:
   - Task.java: thêm `@Version private Long version;`
   - Project.java: thêm `@Version private Long version;`
   - Comment.java: thêm `@Version private Long version;`

2. Tạo Flyway migration:
   src/main/resources/db/migration/V3__add_version_column.sql:
     ALTER TABLE tasks   ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
     ALTER TABLE projects ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
     ALTER TABLE comments ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

3. Trong GlobalExceptionHandler.java, thêm handler:
   @ExceptionHandler(OptimisticLockingFailureException.class)
   public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
     ProblemDetail pd = ProblemDetail.forStatusAndDetail(
       HttpStatus.CONFLICT,
       "Resource was modified by another user. Please reload and try again.");
     pd.setTitle("Concurrent modification detected");
     pd.setProperty("timestamp", Instant.now());
     return pd;
   }

4. Tạo test trong TaskServiceTest.java:
   @Test
   void should_throw_409_when_optimistic_lock_conflict() { ... }

5. Tạo file: doc/adr/ADR-001-optimistic-locking.md
   Giải thích: tại sao chọn Optimistic Lock thay vì Pessimistic, trade-offs.

Commit: "feat(performance/T1): add @Version optimistic locking to Task, Project, Comment"
```

---

## T2 — Database Composite Indexes (2h)

**Mục tiêu:** Tăng tốc queries thực tế, đặc biệt filter task theo project + status.

**Deliverables:**
- `V4__add_composite_indexes.sql` — 5 composite + partial indexes
- `doc/performance/INDEX_STRATEGY.md` — phân tích query patterns
- EXPLAIN ANALYZE output lưu trong doc

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Tạo V4__add_composite_indexes.sql:
   -- Tasks: filter theo project + status (hot path)
   CREATE INDEX CONCURRENTLY idx_tasks_project_status
     ON tasks(project_id, status)
     WHERE status NOT IN ('DONE', 'CANCELLED');

   -- Tasks: filter theo assignee + status (my tasks)
   CREATE INDEX CONCURRENTLY idx_tasks_assignee_status
     ON tasks(assignee_id, status)
     WHERE status NOT IN ('DONE', 'CANCELLED');

   -- Tasks: overdue query
   CREATE INDEX CONCURRENTLY idx_tasks_due_date_active
     ON tasks(due_date)
     WHERE status NOT IN ('DONE', 'CANCELLED') AND due_date IS NOT NULL;

   -- Notifications: unread count (very frequent)
   CREATE INDEX CONCURRENTLY idx_notifications_user_unread
     ON notifications(user_id, is_read)
     WHERE is_read = false;

   -- Comments: by task (list comments)
   CREATE INDEX CONCURRENTLY idx_comments_task_created
     ON comments(task_id, created_at DESC);

2. Start docker-compose postgres, run Flyway:
   cd ~/java_learning/taskflow
   mvn flyway:migrate -Dspring.profiles.active=dev

3. Kiểm tra indexes:
   psql -U postgres taskflow_dev -c "
   SELECT indexname, indexdef FROM pg_indexes
   WHERE tablename IN ('tasks','notifications','comments')
   AND indexname LIKE 'idx_%';"

4. Tạo doc/performance/INDEX_STRATEGY.md:
   - Liệt kê các query patterns chính
   - Tại sao chọn partial index (WHERE status NOT IN ...)
   - Index không dùng → cần DROP

Commit: "perf(T2): add composite + partial indexes for hot query paths"
```

---

## T3 — `@EntityGraph` Fix N+1 (2h)

**Mục tiêu:** Từ 301+ queries xuống còn 1-2 queries khi list tasks.

**Deliverables:**
- `TaskRepository.java` — 3 methods với `@EntityGraph`
- `TaskService.java` — bật SQL logging khi dev
- Test: `NPlusOneTest.java` dùng `SQLStatementCountValidator`
- `doc/performance/N_PLUS_ONE_FIX.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Thêm vào pom.xml (test scope):
   <dependency>
     <groupId>io.hypersistence</groupId>
     <artifactId>hypersistence-utils-hibernate-63</artifactId>
     <version>3.7.5</version>
     <scope>test</scope>
   </dependency>

2. Refactor TaskRepository.java — thêm @EntityGraph:
   @EntityGraph(attributePaths = {"assignee", "project", "labels"})
   Page<Task> findByProjectId(Long projectId, Pageable pageable);

   @EntityGraph(attributePaths = {"assignee", "project", "labels"})
   @Query("SELECT t FROM Task t WHERE t.assignee.id = :userId AND t.status NOT IN ('DONE','CANCELLED')")
   List<Task> findActiveTasksByAssignee(@Param("userId") Long userId);

   // DTO Projection (read-only list)
   @Query("""
     SELECT new com.taskflow.dto.response.TaskListItem(
       t.id, t.title, t.status, t.priority, t.dueDate,
       a.username, a.id, p.name
     )
     FROM Task t
     LEFT JOIN t.assignee a
     LEFT JOIN t.project p
     WHERE t.project.id = :projectId
   """)
   List<TaskListItem> findTaskListItems(@Param("projectId") Long projectId);

3. Trong application.yml (dev profile):
   logging:
     level:
       org.hibernate.SQL: DEBUG
       org.hibernate.orm.jdbc.bind: TRACE

4. Tạo test NPlusOneTest.java:
   @DataJpaTest
   class NPlusOneTest extends AbstractIntegrationTest {
     @Test
     void listTasks_shouldNotCauseNPlusOne() {
       SQLStatementCountValidator.reset();
       taskRepository.findByProjectId(1L, PageRequest.of(0, 20));
       SQLStatementCountValidator.assertSelectCount(1);
     }
   }

5. Tạo doc/performance/N_PLUS_ONE_FIX.md:
   - Trước: N+1 vấn đề là gì
   - Sau: @EntityGraph giải quyết thế nào
   - So sánh: EntityGraph vs JOIN FETCH vs DTO Projection

Commit: "perf(T3): @EntityGraph on TaskRepository — fix N+1 queries"
```

---

## T4 — DTO Projection `TaskListItem` Record (2h)

**Mục tiêu:** Read-only board view không load full entity, tối ưu cho list/grid.

**Deliverables:**
- `TaskListItem.java` — Java 21 Record với constructor mapping
- `TaskQueryService.java` — tách query-side logic (CQRS POC nhỏ)
- API endpoint mới: `GET /api/tasks/board?projectId=` trả `TaskListItem`
- `doc/architecture/QUERY_PROJECTION.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Tạo src/main/java/com/taskflow/dto/response/TaskListItem.java:
   public record TaskListItem(
     Long id,
     String title,
     TaskStatus status,
     Priority priority,
     LocalDate dueDate,
     String assigneeUsername,
     Long assigneeId,
     String projectName
   ) {}

2. Tạo src/main/java/com/taskflow/service/TaskQueryService.java:
   @Service
   @Transactional(readOnly = true)
   @RequiredArgsConstructor
   public class TaskQueryService {

     private final NamedParameterJdbcTemplate jdbc;

     public List<TaskListItem> getBoardView(Long projectId) {
       return jdbc.query("""
         SELECT t.id, t.title, t.status, t.priority, t.due_date,
                u.username AS assignee_username, u.id AS assignee_id,
                p.name AS project_name,
                COUNT(c.id) AS comment_count
         FROM tasks t
         LEFT JOIN users u ON t.assignee_id = u.id
         LEFT JOIN projects p ON t.project_id = p.id
         LEFT JOIN comments c ON c.task_id = t.id
         WHERE t.project_id = :projectId
           AND t.status NOT IN ('DONE','CANCELLED')
         GROUP BY t.id, u.username, u.id, p.name
         ORDER BY t.priority DESC, t.due_date ASC NULLS LAST
         """,
         Map.of("projectId", projectId),
         (rs, row) -> new TaskListItem(
           rs.getLong("id"), rs.getString("title"),
           TaskStatus.valueOf(rs.getString("status")),
           Priority.valueOf(rs.getString("priority")),
           rs.getDate("due_date") != null ? rs.getDate("due_date").toLocalDate() : null,
           rs.getString("assignee_username"), rs.getLong("assignee_id"),
           rs.getString("project_name")
         ));
     }
   }

3. Trong TaskController.java, thêm endpoint:
   @GetMapping("/board")
   public List<TaskListItem> getBoardView(@RequestParam Long projectId) {
     return taskQueryService.getBoardView(projectId);
   }

4. Tạo doc/architecture/QUERY_PROJECTION.md:
   - Tại sao tách TaskQueryService riêng (CQRS preview)
   - NamedParameterJdbcTemplate vs JPA — khi nào dùng cái nào
   - Performance comparison: JPA entity load vs raw SQL projection

Commit: "feat(T4): TaskListItem record + TaskQueryService — read-optimized board view"
```

---

## T5 — HikariCP Production Tuning (2h)

**Mục tiêu:** Connection pool được cấu hình đúng cho production, có leak detection.

**Deliverables:**
- `application-prod.yml` — HikariCP tuning đầy đủ
- `application-dev.yml` — cấu hình nhẹ hơn cho dev
- `doc/performance/HIKARICP_TUNING.md` — giải thích từng param
- Custom Actuator endpoint: pool stats

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Cập nhật src/main/resources/application-prod.yml, thêm:
   spring:
     datasource:
       hikari:
         pool-name: TaskFlowPool-Prod
         maximum-pool-size: 20
         minimum-idle: 5
         connection-timeout: 3000
         idle-timeout: 600000
         max-lifetime: 1800000
         leak-detection-threshold: 60000
         validation-timeout: 5000
         keepalive-time: 120000
         data-source-properties:
           cachePrepStmts: true
           prepStmtCacheSize: 250
           prepStmtCacheSqlLimit: 2048
           useServerPrepStmts: true

2. Cập nhật application-dev.yml:
   spring:
     datasource:
       hikari:
         pool-name: TaskFlowPool-Dev
         maximum-pool-size: 5
         minimum-idle: 1
         leak-detection-threshold: 30000

3. Tạo src/main/java/com/taskflow/actuator/HikariPoolEndpoint.java:
   @Component
   @Endpoint(id = "hikari-pool")
   @RequiredArgsConstructor
   public class HikariPoolEndpoint {
     private final DataSource dataSource;

     @ReadOperation
     public Map<String, Object> poolStats() {
       HikariDataSource ds = (HikariDataSource) dataSource;
       HikariPoolMXBean pool = ds.getHikariPoolMXBean();
       return Map.of(
         "activeConnections", pool.getActiveConnections(),
         "idleConnections", pool.getIdleConnections(),
         "totalConnections", pool.getTotalConnections(),
         "threadsAwaitingConnection", pool.getThreadsAwaitingConnection()
       );
     }
   }

4. Thêm vào application.yml:
   management.endpoints.web.exposure.include: ..., hikari-pool

5. Tạo doc/performance/HIKARICP_TUNING.md:
   - Brian Goetz formula: connections = (core_count * 2) + effective_spindle
   - Từng tham số ý nghĩa + giá trị được chọn
   - Cảnh báo: pool lớn KHÔNG tốt hơn
   - leak-detection: cách đọc stack trace

Commit: "perf(T5): HikariCP production tuning + pool stats actuator endpoint"
```

---

## T6 — SQL Assertion Test với Hypersistence (2h)

**Mục tiêu:** Tự động phát hiện N+1 trong CI, không để regression.

**Deliverables:**
- `NPlusOneTest.java` — test suite đầy đủ
- `TaskRepositoryTest.java` — verify all repository methods
- `doc/testing/SQL_ASSERTION_GUIDE.md`
- GitHub Actions step: chạy performance tests

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Trong pom.xml thêm (test scope):
   <dependency>
     <groupId>io.hypersistence</groupId>
     <artifactId>hypersistence-utils-hibernate-63</artifactId>
     <version>3.7.5</version>
     <scope>test</scope>
   </dependency>

2. Tạo src/test/java/com/taskflow/performance/NPlusOneTest.java:
   @DataJpaTest
   @AutoConfigureTestDatabase(replace = NONE)
   class NPlusOneTest extends AbstractIntegrationTest {

     @Autowired TaskRepository taskRepository;
     @Autowired ProjectRepository projectRepository;

     @Test
     @DisplayName("List tasks by project: exactly 1 SELECT")
     void listTasksByProject_shouldUseOneQuery() {
       SQLStatementCountValidator.reset();
       taskRepository.findByProjectId(1L, PageRequest.of(0, 20));
       SQLStatementCountValidator.assertSelectCount(1);
     }

     @Test
     @DisplayName("Active tasks by assignee: exactly 1 SELECT")
     void activeTasksByAssignee_shouldUseOneQuery() {
       SQLStatementCountValidator.reset();
       taskRepository.findActiveTasksByAssignee(1L);
       SQLStatementCountValidator.assertSelectCount(1);
     }

     @Test
     @DisplayName("Board view projection: exactly 1 SELECT")
     void boardView_shouldUseOneNativeQuery() {
       SQLStatementCountValidator.reset();
       taskRepository.findTaskListItems(1L);
       SQLStatementCountValidator.assertSelectCount(1);
     }
   }

3. Chạy và verify xanh:
   mvn test -Dtest=NPlusOneTest -q

4. Tạo doc/testing/SQL_ASSERTION_GUIDE.md:
   - Cách dùng SQLStatementCountValidator
   - Cách tích hợp vào test pyramid
   - Khi nào cần viết N+1 test

Commit: "test(T6): N+1 assertion tests with hypersistence-utils"
```

---

## T7 — Read Replica Routing POC + Doc (2h)

**Mục tiêu:** Chuẩn bị codebase cho read replica routing khi scale.

**Deliverables:**
- `RoutingDataSource.java` — AbstractRoutingDataSource impl
- `DataSourceConfig.java` — conditional bean (chỉ active khi có replica config)
- `application-prod.yml` — replica datasource properties
- `doc/architecture/READ_REPLICA_ROUTING.md` — full guide + replication lag strategy

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Tạo src/main/java/com/taskflow/config/RoutingDataSource.java:
   public class RoutingDataSource extends AbstractRoutingDataSource {
     @Override
     protected Object determineCurrentLookupKey() {
       return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
         ? DataSourceType.REPLICA
         : DataSourceType.PRIMARY;
     }
   }

   public enum DataSourceType { PRIMARY, REPLICA }

2. Tạo src/main/java/com/taskflow/config/DataSourceConfig.java:
   @Configuration
   @ConditionalOnProperty(name = "app.datasource.replica.url")
   public class DataSourceConfig {

     @Bean @Primary
     public DataSource routingDataSource(
       @Qualifier("primaryDs") DataSource primary,
       @Qualifier("replicaDs") DataSource replica) {
         RoutingDataSource ds = new RoutingDataSource();
         ds.setTargetDataSources(Map.of(PRIMARY, primary, REPLICA, replica));
         ds.setDefaultTargetDataSource(primary);
         return ds;
     }

     @Bean("primaryDs")
     @ConfigurationProperties("spring.datasource.primary")
     public DataSource primaryDataSource() {
       return DataSourceBuilder.create().build();
     }

     @Bean("replicaDs")
     @ConfigurationProperties("spring.datasource.replica")
     public DataSource replicaDataSource() {
       return DataSourceBuilder.create().build();
     }
   }

3. Thêm vào application-prod.yml (commented out — activate khi có replica):
   # app:
   #   datasource:
   #     replica:
   #       url: jdbc:postgresql://replica-host:5432/taskflow
   #       username: ${DB_REPLICA_USER}
   #       password: ${DB_REPLICA_PASSWORD}

4. Verify @Transactional(readOnly = true) routes đúng:
   - TaskService.getTask() → @Transactional(readOnly = true) → REPLICA
   - TaskService.createTask() → @Transactional → PRIMARY

5. Tạo doc/architecture/READ_REPLICA_ROUTING.md:
   - Khi nào cần (traffic > 1000 req/s reads)
   - Replication lag problem + giải pháp read-your-writes
   - Monitoring: replica lag metric

Commit: "feat(T7): read replica routing POC with AbstractRoutingDataSource"
```

---

# Phase 2 — Resilience & Reliability (18h)

---

## T8 — Resilience4j Dependency + Config (2h)

**Mục tiêu:** Scaffold Resilience4j vào project với config production-ready.

**Deliverables:**
- `pom.xml` — Resilience4j dependencies
- `application.yml` — config cho circuit breaker, retry, bulkhead
- `Resilience4jConfig.java` — Spring bean config
- `doc/resilience/RESILIENCE4J_OVERVIEW.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Thêm vào pom.xml:
   <dependency>
     <groupId>io.github.resilience4j</groupId>
     <artifactId>resilience4j-spring-boot3</artifactId>
     <version>2.2.0</version>
   </dependency>
   <dependency>
     <groupId>io.github.resilience4j</groupId>
     <artifactId>resilience4j-micrometer</artifactId>
     <version>2.2.0</version>
   </dependency>
   <dependency>
     <groupId>org.springframework.boot</groupId>
     <artifactId>spring-boot-starter-aop</artifactId>
   </dependency>

2. Thêm vào application.yml:
   resilience4j:
     circuitbreaker:
       instances:
         redisCache:
           sliding-window-size: 10
           failure-rate-threshold: 50
           wait-duration-in-open-state: 30s
           permitted-number-of-calls-in-half-open-state: 3
           register-health-indicator: true
         kafkaPublish:
           sliding-window-size: 5
           failure-rate-threshold: 60
           wait-duration-in-open-state: 60s
     retry:
       instances:
         kafkaPublish:
           max-attempts: 3
           wait-duration: 1s
           exponential-backoff-multiplier: 2
           retry-exceptions:
             - org.apache.kafka.common.errors.RetriableException
             - java.io.IOException
     bulkhead:
       instances:
         reportGeneration:
           max-concurrent-calls: 5
           max-wait-duration: 0
     timelimiter:
       instances:
         externalApi:
           timeout-duration: 5s

3. Expose Resilience4j metrics qua Actuator:
   management:
     health:
       circuitbreakers:
         enabled: true
     endpoint:
       health:
         show-details: always

4. Tạo doc/resilience/RESILIENCE4J_OVERVIEW.md:
   - 4 patterns: Circuit Breaker, Retry, Bulkhead, TimeLimiter
   - State machine: CLOSED → OPEN → HALF_OPEN
   - Khi nào dùng pattern nào
   - Monitoring: /actuator/health/circuitbreakers

Commit: "feat(T8): add Resilience4j dependency + production config"
```

---

## T9 — `@CircuitBreaker` cho Redis Cache (2h)

**Mục tiêu:** App không crash khi Redis down, tự fallback về DB.

**Deliverables:**
- `ProjectService.java` — `@CircuitBreaker` + fallback method
- `TaskService.java` — circuit breaker cho cache operations
- Test: `CircuitBreakerTest.java` — simulate Redis down
- `doc/resilience/CIRCUIT_BREAKER.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Refactor ProjectService.java:
   @CircuitBreaker(name = "redisCache", fallbackMethod = "getProjectByIdFallback")
   @Cacheable(value = "projects", key = "#id")
   public ProjectResponse getProjectById(Long id) {
     return projectRepository.findById(id)
       .map(projectMapper::toResponse)
       .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
   }

   // fallback — Redis down, go straight to DB
   private ProjectResponse getProjectByIdFallback(Long id, Throwable ex) {
     log.warn("Redis circuit OPEN for project {}, falling back to DB. Cause: {}", id, ex.getMessage());
     return projectRepository.findById(id)
       .map(projectMapper::toResponse)
       .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
   }

2. Tương tự cho TaskService.findTaskById() — thêm circuit breaker + fallback

3. Tạo test src/test/java/com/taskflow/resilience/CircuitBreakerTest.java:
   @SpringBootTest
   class CircuitBreakerTest extends AbstractIntegrationTest {

     @Test
     void getProject_whenRedisDown_shouldFallbackToDb() {
       // Stop Redis container
       // Call getProjectById
       // Should return data from DB (not throw)
       // Assert circuit eventually opens
     }
   }

4. Tạo doc/resilience/CIRCUIT_BREAKER.md:
   - Cascading failure scenario (Redis → DB overload)
   - 3 states + transition rules
   - Monitoring: /actuator/health/circuitbreakers
   - AlertManager rule: fire khi circuit OPEN > 5m

Commit: "feat(T9): @CircuitBreaker on ProjectService + TaskService cache — Redis fallback"
```

---

## T10 — `@Retry` cho Kafka Producer (2h)

**Mục tiêu:** Event publish tự retry với exponential backoff khi Kafka broker flaky.

**Deliverables:**
- `NotificationProducer.java` — `@Retry` + `@CircuitBreaker` stacked
- `KafkaProducerConfig.java` — producer timeout, acks config
- Test: `KafkaRetryTest.java`
- `doc/resilience/KAFKA_RETRY.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Refactor NotificationProducer.java:
   @Retry(name = "kafkaPublish", fallbackMethod = "saveToOutbox")
   @CircuitBreaker(name = "kafkaPublish", fallbackMethod = "saveToOutbox")
   public void sendTaskAssigned(TaskAssignedEvent event) {
     kafkaTemplate.send("task-assigned", event.getTaskId().toString(), event)
       .whenComplete((result, ex) -> {
         if (ex != null) throw new KafkaPublishException("Failed to send", ex);
         log.info("Published task-assigned event: {}", event.getTaskId());
       });
   }

   // Fallback: save to outbox for reliable delivery (T11-T12 implements this)
   private void saveToOutbox(TaskAssignedEvent event, Throwable ex) {
     log.warn("Kafka unavailable, saving to outbox: {}", event.getTaskId());
     // outboxRepository.save(...) — T11-T12 sẽ implement
   }

2. Cập nhật KafkaConfig.java — producer timeout:
   producerProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
   producerProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 10000);
   producerProps.put(ProducerConfig.ACKS_CONFIG, "all");  // strongest guarantee
   producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

3. Tạo doc/resilience/KAFKA_RETRY.md:
   - Retry vs Kafka internal retry: khác nhau thế nào
   - ACKS=all + idempotence producer: exactly-once
   - Exponential backoff: 1s → 2s → 4s
   - DLT (Dead Letter Topic) — đã có từ S6, giải thích lại trong context resilience

Commit: "feat(T10): @Retry + @CircuitBreaker on Kafka producer with ACKS=all"
```

---

## T11 — Outbox Table + Entity (Flyway V5) (2h)

**Mục tiêu:** Tạo nền tảng cho Outbox pattern — không mất event khi Kafka down.

**Deliverables:**
- `V5__create_outbox_table.sql` — Flyway migration
- `OutboxEvent.java` — entity + builder
- `OutboxEventRepository.java` — custom queries
- `doc/resilience/OUTBOX_PATTERN.md` — giải thích vấn đề + giải pháp

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Tạo V5__create_outbox_table.sql:
   CREATE TABLE outbox_events (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     aggregate_type VARCHAR(100) NOT NULL,
     aggregate_id VARCHAR(100) NOT NULL,
     event_type VARCHAR(100) NOT NULL,
     payload JSONB NOT NULL,
     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
     published_at TIMESTAMP,
     retry_count INT NOT NULL DEFAULT 0,
     last_error TEXT
   );

   -- Index cho poller query (unpublished events, ordered by created_at)
   CREATE INDEX idx_outbox_unpublished
     ON outbox_events(created_at)
     WHERE published_at IS NULL;

2. Tạo src/main/java/com/taskflow/outbox/OutboxEvent.java:
   @Entity @Table(name = "outbox_events")
   @Getter @Builder
   public class OutboxEvent {
     @Id private UUID id;
     private String aggregateType;
     private String aggregateId;
     private String eventType;
     @Column(columnDefinition = "jsonb") private String payload;
     private Instant createdAt;
     private Instant publishedAt;
     private int retryCount;
     private String lastError;
   }

3. Tạo OutboxEventRepository.java:
   public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

     @Lock(LockModeType.PESSIMISTIC_WRITE)
     @Query("SELECT e FROM OutboxEvent e WHERE e.publishedAt IS NULL ORDER BY e.createdAt ASC LIMIT :limit")
     List<OutboxEvent> findUnpublishedForUpdate(@Param("limit") int limit);

     @Modifying
     @Query("UPDATE OutboxEvent e SET e.publishedAt = :now WHERE e.id = :id")
     void markPublished(@Param("id") UUID id, @Param("now") Instant now);
   }

4. Tạo doc/resilience/OUTBOX_PATTERN.md:
   - Dual-write problem: tại sao direct Kafka publish không reliable
   - Outbox flow: INSERT trong cùng DB transaction → poller publish
   - ACID guarantee: outbox table + business data cùng 1 transaction
   - Debezium CDC: alternative production approach

Commit: "feat(T11): outbox table Flyway V5 + OutboxEvent entity + repository"
```

---

## T12 — OutboxPoller Implementation (2h)

**Mục tiêu:** Poller đọc outbox và publish lên Kafka, idempotent.

**Deliverables:**
- `OutboxPoller.java` — `@Scheduled` + Pessimistic Lock + publish
- `ObjectMapperConfig.java` — shared ObjectMapper bean
- Test: `OutboxPollerTest.java`
- `doc/resilience/OUTBOX_POLLER.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Tạo src/main/java/com/taskflow/outbox/OutboxPoller.java:
   @Component
   @RequiredArgsConstructor
   @Slf4j
   public class OutboxPoller {

     private final OutboxEventRepository outboxRepo;
     private final KafkaTemplate<String, String> kafkaTemplate;
     private static final int BATCH_SIZE = 100;

     @Scheduled(fixedDelay = 1000)    // every 1 second
     @Transactional
     public void poll() {
       List<OutboxEvent> events = outboxRepo.findUnpublishedForUpdate(BATCH_SIZE);

       for (OutboxEvent event : events) {
         String topic = topicFor(event.getEventType());
         try {
           kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
             .get(5, TimeUnit.SECONDS);    // blocking send with timeout
           outboxRepo.markPublished(event.getId(), Instant.now());
           log.debug("Published outbox event: {} {}", event.getEventType(), event.getId());
         } catch (Exception ex) {
           outboxRepo.markRetryFailed(event.getId(), ex.getMessage());
           log.error("Failed to publish outbox event: {}", event.getId(), ex);
         }
       }
     }

     private String topicFor(String eventType) {
       return switch (eventType) {
         case "TaskAssigned" -> "task-assigned";
         case "TaskCompleted" -> "task-completed";
         case "CommentAdded" -> "comment-added";
         default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
       };
     }
   }

2. Thêm vào application.yml:
   spring:
     task:
       scheduling:
         pool:
           size: 2

3. Tạo OutboxPollerTest.java:
   Verify: event in outbox → poll → kafka receives → publishedAt set

4. Tạo doc/resilience/OUTBOX_POLLER.md:
   - Polling vs CDC (Debezium) trade-offs
   - PESSIMISTIC_WRITE: tại sao cần khi nhiều instances chạy
   - retry_count + last_error: monitoring failed events
   - Tuning: fixedDelay vs fixedRate, BATCH_SIZE

Commit: "feat(T12): OutboxPoller — scheduled publish with pessimistic lock + retry tracking"
```

---

## T13 — Refactor NotificationProducer → Outbox (2h)

**Mục tiêu:** Kết nối Outbox pattern với business logic, đảm bảo event không bao giờ mất.

**Deliverables:**
- `NotificationProducer.java` — rewrite để write outbox thay vì direct Kafka
- `TaskService.java` — outbox event trong cùng `@Transactional`
- `ObjectMapper` serialization util
- Test: `OutboxIntegrationTest.java`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Refactor NotificationProducer.java — từ direct Kafka → outbox:
   @Service
   @RequiredArgsConstructor
   @Transactional    // QUAN TRỌNG: phải trong cùng transaction với caller
   public class NotificationProducer {

     private final OutboxEventRepository outboxRepo;
     private final ObjectMapper objectMapper;

     public void sendTaskAssigned(TaskAssignedEvent event) {
       save("Task", event.getTaskId().toString(), "TaskAssigned", event);
     }

     public void sendTaskCompleted(TaskCompletedEvent event) {
       save("Task", event.getTaskId().toString(), "TaskCompleted", event);
     }

     public void sendCommentAdded(CommentAddedEvent event) {
       save("Comment", event.getCommentId().toString(), "CommentAdded", event);
     }

     private void save(String aggregateType, String aggregateId, String eventType, Object payload) {
       try {
         outboxRepo.save(OutboxEvent.builder()
           .id(UUID.randomUUID())
           .aggregateType(aggregateType)
           .aggregateId(aggregateId)
           .eventType(eventType)
           .payload(objectMapper.writeValueAsString(payload))
           .createdAt(Instant.now())
           .retryCount(0)
           .build());
       } catch (JsonProcessingException e) {
         throw new RuntimeException("Failed to serialize event", e);
       }
     }
   }

2. Verify TaskService.assignTask() — outbox event trong cùng @Transactional:
   @Transactional
   public TaskResponse assignTask(Long taskId, Long assigneeId, Long currentUserId) {
     Task task = findTask(taskId);
     task.setAssignee(findUser(assigneeId));
     taskRepository.save(task);
     // Event lưu cùng TX → không bao giờ mất
     notificationProducer.sendTaskAssigned(new TaskAssignedEvent(...));
     return taskMapper.toResponse(task);
   }

3. Tạo test OutboxIntegrationTest.java:
   - Simulate Kafka down khi tạo task
   - Verify task được tạo, outbox event được lưu
   - Start Kafka, chạy OutboxPoller
   - Verify event được publish sau khi Kafka up

4. Tạo doc/resilience/OUTBOX_INTEGRATION.md:
   - Before/After diagram: direct publish vs outbox
   - Transaction boundary explanation
   - Idempotency: duplicate publish prevention (T14)

Commit: "refactor(T13): NotificationProducer writes to outbox table within same TX"
```

---

## T14 — IdempotencyService + Redis (2h)

**Mục tiêu:** Ngăn duplicate task/project tạo khi client retry.

**Deliverables:**
- `IdempotencyService.java` — Redis-based idempotency store
- `IdempotencyRecord.java` — Record lưu response
- Test: `IdempotencyServiceTest.java`
- `doc/resilience/IDEMPOTENCY.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Tạo src/main/java/com/taskflow/resilience/IdempotencyRecord.java:
   public record IdempotencyRecord(
     int httpStatus,
     String body,
     String contentType,
     Instant processedAt,
     boolean complete
   ) {
     public static IdempotencyRecord processing() {
       return new IdempotencyRecord(0, null, null, Instant.now(), false);
     }

     public static IdempotencyRecord completed(int status, String body) {
       return new IdempotencyRecord(status, body, "application/json", Instant.now(), true);
     }
   }

2. Tạo src/main/java/com/taskflow/resilience/IdempotencyService.java:
   @Service
   @RequiredArgsConstructor
   @Slf4j
   public class IdempotencyService {

     private final RedisTemplate<String, IdempotencyRecord> redis;
     private final ObjectMapper objectMapper;
     private static final Duration TTL = Duration.ofHours(24);
     private static final Duration PROCESSING_TTL = Duration.ofMinutes(5);

     public <T> ResponseEntity<T> executeOnce(
       String idempotencyKey,
       Supplier<ResponseEntity<T>> action) {

       String key = "idemp:" + idempotencyKey;

       // 1. Try to acquire
       Boolean acquired = redis.opsForValue()
         .setIfAbsent(key, IdempotencyRecord.processing(), PROCESSING_TTL);

       if (Boolean.FALSE.equals(acquired)) {
         IdempotencyRecord cached = redis.opsForValue().get(key);
         if (cached != null && cached.complete()) {
           log.debug("Returning cached idempotency response for key: {}", idempotencyKey);
           return deserializeResponse(cached);
         }
         throw new BusinessException("Request is already being processed");
       }

       // 2. Execute
       try {
         ResponseEntity<T> response = action.get();
         String body = objectMapper.writeValueAsString(response.getBody());
         redis.opsForValue().set(key,
           IdempotencyRecord.completed(response.getStatusCode().value(), body),
           TTL);
         return response;
       } catch (Exception e) {
         redis.delete(key);  // release lock on failure
         throw e;
       }
     }
   }

3. Tạo test IdempotencyServiceTest.java:
   - Same key twice → second call returns cached result
   - Different keys → both execute
   - Key expired → re-execute

4. Tạo doc/resilience/IDEMPOTENCY.md:
   - Why idempotency matters (network retry, double-click)
   - Redis SETNX as distributed lock
   - TTL strategy: 24h for completed, 5m for processing
   - Client contract: Idempotency-Key header

Commit: "feat(T14): IdempotencyService with Redis — prevent duplicate POST operations"
```

---

## T15 — Apply Idempotency + Graceful Shutdown + Redisson (2h)

**Mục tiêu:** Wire idempotency vào controllers, cấu hình graceful shutdown, distributed job lock.

**Deliverables:**
- `TaskController.java` + `ProjectController.java` — `Idempotency-Key` header
- `application.yml` — graceful shutdown config
- `RedissonConfig.java` + `OverdueTaskNotifier.java` — distributed job
- `doc/resilience/DISTRIBUTED_SYSTEMS.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Cập nhật TaskController.java:
   @PostMapping
   public ResponseEntity<TaskResponse> createTask(
     @RequestHeader(value = "Idempotency-Key", required = false) UUID idempotencyKey,
     @Valid @RequestBody CreateTaskRequest request,
     Authentication auth) {

     if (idempotencyKey != null) {
       return idempotencyService.executeOnce(
         idempotencyKey.toString(),
         () -> ResponseEntity.status(201).body(taskService.createTask(request, auth)));
     }
     return ResponseEntity.status(201).body(taskService.createTask(request, auth));
   }

   // Tương tự cho POST /projects

2. Thêm vào application.yml:
   server:
     shutdown: graceful
   spring:
     lifecycle:
       timeout-per-shutdown-phase: 30s

3. Thêm pom.xml:
   <dependency>
     <groupId>org.redisson</groupId>
     <artifactId>redisson-spring-boot-starter</artifactId>
     <version>3.27.2</version>
   </dependency>

4. Tạo src/main/java/com/taskflow/scheduler/OverdueTaskNotifier.java:
   @Component
   @RequiredArgsConstructor
   @Slf4j
   public class OverdueTaskNotifier {

     private final RedissonClient redisson;
     private final TaskRepository taskRepository;
     private final NotificationProducer producer;

     @Scheduled(cron = "0 0 9 * * *")    // 9 AM daily
     public void notifyOverdueTasks() {
       RLock lock = redisson.getLock("job:overdue-notifier");

       boolean acquired = lock.tryLock(0, 30, TimeUnit.MINUTES);
       if (!acquired) {
         log.info("Overdue notifier already running on another instance — skipping");
         return;
       }
       try {
         List<Task> overdue = taskRepository.findOverdueTasks(LocalDate.now());
         log.info("Processing {} overdue tasks", overdue.size());
         // send notifications
       } finally {
         lock.unlock();
       }
     }
   }

5. Tạo doc/resilience/DISTRIBUTED_SYSTEMS.md:
   - Idempotency wiring trong controller layer
   - Graceful shutdown sequence: K8s SIGTERM → stop accept → drain → exit
   - Redlock vs SETNX — Redisson Lua script guarantee
   - Scheduled job: 1 instance trong cluster

Commit: "feat(T15): apply idempotency + graceful shutdown + Redisson distributed lock"
```

---

# Phase 3 — Observability (16h)

---

## T16 — Micrometer Tracing + OpenTelemetry + Jaeger (2h)

**Mục tiêu:** Trace ID có trong mọi log, Jaeger UI hiển thị span tree.

**Deliverables:**
- `pom.xml` — Micrometer Tracing + OTel exporter
- `application.yml` — tracing config (sampling, OTLP endpoint)
- `docker-compose.yml` — Jaeger service
- Test: verify traceId trong logs

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Thêm vào pom.xml:
   <dependency>
     <groupId>io.micrometer</groupId>
     <artifactId>micrometer-tracing-bridge-otel</artifactId>
   </dependency>
   <dependency>
     <groupId>io.opentelemetry</groupId>
     <artifactId>opentelemetry-exporter-otlp</artifactId>
   </dependency>
   <dependency>
     <groupId>io.opentelemetry</groupId>
     <artifactId>opentelemetry-sdk-extension-autoconfigure</artifactId>
   </dependency>

2. Thêm vào application.yml:
   management:
     tracing:
       sampling:
         probability: 1.0    # dev: 100%
   otel:
     exporter:
       otlp:
         endpoint: http://jaeger:4317
     service:
       name: taskflow-api
   logging:
     pattern:
       level: "%5p [%X{traceId:-},${X{spanId:-}}]"

3. Thêm Jaeger vào docker-compose.yml:
   jaeger:
     image: jaegertracing/all-in-one:1.57
     ports:
       - "16686:16686"    # Jaeger UI
       - "4317:4317"      # OTLP gRPC
     environment:
       COLLECTOR_OTLP_ENABLED: "true"

4. Test:
   - Start: docker-compose up -d jaeger
   - Gọi API: curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/tasks
   - Kiểm tra logs: phải có traceId trong bracket
   - Mở Jaeger UI: http://localhost:16686
   - Tìm service "taskflow-api" → thấy span tree

5. Tạo doc/observability/DISTRIBUTED_TRACING.md:
   - Trace vs Span: định nghĩa
   - B3 vs W3C traceparent format
   - Sampling strategy: 100% dev, 10% prod
   - Jaeger vs Zipkin vs Datadog: khi nào dùng gì

Commit: "feat(T16): Micrometer Tracing + OpenTelemetry + Jaeger integration"
```

---

## T17 — Trace Propagation qua Kafka (2h)

**Mục tiêu:** TraceId lan truyền qua Kafka consumer, Jaeger hiển thị end-to-end flow.

**Deliverables:**
- `KafkaConfig.java` — ObservationRegistry + tracing interceptors
- `NotificationConsumer.java` — verify span continuity
- Custom span trong critical Kafka processing
- `doc/observability/KAFKA_TRACING.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Cập nhật KafkaConfig.java:
   @Bean
   public ProducerFactory<String, Object> producerFactory(
     ObservationRegistry observationRegistry) {
     DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(producerProps());
     factory.addListener(new MicrometerProducerListener<>(observationRegistry));
     return factory;
   }

   @Bean
   public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
     ConsumerFactory<String, Object> consumerFactory,
     ObservationRegistry observationRegistry) {
     var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
     factory.setConsumerFactory(consumerFactory);
     factory.getContainerProperties().setObservationEnabled(true);
     return factory;
   }

2. Verify NotificationConsumer.java nhận đúng trace context:
   @KafkaListener(topics = "task-assigned", groupId = "notification-group")
   public void handleTaskAssigned(TaskAssignedEvent event) {
     log.info("Processing task-assigned event — traceId should be propagated: {}",
       MDC.get("traceId"));
     // process...
   }

3. Test end-to-end trace:
   - Gọi POST /api/tasks/{id}/assign/{userId}
   - Kiểm tra Jaeger UI: phải thấy span tree gồm:
     HTTP POST → TaskService.assignTask → DB INSERT → Kafka send → Consumer → DB INSERT notification
   - Tất cả cùng 1 traceId

4. Tạo doc/observability/KAFKA_TRACING.md:
   - W3C traceparent header trong Kafka message headers
   - Producer → Consumer span linking
   - Async span: parent span có thể đã kết thúc trước khi consumer chạy
   - Sampling: không cần sample 100% Kafka messages

Commit: "feat(T17): trace propagation via Kafka producer + consumer — end-to-end spans"
```

---

## T18 — Custom Spans trong Service Methods (2h)

**Mục tiêu:** Span chi tiết cho business logic, dễ tìm bottleneck trong production.

**Deliverables:**
- `TaskService.java` — custom spans cho `createTask`, `assignTask`
- `TracingConfig.java` — Tracer bean config
- `TracingAspect.java` — AOP-based auto span cho `@Traced` methods
- `doc/observability/CUSTOM_SPANS.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Tạo annotation src/main/java/com/taskflow/aop/Traced.java:
   @Target(ElementType.METHOD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Traced {
     String name() default "";
   }

2. Tạo src/main/java/com/taskflow/aop/TracingAspect.java:
   @Aspect
   @Component
   @RequiredArgsConstructor
   @Slf4j
   public class TracingAspect {

     private final Tracer tracer;

     @Around("@annotation(traced)")
     public Object traceMethod(ProceedingJoinPoint pjp, Traced traced) throws Throwable {
       String spanName = traced.name().isEmpty()
         ? pjp.getSignature().getName()
         : traced.name();

       Span span = tracer.nextSpan().name(spanName).start();
       try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
         span.tag("class", pjp.getTarget().getClass().getSimpleName());
         Object result = pjp.proceed();
         return result;
       } catch (Throwable ex) {
         span.error(ex);
         throw ex;
       } finally {
         span.end();
       }
     }
   }

3. Áp dụng @Traced lên các method quan trọng:
   @Traced("create-task-business-logic")
   public TaskResponse createTask(CreateTaskRequest req, Authentication auth) { ... }

   @Traced("assign-task")
   public TaskResponse assignTask(Long taskId, Long assigneeId, Long userId) { ... }

4. Verify trong Jaeger: span "create-task-business-logic" xuất hiện trong trace tree

5. Tạo doc/observability/CUSTOM_SPANS.md:
   - Khi nào cần custom span (business logic > 100ms)
   - @Traced annotation vs manual Tracer API
   - Span tags vs events vs logs: khi nào dùng cái nào
   - Best practices: không tạo quá nhiều span

Commit: "feat(T18): @Traced AOP aspect + custom spans on critical service methods"
```

---

## T19 — MDC Filter (requestId, userId) (2h)

**Mục tiêu:** Mọi log line đều có requestId + userId → dễ debug theo request.

**Deliverables:**
- `MdcFilter.java` — `OncePerRequestFilter`
- `logback-spring.xml` — pattern include MDC fields
- Test: verify MDC fields trong log output
- `doc/observability/MDC_LOGGING.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Tạo src/main/java/com/taskflow/filter/MdcFilter.java:
   @Component
   @Order(1)
   @RequiredArgsConstructor
   public class MdcFilter extends OncePerRequestFilter {

     @Override
     protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                     FilterChain chain) throws IOException, ServletException {
       try {
         // Request ID
         String requestId = req.getHeader("X-Request-Id");
         if (requestId == null || requestId.isBlank()) {
           requestId = UUID.randomUUID().toString().substring(0, 8);
         }
         MDC.put("requestId", requestId);
         MDC.put("method", req.getMethod());
         MDC.put("uri", req.getRequestURI());

         // User ID từ Security context
         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
         if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserDetails ud) {
           MDC.put("userId", extractUserId(ud));
           MDC.put("username", ud.getUsername());
         }

         // Set response header
         res.addHeader("X-Request-Id", requestId);

         chain.doFilter(req, res);
       } finally {
         MDC.clear();
       }
     }
   }

2. Cập nhật application.yml — log pattern với MDC:
   logging:
     pattern:
       console: "%d{HH:mm:ss.SSS} %5p [%X{traceId:-},${X{spanId:-}}] [%X{requestId:-}] [%X{userId:-}] %logger{36} — %msg%n"

3. Test MdcFilterTest.java:
   @WebMvcTest
   class MdcFilterTest {
     @Test
     void request_shouldHaveRequestIdInResponse() {
       mockMvc.perform(get("/api/tasks").header("X-Request-Id", "test-123"))
         .andExpect(header().string("X-Request-Id", "test-123"));
     }

     @Test
     void request_withoutRequestId_shouldGenerateOne() {
       mockMvc.perform(get("/api/tasks"))
         .andExpect(header().exists("X-Request-Id"));
     }
   }

4. Tạo doc/observability/MDC_LOGGING.md:
   - MDC là gì: ThreadLocal map cho Logback
   - Virtual Threads compatibility: MDC không tự propagate → cần manual copy
   - requestId: correlation ID qua distributed system
   - Performance: MDC.clear() bắt buộc trong finally

Commit: "feat(T19): MdcFilter — requestId + userId in all log lines"
```

---

## T20 — JSON Logging với LogstashEncoder (2h)

**Mục tiêu:** Structured JSON logs — dễ parse với Elasticsearch/Loki/Datadog.

**Deliverables:**
- `pom.xml` — logstash-logback-encoder
- `logback-spring.xml` — JSON appender cho prod profile
- `logback-spring.xml` — human-readable cho dev profile
- Test: verify JSON output format
- `doc/observability/STRUCTURED_LOGGING.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Thêm pom.xml:
   <dependency>
     <groupId>net.logstash.logback</groupId>
     <artifactId>logstash-logback-encoder</artifactId>
     <version>7.4</version>
   </dependency>

2. Tạo src/main/resources/logback-spring.xml:
   <configuration>
     <!-- Dev profile: human-readable -->
     <springProfile name="dev">
       <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
         <encoder>
           <pattern>%d{HH:mm:ss} %5p [%X{traceId:-},${X{spanId:-}}] [%X{requestId:-}] %-40.40logger{39} : %m%n</pattern>
         </encoder>
       </appender>
       <root level="DEBUG">
         <appender-ref ref="CONSOLE"/>
       </root>
     </springProfile>

     <!-- Prod profile: JSON structured -->
     <springProfile name="prod">
       <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
         <encoder class="net.logstash.logback.encoder.LogstashEncoder">
           <includeMdcKeyName>requestId</includeMdcKeyName>
           <includeMdcKeyName>userId</includeMdcKeyName>
           <includeMdcKeyName>username</includeMdcKeyName>
           <includeMdcKeyName>traceId</includeMdcKeyName>
           <includeMdcKeyName>spanId</includeMdcKeyName>
           <customFields>{"app":"taskflow-api","env":"prod"}</customFields>
         </encoder>
       </appender>
       <root level="INFO">
         <appender-ref ref="JSON_CONSOLE"/>
       </root>
     </springProfile>
   </configuration>

3. Test JSON output:
   # Chạy với prod profile
   SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run &
   curl http://localhost:8080/api/tasks -H "Authorization: Bearer $TOKEN"
   # Log phải ra JSON format với traceId, requestId, userId

4. Tạo doc/observability/STRUCTURED_LOGGING.md:
   - Text logging vs JSON logging: tại sao JSON tốt hơn cho production
   - LogstashEncoder fields: @timestamp, level, logger, message, mdc fields
   - Log levels: DEBUG (dev), INFO (prod) — tại sao không dùng DEBUG prod
   - Correlation: dùng requestId + traceId để tìm log của 1 request cụ thể

Commit: "feat(T20): LogstashEncoder JSON logging for prod + human-readable for dev"
```

---

## T21 — Async Appender + Log Pattern Tuning (2h)

**Mục tiêu:** Logging không block request thread, không ảnh hưởng latency.

**Deliverables:**
- `logback-spring.xml` — AsyncAppender wrapping JSON appender
- Log sampling cho high-volume DEBUG logs
- Performance benchmark: sync vs async logging latency
- `doc/observability/ASYNC_LOGGING.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Cập nhật logback-spring.xml — wrap prod appender với AsyncAppender:
   <springProfile name="prod">
     <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
       <!-- ... LogstashEncoder config từ T20 ... -->
     </appender>

     <!-- Async wrapper: logs không block request thread -->
     <appender name="ASYNC_JSON" class="ch.qos.logback.classic.AsyncAppender">
       <appender-ref ref="JSON_CONSOLE"/>
       <queueSize>512</queueSize>
       <discardingThreshold>0</discardingThreshold>    <!-- 0 = không drop logs -->
       <includeCallerData>false</includeCallerData>     <!-- performance: không compute caller -->
       <neverBlock>false</neverBlock>
     </appender>

     <root level="INFO">
       <appender-ref ref="ASYNC_JSON"/>
     </root>
   </springProfile>

2. Thêm log sampling cho verbose packages:
   <logger name="org.hibernate.SQL" level="DEBUG" additivity="false">
     <springProfile name="dev">
       <appender-ref ref="CONSOLE"/>
     </springProfile>
   </logger>
   <logger name="org.apache.kafka" level="WARN"/>
   <logger name="org.springframework.security" level="WARN"/>

3. Benchmark async vs sync (optional, nếu có thời gian):
   Thêm test đo latency thêm khi logging heavy: < 1ms overhead

4. Tạo doc/observability/ASYNC_LOGGING.md:
   - Synchronous logging vấn đề: file I/O blocks request thread
   - AsyncAppender queue: 512 entries — tránh OOM nếu log burst
   - discardingThreshold: 0 (production, không drop), 20% (high throughput)
   - neverBlock: false = block khi queue full (không mất logs) vs true = drop when full
   - Trade-off: memory vs log loss

Commit: "feat(T21): AsyncAppender for prod — non-blocking logging + log level tuning"
```

---

## T22 — Custom KafkaHealthIndicator (2h)

**Mục tiêu:** K8s biết Kafka down → restart pod đúng lúc.

**Deliverables:**
- `KafkaHealthIndicator.java` — custom HealthIndicator
- `RedisHealthIndicator.java` — custom Redis check
- Test: health endpoint khi Kafka down → `DOWN`
- `doc/observability/HEALTH_INDICATORS.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Tạo src/main/java/com/taskflow/actuator/KafkaHealthIndicator.java:
   @Component
   @RequiredArgsConstructor
   @Slf4j
   public class KafkaHealthIndicator implements HealthIndicator {

     private final KafkaAdmin kafkaAdmin;

     @Override
     public Health health() {
       try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
         DescribeClusterResult result = client.describeCluster();
         Collection<Node> nodes = result.nodes().get(5, TimeUnit.SECONDS);
         String clusterId = result.clusterId().get(5, TimeUnit.SECONDS);
         return Health.up()
           .withDetail("brokers", nodes.size())
           .withDetail("clusterId", clusterId)
           .withDetail("bootstrapServers", kafkaAdmin.getConfigurationProperties()
             .get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG))
           .build();
       } catch (Exception ex) {
         log.warn("Kafka health check failed", ex);
         return Health.down(ex)
           .withDetail("error", ex.getMessage())
           .build();
       }
     }
   }

2. Tạo OutboxHealthIndicator.java — alert khi có quá nhiều failed events:
   @Component
   @RequiredArgsConstructor
   public class OutboxHealthIndicator implements HealthIndicator {
     private final OutboxEventRepository outboxRepo;
     private static final int WARN_THRESHOLD = 100;
     private static final int DOWN_THRESHOLD = 1000;

     @Override
     public Health health() {
       long failedCount = outboxRepo.countFailedEvents();
       if (failedCount >= DOWN_THRESHOLD) {
         return Health.down().withDetail("failedOutboxEvents", failedCount).build();
       }
       if (failedCount >= WARN_THRESHOLD) {
         return Health.unknown().withDetail("failedOutboxEvents", failedCount).build();
       }
       return Health.up().withDetail("failedOutboxEvents", failedCount).build();
     }
   }

3. Verify: /actuator/health shows kafka, outbox components

4. Tạo doc/observability/HEALTH_INDICATORS.md:
   - K8s liveness vs readiness probe mapping
   - Custom indicator: khi nào nên tạo
   - Kafka DOWN: liveness fail → pod restart
   - Outbox high: readiness fail → traffic stop

Commit: "feat(T22): KafkaHealthIndicator + OutboxHealthIndicator for K8s probes"
```

---

## T23 — Liveness vs Readiness Probe Split (2h)

**Mục tiêu:** K8s tách biệt liveness (restart?) và readiness (receive traffic?) probe.

**Deliverables:**
- `application.yml` — separate liveness/readiness groups
- `K8s probes` config trong deployment YAML
- `ReadinessHealthContributor.java` — custom readiness check
- `doc/observability/K8S_PROBES.md`

**Implementing prompt:**
```
Trong ~/java_learning/taskflow:

1. Cập nhật application.yml:
   management:
     endpoint:
       health:
         show-details: when-authorized
         probes:
           enabled: true             # enables /actuator/health/liveness và /readiness
     health:
       livenessstate:
         enabled: true
       readinessstate:
         enabled: true
       # Kafka DOWN → liveness DOWN (restart pod)
       kafka:
         enabled: true
       # DB DOWN → liveness DOWN
       db:
         enabled: true
       # Circuit breaker OPEN → readiness DOWN (stop traffic)
       circuitbreakers:
         enabled: true

2. Tạo src/main/java/com/taskflow/actuator/WarmupReadinessContributor.java:
   @Component
   public class WarmupReadinessContributor implements HealthContributor {
     private volatile boolean ready = false;

     @PostConstruct
     public void warmup() {
       // Pre-warm Redis connection pool, Kafka consumer
       ready = true;
     }

     @Override
     public Health health() {
       return ready ? Health.up().build() : Health.down().withDetail("reason", "warming up").build();
     }
   }

3. Tạo K8s probe config (lưu trong doc/k8s/probes-example.yaml):
   livenessProbe:
     httpGet:
       path: /actuator/health/liveness
       port: 8080
     initialDelaySeconds: 60
     periodSeconds: 10
     failureThreshold: 3
   readinessProbe:
     httpGet:
       path: /actuator/health/readiness
       port: 8080
     initialDelaySeconds: 30
     periodSeconds: 5
     failureThreshold: 3

4. Test:
   curl http://localhost:8080/actuator/health/liveness
   curl http://localhost:8080/actuator/health/readiness
   # Cả hai phải trả {"status":"UP"}

5. Tạo doc/observability/K8S_PROBES.md:
   - Liveness: "pod alive?" → restart if DOWN
   - Readiness: "pod ready for traffic?" → remove from LB if DOWN
   - Startup probe: cho app khởi động chậm (Kafka connect)
   - Circuit breaker → readiness DOWN: traffic dừng, pod không restart

Commit: "feat(T23): liveness/readiness probe split + K8s config + warmup contributor"
```

---

## T24 — Architecture Docs + ADR Files (2h)

**Mục tiêu:** Document hoá toàn bộ 48h sprint — Senior engineer level documentation.

**Deliverables:**
- `doc/adr/` — 5 ADR files cho các quyết định quan trọng
- `doc/architecture/SYSTEM_DESIGN.md` — C4 diagram (text)
- `SENIOR_IMPLEMENT_PLAN.md` — cập nhật status tất cả tasks ✅
- `doc/TECHNICAL_GUIDE_SENIOR.md` — quick reference

**Implementing prompt:**
```
Trong ~/java_learning/taskflow/doc:

1. Tạo doc/adr/ADR-001-optimistic-locking.md (đã tạo T1 — review và hoàn thiện)
2. Tạo doc/adr/ADR-002-outbox-pattern.md:
   # ADR-002: Transactional Outbox Pattern
   ## Status: Accepted
   ## Context: Dual-write problem giữa DB và Kafka
   ## Decision: Outbox table trong cùng DB transaction
   ## Consequences: +reliable, -polling overhead, -eventual consistency
   ## Alternatives considered: Debezium CDC, Saga pattern

3. Tạo doc/adr/ADR-003-resilience4j.md:
   # ADR-003: Resilience4j for Circuit Breaking
   ## Context: Redis/Kafka có thể down trong production
   ## Decision: Resilience4j với fallback to DB
   ## Alternatives: Hystrix (deprecated), manual try-catch

4. Tạo doc/adr/ADR-004-distributed-tracing.md:
   # ADR-004: Micrometer Tracing + OpenTelemetry
   ## Context: Cần trace request xuyên suốt HTTP → Kafka → Consumer
   ## Decision: OTel với OTLP exporter → Jaeger
   ## Alternatives: Zipkin (B3), Datadog APM (vendor lock-in)

5. Tạo doc/adr/ADR-005-structured-logging.md:
   # ADR-005: JSON Structured Logging
   ## Context: Text logs khó parse trong ELK/Loki
   ## Decision: LogstashEncoder JSON format (prod), plain text (dev)
   ## Consequences: +searchable, +filterable, -human-readability

6. Cập nhật SENIOR_IMPLEMENT_PLAN.md:
   - Đánh dấu tất cả T1-T24 là ✅ done
   - Thêm section "What's Next" (Phase 4-6)

7. Tạo doc/TECHNICAL_GUIDE_SENIOR.md: quick reference 1-pager:
   - Commands hay dùng: curl health, redis-cli keys, kafka topics
   - Actuator endpoints map
   - Log patterns: cách search theo requestId, traceId
   - Circuit breaker reset: POST /actuator/circuitbreakers/{name}/reset

Commit: "docs(T24): ADR files + architecture docs + senior technical guide"
```

---

# Phần C — Tiếp theo (Phase 4–6, Sprint 2)

Sau khi hoàn thành 48h sprint này, các phase còn lại ưu tiên theo thứ tự:

| Phase | Tasks chính | Ước tính |
|-------|-------------|---------|
| Phase 4 — Security | Rate limiting (Bucket4j), HTTP headers, Encryption at rest PII, Vault | ~12h |
| Phase 5 — Architecture | ArchUnit tests, Hexagonal POC (Task module), CQRS, API versioning | ~15h |
| Phase 6 — DevOps | K8s Helm chart, Zero-downtime DB migration, Canary/Blue-Green, JVM tuning, k6 load test | ~20h |

---

## Mẹo Usage Limit Pro (cho Sprint này)

| Ngày | Tasks | Lưu ý |
|------|-------|-------|
| Day 1–2 | T1-T6 (Performance) | Ít code, nhiều config — session ngắn |
| Day 3–4 | T7-T12 (Resilience setup) | T11-T12 nặng nhất — cần 1 session riêng mỗi task |
| Day 5 | T13-T15 (Wire resilience) | T13 nhiều refactoring — dùng /compact giữa chừng |
| Day 6–7 | T16-T21 (Observability) | T16 cần docker-compose up jaeger trước |
| Day 8 | T22-T24 (Health + Docs) | T24 chỉ viết docs — session nhẹ |

**Quy tắc chung:**
- Mỗi task = 1 Claude session mới
- Bắt đầu session bằng cách paste prompt từ file này
- Kết thúc session bằng `git commit` + `git push`
- Dùng `/compact` khi conversation > 15 messages
