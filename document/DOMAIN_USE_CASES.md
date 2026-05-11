# TaskFlow — Domain Use Cases (Banking · E-Commerce · Logistics)

> Tổng hợp kỹ thuật từ [`TECHNICAL_GUIDE.md`](./TECHNICAL_GUIDE.md) (Mid-level đã làm) và [`SENIOR_ROADMAP.md`](./SENIOR_ROADMAP.md) (Senior chưa làm), kèm **ví dụ/bài toán cụ thể** cho 3 domain trọng điểm và **các kỹ thuật mới bổ sung** cần thiết cho từng domain.

---

## Mục lục

1. [Phần I — Bảng tổng hợp Technical × Domain](#phần-i--bảng-tổng-hợp-technical--domain)
2. [Phần II — Ví dụ cụ thể cho từng Technical hiện có](#phần-ii--ví-dụ-cụ-thể-cho-từng-technical-hiện-có)
   - [A. Java Core & Spring Foundation](#a-java-core--spring-foundation)
   - [B. Persistence & Performance](#b-persistence--performance)
   - [C. Security](#c-security)
   - [D. Caching & AOP](#d-caching--aop)
   - [E. Event-Driven & Resilience](#e-event-driven--resilience)
   - [F. Observability](#f-observability)
   - [G. Testing & DevOps](#g-testing--devops)
   - [H. Architecture Patterns](#h-architecture-patterns)
3. [Phần III — Technical mới bổ sung theo Domain](#phần-iii--technical-mới-bổ-sung-theo-domain)
4. [Phần IV — Lộ trình triển khai theo Domain](#phần-iv--lộ-trình-triển-khai-theo-domain)

---

## Phần I — Bảng tổng hợp Technical × Domain

Legend: 🔴 Critical · 🟡 Strongly recommended · 🟢 Optional

| # | Technical | 🏦 Banking | 🛒 E-Commerce | 🚚 Logistics |
|---|-----------|:----------:|:-------------:|:------------:|
| 1 | Records (DTO) | 🟡 | 🟡 | 🟡 |
| 2 | Virtual Threads | 🟡 | 🔴 | 🔴 |
| 3 | JPA Auditing | 🔴 (regulatory) | 🟡 | 🟡 |
| 4 | LAZY Fetch + JOIN FETCH | 🟡 | 🔴 | 🔴 |
| 5 | Flyway Migration | 🔴 | 🔴 | 🔴 |
| 6 | @Transactional Propagation | 🔴 (REQUIRES_NEW for audit) | 🟡 | 🟡 |
| 7 | Spring Security STATELESS + JWT | 🔴 (+ MFA/OAuth2) | 🔴 | 🔴 |
| 8 | Bean Validation + Custom Constraints | 🔴 | 🔴 | 🔴 |
| 9 | RFC 7807 ProblemDetail | 🟡 | 🟡 | 🟡 |
| 10 | @PreAuthorize | 🔴 (role + amount tier) | 🔴 | 🟡 |
| 11 | Pagination | 🟡 | 🔴 (catalog) | 🔴 (shipments) |
| 12 | Redis Cache | 🟡 (rate/FX) | 🔴 (catalog, cart) | 🟡 (route cache) |
| 13 | AOP Logging | 🟡 | 🟡 | 🟡 |
| 14 | AOP Audit (REQUIRES_NEW) | 🔴 (every txn) | 🟡 | 🟡 |
| 15 | Kafka Event-Driven | 🔴 | 🔴 | 🔴 |
| 16 | Testcontainers IT | 🔴 | 🔴 | 🔴 |
| 17 | Docker Multi-stage | 🔴 | 🔴 | 🔴 |
| 18 | GitHub Actions CI/CD | 🔴 | 🔴 | 🔴 |
| 19 | Actuator + Micrometer + Prometheus | 🔴 | 🔴 | 🔴 |
| 20 | @Version Optimistic Lock | 🟡 (or Pessimistic for balance) | 🔴 (inventory) | 🔴 (shipment status) |
| 21 | Composite Indexes | 🔴 | 🔴 | 🔴 |
| 22 | @EntityGraph (N+1 fix) | 🔴 | 🔴 | 🔴 |
| 23 | DTO Projection (CQRS read) | 🟡 | 🔴 (search) | 🔴 (dashboard) |
| 24 | HikariCP Tuning | 🔴 | 🔴 | 🔴 |
| 25 | Read Replica Routing | 🔴 (statement reports) | 🔴 (catalog browse) | 🔴 (tracking) |
| 26 | Resilience4j (CB, Retry, Bulkhead) | 🔴 (core banking, FX) | 🔴 (payment) | 🔴 (carrier API) |
| 27 | Outbox Pattern | 🔴 (txn → ledger event) | 🔴 (order → fulfillment) | 🔴 (shipment → tracker) |
| 28 | Idempotency Keys | 🔴 (every POST) | 🔴 (payment) | 🔴 (booking) |
| 29 | Distributed Lock (Redisson) | 🔴 (account lock) | 🟡 (flash-sale) | 🟡 (driver assign) |
| 30 | Graceful Shutdown | 🔴 | 🔴 | 🔴 |
| 31 | Distributed Tracing (OTel) | 🔴 | 🔴 | 🔴 |
| 32 | Structured Logging + MDC | 🔴 | 🔴 | 🔴 |
| 33 | Custom Health Indicators | 🟡 | 🟡 | 🟡 |
| 34 | API Rate Limiting (Bucket4j) | 🔴 (per user/IP) | 🔴 (anti-bot) | 🟡 |
| 35 | OAuth2 Resource Server | 🔴 (PSD2, OpenBanking) | 🔴 (SSO) | 🟡 |
| 36 | HTTP Security Headers + CSP | 🔴 | 🔴 | 🟡 |
| 37 | Encryption at Rest (PII) | 🔴 (PCI-DSS) | 🔴 (GDPR) | 🟡 |
| 38 | Secrets Management (Vault) | 🔴 | 🔴 | 🔴 |
| 39 | Hexagonal Architecture | 🔴 (core domain protection) | 🟡 | 🟡 |
| 40 | CQRS | 🟡 (account statement) | 🔴 (catalog) | 🔴 (live tracking) |
| 41 | API Versioning | 🔴 (partner SDK) | 🔴 (mobile app) | 🟡 |
| 42 | ArchUnit Rules | 🔴 | 🟡 | 🟡 |
| 43 | Mutation Testing (Pitest) | 🔴 (money logic) | 🟡 | 🟡 |
| 44 | Contract Testing (Pact) | 🔴 (partner banks) | 🔴 (mobile/web) | 🟡 (carriers) |
| 45 | Performance Testing (k6) | 🔴 | 🔴 (Black Friday) | 🔴 (peak season) |
| 46 | K8s + Helm + HPA | 🔴 | 🔴 | 🔴 |
| 47 | Zero-downtime DB Migration | 🔴 | 🔴 | 🔴 |
| 48 | JVM Tuning (G1/ZGC) | 🔴 (low p99) | 🔴 | 🟡 |
| **NEW** | **Bổ sung trong Phần III** | | | |
| 49 | **Saga Pattern** (Orchestration / Choreography) | 🔴 (money transfer) | 🔴 (order) | 🔴 (multi-leg shipment) |
| 50 | **Event Sourcing** | 🔴 (ledger) | 🟡 (cart history) | 🟡 (shipment journey) |
| 51 | **State Machine** (Spring Statemachine) | 🟡 (loan workflow) | 🔴 (order lifecycle) | 🔴 (shipment status) |
| 52 | **CDC Debezium** | 🔴 (anti-fraud streaming) | 🔴 (search sync) | 🟡 |
| 53 | **Multi-tenancy** | 🟡 (white-label) | 🔴 (marketplace sellers) | 🔴 (3PL clients) |
| 54 | **Database Sharding & Partitioning** | 🔴 (txn history) | 🔴 (orders) | 🔴 (tracking events) |
| 55 | **Time-Series DB** (TimescaleDB) | 🟡 (FX ticks) | 🟡 (clicks) | 🔴 (GPS events) |
| 56 | **WebSocket / SSE** (Real-time) | 🔴 (trading) | 🟡 (live stock, chat) | 🔴 (live tracking) |
| 57 | **BigDecimal Money Arithmetic** | 🔴 | 🔴 | 🟡 |
| 58 | **Full-text Search** (Elasticsearch / OpenSearch) | 🟡 (txn search) | 🔴 (product search) | 🟡 (waybill search) |
| 59 | **GraphQL** | 🟡 | 🔴 (mobile flexibility) | 🟡 |
| 60 | **Geospatial** (PostGIS, geohash) | 🟢 | 🟡 (delivery zone) | 🔴 (routing) |
| 61 | **TCC / 2PC vs Saga** | 🔴 (cross-bank) | 🟡 | 🟡 |
| 62 | **Reactive Streams** (WebFlux) | 🟡 (market data) | 🟡 | 🟡 (GPS ingest) |

---

# Phần II — Ví dụ cụ thể cho từng Technical hiện có

> Trong phần này, mỗi technical được map sang **bài toán cụ thể** trong 3 domain. Code chỉ là snippet minh hoạ ý tưởng — implementation đầy đủ tham khảo `TECHNICAL_GUIDE.md` và `SENIOR_ROADMAP.md`.

---

## A. Java Core & Spring Foundation

### A.1 — Records (DTO)

🏦 **Banking — Money Transfer Request**
```java
public record TransferRequest(
    @NotNull UUID fromAccountId,
    @NotNull UUID toAccountId,
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotBlank @Size(max = 3) String currency,
    @Size(max = 500) String memo,
    @NotNull UUID idempotencyKey      // chống double-spend khi retry
) {}
```

🛒 **E-Commerce — Add to Cart**
```java
public record AddToCartRequest(
    @NotNull UUID skuId,
    @Min(1) @Max(99) int quantity,
    @Size(max = 200) String giftMessage,
    UUID couponId
) {}
```

🚚 **Logistics — Create Shipment**
```java
public record CreateShipmentRequest(
    @NotNull AddressDto origin,
    @NotNull AddressDto destination,
    @NotNull @Size(min = 1) List<@Valid ParcelDto> parcels,
    @NotNull ServiceLevel serviceLevel,    // STANDARD/EXPRESS/SAME_DAY
    LocalDate requestedPickupDate
) {}
```

### A.2 — Virtual Threads (Java 21)

🏦 **Banking — Statement Generation**  
Mỗi tháng generate statements cho 10M tài khoản. Mỗi statement query nhiều bảng (transactions, fees, interest). Virtual Threads cho phép parallel I/O không tốn nhiều OS threads.
```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    accountIds.forEach(id -> executor.submit(() -> generateStatement(id)));
}
```

🛒 **E-Commerce — Bulk Order Sync**  
Sync 50k orders/giờ từ marketplace (Shopee/Lazada) sang internal system. Mỗi order: gọi 3-4 API (inventory, payment, fulfillment).

🚚 **Logistics — GPS Ping Ingestion**  
50k vehicle gửi GPS ping mỗi 5s = 600k req/phút. Mỗi ping: validate → enrich (geo-lookup) → persist → publish event. Virtual Threads ideal cho I/O-bound burst.

### A.3 — JPA Auditing (createdBy, updatedBy)

🏦 **Banking — Regulatory Audit Trail**  
SBV/Basel yêu cầu mọi thay đổi (account open, limit change, KYC update) phải có `created_by`, `updated_by`, `updated_at` + IP address + session ID. `BaseEntity` chuẩn hoá điều này.

🛒 **E-Commerce — Price Change History**  
Mọi thay đổi giá phải biết PM nào, lúc nào → tránh "ai sửa giá thành 0 đồng".

🚚 **Logistics — Driver Action Audit**  
Driver app: mọi action (pickup confirm, delivery confirm, exception report) auto-log who/when để xử lý dispute.

### A.4 — Entity Relationships & N+1

🏦 **Banking — Account & Transactions**  
`Account 1—N Transaction`. List 30 ngày txn của 1 account: dùng `@EntityGraph(attributePaths={"counterparty","fee"})` để 1 query trả về đủ data.

🛒 **E-Commerce — Order & OrderItems & SKU**  
Order detail page: load order + items + SKU + product. 1 order có 20 items → nếu LAZY thuần: 41 queries. Cần `JOIN FETCH order.items.sku.product`.

🚚 **Logistics — Shipment & TrackingEvents**  
Shipment có 10-50 tracking events. Display history: phải fetch sẵn để render timeline, tránh N+1.

### A.5 — Flyway Migration

🏦 **Banking — Schema Versioning Yêu cầu Bắt buộc**  
Audit team yêu cầu reproducible: bất kỳ DB state nào phải truy ngược về 1 set migrations cụ thể. Mỗi migration là 1 PR có review.

🛒 **E-Commerce — Add Column Without Downtime**  
Black Friday + 50M product rows: `ALTER TABLE products ADD COLUMN promo_label VARCHAR(50)` phải dùng Expand-Contract pattern (xem #47).

🚚 **Logistics — Multi-tenant Schema Evolution**  
Mỗi 3PL client = 1 schema riêng. Flyway cần chạy migration trên N schemas: dùng `flyway.schemas` placeholder.

### A.6 — @Transactional Propagation

🏦 **Banking — REQUIRES_NEW cho Audit**  
```java
@Transactional  // outer business transaction
public void transfer(...) {
    debitAccount(from, amount);
    creditAccount(to, amount);
    auditService.log(...);  // @REQUIRES_NEW → audit committed dù outer rollback
}
```

🛒 **E-Commerce — NESTED cho Step-by-step Checkout**  
Checkout: reserve inventory → process payment → create shipment. Mỗi bước có savepoint, fail bước 3 không rollback bước 1 (compensation needed — xem Saga).

🚚 **Logistics — REQUIRED cho Atomic Status Update**  
Update shipment status `IN_TRANSIT → DELIVERED` + tạo tracking event + notify customer → cùng 1 transaction để đảm bảo consistency.

---

## B. Persistence & Performance

### B.1 — @EntityGraph (N+1 Fix)

🏦 **Banking — Statement Query**
```java
@EntityGraph(attributePaths = {"counterparty", "category", "fee"})
@Query("SELECT t FROM Transaction t WHERE t.accountId = :id AND t.bookedAt BETWEEN :from AND :to")
Page<Transaction> findStatement(UUID id, Instant from, Instant to, Pageable p);
```

🛒 **E-Commerce — Order Detail**
```java
@EntityGraph(attributePaths = {"items", "items.sku", "shippingAddress", "billingAddress"})
Optional<Order> findById(UUID id);
```

🚚 **Logistics — Shipment with Events**
```java
@EntityGraph(attributePaths = {"events", "parcels", "carrier"})
Optional<Shipment> findByTrackingNumber(String trackingNumber);
```

### B.2 — DTO Projection (CQRS Read)

🏦 **Banking — Mini Statement** (chỉ 5 fields cho mobile widget):
```sql
SELECT new com.bank.dto.MiniStatementItem(t.id, t.bookedAt, t.amount, t.currency, t.description)
FROM Transaction t WHERE t.accountId = :id ORDER BY t.bookedAt DESC LIMIT 10
```

🛒 **E-Commerce — Catalog Listing** (không load price history, reviews):
```sql
SELECT new com.shop.dto.ProductCard(p.id, p.name, p.thumbnailUrl, p.priceVnd, p.rating)
FROM Product p WHERE p.categoryId = :catId AND p.status = 'ACTIVE'
```

🚚 **Logistics — Live Dashboard** (tối ưu cho 10k shipments render):
```sql
SELECT new com.logi.dto.ShipmentRow(s.id, s.trackingNumber, s.status, s.eta, c.name)
FROM Shipment s JOIN s.carrier c WHERE s.status IN ('IN_TRANSIT','OUT_FOR_DELIVERY')
```

### B.3 — @Version Optimistic Locking

🏦 **Banking — Account Limit Change (low conflict)**  
Limit ít khi sửa song song → optimistic OK. Nếu conflict: 409 + UI reload.  
*Lưu ý:* Balance update phải dùng **pessimistic lock** hoặc atomic UPDATE (xem mục #61).

🛒 **E-Commerce — Inventory Decrement** (high conflict, flash-sale)  
1000 user click "Mua" cùng lúc cho 10 stock cuối → optimistic lock retry rất nhiều.  
**Tốt hơn:** Atomic SQL `UPDATE inventory SET stock = stock - 1 WHERE sku_id = ? AND stock > 0` hoặc Redis decrement.

🚚 **Logistics — Shipment Status Transition**  
2 driver cùng confirm delivery → optimistic version check ngăn double-confirm.

### B.4 — Composite & Partial Indexes

🏦 **Banking — Hot Query Pattern**
```sql
-- Account statement: by account + date range
CREATE INDEX idx_txn_account_booked ON transactions(account_id, booked_at DESC);

-- Anti-fraud lookup: by card + last 24h
CREATE INDEX idx_txn_card_recent ON transactions(card_id, booked_at)
  WHERE booked_at > NOW() - INTERVAL '24 hours';   -- partial
```

🛒 **E-Commerce**
```sql
-- Product search by category + status + price
CREATE INDEX idx_products_cat_status_price ON products(category_id, status, price_vnd)
  WHERE status = 'ACTIVE';

-- Order history: by customer + date
CREATE INDEX idx_orders_customer_created ON orders(customer_id, created_at DESC);
```

🚚 **Logistics**
```sql
-- Active shipments by warehouse
CREATE INDEX idx_shipments_warehouse_status ON shipments(warehouse_id, status)
  WHERE status NOT IN ('DELIVERED', 'CANCELLED', 'RETURNED');

-- Geospatial: nearby drivers
CREATE INDEX idx_drivers_location ON drivers USING GIST (location)
  WHERE status = 'AVAILABLE';
```

### B.5 — HikariCP Production Tuning

🏦 **Banking — Critical Low-Latency**
```yaml
hikari:
  maximum-pool-size: 30
  connection-timeout: 2000      # fail fast — không để user chờ
  validation-timeout: 1000
  leak-detection-threshold: 30000
```

🛒 **E-Commerce — Bursty Traffic**
```yaml
hikari:
  maximum-pool-size: 50         # peak season
  minimum-idle: 20              # warm pool
  connection-timeout: 5000
```

🚚 **Logistics — Many Background Jobs**
```yaml
hikari:
  maximum-pool-size: 25
  max-lifetime: 900000          # 15 phút — connection rotation thường xuyên
```

### B.6 — Read Replica Routing

🏦 **Banking — Statement Generation → Replica**  
Reports/statements không cần real-time → route sang replica → giảm tải primary.  
**Cảnh báo:** Balance check phải đi primary (replication lag → user thấy số dư cũ → over-spend).

🛒 **E-Commerce — Product Browse → Replica**  
99% traffic là browse/search → replica handle. Checkout đi primary.

🚚 **Logistics — Tracking Lookup → Replica**  
Customer track 1 shipment: lag 1-2s chấp nhận được → replica.

---

## C. Security

### C.1 — Spring Security STATELESS + JWT

🏦 **Banking — JWT + MFA + Step-up Authentication**
- Login → JWT (15 phút, ngắn)
- Giao dịch > 50M VND → require step-up (OTP/biometric) → JWT mới có claim `mfa_verified: true`
- Public APIs (FX rate) không cần auth

🛒 **E-Commerce — Anonymous Cart + Guest Checkout**
- Guest: JWT với `role: GUEST`, TTL 7 ngày (cart persist)
- Member: JWT với roles, refresh token rotation
- Admin panel: JWT + IP allowlist

🚚 **Logistics — Multi-app Tokens**
- Driver app: JWT có claim `driver_id`, `vehicle_id`, `route_id`
- Customer app: JWT chuẩn
- Carrier partner API: OAuth2 client credentials (xem mục #35)

### C.2 — Bean Validation + Custom Constraints

🏦 **Banking — Money Field Constraints**
```java
@Constraint(validatedBy = MoneyValidator.class)
public @interface ValidMoney {
    String[] currencies() default {"VND", "USD", "EUR"};
    int maxDecimalPlaces() default 2;
}

public record TransferRequest(
    @ValidMoney(currencies = {"VND"}, maxDecimalPlaces = 0) BigDecimal amount,  // VND: no decimals
    ...
) {}
```

🛒 **E-Commerce — SKU Stock Availability**
```java
@StockAvailable(skuField = "skuId", quantityField = "quantity")
public record AddToCartRequest(UUID skuId, int quantity) {}
```

🚚 **Logistics — Address Validation (Geo-coding)**
```java
@ValidPostalCode(country = "VN")
@CoordinatesWithinCountry("VN")
public record AddressDto(String street, String city, String postalCode, double lat, double lng) {}
```

### C.3 — RFC 7807 ProblemDetail

🏦 **Banking — Insufficient Funds**
```json
{
  "type": "https://api.bank.com/errors/insufficient-funds",
  "title": "Insufficient Funds",
  "status": 422,
  "detail": "Account balance 100,000 VND is less than transfer amount 500,000 VND",
  "instance": "/api/transfers",
  "accountId": "550e8400...",
  "availableBalance": 100000,
  "requestedAmount": 500000
}
```

🛒 **E-Commerce — Out of Stock**
```json
{
  "type": "https://api.shop.com/errors/out-of-stock",
  "title": "Out of Stock",
  "status": 409,
  "detail": "SKU SKU-001 has only 2 items left, requested 5",
  "skuId": "...",
  "available": 2,
  "requested": 5
}
```

🚚 **Logistics — Service Unavailable in Area**
```json
{
  "type": "https://api.logi.com/errors/service-unavailable-area",
  "title": "Service Not Available",
  "status": 422,
  "detail": "Same-day delivery not available for postal code 70000",
  "postalCode": "70000",
  "availableLevels": ["STANDARD", "EXPRESS"]
}
```

### C.4 — @PreAuthorize (Method-level Security)

🏦 **Banking — Amount-tier Authorization**
```java
@PreAuthorize("@txnSecurity.canApprove(#req.amount(), authentication)")
public TransferResponse approveTransfer(@Valid TransferRequest req) { ... }

// Bean:
public boolean canApprove(BigDecimal amount, Authentication auth) {
    return switch (userLevel(auth)) {
        case TELLER -> amount.compareTo(new BigDecimal("10000000")) <= 0;  // 10M VND
        case SUPERVISOR -> amount.compareTo(new BigDecimal("100000000")) <= 0;
        case MANAGER -> true;
    };
}
```

🛒 **E-Commerce — Seller Owns Product**
```java
@PreAuthorize("@productSecurity.isOwner(#productId, authentication)")
public ProductResponse updateProduct(UUID productId, ...) { ... }
```

🚚 **Logistics — Driver Assigned to Shipment**
```java
@PreAuthorize("@shipmentSecurity.isAssignedDriver(#shipmentId, authentication)")
public void confirmDelivery(UUID shipmentId, ...) { ... }
```

---

## D. Caching & AOP

### D.1 — Redis @Cacheable

🏦 **Banking — FX Rates (rarely changes, hot read)**
```java
@Cacheable(value = "fxRates", key = "#from + '-' + #to")
public BigDecimal getRate(String from, String to) { ... }
// TTL: 60s (rate update mỗi phút)
```

🛒 **E-Commerce — Product Detail Page (hot SKUs)**
```java
@Cacheable(value = "products", key = "#sku", unless = "#result.outOfStock()")
public ProductResponse getBySku(String sku) { ... }
// TTL: 10 phút, evict khi update giá/stock
```

🚚 **Logistics — Route Plan Cache (compute-expensive)**
```java
@Cacheable(value = "routes", key = "#origin + '|' + #dest")
public RoutePlan plan(String origin, String dest) { ... }
// TTL: 1 giờ (traffic không đổi quá nhanh)
```

### D.2 — AOP Audit (REQUIRES_NEW)

🏦 **Banking — Every Transaction Audited**  
Mọi method có `@Auditable("MONEY_MOVEMENT")` → log async vào audit DB schema riêng, không rollback dù business fail.

🛒 **E-Commerce — Admin Actions**  
Admin sửa giá / xoá order / refund → audit log với before/after diff.

🚚 **Logistics — Status Override**  
Khi ops manual override status (vd: mark DELIVERED bằng tay) → audit ai/khi/lý do.

---

## E. Event-Driven & Resilience

### E.1 — Kafka Topics by Domain

🏦 **Banking — Topic Design**
```
account.opened            (compacted, infinite retention)
transaction.posted        (partitioned by accountId, retention 7 days → archive S3)
fraud.alert               (high priority)
fx.rate.updated           (every 1s, compacted)
```

🛒 **E-Commerce — Topic Design**
```
order.created
order.paid
order.fulfilled
inventory.reserved        (Saga)
inventory.released
product.priceChanged      (sync to search, cache)
```

🚚 **Logistics — Topic Design**
```
shipment.created
shipment.statusChanged    (partitioned by trackingNumber)
gps.ping                  (high volume, retention 24h)
exception.reported        (driver damage/delay reports)
```

### E.2 — Outbox Pattern

🏦 **Banking — Ledger Event Outbox** (CRITICAL)
- DB commit: `INSERT transaction + INSERT outbox(LedgerPosted)`
- Poller publish to Kafka → BigData lake → reports
- **Không bao giờ** mất event (compliance!)

🛒 **E-Commerce — Order → Fulfillment**
- Order paid → outbox `OrderPaid` → consumer trigger warehouse pick → labels print
- Nếu Kafka chậm: outbox không mất, retry tự động

🚚 **Logistics — Shipment → Carrier API**
- Status change → outbox → poller call carrier webhook
- Carrier API down? Poller retry với exponential backoff

### E.3 — Idempotency Keys

🏦 **Banking — Mobile Banking Retry**
```http
POST /api/transfers
Idempotency-Key: 9f8b1c2d-mobile-app-uuid
```
User mất mạng, app retry tự động → server thấy key đã processed → return cached response → KHÔNG transfer 2 lần.

🛒 **E-Commerce — Payment Submit**  
Cùng `Idempotency-Key` cho stripe/momo callback → ngăn double-charge khi gateway retry webhook.

🚚 **Logistics — Booking Submit**  
Idempotency key cho create-shipment → tránh duplicate khi network glitch.

### E.4 — Resilience4j

🏦 **Banking — FX Service Circuit Breaker**
```java
@CircuitBreaker(name = "fxService", fallbackMethod = "lastKnownRate")
public BigDecimal getCurrentRate(String pair) { ... }

private BigDecimal lastKnownRate(String pair, Throwable t) {
    return fxCacheRepository.findLastRate(pair);  // serve stale, but available
}
```

🛒 **E-Commerce — Payment Gateway Retry + Bulkhead**
```java
@Retry(name = "stripe", fallbackMethod = "queueForLater")
@Bulkhead(name = "stripe", maxConcurrent = 50)   // tránh saturate Stripe API
public PaymentResult charge(ChargeRequest req) { ... }
```

🚚 **Logistics — Carrier API Circuit Breaker**  
GHTK/GHN API có thể chậm → fallback dùng cached price + scheduled retry.

### E.5 — Distributed Lock (Redisson)

🏦 **Banking — Single-instance Daily Interest Job**
```java
@Scheduled(cron = "0 0 1 * * *")  // 1 AM
public void calculateDailyInterest() {
    RLock lock = redisson.getLock("job:daily-interest");
    if (lock.tryLock(0, 4, TimeUnit.HOURS)) {
        try { processInterest(); } finally { lock.unlock(); }
    }
}
```

🛒 **E-Commerce — Flash-sale Throttle**  
Pre-acquire lock per SKU để serialize stock decrement (alternative cho atomic SQL).

🚚 **Logistics — Driver Assignment**  
Optimizer chỉ chạy trên 1 instance để tránh tranh dispatch cùng 1 driver cho 2 shipment.

---

## F. Observability

### F.1 — Distributed Tracing (OTel + Jaeger)

🏦 **Banking — Money Transfer Trace**
```
[HTTP POST /api/transfers]
  ├─ [Auth: verify JWT + MFA]
  ├─ [Fraud check (Redis)]
  ├─ [DB: debit + credit + outbox] (single TX)
  ├─ [Outbox → Kafka publish]
  └─ [Kafka → Consumer: post to GL ledger]
```
Khi report "transfer chậm" — Jaeger ngay lập tức cho thấy fraud check 800ms = bottleneck.

🛒 **E-Commerce — Checkout Trace**  
Cart → Inventory reserve → Payment → Order create → Fulfillment trigger → Email. Mỗi step trên 1 service riêng — trace ID đi xuyên suốt.

🚚 **Logistics — Shipment Tracking**  
Customer query: status query lookup multi-tier (cache → primary → carrier API). Trace cho thấy fallback chain.

### F.2 — Structured Logging + MDC

🏦 **Banking — Required Fields trong Audit**
MDC: `requestId`, `userId`, `accountId` (masked: `***1234`), `txnId`, `ip`, `deviceId`, `traceId`.  
Compliance: search "all transactions of customer X today" by `customerId` in Elasticsearch.

🛒 **E-Commerce — Customer Journey**
MDC: `sessionId`, `customerId`, `cartId`, `orderId`, `traceId`. Phân tích funnel drop-off bằng log mining.

🚚 **Logistics — Tracking Number as Primary Key**
MDC bắt buộc: `trackingNumber`, `shipmentId`, `driverId` → CS search 1 dòng tracking thấy toàn bộ lifecycle.

---

## G. Testing & DevOps

### G.1 — Mutation Testing (Pitest)

🏦 **Banking — CRITICAL cho Money Logic**  
Coverage 100% không đảm bảo đúng. Mutation test xác minh test thực sự phát hiện bug khi code thay đổi `>` thành `>=` (boundary).
```bash
mvn test pitest:mutationCoverage -DtargetClasses=com.bank.txn.*
# mutationThreshold: 90% cho money modules
```

🛒 **E-Commerce — Pricing/Discount Logic**  
Mutation test cho discount calculator: nếu test không catch `subtotal * 0.9` → `subtotal * 0.95` → test yếu.

🚚 **Logistics — ETA Calculator**  
ETA logic phức tạp (distance + traffic + service level) → mutation test ngăn regression.

### G.2 — Contract Testing (Pact)

🏦 **Banking — Partner Banks via PSD2**  
Mỗi partner bank là consumer của TaskBank API → Pact đảm bảo update API không break partner integration.

🛒 **E-Commerce — Mobile App ↔ Backend**  
iOS team viết Pact "tôi expect product detail response như này" → backend verify mỗi PR → không break mobile.

🚚 **Logistics — Carrier Integrations**  
GHTK/GHN/J&T mỗi bên có API contract → Pact catalogue cho phép verify bilaterally.

### G.3 — Performance Testing (k6)

🏦 **Banking — Stress Test Money Transfer**
```javascript
export const options = {
    stages: [
        { duration: '5m',  target: 1000 },
        { duration: '20m', target: 1000 },
    ],
    thresholds: {
        http_req_duration: ['p(99)<500'],  // strict p99 cho banking
        http_req_failed:   ['rate<0.001'],
    },
};
```

🛒 **E-Commerce — Black Friday Simulation**
Spike test: 100 → 10,000 → 50,000 concurrent users. Verify HPA scales, DB connection pool không OOM.

🚚 **Logistics — Peak Season (Tết, 11.11)**
Soak test 7 ngày 5000 concurrent shipment creates/giờ. Detect memory leaks, connection leak.

---

## H. Architecture Patterns

### H.1 — Hexagonal Architecture

🏦 **Banking — Core Domain Bảo vệ**
```
Domain: Account, Transaction, Money (Value Object)
Ports IN: TransferUseCase, OpenAccountUseCase
Ports OUT: AccountRepository, LedgerPublisher, FraudCheckClient
Adapters: JPA / Kafka / REST clients
```
Domain `Money.subtract()` không phụ thuộc Spring/JPA → test thuần Java, swap storage không sửa logic.

🛒 **E-Commerce — Order Aggregate**  
Domain: `Order`, `OrderItem`, `Pricing`. Tách rời khỏi catalog/inventory service → mỗi bounded context tự do.

🚚 **Logistics — Shipment Aggregate**  
Domain: `Shipment`, `Route`, `Parcel`. Adapters: GHTK adapter, GHN adapter (mỗi carrier 1 adapter) → swap carrier không sửa core.

### H.2 — CQRS

🏦 **Banking — Account Statement (read-heavy)**
- Command side: `Transaction` aggregate, normalized 3NF
- Query side: `account_statement_view` (materialized view, denormalized) → query siêu nhanh

🛒 **E-Commerce — Catalog**
- Command: Product master DB (PostgreSQL)
- Query: Elasticsearch index (denormalized: product + category + facets + reviews)

🚚 **Logistics — Live Tracking Dashboard**
- Command: Shipment writes → PostgreSQL
- Query: Materialized view trong Redis hoặc TimescaleDB cho 100k shipments live

---

# Phần III — Technical mới bổ sung theo Domain

## 49. Saga Pattern (Orchestration & Choreography)

### Khi nào cần?

Distributed transaction across services không thể 2PC (Two-Phase Commit) → cần **eventual consistency** với **compensating actions**.

### Choreography (decentralized)
```
[Order Service]  → OrderCreated → [Inventory Service]
                                       ↓
                                  InventoryReserved → [Payment Service]
                                                          ↓
                                                     PaymentSucceeded → [Shipping Service]
                                                                            ↓
                                                                       ShipmentScheduled
Failure: PaymentFailed → [Inventory Service] InventoryReleased → [Order Service] OrderCancelled
```

### Orchestration (centralized)
```java
@Component
public class OrderSagaOrchestrator {
    public void start(OrderCreatedEvent evt) {
        try {
            inventoryClient.reserve(evt.orderId(), evt.items());
            paymentClient.charge(evt.orderId(), evt.amount());
            shippingClient.schedule(evt.orderId());
        } catch (PaymentException e) {
            // Compensating actions in reverse order
            inventoryClient.release(evt.orderId());
            orderClient.markFailed(evt.orderId());
        }
    }
}
```

### Domain Examples

🏦 **Banking — Cross-Bank Wire Transfer**
```
1. Reserve funds (source bank)
2. Send SWIFT/NAPAS message
3. Wait for confirmation (correspondent bank)
4. Credit beneficiary (destination bank)

Compensation: Refund + cancel SWIFT + notify customer
```
**Recommendation:** Orchestration với explicit saga state machine (auditable).

🛒 **E-Commerce — Order Saga** (kinh điển)
```
ReserveInventory → ProcessPayment → CreateShipment → SendNotification
Compensations: Refund, ReleaseInventory, NotifyFailure
```

🚚 **Logistics — Multi-leg Shipment**
```
PickupConfirm (leg 1: SaigonHub)
TransitConfirm (leg 2: HanoiHub)
DeliveryConfirm (leg 3: customer)

Failure at leg 2: ReturnToHub + RefundCustomer + NotifyShipper
```

### Implementation Frameworks
- **Spring Statemachine** (orchestration-ish)
- **Axon Framework** (Saga + Event Sourcing)
- **Camunda** (BPMN workflow engine)
- **Custom** với Outbox pattern + state machine (most common in startup)

---

## 50. Event Sourcing

### Khi nào cần?

Lưu **danh sách event** thay vì state hiện tại. State = replay events. Phù hợp khi:
- Cần audit trail đầy đủ (compliance)
- Cần "time travel" (undo, what-if analysis)
- Multiple read models (CQRS friendly)

```
Traditional:
  UPDATE account SET balance = 100 WHERE id = X    -- mất history

Event Sourcing:
  events = [
    AccountOpened(balance=0),
    Deposited(50),
    Deposited(100),
    Withdrawn(50)
  ]
  balance = replay(events) = 100
```

### Domain Examples

🏦 **Banking — Account Ledger** (kinh điển — bank ledger là event sourcing tự nhiên)
```java
// Event Store
record AccountOpened(UUID id, String holder, Instant at) {}
record MoneyDeposited(UUID id, BigDecimal amount, String ref, Instant at) {}
record MoneyWithdrawn(UUID id, BigDecimal amount, String ref, Instant at) {}

// Replay to compute balance at any point in time
BigDecimal balanceAt(UUID accountId, Instant t) {
    return eventStore.eventsFor(accountId, t).stream()
        .map(Event::delta)
        .reduce(ZERO, BigDecimal::add);
}
```

🛒 **E-Commerce — Cart Activity History**
Track customer journey: `ItemAdded`, `ItemRemoved`, `CouponApplied`, `CheckoutAttempted`. Useful cho abandoned cart analysis + recommendations.

🚚 **Logistics — Shipment Journey** (perfect fit)
```
events = [
  ShipmentCreated, PickedUp, ArrivedAtHub(SGN),
  DepartedHub(SGN), ArrivedAtHub(HAN),
  OutForDelivery, DeliveryAttemptFailed(reason="customer not home"),
  Rescheduled, Delivered
]
```
Display tracking timeline = replay events.

### Tools
- **Axon Framework** (full ES + CQRS)
- **EventStoreDB** (purpose-built DB)
- **Kafka + Compacted Topics** (event log as DB)
- **PostgreSQL** with `events` table (simple, most projects)

---

## 51. State Machine (Spring Statemachine)

### Khi nào cần?

Entity có **lifecycle phức tạp** với rules về transition. Hard-coding `if/else` ở mọi service method → bug + khó test.

```java
// ❌ Implicit state machine — fragile
public void updateStatus(Order o, OrderStatus newStatus) {
    if (o.getStatus() == CREATED && newStatus == PAID) { ... }
    else if (o.getStatus() == PAID && newStatus == SHIPPED) { ... }
    else if (o.getStatus() == PAID && newStatus == CANCELLED) { ... }
    // dozens of if/else, easy to miss case
}

// ✅ Explicit state machine
@Configuration
@EnableStateMachine
public class OrderStateConfig {
    void configure(StateMachineTransitionConfigurer<OrderStatus, OrderEvent> t) {
        t.withExternal().source(CREATED).target(PAID).event(PAY)
         .and().withExternal().source(PAID).target(SHIPPED).event(SHIP)
         .and().withExternal().source(PAID).target(CANCELLED).event(CANCEL)
         .and().withExternal().source(SHIPPED).target(DELIVERED).event(DELIVER)
         .and().withExternal().source(DELIVERED).target(RETURNED).event(RETURN);
    }
}
```

### Domain Examples

🏦 **Banking — Loan Application Workflow**
```
SUBMITTED → DOCS_VERIFIED → CREDIT_CHECK → APPROVED → DISBURSED → ACTIVE → PAID_OFF
                ↓                ↓
            REJECTED        REJECTED
```

🛒 **E-Commerce — Order Lifecycle** (most common use case)
```
CART → CHECKOUT → PAID → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
                    ↓        ↓             ↓           ↓
                CANCELLED CANCELLED    CANCELLED   RETURNED
```

🚚 **Logistics — Shipment Status Machine** (perfect)
```
CREATED → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
              ↓            ↓                ↓              ↓
          CANCELLED   ↓              FAILED_ATTEMPT    RETURNED
                  EXCEPTION → IN_TRANSIT (resume)
                              ↓
                          RETURNED_TO_SENDER
```

### Benefits
- Transition rules là **data** (config), không phải code
- Visualize được (Spring Statemachine có graphviz exporter)
- Test transitions độc lập với business logic
- Add guards: `t.withExternal().source(PAID).target(SHIPPED).guard(inventoryAvailable)`

---

## 52. CDC (Change Data Capture) — Debezium

### Khi nào cần?

Sync data **out-of-band** từ primary DB sang downstream (search engine, data warehouse, cache) mà không cần code dual-write trong service.

```
PostgreSQL WAL (Write-Ahead Log)
       ↓
   Debezium (reads WAL)
       ↓
   Kafka topic (db.public.products)
       ↓
  ┌────┴────┬─────────┐
  ↓         ↓         ↓
ES Sync   Cache    Data Lake
```

### Domain Examples

🏦 **Banking — Real-time Anti-Fraud**
Mỗi `INSERT INTO transactions` → CDC → Kafka → Flink/Spark streaming → ML model scoring → alert nếu suspicious.  
**Tránh dual-write:** không cần service code publish event ngoài lưu DB.

🛒 **E-Commerce — Catalog → Elasticsearch Sync**
Product update trong PostgreSQL → Debezium → Kafka → ES indexer → search reflects ngay (lag ~100ms).

🚚 **Logistics — Operational Data Lake**
Shipment events → CDC → S3 parquet files → Athena/BigQuery cho BI analytics, không ảnh hưởng OLTP.

### vs Outbox Pattern

| Aspect | Outbox | CDC (Debezium) |
|--------|--------|---------------|
| Code change | Cần thêm `outboxRepo.save()` | Không (transparent) |
| Setup ops | Đơn giản (just table + poller) | Phức tạp (Kafka Connect cluster) |
| Latency | ~1s polling | ~100ms (streaming) |
| Event shape | Custom domain events | Generic DB change events |
| Use khi | Bắt đầu, project nhỏ | Scale lớn, nhiều downstream |

---

## 53. Multi-tenancy

### 3 Strategies

| Strategy | Isolation | Cost | Complexity |
|----------|-----------|------|-----------|
| 1. Database per tenant | Strongest | Highest (N DBs) | Low-Medium |
| 2. Schema per tenant | Strong | Medium | Medium (Hibernate `MultiTenantConnectionProvider`) |
| 3. Row per tenant (`tenant_id` column) | Weakest | Lowest | Lowest (Hibernate `@Filter` or `@Where`) |

### Domain Examples

🏦 **Banking — White-label Banking Platform**
TaskBank serves 10 fintech clients (white-label). Strategy: **Schema per tenant** — strong isolation cho compliance audit per tenant.

🛒 **E-Commerce — Marketplace (Shopify-style)**
1M+ sellers. Strategy: **Row-level** với `seller_id` column. Mọi query filter bởi Hibernate global filter.
```java
@Entity
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = "uuid"))
@Filter(name = "tenantFilter", condition = "seller_id = :tenantId")
public class Product { ... }

// Set ở filter:
session.enableFilter("tenantFilter").setParameter("tenantId", currentTenant());
```

🚚 **Logistics — 3PL Multi-client Platform**
3PL serves 100 retail clients. Strategy: **Schema per client** for top-tier clients (medium volume), **row-level** for SMB.

### Tenant Routing

```java
public class TenantInterceptor implements HandlerInterceptor {
    public boolean preHandle(HttpServletRequest req, ...) {
        String tenantId = req.getHeader("X-Tenant-Id");  // or parse from JWT
        TenantContext.set(tenantId);
        return true;
    }
}
```

---

## 54. Database Sharding & Table Partitioning

### Partitioning (single DB, multiple physical tables)

🏦 **Banking — Transactions by Month**
```sql
CREATE TABLE transactions (...) PARTITION BY RANGE (booked_at);
CREATE TABLE transactions_2026_01 PARTITION OF transactions
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
-- Query WHERE booked_at = '2026-01-15' → partition pruning → chỉ scan 1 partition
```

🚚 **Logistics — GPS Events by Day** (high volume)
```sql
CREATE TABLE gps_events (...) PARTITION BY RANGE (recorded_at);
-- Tự động drop partition > 30 ngày để giữ DB nhỏ
```

### Sharding (across DBs)

🛒 **E-Commerce — Orders Sharded by customer_id hash**
```
Shard 0: customer_id hash % 4 == 0 → DB-0
Shard 1: customer_id hash % 4 == 1 → DB-1
Shard 2: ... DB-2
Shard 3: ... DB-3
```
**Trade-off:** Cross-shard query (admin search by email) phải scatter-gather.

**Tools:** Vitess (MySQL sharding), Citus (PostgreSQL distributed), ShardingSphere.

---

## 55. Time-Series Database (TimescaleDB)

### Khi nào cần?

Time-stamped data với high write volume + time-range queries.

🏦 **Banking — FX Tick Data**
Mỗi cặp tiền tệ ~1 tick/giây = 100k pairs × 86400s/day = 8.6B rows/day.

🛒 **E-Commerce — User Click Stream**
Page views, clicks, scroll events → analytics dashboards.

🚚 **Logistics — GPS Telemetry** (perfect fit)
```sql
-- TimescaleDB hypertable
SELECT create_hypertable('gps_events', 'recorded_at',
    chunk_time_interval => INTERVAL '1 day');

-- Continuous aggregate cho dashboard
CREATE MATERIALIZED VIEW vehicle_hourly_avg
WITH (timescaledb.continuous) AS
SELECT vehicle_id, time_bucket('1 hour', recorded_at) AS hour,
       AVG(speed) AS avg_speed, COUNT(*) AS pings
FROM gps_events
GROUP BY vehicle_id, hour;
```

### Alternatives
- **InfluxDB** (purpose-built)
- **ClickHouse** (columnar, analytics)
- **AWS Timestream** (managed)

---

## 56. WebSocket / Server-Sent Events

### Khi nào cần?

Server → client push (chứ không phải poll).

🏦 **Banking — Trading Platform**
Real-time price ticks + order book updates. Latency cực thấp (<10ms).
```java
@MessageMapping("/subscribe/{pair}")
public Flux<PriceTick> subscribe(@DestinationVariable String pair) {
    return priceStream.filter(t -> t.pair().equals(pair));
}
```

🛒 **E-Commerce — Live Inventory + Chat**
- Flash-sale: WebSocket push "still 5 left!" → all viewers
- Customer chat: bidirectional WS

🚚 **Logistics — Live Map Tracking** (killer feature)
```java
// SSE for tracking page (server → browser, no need bidi)
@GetMapping(value = "/api/shipments/{id}/track", produces = TEXT_EVENT_STREAM_VALUE)
public Flux<TrackingUpdate> streamUpdates(@PathVariable UUID id) {
    return Flux.merge(
        currentSnapshot(id),
        kafkaUpdatesFor(id).map(this::toTrackingUpdate)
    );
}
```

### Implementation
- **STOMP over WebSocket** (Spring): bidirectional, msg routing
- **SSE**: simpler, server→client only, HTTP/2 friendly
- **Socket.io** (frontend pairing)

---

## 57. BigDecimal Money Arithmetic

### Tại sao bắt buộc trong Banking?

```java
double a = 0.1, b = 0.2;
System.out.println(a + b);   // → 0.30000000000000004  WRONG!

BigDecimal x = new BigDecimal("0.1");
BigDecimal y = new BigDecimal("0.2");
System.out.println(x.add(y));  // → 0.3  CORRECT
```

### Best Practices

```java
// ✅ Init from String (not double!)
BigDecimal amount = new BigDecimal("100.50");

// ✅ Specify scale + rounding mode explicitly
BigDecimal vat = amount.multiply(new BigDecimal("0.1"))
                       .setScale(2, RoundingMode.HALF_UP);

// ✅ Use Money library for stronger type safety
import javax.money.Monetary;
import org.javamoney.moneta.Money;

Money price = Money.of(100.50, "VND");
Money total = price.add(Money.of(50.00, "VND"));
```

🏦 **Banking — Mandatory cho mọi money field**
```java
@Entity
public class Transaction {
    @Column(precision = 19, scale = 4)  // 19 digits, 4 decimals (e.g., crypto)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;
}
```

🛒 **E-Commerce — Pricing, tax, discount**
```java
// Discount: 10% off
BigDecimal discounted = price.multiply(new BigDecimal("0.9"))
                              .setScale(0, RoundingMode.HALF_UP);  // VND no decimals
```

🚚 **Logistics — Shipping fees** (multi-currency)
Same rules — use Money/BigDecimal cho cross-border shipments.

---

## 58. Full-text Search (Elasticsearch / OpenSearch)

### Khi nào PostgreSQL không đủ?
- Search natural language ("áo thun nam đỏ size L")
- Faceted search (filter by brand, price range, rating)
- Typo tolerance ("samsng" → "samsung")
- Synonyms ("phone" = "điện thoại")
- Aggregations on the fly

### Domain Examples

🏦 **Banking — Transaction Memo Search**
"Tìm tất cả giao dịch có chứa 'Grab' trong 3 tháng" — Elasticsearch index on `memo` field with VN tokenizer.

🛒 **E-Commerce — Product Search** (must-have)
```json
POST /products/_search
{
  "query": {
    "multi_match": {
      "query": "áo thun nam đỏ",
      "fields": ["name^3", "description", "tags"],
      "fuzziness": "AUTO"
    }
  },
  "aggs": {
    "brands": { "terms": { "field": "brand.keyword" } },
    "price_ranges": { "range": { "field": "price",
        "ranges": [{"to":100000},{"from":100000,"to":500000},{"from":500000}] } }
  }
}
```

🚚 **Logistics — Waybill / Customer Search**
CS search by recipient name, phone, address → ES with N-gram analyzer.

### Sync Strategy (đã đề cập #52)
- **Dual write** (bad): code phải nhớ
- **Outbox** (good): explicit events
- **CDC Debezium** (best): transparent, no code change

---

## 59. GraphQL

### Khi nào cần?

- Mobile/web client cần **chọn field linh hoạt** (mobile cần ít, web nhiều)
- N+1 queries thường xuyên với REST → GraphQL batching (DataLoader)
- Multiple clients với data needs khác nhau

### Domain Examples

🏦 **Banking — Mobile App Optimization**
Mobile lock screen widget chỉ cần `balance + lastTxn` → GraphQL query 2 fields, không over-fetch 30 fields như REST.

🛒 **E-Commerce — Mobile vs Web vs PoS** (best fit)
```graphql
query ProductPage($id: ID!) {
    product(id: $id) {
        name, price, images { url, alt }
        reviews(first: 5) { rating, body, author { displayName } }
        relatedProducts(first: 8) { id, name, thumbnailUrl }
    }
}
```
Mỗi client lấy đúng những gì cần — không cần multiple REST endpoints.

🚚 **Logistics — B2B Partner Integrations**
Mỗi 3PL partner cần shape khác nhau → GraphQL flexibility tốt hơn.

### Spring GraphQL
```java
@Controller
public class ProductController {
    @QueryMapping
    public Product product(@Argument UUID id) { ... }

    @BatchMapping
    public Mono<Map<Product, List<Review>>> reviews(List<Product> products) {
        // DataLoader pattern — batch 100 product → 1 query
    }
}
```

---

## 60. Geospatial (PostGIS, Geohash)

🚚 **Logistics — Critical**

```sql
-- Find nearby drivers within 5km
SELECT id, name, ST_Distance(location, ST_MakePoint(:lng, :lat)::geography) as dist
FROM drivers
WHERE ST_DWithin(location, ST_MakePoint(:lng, :lat)::geography, 5000)
  AND status = 'AVAILABLE'
ORDER BY dist
LIMIT 10;
```

```java
// Spring Data Geo support
@Entity
public class Driver {
    @Column(columnDefinition = "geography(Point,4326)")
    private Point location;
}

public interface DriverRepository extends JpaRepository<Driver, UUID> {
    @Query(value = """
        SELECT * FROM drivers
        WHERE ST_DWithin(location, ST_MakePoint(:lng, :lat)::geography, :radiusMeters)
          AND status = 'AVAILABLE'
        ORDER BY ST_Distance(location, ST_MakePoint(:lng, :lat)::geography)
        """, nativeQuery = true)
    List<Driver> findNearby(double lat, double lng, int radiusMeters);
}
```

🛒 **E-Commerce — Delivery Zone Check**  
"SKU này có ship được tới district 12 không?" → polygon containment check.

🏦 **Banking — ATM Nearby**  
"ATM gần nhất trong bán kính 2km" → identical pattern.

---

## 61. TCC (Try-Confirm-Cancel) & Two-Phase Commit vs Saga

### Banking — Cross-bank Transfer Example

**2PC (synchronous, blocking):** Nhanh nhưng coordinator down = entire system block. Hiếm dùng cho microservices.

**TCC (3-phase, programmatic):**
```
Try:    Reserve funds in source + reserve credit in dest (both temporary)
Confirm: Commit reservations
Cancel: Release reservations if any failure
```

**Saga (event-driven):**
```
Step 1: Debit source (real)
Step 2: Credit dest (real)
Failure: Compensating debit on dest + compensating credit on source
```

### Pessimistic Lock for Single-DB Balance Update (most common)
```java
@Transactional
public void transfer(UUID from, UUID to, BigDecimal amount) {
    // PESSIMISTIC_WRITE: row lock until commit
    Account fromAcc = accountRepo.findByIdForUpdate(from)
        .orElseThrow();
    if (fromAcc.getBalance().compareTo(amount) < 0)
        throw new InsufficientFunds();
    fromAcc.debit(amount);

    Account toAcc = accountRepo.findByIdForUpdate(to)
        .orElseThrow();
    toAcc.credit(amount);
}
```
**Deadlock risk:** Always lock accounts in **same order** (e.g., by ID ascending).

---

## 62. Reactive Streams (WebFlux) — Optional

### Khi nào dùng?

**KHÔNG** thay thế MVC + Virtual Threads cho hầu hết trường hợp. Chỉ dùng khi:
- Streaming response (large file, infinite stream)
- Backpressure cần thiết (slow consumer)
- Pure I/O pipeline (no blocking calls)

🏦 **Banking — Market Data Streaming**
```java
@GetMapping(value = "/stream/prices/{pair}", produces = TEXT_EVENT_STREAM_VALUE)
public Flux<PriceTick> stream(@PathVariable String pair) {
    return marketDataService.tickStream(pair)
        .sample(Duration.ofMillis(100))  // backpressure: max 10 ticks/sec
        .onBackpressureLatest();
}
```

🚚 **Logistics — GPS Ingestion Pipeline**
```java
Flux<GpsPing> incoming = kafkaReceiver.receive();
incoming
    .buffer(Duration.ofSeconds(1))     // batch
    .flatMap(batch -> r2dbcRepo.saveAll(batch))
    .doOnError(this::alert)
    .subscribe();
```

---

# Phần IV — Lộ trình triển khai theo Domain

> Mỗi domain có baseline TaskFlow (mid-level), sau đó add **domain-specific patterns** ưu tiên cao.

## 🏦 Banking Roadmap (8 tuần)

| Tuần | Focus | Technical |
|------|-------|-----------|
| 1 | Money correctness | BigDecimal, currency, rounding rules (#57) |
| 2 | Concurrency | Pessimistic lock cho balance, @Version cho config (#20, #61) |
| 3 | Audit & Compliance | AOP audit + REQUIRES_NEW + JPA Auditing (#14, #6) |
| 4 | Ledger reliability | Outbox pattern, Event Sourcing cho ledger (#27, #50) |
| 5 | Saga | Money transfer saga orchestration (#49) |
| 6 | Security | OAuth2 RS + step-up MFA + encryption at rest (#35, #37) |
| 7 | Observability | Distributed tracing + structured logs (#31, #32) |
| 8 | Performance | k6 stress + JVM tuning + read replica (#45, #25, #48) |

## 🛒 E-Commerce Roadmap (8 tuần)

| Tuần | Focus | Technical |
|------|-------|-----------|
| 1 | Catalog | DTO projection + EntityGraph + ES search (#23, #22, #58) |
| 2 | Cart & Order | State machine cho order lifecycle (#51) |
| 3 | Saga | Order saga (inventory + payment + fulfillment) (#49) |
| 4 | Inventory | Atomic decrement + idempotency (#28, #29) |
| 5 | Search & Catalog Sync | CDC Debezium → ES (#52) |
| 6 | API for mobile | GraphQL + API versioning (#59, #41) |
| 7 | Black Friday prep | k6 stress + HPA + cache warming (#45, #46, #12) |
| 8 | Multi-tenancy (marketplace) | Row-level tenant filter (#53) |

## 🚚 Logistics Roadmap (8 tuần)

| Tuần | Focus | Technical |
|------|-------|-----------|
| 1 | Domain modeling | State machine cho shipment (#51) |
| 2 | Geospatial | PostGIS + nearby driver query (#60) |
| 3 | GPS pipeline | TimescaleDB + Virtual Threads ingestion (#55, #2) |
| 4 | Carrier integration | Resilience4j + Outbox for carrier APIs (#26, #27) |
| 5 | Real-time tracking | SSE + Redis pub/sub (#56) |
| 6 | Multi-leg saga | Shipment leg orchestration (#49) |
| 7 | Multi-tenancy (3PL) | Schema-per-tenant + tenant routing (#53) |
| 8 | Live ops dashboard | CQRS view + distributed tracing (#40, #31) |

---

## Tổng kết — Stack Recommendation Matrix

| Domain | Must-Have Stack | Differentiator |
|--------|----------------|----------------|
| 🏦 Banking | Spring Boot 3 · PostgreSQL · Kafka · Vault · Resilience4j · OTel · BigDecimal · Outbox · Event Sourcing | Strong audit, exactly-once-ish via Saga + EventSourcing, OAuth2 Resource Server, multi-region active-passive |
| 🛒 E-Commerce | Spring Boot 3 · PostgreSQL · Elasticsearch · Redis · Kafka · GraphQL · State Machine · CDC · k8s HPA | Catalog search relevance, flash-sale concurrency, mobile-friendly APIs, marketplace multi-tenancy |
| 🚚 Logistics | Spring Boot 3 · PostgreSQL+PostGIS · TimescaleDB · Kafka · Redis · WebSocket/SSE · State Machine · Virtual Threads | Geospatial queries, time-series scale, real-time tracking UX, multi-carrier abstraction |

---

## Tài liệu tham khảo

- [TECHNICAL_GUIDE.md](./TECHNICAL_GUIDE.md) — chi tiết technical đã implement
- [SENIOR_ROADMAP.md](./SENIOR_ROADMAP.md) — senior techniques chưa làm
- [SENIOR_IMPLEMENT_PLAN.md](./SENIOR_IMPLEMENT_PLAN.md) — 48h sprint plan
- [Microservices Patterns — Chris Richardson](https://microservices.io/patterns/)
- [Designing Data-Intensive Applications — Martin Kleppmann](https://dataintensive.net/)
- [Domain-Driven Design — Eric Evans](https://www.domainlanguage.com/ddd/)
- [Enterprise Integration Patterns — Hohpe & Woolf](https://www.enterpriseintegrationpatterns.com/)
