# Sky Take-Out — Full-Stack Food Delivery Platform

A production-style food delivery backend built with Spring Boot, covering the full order lifecycle from placement to real-time delivery notification.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 2.7, Spring MVC |
| Auth | JWT (dual-token: admin + user) |
| ORM | MyBatis with dynamic XML SQL |
| Cache | Redis (dish cache, shop status) |
| Real-time | WebSocket (order push notifications) |
| Storage | Alibaba OSS (image upload) |
| Payments | WeChat Pay v3 API |
| Docs | Knife4j / Swagger 2 |
| Scheduler | Spring `@Scheduled` (cron jobs) |
| DB | MySQL 8 |

## Architecture

```
sky-take-out/
├── sky-common/     # Shared utilities: JWT, OSS, exceptions, AOP constants
├── sky-pojo/       # DTOs, Entities, VOs
└── sky-server/     # Controllers, Services, Mappers, WebSocket, Tasks
```

The project separates admin-facing and user-facing APIs into distinct controller packages (`/admin/**` and `/user/**`), each protected by its own JWT interceptor chain.

## Key Features & Design Decisions

### Real-time order notifications (WebSocket)
When a customer places an order or urges delivery, the server pushes a JSON message to all connected admin clients via WebSocket — no polling required.

```java
// Triggered on successful payment
map.put("type", 1); // 1 = new order, 2 = customer reminder
webSocketServer.sendToAllClient(JSON.toJSONString(map));
```

### Redis caching with targeted invalidation
Dish data is cached per category (`dish_{categoryId}`). On any write operation (create, update, status change), only the affected key — or all `dish_*` keys for bulk operations — is evicted. This avoids the common mistake of flushing the entire cache on every write.

### AOP-driven audit fields
A custom `@AutoFill` annotation on Mapper methods triggers an `@Before` aspect that reflectively sets `createTime`, `updateTime`, `createUser`, and `updateUser` — keeping this cross-cutting concern out of every service method.

```java
@AutoFill(OperationType.INSERT)
void insert(Employee employee);
```

### Automatic order timeout (cron job)
A scheduled task runs every minute to cancel unpaid orders older than 15 minutes, and a nightly job auto-completes orders stuck in "delivering" status — simulating real-world ops.

### Dual-role authentication
Two separate JWT interceptors guard the admin and user route groups. User identity is propagated thread-safely via `ThreadLocal` (`BaseContext`), avoiding any session state on the server.

## API Overview

| Module | Endpoints |
|---|---|
| Employee | Login, CRUD, enable/disable |
| Dish | CRUD, batch delete, start/stop sale |
| Setmeal (Combo) | CRUD with nested dish items |
| Order (Admin) | Search, confirm, reject, dispatch, complete |
| Order (User) | Submit, pay, history, cancel, reorder |
| Cart | Add, list, sub, clear |
| Address Book | CRUD, set default |
| Reports | Turnover, users, orders, top-10 sales, Excel export |
| WebSocket | `/ws/{sid}` — real-time push |

Full interactive docs available at `http://localhost:8080/doc.html` after startup.

## Running Locally

**Prerequisites:** Java 17, MySQL 8, Redis

```bash
# 1. Create database
mysql -u root -p < sql/sky.sql

# 2. Configure credentials
# Edit sky-server/src/main/resources/application-dev.yml
#   - datasource host/port/username/password
#   - redis host/port

# 3. Run
mvn spring-boot:run -pl sky-server
```

The Alibaba OSS and WeChat Pay integrations require credentials — the app starts without them; affected endpoints return a configured error response.

## What I Learned / Notable Challenges

- Designing a **cache invalidation strategy** that avoids stale reads without over-purging — settled on key-pattern delete for admin writes and category-scoped keys for user reads.
- Using **MyBatis dynamic SQL** (`<if>`, `<foreach>`) to support flexible multi-field queries without N+1 issues.
- Coordinating a **WebSocket push** from inside a `@Transactional` service method — needed to ensure the push fires only after the DB commit, handled by separating `paySuccess()` as a post-commit step.
- Implementing **idempotent order submission** — orders are keyed on timestamp-based order numbers; duplicate submissions are blocked at the service layer before hitting the DB.

## Roadmap

- [ ] Migrate from MyBatis to Spring Data JPA
- [ ] Add unit tests with JUnit 5 + Mockito (target 70% service-layer coverage)
- [ ] Containerise with Docker + docker-compose
- [ ] Deploy to AWS EC2 with a GitHub Actions CI/CD pipeline
- [ ] Upgrade to Spring Boot 3 + Java 21
