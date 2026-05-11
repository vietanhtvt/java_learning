# Kế hoạch học Java Core & Spring Boot — Middle Java Engineer

> **Mục tiêu:** Tự tin phỏng vấn vị trí **Middle Java Engineer** (2–4 năm kinh nghiệm)
> **Thời gian:** 16 tuần (~4 tháng), học 2–3 giờ/ngày

---

## Tổng quan lộ trình

```
Phase 1 │ Java Core Nâng cao         │ Tuần 1–6
Phase 2 │ Design Patterns & Clean Code│ Tuần 6–7
Phase 3 │ Spring Boot                 │ Tuần 8–13
Phase 4 │ Database & System Design    │ Tuần 13–14
Phase 5 │ Ôn tập & Mock Interview     │ Tuần 15–16
```

---

## Phase 1 — Java Core Nâng cao (Tuần 1–6)

### 1.1 OOP & Java Fundamentals (Tuần 1)

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| 4 nguyên lý OOP | Encapsulation, Inheritance, Polymorphism, Abstraction — định nghĩa + ví dụ thực tế |
| `interface` vs `abstract class` | Khi nào dùng cái nào, default method (Java 8+), multiple inheritance |
| Generics | `<T>`, bounded wildcard `<? extends T>`, `<? super T>`, type erasure |
| `String` internals | String pool, immutability, `StringBuilder` vs `StringBuffer` |
| `equals()` & `hashCode()` | Contract giữa hai method, khi nào phải override cả hai |
| Exception Handling | Checked vs Unchecked, `try-with-resources`, custom exception hierarchy |
| `final` / `finally` / `finalize` | Sự khác nhau và cách hoạt động |
| Java Memory Model | Stack vs Heap, object lifecycle |

**Câu hỏi phỏng vấn hay gặp:**
- Tại sao `String` immutable? Lợi ích của String pool?
- `==` vs `.equals()` vs `hashCode()` — contract là gì?
- Interface có thể có constructor không? Tại sao?
- Explain covariant return type trong Java.
- Khi nào nên dùng `abstract class` thay vì `interface`?

**Bài tập:** Implement một custom `ImmutablePoint` class, override `equals/hashCode`, viết unit test.

---

### 1.2 Collections Framework (Tuần 2)

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| `ArrayList` vs `LinkedList` | Độ phức tạp O(n) cho get/add/remove, khi nào dùng cái nào |
| `HashMap` internals | Hashing, bucket, linked list → tree (Java 8+), resize, load factor |
| `LinkedHashMap` / `TreeMap` | Ordering đảm bảo gì, dùng khi nào |
| `HashSet` / `TreeSet` | Backed bởi Map, ordering |
| `ConcurrentHashMap` | Thread-safe, segment locking (Java 7) → CAS (Java 8+) |
| `PriorityQueue` / `ArrayDeque` | Heap-based, dùng trong scheduling/BFS/DFS |
| `Comparable` vs `Comparator` | Natural order vs custom order |
| `Collections` utility | `sort`, `binarySearch`, `unmodifiableList`, `synchronizedList` |
| `Iterable` vs `Iterator` | Fail-fast vs fail-safe iterator |

**Câu hỏi phỏng vấn hay gặp:**
- `HashMap` xử lý collision như thế nào? Tại sao Java 8 chuyển từ list sang tree?
- Khi nào `HashMap` resize? Load factor mặc định là bao nhiêu?
- `ConcurrentHashMap` vs `Hashtable` vs `Collections.synchronizedMap()`?
- `HashMap` có cho phép `null` key không? `TreeMap` thì sao?
- `ArrayList` initial capacity là bao nhiêu? Grow như thế nào?

**Bài tập:** Implement `LRU Cache` dùng `LinkedHashMap`.

---

### 1.3 Concurrency & Multithreading (Tuần 3)

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| Thread lifecycle | `NEW → RUNNABLE → BLOCKED/WAITING → TERMINATED` |
| Tạo thread | `Runnable`, `Callable`, `Thread`, `FutureTask` |
| `synchronized` | Object lock vs class lock, re-entrant |
| `volatile` | Visibility guarantee, không đảm bảo atomicity |
| `Lock` interface | `ReentrantLock`, `ReadWriteLock`, `tryLock()` |
| Executor Framework | `ExecutorService`, `ThreadPoolExecutor`, `Executors` factory |
| `Future` & `CompletableFuture` | Async, chaining, `thenApply`, `thenCompose`, `exceptionally` |
| `ThreadLocal` | Per-thread storage, memory leak risk |
| `java.util.concurrent` | `CountDownLatch`, `Semaphore`, `CyclicBarrier`, `Phaser` |
| Common problems | Deadlock, livelock, starvation, race condition |
| Atomic classes | `AtomicInteger`, `AtomicReference`, CAS operation |

**Câu hỏi phỏng vấn hay gặp:**
- `volatile` đảm bảo gì? Có thể thay thế `synchronized` không? Khi nào thì được?
- Deadlock xảy ra khi nào? Làm thế nào để phòng tránh và phát hiện?
- `ThreadLocal` dùng để làm gì? Memory leak xảy ra khi nào?
- `Future` vs `CompletableFuture` — sự khác biệt?
- Thread pool sizing: CPU-bound vs IO-bound task khác nhau thế nào?
- `CountDownLatch` vs `CyclicBarrier` — khác nhau ở điểm nào?

**Bài tập:** Implement producer-consumer pattern dùng `BlockingQueue`; viết code demo deadlock rồi fix.

---

### 1.4 Functional Programming & Stream API (Tuần 4)

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| Lambda expression | Syntax, effectively final, scope |
| Functional Interfaces | `Predicate`, `Function`, `Consumer`, `Supplier`, `BiFunction`, `UnaryOperator` |
| Method Reference | `Class::method`, `instance::method`, `Class::new` |
| Stream API — intermediate | `filter`, `map`, `flatMap`, `distinct`, `sorted`, `peek`, `limit`, `skip` |
| Stream API — terminal | `collect`, `reduce`, `count`, `findFirst`, `anyMatch`, `forEach` |
| `Collectors` | `toList`, `toMap`, `groupingBy`, `partitioningBy`, `joining` |
| `Optional` | `map`, `flatMap`, `orElse`, `orElseGet`, `ifPresent` |
| Parallel Stream | Fork-Join pool, khi nào dùng được, khi nào không |
| Lazy evaluation | Intermediate ops lazy, terminal ops eager |

**Câu hỏi phỏng vấn hay gặp:**
- Stream lazy evaluation hoạt động thế nào? Cho ví dụ.
- `map` vs `flatMap`? Khi nào dùng `flatMap`?
- Parallel stream có luôn nhanh hơn sequential không? Tại sao?
- `Optional` sinh ra để giải quyết vấn đề gì? Anti-pattern của `Optional` là gì?
- `Stream` có thể dùng lại không?

**Bài tập:** Cho danh sách orders, tính tổng doanh thu theo category, tìm top 3 sản phẩm bán chạy dùng Stream API.

---

### 1.5 JVM Internals & Performance (Tuần 5)

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| JVM Architecture | ClassLoader → Bytecode Verifier → Execution Engine |
| Memory Areas | Heap (Young/Survivor/Old), Metaspace, Stack, PC Register, Native Stack |
| Garbage Collection | Minor GC vs Major GC vs Full GC |
| GC Algorithms | Serial, Parallel, CMS, G1GC (default Java 9+), ZGC, Shenandoah |
| GC Tuning | `-Xms`, `-Xmx`, `-XX:+UseG1GC`, `-XX:NewRatio` |
| ClassLoader | Bootstrap → Extension → Application, delegation model |
| JIT Compiler | Hotspot compilation, C1 vs C2 |
| Memory leak | Patterns: static collection, unclosed resource, ThreadLocal |
| Profiling tools | VisualVM, JConsole, `jstack`, `jmap`, `jstat` |

**Câu hỏi phỏng vấn hay gặp:**
- Mô tả các vùng nhớ trong JVM.
- `OutOfMemoryError` vs `StackOverflowError` — nguyên nhân từng loại?
- Minor GC vs Full GC — khi nào xảy ra?
- Tại sao G1GC được recommend từ Java 9+?
- Memory leak trong Java xảy ra thế nào? Cho ví dụ.

---

### 1.6 Java Modern Features (Tuần 6 — buổi 1)

| Version | Feature quan trọng |
|---------|-------------------|
| Java 8 | Lambda, Stream, Optional, Default/Static method trong interface, `LocalDate`/`LocalDateTime` |
| Java 9 | Module system (JPMS), `List.of()`, `Map.of()`, `Set.of()` |
| Java 10 | `var` (local variable type inference) |
| Java 11 (LTS) | `String` methods (`strip`, `isBlank`, `lines`, `repeat`), `var` trong lambda |
| Java 14 | `record` (preview), `switch` expression ổn định |
| Java 16 | `record` ổn định, `instanceof` pattern matching |
| Java 17 (LTS) | Sealed classes/interfaces, switch pattern matching preview |
| Java 21 (LTS) | Virtual Threads (Project Loom), Record Patterns, Sequenced Collections |

**Câu hỏi hay gặp:**
- `record` class khác `class` thường thế nào? Khi nào dùng?
- Virtual Threads giải quyết vấn đề gì so với platform threads?
- Sealed class dùng để làm gì?

---

## Phase 2 — Design Patterns & Clean Code (Tuần 6–7)

### 2.1 Design Patterns quan trọng nhất

**Creational:**
| Pattern | Khi nào dùng | Ví dụ thực tế |
|---------|-------------|--------------|
| Singleton | Chỉ cần 1 instance | Spring Bean (default scope), DB connection pool |
| Builder | Object phức tạp nhiều field | `StringBuilder`, Lombok `@Builder`, `HttpClient.newBuilder()` |
| Factory Method | Tạo object mà không biết class cụ thể | Spring `BeanFactory`, `Calendar.getInstance()` |
| Abstract Factory | Family of related objects | JDBC driver, UI component kit |

**Structural:**
| Pattern | Khi nào dùng | Ví dụ thực tế |
|---------|-------------|--------------|
| Proxy | Intercept, lazy load, security | Spring AOP, JPA Lazy loading |
| Decorator | Thêm behavior không sửa class gốc | Java IO streams (`BufferedReader wraps FileReader`) |
| Adapter | Bridge interface không tương thích | `Arrays.asList()`, legacy integration |
| Facade | Đơn giản hóa subsystem phức tạp | Service layer trong Spring, SLF4J |

**Behavioral:**
| Pattern | Khi nào dùng | Ví dụ thực tế |
|---------|-------------|--------------|
| Strategy | Thay đổi algorithm at runtime | `Comparator`, payment method switching |
| Observer | Event-driven, notify subscribers | Spring ApplicationEvent, Kafka listener |
| Template Method | Define skeleton, subclass fills steps | `JdbcTemplate`, `AbstractList` |
| Command | Encapsulate request as object | Undo/redo, task queue |
| Chain of Responsibility | Pass request qua chain handler | Spring Security filter chain, servlet filter |

**Câu hỏi phỏng vấn:**
- Singleton thread-safe như thế nào? Double-checked locking?
- Strategy vs Template Method — khác gì nhau?
- Spring dùng những Design Pattern nào?

### 2.2 SOLID Principles

| Principle | Tên | Nội dung ngắn gọn |
|-----------|-----|------------------|
| S | Single Responsibility | 1 class chỉ có 1 lý do để thay đổi |
| O | Open/Closed | Open for extension, closed for modification |
| L | Liskov Substitution | Subclass phải thay thế được superclass |
| I | Interface Segregation | Nhiều interface nhỏ tốt hơn 1 interface lớn |
| D | Dependency Inversion | Depend on abstraction, not implementation |

---

## Phase 3 — Spring Boot (Tuần 8–13)

### 3.1 Spring Core — IoC & DI (Tuần 8)

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| IoC Container | `ApplicationContext` vs `BeanFactory` |
| Dependency Injection | Constructor (recommended) vs Setter vs Field injection |
| Bean Lifecycle | Instantiation → Populate → `@PostConstruct` → Use → `@PreDestroy` → Destroy |
| Stereotype Annotations | `@Component`, `@Service`, `@Repository`, `@Controller` |
| `@Configuration` & `@Bean` | Java-based config vs classpath scanning |
| `@Scope` | `singleton` (default), `prototype`, `request`, `session` |
| Spring AOP | `@Aspect`, `@Around`, `@Before`, `@After`, `@AfterReturning`, Pointcut expression |
| `@Profile` | Environment-specific beans |

**Câu hỏi phỏng vấn:**
- IoC và DI liên quan thế nào?
- Tại sao Constructor injection được recommend hơn Field injection?
- Singleton bean có thread-safe không? Cần làm gì để đảm bảo?
- `@Component` vs `@Bean` — khi nào dùng cái nào?
- Circular dependency trong Spring — phát hiện và giải quyết thế nào?
- AOP proxy là JDK proxy hay CGLIB? Khi nào dùng loại nào?

---

### 3.2 Spring Boot Auto-configuration (Tuần 8)

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| `@SpringBootApplication` | Gồm `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan` |
| Auto-configuration mechanism | `spring.factories` (Boot 2.x) / `AutoConfiguration.imports` (Boot 3.x) |
| `@Conditional` annotations | `@ConditionalOnClass`, `@ConditionalOnProperty`, `@ConditionalOnMissingBean` |
| `application.yml` / `.properties` | Profiles, `spring.profiles.active`, externalized config |
| `@ConfigurationProperties` | Type-safe config binding với validation |
| `@Value` | Simple value injection, SpEL |

---

### 3.3 Spring MVC & REST API (Tuần 9)

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| DispatcherServlet | Request processing flow |
| `@RestController` | = `@Controller` + `@ResponseBody` |
| Request mapping | `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping` |
| Request params | `@PathVariable`, `@RequestParam`, `@RequestBody`, `@RequestHeader` |
| Response | `ResponseEntity<T>`, HTTP status, custom headers |
| Exception Handling | `@ControllerAdvice` + `@ExceptionHandler`, `ResponseStatusException` |
| Validation | JSR-303: `@Valid`, `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`, `@Pattern` |
| Filter vs Interceptor | `OncePerRequestFilter`, `HandlerInterceptor` — vào thời điểm nào trong lifecycle |
| Content Negotiation | JSON (Jackson), XML |
| CORS | `@CrossOrigin`, global config qua `WebMvcConfigurer` |

**Câu hỏi phỏng vấn:**
- `@Controller` vs `@RestController` khác gì?
- `@RequestBody` dùng được với `GET` không? Tại sao?
- Filter vs Interceptor vs AOP — dùng khi nào?
- Làm thế nào để handle exception globally trong Spring Boot?
- `@Valid` vs `@Validated` — khác gì?

**Bài tập:** Xây REST API quản lý sản phẩm với CRUD, pagination, validation, global exception handler, custom error response format.

---

### 3.4 Spring Data JPA & Database (Tuần 10)

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| JPA / Hibernate | Entity, `@Table`, `@Column`, `@Id`, `@GeneratedValue` strategies |
| Relationships | `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@OneToOne` + `mappedBy`, `cascade` |
| Fetch Type | `LAZY` vs `EAGER` — N+1 problem, `JOIN FETCH`, `@EntityGraph` |
| Repository | `JpaRepository`, `PagingAndSortingRepository`, custom `@Query` |
| JPQL & Native Query | `@Query`, `nativeQuery = true`, `@NamedQuery` |
| Transaction | `@Transactional`, propagation types, isolation levels |
| Pagination & Sorting | `Pageable`, `PageRequest.of()`, `Sort` |
| Connection Pool | HikariCP — `maximumPoolSize`, `connectionTimeout` |
| Database Migration | Flyway hoặc Liquibase |
| Optimistic vs Pessimistic Locking | `@Version`, `LockModeType` |

**Transaction Propagation:**

| Type | Mô tả |
|------|-------|
| `REQUIRED` (default) | Join existing hoặc tạo mới |
| `REQUIRES_NEW` | Luôn tạo transaction mới, suspend existing |
| `NESTED` | Savepoint trong transaction hiện tại |
| `SUPPORTS` | Dùng nếu có, không cần không sao |
| `NOT_SUPPORTED` | Suspend existing transaction |
| `NEVER` | Throw exception nếu có transaction |

**Isolation Levels:**

| Level | Dirty Read | Non-repeatable Read | Phantom Read |
|-------|-----------|-------------------|-------------|
| Read Uncommitted | Có thể | Có thể | Có thể |
| Read Committed | Không | Có thể | Có thể |
| Repeatable Read | Không | Không | Có thể |
| Serializable | Không | Không | Không |

**Câu hỏi phỏng vấn:**
- N+1 problem là gì? Detect thế nào? Fix thế nào?
- `LAZY` fetch có thể gây ra lỗi gì? (LazyInitializationException)
- `@Transactional` hoạt động thế nào bên trong? (AOP proxy)
- `@Transactional` trên private method có hoạt động không? Tại sao?
- Optimistic locking vs Pessimistic locking — khi nào dùng loại nào?
- Flyway vs Liquibase — khác gì nhau?

---

### 3.5 Spring Security (Tuần 11)

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| Authentication vs Authorization | Xác thực (ai?) vs phân quyền (làm được gì?) |
| Security Filter Chain | Thứ tự các filter, `SecurityFilterChain` bean |
| `UserDetailsService` | Load user từ DB, `UserDetails` interface |
| Password Encoding | `BCryptPasswordEncoder`, không lưu plain text |
| JWT | Tạo token, validate, parse claims, refresh token strategy |
| Session vs Stateless | `SessionCreationPolicy.STATELESS` cho REST API |
| Method Security | `@PreAuthorize("hasRole('ADMIN')")`, `@PostAuthorize`, `@Secured` |
| CSRF | Tại sao REST API thường disable CSRF? |
| CORS | Config trong Spring Security |
| OAuth2 / OIDC | Resource server, Authorization server cơ bản |

**JWT Flow:**
```
Client → POST /auth/login (username/password)
Server → Validate credentials → Generate JWT (access + refresh token)
Client → Gửi JWT trong Authorization: Bearer <token>
Server → Validate JWT signature, check expiry → Process request
```

**Câu hỏi phỏng vấn:**
- Spring Security filter chain hoạt động thế nào?
- JWT có thể revoke không? Giải quyết thế nào?
- CSRF là gì? Tại sao REST API với JWT không cần CSRF protection?
- `@PreAuthorize` hoạt động thế nào bên trong?
- Refresh token strategy: lưu ở đâu, rotate thế nào?

---

### 3.6 Testing (Tuần 12)

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| JUnit 5 | `@Test`, `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll`, `@DisplayName`, `@ParameterizedTest` |
| Mockito | `@Mock`, `@Spy`, `@InjectMocks`, `when().thenReturn()`, `verify()`, `ArgumentCaptor` |
| `@SpringBootTest` | Full context, integration test |
| `@WebMvcTest` | Test controller layer, mock service |
| `@DataJpaTest` | Test repository layer, H2 in-memory |
| MockMvc | `mockMvc.perform()`, `.andExpect()`, test REST endpoints |
| Testcontainers | Integration test với PostgreSQL/Redis thật |
| Code Coverage | JaCoCo, minimum coverage threshold |
| Test Pyramid | Unit > Integration > E2E |

**Câu hỏi phỏng vấn:**
- `@Mock` vs `@MockBean` — khác gì? Dùng khi nào?
- `@Spy` vs `@Mock` — khác gì?
- Tại sao `@DataJpaTest` nhanh hơn `@SpringBootTest`?
- Làm thế nào để test private method? Có nên test không?
- Test pyramid là gì? Tại sao quan trọng?

---

### 3.7 Caching, Messaging & DevOps (Tuần 13)

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| Spring Cache | `@Cacheable`, `@CachePut`, `@CacheEvict`, cache abstraction |
| Redis | Spring Data Redis, `RedisTemplate`, TTL, eviction policy |
| Cache strategies | Cache-aside, Write-through, Write-behind, Read-through |
| Kafka cơ bản | Producer, Consumer, Topic, Partition, Consumer Group, Offset |
| Spring Kafka | `@KafkaListener`, `KafkaTemplate`, error handling |
| Spring Boot Actuator | `/actuator/health`, `/actuator/metrics`, `/actuator/info`, Micrometer |
| Logging | SLF4J + Logback, MDC (correlation ID), structured JSON logging |
| Docker | Dockerfile multi-stage build, docker-compose cho Spring Boot + DB + Redis |
| GitHub Actions | CI pipeline: build → test → Docker build |

**Câu hỏi phỏng vấn:**
- Cache-aside vs Read-through strategy — khác gì?
- Cache stampede là gì? Giải quyết thế nào?
- Kafka partition và consumer group liên quan thế nào?
- Tại sao dùng correlation ID trong logging?

---

## Phase 4 — Database & System Design cơ bản (Tuần 13–14)

### 4.1 SQL & Database

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| SQL cơ bản | `JOIN` types, `GROUP BY`, `HAVING`, subquery, window function |
| Index | B-Tree index, covering index, composite index, khi nào index không dùng được |
| Query Optimization | `EXPLAIN ANALYZE`, avoid SELECT *, avoid function on indexed column |
| Transaction | ACID properties |
| Normalization | 1NF, 2NF, 3NF — khi nào denormalize? |
| PostgreSQL specifics | `JSONB`, `UUID`, `SERIAL` vs `IDENTITY` |

### 4.2 System Design cơ bản (cho Middle level)

| Chủ đề | Nội dung cần nắm |
|--------|-----------------|
| REST API design | Resource naming, HTTP methods, status codes, versioning |
| Pagination | Offset vs cursor-based pagination |
| Rate Limiting | Token bucket, fixed window |
| Caching | Khi nào cache, TTL strategy, invalidation |
| Async processing | Message queue cho long-running task |
| Database design | ERD, foreign key, index strategy |
| Monolith vs Microservices | Tradeoffs, khi nào dùng cái nào |
| Load Balancing | Round robin, least connections |
| CAP Theorem | Consistency, Availability, Partition tolerance |

---

## Phase 5 — Ôn tập & Mock Interview (Tuần 15–16)

### Checklist kỹ năng Middle Java Engineer

#### Java Core
- [ ] Giải thích `HashMap` internal: hashing → bucket → collision → resize
- [ ] Viết thread-safe Singleton theo Double-Checked Locking
- [ ] Implement producer-consumer dùng `BlockingQueue`
- [ ] Stream API thành thạo: `groupingBy`, `flatMap`, `reduce`
- [ ] Giải thích GC: Young Gen → Survivor → Old Gen promotion
- [ ] Debug deadlock từ thread dump

#### Design Patterns
- [ ] Implement Builder pattern tay (không dùng Lombok)
- [ ] Giải thích Strategy pattern với ví dụ trong Spring
- [ ] Nhận biết pattern trong Spring (Proxy, Template, Observer, Factory)

#### Spring Boot
- [ ] Xây REST API CRUD hoàn chỉnh với validation + exception handling
- [ ] Implement JWT authentication từ đầu
- [ ] Giải thích `@Transactional` AOP proxy, self-invocation problem
- [ ] Fix N+1 problem bằng `JOIN FETCH` hoặc `@EntityGraph`
- [ ] Viết `@WebMvcTest` + `@DataJpaTest`
- [ ] Configure Redis cache với `@Cacheable`
- [ ] Dockerize Spring Boot app với multi-stage Dockerfile

#### Database
- [ ] Giải thích index B-Tree và khi nào index không được dùng
- [ ] Viết query với `EXPLAIN ANALYZE` và tối ưu
- [ ] Isolation level và khi nào cần dùng `SERIALIZABLE`

---

## Dự án thực hành (làm song song với học)

| Dự án | Kỹ năng luyện | Tuần |
|-------|--------------|------|
| **Task Manager API** | REST CRUD, Spring Data JPA, Validation, Global Exception Handler | 9–10 |
| **Auth Service** | Spring Security, JWT, Refresh Token, BCrypt | 11 |
| **Product Catalog** | Redis Cache, Pagination, File Upload (S3 giả lập) | 12–13 |
| **Mini Blog** | Full stack: JPA relationships, N+1 fix, Testing, Docker Compose | 14 |

---

## Timeline tuần-by-tuần

| Tuần | Phase | Nội dung chính | Bài tập |
|------|-------|---------------|---------|
| 1 | Core | OOP, Generics, Exception, String | Implement ImmutablePoint |
| 2 | Core | Collections, HashMap internals | Implement LRU Cache |
| 3 | Core | Concurrency, ExecutorService, CompletableFuture | Producer-Consumer, Deadlock demo |
| 4 | Core | Stream API, Optional, Functional Interfaces | Stream exercises trên danh sách orders |
| 5 | Core | JVM, GC, Memory Model | Phân tích heap dump (VisualVM) |
| 6 | Core + Patterns | Java 11–21 features, SOLID | Record class, Sealed class |
| 7 | Patterns | Design Patterns (Creational + Structural + Behavioral) | Implement Strategy + Observer |
| 8 | Spring | Spring Core, IoC, DI, AOP, Auto-config | Spring + AOP logging demo |
| 9 | Spring | Spring MVC, REST API, Validation | Task Manager API |
| 10 | Spring | Spring Data JPA, Transaction, N+1 | Thêm JPA vào Task Manager |
| 11 | Spring | Spring Security, JWT | Auth Service |
| 12 | Spring | Testing: JUnit 5, Mockito, WebMvcTest | Test toàn bộ Task Manager |
| 13 | Spring + DevOps | Redis, Kafka cơ bản, Docker, Actuator | Dockerize + Redis cache |
| 14 | DB + Design | SQL optimize, System Design cơ bản | Mini Blog project |
| 15 | Review | Ôn tập Java Core + Spring Boot | Mock interview tự làm |
| 16 | Interview | Mock interview, behavioral questions | Final project review |

---

## Nguồn học tham khảo

| Tài nguyên | Loại | Mục đích |
|-----------|------|---------|
| [Baeldung](https://www.baeldung.com) | Website | Spring Boot, Java in-depth, best practices |
| [Java Brains](https://www.youtube.com/@Java.Brains) | YouTube | Spring Boot video series, dễ hiểu |
| [Amigoscode](https://www.youtube.com/@amigoscode) | YouTube | Spring Boot full course, thực chiến |
| *Effective Java* — Joshua Bloch | Sách | Java best practices, must-read |
| *Spring in Action* — Craig Walls | Sách | Spring Boot comprehensive |
| [Spring Official Docs](https://docs.spring.io/spring-boot/docs/current/reference/html/) | Docs | Reference chính thức |
| [Java Language Spec](https://docs.oracle.com/javase/specs/) | Docs | Khi cần hiểu sâu |
| [Refactoring.Guru](https://refactoring.guru/design-patterns) | Website | Design Patterns với ví dụ rõ ràng |

---

## Câu hỏi behavioral cho phỏng vấn Middle

> Middle engineer không chỉ bị hỏi kỹ thuật — chuẩn bị những câu này:

- Kể về lần bạn phải debug một production issue khó — approach thế nào?
- Khi bạn không đồng ý với quyết định kỹ thuật của team, bạn xử lý thế nào?
- Kể về một dự án bạn refactor legacy code — tradeoffs gì?
- Bạn prioritize task thế nào khi có nhiều việc cùng lúc?
- Describe một lần bạn mentor/help một junior developer.

---

*Cập nhật lần cuối: 2026-05*
