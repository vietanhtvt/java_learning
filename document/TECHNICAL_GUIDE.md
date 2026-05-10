# TaskFlow API — Hướng Dẫn Kỹ Thuật Chi Tiết

> Tài liệu giải thích toàn bộ kỹ thuật được áp dụng trong dự án, kèm ưu/nhược điểm và lý do lựa chọn.
>
> 📘 **Xem thêm:** [`SENIOR_ROADMAP.md`](./SENIOR_ROADMAP.md) — kế hoạch nâng cấp lên senior production-grade (Resilience4j, Outbox Pattern, Distributed Tracing, Hexagonal Architecture, Kubernetes...).

---

## Mục lục

1. [Java 21 Modern Features](#1-java-21-modern-features)
2. [Spring Boot 3.3 & IoC / DI](#2-spring-boot-33--ioc--di)
3. [JPA Auditing & BaseEntity](#3-jpa-auditing--baseentity)
4. [Entity Relationships & Fetch Strategy](#4-entity-relationships--fetch-strategy)
5. [Flyway Database Migration](#5-flyway-database-migration)
6. [Repository Pattern & Custom JPQL](#6-repository-pattern--custom-jpql)
7. [Service Layer & Transactions](#7-service-layer--transactions)
8. [DTO Pattern với Java Records](#8-dto-pattern-với-java-records)
9. [Spring Security 6 — STATELESS Architecture](#9-spring-security-6--stateless-architecture)
10. [JWT Authentication (JJWT 0.12)](#10-jwt-authentication-jjwt-012)
11. [Bean Validation & Custom Constraints](#11-bean-validation--custom-constraints)
12. [Exception Handling — RFC 7807 Problem Details](#12-exception-handling--rfc-7807-problem-details)
13. [Method-level Security — @PreAuthorize](#13-method-level-security--preauthorize)
14. [CORS Configuration](#14-cors-configuration)
15. [Pagination với Spring Data](#15-pagination-với-spring-data)
16. [Redis Cache — @Cacheable & RedisCacheManager](#16-redis-cache--cacheable--rediscachemanager)
17. [Spring AOP — LoggingAspect & AuditAspect](#17-spring-aop--loggingaspect--auditaspect)
18. [Apache Kafka — Event-Driven Architecture](#18-apache-kafka--event-driven-architecture)
19. [Testing — JUnit 5 + Mockito + Testcontainers](#19-testing--junit-5--mockito--testcontainers)
20. [Docker — Multi-stage Build & docker-compose](#20-docker--multi-stage-build--docker-compose)
21. [CI/CD — GitHub Actions](#21-cicd--github-actions)
22. [Actuator & Micrometer — Observability](#22-actuator--micrometer--observability)

---

## 1. Java 21 Modern Features

### 1.1 Records

```java
// Thay thế class DTO truyền thống bằng record
public record CreateProjectRequest(
    @NotBlank String name,
    @Size(max = 2000) String description
) {}

// Tương đương với class này nhưng ngắn gọn hơn nhiều:
public class CreateProjectRequest {
    private final String name;
    private final String description;
    // constructor, getters, equals, hashCode, toString — tất cả tự sinh
}
```

**Ưu điểm:**
- Tự động sinh constructor, getters, `equals()`, `hashCode()`, `toString()`
- **Immutable** (bất biến) theo mặc định — thread-safe, không có lỗi mutation
- Code ngắn hơn 70-80% so với class thuần
- Rõ ràng về ý định: "đây là carrier object, không có logic"

**Nhược điểm:**
- Không thể kế thừa từ class khác (chỉ có thể implement interface)
- Không thể có field có thể thay đổi — đôi khi bất tiện với builder pattern
- Một số thư viện cũ (Jackson trước 2.12) cần cấu hình thêm để deserialize

**Khi nào dùng:** DTOs, Value Objects, response wrappers — bất kỳ thứ gì chỉ cần lưu data.

---

### 1.2 Virtual Threads (Project Loom)

```yaml
# application.yml
spring:
  threads:
    virtual:
      enabled: true
```

```java
// Kafka consumer sẽ dùng Virtual Thread ở Session 6
@KafkaListener(topics = "task-events")
public void consume(TaskEvent event) {
    // Chạy trên Virtual Thread thay vì Platform Thread
    notificationService.create(event);
}
```

**Cơ chế hoạt động:**

```
Platform Thread (OS Thread) — nặng, giới hạn ~1000/JVM
       ↓
Virtual Thread — nhẹ, có thể có hàng triệu/JVM
       ↓ mount/unmount tự động
Carrier Thread (Platform Thread)
```

Khi Virtual Thread bị block I/O (chờ DB, HTTP...), JVM tự động **unmount** nó khỏi carrier thread, cho phép carrier thread xử lý việc khác. Khi I/O xong, Virtual Thread được mount lại và tiếp tục.

**Ưu điểm:**
- Throughput cực cao với workload I/O-bound (API calls, DB queries)
- Code vẫn viết theo kiểu blocking thông thường — dễ đọc hơn reactive
- Không cần WebFlux / Project Reactor

**Nhược điểm:**
- Không giúp CPU-bound tasks (tính toán nặng)
- Một số thư viện cũ dùng `synchronized` block có thể gây **pinning** (Virtual Thread bị kẹt)
- Cần Java 21+

---

### 1.3 Pattern Matching

```java
// Dùng trong GlobalExceptionHandler
ex.getBindingResult().getAllErrors().forEach(error -> {
    // Pattern matching for instanceof — không cần cast thủ công
    String fieldName = error instanceof FieldError fe
        ? fe.getField()      // fe đã được cast tự động
        : error.getObjectName();
    errors.put(fieldName, error.getDefaultMessage());
});
```

**Ưu điểm:** Loại bỏ explicit cast, code an toàn hơn và ngắn hơn.

---

## 2. Spring Boot 3.3 & IoC / DI

### 2.1 Inversion of Control (IoC)

**Khái niệm:** Thay vì code tạo dependency của mình (`new UserRepository()`), Spring Container tạo và quản lý tất cả objects (beans). Code chỉ "xin" dependency.

```java
// Không dùng IoC — tightly coupled
public class ProjectService {
    private UserRepository userRepo = new UserRepository(); // BAD
}

// Dùng IoC — loosely coupled
@Service
@RequiredArgsConstructor  // Lombok tự tạo constructor injection
public class ProjectService {
    private final UserRepository userRepository; // Spring inject vào
    private final ProjectRepository projectRepository;
}
```

### 2.2 Dependency Injection — Constructor vs Field vs Setter

```java
// ❌ Field Injection — không nên dùng
@Service
public class ProjectService {
    @Autowired private UserRepository userRepository; // khó test, null khi không có Spring
}

// ✅ Constructor Injection — Spring Boot 3 khuyến nghị
@Service
@RequiredArgsConstructor
public class ProjectService {
    private final UserRepository userRepository; // final = bất biến, dễ test
}
```

**Tại sao Constructor Injection tốt hơn:**
| Tiêu chí | Field Injection | Constructor Injection |
|----------|-----------------|-----------------------|
| Unit test | Phải dùng Reflection | Truyền mock trực tiếp vào constructor |
| Immutability | Không có | `final` field |
| Circular dependency | Phát hiện muộn (runtime) | Phát hiện sớm (startup) |
| Nullable risk | Cao | Thấp (compile-time check) |

### 2.3 Bean Lifecycle & @ConfigurationProperties

```java
// Thay vì @Value cho từng property
@Value("${jwt.secret}")
private String secret;
@Value("${jwt.expiration-ms}")
private long expirationMs;

// Dùng @ConfigurationProperties — type-safe, gom nhóm
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secret,
    long expirationMs,
    long refreshExpirationMs
) {}
```

**Ưu điểm của `@ConfigurationProperties`:**
- Type-safe — lỗi cấu hình bắt được lúc startup, không phải runtime
- Validation với `@Validated` + `@NotNull`, `@Min`...
- IDE autocomplete trong `application.yml`
- Dễ test: tạo instance trực tiếp không cần Spring context

---

## 3. JPA Auditing & BaseEntity

```java
@MappedSuperclass  // Không tạo bảng riêng, fields được kế thừa vào bảng con
@EntityListeners(AuditingEntityListener.class)  // Spring tự điền audit fields
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreatedDate
    @Column(updatable = false)  // Không cho phép thay đổi sau khi tạo
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;
}
```

**AuditorAware — nguồn "ai đang thực hiện":**

```java
@Bean
public AuditorAware<String> auditorAware() {
    return () -> {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.of("system");
        }
        return Optional.of(auth.getName()); // username của user đang login
    };
}
```

**Ưu điểm:**
- Không cần set `createdAt`, `updatedAt` thủ công ở mọi service
- Audit trail tự động — ai tạo/sửa record
- `@MappedSuperclass` không tạo bảng riêng — không overhead

**Nhược điểm:**
- Cần `@EnableJpaAuditing` ở main class
- `@CreatedBy`/`@LastModifiedBy` cần `AuditorAware` bean — phức tạp hơn một chút

### UUID vs AUTO_INCREMENT

```java
// UUID — dùng trong TaskFlow
@GeneratedValue(strategy = GenerationType.UUID)
private UUID id;
```

| Tiêu chí | UUID | AUTO_INCREMENT (Long) |
|----------|---------|-----------------------|
| Dự đoán được | Không — bảo mật hơn | Có — dễ enumerate |
| Distributed generation | Có thể tạo ở client | Chỉ DB mới biết giá trị tiếp theo |
| Index size | 16 bytes | 8 bytes |
| Performance insert | Chậm hơn (random insert vào B-tree) | Nhanh hơn (sequential) |
| URL exposure | `/tasks/550e8400-e29b-41d4-a716` | `/tasks/42` — lộ số lượng |

**Dùng UUID khi:** ID lộ ra ngoài API, cần distributed generation.
**Dùng Long khi:** Internal join table, cần insert performance tối đa.

---

## 4. Entity Relationships & Fetch Strategy

### 4.1 Các loại quan hệ trong TaskFlow

```
User ──<M:M>── Role          (user_roles join table)
User ──<1:M>── UserProject   (membership)
Project ──<1:M>── UserProject
Project ──<1:M>── Task
Task ──<1:M>── Comment
Task ──<M:M>── Label         (task_labels join table)
```

### 4.2 @ManyToOne — Lazy vs Eager

```java
// TaskFlow dùng LAZY cho tất cả @ManyToOne
@ManyToOne(fetch = FetchType.LAZY)  // ✅ Default nên là LAZY
@JoinColumn(name = "project_id", nullable = false)
private Project project;
```

```
EAGER (mặc định của @ManyToOne trước JPA 2):
  SELECT task WHERE id=1
  → Ngay lập tức: SELECT project WHERE id=task.project_id
  → Ngay lập tức: SELECT user WHERE id=project.owner_id
  → ... (N+1 problem nếu không cẩn thận)

LAZY:
  SELECT task WHERE id=1
  → project chỉ được load khi code gọi task.getProject()
  → Tiết kiệm query nếu không cần
```

**N+1 Problem:**

```java
// ❌ N+1: 1 query lấy tasks + N query lấy project cho từng task
List<Task> tasks = taskRepo.findAll(); // 1 query
tasks.forEach(t -> System.out.println(t.getProject().getName())); // N queries!

// ✅ JOIN FETCH: 1 query duy nhất
@Query("SELECT t FROM Task t JOIN FETCH t.project WHERE t.assignee.id = :id")
List<Task> findWithProject(@Param("id") UUID id);
```

### 4.3 @ManyToMany — Pitfall thường gặp

```java
// User entity
@ManyToMany(fetch = FetchType.EAGER)  // EAGER cho roles (thường nhỏ)
@JoinTable(name = "user_roles", ...)
private Set<Role> roles;
```

**Tại sao dùng `Set` thay vì `List` cho `@ManyToMany`?**

```
List + @ManyToMany → Hibernate có thể xóa toàn bộ và insert lại khi thêm/xóa 1 element
Set + @ManyToMany → Hibernate chỉ xóa/insert phần thay đổi (efficient)
```

### 4.4 Cascade & OrphanRemoval

```java
@OneToMany(mappedBy = "task",
           cascade = CascadeType.ALL,   // Mọi operation trên Task → áp dụng cho Comment
           orphanRemoval = true)         // Xóa task → xóa comments không có parent
private List<Comment> comments;
```

| Cascade Type | Ý nghĩa |
|-------------|---------|
| `PERSIST` | Save task → auto save comments mới |
| `MERGE` | Update task → auto update comments |
| `REMOVE` | Xóa task → xóa toàn bộ comments |
| `ALL` | Tất cả trên |
| `orphanRemoval` | Comment bị remove khỏi list → xóa khỏi DB |

---

## 5. Flyway Database Migration

```
src/main/resources/db/migration/
├── V1__init.sql        ← Schema toàn bộ
└── V2__seed_roles.sql  ← Data seed
```

**Quy tắc đặt tên:** `V{version}__{mô_tả}.sql`

```sql
-- V1__init.sql
CREATE TABLE users (
    id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    ...
);

-- Flyway lưu trạng thái trong bảng flyway_schema_history
-- Mỗi migration chỉ chạy một lần duy nhất
```

**Ưu điểm:**
- Version control cho database schema — biết chính xác DB đang ở version nào
- Rollback an toàn — mỗi migration là một transaction
- Reproducible — mọi environment (dev/staging/prod) đều có cùng schema
- `baseline-on-migrate: true` — an toàn với DB đang tồn tại

**Nhược điểm:**
- Migration đã chạy **không thể sửa** (checksum mismatch → Flyway reject)
- Cần tạo migration mới để alter schema đã deploy
- Team lớn cần quy trình để tránh version conflict

**So sánh với `spring.jpa.hibernate.ddl-auto`:**

| | Flyway | `ddl-auto: create` | `ddl-auto: validate` |
|--|--------|-------------------|---------------------|
| Production-safe | ✅ | ❌ (xóa data!) | ✅ |
| Version tracking | ✅ | ❌ | ❌ |
| Rollback | ✅ (với undo migration) | ❌ | ❌ |
| Dùng cho | Mọi môi trường | Chỉ dev/test | Production |

---

## 6. Repository Pattern & Custom JPQL

### 6.1 Spring Data JPA Magic

```java
public interface TaskRepository extends JpaRepository<Task, UUID> {
    // Spring tự sinh SQL từ tên method
    Page<Task> findByProjectId(UUID projectId, Pageable pageable);
    // → SELECT * FROM tasks WHERE project_id = ? LIMIT ? OFFSET ?

    long countByProjectIdAndStatus(UUID projectId, TaskStatus status);
    // → SELECT COUNT(*) FROM tasks WHERE project_id = ? AND status = ?
}
```

**Method naming convention:**
```
findBy{Field}         → WHERE field = ?
findBy{Field}And{F2}  → WHERE field = ? AND f2 = ?
countBy{Field}        → SELECT COUNT(*) WHERE field = ?
existsBy{Field}       → SELECT EXISTS(...) WHERE field = ?
deleteBy{Field}       → DELETE WHERE field = ?
findTop3By{Field}     → LIMIT 3
findBy{Field}OrderBy{F2}Desc → ORDER BY
```

### 6.2 Custom JPQL Queries

```java
// JPQL — query trên Java objects, không phải SQL tables
@Query("""
    SELECT t FROM Task t
    WHERE t.project.id = :projectId
      AND t.dueDate < :today
      AND t.status NOT IN ('DONE', 'CANCELLED')
    ORDER BY t.dueDate ASC
    """)
List<Task> findOverdueTasksByProject(@Param("projectId") UUID projectId,
                                     @Param("today") LocalDate today);
```

**JPQL vs Native SQL:**

| | JPQL | Native SQL |
|--|------|-----------|
| Syntax | Java object-based | SQL table-based |
| Portability | Database-agnostic | DB-specific |
| Type safety | Partial (còn string) | Không |
| Complex queries | Đôi khi awkward | Linh hoạt hơn |
| Dùng khi | 80% cases | Complex aggregation, window functions |

```java
// Native SQL khi cần (ví dụ window functions)
@Query(value = """
    SELECT *, ROW_NUMBER() OVER (PARTITION BY project_id ORDER BY created_at) as rn
    FROM tasks WHERE project_id = :id
    """, nativeQuery = true)
List<Object[]> findTasksWithRowNumber(@Param("id") UUID id);
```

### 6.3 Dynamic Query với Parameters

```java
@Query("""
    SELECT t FROM Task t
    WHERE t.project.id = :projectId
      AND (:status IS NULL OR t.status = :status)
      AND (:priority IS NULL OR t.priority = :priority)
      AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
    """)
Page<Task> findByProjectWithFilters(
    @Param("projectId") UUID projectId,
    @Param("status") TaskStatus status,       // null = bỏ qua filter
    @Param("priority") Priority priority,     // null = bỏ qua filter
    @Param("assigneeId") UUID assigneeId,
    Pageable pageable);
```

**Nhược điểm của cách này:** JPQL không thể optimize tốt khi tất cả params là null. Với filter phức tạp, nên dùng **JPA Criteria API** hoặc **QueryDSL**.

---

## 7. Service Layer & Transactions

### 7.1 @Transactional

```java
@Service
@Transactional(readOnly = true)  // Default cho cả class — optimize read queries
public class ProjectService {

    // Ghi đè: method này cần write transaction
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, UUID ownerId) {
        Project project = projectRepository.save(project);
        userProjectRepository.save(membership); // Cùng transaction — rollback nếu lỗi
        return ProjectResponse.from(project);
    }

    // Kế thừa readOnly từ class
    public ProjectResponse getProject(UUID id, UUID userId) {
        return ProjectResponse.from(findProjectOrThrow(id));
    }
}
```

**`readOnly = true` mang lại gì?**
- Hibernate **skip dirty checking** (không scan xem entity có thay đổi không)
- PostgreSQL tối ưu hóa read-only transactions
- Một số connection pool routing reads sang replica

**Transaction Propagation:**

```
REQUIRED (default):
  serviceA.method() → Tạo transaction mới
    └─ serviceB.method() → Tham gia transaction của A (không tạo mới)
       → Nếu B throw exception → rollback cả A và B

REQUIRES_NEW:
  serviceA.method() → Transaction 1
    └─ serviceB.method() → Transaction 2 (độc lập)
       → Nếu B throw exception → rollback chỉ B, A vẫn OK

Dùng trong TaskFlow: Audit logging nên dùng REQUIRES_NEW
→ Dù business logic fail, audit log vẫn được ghi
```

### 7.2 Business Logic vs Controller

**Nguyên tắc:** Controller chỉ handle HTTP, Service chứa business logic.

```java
// ✅ Controller — chỉ parse request, gọi service, format response
@PostMapping("/projects/{projectId}/tasks")
public ResponseEntity<TaskResponse> createTask(
    @PathVariable UUID projectId,
    @Valid @RequestBody CreateTaskRequest request,
    @AuthenticationPrincipal UserDetails user) {

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(taskService.createTask(projectId, request, extractUserId(user)));
}

// ✅ Service — business logic, authorization, orchestration
public TaskResponse createTask(UUID projectId, CreateTaskRequest req, UUID reporterId) {
    assertMember(projectId, reporterId);     // ← authorization
    Set<Label> labels = resolveLabels(...);  // ← business logic
    Task task = Task.builder()...build();
    return TaskResponse.from(taskRepository.save(task));
}
```

---

## 8. DTO Pattern với Java Records

### 8.1 Tại sao không trả Entity trực tiếp?

```java
// ❌ Trả Entity trực tiếp — nhiều vấn đề
@GetMapping("/tasks/{id}")
public Task getTask(@PathVariable UUID id) {
    return taskRepository.findById(id).orElseThrow();
    // Vấn đề 1: Lộ password, internal fields
    // Vấn đề 2: Lazy loading → LazyInitializationException ngoài transaction
    // Vấn đề 3: Jackson serialize vòng lặp (Task → Project → Task)
    // Vấn đề 4: API contract gắn chặt với DB schema
}

// ✅ Dùng DTO
@GetMapping("/tasks/{id}")
public TaskResponse getTask(@PathVariable UUID id) {
    return TaskResponse.from(taskRepository.findById(id).orElseThrow());
}
```

### 8.2 Static Factory Method Pattern

```java
public record TaskResponse(UUID id, String title, TaskStatus status, ...) {

    // Static factory — centralize mapping logic
    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getStatus(),
            task.getAssignee() != null ? UserSummaryResponse.from(task.getAssignee()) : null,
            ...
        );
    }
}
```

**Ưu điểm:** Mapping logic ở một chỗ — dễ maintain, dễ test.

**Nhược điểm:** Với project lớn, nên dùng **MapStruct** thay vì viết `from()` thủ công.

### 8.3 PageResponse Wrapper

```java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(), page.getNumber(), page.getSize(),
            page.getTotalElements(), page.getTotalPages(), page.isLast()
        );
    }
}
```

**Tại sao không trả `Page<T>` trực tiếp?** Spring's `Page` object chứa nhiều metadata không cần thiết cho client và khó control format. Wrapper cho phép customize response shape.

---

## 9. Spring Security 6 — STATELESS Architecture

### 9.1 So sánh STATEFUL vs STATELESS

```
STATEFUL (Session-based):
  Client → Login → Server tạo Session (lưu trong memory/Redis)
  Client → Gửi SessionID trong Cookie mỗi request
  Server → Lookup SessionID → Xác thực

  Vấn đề với microservices/horizontal scaling:
  Server 1 có session, Server 2 không có → cần sticky session hoặc shared store

STATELESS (JWT-based) — TaskFlow dùng cái này:
  Client → Login → Server trả JWT
  Client → Gửi JWT trong Authorization header mỗi request
  Server → Verify JWT signature → Không cần lookup DB
  
  ✅ Scale out dễ dàng — mọi server đều verify được JWT
  ✅ Stateless — server không lưu gì
  ❌ Không thể revoke token trước khi expire (giải quyết bằng blacklist hoặc refresh rotation)
```

### 9.2 SecurityFilterChain

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        // CSRF không cần với STATELESS (không có session/cookie để CSRF exploit)

        .sessionManagement(sm ->
            sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Không tạo/dùng HttpSession

        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()  // Public
            .anyRequest().authenticated()                  // Còn lại cần auth
        )

        .addFilterBefore(jwtAuthFilter,
            UsernamePasswordAuthenticationFilter.class);
        // JWT filter chạy TRƯỚC filter mặc định của Spring

    return http.build();
}
```

**Filter chain order:**

```
Request → [ChannelProcessingFilter]
        → [CorsFilter]
        → [JwtAuthFilter]          ← Custom filter của chúng ta
        → [UsernamePasswordAuthFilter]  ← Filter mặc định (không dùng trong STATELESS)
        → [ExceptionTranslationFilter]
        → [FilterSecurityInterceptor]
        → Controller
```

### 9.3 DaoAuthenticationProvider

```java
@Bean
public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);  // Load user từ DB
    provider.setPasswordEncoder(passwordEncoder());       // So sánh hash
    return provider;
}
```

```
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken("alice", "password123"))
  ↓
DaoAuthenticationProvider.authenticate()
  ↓
userDetailsService.loadUserByUsername("alice")  // Load từ DB
  ↓
passwordEncoder.matches("password123", storedHash)  // BCrypt compare
  ↓
Nếu match → Authentication object
Nếu không → BadCredentialsException
```

---

## 10. JWT Authentication (JJWT 0.12)

### 10.1 Cấu trúc JWT

```
eyJhbGciOiJIUzI1NiJ9  .  eyJ1aWQiOiI1NTBlODQwMCIsInN1YiI6ImFsaWNlIn0  .  SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
      Header                              Payload                                    Signature
  (algorithm)                    (claims — không encrypt)                   (HMAC-SHA256 của header+payload)
```

```json
// Payload decode được (base64)
{
  "uid": "550e8400-e29b-41d4-a716",
  "sub": "alice",          // username
  "type": "access",
  "iat": 1715337600,       // issued at
  "exp": 1715424000        // expiration
}
```

**Quan trọng:** JWT payload **không được encrypt**, chỉ được sign. Đừng lưu thông tin nhạy cảm (password, credit card) trong JWT.

### 10.2 JJWT 0.12 API

```java
// Tạo token — JJWT 0.12 API mới
String token = Jwts.builder()
    .claims(Map.of("uid", userId.toString(), "type", "access"))
    .subject(username)
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + expirationMs))
    .signWith(signingKey())   // Tự chọn algorithm từ key length
    .compact();

// Parse token
Claims claims = Jwts.parser()
    .verifyWith(signingKey())   // Verify signature
    .build()
    .parseSignedClaims(token)   // Parse + verify expiration tự động
    .getPayload();
```

**JJWT 0.11 vs 0.12 — Breaking changes:**

| JJWT 0.11 | JJWT 0.12 |
|-----------|-----------|
| `Jwts.parserBuilder()` | `Jwts.parser()` |
| `.setSigningKey()` | `.verifyWith()` |
| `.parseClaimsJws()` | `.parseSignedClaims()` |
| `.setClaims()` | `.claims()` |

### 10.3 Access Token vs Refresh Token

```
Access Token:
  - TTL ngắn (24 giờ trong TaskFlow)
  - Dùng cho mọi API request
  - Nếu bị đánh cắp → thiệt hại tối đa 24 giờ

Refresh Token:
  - TTL dài (7 ngày)
  - Chỉ dùng cho /auth/refresh
  - Dùng để xin Access Token mới khi expired
  - TaskFlow claim: { "type": "refresh" }
  - JwtAuthFilter chặn Refresh Token dùng làm Access Token

Flow:
  Login → Access Token (24h) + Refresh Token (7d)
  Access Token hết hạn → Dùng Refresh Token → Access Token mới + Refresh Token mới (rotation)
  Refresh Token hết hạn → Phải login lại
```

**Rotation strategy:** Mỗi lần refresh, cả 2 token đều được tạo mới — giảm window tấn công.

### 10.4 JwtAuthFilter

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {

    String token = extractToken(request);  // Lấy từ "Authorization: Bearer ..."

    if (token != null
        && jwtUtil.isTokenValid(token)      // Verify signature + expiration
        && !jwtUtil.isRefreshToken(token)   // Chặn refresh token
        && SecurityContextHolder.getContext().getAuthentication() == null  // Chưa auth
    ) {
        String username = jwtUtil.extractUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Set authentication vào SecurityContext
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    filterChain.doFilter(request, response);  // Tiếp tục chain dù có auth hay không
}
```

**Tại sao check `getAuthentication() == null`?** Tránh override authentication đã được set bởi filter trước trong cùng request.

---

## 11. Bean Validation & Custom Constraints

### 11.1 Standard Constraints

```java
public record RegisterRequest(
    @NotBlank(message = "Username is required")          // Không null, không rỗng, không blank
    @Size(min = 3, max = 50)                             // Length check
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")                // Regex
    @UniqueUsername                                       // Custom constraint
    String username,

    @NotBlank @Email                                     // Email format check
    @UniqueEmail
    String email,

    @NotBlank
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    String password
) {}
```

**Validation được kích hoạt ở Controller bằng `@Valid`:**

```java
@PostMapping("/register")
public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    // Nếu validation fail → MethodArgumentNotValidException tự động
    // GlobalExceptionHandler bắt → trả 422 với danh sách lỗi
}
```

### 11.2 Custom ConstraintValidator

```java
// 1. Định nghĩa annotation
@Constraint(validatedBy = UniqueEmailValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueEmail {
    String message() default "Email is already registered";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// 2. Implement logic
@Component  // Spring bean — có thể inject repository
@RequiredArgsConstructor
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userRepository;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) return true; // @NotBlank handle riêng
        return !userRepository.existsByEmail(email);       // DB lookup
    }
}
```

**Ưu điểm:**
- Reusable — dùng `@UniqueEmail` ở bất kỳ DTO nào
- Declarative — business rule rõ ràng trong annotation
- Tự động trigger khi có `@Valid`

**Nhược điểm:**
- Mỗi field có annotation này → 1 DB query → N annotations = N queries
- Có race condition: 2 user register cùng lúc → cả 2 pass validation → 1 cái fail unique constraint ở DB → cần xử lý `DataIntegrityViolationException`

---

## 12. Exception Handling — RFC 7807 Problem Details

### 12.1 RFC 7807 Format

**Trước:** Mỗi API trả error format khác nhau → client phải handle nhiều format

**RFC 7807 Problem Details:** Chuẩn hóa format lỗi HTTP API

```json
{
  "type": "https://taskflow.api/errors/not-found",   // URI xác định loại lỗi
  "title": "Resource Not Found",                      // Mô tả ngắn
  "status": 404,                                      // HTTP status
  "detail": "Task not found with id: 550e8400...",   // Mô tả chi tiết
  "instance": "/api/tasks/550e8400",                 // Request URI (tự điền)

  // Extension fields (tùy ý)
  "errors": {
    "email": "Email is already registered",
    "username": "Username is already taken"
  }
}
```

### 12.2 Exception Hierarchy

```
RuntimeException
├── ResourceNotFoundException   → 404
├── AccessDeniedException       → 403
└── BusinessException           → 400 (hoặc custom status)

Spring built-in:
├── MethodArgumentNotValidException → 422 (validation)
├── BadCredentialsException         → 401
└── AccessDeniedException           → 403
```

### 12.3 GlobalExceptionHandler

```java
@RestControllerAdvice  // Kết hợp @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://taskflow.api/errors/not-found"));
        return problem;  // Spring 6 tự serialize sang RFC 7807 format
    }
}
```

**`ResponseEntityExceptionHandler`:** Base class của Spring xử lý các built-in exception (`MethodArgumentNotValidException`, `HttpMessageNotReadableException`...). Override để customize format.

**Ưu điểm:**
- Một chỗ duy nhất handle mọi exception
- Consistent format trả về cho client
- Không cần try-catch trong controller/service

---

## 13. Method-level Security — @PreAuthorize

### 13.1 Dùng SpEL Expression

```java
// Controller — khai báo security requirement
@PutMapping("/{projectId}")
@PreAuthorize("@projectSecurity.isOwner(#projectId, authentication)")
public ResponseEntity<ProjectResponse> updateProject(
    @PathVariable UUID projectId, ...) { ... }

// projectSecurity bean
@Component("projectSecurity")
public class ProjectSecurity {
    public boolean isOwner(UUID projectId, Authentication auth) {
        UUID userId = UUID.fromString(auth.getName()); // principal = userId string
        return projectRepository.existsByIdAndOwnerId(projectId, userId);
    }
}
```

**Cú pháp SpEL:**
```
@projectSecurity.isOwner(#projectId, authentication)
│                │         │           └── Spring Security Authentication object
│                │         └── #tên_param → lấy giá trị từ method parameter
│                └── method name của bean
└── @ → tham chiếu Spring bean theo tên
```

### 13.2 Các annotation bảo mật

```java
@PreAuthorize("...")    // Kiểm tra TRƯỚC khi method chạy → method không chạy nếu fail
@PostAuthorize("...")   // Kiểm tra SAU khi method chạy → có thể filter result
@PreFilter("...")       // Filter collection argument trước khi method chạy
@PostFilter("...")      // Filter collection result sau khi method chạy

// Ví dụ @PostFilter
@PostFilter("filterObject.ownerId == authentication.name")
List<Project> getProjects() { ... }
// Spring tự lọc list, chỉ giữ project có ownerId = userId hiện tại
```

**Tại sao dùng `@PreAuthorize` thay vì check trong Service?**

```java
// ❌ Check trong service — phải nhớ check ở mọi nơi
public ProjectResponse updateProject(UUID projectId, ...) {
    if (!isOwner(projectId, userId)) throw new AccessDeniedException();
    // logic...
}

// ✅ @PreAuthorize — declarative, không thể "quên"
// Security requirement visible ngay tại API definition
@PreAuthorize("@projectSecurity.isOwner(#projectId, authentication)")
public ResponseEntity<ProjectResponse> updateProject(...) {
    // logic chỉ chạy nếu đã pass security check
}
```

---

## 14. CORS Configuration

### 14.1 Tại sao cần CORS?

```
Browser Same-Origin Policy:
  JavaScript ở https://frontend.com KHÔNG được gọi https://api.taskflow.com
  → Cross-Origin request bị block bởi browser

CORS (Cross-Origin Resource Sharing):
  Server nói với browser: "Cho phép origin X gọi tôi"
  → Browser thực hiện Preflight request (OPTIONS) trước
  → Server trả Access-Control headers
  → Browser cho phép actual request
```

### 14.2 TaskFlow CORS Config

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));         // Cho phép mọi origin (dev)
    config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    config.setAllowedHeaders(List.of("*"));                // Cho phép mọi header
    config.setAllowCredentials(true);                      // Cho phép cookies/auth header
    config.setMaxAge(3600L);                               // Cache preflight 1 giờ

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

**Production nên restrict:**
```java
config.setAllowedOriginPatterns(List.of(
    "https://app.taskflow.com",
    "https://admin.taskflow.com"
));
```

**`allowedOriginPatterns` vs `allowedOrigins`:**
- `allowedOrigins("*")` + `allowCredentials(true)` → **lỗi** (Spring throw exception)
- `allowedOriginPatterns("*")` + `allowCredentials(true)` → **hoạt động** (wildcard pattern)

---

## 15. Pagination với Spring Data

### 15.1 Pageable Interface

```java
// Controller nhận Pageable từ request params
@GetMapping("/projects/{projectId}/tasks")
public ResponseEntity<PageResponse<TaskResponse>> getTasks(
    @PathVariable UUID projectId,
    @PageableDefault(size = 20, sort = "updatedAt") Pageable pageable,
    ...) { }

// Request:
// GET /api/projects/{id}/tasks?page=0&size=10&sort=createdAt,desc
//                              └─────── Pageable tự parse ────────┘
```

### 15.2 Query tự động với Pageable

```java
// Repository
Page<Task> findByProjectId(UUID projectId, Pageable pageable);
// → SELECT * FROM tasks WHERE project_id = ? ORDER BY updated_at DESC LIMIT 20 OFFSET 0
// → SELECT COUNT(*) FROM tasks WHERE project_id = ?  ← count query tự động

// Kết quả
Page<Task> page = taskRepo.findByProjectId(id, pageable);
page.getContent();       // List<Task> cho page hiện tại
page.getTotalElements(); // Tổng số records
page.getTotalPages();    // Tổng số pages
page.isLast();           // Page cuối chưa?
```

### 15.3 Tại sao dùng PageResponse wrapper?

```java
// Spring Page<T> serialize thành JSON rất dài và có nhiều field không cần
{
  "content": [...],
  "pageable": { "sort": {...}, "offset": 0, "pageNumber": 0, ... },
  "last": false,
  "totalPages": 5,
  "totalElements": 100,
  "size": 20,
  "number": 0,
  "sort": {...},
  "first": true,
  "numberOfElements": 20,
  "empty": false
}

// PageResponse wrapper — chỉ giữ những gì client cần
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "last": false
}
```

---

## 16. Redis Cache — @Cacheable & RedisCacheManager

### 16.1 Tại sao cần Cache?

Mỗi request `GET /api/tasks/{id}` đều query PostgreSQL. Nếu có 1000 user cùng xem task nổi tiếng → 1000 DB query giống nhau trong 1 giây. Cache giải quyết vấn đề này bằng cách lưu kết quả vào bộ nhớ nhanh hơn.

```
Không cache:
  Client → API → PostgreSQL → trả về (mỗi request ~10ms DB)

Có Redis cache:
  Client → API → Redis HIT → trả về ngay (< 1ms)
                └→ Redis MISS → PostgreSQL → lưu vào Redis → trả về
```

### 16.2 RedisCacheManager với TTL riêng cho từng cache

```java
@Bean
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration defaultConfig = defaultCacheConfig();

    Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
        CACHE_PROJECTS, defaultConfig.entryTtl(Duration.ofMinutes(10)), // project ít thay đổi
        CACHE_TASKS,    defaultConfig.entryTtl(Duration.ofMinutes(5)),  // task thay đổi thường hơn
        CACHE_USERS,    defaultConfig.entryTtl(Duration.ofMinutes(30))  // user hiếm khi thay đổi
    );

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigs)  // override TTL per cache
        .build();
}
```

**Tại sao TTL khác nhau?**
- `projects` cache: project bị update ít — TTL 10 phút OK
- `tasks` cache: task có thể thay đổi status liên tục — TTL 5 phút an toàn hơn
- Nếu TTL quá dài → stale data; quá ngắn → cache không có giá trị

### 16.3 Các annotation Cache

```java
// Lưu kết quả vào cache với key = projectId
@Cacheable(value = "projects", key = "#projectId")
public ProjectResponse getProject(UUID projectId, UUID userId) { ... }
// Lần đầu: DB query → lưu vào Redis key "projects::projectId"
// Các lần sau: đọc từ Redis, KHÔNG vào method body

// Xóa cache khi update (cache đã stale)
@CacheEvict(value = "projects", key = "#projectId")
public ProjectResponse updateProject(UUID projectId, ...) { ... }

// Xóa nhiều cache cùng lúc
@Caching(evict = {
    @CacheEvict(value = "projects", key = "#projectId"),
    @CacheEvict(value = "tasks", allEntries = true) // xóa toàn bộ tasks cache
})
public void deleteProject(UUID projectId, ...) { ... }
```

### 16.4 Serialization trong Redis

```java
// Vấn đề: Redis lưu byte[], cần serialize Java object → bytes và ngược lại
// TaskFlow dùng JSON serialization với type info:

ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());         // Cho LocalDate, LocalDateTime
mapper.activateDefaultTyping(..., JsonTypeInfo.As.PROPERTY);
// → Lưu type info vào JSON để deserialize đúng class

// Redis key: "tasks::550e8400-e29b-41d4-a716"
// Redis value: {"@class":"com.taskflow.dto.response.TaskResponse","id":"550e...",...)
```

**Ưu điểm:**
- Đọc từ Redis nhanh hơn DB 10-100x với data đã cache
- Giảm tải cho PostgreSQL đáng kể trong high-traffic

**Nhược điểm:**
- **Cache Invalidation** — vấn đề khó nhất trong cache: khi nào xóa cache?
  - `@CacheEvict` xóa sau write → trong khoảng thời gian ngắn, có thể serve stale data
- **Cache Stampede** — nhiều request cùng miss cache → đổ vào DB cùng lúc. Giải quyết bằng Lua script lock hoặc probabilistic early expiration
- Tăng độ phức tạp của hệ thống — thêm một điểm failure (Redis down)

### 16.5 @Cacheable chỉ hoạt động với Spring Proxy

```java
// ❌ Gọi nội bộ trong cùng class → cache KHÔNG hoạt động
@Service
public class ProjectService {
    public void someMethod() {
        getProject(id, userId); // Gọi trực tiếp, bypass Spring proxy → không cache
    }

    @Cacheable("projects")
    public ProjectResponse getProject(UUID id, UUID userId) { ... }
}

// ✅ Cache hoạt động khi gọi qua Spring bean
ProjectService projectService; // inject
projectService.getProject(id, userId); // → qua proxy → cache check
```

---

## 17. Spring AOP — LoggingAspect & AuditAspect

### 17.1 AOP là gì?

**Aspect-Oriented Programming** — cách tách "cross-cutting concerns" (logging, audit, security, metrics) ra khỏi business logic.

```
Không AOP — logic bị lẫn với infrastructure:
  public TaskResponse createTask(...) {
      log.info("Creating task...");          // logging
      long start = System.currentTimeMillis();
      auditLog.save(...);                    // audit
      // business logic
      long time = System.currentTimeMillis() - start;
      log.info("Done in {}ms", time);        // logging
  }

Với AOP — business logic thuần túy:
  public TaskResponse createTask(...) {
      // chỉ có business logic
      // LoggingAspect tự động wrap quanh method này
  }
```

### 17.2 Các khái niệm AOP

```
Aspect   — Class chứa cross-cutting logic (LoggingAspect, AuditAspect)
Advice   — Method trong Aspect được thực thi (@Before, @After, @Around)
Pointcut — Biểu thức xác định method nào bị intercept
JoinPoint— Thời điểm cụ thể bị intercept (method call)
```

```java
// Pointcut: tất cả public method trong package service
@Pointcut("execution(public * com.taskflow.service.*.*(..))")
public void serviceMethods() {}

// Advice: @Around = wrap quanh method (chạy trước + sau)
@Around("serviceMethods()")
public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
    long start = System.currentTimeMillis();
    try {
        Object result = joinPoint.proceed(); // Chạy method gốc
        log.debug("← {}ms", System.currentTimeMillis() - start);
        return result;
    } catch (Throwable ex) {
        log.warn("✕ threw {}", ex.getMessage());
        throw ex; // Re-throw để caller xử lý
    }
}
```

**4 loại Advice:**
| Annotation | Khi nào chạy | Có thể modify result? |
|------------|-------------|----------------------|
| `@Before`  | Trước method | Không |
| `@After`   | Sau method (dù exception) | Không |
| `@AfterReturning` | Sau method success | Có (binding returnValue) |
| `@Around`  | Wrap hoàn toàn | Có (có thể thay đổi result hoặc skip method) |

### 17.3 @Auditable — Custom Annotation Advice

```java
// Annotation marker — đặt trên method cần audit
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();   // "CREATE_PROJECT", "UPDATE_TASK"...
}

// Pointcut dựa trên annotation — chỉ intercept method có @Auditable
@Around("@annotation(com.taskflow.aop.Auditable)")
public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
    Object result = joinPoint.proceed(); // Business logic chạy trước

    try {
        saveAuditLog(auditable, joinPoint.getArgs());
    } catch (Exception ex) {
        log.error("Audit failed", ex); // Không để audit fail crash business
    }

    return result;
}
```

**Tại sao business logic chạy trước rồi mới audit?**
Nếu business logic fail (rollback), không nên ghi audit log. Ghi audit sau khi `proceed()` thành công đảm bảo chỉ log operation thực sự xảy ra.

### 17.4 @Async + REQUIRES_NEW cho Audit

```java
@Async           // Chạy trên thread riêng — không block response trả về
@Transactional(propagation = Propagation.REQUIRES_NEW)
// REQUIRES_NEW = tạo transaction mới, độc lập với business transaction
// Đảm bảo audit_log được commit dù business transaction có rollback
protected void saveAuditLog(Auditable auditable, Object[] args) {
    auditLogRepository.save(AuditLog.builder()...build());
}
```

```
REQUIRED (default):
  Business Transaction ──────────────────────────────
    └─ Audit (cùng transaction) ──
  Nếu business rollback → audit cũng rollback → MẤT audit log!

REQUIRES_NEW:
  Business Transaction ──────────────────────────────
  Audit Transaction (mới) ──  ← commit độc lập
  Nếu business rollback → audit vẫn được commit ✅
```

**Ưu điểm AOP:**
- Code sạch — không lẫn lộn logging/audit với business logic
- Reusable — `@Auditable` dùng ở bất kỳ method nào
- Không xâm phạm code gốc — thêm/xóa cross-cutting concern không sửa business code

**Nhược điểm AOP:**
- Khó debug — execution flow không tuyến tính
- Performance overhead nhỏ của proxy (thường < 1ms, có thể bỏ qua)
- `@Around` quên gọi `joinPoint.proceed()` → method không bao giờ chạy (subtle bug)
- AOP chỉ hoạt động với Spring-managed beans, và chỉ qua Spring proxy (giống cache)

---

## 18. Apache Kafka — Event-Driven Architecture

### 18.1 Tại sao cần Kafka?

**Vấn đề với synchronous flow:**
```
Client → TaskService.updateTask() → gửi email notification
                                  → gửi push notification
                                  → cập nhật activity feed
                                  → ...

Response time = business logic + tất cả side effects
Nếu email server chậm → cả request bị chậm
Nếu notification service lỗi → update task cũng fail
```

**Giải pháp với Kafka:**
```
Client → TaskService.updateTask() → publish event → trả response ngay
                    ↓
              Kafka Topic "task-assigned"
                    ↓
         NotificationConsumer → tạo Notification (async)
         EmailConsumer        → gửi email (async, service khác)
         ActivityConsumer     → cập nhật feed (async, service khác)
```

**Decoupling** — TaskService không biết ai đang lắng nghe event. Thêm consumer mới không cần sửa producer.

### 18.2 Kafka Concepts

```
Producer     → Gửi message vào Topic
Topic        → Kênh phân loại message ("task-assigned", "task-completed")
Partition    → Topic được chia thành nhiều partition → parallel processing
Offset       → Vị trí của message trong partition (tăng dần, không thay đổi)
Consumer     → Đọc message từ Topic
ConsumerGroup→ Nhiều consumer cùng group → mỗi partition chỉ được đọc bởi 1 consumer
Broker       → Kafka server lưu message
```

```
Topic "task-assigned" với 3 partitions:
  Partition 0: [msg0, msg3, msg6, ...]
  Partition 1: [msg1, msg4, msg7, ...]
  Partition 2: [msg2, msg5, msg8, ...]

ConsumerGroup "taskflow-group" với 3 consumers:
  Consumer A → đọc Partition 0
  Consumer B → đọc Partition 1
  Consumer C → đọc Partition 2
  → 3x throughput so với 1 consumer
```

### 18.3 TaskFlow Kafka Design

```java
// Partition key = taskId → các event của cùng một task luôn vào cùng partition
// → đảm bảo thứ tự xử lý cho một task cụ thể
kafkaTemplate.send(topic, taskId.toString(), event);

// Events là Java Records — immutable, serializable
public record TaskAssignedEvent(
    UUID taskId,
    String taskTitle,
    UUID projectId,
    UUID assigneeId,
    UUID assignedById,
    LocalDateTime occurredAt  // Thời điểm event xảy ra (không phải lúc consume)
) {}
```

**Event Design Best Practices trong TaskFlow:**
- **Self-contained**: Event chứa đủ info để xử lý — không cần query thêm DB
- **Immutable**: Records không thể thay đổi sau khi tạo
- `occurredAt`: Phân biệt khi nào event xảy ra vs khi nào được xử lý

### 18.4 Producer — Fire and Forget vs Confirmed

```java
// TaskFlow dùng async với callback (fire-and-forget với error logging)
CompletableFuture<SendResult<String, Object>> future =
    kafkaTemplate.send(topic, key, payload);

future.whenComplete((result, ex) -> {
    if (ex != null) {
        log.error("Failed to send event: {}", ex.getMessage());
        // Production: retry, dead letter queue, alert
    } else {
        log.debug("Sent offset={}", result.getRecordMetadata().offset());
    }
});
```

**3 mức độ delivery guarantee:**
| Mode | Config | Đảm bảo | Performance |
|------|--------|---------|-------------|
| Fire & Forget | `acks=0` | Không đảm bảo | Nhanh nhất |
| Leader Ack | `acks=1` | Leader nhận | Trung bình |
| All Acks | `acks=all` | Tất cả replica | Chậm nhất, an toàn nhất |

### 18.5 Consumer với Virtual Threads

```java
@KafkaListener(topics = KafkaConfig.TOPIC_TASK_ASSIGNED,
               groupId = "${spring.kafka.consumer.group-id}")
@Transactional
public void onTaskAssigned(@Payload TaskAssignedEvent event) {
    // Method này chạy trên Virtual Thread (spring.threads.virtual.enabled=true)
    // I/O-bound: đọc DB (userRepository, taskRepository), ghi DB (notificationRepository)
    // Virtual Thread xử lý tốt I/O blocking
    userRepository.findById(event.assigneeId()).ifPresent(assignee -> {
        notificationRepository.save(Notification.builder()...build());
    });
}
```

**Tại sao Consumer phù hợp với Virtual Threads?**
Consumer chủ yếu làm I/O: đọc message từ Kafka → query DB → ghi DB. Mỗi bước đều là I/O blocking. Virtual Thread sẽ unmount khi block I/O, cho phép xử lý nhiều message song song mà không cần nhiều platform threads.

### 18.6 At-Least-Once Delivery & Idempotency

```
Kafka đảm bảo At-Least-Once (ít nhất một lần):
- Message KHÔNG bao giờ bị mất
- Nhưng có thể được deliver nhiều lần (khi consumer crash trước khi commit offset)

Ví dụ:
  Consumer đọc message TaskAssigned → ghi Notification → crash TRƯỚC khi commit offset
  → Kafka replay message → Consumer đọc lại → ghi Notification lần 2 → duplicate!

Giải pháp (chưa implement trong MVP):
  Option 1: Idempotent consumer — check trước khi insert
    if (!notificationRepo.existsByTaskIdAndUserIdAndType(...)) {
        notificationRepo.save(notification);
    }
  Option 2: Kafka transaction + Exactly-Once Semantics (EOS)
```

**Ưu điểm Kafka:**
- Horizontal scale consumer dễ dàng — thêm instance, Kafka tự rebalance partition
- Message persistence — lưu trữ lâu dài (configurable), replay được
- Decoupling hoàn toàn giữa producer và consumer

**Nhược điểm Kafka:**
- Operational complexity cao — cần Zookeeper/KRaft, monitoring, tuning
- Latency cao hơn direct call (thường 5-50ms overhead)
- At-Least-Once → cần thiết kế consumer idempotent
- Debugging distributed event flow khó hơn synchronous call

---

## 19. Testing — JUnit 5 + Mockito + Testcontainers

### 19.1 Unit Tests với Mockito

Unit test kiểm tra logic của một class/method **trong cô lập** — tất cả dependencies bị mock.

```java
@ExtendWith(MockitoExtension.class)   // tích hợp Mockito với JUnit 5
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock ProjectRepository projectRepository;
    @Mock NotificationProducer notificationProducer;
    @Mock Counter taskCreatedCounter;
    @Mock Counter taskCompletedCounter;

    @InjectMocks TaskService taskService;   // inject tất cả @Mock vào TaskService

    @Nested
    class CreateTask {
        @Test
        void shouldPublishKafkaEventWhenAssigneeSet() {
            // GIVEN — chuẩn bị dữ liệu
            UUID projectId = UUID.randomUUID();
            UUID reporterId = UUID.randomUUID();
            UUID assigneeId = UUID.randomUUID();

            given(projectRepository.isMember(projectId, reporterId)).willReturn(true);
            given(projectRepository.findByIdWithOwner(projectId)).willReturn(Optional.of(project));
            given(userRepository.findById(reporterId)).willReturn(Optional.of(reporter));
            given(userRepository.findById(assigneeId)).willReturn(Optional.of(assignee));
            given(taskRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // WHEN — thực thi
            taskService.createTask(projectId, request, reporterId);

            // THEN — kiểm tra kết quả
            ArgumentCaptor<TaskAssignedEvent> captor =
                ArgumentCaptor.forClass(TaskAssignedEvent.class);
            verify(notificationProducer).sendTaskAssigned(captor.capture());

            TaskAssignedEvent event = captor.getValue();
            assertThat(event.assigneeId()).isEqualTo(assigneeId);
            assertThat(event.assignedById()).isEqualTo(reporterId);
        }
    }
}
```

**Giải thích pattern:**

| Thành phần | Vai trò |
|------------|---------|
| `@ExtendWith(MockitoExtension.class)` | Thay thế `@RunWith(MockitoJUnitRunner)` của JUnit 4 |
| `@Mock` | Tạo mock object — tất cả method trả về null/0/false theo mặc định |
| `@InjectMocks` | Mockito inject tất cả `@Mock` vào constructor/field của class cần test |
| `given(...).willReturn(...)` | BDD style (Behavior-Driven Development) — dễ đọc hơn `when/thenReturn` |
| `ArgumentCaptor` | Capture đối số được truyền vào mock để kiểm tra chi tiết |
| `@Nested` | Nhóm test theo use case — test tree dễ đọc hơn |

**Ưu điểm Unit Test:**
- Chạy cực nhanh (milliseconds) — không cần DB, Redis, Kafka
- Kiểm tra logic thuần túy, không bị ảnh hưởng bởi infrastructure
- Dễ test edge cases (null, exception, race condition)

**Nhược điểm:**
- Không phát hiện lỗi tích hợp (query SQL sai, serialization lỗi)
- Mock có thể che giấu behavior thật của dependency

### 19.2 Testcontainers — Integration Tests với Container Thật

Testcontainers tự động start Docker container (PostgreSQL, Redis, Kafka) trong quá trình test, đảm bảo behavior giống production.

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // static = shared giữa tất cả test method trong class hierarchy
    // Tránh start/stop container mỗi test → tiết kiệm thời gian
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("taskflow_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> REDIS =
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Container
    static final KafkaContainer KAFKA =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Override application.yml bằng địa chỉ container động
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}
```

**Tại sao dùng `static` container?**

```
Không static:              static (Shared):
Test 1: start → test → stop    Container start 1 lần
Test 2: start → test → stop  → Test 1 chạy
Test 3: start → test → stop    Test 2 chạy
                               Test 3 chạy
Total: 3 × (start_time)        Container stop 1 lần
                               Total: 1 × (start_time) — nhanh hơn ~3x
```

**@DynamicPropertySource** giải quyết vấn đề port ngẫu nhiên của container. Port của PostgreSQL container không cố định — mỗi lần start sẽ khác nhau. `@DynamicPropertySource` inject URL/port thật vào Spring context sau khi container đã start.

### 19.3 Integration Tests với TestRestTemplate

```java
class TaskControllerIT extends AbstractIntegrationTest {

    @Autowired TestRestTemplate restTemplate;

    @Test
    void shouldReturn403WhenNotMember() {
        // Đăng ký user khác, không thuộc project
        String otherToken = registerAndLogin("other@test.com", "other");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(otherToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<ProblemDetail> response = restTemplate.exchange(
            "/api/tasks/{id}", HttpMethod.GET, request, ProblemDetail.class, taskId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
```

`TestRestTemplate` là HTTP client cho integration test — thực hiện HTTP call thật đến embedded server, qua toàn bộ filter chain (JWT, CORS, ...) giống production.

### 19.4 Cache Integration Test

```java
@Test
void shouldPopulateCacheAfterFirstGet() {
    // Lần 1: miss cache → hit DB
    projectService.getProject(projectId, userId);

    // Kiểm tra cache đã được populate
    Cache.ValueWrapper cached = cacheManager.getCache(CACHE_PROJECTS).get(projectId);
    assertThat(cached).isNotNull();

    // Lần 2: hit cache → không gọi DB
    projectService.getProject(projectId, userId);
    // verify repository chỉ được gọi 1 lần
    verify(projectRepository, times(1)).findByIdWithMembers(projectId);
}
```

**Ưu điểm Integration Tests:**
- Kiểm tra toàn bộ stack (HTTP → Security → Service → DB)
- Phát hiện N+1 queries, transaction boundary issues, serialization errors
- Testcontainers đảm bảo behavior giống production database

**Nhược điểm:**
- Chậm hơn unit test (~10-30 giây cho mỗi test class)
- Cần Docker daemon chạy
- Flaky tests nếu container không healthy kịp
- Tốn resource (RAM, CPU)

**Chiến lược testing (Testing Pyramid):**

```
        /\          Integration Tests (ít, chậm, confidence cao)
       /  \         ← Testcontainers, TestRestTemplate
      /    \
     /      \       Service Tests (vừa phải)
    /        \      ← Mockito unit tests
   /          \
  /____________\    Unit Tests (nhiều, nhanh)
                    ← Pure logic tests, no dependencies
```

---

## 20. Docker — Multi-stage Build & docker-compose

### 20.1 Multi-stage Dockerfile

```dockerfile
# ─── Stage 1: Builder ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY pom.xml .
# Cache Maven dependencies riêng → chỉ re-download khi pom.xml thay đổi
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -q

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Non-root user — security best practice
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow

COPY --from=builder /app/target/*.jar app.jar

USER taskflow

EXPOSE 8080

ENTRYPOINT ["java",
    "-XX:+UseContainerSupport",
    "-XX:MaxRAMPercentage=75.0",
    "-jar", "app.jar"]
```

**Tại sao Multi-stage?**

| Stage | Image | Kích thước |
|-------|-------|-----------|
| Builder (JDK) | eclipse-temurin:21-jdk-alpine | ~340 MB |
| Runtime (JRE) | eclipse-temurin:21-jre-alpine | ~180 MB |
| Final image | JRE + app.jar | ~220 MB |

JDK chứa compiler, javac, javadoc — không cần thiết trong production. JRE chỉ cần để chạy `.jar`. **Tiết kiệm ~120 MB** và giảm attack surface.

### 20.2 Build Cache với `--mount=type=cache`

```dockerfile
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests
```

`--mount=type=cache` là BuildKit feature — cache Maven local repository (`~/.m2`) giữa các lần build. Chỉ download lại dependency khi `pom.xml` thay đổi.

```
Lần build 1: Download 200+ dependencies → 3 phút
Lần build 2: pom.xml không đổi → cache hit → 30 giây
Lần build 3: Chỉ src thay đổi → cache hit → 30 giây
```

**Lưu ý:** `--mount=type=cache` không tương thích với Docker Buildx layer cache thông thường. GitHub Actions dùng `cache-from: type=gha` kết hợp với BuildKit.

### 20.3 JVM Container Flags

```
-XX:+UseContainerSupport    Đọc CPU/RAM từ cgroup limit của container
                            (không phải từ host machine)
-XX:MaxRAMPercentage=75.0   JVM heap tối đa = 75% RAM container
                            Ví dụ: container 512MB → heap tối đa ~384MB
                            Còn lại 25% cho non-heap, thread stacks, JVM overhead
```

**Tại sao cần `UseContainerSupport`?**

Trước Java 10, JVM không hiểu cgroup limits. Nếu container được giới hạn 512MB nhưng host có 16GB, JVM sẽ set heap = 4GB (¼ × 16GB) → container bị kill bởi OOMKiller.

Java 11+ bật mặc định `UseContainerSupport`. Flag vẫn được ghi để minh tường.

### 20.4 Non-root User trong Container

```dockerfile
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow
USER taskflow
```

**-S flag**: system user — không có home directory, không có shell, không thể login. Đây là security principle of least privilege: application không cần quyền root để đọc/chạy file JAR.

Nếu application bị compromise, attacker chỉ có quyền của user `taskflow`, không thể:
- Cài thêm package (`apt-get`)
- Đọc file của user khác
- Bind port < 1024

### 20.5 docker-compose cho Development

```yaml
services:
  app:
    build: ./taskflow
    ports: ["8080:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/taskflow
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
    depends_on:
      postgres:
        condition: service_healthy   # chờ postgres HEALTHY, không chỉ started
      kafka:
        condition: service_healthy

  postgres:
    image: postgres:16-alpine
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U taskflow"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    profiles: ["dev"]              # chỉ start khi --profile dev
    ports: ["8090:8080"]
```

**`condition: service_healthy`** vs `condition: service_started`:

- `service_started`: container đã start (nhưng service bên trong chưa chắc ready)
- `service_healthy`: healthcheck command thành công → service thật sự sẵn sàng

Nếu dùng `service_started`, app có thể gặp lỗi "Connection refused" khi PostgreSQL chưa kịp init.

**Ưu điểm docker-compose:**
- Start toàn bộ stack bằng 1 lệnh: `docker compose up -d`
- Môi trường nhất quán giữa dev machines
- Dễ dàng thêm/bỏ service

**Nhược điểm:**
- Không phù hợp cho production (dùng Kubernetes hoặc ECS thay thế)
- Không có auto-restart policy nâng cao, scaling

---

## 21. CI/CD — GitHub Actions

### 21.1 Tổng quan Workflow

```yaml
name: CI/CD

on:
  push:
    branches: [master, main]
    paths:
      - 'taskflow/**'          # chỉ trigger khi code thay đổi
      - '.github/workflows/ci.yml'
  pull_request:
    branches: [master, main]
    paths:
      - 'taskflow/**'

jobs:
  test:   # Job 1: Compile + Test
    ...
  docker: # Job 2: Build & Push (chỉ khi push lên main/master)
    needs: test   # phụ thuộc test — chỉ chạy khi test pass
    if: github.ref == 'refs/heads/master' || github.ref == 'refs/heads/main'
```

**Path filters** (`paths:`) tránh trigger CI khi chỉ thay đổi documentation hay file không liên quan.

### 21.2 Test Job — Service Containers

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16-alpine
        env:
          POSTGRES_DB: taskflow_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
        options: >-
          --health-cmd "pg_isready -U test"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
```

GitHub Actions **service containers** là Docker container chạy song song với job runner. Khác với Testcontainers (start từ Java code), service containers được quản lý bởi GitHub Actions runner. Cả 2 approach đều valid — project này dùng Testcontainers trong integration test để tự quản lý.

### 21.3 Maven Cache

```yaml
- name: Set up Java 21
  uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
    cache: maven    # cache ~/.m2 giữa các runs
```

`cache: maven` tự động cache Maven local repository. Cache key dựa trên `pom.xml` checksum → invalidate khi dependencies thay đổi.

**Tiết kiệm:** ~1-2 phút per run thay vì download lại 200+ jars.

### 21.4 Test Separation

```yaml
- name: Run unit tests
  run: mvn test -pl . -Dtest="*ServiceTest" -q

- name: Run integration tests
  run: mvn verify -Dtest="*IT" -DfailIfNoTests=false
  env:
    TESTCONTAINERS_RYUK_DISABLED: "false"
```

Unit tests (`*ServiceTest`) và integration tests (`*IT`) chạy riêng biệt để:
1. Fail fast — unit test fail ngay, không cần chờ integration tests khởi động container
2. Rõ ràng error: biết ngay là unit test hay integration test lỗi

`TESTCONTAINERS_RYUK_DISABLED=false`: Ryuk là Testcontainers resource reaper — tự dọn container/network khi JVM exit. Bật trong CI để tránh container leak.

### 21.5 Artifact Upload

```yaml
- name: Upload test results
  if: always()   # upload dù test pass hay fail
  uses: actions/upload-artifact@v4
  with:
    name: test-results
    path: taskflow/target/surefire-reports/

- name: Upload coverage report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: jacoco-report
    path: taskflow/target/site/jacoco/
```

`if: always()` đảm bảo test results luôn được upload kể cả khi job fail — cần thiết để debug failing tests.

### 21.6 Docker Build & Push

```yaml
- name: Extract Docker metadata
  id: meta
  uses: docker/metadata-action@v5
  with:
    images: ghcr.io/${{ github.repository }}/taskflow-api
    tags: |
      type=sha,prefix=sha-           # sha-abc1234
      type=ref,event=branch          # master
      type=raw,value=latest,enable={{is_default_branch}}   # latest (chỉ main/master)

- name: Build and push Docker image
  uses: docker/build-push-action@v5
  with:
    context: ./taskflow
    push: true
    tags: ${{ steps.meta.outputs.tags }}
    cache-from: type=gha            # dùng GitHub Actions cache
    cache-to: type=gha,mode=max     # mode=max cache tất cả layers
```

**Tagging strategy:**
- `sha-abc1234` — immutable, truy ra đúng commit đã build
- `master` — latest build của branch
- `latest` — chỉ main branch, dùng cho mặc định pull

**`type=gha` cache** lưu Docker layers vào GitHub Actions cache storage (~10GB free). `mode=max` cache tất cả layers kể cả intermediate stages của multi-stage build.

**GHCR (GitHub Container Registry):**

```yaml
- name: Log in to GitHub Container Registry
  uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}   # automatic token, không cần tạo thủ công
```

`GITHUB_TOKEN` tự động được tạo bởi GitHub Actions cho mỗi run — không cần lưu credentials thủ công. Token có scope `packages: write` như khai báo trong `permissions`.

**Ưu điểm GitHub Actions:**
- Tích hợp sẵn với GitHub repository, PR, issues
- GITHUB_TOKEN tự động — bảo mật hơn personal access tokens
- Marketplace actions phong phú
- Free tier hào phóng (2000 phút/tháng cho public repo)

**Nhược điểm:**
- Vendor lock-in GitHub
- Runner có thể chậm giờ cao điểm
- Secret scanning kém hơn HashiCorp Vault

---

## 22. Actuator & Micrometer — Observability

### 22.1 Spring Boot Actuator

Actuator expose các HTTP endpoints cho monitoring và management:

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when_authorized
  metrics:
    tags:
      application: taskflow
```

**Các endpoints quan trọng:**

| Endpoint | URL | Mục đích |
|----------|-----|----------|
| `/actuator/health` | `GET` | Kiểm tra app, DB, Redis health |
| `/actuator/metrics` | `GET` | Danh sách tất cả metrics |
| `/actuator/metrics/{name}` | `GET` | Chi tiết metric cụ thể |
| `/actuator/prometheus` | `GET` | Export metrics theo format Prometheus |
| `/actuator/info` | `GET` | App version, git commit info |

### 22.2 Counter vs Gauge

Micrometer có nhiều loại metric. Project dùng 2 loại phổ biến:

**Counter** — chỉ tăng, không giảm, đo **tổng số sự kiện**:

```java
@Bean
public Counter taskCreatedCounter(MeterRegistry registry) {
    return Counter.builder("taskflow.tasks.created")
        .description("Total number of tasks created")
        .tag("app", "taskflow")
        .register(registry);
}

// Trong service:
taskCreatedCounter.increment();   // +1 mỗi khi task được tạo
```

Khi query Prometheus: `rate(taskflow_tasks_created_total[5m])` → tốc độ tạo task trong 5 phút.

**Gauge** — đo **giá trị hiện tại**, có thể tăng và giảm:

```java
@Bean
public Gauge tasksInProgressGauge(MeterRegistry registry) {
    return Gauge.builder("taskflow.tasks.in_progress",
            taskRepository,
            repo -> repo.countByStatus(TaskStatus.IN_PROGRESS))  // lazy query
        .description("Current number of tasks in progress")
        .tag("app", "taskflow")
        .register(registry);
}
```

`Gauge` không lưu value — mỗi lần Prometheus scrape, hàm lambda `repo -> repo.countByStatus(...)` được gọi để lấy giá trị hiện tại từ DB.

**So sánh Counter vs Gauge:**

| Đặc điểm | Counter | Gauge |
|-----------|---------|-------|
| Chiều tăng | Chỉ tăng | Tăng/giảm |
| Dùng cho | Events (requests, errors, tasks created) | Current state (queue size, connections, in-progress tasks) |
| Query | `rate(metric[5m])` (tốc độ) | Giá trị trực tiếp |
| Reset khi restart | Về 0 | Query DB lại |

**Các loại metric khác trong Micrometer:**

| Loại | Dùng cho |
|------|---------|
| `Timer` | Đo thời gian và count (HTTP request latency) |
| `DistributionSummary` | Phân phối giá trị (response size, payload size) |
| `LongTaskTimer` | Đo long-running task (background job đang chạy) |

### 22.3 Prometheus Scraping Flow

```
App (port 8080)                Prometheus              Grafana
/actuator/prometheus  ←scrape every 15s→  store TSDB  →  visualize
                               │
                        Alert Manager
                               │
                         PagerDuty/Slack
```

**Prometheus format** (text-based):

```
# HELP taskflow_tasks_created_total Total number of tasks created
# TYPE taskflow_tasks_created_total counter
taskflow_tasks_created_total{app="taskflow",} 42.0

# HELP taskflow_tasks_in_progress Current number of tasks in progress
# TYPE taskflow_tasks_in_progress gauge
taskflow_tasks_in_progress{app="taskflow",} 7.0
```

### 22.4 Tags (Labels)

```java
Counter.builder("taskflow.tasks.completed")
    .tag("app", "taskflow")          // global tag
    .tag("environment", "production")
    .register(registry);
```

Tags cho phép filter và aggregate trong Prometheus/Grafana:

```promql
# Tổng task completed của tất cả instances
sum(taskflow_tasks_completed_total{app="taskflow"})

# Rate theo environment
rate(taskflow_tasks_completed_total{environment="production"}[5m])
```

**Cảnh báo về High Cardinality Tags:** Đừng dùng tag có giá trị không bounded như `userId`, `taskId`. Mỗi unique tag combination = 1 time series riêng → Prometheus OOM với hàng triệu series.

```java
// ❌ Nguy hiểm — unbounded cardinality
Counter.builder("task.operations")
    .tag("taskId", taskId.toString())   // triệu task = triệu time series
    .register(registry);

// ✅ Đúng — bounded values
Counter.builder("task.operations")
    .tag("operation", "create")         // create/update/delete = 3 values
    .tag("priority", priority.name())   // LOW/MEDIUM/HIGH/CRITICAL = 4 values
    .register(registry);
```

### 22.5 Health Checks

Spring Boot Actuator tự động expose health indicators cho:
- **DB** (`DataSourceHealthIndicator`): ping PostgreSQL
- **Redis** (`RedisHealthIndicator`): ping Redis server
- **Kafka** (`KafkaHealthIndicator`): check broker connectivity
- **Disk space** (`DiskSpaceHealthIndicator`): kiểm tra không gian đĩa

```json
// GET /actuator/health
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "PostgreSQL", "version": "16.2" } },
    "redis": { "status": "UP", "details": { "version": "7.2.4" } },
    "kafka": { "status": "UP" }
  }
}
```

Kubernetes/ECS có thể dùng `/actuator/health` làm **liveness/readiness probe** — tự động restart container khi DB connection fail.

**Ưu điểm Actuator + Micrometer:**
- Zero-code metrics — Spring Boot tự động expose JVM, HTTP, DB pool metrics
- Vendor-neutral — cùng code chạy với Prometheus, Datadog, CloudWatch
- Production-ready observability mà không cần thêm sidecar agent

**Nhược điểm:**
- Actuator endpoints cần bảo mật cẩn thận (không expose `/actuator/*` ra internet)
- Micrometer overhead nhỏ cho Counter/Timer (negligible trong hầu hết use cases)
- Gauge chạy query DB mỗi lần scrape — cần cẩn thận với query nặng

---

## Tổng kết — Technology Decision Matrix

| Kỹ thuật | Dùng khi | Không dùng khi |
|----------|----------|----------------|
| Records (DTO) | Immutable data carrier | Cần mutation hoặc kế thừa |
| UUID PK | ID expose ra ngoài | Internal tables, cần max insert performance |
| LAZY fetch | Mặc định cho @ManyToOne | Association luôn cần thiết (dùng JOIN FETCH) |
| JPQL | Standard queries | Window functions, complex aggregation |
| Constructor injection | Luôn dùng | — |
| `readOnly = true` | Read-only service method | Cần write to DB |
| JWT STATELESS | REST API, microservices | Cần revoke ngay lập tức |
| `@PreAuthorize` | Declarative access control | Fine-grained, context-heavy logic |
| Flyway | Mọi môi trường | Prototype/throwaway projects |
| Custom Validator | Reusable business rule | One-off check trong service |
| RFC 7807 | Public API | Internal tooling |
| Redis Cache | Hot data, read-heavy | Data thay đổi mỗi request, cần consistency tuyệt đối |
| `@Cacheable` | Idempotent read method | Method có side effect |
| `@CacheEvict` | Sau mỗi write operation | — |
| AOP `@Around` | Logging, audit, metrics | Business logic (dùng service thay vì) |
| `@Auditable` | Reusable audit trail | One-off audit requirement |
| `REQUIRES_NEW` transaction | Audit log, quan trọng không rollback | Cần chung transaction với caller |
| Kafka | Async event, decoupled services | Simple notification, monolith nhỏ |
| Kafka Records (event) | Event payload | Command (direct action) |
| Virtual Threads | I/O-bound workload (Kafka consumer, API) | CPU-bound tasks |
| Mockito `@Mock` | Unit test — isolate logic | Khi cần test tích hợp thật |
| `@InjectMocks` | Inject tất cả mocks vào class cần test | Complex setup cần factory method |
| `ArgumentCaptor` | Kiểm tra đối số truyền vào mock | Chỉ cần verify mock được gọi |
| Testcontainers | Integration test với service thật | Unit test, CI không có Docker |
| `static` container | Shared giữa test methods — tiết kiệm start time | Cần isolation tuyệt đối |
| `@DynamicPropertySource` | Override config với port container động | Config cố định |
| TestRestTemplate | Full HTTP integration test | Unit/service test |
| Multi-stage Docker | Production image nhỏ, secure | Prototype/dev only |
| `--mount=type=cache` | Cache Maven deps giữa builds | Docker < 18.09 (không có BuildKit) |
| `UseContainerSupport` | JVM trên container | Bare metal (không có cgroup limits) |
| Non-root container user | Security hardening | Dev convenience |
| GitHub Actions | CI/CD tích hợp GitHub | Self-hosted CI, GitLab |
| `condition: service_healthy` | Chờ service thật sự ready | Quick scripts không cần init |
| Actuator `/health` | Liveness/readiness probe | Không deploy lên Kubernetes/ECS |
| Counter | Đếm sự kiện (tasks created, errors) | Giá trị hiện tại có thể giảm |
| Gauge | Trạng thái hiện tại (in-progress count) | Tổng sự kiện theo thời gian |
| Micrometer Tags | Filter/aggregate metrics | High cardinality values (userId, taskId) |
