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

### Phần C — Roadmap Triển khai

33. [Roadmap 6 Phase (12 tuần)](#33-roadmap-6-phase-12-tuần)
34. [Effort Estimation Matrix](#34-effort-estimation-matrix)
35. [Quick-win priorities (làm trước)](#35-quick-win-priorities)

### Phần D — Java Core & JVM Mastery (mở rộng)

36. [JVM Memory Model & Heap Structure](#36-jvm-memory-model--heap-structure)
37. [Garbage Collection Deep Dive (G1, ZGC, Shenandoah)](#37-garbage-collection-deep-dive)
38. [Java Memory Model — Happens-before, volatile, synchronized](#38-java-memory-model--happens-before)
39. [Class Loading & ClassLoader Hierarchy](#39-class-loading--classloader-hierarchy)

### Phần E — Concurrency Mastery (mở rộng)

40. [Virtual Threads (Project Loom)](#40-virtual-threads-project-loom)
41. [CompletableFuture & Async Pipelines](#41-completablefuture--async-pipelines)
42. [Lock-free Programming (CAS, Atomic*, LongAdder)](#42-lock-free-programming)
43. [ThreadPool Sizing & Tuning](#43-threadpool-sizing--tuning)

### Phần F — Distributed Systems Patterns (mở rộng)

44. [CAP, PACELC & Consistency Models](#44-cap-pacelc--consistency-models)
45. [Saga Pattern (Orchestration vs Choreography)](#45-saga-pattern)
46. [Event Sourcing](#46-event-sourcing)
47. [Sharding & Partitioning Strategies](#47-sharding--partitioning)

### Phần G — Spring Boot Internals (mở rộng)

48. [Auto-Configuration Magic](#48-auto-configuration-magic)
49. [Bean Lifecycle & Scopes](#49-bean-lifecycle--scopes)
50. [Spring AOP Proxy Mechanism (JDK vs CGLIB)](#50-spring-aop-proxy-mechanism)
51. [@Transactional Pitfalls (self-invocation, propagation)](#51-transactional-pitfalls)

### Phần H — Database Deep Dive (mở rộng)

52. [PostgreSQL MVCC & Transaction Isolation](#52-postgresql-mvcc--transaction-isolation)
53. [B-Tree vs Hash vs GIN Index Internals](#53-b-tree-vs-hash-vs-gin-index-internals)
54. [Query Planner & EXPLAIN Mastery](#54-query-planner--explain-mastery)
55. [Caching Patterns (Cache-aside, Write-through, Write-behind)](#55-caching-patterns)

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

```
T0: User A đọc Task#1 (status=TODO, version=5)
T0: User B đọc Task#1 (status=TODO, version=5)
T1: User A đổi status=IN_PROGRESS, save → version=6 ✓
T2: User B đổi status=DONE, save → version=6 (overwrite A's change!) ✗
```

User A's change bị mất hoàn toàn → **Lost Update Anomaly**.

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

**3 trạng thái:**
- `CLOSED`: requests đi qua bình thường
- `OPEN`: requests fail ngay (không call downstream) — protect downstream
- `HALF_OPEN`: thử vài request để test recovery

#### Sơ đồ chuyển trạng thái

```
                  failure-rate >= 50%
                  trong sliding window
        ┌─────────────────────────────────┐
        │                                 ▼
   ┌─────────┐                      ┌─────────┐
   │ CLOSED  │                      │  OPEN   │
   │  (pass) │                      │ (block) │
   └─────────┘                      └─────────┘
        ▲                                 │
        │                                 │ sau wait-duration
        │ thành công                      │ (30s)
        │ N calls                         ▼
        │                          ┌────────────┐
        └──────────────────────────│ HALF_OPEN  │
                                   │ (probe N)  │
                                   └────────────┘
                                         │
                                         │ thất bại bất kỳ
                                         ▼
                                     OPEN lại
```

#### Flow một request đi qua Circuit Breaker

```
Client ──▶ [CB Decorator] ──┬──▶ Redis (downstream)
                            │
            CLOSED?         │
              │             │
              ├─ Yes ───────┘ call thật
              │              ├─ success → reset metrics
              │              └─ fail    → ghi nhận, đếm
              │
              └─ OPEN ──────▶ Fallback (DB) — KHÔNG call Redis
                              (fail-fast, không tốn thread)
```

**Tại sao quan trọng:** thread pool của Spring (tomcat: 200) sẽ cạn nhanh nếu mỗi request đợi timeout 30s khi Redis treo. CB cắt sớm → trả lỗi nhanh → thread sẵn sàng phục vụ request lành mạnh khác → tránh **thread starvation cascade**.

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

#### Sơ đồ kiến trúc Outbox

```
┌─────────────────────────────────────────────────────────────┐
│  Application (Spring Boot)                                  │
│                                                              │
│  ┌──────────────┐      single DB transaction                │
│  │ TaskService  │      ┌──────────────────────────────┐     │
│  │  .create()   │─────▶│ INSERT tasks ...             │     │
│  └──────────────┘      │ INSERT outbox (payload) ...  │     │
│                        │ COMMIT                       │     │
│                        └──────────────────────────────┘     │
│                                  │                          │
│                                  ▼                          │
│                       ┌──────────────────────┐              │
│                       │ PostgreSQL           │              │
│                       │  ├─ tasks            │              │
│                       │  └─ outbox           │              │
│                       └──────────────────────┘              │
│                                  ▲                          │
│                                  │ poll mỗi 1s              │
│                       ┌──────────┴──────────┐               │
│                       │ OutboxPoller        │               │
│                       │ (Scheduled thread)  │               │
│                       └──────────┬──────────┘               │
│                                  │ kafka.send()             │
└──────────────────────────────────┼──────────────────────────┘
                                   ▼
                            ┌──────────────┐
                            │   Kafka      │
                            │   broker     │
                            └──────────────┘
                                   │
                                   ▼
                          [Consumer services]
```

#### Tại sao "atomic" — visual hóa 4 kịch bản

```
Trường hợp 1: Cả 2 commit OK
  TX commit ──▶ [task ✓] [outbox ✓]
  Poller sau ──▶ kafka send ✓ → mark published
  ✓ Event được publish đúng 1 lần.

Trường hợp 2: App crash ngay sau COMMIT
  TX commit ──▶ [task ✓] [outbox ✓]
  Pod restart, Poller chạy lại
  ──▶ outbox vẫn còn → kafka send → mark published
  ✓ Không mất event.

Trường hợp 3: Kafka broker down
  TX commit ──▶ [task ✓] [outbox ✓]
  Poller send fail
  ──▶ outbox row vẫn published_at=NULL
  ──▶ retry tự động cho đến khi Kafka up
  ✓ Eventually delivered.

Trường hợp 4 (đã ngăn được): dual-write cũ
  INSERT task ✓
  kafka.send() ✗  ← network error
  ✗ ROLLBACK task — nhưng kafka có thể đã nhận!
  ✗ Inconsistency: event tồn tại nhưng task không.
```

#### Trade-off — Latency

Vì poller chạy mỗi 1s → event đến consumer chậm hơn ~500ms so với send trực tiếp. Nếu cần realtime hơn:
- Giảm `fixedDelay` xuống 100ms
- Dùng **Debezium CDC**: PostgreSQL WAL → Kafka, latency ~10ms, không cần code poller

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

#### Cấu trúc dữ liệu Trace & Span

```
Trace (1 user request) = trace_id duy nhất xuyên hệ thống
│
└─ Span (1 đơn vị công việc) — có span_id riêng, parent_span_id
   ├─ name        : "POST /api/tasks"
   ├─ start_time  : ...
   ├─ end_time    : ...
   ├─ attributes  : {http.method, user.id, db.statement}
   └─ events      : [exception, log]

Cây span:

trace_id=abc123 ─┬─ span "POST /api/tasks"          (root span)
                 │  ├─ span "DB save Task"           (child)
                 │  ├─ span "Redis cache.put"        (child)
                 │  └─ span "Kafka publish"          (child)
                 │      │
                 │      └─ propagate qua header `traceparent`
                 │         │
                 │         ▼
                 └─ span "NotificationConsumer"      (cùng trace, khác process)
                    ├─ span "DB save Notification"
                    └─ span "SMTP send"
```

#### Cơ chế propagation qua HTTP & Kafka

```
HTTP request                         Kafka message
─────────────                         ─────────────
Header:                               Header:
  traceparent:                         traceparent:
    00-abc123-span1-01                   00-abc123-span2-01
                                         (cùng trace_id, span_id mới)

W3C Trace Context format:
  00 - version
  abc123... - trace_id (16 bytes)
  span1... - parent_span_id (8 bytes)
  01 - flags (sampled?)
```

Spring Boot 3 + Micrometer **tự động** inject `traceparent` vào outgoing HTTP / Kafka headers, và extract khi nhận → bạn không cần code gì.

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

# Phần C — Roadmap Triển khai

## 33. Roadmap 6 Phase (12 tuần)

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

## 34. Effort Estimation Matrix

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

## 35. Quick-win Priorities

Nếu thời gian hạn chế, làm 5 task này trước (~5 ngày, ROI cao nhất):

1. **`@Version` optimistic locking** (0.5 ngày) — fix concurrency bug ngay
2. **Composite indexes** (0.5 ngày) — improve query performance 10x
3. **EntityGraph cho TaskRepository** (1 ngày) — fix N+1
4. **Distributed tracing + MDC** (1.5 ngày) — debug production dễ hơn x10
5. **Outbox pattern** (1.5 ngày) — không bao giờ mất event nữa

Sau quick-win này, dự án đã có "production grade ROI cao" mà không cần refactor lớn.

---

# Phần D — Java Core & JVM Mastery

> Senior Java engineer **bắt buộc** hiểu JVM ở mức "biết cái gì xảy ra dưới capo" — đặc biệt khi debug production OOM, tuning GC, hoặc giải thích vì sao app chậm.

## 36. JVM Memory Model & Heap Structure

### Tổng quan bộ nhớ JVM

```
┌──────────────────────────────────────────────────────────────┐
│  JVM Process Memory (toàn bộ RAM container thấy)             │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ HEAP (đối tượng Java — quản lý bởi GC)                  │ │
│  │                                                          │ │
│  │  Young Generation                                        │ │
│  │  ┌──────┐  ┌──────────┐  ┌──────────┐                   │ │
│  │  │ Eden │  │ Survivor │  │ Survivor │                   │ │
│  │  │      │  │   S0     │  │   S1     │                   │ │
│  │  └──────┘  └──────────┘  └──────────┘                   │ │
│  │  ───────────────────────────────────                    │ │
│  │  Old Generation (Tenured)                                │ │
│  │  ┌──────────────────────────────────────────┐           │ │
│  │  │ Long-lived objects (cache, beans, ...)   │           │ │
│  │  └──────────────────────────────────────────┘           │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ Metaspace (Class metadata — KHÔNG còn PermGen từ Java8) │ │
│  │  - Class definitions, method bytecodes, constant pool   │ │
│  │  - Default: tăng đến hết RAM nếu không -XX:MaxMetaspaceSize │
│  └─────────────────────────────────────────────────────────┘ │
│                                                               │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐ │
│  │ Code Cache   │ │ Thread Stack │ │ Direct Memory        │ │
│  │ (JIT compiled│ │ (1 stack/    │ │ (Netty buffers, NIO) │ │
│  │  native code)│ │  thread,512K)│ │ -XX:MaxDirectMemorySize│ │
│  └──────────────┘ └──────────────┘ └──────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

### Vòng đời 1 đối tượng (Generational Hypothesis)

```
new Task()
    │
    ▼
┌─────────┐  Minor GC ┌──────────┐  Minor GC ┌──────────┐
│  Eden   │ ─────────▶│ Survivor │ ─────────▶│ Survivor │
│         │           │   S0     │           │   S1     │
└─────────┘           └──────────┘           └──────────┘
                            │                      │
                            │ tenuring threshold   │
                            │ (default: 15 lần GC) │
                            ▼                      │
                      ┌──────────────────┐         │
                      │  Old Generation  │◀────────┘
                      └──────────────────┘
                            │
                            │ Major GC (Full GC) — pause lâu
                            ▼
                       Garbage collected
```

**Generational hypothesis**: hầu hết object chết trẻ → Minor GC chỉ quét Young (nhanh) thay vì cả heap.

### Tại sao OOM xảy ra — 5 vị trí khác nhau

| Lỗi | Vị trí | Nguyên nhân thường gặp |
|-----|--------|------------------------|
| `OutOfMemoryError: Java heap space` | Heap | Memory leak, cache không bounded, load file quá lớn |
| `OutOfMemoryError: Metaspace` | Metaspace | Class loader leak (hot reload dev, Groovy dynamic class) |
| `OutOfMemoryError: unable to create new native thread` | OS thread limit | Tạo quá nhiều `new Thread()`, không dùng pool |
| `OutOfMemoryError: Direct buffer memory` | Direct memory | Netty buffer leak, kết nối không close |
| `StackOverflowError` | Thread stack | Đệ quy vô hạn |

### Heap Sizing trong container

```bash
# CŨ — sai trong container:
-Xms2g -Xmx2g           # cố định, không respect cgroup limit

# MỚI — best practice từ Java 11+:
-XX:InitialRAMPercentage=50.0
-XX:MaxRAMPercentage=75.0
# JVM tự đọc cgroup memory limit → set heap = 75% RAM container
```

**Lý do để 25% RAM cho off-heap:**
- Metaspace (~100-300MB)
- Thread stacks (200 threads × 512KB = 100MB)
- Code cache (~50MB)
- Direct buffers (Netty, NIO)
- Native lib (Postgres JDBC, JNI)

---

## 37. Garbage Collection Deep Dive

### So sánh các GC algorithm

| GC | Pause Goal | Throughput | Heap size | Use case |
|----|-----------|------------|-----------|----------|
| **Serial** | Cao | Thấp | < 100MB | Embedded, CLI tool |
| **Parallel** | Cao | Cao nhất | < 8GB | Batch job, throughput-first |
| **G1** (default Java 9+) | ~200ms | Cao | 4GB - 32GB | General-purpose web app |
| **ZGC** | < 1ms | Trung bình | 8GB - 16TB | Latency-critical, large heap |
| **Shenandoah** | < 10ms | Trung bình | 4GB - 100GB+ | Tương tự ZGC, OpenJDK |

### G1GC — Region-based collector

```
G1 chia heap thành ~2000 regions (mỗi region 1-32MB):

┌────┬────┬────┬────┬────┬────┬────┬────┐
│ E  │ E  │ S0 │ O  │ O  │ E  │ S1 │ O  │
├────┼────┼────┼────┼────┼────┼────┼────┤
│ O  │ E  │ H  │ H  │ E  │ O  │ E  │ S0 │
├────┼────┼────┼────┼────┼────┼────┼────┤
│ E  │ O  │ E  │ E  │ O  │ E  │ E  │ E  │
└────┴────┴────┴────┴────┴────┴────┴────┘

E = Eden, S = Survivor, O = Old, H = Humongous (object > 50% region size)
```

**Khác Parallel GC:**
- Region có thể chuyển đổi vai trò (Eden ↔ Survivor ↔ Old)
- Mỗi cycle: G1 chỉ collect các region "rác nhất" (Garbage-First → tên G1)
- Predictable pause time: bạn nói `-XX:MaxGCPauseMillis=200` → G1 cố giữ pause ≤ 200ms

### ZGC — Sub-millisecond pause

```
Cách hoạt động chính:
1. Colored pointers (sử dụng bit không dùng trong địa chỉ 64-bit để encode trạng thái GC)
2. Concurrent mark + compact (chạy song song với application thread)
3. Load barrier (kiểm tra mỗi lần đọc reference)

Trade-off:
+ Pause < 1ms ngay cả heap 16TB
+ Scale tuyến tính với heap size
- Throughput thấp hơn G1 ~5-15% (do barrier overhead)
- Tốn memory hơn ~10% (forwarding tables)
```

**Khi nào dùng:**
```bash
# Khi p99 < 50ms là yêu cầu nghiêm ngặt (trading, real-time bidding)
-XX:+UseZGC -XX:+ZGenerational      # Java 21+ Generational ZGC
```

### Phân tích GC log

```bash
# Bật GC log (Java 9+)
-Xlog:gc*:file=/var/log/gc.log:time,uptime,level,tags:filecount=10,filesize=10M
```

Đọc log:
```
[2.345s][info][gc] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 100M->20M(256M) 25.123ms
       │            │                                                │       │       │
       │            │                                                │       │       └─ pause time
       │            │                                                │       └─ heap size sau GC
       │            │                                                └─ heap trước → sau
       │            └─ Loại GC (Young/Mixed/Full)
       └─ Thời điểm
```

**Tín hiệu cần lo lắng:**
- Full GC > 1 lần/giờ → memory leak hoặc heap quá nhỏ
- Pause > MaxGCPauseMillis nhiều lần → tăng heap hoặc đổi sang ZGC
- Allocation rate > 1GB/s → object churn quá cao, tối ưu code

### Tool phân tích

- **GCViewer** (offline) — load GC log, xem biểu đồ pause/throughput
- **gceasy.io** (online, free tier) — upload log, AI suggest tuning
- **JFR (Java Flight Recorder)** — sampling profiler built-in, low overhead

```bash
# Capture JFR 60s
jcmd <pid> JFR.start duration=60s filename=app.jfr
# Mở bằng JDK Mission Control (JMC)
```

---

## 38. Java Memory Model — Happens-before

### Vấn đề cốt lõi: Hiển thị giữa threads

```java
class Worker {
    private boolean stopped = false;       // shared variable

    public void run() {
        while (!stopped) {                 // Thread A đọc
            doWork();
        }
    }

    public void stop() {
        stopped = true;                    // Thread B ghi
    }
}
```

**Câu hỏi**: Thread A có thấy `stopped = true` ngay không?

**Đáp**: KHÔNG đảm bảo! Vì:
1. Compiler có thể optimize: cache `stopped` vào register (vòng lặp vô tận)
2. CPU có thể reorder instructions
3. Mỗi CPU core có cache L1/L2 riêng — không tự sync ngay

### Mô hình bộ nhớ thực tế

```
        ┌─── Thread A ───┐         ┌─── Thread B ───┐
        │ Registers      │         │ Registers      │
        │ stopped=false  │         │                │
        └───────┬────────┘         └───────┬────────┘
                │ L1 cache                 │ L1 cache
        ┌───────┴────────┐         ┌───────┴────────┐
        │ stopped=false  │         │ stopped=true   │  ← Thread B set
        └───────┬────────┘         └───────┬────────┘
                │                          │
                └──────── L2/L3 ───────────┘
                            │
                  ┌─────────┴─────────┐
                  │   Main Memory     │
                  │ stopped=??        │  ← chưa biết khi nào sync
                  └───────────────────┘
```

Nếu không có **memory barrier**, Thread A có thể nhìn vào L1 cache mãi mãi → vòng lặp không bao giờ thoát.

### Giải pháp: `volatile`

```java
private volatile boolean stopped = false;
```

`volatile` đảm bảo:
1. **Visibility**: ghi → flush về main memory ngay; đọc → load từ main memory
2. **Ordering**: cấm reorder qua biến volatile (insert memory barriers)
3. **KHÔNG đảm bảo atomicity** với compound op (`volatile int i; i++;` vẫn race condition)

### Happens-before — Quy tắc đảm bảo visibility

Một thao tác `A` **happens-before** `B` (HB) nếu kết quả của `A` được đảm bảo visible với `B`.

```
┌──────────────────────────────────────────────────────────────┐
│  Quy tắc happens-before:                                     │
│                                                               │
│  1. Program order: trong cùng thread, code trước HB code sau │
│  2. Monitor lock: unlock(M) HB tới lock(M) ở thread khác     │
│  3. Volatile: write(v) HB tới read(v) ở thread khác          │
│  4. Thread start: t.start() HB tới actions trong t           │
│  5. Thread join: actions trong t HB tới t.join() return      │
│  6. Transitivity: A HB B, B HB C → A HB C                    │
└──────────────────────────────────────────────────────────────┘
```

### Ví dụ áp dụng

```java
class SafePublish {
    private Config config;                    // KHÔNG volatile
    private volatile boolean ready = false;   // volatile flag

    // Thread A (publisher)
    void publish(Config c) {
        config = c;          // (1)
        ready = true;        // (2) volatile write
    }

    // Thread B (subscriber)
    Config consume() {
        if (ready) {         // (3) volatile read
            return config;   // (4) — đảm bảo thấy config từ (1)?
        }
        return null;
    }
}
```

**Phân tích:**
- (1) happens-before (2): program order
- (2) happens-before (3): volatile write HB volatile read
- (3) happens-before (4): program order
- → (1) HB (4): transitivity → Thread B đảm bảo thấy `config` từ (1)

Đây là "**piggyback synchronization**" — flag volatile bảo vệ luôn non-volatile field đi trước nó.

### `synchronized` vs `volatile` vs `Atomic*`

| Construct | Atomicity | Visibility | Ordering | Performance |
|-----------|-----------|------------|----------|-------------|
| `volatile` | ✗ (chỉ read/write đơn) | ✓ | ✓ | Nhanh nhất |
| `synchronized` | ✓ | ✓ | ✓ | Chậm (lock contention) |
| `AtomicInteger` | ✓ (CAS) | ✓ | ✓ | Trung bình |
| `final` field | — | ✓ (sau constructor) | ✓ | Free |

---

## 39. Class Loading & ClassLoader Hierarchy

### Hierarchy (Java 9+)

```
┌─────────────────────────────────────────────┐
│ Bootstrap ClassLoader (native, không Java)  │
│  → load java.lang.*, java.util.* ...        │
│  → rt.jar (Java 8) / java.base module (9+)  │
└──────────────────┬──────────────────────────┘
                   │ parent
┌──────────────────┴──────────────────────────┐
│ Platform ClassLoader                        │
│  → java.sql, java.xml, ... modules          │
└──────────────────┬──────────────────────────┘
                   │ parent
┌──────────────────┴──────────────────────────┐
│ Application ClassLoader (System)            │
│  → classpath ($CLASSPATH, -cp)              │
│  → JAR dependencies của app                 │
└──────────────────┬──────────────────────────┘
                   │ parent (Spring Boot fat JAR)
┌──────────────────┴──────────────────────────┐
│ LaunchedURLClassLoader (Spring Boot)        │
│  → BOOT-INF/classes/, BOOT-INF/lib/*.jar    │
└─────────────────────────────────────────────┘
```

### Quy tắc Parent Delegation

```
Khi load class "com.taskflow.TaskService":

  AppClassLoader.loadClass()
       │
       ▼
  Hỏi parent (Platform) trước
       │
       ▼
  Platform hỏi Bootstrap
       │
       ▼
  Bootstrap không có → return null
       │
       ▼
  Platform tự load → không có → return null
       │
       ▼
  AppClassLoader tự load từ classpath → ✓
```

**Lý do delegation**: ngăn user ghi đè class core. Ví dụ bạn không thể tạo `java.lang.String` của riêng mình — Bootstrap đã load rồi.

### Class Loading Lifecycle

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Loading   │───▶│   Linking   │───▶│Initialization│──▶│   Usage     │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                          │
                          ├─ Verification (bytecode hợp lệ?)
                          ├─ Preparation (cấp memory cho static field)
                          └─ Resolution (resolve symbolic ref → direct ref)
```

**Initialization (`<clinit>`)**: chạy static initializers + gán giá trị static field. Lazy — chỉ chạy khi class **lần đầu được dùng tích cực**.

```java
class Config {
    static final Logger LOG = LoggerFactory.getLogger(Config.class);
    static {
        System.out.println("Config loaded!");   // chạy 1 lần duy nhất
    }
}

Config.someStaticMethod();   // ← trigger init
```

### Hot Reload & ClassLoader Leak

Spring DevTools dùng **2 classloader** để hot-reload:
```
Base ClassLoader   → các JAR không đổi (Spring, Hibernate)
Restart ClassLoader → class của bạn (reload nhanh)
```

Khi restart: vứt restart classloader, tạo cái mới → class cũ bị GC.

**Vấn đề leak**: nếu base classloader giữ reference đến class của restart classloader (vd: ThreadLocal, cache static) → restart classloader không thể GC → **Metaspace OOM sau nhiều lần reload**.

```java
// SAI — gây leak nếu redeploy
static final Map<String, MyService> cache = new HashMap<>();

// ĐÚNG — dùng weak reference hoặc clean up
static final Map<String, WeakReference<MyService>> cache = new HashMap<>();
```

---

# Phần E — Concurrency Mastery

## 40. Virtual Threads (Project Loom)

### Vấn đề OS thread

```
1 OS thread = ~1MB stack + kernel context (~2-8KB)
Tomcat default pool: 200 threads
→ 200MB chỉ riêng thread stack
→ Mỗi request chiếm 1 OS thread suốt thời gian xử lý

Bottleneck: blocking I/O (DB, HTTP call)
→ Thread idle waiting I/O nhưng vẫn chiếm OS resource
→ Throughput limit ≈ pool size
```

### Virtual Threads (Java 21 GA)

```
Platform Thread (OS thread)         Virtual Thread (JVM-managed)
─────────────────────                ─────────────────────────
Stack: 1MB cố định                   Stack: vài KB, grow on demand
Tạo: ~ms                              Tạo: ~µs
Giới hạn: vài ngàn                    Giới hạn: hàng triệu
Scheduling: OS                        Scheduling: JVM (ForkJoinPool)
```

#### Cơ chế mounting

```
┌──────────────────────────────────────────────────────────┐
│ Carrier Threads (Platform — bằng số CPU core)            │
│  ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐         │
│  │ CPU0   │  │ CPU1   │  │ CPU2   │  │ CPU3   │         │
│  └───┬────┘  └───┬────┘  └───┬────┘  └───┬────┘         │
└──────┼───────────┼───────────┼───────────┼──────────────┘
       │ mount     │ mount     │ mount     │ mount
       ▼           ▼           ▼           ▼
   ┌──────┐    ┌──────┐    ┌──────┐    ┌──────┐
   │  VT  │    │  VT  │    │  VT  │    │  VT  │  ← chạy
   │  #1  │    │  #2  │    │  #3  │    │  #4  │
   └──────┘    └──────┘    └──────┘    └──────┘

Hàng đợi (queue) Virtual Threads chờ:
[VT#5] [VT#6] [VT#7] [VT#8] [VT#9] ... [VT#1000000]

Khi VT block (I/O):
  → unmount khỏi carrier
  → continuation lưu lại (heap)
  → carrier free để chạy VT khác
  → I/O xong → VT mounted lại lên carrier nào đó
```

### Sử dụng

```java
// Spring Boot 3.2+ — bật virtual thread
spring:
  threads:
    virtual:
      enabled: true
# → Tomcat dùng VT cho mỗi request thay vì pool thread cố định
```

```java
// Code thủ công
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 10_000).forEach(i ->
        executor.submit(() -> {
            // Mỗi task = 1 VT, blocking ok!
            return httpClient.send(req, BodyHandlers.ofString());
        }));
}
```

### Khi nào VT hữu ích vs hại

✓ **Hữu ích:**
- Code blocking-style (synchronous), nhiều I/O wait
- Microservice gọi nhiều downstream API
- Web request handler trả lời sau khi gọi DB, Redis, Kafka

✗ **KHÔNG dùng VT:**
- CPU-bound (encryption, image processing) — VT chỉ thêm overhead
- Code dùng `synchronized` heavy → "pinning" — VT giữ carrier không nhường được
  - Solution: dùng `ReentrantLock` thay `synchronized`
- ThreadLocal heavy — VT quá nhiều → ThreadLocal × N triệu → OOM

### Pinning Issue

```java
// SAI — synchronized pin VT vào carrier
synchronized (lock) {
    httpClient.send(...);   // I/O blocking khi đang synchronized
    // → VT KHÔNG unmount → carrier bị chiếm → throughput giảm
}

// ĐÚNG
ReentrantLock lock = new ReentrantLock();
lock.lock();
try {
    httpClient.send(...);   // VT có thể unmount như bình thường
} finally {
    lock.unlock();
}
```

Detect pinning:
```bash
-Djdk.tracePinnedThreads=full
```

---

## 41. CompletableFuture & Async Pipelines

### Sơ đồ pipeline

```
CompletableFuture<User> userF = CompletableFuture
    .supplyAsync(() -> fetchUser(id), executor)         (1)
    .thenApply(user -> enrichProfile(user))             (2)
    .thenCompose(user -> fetchPermissionsAsync(user))   (3)
    .exceptionally(ex -> defaultUser());                (4)

Diagram:

  Thread Pool
   ┌─────────┐
   │ task #1 │ ──── (1) fetchUser ────┐
   └─────────┘                        │
                                      ▼
   ┌─────────┐                  ┌──────────┐
   │ task #2 │ ──── (2) enrich  │  User    │
   └─────────┘     (sync, cùng  └──────────┘
                  thread vừa xong)     │
                                       ▼
                                  ┌──────────┐
                                  │  User    │
                                  └────┬─────┘
                                       │
                                       ▼ (3) compose — wait async result
   ┌─────────┐                  ┌──────────────┐
   │ task #3 │ ─ fetchPerms ──▶│ User + Perms │
   └─────────┘                  └──────────────┘
                                       │
                                       ▼ (4) fallback nếu exception
                                  ┌──────────┐
                                  │ Final ✓  │
                                  └──────────┘
```

### Kết hợp song song

```java
CompletableFuture<User> userF    = CompletableFuture.supplyAsync(() -> fetchUser(id));
CompletableFuture<Project> projF = CompletableFuture.supplyAsync(() -> fetchProject(pid));
CompletableFuture<List<Task>> taskF = CompletableFuture.supplyAsync(() -> fetchTasks(id));

// Đợi cả 3, combine kết quả
CompletableFuture<Dashboard> dashF = CompletableFuture
    .allOf(userF, projF, taskF)
    .thenApply(v -> new Dashboard(userF.join(), projF.join(), taskF.join()));

// Latency = max(t1, t2, t3) thay vì t1+t2+t3
```

```
Sequential (xấu):
[fetchUser 100ms][fetchProj 80ms][fetchTasks 150ms] = 330ms

Parallel (tốt):
[fetchUser  100ms]
[fetchProj   80ms]   ╮
[fetchTasks 150ms]   ├─▶ tổng = max = 150ms
                     ╯
```

### Pitfall: ForkJoinPool common

`supplyAsync()` không có executor → dùng `ForkJoinPool.commonPool()` (size = CPU - 1).
- Blocking IO trong common pool → starve cả JVM
- **Luôn truyền executor riêng cho blocking workload**

```java
private static final ExecutorService IO_EXECUTOR =
    Executors.newFixedThreadPool(50);  // pool riêng cho IO

CompletableFuture.supplyAsync(() -> blockingDbCall(), IO_EXECUTOR);
```

Hoặc dùng Virtual Thread executor (Java 21+):
```java
private static final ExecutorService VT_EXECUTOR =
    Executors.newVirtualThreadPerTaskExecutor();
```

---

## 42. Lock-free Programming

### CAS (Compare-And-Swap)

```
CAS(address, expected, new):
  if (*address == expected) {
      *address = new;
      return true;
  } else {
      return false;
  }

Atomic ở mức CPU instruction (LOCK CMPXCHG trên x86).
```

### `AtomicInteger.incrementAndGet()` thực chất là CAS loop

```java
public final int incrementAndGet() {
    int prev, next;
    do {
        prev = get();        // 1. đọc giá trị hiện tại
        next = prev + 1;     // 2. tính giá trị mới
    } while (!compareAndSet(prev, next));  // 3. CAS, retry nếu fail
    return next;
}
```

```
Thread A & B cùng increment counter=10:

Thread A: get()=10, next=11, CAS(10→11) ✓ → counter=11
Thread B: get()=10, next=11, CAS(10→11) ✗ ← fail, retry
Thread B: get()=11, next=12, CAS(11→12) ✓ → counter=12
```

→ **Không bao giờ lost update**, không cần lock.

### `LongAdder` — Tối ưu cho contention cao

Vấn đề: `AtomicLong` với 1000 thread cùng increment → CAS fail liên tục → throughput tụt.

`LongAdder` shard giá trị thành N cell, mỗi thread ghi cell riêng:

```
AtomicLong:
                      ┌──────────┐
  Thread 1 ──CAS────▶ │  counter │ ◀──CAS── Thread 2
  Thread 3 ──CAS────▶ │   = 42   │ ◀──CAS── Thread 4
                      └──────────┘
                      ↑ contention bottleneck!

LongAdder:
  Thread 1 ──▶ [cell 0: 10]
  Thread 2 ──▶ [cell 1: 8]    ╮
  Thread 3 ──▶ [cell 2: 12]   ├─▶ sum() = 42 (đọc gộp)
  Thread 4 ──▶ [cell 3: 12]   ╯
  ↑ không tranh giành
```

**Khi nào dùng:** counter ghi nhiều, đọc ít (metrics, statistics).

### Tools

- `AtomicReference<T>` — CAS cho object reference (lock-free stack, queue)
- `ConcurrentHashMap` — segmented, CAS-based (Java 8+)
- `LongAccumulator` — generalized LongAdder (max, min, custom)

---

## 43. ThreadPool Sizing & Tuning

### Công thức (Little's Law)

```
N_threads = N_cpu × U_target × (1 + W/C)

Trong đó:
  N_cpu    = số CPU core
  U_target = target CPU utilization (0.0 - 1.0)
  W        = thời gian wait (I/O)
  C        = thời gian compute
```

### Ví dụ tính

```
App của bạn:
  CPU = 4 core
  Target utilization = 0.8
  1 request: 10ms compute + 90ms DB wait
  → W/C = 90/10 = 9

  N_threads = 4 × 0.8 × (1 + 9) = 32

Tomcat pool size nên đặt ~32-40 cho workload này.
```

### Loại executor

| Executor | Behavior | Use case |
|----------|----------|----------|
| `newFixedThreadPool(n)` | n thread cố định, queue unbounded | Predictable load |
| `newCachedThreadPool()` | Tạo thread on-demand, idle 60s thì kill | **NGUY HIỂM**: load spike → OOM |
| `newSingleThreadExecutor()` | 1 thread, sequential | Order-sensitive task |
| `newWorkStealingPool()` | ForkJoinPool, work-steal | CPU-bound, divide-and-conquer |
| `newVirtualThreadPerTaskExecutor()` | 1 VT/task | IO-heavy (Java 21+) |

### Custom với ThreadPoolExecutor

```java
new ThreadPoolExecutor(
    corePoolSize,                   // 10 — luôn giữ
    maxPoolSize,                    // 50 — peak
    keepAliveTime, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(100),  // bounded! quan trọng
    new ThreadFactoryBuilder()
        .setNameFormat("taskflow-worker-%d")
        .setDaemon(false)
        .build(),
    new ThreadPoolExecutor.CallerRunsPolicy()  // backpressure
);
```

#### Reject Policy

```
Khi pool full + queue full:

AbortPolicy (default)     → throw RejectedExecutionException
CallerRunsPolicy          → caller thread tự chạy task (backpressure tự nhiên)
DiscardPolicy             → drop silently (NGUY HIỂM)
DiscardOldestPolicy       → drop task cũ nhất trong queue
```

`CallerRunsPolicy` thường tốt: khi pool full, caller bị block → tự nhiên slow down upstream.

### Monitor thread pool

```java
@Scheduled(fixedRate = 30000)
void logPoolStats() {
    log.info("active={}, queue={}, completed={}, rejected={}",
        executor.getActiveCount(),
        executor.getQueue().size(),
        executor.getCompletedTaskCount(),
        executor.getRejectedExecutionHandler());
}
```

Tốt hơn: Micrometer `ExecutorServiceMetrics`:
```java
ExecutorServiceMetrics.monitor(meterRegistry, executor, "taskflow-pool");
```
→ Prometheus có metrics `executor_pool_size_threads`, `executor_queued_tasks`.

---

# Phần F — Distributed Systems Patterns

## 44. CAP, PACELC & Consistency Models

### CAP Theorem

Trong distributed system, khi có **network partition (P)**, phải chọn:
- **Consistency (C)**: mọi node đọc cùng 1 giá trị mới nhất
- **Availability (A)**: mọi request đều được response (có thể stale)

```
        Consistency
            ▲
            │
       ┌────┴────┐
       │   CP    │   ← Partition xảy ra → từ chối read/write để giữ consistent
       │ (HBase, │     (vd: MongoDB primary down → secondary từ chối write)
       │  ZK,    │
       │  etcd)  │
       └────┬────┘
            │
        ────┼──── Partition tolerance
            │     (không tránh được trong distributed)
       ┌────┴────┐
       │   AP    │   ← Partition xảy ra → vẫn serve nhưng có thể stale
       │ (DynamoDB,    (eventual consistency)
       │  Cassandra,
       │  Redis cluster)
       └─────────┘
            │
            ▼
        Availability
```

### PACELC — Mở rộng CAP

```
IF Partition THEN choose between A and C  (như CAP)
ELSE          THEN choose between L and C  (latency vs consistency khi healthy)
```

| System | Partition? | Healthy? |
|--------|-----------|----------|
| MongoDB (mặc định) | CP | EC (low latency) |
| Cassandra | AP | EL (low latency, eventual) |
| PostgreSQL | CA (single node) | EC (strong consistency) |
| DynamoDB | AP | EL |

### Consistency Models — Spectrum

```
Strong ◀────────────────────────────────────────────────▶ Weak
│                                                          │
├─ Linearizability   (mọi op nhìn như thực hiện tuần tự)  │
│                                                          │
├─ Sequential        (consistent với program order)       │
│                                                          │
├─ Causal            (cause→effect ordering preserved)    │
│                                                          │
├─ Read-your-writes  (user thấy ngay write của mình)      │
│                                                          │
├─ Monotonic reads   (không bao giờ thấy giá trị cũ hơn)  │
│                                                          │
└─ Eventual          (cuối cùng cũng đồng nhất)           │
                                                           ▼
```

### TaskFlow áp dụng

- **PostgreSQL** (primary): linearizable cho task data → user thấy ngay change
- **Redis cache**: eventual — invalidate sau write, có thể stale ~ms
- **Kafka events**: at-least-once delivery + idempotent consumer = effectively-once

---

## 45. Saga Pattern

### Vấn đề: Distributed Transaction

```
Use case: Đặt hàng (e-commerce)
  1. Order service     — tạo order
  2. Payment service   — charge thẻ
  3. Inventory service — trừ stock
  4. Shipping service  — schedule giao hàng

Không thể dùng 2PC (Two-Phase Commit) vì:
- Microservice không share DB
- 2PC block resources lâu, không scale
```

### Saga = Chuỗi local transactions + Compensating actions

#### Orchestration (centralized)

```
   ┌──────────────────────┐
   │  Saga Orchestrator   │
   │  (state machine)     │
   └──────────────────────┘
        │   │   │   │
        ▼   ▼   ▼   ▼
   ┌───────┐ ┌────────┐ ┌──────────┐ ┌──────────┐
   │ Order │ │Payment │ │Inventory │ │ Shipping │
   └───────┘ └────────┘ └──────────┘ └──────────┘

Flow happy path:
  Orch → Order.create()        → ok
  Orch → Payment.charge()      → ok
  Orch → Inventory.reserve()   → ok
  Orch → Shipping.schedule()   → ok
  ✓ Done

Flow compensation:
  Orch → Order.create()        → ok
  Orch → Payment.charge()      → ok
  Orch → Inventory.reserve()   → FAIL (out of stock)
  Orch → Payment.refund()       ← compensate
  Orch → Order.cancel()         ← compensate
  ✗ Rolled back
```

#### Choreography (decentralized)

```
Mỗi service phát event sau khi thành công → service tiếp theo lắng nghe.

  Order ──(OrderCreated)──▶ Kafka ──▶ Payment
                                       │
  Payment ──(PaymentDone)──▶ Kafka ──▶ Inventory
                                       │
  Inventory ──(StockReserved)──▶ Kafka ──▶ Shipping

Compensation flow (Inventory fail):
  Inventory ──(StockFailed)──▶ Kafka ──▶ Payment.refund()
                                          │
                                          └──▶ Order.cancel()
```

### So sánh

| | Orchestration | Choreography |
|--|---------------|--------------|
| Coupling | Centralized | Loose |
| Visibility | Dễ trace (1 chỗ) | Khó (rải rác) |
| Failure handling | Rõ ràng | Phức tạp |
| Adding new step | Sửa orchestrator | Service mới subscribe event |
| Khi dùng | Logic phức tạp, nhiều branch | Pipeline tuyến tính |

### Implementation với Spring

- **Orchestration**: Spring State Machine, Camunda BPMN, AWS Step Functions
- **Choreography**: Kafka + Spring Cloud Stream + Outbox pattern (đã có ở mục 9)

### Saga property cần đảm bảo

```
1. Compensatable transactions — mỗi step có "undo"
   (vd: Payment.charge ↔ Payment.refund)

2. Idempotent — retry cùng request không nhân đôi
   (dùng idempotency key — mục 10)

3. Commutative compensation — undo có thể chạy theo thứ tự bất kỳ
   (vì failure có thể xảy ra ở step bất kỳ)
```

---

## 46. Event Sourcing

### Truyền thống (CRUD)

```
DB table tasks:
┌────┬──────────────┬──────────┬───────────┐
│ id │ title        │ status   │ priority  │
├────┼──────────────┼──────────┼───────────┤
│ 1  │ Fix bug      │ DONE     │ HIGH      │  ← snapshot hiện tại
└────┴──────────────┴──────────┴───────────┘

Vấn đề: mất lịch sử thay đổi (ai đổi gì, khi nào, lý do)
```

### Event Sourcing

```
Lưu chuỗi event thay vì state hiện tại:

events table:
┌────┬──────────┬───────────────────┬──────────────────────────────┐
│ id │ task_id  │ event_type        │ payload                       │
├────┼──────────┼───────────────────┼──────────────────────────────┤
│ 1  │ task-X   │ TaskCreated       │ {title:"Fix bug",pri:HIGH}    │
│ 2  │ task-X   │ TaskAssigned      │ {assignee:"alice"}            │
│ 3  │ task-X   │ TaskStatusChanged │ {from:TODO,to:IN_PROGRESS}    │
│ 4  │ task-X   │ TaskCommented     │ {comment:"PR ready"}          │
│ 5  │ task-X   │ TaskStatusChanged │ {from:IN_PROGRESS,to:DONE}    │
└────┴──────────┴───────────────────┴──────────────────────────────┘

State hiện tại = fold(events) — replay tất cả event để tính state.
```

### Lợi ích

- **Audit log miễn phí** — biết chính xác ai làm gì khi nào
- **Time-travel debugging** — replay state tại bất kỳ thời điểm nào
- **Temporal queries** — "task này có status gì 3 ngày trước?"
- **Event-driven side effects** — gửi notification, sync với search index từ event stream

### Thách thức

```
1. Schema evolution
   Event v1: {title, priority}
   Event v2: {title, priority, dueDate}
   → Phải versioning event hoặc dùng upcaster

2. Replay performance
   Task có 10000 events → fold mỗi lần đọc = chậm
   → Snapshot mỗi N events: lưu state hiện tại + chỉ replay events sau snapshot

3. Eventual consistency
   Read model (projection) build từ events → có lag
```

### CQRS + Event Sourcing (combo phổ biến)

```
┌──────────────┐                    ┌─────────────────┐
│  Command API │ ─── append ──────▶ │   Event Store   │
│  (write)     │                    │   (immutable)   │
└──────────────┘                    └────────┬────────┘
                                             │
                                             │ projection
                                             ▼
                                    ┌─────────────────┐
                                    │  Read Models    │
                                    │  (denormalized) │
                                    └────────┬────────┘
                                             │
                                             ▼
┌──────────────┐                    ┌─────────────────┐
│  Query API   │ ◀──── read ─────── │   PostgreSQL,   │
│  (read)      │                    │   Elasticsearch │
└──────────────┘                    └─────────────────┘
```

Frameworks: **Axon Framework**, **EventStoreDB**, **Apache Kafka** (event store đơn giản).

---

## 47. Sharding & Partitioning

### Vertical vs Horizontal partition

```
Vertical partitioning (tách cột):
┌──────────────────────────────────────┐
│ tasks                                │
│  id, title, status, priority         │ → tasks_core
│  description, attachments_blob       │ → tasks_detail (cold data)
└──────────────────────────────────────┘

Horizontal sharding (tách hàng theo key):
┌─────────────────────────────────────────────────────────┐
│ Shard 0: tasks với hash(project_id) % 4 == 0            │
│ Shard 1: tasks với hash(project_id) % 4 == 1            │
│ Shard 2: tasks với hash(project_id) % 4 == 2            │
│ Shard 3: tasks với hash(project_id) % 4 == 3            │
└─────────────────────────────────────────────────────────┘
```

### Shard Key Selection

| Strategy | Cách | Pros | Cons |
|----------|------|------|------|
| **Hash-based** | `hash(user_id) % N` | Phân bố đều | Range query phải scan all shards |
| **Range-based** | `created_at` ranges | Range query hiệu quả | Hot spot (data mới ghi vào 1 shard) |
| **Geo-based** | `region = US, EU, ASIA` | Latency thấp theo region | Khó rebalance |
| **Lookup table** | bảng map tenant→shard | Linh hoạt | Thêm 1 lookup query |

### Consistent Hashing

```
Hash ring (0 → 2^32):

       node_A (hash=100M)
       ┌──────────────┐
       │              │
node_D │              │ node_B (hash=900M)
(hash= │   RING       │
3.5B)  │              │
       │              │
       └──────────────┘
       node_C (hash=2B)

Key hash=600M → tìm node tiếp theo clockwise = node_B

Khi thêm node mới: chỉ ~1/N keys cần re-shard (thay vì gần như toàn bộ với hash mod N).
```

### Hot Spot vấn đề

```
Sharding theo user_id:
  Shard 0: user A (1M tasks)  ← celebrity user → hot shard
  Shard 1: user B (10 tasks)
  Shard 2: user C (50 tasks)
  ...

Giải pháp:
- Composite key: hash(user_id + task_id) — phân tán task của 1 user
- Sub-sharding: shard hot key tiếp ra N micro-shards
```

### Cross-shard query

```sql
-- ❌ Slow nếu tasks shard theo project_id
SELECT * FROM tasks WHERE assignee_id = ? ORDER BY due_date;
-- Phải query tất cả N shards, merge

-- ✓ Solutions:
-- 1. Secondary index sharded riêng (Elasticsearch)
-- 2. Denormalize: lưu thêm shard "by_assignee"
-- 3. Application-level scatter-gather + parallel query
```

---

# Phần G — Spring Boot Internals

## 48. Auto-Configuration Magic

### Sơ đồ flow startup

```
@SpringBootApplication
       │
       ├─ @SpringBootConfiguration  (= @Configuration)
       ├─ @ComponentScan
       └─ @EnableAutoConfiguration
              │
              ▼
       AutoConfigurationImportSelector
              │
              │ load tất cả file:
              │  META-INF/spring/org.springframework.boot.
              │  autoconfigure.AutoConfiguration.imports
              │
              ▼
       List ~150 auto-config class:
              │
              ├─ DataSourceAutoConfiguration
              ├─ JpaRepositoriesAutoConfiguration
              ├─ RedisAutoConfiguration
              ├─ KafkaAutoConfiguration
              ├─ SecurityAutoConfiguration
              └─ ...
              │
              ▼
       Mỗi class check @Conditional*:
              │
              ├─ @ConditionalOnClass(DataSource.class)         ✓ (Hikari trong classpath)
              ├─ @ConditionalOnMissingBean(DataSource.class)   ✓ (user chưa khai báo)
              ├─ @ConditionalOnProperty("spring.datasource.url") ✓
              │
              ▼
       → Apply config: tạo DataSource bean với Hikari
```

### Conditional annotations phổ biến

| Annotation | Khi nào active |
|-----------|----------------|
| `@ConditionalOnClass` | Class có trong classpath |
| `@ConditionalOnMissingClass` | Class KHÔNG có |
| `@ConditionalOnBean` | Bean đã được khai báo trước đó |
| `@ConditionalOnMissingBean` | Bean chưa có (user override) |
| `@ConditionalOnProperty` | Property có giá trị nhất định |
| `@ConditionalOnWebApplication` | App là servlet/reactive |
| `@ConditionalOnExpression` | SpEL expression true |

### Override mặc định

```java
// SecurityConfig của user
@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        // custom config
        return http.build();
    }
}

// → SecurityAutoConfiguration thấy @ConditionalOnMissingBean(SecurityFilterChain.class)
//   → skip, dùng config user
```

### Debug auto-config

```bash
# Bật debug: list điều kiện nào pass/fail
java -jar app.jar --debug

# Trong actuator
GET /actuator/conditions
```

Output:
```
positiveMatches:
   DataSourceAutoConfiguration#dataSource matched:
     - @ConditionalOnClass found 'javax.sql.DataSource'
     - @ConditionalOnMissingBean (types: DataSource) did not find any beans

negativeMatches:
   MongoAutoConfiguration: did not match:
     - @ConditionalOnClass did not find 'com.mongodb.MongoClient'
```

### Tạo Custom Starter

```
my-starter/
├── pom.xml
├── src/main/java/
│   └── com/taskflow/CustomAutoConfiguration.java
└── src/main/resources/
    └── META-INF/spring/
        org.springframework.boot.autoconfigure.AutoConfiguration.imports
        ← chứa: com.taskflow.CustomAutoConfiguration
```

```java
@AutoConfiguration
@ConditionalOnProperty("taskflow.feature.x.enabled")
public class CustomAutoConfiguration {
    @Bean
    public MyFeature myFeature() { ... }
}
```

---

## 49. Bean Lifecycle & Scopes

### Lifecycle complete

```
1. Instantiation
       │ new MyBean() (constructor injection)
       ▼
2. Populate properties
       │ setter / field injection
       ▼
3. Aware interfaces
       │ setBeanName, setBeanFactory, setApplicationContext
       ▼
4. BeanPostProcessor.postProcessBeforeInitialization()
       │ ← AOP proxy được tạo ở đây!
       ▼
5. @PostConstruct / InitializingBean.afterPropertiesSet() / init-method
       ▼
6. BeanPostProcessor.postProcessAfterInitialization()
       │
       ▼
7. Bean ready — sẵn sàng dùng
       │
       │ ... runtime ...
       │
       ▼
8. @PreDestroy / DisposableBean.destroy() / destroy-method
```

### Bean Scopes

| Scope | Số instance | Khi nào tạo | Use case |
|-------|------------|-------------|----------|
| `singleton` (default) | 1 / container | Eager (startup) | Stateless service |
| `prototype` | N (mỗi request injection) | Lazy | Stateful, không thread-safe |
| `request` | 1 / HTTP request | Per request | Request-scoped data |
| `session` | 1 / HTTP session | Per session | User session state |
| `application` | 1 / ServletContext | Per app | ServletContext-wide |
| `websocket` | 1 / WebSocket | Per socket | WS state |

### Scope mismatch pitfall

```java
// SAI — singleton inject prototype
@Service
public class TaskService {   // singleton
    @Autowired
    private TaskValidator validator;   // prototype — nhưng chỉ inject 1 lần!
}
```

→ `validator` được resolve khi `TaskService` được tạo (startup) → mọi request dùng cùng 1 instance, không phải prototype thực sự.

**Fix**: dùng `ObjectProvider` hoặc `@Lookup`:

```java
@Service
public class TaskService {
    @Autowired
    private ObjectProvider<TaskValidator> validatorProvider;

    public void create(...) {
        TaskValidator v = validatorProvider.getObject();  // mỗi lần 1 instance mới
        ...
    }
}
```

---

## 50. Spring AOP Proxy Mechanism

### Tại sao Spring dùng Proxy?

Khi bạn thêm `@Transactional` lên method:

```java
@Service
public class TaskService {
    @Transactional
    public Task save(Task t) {
        return taskRepo.save(t);
    }
}
```

Spring KHÔNG modify bytecode `TaskService`. Thay vào đó tạo **proxy** wrap nó.

### JDK Dynamic Proxy vs CGLIB

```
JDK Dynamic Proxy (interface-based):
─────────────────────────────────────
  Class:        TaskServiceImpl implements ITaskService
  Proxy:        $Proxy0 implements ITaskService
                  │
                  ├─ invoke() → AOP interceptor chain → real TaskServiceImpl

Yêu cầu: Bean phải implement interface.

CGLIB (class-based, default từ Spring Boot 2):
──────────────────────────────────────────────
  Class:        TaskService (no interface)
  Proxy:        TaskService$$EnhancerBySpringCGLIB extends TaskService
                  │
                  └─ override methods → AOP interceptor chain → super.method()

Yêu cầu: class non-final, method non-final.
```

### Visualize gọi method qua proxy

```
Client gọi: taskService.save(task)
              │
              ▼
       ┌────────────────────────────────────┐
       │ TaskService$$EnhancerBySpringCGLIB │  ← Proxy
       │  (extends TaskService)              │
       └────────────┬───────────────────────┘
                    │
                    │ enter interceptor chain
                    ▼
       ┌────────────────────────────────────┐
       │ TransactionInterceptor             │
       │  1. beginTransaction()             │
       │  2. proceed() ───────┐             │
       └─────────────────────│──────────────┘
                              │
                              ▼ super.save(task)
                    ┌────────────────────┐
                    │ TaskService (real) │
                    │  save() executes   │
                    └─────────┬──────────┘
                              │ return
                              ▼
       ┌────────────────────────────────────┐
       │ TransactionInterceptor             │
       │  3. commit()                       │
       │  4. return value                   │
       └────────────────────────────────────┘
                              │
                              ▼
                    Client nhận kết quả
```

### Pitfall: Self-invocation

```java
@Service
public class TaskService {
    public void createBatch(List<Task> tasks) {
        for (Task t : tasks) {
            this.save(t);    // ← KHÔNG đi qua proxy!
        }                     //   → @Transactional KHÔNG có hiệu lực
    }

    @Transactional
    public Task save(Task t) { ... }
}
```

`this.save(t)` gọi method trực tiếp trên `TaskService` (chứ không phải proxy) → AOP không can thiệp.

**Fix:**

```java
// Option 1: Tự inject chính mình
@Service
public class TaskService {
    @Autowired
    private TaskService self;     // self = proxy

    public void createBatch(List<Task> tasks) {
        tasks.forEach(self::save);  // gọi qua proxy → transactional active
    }
}

// Option 2: Tách ra class khác
@Service
@RequiredArgsConstructor
public class TaskBatchService {
    private final TaskService taskService;   // injected proxy

    public void createBatch(List<Task> tasks) {
        tasks.forEach(taskService::save);
    }
}

// Option 3: AspectJ load-time weaving (modify bytecode trực tiếp, không cần proxy)
@EnableAspectJAutoProxy(proxyTargetClass = true)  // CGLIB
// hoặc dùng @EnableLoadTimeWeaving (phức tạp hơn)
```

### Method visibility limit

```
JDK proxy:   chỉ public method (interface không có protected/private)
CGLIB proxy: public + protected (subclass-able), KHÔNG private/static/final
```

→ `@Transactional` trên `private` method **không hoạt động**.

---

## 51. @Transactional Pitfalls

### Pitfall 1: Self-invocation (xem mục 50)

### Pitfall 2: Propagation hiểu sai

```
REQUIRED (default):
  Caller có TX     → join TX
  Caller chưa có   → tạo TX mới

REQUIRES_NEW:
  Caller có TX     → suspend, tạo TX mới độc lập
  Caller chưa có   → tạo TX mới
  → Commit/rollback độc lập với outer

NESTED:
  Caller có TX     → savepoint trong TX hiện tại
  → Rollback chỉ revert tới savepoint
  → Chỉ support trên JDBC, không support JPA full

SUPPORTS:
  Caller có TX     → join
  Caller chưa có   → chạy non-transactional

NOT_SUPPORTED:
  Suspend TX hiện tại, chạy non-transactional

NEVER:
  Throw nếu có TX

MANDATORY:
  Throw nếu KHÔNG có TX (force caller bọc)
```

### Use case REQUIRES_NEW

```java
@Service
public class OrderService {

    @Transactional
    public void placeOrder(...) {
        orderRepo.save(order);            // TX-A

        try {
            auditService.logOrder(...);   // muốn audit LUÔN commit
        } catch (Exception e) {           // kể cả khi TX-A rollback
            log.warn("Audit failed", e);
        }
    }
}

@Service
public class AuditService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrder(...) {
        auditRepo.save(audit);   // TX-B độc lập
    }
}
```

→ Order rollback nhưng audit log vẫn lưu.

### Pitfall 3: Rollback rules

```java
@Transactional
public void process() {
    repo.save(...);
    throw new BusinessException();   // ← checked exception → KHÔNG rollback default!
}
```

Spring rollback default chỉ cho **unchecked** (`RuntimeException`, `Error`).

**Fix:**
```java
@Transactional(rollbackFor = Exception.class)  // rollback tất cả
```

### Pitfall 4: Read-only optimization

```java
@Transactional(readOnly = true)
public List<Task> findAll() { ... }
```

`readOnly = true`:
- Hibernate skip dirty checking (faster)
- JDBC driver có thể route đến read replica
- KHÔNG ngăn write (chỉ là hint) — write trong readOnly TX vẫn commit!

### Pitfall 5: Transaction timeout không tự cancel query

```java
@Transactional(timeout = 5)
public void slowMethod() {
    jdbc.queryForList("SELECT pg_sleep(60)");  // chạy 60s
}
```

`timeout=5` chỉ throw `TransactionTimedOutException` SAU KHI query xong. Query vẫn chạy 60s.

**Fix**: set `statement_timeout` ở DB hoặc `defaultStatementTimeout` ở DataSource:

```yaml
spring:
  datasource:
    hikari:
      data-source-properties:
        statement_timeout: 5000   # PostgreSQL — cancel query ở DB
```

### Sơ đồ ra/vào TX

```
HTTP request
   │
   ▼
@Transactional method
   │  ← TX bắt đầu (BEGIN)
   │
   ▼
Bussiness logic
   │
   ├─ repo.save() ─┐
   │               │
   │               ▼
   │           Hibernate flush ─► JDBC ─► PostgreSQL
   │               │                          │
   │               └──────────────────────────┘
   │                  (vẫn trong TX,
   │                   chưa COMMIT)
   ▼
return / throw
   │
   ▼
@Transactional aspect (after)
   │
   ├─ Thành công      → COMMIT
   ├─ RuntimeException → ROLLBACK
   └─ Checked Exception → COMMIT (trừ khi rollbackFor)
```

---

# Phần H — Database Deep Dive

## 52. PostgreSQL MVCC & Transaction Isolation

### MVCC (Multi-Version Concurrency Control)

PostgreSQL KHÔNG dùng read lock. Thay vào đó: **mỗi UPDATE tạo version mới của row, giữ version cũ cho transaction đang đọc**.

```
Row task_id=1 bên trong PostgreSQL (heap):

  ┌──────────────────────────────────────────────────────┐
  │ xmin=100, xmax=∞   title="A"   status=TODO           │ ← version 1
  ├──────────────────────────────────────────────────────┤
  │ xmin=105, xmax=∞   title="A"   status=IN_PROGRESS    │ ← version 2 (UPDATE)
  ├──────────────────────────────────────────────────────┤
  │ xmin=110, xmax=∞   title="B"   status=IN_PROGRESS    │ ← version 3 (UPDATE)
  └──────────────────────────────────────────────────────┘

  xmin = transaction id tạo version này
  xmax = transaction id xóa version này (∞ = chưa xóa)
```

### Tx isolation level

```
              ┌─ Dirty Read ─┬─ Non-repeatable ─┬─ Phantom Read ─┬─ Serialization
              │              │      Read         │                │   Anomaly
─────────────┼──────────────┼───────────────────┼────────────────┼─────────────────
READ UNCOMM. │     ✗        │       ✗            │      ✗          │      ✗
READ COMM.   │     ✓ ngăn   │       ✗            │      ✗          │      ✗
(PG default) │              │                    │                 │
REPEATABLE   │     ✓        │       ✓ ngăn       │      ✓ (PG)     │      ✗
SERIALIZABLE │     ✓        │       ✓            │      ✓          │      ✓ ngăn
```

### Ví dụ Non-repeatable Read

```
Tx A (READ COMMITTED)              Tx B
────────────────────               ────────────
BEGIN
SELECT * FROM tasks                BEGIN
WHERE id=1; → status=TODO          UPDATE tasks
                                   SET status=DONE
                                   WHERE id=1;
                                   COMMIT;

SELECT * FROM tasks
WHERE id=1; → status=DONE  ← KHÁC LẦN ĐỌC TRƯỚC!
COMMIT
```

→ Trong READ COMMITTED, Tx A có thể đọc khác nhau giữa 2 lần SELECT.

### REPEATABLE READ (Snapshot Isolation)

```
Tx A bắt đầu lúc T=100 → snapshot tại T=100.
Trong Tx A:
  - SELECT thấy state tại T=100
  - Update từ Tx khác (T > 100) → invisible
  - Tx A chỉ commit OK nếu không conflict với write khác
```

### SERIALIZABLE — Pessimistic detection

```
PostgreSQL dùng SSI (Serializable Snapshot Isolation):
1. Track read-write dependency giữa các Tx
2. Detect "dangerous structure" (cycle) → abort 1 Tx
   với SerializationFailureException
3. App retry Tx

→ KHÔNG block (như 2PL trong SQL Server) — vẫn snapshot read
→ NHƯNG có overhead tracking + retry cost
```

### Khi nào chọn level nào?

| Level | Khi nào | Trade-off |
|-------|---------|-----------|
| READ COMMITTED | Default web app, đa số case | Non-repeatable read có thể chấp nhận |
| REPEATABLE READ | Báo cáo, query nhiều dòng cần consistent | Serialization error → retry |
| SERIALIZABLE | Financial, accounting | Throughput thấp |

Trong Spring:
```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public Report generateMonthlyReport() {
    // tất cả query trong method nhìn cùng 1 snapshot
}
```

### Vacuum & Bloat

```
Mỗi UPDATE tạo version mới → version cũ thành "dead tuple".

Vacuum (autovacuum chạy nền) → xóa dead tuple, recycle space.

Vacuum behind → table size phình to (bloat) → query chậm.

Monitor:
SELECT relname, n_dead_tup, last_autovacuum
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;
```

---

## 53. B-Tree vs Hash vs GIN Index Internals

### B-Tree (default — 95% case)

```
B-Tree cho cột `due_date`:

                  [25, 50]                         ← internal node
                 /    |    \
            [5,15] [30,40] [60,80]                ← internal node
            / | \   / | \   / | \
          ...leaf pages with (key, ctid)...      ← leaf node

ctid = (page_id, offset) — pointer đến row trong heap
```

**Phù hợp**: equality (`=`), range (`<`, `>`, `BETWEEN`), prefix LIKE (`'foo%'`), ORDER BY.

#### Composite index — Quan trọng thứ tự

```sql
CREATE INDEX idx_t ON tasks(project_id, status, priority);
```

```
Query                                   Dùng được index?
────────────────────────────────────    ─────────────────
WHERE project_id=?                       ✓ (leftmost prefix)
WHERE project_id=? AND status=?          ✓
WHERE project_id=? AND priority=?        ✓ (project_id seek, priority filter)
WHERE status=? AND priority=?            ✗ (thiếu project_id)
WHERE priority=?                          ✗
```

**Quy tắc leftmost prefix**: index trên `(A, B, C)` dùng được khi WHERE chứa `A`, hoặc `A, B`, hoặc `A, B, C` — bỏ A không dùng được.

#### Index-only scan

```sql
CREATE INDEX idx ON tasks(project_id, status, due_date);

SELECT due_date FROM tasks WHERE project_id=? AND status='TODO';
```

Tất cả cột query cần có trong index → KHÔNG cần đọc heap → cực nhanh.

### Hash Index

```sql
CREATE INDEX idx ON tasks USING HASH (id);
```

```
Hash(key) → bucket → entry list

Phù hợp:  WHERE id = ?  (equality)
Không:    WHERE id < ?, ORDER BY id
```

PostgreSQL 10+ hash index đã WAL-logged → an toàn dùng. Tuy nhiên B-tree cũng O(log n) → trừ trường hợp rất đặc biệt, dùng B-tree.

### GIN (Generalized Inverted Index)

Cho cấu trúc multi-value: array, JSONB, full-text.

```
JSONB column `metadata`:
  task 1: {"tags": ["urgent", "bug"]}
  task 2: {"tags": ["feature"]}
  task 3: {"tags": ["bug", "feature"]}

GIN inverted:
  "urgent"  → [1]
  "bug"     → [1, 3]
  "feature" → [2, 3]
```

```sql
CREATE INDEX idx ON tasks USING GIN (metadata);

SELECT * FROM tasks WHERE metadata @> '{"tags": ["bug"]}';
-- → tìm "bug" trong inverted index → [1, 3] → fetch
```

### Full-text search

```sql
CREATE INDEX idx ON tasks USING GIN (to_tsvector('english', description));

SELECT * FROM tasks
WHERE to_tsvector('english', description) @@ to_tsquery('production & bug');
```

### Trigram Index (pg_trgm) — Fuzzy search

```sql
CREATE EXTENSION pg_trgm;
CREATE INDEX idx ON tasks USING GIN (title gin_trgm_ops);

-- Hỗ trợ LIKE '%foo%' và similarity
SELECT * FROM tasks WHERE title LIKE '%bug%';
SELECT * FROM tasks WHERE similarity(title, 'critical bug') > 0.3;
```

### Khi nào index làm CHẬM?

```
1. Write-heavy: mỗi INSERT/UPDATE phải update tất cả index
2. Low cardinality: index trên boolean → planner ignore, dùng seq scan rẻ hơn
3. Index không bao giờ dùng → tốn space, slow down write

Tìm unused index:
SELECT * FROM pg_stat_user_indexes WHERE idx_scan = 0;
```

---

## 54. Query Planner & EXPLAIN Mastery

### Đọc EXPLAIN output

```sql
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT t.*, u.username
FROM tasks t
JOIN users u ON t.assignee_id = u.id
WHERE t.project_id = 'abc'
  AND t.status = 'TODO'
ORDER BY t.due_date
LIMIT 50;
```

Output (giản lược):
```
Limit  (cost=125.30..125.42 rows=50 width=200) (actual time=2.45..2.78 rows=50 loops=1)
  Buffers: shared hit=120
  ->  Sort  (cost=125.30..127.83 rows=1012 width=200) (actual time=2.44..2.62)
        Sort Key: t.due_date
        Sort Method: top-N heapsort  Memory: 50kB
        ->  Hash Join  (cost=15.00..98.00 rows=1012 width=200) (actual time=0.45..2.10)
              Hash Cond: (t.assignee_id = u.id)
              ->  Index Scan using idx_tasks_project_status on tasks t
                    (cost=0.42..82.00 rows=1012 width=180) (actual time=0.02..1.20)
                    Index Cond: ((project_id = 'abc') AND (status = 'TODO'))
              ->  Hash  (cost=10.00..10.00 rows=400 width=20)
                    ->  Seq Scan on users u  ...
Planning Time: 0.250 ms
Execution Time: 2.85 ms
```

### Đọc cost & actual

```
(cost=X..Y rows=N width=W) (actual time=A..B rows=R loops=L)

cost=X    : cost để return row đầu tiên
cost=Y    : cost để return toàn bộ
rows=N    : ước tính planner
actual    : thực tế khi chạy
loops     : số lần node được execute (quan trọng với nested loop)
```

**Red flag:**
- `rows` estimate >> actual hoặc << actual → statistic outdated → `ANALYZE table`
- `Seq Scan` trên bảng > 10K rows trong join → thiếu index
- `Sort` chiếm > 50% thời gian → tạo index theo ORDER BY
- `loops=10000` → có thể nested loop bị blow up

### Join algorithms

```
Nested Loop:
  for each row in A:
      for each row in B matching:
          emit
  → Tốt khi: A nhỏ (< 100 rows), B có index trên join key
  → Tệ khi: cả 2 lớn → O(A × B)

Hash Join:
  1. Build hash từ A (table nhỏ hơn) trong memory
  2. Scan B, probe vào hash
  → Tốt khi: 1 bảng nhỏ vừa memory, equality join
  → Cần work_mem đủ lớn

Merge Join:
  1. Sort cả 2 theo join key
  2. Merge tuần tự
  → Tốt khi: cả 2 lớn, đã sort sẵn (có index)
```

### Tham số quan trọng

```sql
-- Cập nhật statistic
ANALYZE tasks;

-- Tăng sample rate (cho cột phân bố lệch)
ALTER TABLE tasks ALTER COLUMN status SET STATISTICS 1000;

-- Tăng work_mem cho session (hash join, sort)
SET work_mem = '64MB';

-- Force planner thử kế hoạch khác (debug only)
SET enable_seqscan = OFF;  -- bắt phải dùng index
EXPLAIN ANALYZE ...;
SET enable_seqscan = ON;
```

### Auto-explain

```sql
-- postgresql.conf hoặc per-session
LOAD 'auto_explain';
SET auto_explain.log_min_duration = 1000;   -- log query > 1s
SET auto_explain.log_analyze = ON;
```

→ Query chậm tự động log kèm EXPLAIN ANALYZE.

### Pgbouncer / Connection pooling

```
App ─┬─ HikariCP (20 conn)
     │      │
     │      ▼
     │  PgBouncer (transaction pool, 1000 client → 20 PG conn)
     │      │
     │      ▼
     └─ PostgreSQL (20 real conn, max_connections=100)
```

`max_connections` PG nên thấp (vd 100). Dùng PgBouncer trước để multiplex.

---

## 55. Caching Patterns

### Cache-aside (Lazy loading) — PHỔ BIẾN NHẤT

```
READ:
  App ──▶ Cache (Redis)
          │
          ├─ HIT  → return cached
          │
          └─ MISS → App ──▶ DB ──▶ App ──▶ Cache.put(key, value, TTL)
                                                   │
                                                   └─ Return value

WRITE:
  App ──▶ DB (write)
  App ──▶ Cache.delete(key)   ← invalidate

  → Lần đọc tiếp theo: MISS → load fresh từ DB
```

```
┌─────┐    1. get(k)    ┌───────┐
│ App │ ───────────────▶│ Cache │
└──┬──┘                 └───┬───┘
   │                        │ MISS
   │ 2. SELECT              ▼
   │                    (cache miss)
   │
   ▼
┌─────┐    3. value     ┌───────┐
│ DB  │ ───────────────▶│ App   │
└─────┘                 └───┬───┘
                            │ 4. set(k, v, TTL)
                            ▼
                        ┌───────┐
                        │ Cache │
                        └───────┘
```

**Ưu**: Đơn giản, cache chỉ chứa data thực sự được dùng.
**Nhược**: Cache stampede — nhiều request cùng MISS 1 key đắt → DB bị đập.

#### Cache stampede mitigation

```java
// Probabilistic early refresh
long ttlRemaining = redis.ttl(key);
double beta = 1.0;
double randomDelta = -Math.log(Math.random()) * computeTime * beta;
if (ttlRemaining - randomDelta <= 0) {
    refresh();   // refresh sớm vài request, tránh expire đồng loạt
}

// Distributed lock — chỉ 1 thread compute
if (redis.setIfAbsent("lock:"+key, "1", 30s)) {
    try {
        value = loadFromDB();
        redis.set(key, value, ttl);
    } finally {
        redis.delete("lock:"+key);
    }
}
```

### Write-through

```
WRITE:
  App ──▶ Cache.put(k, v) ──▶ Cache ──▶ DB
                              (cache tự sync DB)

READ:
  App ──▶ Cache (luôn fresh)
```

**Ưu**: Cache luôn consistent với DB.
**Nhược**: Latency write cao (2 hops), cache chứa cả data không bao giờ đọc.

### Write-behind (Write-back)

```
WRITE:
  App ──▶ Cache.put(k, v)
          │
          │ async, batch
          ▼
        DB (eventually)
```

**Ưu**: Write nhanh nhất.
**Nhược**: Mất data nếu cache crash trước khi flush. Phức tạp.

### Read-through

```
App ──▶ Cache ──▶ DB
        (cache tự load nếu miss)

App không biết DB tồn tại — chỉ talk với cache.
```

Thường dùng với cache library tích hợp (Hazelcast, Apache Ignite).

### Refresh-ahead

```
Cache tự refresh trước khi TTL hết — predict access pattern.

Phù hợp: hot key, access pattern đều đặn.
```

### So sánh

| Pattern | Write Latency | Read Latency | Consistency | Complexity |
|---------|---------------|--------------|-------------|-----------|
| Cache-aside | Low | Low (hit) / High (miss) | Eventual | Low |
| Write-through | High | Low | Strong | Medium |
| Write-behind | Lowest | Low | Eventual (lose risk) | High |
| Read-through | Low | Low (hit) / High (miss) | Eventual | Medium |
| Refresh-ahead | Low | Low | Eventual | High |

### Áp dụng TaskFlow

```java
@Service
public class TaskService {

    @Cacheable(value = "tasks", key = "#id", unless = "#result == null")
    public TaskResponse getTask(UUID id) {
        return taskRepository.findById(id)
            .map(TaskResponse::from)
            .orElse(null);
    }

    @CacheEvict(value = "tasks", key = "#id")
    @Transactional
    public TaskResponse updateTask(UUID id, ...) {
        // ...
    }
}
```

→ Spring Cache abstraction = cache-aside pattern.

### Invalidation strategy

```
"There are only two hard things in Computer Science:
 cache invalidation and naming things." — Phil Karlton

Strategies:
1. TTL (time-based)       — đơn giản, có thể stale
2. Event-based invalidate — chính xác hơn, complex
3. Version-based          — key có version (vd: "task:123:v5"), bump version để invalidate
```

---

## Tài liệu tham khảo

### Spring & Resilience
- [Spring Boot Production-ready Features](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#production-ready)
- [Spring Framework Reference — Transaction](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
- [Resilience4j Docs](https://resilience4j.readme.io/)

### Distributed Systems
- [Microservices Patterns — Chris Richardson](https://microservices.io/patterns/)
- [Designing Data-Intensive Applications — Martin Kleppmann](https://dataintensive.net/)
- [microservices.io — Saga, CQRS, Outbox catalog](https://microservices.io/patterns/data/saga.html)

### JVM, Concurrency
- [Java Memory Model Pragmatics — Aleksey Shipilëv](https://shipilev.net/blog/2014/jmm-pragmatics/)
- [Java Concurrency in Practice — Brian Goetz](https://jcip.net/)
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [GC Tuning Guide (Oracle)](https://docs.oracle.com/en/java/javase/21/gctuning/)

### Database
- [High-Performance Java Persistence — Vlad Mihalcea](https://vladmihalcea.com/books/high-performance-java-persistence/)
- [PostgreSQL — Internals & MVCC](https://www.postgresql.org/docs/current/mvcc.html)
- [Use The Index, Luke! — Markus Winand](https://use-the-index-luke.com/)

### Observability & DevOps
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [The Twelve-Factor App](https://12factor.net/)
- [Cloud Native Patterns — Cornelia Davis](https://www.manning.com/books/cloud-native-patterns)
- [Site Reliability Engineering (Google)](https://sre.google/books/)

---

## Changelog tài liệu

| Ngày | Thay đổi | Phần |
|------|----------|------|
| 2026-05-12 | Bổ sung Phần D — Java Core & JVM Mastery (4 mục): heap structure, GC deep dive, Java Memory Model, ClassLoader | §36-39 |
| 2026-05-12 | Bổ sung Phần E — Concurrency Mastery (4 mục): Virtual Threads, CompletableFuture, lock-free, threadpool sizing | §40-43 |
| 2026-05-12 | Bổ sung Phần F — Distributed Systems (4 mục): CAP/PACELC, Saga, Event Sourcing, Sharding | §44-47 |
| 2026-05-12 | Bổ sung Phần G — Spring Boot Internals (4 mục): Auto-config, Bean lifecycle, AOP proxy, @Transactional pitfalls | §48-51 |
| 2026-05-12 | Bổ sung Phần H — Database Deep Dive (4 mục): MVCC, index internals, query planner, caching patterns | §52-55 |
| 2026-05-12 | Thêm sơ đồ ASCII: Circuit Breaker state machine, Outbox 4-scenarios, Distributed Tracing W3C, Bean lifecycle, AOP proxy flow | §8,9,13,49,50 |
