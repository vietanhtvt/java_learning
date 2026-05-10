# TaskFlow API — Hướng Dẫn Kỹ Thuật Chi Tiết

> Tài liệu giải thích toàn bộ kỹ thuật được áp dụng trong dự án, kèm ưu/nhược điểm và lý do lựa chọn.

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
