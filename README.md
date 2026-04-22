# Grid07 — Spring Boot Backend Assignment

## Quick Start

```bash
# 1. Start Postgres + Redis
docker-compose up -d

# 2. Run the application
./mvnw spring-boot:run

# 3. App is live at http://localhost:8080
# Seed data (2 users, 2 bots) is inserted on first boot.
```

## Tech Stack
- Java 17 · Spring Boot 3.2.5
- PostgreSQL 15 (JPA / Hibernate)
- Redis 7 (Spring Data Redis / Lettuce)

---

## Phase 1 — Core API

Four JPA entities: **User**, **Bot**, **Post**, **Comment**.

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/posts | Create a post |
| POST | /api/posts/{id}/comments | Add a comment (human or bot) |
| POST | /api/posts/{id}/like | Like a post |
| GET  | /api/posts/{id}/virality | Virality score + bot count |
| POST | /api/users | Create a user |
| POST | /api/bots  | Create a bot  |

---

## Phase 2 — Redis Virality Engine & Atomic Locks

### Virality Score
Stored at `post:{id}:virality_score`.
- Bot reply   → +1
- Human like  → +20
- Human comment → +50

### Horizontal Cap (≤ 100 bot replies per post)
Key: `post:{id}:bot_count`

**Thread-safety guarantee**: Redis `INCR` is a single atomic command.
Redis processes commands sequentially (single-threaded), so even under
200 concurrent requests, each INCR is serialised. Requests 1–100 receive
back ≤ 100 and are allowed. Requests 101–200 receive > 100, the counter
is immediately decremented back, and the request is rejected **before**
any DB transaction is opened. Result: exactly 100 rows in the DB. ✓

### Vertical Cap (≤ 20 depth levels)
Derived from `parent.depthLevel + 1` before any write. Rejected with 429.

### Cooldown Cap (bot ↔ human, 10-minute TTL)
Key: `cooldown:bot_{botId}:human_{humanId}`
Uses `SET NX EX 600` (`setIfAbsent` with TTL) — atomic check-and-set in Redis,
no race window.

---

## Phase 3 — Notification Engine

### Throttler
- Cooldown key: `notif:cooldown:user_{id}` (15-min TTL)
- Pending list:  `user:{id}:pending_notifs`

If cooldown key exists → push message to list.
If not → log "Push Notification Sent", set cooldown.

### CRON Sweeper
`@Scheduled(fixedRate = 5 * 60 * 1000)` — every 5 minutes.
Scans all `user:*:pending_notifs` keys, summarises and logs, deletes list.

---

## Phase 4 — Statelessness & Data Integrity

| Requirement | Implementation |
|-------------|----------------|
| No in-memory state | Zero HashMap / static variables; all counters in Redis |
| Redis as gatekeeper | All Redis guards run before `@Transactional` DB save |
| Race condition (200 bots) | Atomic INCR stops at exactly 100; 429 returned to the rest |

---

## Postman
Import `Grid07_Postman_Collection.json`. Variable: `baseUrl = http://localhost:8080`.