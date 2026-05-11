# TaskFlow API — Senior Spring Boot Project Plan

> Nâng cấp TaskFlow từ **MVP mid-level** (✅ hoàn thành 10/5/2026) lên **Senior Production-grade**.
>
> Tài liệu này định nghĩa **stack công nghệ mới nhất 2025-2026**, kiến trúc mục tiêu và lịch 24 sessions (12 tuần).

---

## Mục tiêu

| Trạng thái hiện tại | Mục tiêu Senior |
|---------------------|-----------------|
| Spring Boot 3.3, JPA, JWT thuần | Spring Boot 3.5, OAuth2 Resource Server |
| Redis cache cơ bản | Redis + Resilience4j Circuit Breaker |
| Kafka direct publish | Transactional Outbox + At-least-once |
| Text logging | Structured JSON + MDC + Distributed Tracing |
| Docker Compose dev | Kubernetes + Helm + Canary Deploy |
| Unit + Integration test | + Mutation + Contract + Performance + Chaos |
| Layered architecture | Hexagonal / Clean Architecture |
| No rate limiting | Bucket4j + tier-based rate limiting |

---

## Stack Công nghệ Senior (2025-2026)

### Core Platform

| Layer | Công nghệ | Version | Ghi chú |
|-------|-----------|---------|---------|
| Language | **Java 21 LTS** | 21.0.x | Virtual Threads, Records, Pattern Matching, Sequenced Collections |
| Framework | **Spring Boot 3.5** | 3.5.x | Tích hợp Spring Framework 6.2 |
| Build | **Maven** / Gradle 8 | 3.9.x | |
| JVM | GraalVM / OpenJDK 21 | | ZGC Generational (Java 21+) |

### Data Layer

| Công nghệ | Version | Mục đích |
|-----------|---------|---------|
| **PostgreSQL 17** | 17.x | Primary DB — JSON operators, Partitioning |
| **Spring Data JPA / Hibernate 7** | 7.0 | ORM, @EntityGraph, @BatchSize |
| **Flyway 10** | 10.x | DB migrations, expand-contract pattern |
| **HikariCP** | (bundled) | Connection pool tuning |
| **Redis 7.4** | 7.4.x | Cache, Rate Limiting (Bucket4j), Idempotency keys |
| **Redisson** | 3.36.x | Distributed locking (Redlock) |

### Messaging & Events

| Công nghệ | Version | Mục đích |
|-----------|---------|---------|
| **Apache Kafka 3.9** | 3.9.x | Event streaming |
| **Spring Kafka** | 3.3.x | Producer/Consumer wrapper |
| **Debezium** | 3.0.x | CDC để implement Outbox pattern (alternative) |

### Resilience

| Công nghệ | Version | Pattern |
|-----------|---------|---------|
| **Resilience4j** | 2.2.x | Circuit Breaker, Retry, Bulkhead, TimeLimiter |
| **Bucket4j** | 8.10.x | Token bucket rate limiting (Redis-backed) |

### Security

| Công nghệ | Version | Mục đích |
|-----------|---------|---------|
| **Spring Security 6.4** | 6.4.x | OAuth2 Resource Server, HTTP headers |
| **Keycloak 26** | 26.x | Identity Provider (OIDC/OAuth2) |
| **Spring Cloud Vault** | 4.x | Secrets management |
| **Vault** | 1.18.x | HashiCorp Vault |

### Observability

| Công nghệ | Version | Mục đích |
|-----------|---------|---------|
| **Micrometer Tracing** | 1.4.x | Distributed tracing bridge |
| **OpenTelemetry Java** | 2.x | OTel SDK / Exporter OTLP |
| **Jaeger** | 2.x | Trace backend (dev) |
| **Logback + Logstash Encoder** | 8.x | JSON structured logging |
| **Prometheus + Grafana** | latest | Metrics visualization |

### Architecture & Testing

| Công nghệ | Version | Mục đích |
|-----------|---------|---------|
| **ArchUnit** | 1.4.x | Architecture rule tests |
| **Pitest** | 1.17.x | Mutation testing |
| **Pact** | 4.6.x | Contract testing (provider) |
| **k6** | latest | Performance / load testing |
| **Chaos Monkey** | 3.1.x | Chaos Engineering |
| **WireMock** | 3.x | External service mocking |

### Infrastructure & DevOps

| Công nghệ | Version | Mục đích |
|-----------|---------|---------|
| **Kubernetes (K8s)** | 1.32.x | Container orchestration |
| **Helm** | 3.17.x | K8s packaging |
| **Argo Rollouts** | 1.8.x | Canary / Blue-Green deployment |
| **Docker** | 27.x | Container runtime |
| **GitHub Actions** | | CI/CD |

---

## Kiến trúc Mục tiêu

### Hexagonal Architecture (Ports & Adapters)

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Core                          │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   Domain Layer                           ││
│  │  Task, Project, User (rich domain models)                ││
│  │  Business Rules: TaskStatus transitions, Permissions     ││
│  └─────────────────────────────────────────────────────────┘│
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                 Application Layer                        ││
│  │  CreateTaskUseCase, AssignTaskUseCase                    ││
│  │  GetTaskBoardQuery, SearchTasksQuery (CQRS)              ││
│  └─────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────┘
            ▲ Inbound Ports            Outbound Ports ▼
            │                                        │
   ┌────────┴────────┐              ┌────────────────┴──────┐
   │  Inbound Adapters│              │  Outbound Adapters     │
   ├─────────────────┤              ├───────────────────────┤
   │ REST Controllers │              │ JpaTaskRepository      │
   │ Kafka Consumers │              │ RedisTaskCache         │
   │ (v1, v2)        │              │ KafkaEventPublisher    │
   └─────────────────┘              │ OutboxRepository       │
                                    │ VaultSecretsAdapter    │
                                    └───────────────────────┘
```

### CQRS Flow

```
Write (Command side)               Read (Query side)
─────────────────                  ────────────────
POST /api/v1/tasks                 GET /api/v1/tasks/board
      ↓                                   ↓
CreateTaskUseCase              GetBoardQuery (CQRS)
      ↓                                   ↓
TaskCommandService            TaskQueryService
      ↓                                   ↓
JPA Entity (Task)             JdbcTemplate (read replica)
      ↓                        → TaskBoardView (DTO, no entity)
Outbox (→ Kafka)
```

### Observability Stack

```
App ──OTLP──→ OpenTelemetry Collector ──→ Jaeger (traces)
 │                                   └──→ Prometheus (metrics)
 │                                            ↓
 └──JSON logs──→ Fluentd/Promtail ──→ Grafana (dashboards)
```

---

## 6 Phases — 12 Tuần — 24 Sessions

| Phase | Tuần | Sessions | Mục tiêu | Effort |
|-------|------|----------|----------|--------|
| **1: Performance** | 1-2 | S1–S6 | N+1 fix, @Version, Index, HikariCP | 8 days |
| **2: Resilience** | 3-4 | S7–S12 | Resilience4j, Outbox, Idempotency, Redisson | 12 days |
| **3: Observability** | 5-6 | S13–S16 | Tracing, Structured Logging, Health | 6 days |
| **4: Security** | 7-8 | S17–S21 | Rate Limit, Headers, Encryption, Vault | 10 days |
| **5: Architecture** | 9-10 | S22–S28 | Hexagonal, CQRS, Versioning, ArchUnit | 15 days |
| **6: Testing+DevOps** | 11-12 | S29–S36 | Pitest, Pact, k6, K8s, Helm, Canary | 10 days |

---

## Chi tiết Sessions

### Phase 1 — Performance Hardening

| Session | Chủ đề | Task chính |
|---------|--------|-----------|
| S1 | @Version + Flyway | Thêm `version` column vào `tasks`, `projects`, `comments` |
| S2 | EntityGraph | Refactor TaskRepository → `@EntityGraph` cho list queries |
| S3 | DTO Projection | Tạo `TaskListItem`, `TaskBoardView` projections |
| S4 | Composite Indexes | Flyway `V4__indexes.sql` + partial indexes |
| S5 | HikariCP Tuning | `application-prod.yml` + leak detection |
| S6 | N+1 Tests + 409 | Hypersistence assertions + OptimisticLock exception handler |

### Phase 2 — Resilience

| Session | Chủ đề | Task chính |
|---------|--------|-----------|
| S7 | Resilience4j Setup | Dependency + CircuitBreaker cho Redis reads |
| S8 | Retry + Bulkhead | Retry Kafka publish, Bulkhead cho report endpoints |
| S9 | Outbox Table | Flyway `V5__outbox.sql` + `OutboxEvent` entity |
| S10 | OutboxPoller | `@Scheduled` poller + refactor NotificationProducer |
| S11 | Idempotency | `IdempotencyService` + Redis storage |
| S12 | Graceful + Redisson | Shutdown config + Distributed lock scheduled jobs |

### Phase 3 — Observability

| Session | Chủ đề | Task chính |
|---------|--------|-----------|
| S13 | Tracing Setup | Micrometer + OTel + Jaeger docker-compose |
| S14 | Custom Spans | Span trong TaskService, ProjectService |
| S15 | MDC + JSON Log | MdcFilter + LogstashEncoder + Async appender |
| S16 | Health Indicators | KafkaHealthIndicator + liveness/readiness split |

### Phase 4 — Security Hardening

| Session | Chủ đề | Task chính |
|---------|--------|-----------|
| S17 | Rate Limiting | Bucket4j filter + Redis-backed buckets |
| S18 | Tier-based Limit | FREE/PRO/ENTERPRISE tiers + tests |
| S19 | HTTP Headers | CSP, HSTS, X-Frame-Options |
| S20 | Encryption at Rest | `EncryptedStringConverter` + email_hash column |
| S21 | Vault Integration | Spring Cloud Vault + K8s auth |

### Phase 5 — Architecture

| Session | Chủ đề | Task chính |
|---------|--------|-----------|
| S22 | ArchUnit Setup | Dependency rule tests + no-cycle tests |
| S23 | Hexagonal Domain | `Task` domain model, value objects, `TaskId` |
| S24 | Hexagonal Ports | `CreateTaskUseCase`, `TaskRepository` port, `EventPublisher` port |
| S25 | JPA Adapter | `JpaTaskRepository` adapter + mapper |
| S26 | Kafka Adapter | `KafkaEventPublisher` adapter |
| S27 | CQRS Query Side | `TaskQueryService` với JdbcTemplate + `TaskBoardView` |
| S28 | API Versioning | `/api/v1/` prefix + Swagger update |

### Phase 6 — Testing & DevOps

| Session | Chủ đề | Task chính |
|---------|--------|-----------|
| S29 | Pitest Setup | Plugin config + first mutation test run |
| S30 | Pitest Fixes | Fix weak tests found by mutation testing |
| S31 | Pact Contract | Provider verification test |
| S32 | k6 Load Test | Load script + CI integration |
| S33 | Helm Chart | Chart.yaml + values.yaml + deployment template |
| S34 | K8s Manifests | HPA + PDB + Ingress |
| S35 | Canary Deploy | Argo Rollouts config + analysis template |
| S36 | JVM + Chaos | GC flags + Chaos Monkey staging config |

---

## Tech Decisions

### Tại sao Spring Boot 3.5 thay vì 3.3?

- Spring Boot 3.5 (dự kiến Q2 2026): Spring Framework 6.2, AOT improvements
- Virtual Threads ổn định hơn (Project Loom production-ready)
- GraalVM Native Image hỗ trợ tốt hơn
- Nếu chưa release: dùng 3.3.x, upgrade sau

### Tại sao Keycloak thay vì JWT thuần?

- Keycloak 26: OpenID Connect Discovery, JWKS endpoint tự động
- Token rotation + revocation built-in
- MFA, social login, admin UI sẵn
- Spring OAuth2 Resource Server chỉ verify JWT via JWKS → 0 code change cho auth logic

### Tại sao Outbox thay vì Saga?

- TaskFlow không có cross-service transactions → Outbox đủ
- Outbox đơn giản hơn Saga, dễ debug hơn
- Saga phù hợp microservices với nhiều service cùng tham gia 1 transaction

### Tại sao Hexagonal chỉ cho 1 module (POC)?

- Boilerplate lớn (mapper giữa domain ↔ JPA entity)
- TaskFlow là monolith → Layered đủ tốt
- Hexagonal POC để học pattern, không áp dụng toàn bộ codebase

---

## Quick-win (Làm trước nếu thời gian hạn chế)

Chỉ cần 5 sessions này để có production-grade ROI cao nhất:

| Priority | Session | Task | Thời gian |
|----------|---------|------|-----------|
| 1 | S1 | @Version optimistic locking | 0.5 ngày |
| 2 | S4 | Composite indexes | 0.5 ngày |
| 3 | S2+S3 | EntityGraph + DTO Projection | 1 ngày |
| 4 | S13-S15 | Distributed tracing + MDC | 1.5 ngày |
| 5 | S9-S10 | Outbox pattern | 1.5 ngày |

---

## Dependencies giữa Phases

```
Phase 1 (Performance) ──────────────────────┐
                                             ▼
Phase 3 (Observability) → có thể làm song song với Phase 2
                                             │
Phase 2 (Resilience) ────────────────────────┤
                                             ▼
Phase 4 (Security) ──────────────────────────┤
                                             ▼
Phase 5 (Architecture) [refactor, cần Phase 1-4 stable]
                                             │
                                             ▼
Phase 6 (Testing + DevOps) [cần code stable từ tất cả phases trên]
```

**Có thể song song:**
- Phase 3 (Observability) không phụ thuộc Phase 2 → có thể làm song song
- ArchUnit (S22) có thể làm ngay sau MVP vì chỉ là tests

---

*Cập nhật lần cuối: 11/5/2026*
