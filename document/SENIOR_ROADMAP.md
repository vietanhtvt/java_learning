# TaskFlow API — Senior Java Roadmap

> Bản đồ nâng cấp dự án từ **Mid-level** (đã hoàn thành 4 ngày MVP) lên **Senior Production-grade**.
>
> Tài liệu này bổ sung cho [`TECHNICAL_GUIDE.md`](./TECHNICAL_GUIDE.md):
> - **TECHNICAL_GUIDE.md**: giải thích kỹ thuật **đã implement**.
> - **SENIOR_ROADMAP.md**: liệt kê kỹ thuật **chưa có** + kế hoạch triển khai theo phase.

---

## Mục lục

### Phần A — Phân tích Gap (cái gì còn thiếu)

1. [Nhận xét tổng quan dự án MVP](#1-nhận-xét-tổng-quan-dự-án-mvp)
2. [Phân loại kỹ thuật Senior cần bổ sung](#2-phân-loại-kỹ-thuật-senior-cần-bổ-sung)

### Phần B — Chi tiết Kỹ thuật Senior

#### B.1 — Performance & Scalability
3. [N+1 Query & Fetch Optimization (EntityGraph, @BatchSize)](#3-n1-query--fetch-optimization)
4. [Optimistic Locking với @Version](#4-optimistic-locking-với-version)
5. [Database Indexing Strategy](#5-database-indexing-strategy)
6. [HikariCP Production Tuning](#6-hikaricp-production-tuning)
7. [Read Replica Routing — AbstractRoutingDataSource](#7-read-replica-routing)

#### B.2 — Resilience & Reliability
8. [Resilience4j — Circuit Breaker, Retry, Bulkhead](#8-resilience4j)
9. [Transactional Outbox Pattern (Reliable Kafka Publishing)](#9-transactional-outbox-pattern)
10. [Idempotency Keys cho POST endpoints](#10-idempotency-keys)
11. [Distributed Locking với Redisson](#11-distributed-locking-với-redisson)
12. [Graceful Shutdown & Lifecycle Management](#12-graceful-shutdown)

#### B.3 — Observability (Production-grade)
13. [Distributed Tracing — Micrometer Tracing + OpenTelemetry](#13-distributed-tracing)
14. [Structured Logging với MDC + JSON](#14-structured-logging-với-mdc)
15. [Custom Health Indicators](#15-custom-health-indicators)

#### B.4 — Security (Senior-level)
16. [API Rate Limiting với Bucket4j](#16-api-rate-limiting)
17. [OAuth2 Resource Server (thay JWT thuần)](#17-oauth2-resource-server)
18. [HTTP Security Headers + CSP](#18-http-security-headers)
19. [Encryption at Rest cho PII (JPA AttributeConverter)](#19-encryption-at-rest)
20. [Secrets Management — Vault / AWS Secrets Manager](#20-secrets-management)

#### B.5 — Architecture Patterns
21. [Hexagonal / Clean Architecture (Ports & Adapters)](#21-hexagonal-architecture)
22. [CQRS — tách Command / Query model](#22-cqrs-pattern)
23. [API Versioning Strategy](#23-api-versioning)
24. [ArchUnit — Enforce Architecture Rules](#24-archunit)

#### B.6 — Advanced Testing
25. [Mutation Testing với Pitest](#25-mutation-testing-với-pitest)
26. [Contract Testing — Pact / Spring Cloud Contract](#26-contract-testing)
27. [Performance Testing — k6 / Gatling](#27-performance-testing)
28. [Chaos Engineering — Chaos Monkey](#28-chaos-engineering)

#### B.7 — DevOps Senior
29. [Kubernetes Deployment + Helm Chart](#29-kubernetes-deployment)
30. [Zero-downtime Database Migration (Expand-Contract)](#30-zero-downtime-db-migration)
31. [Blue/Green & Canary Deployment](#31-bluegreen--canary)
32. [JVM Tuning & GC Selection (G1 vs ZGC)](#32-jvm-tuning--gc)

#### B.8 — Modern Java & Advanced Patterns
33. [Virtual Threads (Project Loom) — Java 21](#33-virtual-threads-project-loom--java-21)
34. [GraalVM Native Image — Cold Start & Memory](#34-graalvm-native-image--cold-start--memory)
35. [Saga Pattern — Distributed Transactions](#35-saga-pattern--distributed-transactions)
36. [Caching Strategies — 4 Patterns](#36-caching-strategies--4-patterns)
37. [Dead Letter Queue & Retry Topics (Kafka)](#37-dead-letter-queue--retry-topics-kafka)
38. [Database Sharding & Partitioning](#38-database-sharding--partitioning)
39. [Multi-tenancy Strategies](#39-multi-tenancy-strategies)
40. [API Gateway & BFF Pattern](#40-api-gateway--bff-pattern)
41. [Domain Events vs Integration Events](#41-domain-events-vs-integration-events)
42. [Reactive Programming — WebFlux & R2DBC](#42-reactive-programming--webflux--r2dbc)

### Phần C — Roadmap Triển khai

43. [Roadmap 6 Phase (12 tuần)](#43-roadmap-6-phase-12-tuần)
44. [Effort Estimation Matrix](#44-effort-estimation-matrix)
45. [Quick-win priorities (làm trước)](#45-quick-win-priorities)

---

# Phần A — Phân tích Gap

## 1. Nhận xét tổng quan dự án MVP

Dự án TaskFlow sau 4 ngày đã đạt mức **mid-level production-ready**:

✅ **Đã có:**
- Foundation đầy đủ: Spring Boot 3.3, JPA, Flyway, Security STATELESS với JWT
- Cache layer (Redis), Event-driven (Kafka), AOP cho audit
- Test layered (unit + integration với Testcontainers), Docker, CI/CD, Actuator/Prometheus

❌ **Thiếu (chưa đạt senior bar):**
- **Concurrency safety** — chưa có optimistic/pessimistic lock, dễ bị lost-update khi 2 user edit task cùng lúc
- **Reliability gaps** — Kafka publish có thể mất event nếu DB commit thành công nhưng broker down (chưa có Outbox pattern)
- **No resilience patterns** — Redis/Kafka down sẽ làm app crash, không có circuit breaker
- **No distributed tracing** — request đi qua nhiều layer (HTTP → Service → Kafka → Consumer → DB) mà không có correlation ID
- **Logging chưa structured** — text logs khó parse, không có MDC
- **No rate limiting** — API có thể bị abuse
- **Architecture phẳng** — service phụ thuộc trực tiếp infrastructure (JPA, Redis, Kafka client) — khó test, khó thay storage
- **Test pyramid thiếu top** — chưa có mutation testing, contract testing, performance testing
- **Deployment cơ bản** — Docker chỉ cho dev, chưa có K8s, chưa có blue/green

## 2. Phân loại kỹ thuật Senior cần bổ sung

| Domain | Kỹ thuật | Mức độ ưu tiên |
|--------|----------|----------------|
| Performance | N+1 optimization, Optimistic Lock, HikariCP tuning, Index strategy | 🔴 Cao |
| Performance | Read replica routing | 🟡 Trung bình |
| Reliability | Resilience4j, Outbox Pattern, Idempotency keys | 🔴 Cao |
| Reliability | Distributed lock, Graceful shutdown | 🟡 Trung bình |
| Observability | Distributed tracing, Structured logging | 🔴 Cao |
| Observability | Custom health indicators | 🟢 Thấp |
| Security | Rate limiting, HTTP headers | 🔴 Cao |
| Security | OAuth2, Encryption at rest, Vault | 🟡 Trung bình |
| Architecture | Hexagonal, CQRS, API versioning, ArchUnit | 🟡 Trung bình |
| Testing | Mutation, Contract, Performance, Chaos | 🟡 Trung bình |
| DevOps | Kubernetes, Zero-downtime migration | 🔴 Cao |
| DevOps | Blue/Green, JVM tuning | 🟡 Trung bình |

---

# Phần B — Chi tiết Kỹ thuật Senior

# B.1 Performance & Scalability

## 3. N+1 Query & Fetch Optimization

### Vấn đề thực tế trong TaskFlow

```java
// TaskRepository.findActiveTasksByAssignee()
List<Task> tasks = taskRepository.findActiveTasksByAssignee(userId);
// Trả về List<Task> với LAZY assignee, project, labels

// Khi map sang TaskResponse.from(task):
tasks.stream().map(t -> TaskResponse.from(t)).toList();
// Mỗi task → load assignee, project, labels riêng = N+1 query!
```

Với 100 tasks → **301+ queries** (1 cho list + 100 assignee + 100 project + 100 labels).

### Giải pháp 1: `@EntityGraph`

```java
@EntityGraph(attributePaths = {"assignee", "project", "labels"})
@Query("SELECT t FROM Task t WHERE t.assignee.id = :userId AND t.status NOT IN ('DONE','CANCELLED')")
List<Task> findActiveTasksByAssignee(@Param("userId") UUID userId);
```

Hibernate sẽ generate **1 query với LEFT JOIN** cho tất cả attribute trong `attributePaths`.

### Giải pháp 2: `@BatchSize` cho collection

```java
@Entity
public class Task {
    @ManyToMany
    @BatchSize(size = 50)   // load 50 task's labels trong 1 query
    private Set<Label> labels;
}
```

`@BatchSize` không loại bỏ N+1 hoàn toàn nhưng giảm từ N queries → N/batch_size queries.

### Giải pháp 3: DTO Projection (read-only)

```java
@Query("""
    SELECT new com.taskflow.dto.TaskListItem(
        t.id, t.title, t.status, t.priority,
        a.username, p.name
    )
    FROM Task t LEFT JOIN t.assignee a LEFT JOIN t.project p
    WHERE t.assignee.id = :userId
    """)
List<TaskListItem> findTaskListItems(@Param("userId") UUID userId);
```

Không load entity → không có lazy proxy → không thể N+1. Phù hợp cho list/grid view.

### So sánh

| Approach | Khi nào dùng | Trade-off |
|----------|--------------|-----------|
| `@EntityGraph` | List + cần entity đầy đủ (write back) | Cartesian product nếu fetch nhiều `*ToMany` |
| `@BatchSize` | Detail view, lazy loading có thể tolerate | Vẫn có nhiều round-trip |
| DTO Projection | Read-only list/report | Không reusable nếu DTO thay đổi |
| `JOIN FETCH` (JPQL) | Khi cần ràng buộc thêm WHERE | Giống @EntityGraph nhưng explicit hơn |

### Phát hiện N+1 tự động

```yaml
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        # Throw exception khi phát hiện lazy load ngoài transaction
        enable_lazy_load_no_trans: false
```

**Hypersistence Utils** thư viện hỗ trợ phát hiện N+1 trong test:

```java
@Test
void shouldNotHaveNPlusOne() {
    SQLStatementCountValidator.reset();
    taskService.getMyTasks(userId);
    SQLStatementCountValidator.assertSelectCount(1);  // chỉ 1 query
}
```

---

## 4. Optimistic Locking với @Version

### Vấn đề: Lost Update

Không có lock → 2 user cùng update → 1 update bị nuốt:

```
Time   User A                    DB (Task#1)              User B
─────────────────────────────────────────────────────────────────
T0     SELECT  ─────────►   status=TODO, v=5
T0                          status=TODO, v=5    ◄───── SELECT
T1     local: TODO→DOING                       local: TODO→DONE
T2     UPDATE ─────────►    status=DOING, v=6
T3                          status=DONE, v=7    ◄───── UPDATE  ✗
                            (A's change LOST!)
```

User A's change bị nuốt hoàn toàn → **Lost Update Anomaly**.

### Giải pháp: `@Version`

```java
@Entity
public class Task extends BaseEntity {
    @Version
    private Long version;
    // ...
}
```

Hibernate tự động:
1. Mỗi UPDATE: `WHERE id = ? AND version = ?`
2. Increment version sau mỗi save thành công
3. Throw `OptimisticLockException` nếu version mismatch

### Sequence với @Version — Conflict được detect

```
Time   User A                       DB (Task#1)                    User B
──────────────────────────────────────────────────────────────────────────
T0     SELECT ─────────►       id=1, status=TODO, v=5
T0                              id=1, status=TODO, v=5   ◄────── SELECT
T1     UPDATE WHERE id=1
         AND version=5 ─────►   ✓ matched → v=6, status=DOING
T2                              UPDATE WHERE id=1
                                  AND version=5         ◄────── 0 rows affected
T3                              Hibernate throws         ─────► OptimisticLockException
T4                              ← retry: SELECT (v=6, status=DOING) → merge → UPDATE OK
```

→ Hibernate generate SQL:

```sql
UPDATE tasks SET status='DOING', version=6
WHERE id=1 AND version=5;     -- WHERE version=? là CORE của optimistic lock

-- Nếu rows affected = 0 → ai đó đã update trước → throw exception
```

### Xử lý exception

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "Resource đã được sửa bởi user khác. Vui lòng reload và thử lại.");
        pd.setTitle("Concurrent modification detected");
        return pd;
    }
}
```

Frontend cần:
- Hiển thị thông báo conflict cho user
- Reload data và để user merge thủ công

### Optimistic vs Pessimistic Lock

| Loại | Khi nào dùng | Cost |
|------|--------------|------|
| Optimistic (`@Version`) | Conflict hiếm, đọc nhiều | Retry overhead khi conflict |
| Pessimistic (`SELECT FOR UPDATE`) | Conflict thường, write-heavy | DB row lock, có thể deadlock |

```java
// Pessimistic example
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT t FROM Task t WHERE t.id = :id")
Optional<Task> findByIdForUpdate(@Param("id") UUID id);
```

---

## 5. Database Indexing Strategy

### Audit indexes hiện tại

Project hiện đã có indexes cơ bản trong `V1__init.sql`. Bổ sung **composite indexes** cho query patterns thực tế:

```sql
-- TaskRepository.findByProjectWithFilters() filter theo project + status + priority + assignee
CREATE INDEX idx_tasks_project_status ON tasks(project_id, status)
    WHERE status NOT IN ('DONE', 'CANCELLED');   -- partial index

CREATE INDEX idx_tasks_assignee_status ON tasks(assignee_id, status)
    WHERE status NOT IN ('DONE', 'CANCELLED');

-- TaskRepository.findOverdueTasks() - filter theo due_date
CREATE INDEX idx_tasks_due_date_active ON tasks(due_date)
    WHERE status NOT IN ('DONE', 'CANCELLED');

-- Notification unread count - high frequency query
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read)
    WHERE is_read = false;
```

### Index Cardinality & Selectivity

```sql
-- Phân tích cardinality
SELECT
    schemaname, tablename, indexname,
    idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- Index không bao giờ dùng → drop
SELECT * FROM pg_stat_user_indexes WHERE idx_scan = 0;
```

### EXPLAIN ANALYZE cho slow queries

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM tasks
WHERE project_id = '...' AND status = 'IN_PROGRESS'
ORDER BY priority DESC, due_date ASC;
```

Tìm:
- `Seq Scan` trên bảng lớn → cần index
- `Sort` chiếm thời gian lớn → consider index theo ORDER BY
- `Nested Loop` × `Hash Join` lớn → consider denormalize

### Partial Index — Tiết kiệm dung lượng

Active task chỉ chiếm ~20% bảng → partial index nhỏ hơn 5x:

```sql
CREATE INDEX idx_tasks_active ON tasks(project_id, due_date)
    WHERE status NOT IN ('DONE', 'CANCELLED');
```

---

## 6. HikariCP Production Tuning

### Cấu hình hiện tại (default)

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
```

### Cấu hình production-grade

```yaml
spring:
  datasource:
    hikari:
      pool-name: TaskFlowHikariPool
      maximum-pool-size: 20            # = (core_count * 2) + effective_spindle_count
      minimum-idle: 5
      connection-timeout: 3000          # ms — fail fast
      idle-timeout: 600000              # 10 phút
      max-lifetime: 1800000             # 30 phút (< DB wait_timeout)
      leak-detection-threshold: 60000   # 1 phút — phát hiện connection leak
      validation-timeout: 5000
      keepalive-time: 120000            # ping mỗi 2 phút
      connection-test-query: SELECT 1   # cho non-JDBC4 driver
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
```

### Pool Sizing Formula

**The Brian Goetz formula:**

```
connections = ((core_count * 2) + effective_spindle_count)
```

Với 4 cores, SSD (spindle=0): `pool = 8`. Production thường set 10-20.

**Quan trọng:** Pool quá lớn KHÔNG cải thiện performance. Lý do:
- DB chỉ xử lý song song được giới hạn (CPU + IO)
- Connection idle vẫn tốn memory ở DB side (PostgreSQL ~10MB/connection)

### Leak Detection

```yaml
hikari:
  leak-detection-threshold: 60000   # log warning sau 1 phút giữ connection
```

Sẽ log stack trace của thread giữ connection quá lâu → giúp tìm bug forget close.

---

## 7. Read Replica Routing

Khi traffic lớn, tách read/write database:
- **Primary** (1 instance) — nhận tất cả writes
- **Replicas** (N instances) — nhận read-only queries

### `AbstractRoutingDataSource` Pattern

```java
public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            ? "REPLICA" : "PRIMARY";
    }
}

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource routingDataSource(
        @Qualifier("primaryDataSource") DataSource primary,
        @Qualifier("replicaDataSource") DataSource replica) {

        Map<Object, Object> targets = Map.of(
            "PRIMARY", primary,
            "REPLICA", replica);

        RoutingDataSource ds = new RoutingDataSource();
        ds.setTargetDataSources(targets);
        ds.setDefaultTargetDataSource(primary);
        return ds;
    }
}
```

### Sử dụng

```java
@Service
public class TaskService {

    @Transactional(readOnly = true)   // routes to REPLICA
    public TaskResponse getTask(UUID id) { ... }

    @Transactional                    // routes to PRIMARY
    public TaskResponse createTask(...) { ... }
}
```

### Trade-off: Replication Lag

```
T0: User write → Primary (committed)
T1: User read → Replica (chưa replicate xong) → 404!
```

Giải pháp:
1. **Read-your-writes** routing: sau khi write, route read về primary trong 5s
2. Sticky session đến primary cho user vừa write

---

# B.2 Resilience & Reliability

## 8. Resilience4j

### Vấn đề: Cascading Failure

```
Redis down → Cache miss tất cả requests → DB overload → DB cũng down → toàn hệ thống chết
```

### Pattern 1: Circuit Breaker

```java
@CircuitBreaker(name = "redisCache", fallbackMethod = "getFromDb")
public TaskResponse getTask(UUID taskId, UUID userId) {
    return taskCache.get(taskId, () -> loadFromDb(taskId));
}

public TaskResponse getFromDb(UUID taskId, UUID userId, Throwable t) {
    log.warn("Redis circuit OPEN, falling back to DB", t);
    return loadFromDb(taskId);
}
```

```yaml
resilience4j:
  circuitbreaker:
    instances:
      redisCache:
        sliding-window-size: 10
        failure-rate-threshold: 50    # mở circuit khi 50% requests fail
        wait-duration-in-open-state: 30s
        permitted-number-of-calls-in-half-open-state: 3
```

**3 trạng thái + state machine:**

```
                ┌─────────────────────────────────────────┐
                │                                         │
                │     fail rate < threshold               │
                │                                         │
        ┌───────▼────────┐                       ┌────────┴────────┐
        │     CLOSED     │  fail rate ≥ 50%      │      OPEN       │
        │ (normal calls) ├──────────────────────►│ (fail fast: no  │
        │                │                       │  downstream     │
        │ count failures │                       │  calls allowed) │
        └───────▲────────┘                       └────────┬────────┘
                │                                         │
                │  N test calls all OK                    │ wait-duration
                │                                         │ (30s)
                │                                         ▼
                │                                ┌─────────────────┐
                │   any test call fails          │   HALF_OPEN     │
                └────────────────────────────────┤ (probe: allow 3 │
                                                 │  test calls)    │
                                                 └─────────────────┘
```

- `CLOSED`: requests đi qua bình thường, đếm failure rate trong sliding window
- `OPEN`: requests fail ngay không gọi downstream → protect downstream khỏi cascading failure
- `HALF_OPEN`: sau wait-duration, cho qua `permitted-number-of-calls` để test → tất cả OK thì CLOSED, có fail thì quay lại OPEN

### Pattern 2: Retry với Exponential Backoff

```java
@Retry(name = "kafkaPublish", fallbackMethod = "saveToOutbox")
public void publishEvent(TaskAssignedEvent event) {
    kafkaTemplate.send(TOPIC, event);
}
```

```yaml
resilience4j:
  retry:
    instances:
      kafkaPublish:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2   # 1s, 2s, 4s
        retry-exceptions:
          - org.apache.kafka.common.errors.RetriableException
```

### Pattern 3: Bulkhead — Isolate Resources

```java
@Bulkhead(name = "expensiveReport", type = Bulkhead.Type.SEMAPHORE)
public Report generateReport(...) { ... }
```

```yaml
resilience4j:
  bulkhead:
    instances:
      expensiveReport:
        max-concurrent-calls: 5    # tối đa 5 report concurrent
        max-wait-duration: 0       # fail-fast nếu full
```

Không để báo cáo nặng chiếm hết thread pool, ảnh hưởng các API khác.

### Pattern 4: TimeLimiter

```java
@TimeLimiter(name = "externalApi")
@CircuitBreaker(name = "externalApi")
public CompletableFuture<Result> callExternalApi() {
    return CompletableFuture.supplyAsync(...);
}
```

Hủy request sau timeout → giải phóng thread.

---

## 9. Transactional Outbox Pattern

### Vấn đề: Dual-Write Problem

```java
@Transactional
public Task createTask(...) {
    Task task = taskRepository.save(...);  // ✓ DB committed
    kafkaTemplate.send(TOPIC, event);       // ✗ broker down → event mất!
    return task;
}
```

Nếu Kafka down sau khi DB commit → task tồn tại nhưng KHÔNG có event → notification không gửi.

### Giải pháp: Outbox Table

```sql
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP,
    INDEX (published_at) WHERE published_at IS NULL
);
```

### Flow

```
1. Service.createTask():
   BEGIN TX
     INSERT task ...
     INSERT outbox (event TaskAssigned)
   COMMIT TX

2. OutboxPoller (separate thread, mỗi 1s):
   SELECT * FROM outbox WHERE published_at IS NULL LIMIT 100
   FOR EACH event:
     kafkaTemplate.send(...)
     UPDATE outbox SET published_at = NOW()

3. Nếu Kafka down: event vẫn ở outbox → retry sau
```

### Kiến trúc tổng thể

```
        ┌──────────────────────────────────────────────────────────┐
        │                  Service.createTask()                    │
        │                                                          │
        │   ┌──────────────────────────────────────┐               │
        │   │           ONE Database Tx            │               │
        │   │  ┌─────────────┐   ┌─────────────┐   │               │
        │   │  │   tasks     │   │   outbox    │   │               │
        │   │  │ INSERT row  │   │ INSERT row  │   │               │
        │   │  └─────────────┘   └─────────────┘   │               │
        │   │         ATOMIC COMMIT                │               │
        │   └──────────────────────────────────────┘               │
        └─────────────────────┬────────────────────────────────────┘
                              │ (data persisted)
                              ▼
        ┌──────────────────────────────────────────────────────────┐
        │   OutboxPoller (or Debezium CDC reading WAL)             │
        │   - polls/streams unpublished rows every 1s              │
        │   - sends to Kafka                                       │
        │   - marks row as published                               │
        │   - retries on failure (event NEVER lost)                │
        └─────────────────────┬────────────────────────────────────┘
                              ▼
                          ┌────────┐
                          │ Kafka  │ ──► Consumers (email, slack, audit...)
                          └────────┘
```

→ Key insight: **DB transaction guarantees** rằng `task` và `outbox` được tạo cùng lúc. Không có "task created but event lost" possibility nữa.

### Implementation

```java
@Service
@RequiredArgsConstructor
public class TaskService {

    @Transactional
    public Task createTask(...) {
        Task task = taskRepository.save(...);
        outboxRepository.save(OutboxEvent.builder()
            .aggregateType("Task")
            .aggregateId(task.getId())
            .eventType("TaskAssigned")
            .payload(toJson(event))
            .build());
        return task;
    }
}

@Component
@RequiredArgsConstructor
public class OutboxPoller {

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void poll() {
        List<OutboxEvent> events = outboxRepository
            .findTop100ByPublishedAtIsNullOrderByCreatedAt();

        for (OutboxEvent e : events) {
            kafkaTemplate.send(topicFor(e), e.getAggregateId().toString(), e.getPayload())
                .whenComplete((r, ex) -> {
                    if (ex == null) {
                        outboxRepository.markPublished(e.getId());
                    }
                });
        }
    }
}
```

### Alternative: Debezium CDC

Production-grade hơn — không cần poller code:
1. Debezium đọc PostgreSQL WAL (Write-Ahead Log)
2. Tự động publish thay đổi outbox table → Kafka topic
3. Ưu điểm: không miss event, không cần polling

---

## 10. Idempotency Keys

### Vấn đề

User click "Create Task" 2 lần do mạng chậm → 2 task được tạo.

### Giải pháp

Client gửi `Idempotency-Key` header với UUID:

```http
POST /api/tasks
Idempotency-Key: 9f8b1c2d-...
{ "title": "..." }
```

Server:
1. Check Redis: key đã tồn tại? → return cached response
2. Nếu chưa: process → cache response 24h

```java
@RestController
public class TaskController {

    @PostMapping("/api/projects/{id}/tasks")
    public ResponseEntity<TaskResponse> createTask(
        @PathVariable UUID id,
        @RequestHeader("Idempotency-Key") UUID idempotencyKey,
        @Valid @RequestBody CreateTaskRequest request) {

        return idempotencyService.executeOnce(idempotencyKey, () -> {
            TaskResponse task = taskService.createTask(id, request, ...);
            return ResponseEntity.status(HttpStatus.CREATED).body(task);
        });
    }
}

@Service
public class IdempotencyService {

    private final RedisTemplate<String, IdempotencyRecord> redis;

    public <T> ResponseEntity<T> executeOnce(UUID key, Supplier<ResponseEntity<T>> action) {
        String redisKey = "idemp:" + key;

        // 1. Try lock
        Boolean acquired = redis.opsForValue()
            .setIfAbsent(redisKey, IdempotencyRecord.PROCESSING, Duration.ofMinutes(5));

        if (Boolean.FALSE.equals(acquired)) {
            // Already processed or in-progress
            IdempotencyRecord cached = redis.opsForValue().get(redisKey);
            if (cached.isComplete()) {
                return ResponseEntity.status(cached.status()).body((T) cached.body());
            }
            throw new ConcurrentRequestException("Request đang được xử lý");
        }

        // 2. Execute
        ResponseEntity<T> response = action.get();

        // 3. Cache result
        redis.opsForValue().set(redisKey,
            IdempotencyRecord.complete(response),
            Duration.ofHours(24));

        return response;
    }
}
```

---

## 11. Distributed Locking với Redisson

### Use Case

- Background job chạy trên nhiều instance → 1 instance chạy thôi
- Critical section: "1 user 1 lúc 1 task move giữa columns"

### Setup Redisson

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.27.2</version>
</dependency>
```

### Implementation

```java
@Service
@RequiredArgsConstructor
public class TaskService {

    private final RedissonClient redisson;

    public TaskResponse moveTask(UUID taskId, ColumnPosition pos, UUID userId) {
        RLock lock = redisson.getLock("lock:task:" + taskId);

        try {
            boolean acquired = lock.tryLock(5, 30, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException("Task đang được sửa bởi user khác");
            }

            return doMoveTask(taskId, pos, userId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

### Scheduled Job với Lock

```java
@Component
@RequiredArgsConstructor
public class OverdueTaskNotifier {

    private final RedissonClient redisson;

    @Scheduled(cron = "0 0 9 * * *")   // 9 AM daily
    public void notifyOverdue() {
        RLock lock = redisson.getLock("job:overdue-notifier");

        if (lock.tryLock(0, 30, TimeUnit.MINUTES)) {
            try {
                // chỉ 1 instance trong cluster chạy
                doNotify();
            } finally {
                lock.unlock();
            }
        }
    }
}
```

### Redlock vs SETNX

- **SETNX** (basic): có thể release lock của thread khác nếu thread A timeout, thread B acquire, thread A finish & release → release lock của B
- **Redlock**: Redisson implement, dùng UUID + Lua script đảm bảo only-owner-can-release

---

## 12. Graceful Shutdown

### Vấn đề

Container restart → in-flight requests bị cắt giữa chừng → user thấy lỗi.

### Giải pháp Spring Boot 3

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

Khi nhận `SIGTERM`:
1. Stop accept new requests
2. Wait in-flight requests xong (max 30s)
3. Shutdown beans theo order
4. Exit

### Kafka Listener Shutdown

```java
@KafkaListener(...)
public void onTaskAssigned(TaskAssignedEvent event) {
    // Spring Kafka handle graceful: poll xong message hiện tại, không poll thêm
}
```

### K8s Pod Termination

```yaml
spec:
  terminationGracePeriodSeconds: 60   # > app's graceful timeout
  containers:
    - name: app
      lifecycle:
        preStop:
          exec:
            command: ["sleep", "10"]   # đợi K8s remove from service
```

`preStop` cho service mesh / kube-proxy thời gian remove pod khỏi load balancer trước khi SIGTERM.

---

# B.3 Observability (Production-grade)

## 13. Distributed Tracing

### Vấn đề

```
HTTP request → TaskController → TaskService → DB
                                  ↓
                              Kafka publish → Consumer → DB → Notification
```

Khi user báo "tạo task chậm" — không biết bottleneck ở đâu (DB? Kafka? Consumer?).

### Giải pháp: Micrometer Tracing + OpenTelemetry

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

```yaml
management:
  tracing:
    sampling:
      probability: 1.0    # dev: 100%, prod: 0.1
otel:
  exporter:
    otlp:
      endpoint: http://jaeger:4317
```

### Tự động thêm Trace ID vào logs

```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name},%X{traceId:-},%X{spanId:-}]"
```

Output:
```
INFO  [taskflow,abc123def456,789xyz] Creating task...
```

### Custom Span

```java
@Service
@RequiredArgsConstructor
public class TaskService {

    private final Tracer tracer;

    public Task createTask(...) {
        Span span = tracer.nextSpan().name("calculate-priority").start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // calculation
            return ...;
        } finally {
            span.end();
        }
    }
}
```

### Trace Context Propagation qua Kafka

Spring Boot 3 + Micrometer tự động inject `traceparent` header vào Kafka message → consumer nhận được context tự động.

```
Producer span:  [HTTP /api/tasks (200ms)]
                  ├─ [DB INSERT task (15ms)]
                  └─ [Kafka send (5ms)]
                                  └─ [Consumer receive]
                                       ├─ [DB INSERT notification]
                                       └─ [Email send (300ms)]   ← bottleneck!
```

Trên Jaeger UI thấy ngay flow này.

### Cấu trúc Trace, Span, Context

```
TRACE (1 request end-to-end) — TraceID: abc123
│
├── SPAN A: HTTP POST /api/tasks      [parent=null, 200ms]
│   │
│   ├── SPAN B: TaskService.create   [parent=A, 180ms]
│   │   ├── SPAN C: DB INSERT        [parent=B, 15ms]
│   │   └── SPAN D: Kafka send       [parent=B, 5ms]   ──┐
│   │                                                    │  context propagated
│   └── SPAN E: HTTP response        [parent=A, 2ms]     │  via 'traceparent'
│                                                        │  Kafka header
└── SPAN F: NotificationConsumer    [parent=D, 320ms] ◄──┘
    ├── SPAN G: DB INSERT notif      [parent=F, 10ms]
    └── SPAN H: Email SMTP send      [parent=F, 300ms]  ← bottleneck visible
```

W3C Trace Context HTTP header format:

```
traceparent: 00-abc123def456789a-0011223344556677-01
              │   │                │                │
              │   │                │                └── flags (01 = sampled)
              │   │                └── parent span ID
              │   └── trace ID (128 bit)
              └── version
```

→ Mọi service trên trace chain inject header này → backend (Jaeger/Tempo/Datadog) reassemble thành tree view.

---

## 14. Structured Logging với MDC

### Hiện tại

```
INFO  c.t.s.TaskService - Task created
```

→ Khó parse, không liên kết với request, user.

### Giải pháp 1: MDC (Mapped Diagnostic Context)

```java
@Component
public class MdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) {
        String requestId = req.getHeader("X-Request-Id");
        if (requestId == null) requestId = UUID.randomUUID().toString();

        try {
            MDC.put("requestId", requestId);
            MDC.put("method", req.getMethod());
            MDC.put("uri", req.getRequestURI());

            String userId = extractUserId(SecurityContextHolder.getContext());
            if (userId != null) MDC.put("userId", userId);

            chain.doFilter(req, res);
        } finally {
            MDC.clear();
        }
    }
}
```

### Giải pháp 2: JSON Logging với Logback

```xml
<!-- logback-spring.xml -->
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>requestId</includeMdcKeyName>
        <includeMdcKeyName>userId</includeMdcKeyName>
        <includeMdcKeyName>traceId</includeMdcKeyName>
    </encoder>
</appender>
```

Output:
```json
{
  "@timestamp": "2026-05-15T10:23:45.123Z",
  "level": "INFO",
  "logger": "c.t.s.TaskService",
  "message": "Task created",
  "requestId": "abc-123",
  "userId": "user-456",
  "traceId": "trace-789",
  "thread_name": "http-nio-8080-exec-1"
}
```

→ Elasticsearch / Loki / Datadog parse trực tiếp, search/filter dễ dàng.

### Async Logging (giảm latency)

```xml
<appender name="ASYNC" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="JSON"/>
    <queueSize>512</queueSize>
    <discardingThreshold>0</discardingThreshold>
</appender>
```

---

## 15. Custom Health Indicators

```java
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    @Override
    public Health health() {
        try (AdminClient client = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClusterResult result = client.describeCluster();
            int nodeCount = result.nodes().get(5, TimeUnit.SECONDS).size();
            return Health.up()
                .withDetail("brokers", nodeCount)
                .withDetail("clusterId", result.clusterId().get())
                .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

K8s liveness probe theo `/actuator/health` → tự động restart pod khi Kafka down (hoặc tách riêng `/health/liveness` vs `/health/readiness`).

---

# B.4 Security (Senior-level)

## 16. API Rate Limiting

### Bucket4j + Redis

```java
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ProxyManager<String> proxyManager;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {

        String key = "rl:" + extractClientId(req);   // userId or IP

        Bucket bucket = proxyManager.builder().build(key, BucketConfiguration.builder()
            .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))   // 100 req/min
            .build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            res.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(req, res);
        } else {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setHeader("Retry-After",
                String.valueOf(probe.getNanosToWaitForRefill() / 1_000_000_000));
            res.getWriter().write("Rate limit exceeded");
        }
    }
}
```

### Tier-based limiting

```java
String tier = userService.getTier(userId);   // FREE, PRO, ENTERPRISE
int limit = switch (tier) {
    case "FREE"       -> 100;
    case "PRO"        -> 1000;
    case "ENTERPRISE" -> 10000;
    default -> 100;
};
```

---

## 17. OAuth2 Resource Server

JWT thuần đang dùng có hạn chế:
- Không có discovery endpoint
- Không có token revocation
- Tự ký key — không trade với SSO providers

### Spring OAuth2 Resource Server

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.taskflow.com
          jwk-set-uri: https://auth.taskflow.com/.well-known/jwks.json
```

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filter(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/**").authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtConverter() {
        var converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthorityPrefix("ROLE_");
        converter.setAuthoritiesClaimName("roles");

        var authConverter = new JwtAuthenticationConverter();
        authConverter.setJwtGrantedAuthoritiesConverter(converter);
        return authConverter;
    }
}
```

### Lợi ích

- Identity Provider (Keycloak, Auth0) handle: registration, password reset, MFA, social login
- App chỉ verify JWT signature qua JWKS endpoint
- Token rotation, revocation built-in

---

## 18. HTTP Security Headers

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filter(HttpSecurity http) throws Exception {
        http.headers(h -> h
            .contentTypeOptions(c -> {})       // X-Content-Type-Options: nosniff
            .xssProtection(x -> x.disable())   // deprecated, dùng CSP
            .frameOptions(f -> f.deny())       // X-Frame-Options: DENY
            .httpStrictTransportSecurity(s -> s
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true))
            .contentSecurityPolicy(c -> c
                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'"))
            .referrerPolicy(r -> r.policy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));

        return http.build();
    }
}
```

| Header | Bảo vệ chống |
|--------|--------------|
| `X-Content-Type-Options: nosniff` | MIME sniffing attack |
| `X-Frame-Options: DENY` | Clickjacking |
| `Strict-Transport-Security` | Protocol downgrade, MITM |
| `Content-Security-Policy` | XSS, data injection |
| `Referrer-Policy` | Information leak |

---

## 19. Encryption at Rest

PII (Personally Identifiable Information) như email, phone cần mã hóa trong DB.

### JPA AttributeConverter

```java
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final AesEncryptor encryptor;   // AES-256-GCM

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) return null;
        return encryptor.encrypt(plaintext);
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        if (ciphertext == null) return null;
        return encryptor.decrypt(ciphertext);
    }
}

@Entity
public class User {

    @Convert(converter = EncryptedStringConverter.class)
    private String email;

    @Convert(converter = EncryptedStringConverter.class)
    private String phoneNumber;
}
```

### Trade-offs

✗ **Không thể search exact match** trên encrypted column (vì ciphertext khác nhau cho cùng plaintext nếu có IV)
- Giải pháp: lưu thêm `email_hash` (SHA-256 deterministic) cho lookup

```sql
CREATE INDEX idx_users_email_hash ON users(email_hash);
```

---

## 20. Secrets Management

### Hiện tại (BAD)

```yaml
jwt:
  secret: ${JWT_SECRET:hardcoded-default}
```

→ secret in env var có thể leak qua `ps aux`, container metadata, error logs.

### Spring Cloud Vault

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-vault-config</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    vault:
      uri: https://vault.taskflow.com
      authentication: KUBERNETES
      kubernetes:
        role: taskflow-app
      kv:
        enabled: true
        backend: secret
        application-name: taskflow
```

App tự authenticate với Vault qua K8s service account → fetch secrets at startup → không cần ENV vars.

### Dynamic Secrets (xịn nhất)

Vault generate DB credentials với TTL ngắn (1 hour):
- App fetch credential mới mỗi giờ
- Compromised credential expire nhanh
- Audit log đầy đủ ai access lúc nào

---

# B.5 Architecture Patterns

## 21. Hexagonal Architecture

### Kiến trúc hiện tại (Layered)

```
Controller → Service → Repository (JPA)
              ↓
            Redis
              ↓
            Kafka
```

Service phụ thuộc trực tiếp infrastructure → khó test, khó thay đổi storage.

### Hexagonal (Ports & Adapters)

```
       ┌─────────────────────────────────────┐
       │                                     │
       │       Application Core              │
       │  (Use Cases, Domain Models)         │
       │                                     │
       │  ┌───────────────────────────────┐  │
       │  │       Domain Logic            │  │
       │  │   (Task, Project entities,    │  │
       │  │    business rules)            │  │
       │  └───────────────────────────────┘  │
       │                                     │
       └──┬──────────────────────────────┬───┘
          │                              │
       Inbound Ports               Outbound Ports
       (interfaces)                 (interfaces)
          │                              │
   ┌──────┴───────┐              ┌───────┴──────┐
   │   Adapters   │              │   Adapters    │
   ├──────────────┤              ├───────────────┤
   │ REST Ctrl    │              │ JPA Repo      │
   │ GraphQL      │              │ Redis Cache   │
   │ gRPC         │              │ Kafka Producer│
   │ Kafka Cons.  │              │ S3 Storage    │
   └──────────────┘              └───────────────┘
```

### Refactor TaskService

```java
// === Domain core (no Spring, no JPA) ===
package com.taskflow.domain.task;

public class Task {
    private TaskId id;
    private String title;
    private TaskStatus status;
    private Priority priority;

    // Domain methods
    public void assign(UserId assignee) {
        if (this.status == TaskStatus.DONE) {
            throw new BusinessRuleViolation("Cannot assign DONE task");
        }
        this.assignee = assignee;
    }
}

// === Inbound port ===
package com.taskflow.application.port.in;

public interface CreateTaskUseCase {
    Task createTask(CreateTaskCommand command);
}

// === Outbound port ===
package com.taskflow.application.port.out;

public interface TaskRepository {
    Task save(Task task);
    Optional<Task> findById(TaskId id);
}

public interface EventPublisher {
    void publish(DomainEvent event);
}

// === Application service (use case implementation) ===
package com.taskflow.application.service;

@UseCase
@RequiredArgsConstructor
public class CreateTaskService implements CreateTaskUseCase {
    private final TaskRepository taskRepo;
    private final EventPublisher eventPublisher;

    @Override
    public Task createTask(CreateTaskCommand cmd) {
        Task task = Task.create(cmd.title(), cmd.priority(), ...);
        Task saved = taskRepo.save(task);
        eventPublisher.publish(new TaskCreatedEvent(saved.getId()));
        return saved;
    }
}

// === Adapters ===
package com.taskflow.adapter.out.persistence;

@Component
class JpaTaskRepository implements TaskRepository {
    private final TaskJpaRepository jpa;
    private final TaskMapper mapper;

    public Task save(Task task) {
        TaskEntity entity = mapper.toEntity(task);
        return mapper.toDomain(jpa.save(entity));
    }
}

package com.taskflow.adapter.out.messaging;

@Component
class KafkaEventPublisher implements EventPublisher {
    public void publish(DomainEvent event) { ... }
}
```

### Lợi ích

- **Testability**: test domain với in-memory adapter, không cần Spring/DB
- **Flexibility**: thay JPA → MongoDB chỉ cần đổi adapter
- **Clear boundaries**: business logic không leak vào framework

### Trade-offs

- **Boilerplate** lớn — cần mappers giữa domain ↔ entity
- Overhead cho project nhỏ (TaskFlow MVP có thể không đáng)
- Dùng khi: project lớn, domain phức tạp, team nhiều người

---

## 22. CQRS Pattern

Tách model **Command** (write) khỏi **Query** (read).

### Vấn đề hiện tại

```java
// Cùng entity Task được dùng cho:
TaskResponse getTask(...)         // Read - cần JOIN nhiều bảng
Task createTask(...)              // Write - chỉ 1 entity
List<TaskListItem> getTasks(...)  // List - cần aggregation
```

→ JPA Entity bị "stretched" để cover cả 3 use case → không tối ưu.

### CQRS Implementation

```java
// === Command side - dùng JPA ===
@Service
@Transactional
public class TaskCommandService {
    public void createTask(CreateTaskCommand cmd) {
        Task task = ...; taskRepository.save(task);
    }

    public void updateStatus(UpdateStatusCommand cmd) { ... }
}

// === Query side - dùng JdbcTemplate / projection / read replica ===
@Service
@Transactional(readOnly = true)
public class TaskQueryService {

    private final NamedParameterJdbcTemplate jdbc;

    public List<TaskListView> findTasksForBoard(UUID projectId) {
        // Custom SQL tối ưu cho board view
        return jdbc.query("""
            SELECT t.id, t.title, t.status, t.priority,
                   a.username AS assignee_name, a.avatar_url,
                   COUNT(c.id) AS comment_count
            FROM tasks t
            LEFT JOIN users a ON t.assignee_id = a.id
            LEFT JOIN comments c ON c.task_id = t.id
            WHERE t.project_id = :projectId
            GROUP BY t.id, a.username, a.avatar_url
            ORDER BY t.priority DESC, t.due_date
            """,
            Map.of("projectId", projectId),
            taskListViewMapper);
    }
}
```

### Advanced CQRS

- **Materialized Views**: pre-aggregate task statistics
- **Event Sourcing + CQRS**: write = events, read = projection
- **Separate DB**: read DB = read replica với denormalized schema

---

## 23. API Versioning

### URI Versioning (đơn giản nhất)

```java
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskControllerV1 { ... }

@RestController
@RequestMapping("/api/v2/tasks")
public class TaskControllerV2 { ... }
```

### Header Versioning

```java
@GetMapping(value = "/api/tasks", headers = "API-Version=1")
public TaskResponseV1 getV1() { ... }

@GetMapping(value = "/api/tasks", headers = "API-Version=2")
public TaskResponseV2 getV2() { ... }
```

### Strategy

| Method | Pros | Cons |
|--------|------|------|
| URI (`/v1`, `/v2`) | Cache-friendly, Swagger rõ ràng | URL bloat |
| Header | Cleaner URI | Khó test với browser |
| Content-Type (`application/vnd.taskflow.v1+json`) | Pure REST | Phức tạp với client |

**Rule:** chỉ bump version khi **breaking change**. Backward-compatible thay đổi (thêm field) không cần version mới.

---

## 24. ArchUnit

Enforce architectural rules bằng test.

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

```java
@AnalyzeClasses(packages = "com.taskflow")
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_should_only_call_services =
        classes().that().resideInAPackage("..controller..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..service..", "..dto..", "java..", "org.springframework..");

    @ArchTest
    static final ArchRule entities_should_not_be_used_in_controllers =
        noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..entity..");

    @ArchTest
    static final ArchRule services_should_be_annotated =
        classes().that().resideInAPackage("..service..")
            .and().areNotInterfaces()
            .should().beAnnotatedWith(Service.class);

    @ArchTest
    static final ArchRule no_cycles =
        slices().matching("com.taskflow.(*)..")
            .should().beFreeOfCycles();
}
```

CI fail nếu ai đó bypass kiến trúc → tự động giữ codebase clean.

---

# B.6 Advanced Testing

## 25. Mutation Testing với Pitest

Đo chất lượng test thực sự — không chỉ "code coverage".

### Vấn đề với Coverage 100%

```java
public boolean isOverdue(Task task) {
    return task.getDueDate().isBefore(LocalDate.now());
}

@Test
void test() {
    assertNotNull(isOverdue(task));  // 100% coverage nhưng vô nghĩa!
}
```

### Pitest

Pitest "đột biến" code:
- Thay `<` thành `<=`
- Thay `+` thành `-`
- Đảo `true`/`false`

Nếu test KHÔNG fail sau mutation → test yếu (không phát hiện bug).

```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.16.1</version>
    <configuration>
        <targetClasses>
            <param>com.taskflow.service.*</param>
        </targetClasses>
        <mutationThreshold>70</mutationThreshold>
    </configuration>
</plugin>
```

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

Output: `Mutation Score: 73%` → 73% mutations được test catch.

---

## 26. Contract Testing

### Vấn đề

TaskFlow API có nhiều consumer (web, mobile, partner).
Khi deploy phiên bản mới → consumer nào bị broken?

### Pact

**Consumer driven contracts:**
1. Consumer (mobile app) viết test "tôi expect API trả response như này"
2. Generated Pact file (JSON contract)
3. Provider (TaskFlow API) verify Pact file → fail nếu vi phạm

```java
// Provider side test
@Provider("taskflow-api")
@PactFolder("pacts")
class TaskFlowProviderTest {

    @State("a task with id 123 exists")
    void taskExists() {
        // setup test data
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }
}
```

CI: nếu provider thay đổi response format → Pact verify fail → block deploy.

---

## 27. Performance Testing

### k6 (recommended for senior)

```javascript
// taskflow-load.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 100 },   // ramp up
        { duration: '2m',  target: 100 },   // steady
        { duration: '30s', target: 200 },   // spike
        { duration: '30s', target: 0 },     // ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        http_req_failed:   ['rate<0.01'],
    },
};

export default function () {
    const res = http.get('http://localhost:8080/api/tasks/my', {
        headers: { Authorization: `Bearer ${__ENV.TOKEN}` },
    });
    check(res, { 'status 200': (r) => r.status === 200 });
    sleep(1);
}
```

```bash
k6 run taskflow-load.js
```

Output: req/s, p50/p95/p99 latency, error rate.

### Tích hợp CI

Thêm vào GitHub Actions:
```yaml
- name: k6 load test
  uses: grafana/k6-action@v0.3.1
  with:
    filename: tests/load.js
```

---

## 28. Chaos Engineering

```xml
<dependency>
    <groupId>de.codecentric</groupId>
    <artifactId>chaos-monkey-spring-boot</artifactId>
</dependency>
```

```yaml
chaos:
  monkey:
    enabled: true
    watcher:
      service: true
    assaults:
      latencyActive: true
      latencyRangeStart: 1000
      latencyRangeEnd: 3000
      exceptionsActive: true
      level: 5    # 5% requests bị chaos
```

Trong staging: 5% requests bị inject 1-3s latency hoặc throw exception → kiểm tra resilience của system.

---

# B.7 DevOps Senior

## 29. Kubernetes Deployment

### Helm Chart Structure

```
helm/taskflow/
├── Chart.yaml
├── values.yaml
├── values-prod.yaml
└── templates/
    ├── deployment.yaml
    ├── service.yaml
    ├── ingress.yaml
    ├── configmap.yaml
    ├── hpa.yaml
    └── pdb.yaml
```

### deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .Release.Name }}
spec:
  replicas: {{ .Values.replicaCount }}
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0     # zero-downtime
  template:
    spec:
      containers:
      - name: app
        image: {{ .Values.image.repository }}:{{ .Values.image.tag }}
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: prod
        - name: JAVA_TOOL_OPTIONS
          value: "-XX:MaxRAMPercentage=75 -XX:+UseG1GC"
        envFrom:
        - secretRef:
            name: {{ .Release.Name }}-secrets
```

### HPA (Horizontal Pod Autoscaler)

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: taskflow
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: taskflow
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Pods
    pods:
      metric:
        name: http_server_requests_seconds_count   # Micrometer metric
      target:
        type: AverageValue
        averageValue: "100"
```

### PDB (Pod Disruption Budget)

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: taskflow
spec:
  minAvailable: 2   # luôn có ít nhất 2 pod available kể cả khi node maintenance
  selector:
    matchLabels:
      app: taskflow
```

---

## 30. Zero-downtime Database Migration

### Vấn đề

```sql
-- V3: ALTER TABLE tasks ADD COLUMN priority_score INT NOT NULL DEFAULT 0;
```

Trên bảng 50M rows → AccessExclusiveLock → block tất cả query → downtime!

### Expand-Contract Pattern

**Phase 1 — Expand (deploy version N):**
```sql
-- V3: ADD nullable column (instant)
ALTER TABLE tasks ADD COLUMN priority_score INT;
```
Code version N: write to BOTH old `priority` enum AND new `priority_score`. Read từ `priority`.

**Phase 2 — Backfill:**
```sql
-- V4: backfill với batches để không lock lâu
DO $$
DECLARE batch_size INT := 10000;
BEGIN
    LOOP
        UPDATE tasks SET priority_score = CASE priority
            WHEN 'CRITICAL' THEN 4
            WHEN 'HIGH' THEN 3
            WHEN 'MEDIUM' THEN 2
            WHEN 'LOW' THEN 1
        END
        WHERE priority_score IS NULL
        LIMIT batch_size;

        EXIT WHEN NOT FOUND;
        COMMIT;
        PERFORM pg_sleep(0.1);
    END LOOP;
END $$;
```

**Phase 3 — Contract (deploy version N+1):**
```sql
-- V5: read từ priority_score, drop priority
ALTER TABLE tasks ALTER COLUMN priority_score SET NOT NULL;
ALTER TABLE tasks DROP COLUMN priority;
```

Code version N+1: chỉ dùng `priority_score`.

**Tools hỗ trợ:**
- `pg_repack` — rebuild table without lock
- `gh-ost` (MySQL) — online schema migration
- Liquibase với `changelog labels` để conditional execute

---

## 31. Blue/Green & Canary

### Blue/Green

```
Blue (current production - v1.0)  ← traffic 100%
Green (new version - v1.1)        ← deploy, test internally

Switch:
Blue                                ← traffic 0%
Green                               ← traffic 100%

Rollback nếu có vấn đề:
Switch traffic back to Blue.
```

K8s implementation: 2 Deployments + 1 Service (selector switch).

### Canary

```
v1.0  ← 95% traffic
v1.1  ← 5% traffic (canary)

Monitor metrics. If healthy:
v1.0  ← 50%
v1.1  ← 50%

If still healthy:
v1.0  ← 0%
v1.1  ← 100%
```

Tools: Istio, Argo Rollouts, Flagger.

```yaml
# Argo Rollout example
apiVersion: argoproj.io/v1alpha1
kind: Rollout
spec:
  strategy:
    canary:
      steps:
      - setWeight: 5
      - pause: { duration: 10m }
      - setWeight: 25
      - pause: { duration: 10m }
      - setWeight: 50
      - pause: { duration: 10m }
      - setWeight: 100
      analysis:
        templates:
        - templateName: error-rate
        startingStep: 2
```

`error-rate` analysis tự động rollback nếu canary version có error rate > threshold.

---

## 32. JVM Tuning & GC

### JVM Memory Layout

```
┌────────────────────────────────────────────────────────────────┐
│                      JVM Process Memory                        │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                       HEAP                               │  │
│  │  ┌────────────────────┐  ┌────────────────────────────┐  │  │
│  │  │  Young Generation  │  │      Old Generation        │  │  │
│  │  │ ┌────┬─────┬─────┐ │  │  (long-lived objects,      │  │  │
│  │  │ │Eden│ S0  │ S1  │ │  │   cache entries, beans)    │  │  │
│  │  │ └────┴─────┴─────┘ │  │                            │  │  │
│  │  │ (new allocations)  │  │  Promoted from Young after │  │  │
│  │  │                    │  │  ~15 GC survivals          │  │  │
│  │  └────────────────────┘  └────────────────────────────┘  │  │
│  │     Minor GC (fast)            Major/Full GC (slow!)     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  ┌──────────────────────┐  ┌──────────────────────────────┐    │
│  │     Metaspace        │  │    Direct Memory (Off-heap)  │    │
│  │ (Class metadata,     │  │  - NIO ByteBuffers           │    │
│  │  reflection data)    │  │  - Netty pools               │    │
│  │ Off-heap, native     │  │  - Lucene/Elasticsearch      │    │
│  └──────────────────────┘  └──────────────────────────────┘    │
│                                                                │
│  ┌──────────────────────┐  ┌──────────────────────────────┐    │
│  │     Thread Stacks    │  │      Code Cache              │    │
│  │ (1MB × N threads)    │  │  (JIT compiled native code)  │    │
│  └──────────────────────┘  └──────────────────────────────┘    │
└────────────────────────────────────────────────────────────────┘
```

**Lifecycle của object:**
```
new Task()  →  Eden  →[minor GC]→  S0  →[minor GC]→  S1  →[after ~15 cycles]→  Old Gen
```

### So sánh các GC algorithms

| GC | Pause time | Throughput | Heap size | Khi nào dùng |
|----|-----------|------------|-----------|--------------|
| **Serial GC** | Cao (>500ms) | Cao single-thread | <100MB | Embedded, dev |
| **Parallel GC** | Cao (~500ms) | Cao nhất | <8GB | Batch jobs |
| **G1GC** (default Java 21) | ~200ms | Tốt | 4-32GB | General web apps |
| **ZGC** | <1ms | Vừa | 8GB-16TB | Low-latency apps |
| **Shenandoah** | <10ms | Vừa | 4-100GB | Low-latency, RedHat |

### Spring Boot 3 + Java 21 default

- GC: G1GC (general-purpose)
- Heap: 25% RAM (without container support flag)

### Production Tuning

**Latency-sensitive (low p99 quan trọng):**

```bash
java \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:G1HeapRegionSize=8M \
  -XX:+ParallelRefProcEnabled \
  -XX:+DisableExplicitGC \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/heap-dumps/ \
  -jar app.jar
```

**Throughput-heavy (batch processing):**

```bash
-XX:+UseParallelGC
```

**Ultra-low latency (Java 21+ ZGC):**

```bash
-XX:+UseZGC -XX:+ZGenerational
```

ZGC: pause < 1ms, scale tới TB heap.

### GC Logging

```bash
-Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=5,filesize=10M
```

Phân tích: GCViewer, gceasy.io.

### Monitoring Metrics

```
jvm.memory.used        — heap usage
jvm.gc.pause           — GC pause time (cần < SLO)
jvm.gc.memory.allocated — allocation rate
jvm.threads.peak       — thread peak (detect leak)
```

---

# B.8 Modern Java & Advanced Patterns

## 33. Virtual Threads (Project Loom) — Java 21

### Vấn đề: Platform Thread Cost

Mỗi platform thread = 1 OS thread = ~1MB stack memory. Tomcat default `maxThreads=200` → 200MB cho thread alone. Server với traffic cao (10K concurrent connections) **không thể** dùng "thread-per-request" model vì OOM.

```
┌─────────────────────────────────────────────────────────────┐
│   Traditional: Platform Threads (1 thread = 1 OS thread)    │
│                                                             │
│   Request → ┌──────────┐ ── OS Thread A (1MB) ── blocked    │
│             │  Tomcat  │      on JDBC call (sleeping)       │
│   Request → │  Thread  │ ── OS Thread B (1MB) ── blocked    │
│             │  Pool    │      on HTTP call                  │
│   Request → └──────────┘ ── OS Thread C (1MB) ── busy CPU   │
│                                                             │
│   10K connections × 1MB = 10GB RAM JUST FOR THREADS!        │
└─────────────────────────────────────────────────────────────┘
```

### Giải pháp: Virtual Threads

Virtual thread = lightweight Java thread (vài KB) được mount lên **carrier platform thread** chỉ khi thực thi CPU work. Khi block (I/O, sleep, synchronized) → JVM unmount, carrier thread tự do phục vụ virtual thread khác.

```
┌─────────────────────────────────────────────────────────────────┐
│  Virtual Threads (Java 21+)                                     │
│                                                                 │
│  100K Virtual ───┐                                              │
│  Threads (each   │     ┌─────── Carrier Pool ───────┐           │
│  vài KB)         ├────►│ OS Thread 1 ◄── Mounted    │           │
│                  │     │ OS Thread 2 ◄── VT-A       │           │
│  When VT blocks  │     │ OS Thread 3 ◄── VT-B       │           │
│  on I/O → unmount│     │ ... (≈ #cores)             │           │
│  carrier is free │     └────────────────────────────┘           │
│  for another VT  │                                              │
└─────────────────────────────────────────────────────────────────┘
```

### Enable trong Spring Boot 3.2+

```yaml
spring:
  threads:
    virtual:
      enabled: true     # Tomcat dùng VT cho request, @Async dùng VT
```

Hoặc programmatic:
```java
@Bean
public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutor() {
    return protocol -> protocol.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
}
```

### Pitfalls — khi KHÔNG dùng VT

1. **CPU-bound work** (image processing, compression): VT không giúp; dùng ForkJoinPool.
2. **synchronized blocks pinning**: VT bị "pin" vào carrier khi vào `synchronized` → carrier không thể serve VT khác → nullify VT advantage. **Migrate sang `ReentrantLock`.**
3. **ThreadLocal abuse**: VT có thể có 1M instance → ThreadLocal x 1M = OOM. Dùng `ScopedValue` (Java 21 preview) thay thế.
4. **Connection pool sizing**: Pool nhỏ vẫn là bottleneck. VT không giúp tăng DB capacity; cần tăng pool hoặc dùng async DB driver.

```java
// BAD - pin VT vào carrier
synchronized(lock) {
    dbCall();    // I/O sẽ pin carrier
}

// GOOD - ReentrantLock không pin
lock.lock();
try { dbCall(); } finally { lock.unlock(); }
```

### Detect Pinning

```bash
java -Djdk.tracePinnedThreads=full -jar app.jar
```

Output:
```
Thread[VirtualThread[#42],main] reason: pinned 0 ms
    java.base/java.lang.Object.wait(Object.java:...)   ← problematic
```

---

## 34. GraalVM Native Image — Cold Start & Memory

### Vấn đề

Spring Boot trên JVM:
- Cold start: 3-5s (load classes, JIT warmup)
- Memory: 300-500MB baseline

Serverless (AWS Lambda, Cloud Run): cold start mỗi invocation → user latency spike + chi phí cao.

### GraalVM Native Image

Compile Java bytecode → **native executable** tại build time (AOT - Ahead Of Time):

```
Build time:                      Runtime:
┌─────────────────┐              ┌─────────────────┐
│ .java + .jar    │              │  native binary  │
│ (Spring beans,  │  GraalVM     │  - 50ms start   │
│  dependencies,  │ ───────────► │  - 50MB memory  │
│  reflection     │   native-    │  - No JIT       │
│  metadata)      │   image      │  - No GC tuning │
└─────────────────┘              └─────────────────┘
```

### Spring Boot 3 hỗ trợ sẵn

```bash
./mvnw native:compile -Pnative
./target/taskflow
# Startup: 0.05s vs 3s JVM
```

### Trade-offs

| Aspect | JVM | Native |
|--------|-----|--------|
| Cold start | 3-5s | 50ms |
| Memory | 400MB | 80MB |
| Peak throughput | Cao (JIT optimize) | Thấp hơn 10-20% |
| Build time | 30s | 5-10 phút |
| Binary size | ~50MB | ~80MB |
| Reflection | Free | Phải khai báo metadata |
| Dynamic class load | Free | Không cho phép |

### Reflection Hints (cho thư viện ngoài Spring)

```java
@RegisterReflectionForBinding({MyDto.class, MyEvent.class})
@Configuration
public class NativeConfig { }
```

→ Production rule: **Dùng native cho serverless, Lambda, CLI tools.** Server-side với traffic cao dùng JVM vẫn tốt hơn (throughput).

---

## 35. Saga Pattern — Distributed Transactions

### Vấn đề: 2PC không scale

Khi 1 use case touch nhiều service (Order → Payment → Inventory → Shipping), traditional 2-Phase Commit:
- Locks across services suốt thời gian commit → throughput thấp
- Coordinator chết → resource locked forever
- Không phù hợp với eventual consistency của microservices

### Saga = chuỗi local transactions + compensating actions

**Hai flavors:**

#### Choreography Saga (event-driven, no central coordinator)

```
┌──────────┐                            ┌──────────┐
│ Order    │── OrderCreated event ────►│ Payment  │
│ Service  │                            │ Service  │
└──────────┘                            └────┬─────┘
     ▲                                       │
     │                                       │ PaymentCompleted
     │ ShippingFailed                        ▼
     │ → cancel order               ┌──────────────┐
     │ + refund                     │  Inventory   │
     │                              │  Service     │
     │                              └──────┬───────┘
     │                                     │ InventoryReserved
     │                                     ▼
     │                              ┌──────────────┐
     └──────────────────────────────│  Shipping    │
                                    │  Service     │
                                    └──────────────┘
```

**Mỗi service:**
- React tới event upstream
- Execute local TX
- Publish event downstream
- On failure → publish compensating event ngược chiều

#### Orchestration Saga (central coordinator)

```
                  ┌────────────────────────┐
                  │   Saga Orchestrator    │
                  │   (state machine)      │
                  └──┬──────┬───────┬──────┘
                     │      │       │
              cmd:   │  cmd:│   cmd:│
              charge │ reserve│  ship│
                     ▼      ▼       ▼
                  ┌─────┐ ┌─────┐ ┌──────┐
                  │ Pay │ │ Inv │ │ Ship │
                  └──┬──┘ └──┬──┘ └──┬───┘
                     │       │       │
              reply: │  reply:│  reply:
              charged│reserved│ shipped
                     ▼       ▼       ▼
                  ┌─────────────────────┐
                  │   Orchestrator      │
                  │   (next step or     │
                  │    compensate)      │
                  └─────────────────────┘
```

### Compensating Transaction

Mỗi step có "undo" tương ứng (KHÔNG phải DB rollback, là **business action ngược lại**):

| Forward action       | Compensation                  |
|----------------------|-------------------------------|
| `chargePayment()`    | `refundPayment()`             |
| `reserveInventory()` | `releaseInventory()`          |
| `bookShipping()`     | `cancelShipping()`            |
| `sendEmail()`        | `sendCancellationEmail()` (không "unsend" được) |

### So sánh

| Aspect | Choreography | Orchestration |
|--------|--------------|---------------|
| Coupling | Lỏng (event-based) | Tight với orchestrator |
| Visibility | Khó debug (logic phân tán) | Dễ trace flow |
| Scaling | Service độc lập | Orchestrator có thể bottleneck |
| Use when | 2-4 services đơn giản | Workflow phức tạp, conditional |

Tools: Camunda, Temporal, AWS Step Functions, Axon Framework.

---

## 36. Caching Strategies — 4 Patterns

### Pattern 1: Cache-Aside (Lazy Loading) — phổ biến nhất

```
   Read flow:                   Write flow:
   ┌──────┐                     ┌──────┐
   │ App  │                     │ App  │
   └──┬───┘                     └──┬───┘
      │ 1. GET key                 │ 1. UPDATE row
      ▼                            ▼
   ┌──────┐                     ┌──────┐
   │Cache │ miss?               │  DB  │
   └──┬───┘                     └──────┘
      │ 2. SELECT                  │
      ▼                            │ 2. DELETE/INVALIDATE key
   ┌──────┐                        ▼
   │  DB  │ → return value      ┌──────┐
   └──────┘                     │Cache │
      ▲                         └──────┘
      │ 3. SET key into cache
```

```java
public Task getTask(UUID id) {
    Task cached = cache.get("task:" + id);
    if (cached != null) return cached;
    Task task = db.findById(id);
    cache.put("task:" + id, task);
    return task;
}
```

→ App điều khiển cache. DB là source of truth. Cache không "consistent" với DB ngay tức thì.

### Pattern 2: Read-Through (cache library handles miss)

```
   App ──► Cache library ──► DB (on miss)
                   ▲
                   └── library tự fill cache
```

Cache library (Caffeine LoadingCache, Redisson MapCache) tự load DB khi miss → app không cần biết:

```java
LoadingCache<UUID, Task> cache = Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .build(id -> taskRepository.findById(id).orElseThrow());

Task t = cache.get(id);   // tự load nếu miss
```

### Pattern 3: Write-Through (đồng bộ write cache + DB)

```
   App ──► Cache ──► DB
              │
              └── đồng thời update cache (synchronous)
```

Write luôn đi qua cache → cache luôn fresh. Trade-off: write latency tăng (đi qua 2 hop).

### Pattern 4: Write-Behind (Write-Back)

```
   App ──► Cache ──► (return immediately)
              │
              └─── async flush DB in batch
```

Write rất nhanh (chỉ ghi cache) → cache batch flush DB. Risk: cache chết → mất data chưa flush.

### Comparison

| Pattern | Consistency | Write latency | Read latency | Risk |
|---------|-------------|---------------|--------------|------|
| Cache-Aside | Eventual (stale ok) | Normal | Fast on hit | App phải invalidate đúng |
| Read-Through | Same as aside | Normal | Fast on hit | Cache là SPOF |
| Write-Through | Strong | Slow (2 hop) | Fast | Cache là SPOF |
| Write-Behind | Eventual (lost-write risk) | Fast | Fast | Mất data nếu cache crash |

**Production rule:** Default dùng **Cache-Aside với TTL ngắn**. Chỉ chuyển sang Write-Through khi cần strong consistency, Write-Behind khi cần extreme write throughput (analytics).

---

## 37. Dead Letter Queue & Retry Topics (Kafka)

### Vấn đề: Poison Message

Một message bị malformed → consumer throw exception → commit fail → consumer **re-poll cùng message** → infinite loop, lag tăng vô tận.

### Solution: Retry topic + DLQ

```
                    ┌────────────────────────────┐
                    │  Original Topic: orders    │
                    └─────────────┬──────────────┘
                                  │ consume
                                  ▼
                          ┌───────────────┐
                          │   Consumer    │
                          └───┬───────────┘
                              │
                  success ────┘    ────── exception
                                          │
                                          ▼
                              ┌───────────────────────┐
                              │ Topic: orders.retry.5s│ (delay 5s, re-consume)
                              └──────────┬────────────┘
                                         │ still fail
                                         ▼
                              ┌──────────────────────┐
                              │Topic: orders.retry.1m│ (delay 1 min)
                              └──────────┬───────────┘
                                         │ still fail
                                         ▼
                              ┌──────────────────────┐
                              │  Topic: orders.DLQ   │ (manual review)
                              └──────────────────────┘
```

### Spring Kafka — `@RetryableTopic`

```java
@RetryableTopic(
    attempts = "4",
    backoff = @Backoff(delay = 5000, multiplier = 2.0),
    autoCreateTopics = "true",
    topicSuffixingStrategy = SUFFIX_WITH_INDEX_VALUE,
    dltStrategy = FAIL_ON_ERROR
)
@KafkaListener(topics = "orders", groupId = "order-svc")
public void onOrder(Order order) {
    // throw → goes to orders-retry-0, then -1, then -2, finally orders-dlt
}

@DltHandler
public void onDlt(Order order, @Header(KafkaHeaders.ORIGINAL_TOPIC) String topic) {
    log.error("Order {} bị move to DLT: {}", order.id(), topic);
    alerting.notify("Order DLQ", order);
}
```

→ Auto-create 4 topics: `orders`, `orders-retry-0`, `orders-retry-1`, `orders-dlt`.

### DLQ Triage Process (production)

1. **Alert** on DLQ depth > 0 (Prometheus + alert)
2. **Inspect** message header `kafka_dlt-exception-stacktrace`
3. **Fix** code/data → **replay** from DLQ topic về original topic
4. Tools: `kafka-console-consumer`, custom replay job, AKHQ UI

---

## 38. Database Sharding & Partitioning

### Vertical vs Horizontal Scaling

```
Vertical (Scale Up):                Horizontal (Scale Out):
┌────────────┐                      ┌─────┐ ┌─────┐ ┌─────┐
│            │                      │Shard│ │Shard│ │Shard│
│   1 BIG    │                      │  1  │ │  2  │ │  3  │
│ DB Server  │                      └─────┘ └─────┘ └─────┘
│ 64 CPU/    │                      Each: small data subset
│ 512GB RAM  │                      Routing layer needed
└────────────┘
Limit: máy lớn nhất                 Limit: thực tế không có
Cost: $$$$$                         Cost: linear với data
```

### Partitioning Strategies

#### 1. Range Partitioning

```
shard_1: user_id 0          - 1,000,000
shard_2: user_id 1,000,001  - 2,000,000
shard_3: user_id 2,000,001  - 3,000,000
```

Pros: query range hiệu quả. Cons: hot spot nếu data skew (latest users hot).

#### 2. Hash Partitioning

```
shard = hash(user_id) % num_shards
```

Pros: even distribution. Cons: range query phải fan-out N shards.

#### 3. Directory-based (Lookup Table)

```
┌────────────┐  ┌──────────────┐
│ user_id    │  │  Shard map   │
│ → shard_2  │  │  user_1→s1   │
│            │  │  user_2→s3   │
└────────────┘  └──────────────┘
```

Pros: flexible re-balance. Cons: lookup overhead.

### Postgres Partitioning (native)

```sql
CREATE TABLE tasks (
    id UUID, project_id UUID, created_at TIMESTAMP, ...
) PARTITION BY RANGE (created_at);

CREATE TABLE tasks_2026_01 PARTITION OF tasks
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

CREATE TABLE tasks_2026_02 PARTITION OF tasks
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
```

→ Query `WHERE created_at > '2026-02-01'` chỉ scan partition liên quan (partition pruning).

### Sharding Pitfalls

- **Cross-shard JOIN**: tránh hoặc denormalize
- **Re-sharding**: tốn kém — bắt đầu với 64+ "logical shards" trên 4 physical
- **Distributed transaction**: dùng Saga, không 2PC
- **Aggregation**: cần fan-out + merge layer

---

## 39. Multi-tenancy Strategies

### 3 patterns

```
┌──────────────────────────────────────────────────────────────────┐
│ 1. DATABASE PER TENANT (highest isolation)                       │
│   Tenant A  ──► DB-A (full instance)                             │
│   Tenant B  ──► DB-B                                             │
│   ✓ Strong isolation, GDPR safe, custom per-tenant schema        │
│   ✗ Cost cao (N DB instance), migration phải chạy N lần          │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ 2. SCHEMA PER TENANT (medium isolation)                          │
│   Tenant A  ──► postgres.schema_tenant_a                         │
│   Tenant B  ──► postgres.schema_tenant_b                         │
│   ✓ 1 DB instance, isolation tốt                                 │
│   ✗ Migration vẫn N lần, có limit số schema (1K+)                │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│ 3. SHARED SCHEMA + tenant_id DISCRIMINATOR (cheapest)            │
│   tasks(id, tenant_id, title, ...)                               │
│   WHERE tenant_id = ?  ← mọi query                               │
│   ✓ Rẻ nhất, scale tốt, 1 migration                              │
│   ✗ Rủi ro leak nếu quên WHERE tenant_id                         │
└──────────────────────────────────────────────────────────────────┘
```

### Implement Discriminator Pattern với Hibernate

```java
@Entity
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Task {
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
    // ...
}

@Component
public class TenantFilterAspect {
    @PersistenceContext private EntityManager em;

    @Before("execution(* com.taskflow.service.*.*(..))")
    public void enableTenantFilter() {
        UUID tenant = TenantContext.getCurrentTenant();
        em.unwrap(Session.class)
          .enableFilter("tenantFilter")
          .setParameter("tenantId", tenant);
    }
}
```

### Postgres Row-Level Security (defense in depth)

```sql
ALTER TABLE tasks ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON tasks
    USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- Set per session
SET app.current_tenant = 'tenant-a-uuid';
```

→ Even nếu app quên `WHERE tenant_id`, DB tự filter. **Production rule: dùng cả 2 lớp.**

---

## 40. API Gateway & BFF Pattern

### Vấn đề: Frontend gọi N microservices

```
Without Gateway:
   Web   ─────► Task Service    (auth tự handle)
    │    ─────► User Service    (auth tự handle)
    │    ─────► Notif Service   (auth tự handle)
    └    ─────► Project Service (auth tự handle)
   Mobile ──► same... và phải merge response → slow on mobile network
```

### With API Gateway

```
   ┌────────┐         ┌─────────────────────────┐
   │  Web   │         │     API Gateway         │     ┌──────────┐
   ├────────┤────────►│  - Auth (1 lần)         │────►│  Task    │
   │ Mobile │         │  - Rate limit           │     ├──────────┤
   ├────────┤────────►│  - Routing              │────►│  User    │
   │Partner │         │  - Aggregation (BFF)    │     ├──────────┤
   └────────┘         │  - Response shaping     │────►│  Notif   │
                      │  - Circuit breaker      │     ├──────────┤
                      └─────────────────────────┘────►│ Project  │
                                                      └──────────┘
   Tools: Spring Cloud Gateway, Kong, AWS API Gateway
```

**Cross-cutting concerns** moved ra gateway → mỗi microservice chỉ care business logic.

### BFF (Backend for Frontend)

Mỗi client type có 1 gateway riêng tối ưu cho nó:

```
   Web (desktop)  ──► BFF-Web     ──► microservices
                     (rich data,
                      JSON tree)

   Mobile (3G)    ──► BFF-Mobile  ──► microservices
                     (compact response,
                      pre-aggregated)

   Partner        ──► BFF-Partner ──► microservices
                     (rate-limited,
                      versioned API)
```

→ Tránh "1 size fits all" API. Mobile không cần `task.assignee.profile.preferences.*` (waste 3G bandwidth).

### Spring Cloud Gateway example

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: task-service
          uri: lb://task-service
          predicates:
            - Path=/api/tasks/**
          filters:
            - name: CircuitBreaker
              args:
                name: taskBreaker
                fallbackUri: forward:/fallback/tasks
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 100
```

---

## 41. Domain Events vs Integration Events

Sai lầm thường gặp: phân biệt mơ hồ → schema mess, coupling cao.

```
┌──────────────────────────────────────────────────────────────────┐
│  DOMAIN EVENT (in-process, within bounded context)               │
│                                                                  │
│   TaskService.create()                                           │
│       │                                                          │
│       └── publish TaskCreated (Java object)                      │
│              │                                                   │
│              └── @EventListener inside SAME service              │
│                     - update statistics                          │
│                     - invalidate cache                           │
│                     - trigger validation                         │
│                                                                  │
│   ✓ Rich domain model, refactor freely                           │
│   ✓ Same TX boundary                                             │
│   ✓ Spring ApplicationEvents                                     │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│  INTEGRATION EVENT (cross-service, public contract)              │
│                                                                  │
│   TaskService ──► Kafka topic: task.events                       │
│                       │                                          │
│                       ▼                                          │
│                   ┌─────────────┐ ┌─────────────┐                │
│                   │ Notif Svc   │ │ Audit Svc   │                │
│                   │ (other team)│ │ (other team)│                │
│                   └─────────────┘ └─────────────┘                │
│                                                                  │
│   ✓ Stable schema (Avro/Protobuf, versioned)                     │
│   ✓ Eventually consistent                                        │
│   ✓ Outbox pattern bắt buộc (xem section 9)                      │
└──────────────────────────────────────────────────────────────────┘
```

### Naming convention

- **Domain**: `TaskCreatedEvent` (verb-past-tense), rich object, full entity reference
- **Integration**: `TaskCreatedV1` (versioned), flat DTO, IDs only (no entity reference)

### Translation Layer

```java
@Component
class TaskEventTranslator {

    @EventListener   // catch domain event
    void on(TaskCreatedEvent domainEvent) {
        // translate → publish integration event qua outbox
        outboxRepo.save(OutboxEvent.builder()
            .topic("task.events")
            .eventType("TaskCreatedV1")
            .payload(new TaskCreatedV1(
                domainEvent.task().id(),
                domainEvent.task().title(),
                domainEvent.task().createdAt()))
            .build());
    }
}
```

→ Domain core không biết Kafka. Translator là **anti-corruption layer**.

---

## 42. Reactive Programming — WebFlux & R2DBC

### Khi nào dùng?

Use case "thousands of slow upstream calls" (gateway, fan-out aggregation, SSE/WebSocket fanout). KHÔNG dùng cho CRUD app thông thường (overhead, code phức tạp).

### Thread model comparison

```
┌─────────────────────────────────────────────────────────────────┐
│ MVC (blocking): 1 request = 1 thread suốt life                  │
│                                                                 │
│  ┌──────┐  blocked   ┌──────┐  blocked  ┌──────┐                │
│  │  T1  │───────────►│ DB   │──────────►│ HTTP │                │
│  │      │  10ms wait │      │  50ms wait│      │                │
│  └──────┘            └──────┘           └──────┘                │
│  Thread sleep 60ms → cant serve other req                       │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ WebFlux (reactive): event loop, few threads serve many requests │
│                                                                 │
│  Event Loop (≈ #cores threads):                                 │
│    req1 → schedule DB call → callback registered → next req     │
│    req2 → schedule HTTP call → callback registered → next req   │
│    DB callback fires → resume req1 chain                        │
│                                                                 │
│  4 threads phục vụ 10K concurrent requests                      │
└─────────────────────────────────────────────────────────────────┘
```

### Code style

```java
// MVC
@GetMapping("/{id}")
public Task get(@PathVariable UUID id) {
    return taskRepo.findById(id);   // blocking
}

// WebFlux
@GetMapping("/{id}")
public Mono<Task> get(@PathVariable UUID id) {
    return taskRepo.findById(id);   // returns Mono — không block
}
```

Composition:

```java
public Mono<TaskDashboard> getDashboard(UUID userId) {
    return Mono.zip(
        taskRepo.findActiveByUser(userId),       // Flux<Task>
        notifRepo.unreadCount(userId),           // Mono<Long>
        projectRepo.findByUser(userId)           // Flux<Project>
    ).map(tuple -> new TaskDashboard(
        tuple.getT1(), tuple.getT2(), tuple.getT3()));
}
```

→ 3 calls **parallel**, non-blocking. Trong MVC bạn cần `CompletableFuture` boilerplate.

### Backpressure

```
Producer (fast)   ─────► Subscriber (slow)
                          │
                          └── signal: "I want only 10 items at a time"
                                       (request(10))
```

Producer chỉ emit n items khi subscriber yêu cầu → tránh OOM.

### Trade-offs

| Aspect | MVC + Virtual Threads (Java 21) | WebFlux |
|--------|--------------------------------|---------|
| Code style | Imperative (dễ đọc) | Functional/callback (khó debug) |
| Throughput | Cao (VT giải quyết blocking) | Cao |
| Backpressure | Không có native | Built-in |
| Stack trace | Đầy đủ | Bị fragment |
| Library support | Tất cả Java libraries | Cần reactive driver (R2DBC, Reactor Netty) |

→ **Modern rule (Java 21+):** Default dùng MVC + Virtual Threads. Chỉ dùng WebFlux khi cần backpressure thật sự (streaming, fan-out).

---

# Phần C — Roadmap Triển khai

## 43. Roadmap 6 Phase (12 tuần)

### Phase 1 — Performance Hardening (Tuần 1-2)
**Mục tiêu:** Loại bỏ N+1, đảm bảo concurrency safety.

- [ ] **Task 1.1**: Thêm `@Version` cho `Task`, `Project`, `Comment` + Flyway `V3__add_version.sql`
- [ ] **Task 1.2**: Refactor TaskRepository dùng `@EntityGraph` cho list queries
- [ ] **Task 1.3**: Tạo DTO projection `TaskListItem` cho board view
- [ ] **Task 1.4**: Thêm composite indexes (Flyway `V4__add_composite_indexes.sql`)
- [ ] **Task 1.5**: HikariCP production tuning trong `application-prod.yml`
- [ ] **Task 1.6**: SQL count assertion test với hypersistence-utils
- [ ] **Task 1.7**: Update GlobalExceptionHandler handle `OptimisticLockingFailureException` → 409

**Deliverable:** PR `feat(performance): add @Version, EntityGraph, composite indexes`

### Phase 2 — Resilience (Tuần 3-4)
**Mục tiêu:** Hệ thống chịu được Redis/Kafka down, không mất data.

- [ ] **Task 2.1**: Thêm Resilience4j dependency
- [ ] **Task 2.2**: `@CircuitBreaker` cho Redis cache reads (fallback to DB)
- [ ] **Task 2.3**: `@Retry` cho Kafka produce
- [ ] **Task 2.4**: Implement Outbox pattern: `outbox` table + `OutboxPoller`
- [ ] **Task 2.5**: Refactor `NotificationProducer` → write to outbox thay vì direct send
- [ ] **Task 2.6**: Idempotency middleware + `IdempotencyService` với Redis
- [ ] **Task 2.7**: Apply idempotency cho `POST /tasks`, `POST /projects`
- [ ] **Task 2.8**: Graceful shutdown config + Kafka listener handling
- [ ] **Task 2.9**: Redisson + distributed lock cho scheduled jobs

**Deliverable:** PR `feat(resilience): outbox pattern, idempotency, circuit breaker`

### Phase 3 — Observability (Tuần 5-6)
**Mục tiêu:** Trace mỗi request end-to-end, log có ngữ nghĩa.

- [ ] **Task 3.1**: Thêm Micrometer Tracing + OpenTelemetry exporter
- [ ] **Task 3.2**: Setup Jaeger (docker-compose dev profile)
- [ ] **Task 3.3**: Custom span trong critical service methods
- [ ] **Task 3.4**: MDC filter cho `requestId`, `userId`
- [ ] **Task 3.5**: JSON logging với LogstashEncoder
- [ ] **Task 3.6**: Async appender
- [ ] **Task 3.7**: Custom HealthIndicator cho Kafka
- [ ] **Task 3.8**: `/actuator/health/liveness` vs `/actuator/health/readiness`

**Deliverable:** PR `feat(observability): distributed tracing + structured logging`

### Phase 4 — Security Hardening (Tuần 7-8)
**Mục tiêu:** API safe khỏi abuse, secrets tách khỏi code.

- [ ] **Task 4.1**: Bucket4j + Redis-based rate limiting filter
- [ ] **Task 4.2**: HTTP security headers (CSP, HSTS, X-Frame-Options)
- [ ] **Task 4.3**: Encryption at rest cho PII (email, phone) — `EncryptedStringConverter`
- [ ] **Task 4.4**: Email hash column cho searchability
- [ ] **Task 4.5**: Spring Cloud Vault integration
- [ ] **Task 4.6**: Migrate JWT → OAuth2 Resource Server (optional, breaking change)

**Deliverable:** PR `feat(security): rate limiting, encryption at rest, vault`

### Phase 5 — Architecture Refactoring (Tuần 9-10)
**Mục tiêu:** Clean architecture, enforce rules, prepare for scale.

- [ ] **Task 5.1**: ArchUnit tests cho package dependencies
- [ ] **Task 5.2**: Refactor 1 module sang Hexagonal (POC: Task module)
  - Domain layer: `Task`, `TaskId`, `TaskStatus` value objects
  - Inbound ports: `CreateTaskUseCase`, `UpdateTaskUseCase`
  - Outbound ports: `TaskRepository`, `EventPublisher`
  - Adapters: `JpaTaskRepository`, `KafkaEventPublisher`
- [ ] **Task 5.3**: CQRS POC cho task list view (JdbcTemplate query side)
- [ ] **Task 5.4**: API versioning `/api/v1/...` (URL prefix)

**Deliverable:** PR `refactor(arch): hexagonal architecture POC + ArchUnit + API versioning`

### Phase 6 — Advanced Testing & DevOps (Tuần 11-12)
**Mục tiêu:** Quality + production deployment.

- [ ] **Task 6.1**: Pitest mutation testing (target 70%)
- [ ] **Task 6.2**: k6 load test scripts + GitHub Actions integration
- [ ] **Task 6.3**: Pact contract test (provider side)
- [ ] **Task 6.4**: Helm chart cho TaskFlow
- [ ] **Task 6.5**: K8s manifests: Deployment, Service, Ingress, HPA, PDB
- [ ] **Task 6.6**: ConfigMap + Secret management
- [ ] **Task 6.7**: Argo Rollouts canary deployment config
- [ ] **Task 6.8**: Zero-downtime DB migration runbook (expand-contract example)
- [ ] **Task 6.9**: JVM tuning + GC log + heap dump on OOM
- [ ] **Task 6.10**: Chaos Monkey trong staging environment

**Deliverable:** PR `feat(devops): K8s helm chart, mutation/contract tests, JVM tuning`

---

## 44. Effort Estimation Matrix

| Phase | Effort (man-days) | Risk | Business Value |
|-------|------------------|------|----------------|
| Phase 1: Performance | 8 | Low | 🔴🔴🔴 High |
| Phase 2: Resilience | 12 | Medium | 🔴🔴🔴 High |
| Phase 3: Observability | 6 | Low | 🔴🔴🔴 High |
| Phase 4: Security | 10 | Medium | 🔴🔴 Medium |
| Phase 5: Architecture | 15 | High (refactoring) | 🔴🔴 Medium |
| Phase 6: DevOps | 10 | Medium | 🔴🔴🔴 High |
| **Total** | **~61 days** | | |

**Risk Notes:**
- Phase 5 cao vì refactoring lớn, dễ break existing functionality → cần test coverage tốt trước
- Phase 2 OAuth2 migration là **breaking change** → cần coordinate với clients

---

## 45. Quick-win Priorities

Nếu thời gian hạn chế, làm 5 task này trước (~5 ngày, ROI cao nhất):

1. **`@Version` optimistic locking** (0.5 ngày) — fix concurrency bug ngay
2. **Composite indexes** (0.5 ngày) — improve query performance 10x
3. **EntityGraph cho TaskRepository** (1 ngày) — fix N+1
4. **Distributed tracing + MDC** (1.5 ngày) — debug production dễ hơn x10
5. **Outbox pattern** (1.5 ngày) — không bao giờ mất event nữa

Sau quick-win này, dự án đã có "production grade ROI cao" mà không cần refactor lớn.

---

## Tài liệu tham khảo

### Core Spring & Java
- [Spring Boot Production-ready Features](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready)
- [Resilience4j Docs](https://resilience4j.readme.io/)
- [High-Performance Java Persistence — Vlad Mihalcea](https://vladmihalcea.com/books/high-performance-java-persistence/)
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Spring Boot Native — GraalVM](https://docs.spring.io/spring-boot/reference/packaging/native-image/index.html)

### Architecture
- [Microservices Patterns — Chris Richardson](https://microservices.io/patterns/)
- [Hexagonal Architecture — Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- [Domain-Driven Design — Eric Evans](https://www.dddcommunity.org/book/evans_2003/)
- [Implementing Domain-Driven Design — Vaughn Vernon](https://www.informit.com/store/implementing-domain-driven-design-9780321834577)

### Reliability & Observability
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Release It! — Michael T. Nygard](https://pragprog.com/titles/mnee2/release-it-second-edition/)
- [Site Reliability Engineering (Google)](https://sre.google/books/)

### Cloud Native & DevOps
- [The Twelve-Factor App](https://12factor.net/)
- [Cloud Native Patterns — Cornelia Davis](https://www.manning.com/books/cloud-native-patterns)
- [Kubernetes Patterns — Bilgin Ibryam](https://www.oreilly.com/library/view/kubernetes-patterns-2nd/9781098131678/)

### Data
- [Designing Data-Intensive Applications — Martin Kleppmann](https://dataintensive.net/)
- [Database Internals — Alex Petrov](https://www.databass.dev/)
