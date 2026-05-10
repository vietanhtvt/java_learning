# Java Learning Roadmap — Middle Java Engineer

Mục tiêu: nắm vững Java Core và Spring Boot để tự tin phỏng vấn vị trí **Middle Java Engineer**.

---

## Tổng quan lộ trình

```
Phase 1: Java Core (6–8 tuần)
Phase 2: Java Spring Boot (6–8 tuần)
Phase 3: Ôn tập & Mock Interview (2–3 tuần)
```

---

## Phase 1 — Java Core

### 1.1 OOP & Java Fundamentals (Tuần 1–2)

| Chủ đề | Nội dung |
|--------|----------|
| OOP | Encapsulation, Inheritance, Polymorphism, Abstraction |
| Class & Object | Constructor, `this`, `super`, static vs instance |
| Interface & Abstract Class | Sự khác nhau, khi nào dùng cái nào |
| Generics | `<T>`, wildcard `<? extends T>`, `<? super T>` |
| Enum | Enum với method, field, constructor |
| Exception Handling | Checked vs Unchecked, `try-with-resources`, custom exception |
| Java Memory Model | Stack vs Heap, Garbage Collection cơ bản |

**Câu hỏi phỏng vấn thường gặp:**
- Interface vs Abstract class khác nhau thế nào?
- `==` vs `.equals()` vs `hashCode()` — contract giữa chúng?
- Tại sao `String` là immutable?
- Explain `final`, `finally`, `finalize`.

---

### 1.2 Collections & Data Structures (Tuần 2–3)

| Chủ đề | Nội dung |
|--------|----------|
| List | `ArrayList` vs `LinkedList` — độ phức tạp O(n) |
| Map | `HashMap` vs `LinkedHashMap` vs `TreeMap` — cách hoạt động bên trong |
| Set | `HashSet`, `TreeSet`, `LinkedHashSet` |
| Queue / Deque | `PriorityQueue`, `ArrayDeque` |
| Collections utility | `Collections.sort()`, `Collections.unmodifiableList()` |

**Câu hỏi phỏng vấn thường gặp:**
- `HashMap` hoạt động nội bộ như thế nào? Collision được xử lý ra sao?
- Khi nào `HashMap` resize? Load factor là gì?
- `ConcurrentHashMap` vs `Hashtable` vs `synchronized HashMap`?
- `Comparable` vs `Comparator`?

---

### 1.3 Concurrency & Multithreading (Tuần 3–4)

| Chủ đề | Nội dung |
|--------|----------|
| Thread lifecycle | `NEW`, `RUNNABLE`, `BLOCKED`, `WAITING`, `TERMINATED` |
| Thread tạo | `Runnable`, `Callable`, `Thread` |
| Synchronization | `synchronized`, `volatile`, `Lock` interface |
| Executor Framework | `ExecutorService`, `ThreadPoolExecutor`, `FixedThreadPool` |
| Future & CompletableFuture | Async programming, chaining |
| Common problems | Deadlock, Race condition, Starvation |
| java.util.concurrent | `CountDownLatch`, `Semaphore`, `CyclicBarrier` |

**Câu hỏi phỏng vấn thường gặp:**
- `volatile` đảm bảo gì? Có thay thế `synchronized` không?
- Explain deadlock — cách phát hiện và ngăn chặn?
- `ThreadLocal` dùng để làm gì?
- `Future` vs `CompletableFuture`?

---

### 1.4 Functional Programming & Stream API (Tuần 4–5)

| Chủ đề | Nội dung |
|--------|----------|
| Lambda | Syntax, functional interfaces |
| Method Reference | `::` operator |
| Stream API | `filter`, `map`, `flatMap`, `reduce`, `collect` |
| Optional | Tránh `NullPointerException` |
| Functional Interfaces | `Predicate`, `Function`, `Consumer`, `Supplier`, `BiFunction` |

**Câu hỏi phỏng vấn thường gặp:**
- Stream lazy evaluation hoạt động thế nào?
- `map` vs `flatMap`?
- Parallel stream — khi nào nên dùng, khi nào không?

---

### 1.5 JVM & Performance (Tuần 5–6)

| Chủ đề | Nội dung |
|--------|----------|
| JVM Architecture | ClassLoader, JIT Compiler, Execution Engine |
| Memory Areas | Heap (Young/Old Gen), Metaspace, Stack, PC Register |
| Garbage Collection | GC algorithms: G1, ZGC, Shenandoah |
| GC Tuning cơ bản | `-Xms`, `-Xmx`, `-XX:+UseG1GC` |
| Class Loading | Bootstrap, Extension, Application ClassLoader |

**Câu hỏi phỏng vấn thường gặp:**
- Các vùng nhớ trong JVM?
- `OutOfMemoryError` vs `StackOverflowError` — nguyên nhân?
- GC hoạt động như thế nào? Minor GC vs Major GC?

---

### 1.6 Java Modern Features (Tuần 6)

| Version | Feature |
|---------|---------|
| Java 8 | Lambda, Stream, Optional, Default method, `LocalDate`/`LocalTime` |
| Java 9 | Module system, `List.of()`, `Map.of()` |
| Java 11 | `String` methods mới, `var` trong lambda |
| Java 14–16 | Records, Pattern Matching `instanceof`, Sealed class |
| Java 17 (LTS) | Sealed classes, Switch expression |
| Java 21 (LTS) | Virtual Threads (Project Loom), Record Patterns |

---

## Phase 2 — Spring Boot

### 2.1 Spring Core & IoC (Tuần 7–8)

| Chủ đề | Nội dung |
|--------|----------|
| IoC Container | `ApplicationContext`, `BeanFactory` |
| Dependency Injection | Constructor injection, Setter injection, Field injection |
| Bean Lifecycle | `@PostConstruct`, `@PreDestroy`, `InitializingBean` |
| Annotations | `@Component`, `@Service`, `@Repository`, `@Controller` |
| `@Configuration` & `@Bean` | Java-based config |
| `@Scope` | Singleton, Prototype, Request, Session |
| Spring AOP | `@Aspect`, `@Around`, `@Before`, `@After`, Pointcut |

**Câu hỏi phỏng vấn thường gặp:**
- IoC và DI khác nhau thế nào?
- Tại sao nên dùng Constructor injection thay vì Field injection?
- Singleton bean có thread-safe không?
- `@Component` vs `@Bean` khác gì?

---

### 2.2 Spring Boot Auto-configuration (Tuần 8)

| Chủ đề | Nội dung |
|--------|----------|
| `@SpringBootApplication` | Gồm `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan` |
| Auto-configuration | `spring.factories` / `AutoConfiguration.imports` |
| `@Conditional` | `@ConditionalOnClass`, `@ConditionalOnProperty` |
| `application.yml` | Profiles, externalized config |
| `@ConfigurationProperties` | Type-safe config binding |

---

### 2.3 Spring MVC & REST API (Tuần 9)

| Chủ đề | Nội dung |
|--------|----------|
| REST Controllers | `@RestController`, `@RequestMapping`, `@GetMapping`, ... |
| Request handling | `@PathVariable`, `@RequestParam`, `@RequestBody` |
| Response | `ResponseEntity`, HTTP status codes |
| Exception Handling | `@ControllerAdvice`, `@ExceptionHandler` |
| Validation | `@Valid`, `@NotNull`, `@Size`, `BindingResult` |
| Content Negotiation | JSON, XML |
| Filter & Interceptor | `OncePerRequestFilter`, `HandlerInterceptor` |

**Câu hỏi phỏng vấn thường gặp:**
- `@Controller` vs `@RestController`?
- Làm thế nào để handle exception globally?
- Filter vs Interceptor vs AOP — dùng khi nào?

---

### 2.4 Spring Data JPA & Database (Tuần 10)

| Chủ đề | Nội dung |
|--------|----------|
| JPA / Hibernate | Entity, `@Table`, `@Column`, `@Id`, `@GeneratedValue` |
| Relationships | `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `@OneToOne` |
| Fetch Type | `LAZY` vs `EAGER` — N+1 problem |
| Repository | `JpaRepository`, `CrudRepository`, custom queries |
| JPQL & Native Query | `@Query`, `nativeQuery = true` |
| Transaction | `@Transactional`, propagation, isolation levels |
| Pagination & Sorting | `Pageable`, `PageRequest`, `Sort` |
| Connection Pool | HikariCP config |

**Câu hỏi phỏng vấn thường gặp:**
- N+1 problem là gì? Cách giải quyết?
- `LAZY` vs `EAGER` fetching?
- Transaction propagation types: `REQUIRED`, `REQUIRES_NEW`, `NESTED`?
- Isolation levels: Read Uncommitted, Read Committed, Repeatable Read, Serializable?

---

### 2.5 Spring Security (Tuần 11)

| Chủ đề | Nội dung |
|--------|----------|
| Authentication vs Authorization | Khái niệm và flow |
| SecurityFilterChain | Cách cấu hình |
| JWT | Tạo, validate, refresh token |
| `UserDetailsService` | Custom user loading |
| OAuth2 / OpenID Connect | Cơ bản |
| Method Security | `@PreAuthorize`, `@Secured` |
| CSRF, CORS | Config cho REST API |

**Câu hỏi phỏng vấn thường gặp:**
- Spring Security filter chain hoạt động thế nào?
- JWT stateless authentication flow?
- CSRF là gì? REST API có cần CSRF protection không?

---

### 2.6 Testing (Tuần 12)

| Chủ đề | Nội dung |
|--------|----------|
| Unit Test | JUnit 5, `@Test`, `@BeforeEach`, `@AfterEach` |
| Mocking | Mockito: `@Mock`, `@InjectMocks`, `when()`, `verify()` |
| Spring Boot Test | `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest` |
| MockMvc | Test REST endpoints |
| Test Container | Integration test với real DB |
| Code Coverage | JaCoCo |

---

### 2.7 Caching, Messaging & DevOps cơ bản (Tuần 12–13)

| Chủ đề | Nội dung |
|--------|----------|
| Caching | `@Cacheable`, `@CacheEvict`, Redis integration |
| Messaging | Spring Kafka / RabbitMQ cơ bản |
| Actuator | Health check, metrics, `/actuator` endpoints |
| Docker | Dockerfile cho Spring Boot, docker-compose |
| CI/CD | GitHub Actions workflow cơ bản |
| Logging | SLF4J + Logback, structured logging |

---

## Phase 3 — Ôn tập & Chuẩn bị Phỏng vấn

### Checklist kỹ năng Middle Java Engineer

#### Java Core
- [ ] Giải thích được HashMap internal (hashCode, collision, resize)
- [ ] Viết được ThreadPool từ đầu dùng `ExecutorService`
- [ ] Debug được deadlock scenario
- [ ] Dùng được Stream API thành thạo
- [ ] Hiểu GC và tối ưu memory cơ bản

#### Spring Boot
- [ ] Xây được REST API CRUD hoàn chỉnh
- [ ] Implement JWT authentication
- [ ] Viết unit test + integration test cho Service và Controller
- [ ] Hiểu transaction và giải quyết N+1 problem
- [ ] Cấu hình được Redis cache
- [ ] Dockerize được ứng dụng

#### System Design (cơ bản)
- [ ] Thiết kế được REST API theo chuẩn
- [ ] Hiểu database indexing
- [ ] Biết khi nào dùng cache
- [ ] Hiểu cơ bản về microservices vs monolith

---

### Nguồn học tham khảo

| Tài nguyên | Link | Mục đích |
|-----------|------|---------|
| Baeldung | https://www.baeldung.com | Spring Boot, Java in-depth |
| Java Brains (YouTube) | Search "Java Brains" | Spring Boot video series |
| Effective Java (Book) | Joshua Bloch | Best practices |
| Spring Docs | https://docs.spring.io | Official reference |
| LeetCode | https://leetcode.com | Algorithm (nếu cần) |

---

### Dự án thực hành đề xuất

1. **Todo REST API** — CRUD, Pagination, Spring Data JPA, H2/PostgreSQL
2. **User Auth Service** — JWT, Refresh Token, Spring Security
3. **Product Catalog** — Redis cache, Pagination, File upload
4. **Mini E-commerce** — Microservices cơ bản, Kafka event, Docker Compose

---

## Timeline tổng hợp

| Tuần | Nội dung |
|------|----------|
| 1–2 | OOP, Generics, Exception, String, Memory |
| 2–3 | Collections, HashMap internals |
| 3–4 | Concurrency, Thread, ExecutorService |
| 4–5 | Stream API, Lambda, Optional |
| 5–6 | JVM, GC, Java 8–21 features |
| 7–8 | Spring Core, IoC, DI, AOP, Auto-config |
| 9 | Spring MVC, REST API, Validation |
| 10 | Spring Data JPA, Transaction, N+1 |
| 11 | Spring Security, JWT |
| 12 | Testing: JUnit, Mockito, MockMvc |
| 13 | Caching, Kafka cơ bản, Docker, Actuator |
| 14–16 | Ôn tập, mock interview, dự án tổng hợp |
