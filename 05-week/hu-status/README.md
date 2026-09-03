<!-- HU-STATUS TEMPLATE - do NOT remove the <!-- ... --> markers or the table headers.
     Your weekly grade is read AUTOMATICALLY from this file:
       05-week/hu-status/README.md  (inside YOUR fork). English. -->

# Weekly Status - Week 05

<!-- CONFIG-START - must match your profile repo (username/username) CONFIG -->
- FULL_NAME: Elkin Stiven Contreras Rojas
- GITHUB_USER: contreras-elkin
- TEAM: Elkin Stiven Contreras Rojas, Alejandro Ortiz Vargas
- SPRINT_GOAL: Define the Phase 1 modular monolith - internal module structure, strict boundaries, in-process communication (module API + domain events) and a dependency-ordered backlog (Epics 0-5) toward the MVP.
<!-- CONFIG-END -->

## 1. User stories worked this week
| HU ID | Title | Status (todo/doing/done) | Evidence (PR or commit URL) |
|---|---|---|---|
| HU-03 | MVP monolith | doing | _add PR/commit URL_ |

## 2. My individual contribution
- Authored `architectura_monolith.md`: internal design of the Phase 1 modular monolith (single Spring Boot deployable; `backend/` and `frontend/` as sibling folders).
  - Guiding rule: a module never touches another module's tables/entities/repositories - all interaction goes through the module's public `XModuleApi` (method call) or an in-process domain event. That single rule is what makes later extraction cheap.
  - Package-per-module (`auth`, `catalog`, `chat`, `transactions`, `notifications`) plus a `shared/` kernel; 4 simple layers per module (domain / application / infrastructure / web), not full hexagonal.
  - Boundary enforcement via `spring-modulith-starter-test` + `ArchitectureTests` (`ApplicationModules...verify()`), run on `mvn test`.
  - Two communication mechanisms that mirror the future microservices: synchronous -> direct `XModuleApi` call (future REST client); asynchronous -> Spring `ApplicationEventPublisher` + `@TransactionalEventListener` (future RabbitMQ). `notifications` exposes no API - it only reacts to `TransactionConfirmed` / `NewChatMessage`.
  - One PostgreSQL instance, one schema per module, Flyway with a reserved version range per module (`V1xx` auth ... `V5xx` notifications) because all migration folders share a single history.
  - JWT issued and validated by the monolith itself (Spring Security OAuth2 Resource Server, symmetric HS256); conventions for new modules (`permitAll` list, `@AuthenticationPrincipal Jwt`, `@PreAuthorize("hasRole(...)")`).
  - Explicit "not in Phase 1" list (no hexagonal, CQRS, Saga, Circuit Breaker, API Gateway, Outbox) and the future extraction path (Strangler Fig: ModuleApi -> REST client, events -> RabbitMQ, schema -> own database, separate deploy).
- Authored `backlog_monolith.md`: dependency-ordered backlog.
  - Epic 0 Foundation -> Epic 1 Auth/Users (RF1-RF2) -> Epic 2 Catalog (RF3-RF4) -> Epic 3 Chat (RF5-RF6) -> Epic 4 Transactions (RF7-RF8) -> Epic 5 Notifications (RF9).
  - Each epic ships its own thin React slice (no big-bang frontend) and must be demonstrable end-to-end before the next one starts.
  - Recorded state: Epics 0-4 completed with met exit criteria (verified end-to-end in a browser + API tests, `ArchitectureTests` green); Epic 5 (Notifications) pending.
  - Recorded per-epic decisions: local photo upload (one per product), sessionless public catalog, logical deletion, STOMP + in-memory broker for chat, JWT in the `CONNECT` frame, Stripe test-mode hosted Checkout, append-only ledger with 0% fee in Phase 1.

## 3. Blockers and risks
- **Divergence from the committed PDR:** the monolith docs use **Stripe (test mode)** as the payment gateway, whereas `PDR.pdf` §7 specifies **Wompi (sandbox)**; and Chat lives in a PostgreSQL `chat` schema in Phase 1 while the PDR / domain analysis target **Go + MongoDB** for the extracted service. Both are deliberate Phase 1 simplifications but need an ADR.
- Epic 5 (Notifications) is not implemented yet - `TransactionConfirmed` / `NewChatMessage` are already published but have no listener.
- Chat real-time transport (STOMP WebSocket) is the highest-risk piece; the PDR contingency (degrade to HTTP polling every 5 s) is not needed yet but stays open.
- Frontend JWT moved to `localStorage` with no refresh token - accepted for Phase 1, revisit for security later.

## 4. Plan for next week
- Implement Epic 5 (Notifications): listeners for `TransactionConfirmed` and `NewChatMessage`, a REST endpoint for a user to list their notifications, and an unread badge in the frontend.

## 5. Compliance self-check
- [x] Conventional Commits - `type(scope): summary`
- [ ] Per-environment HU branch + PR to that environment (hu-xxx-dev -> develop, ...)
- [x] Testable acceptance criteria
- [x] Tests added/updated (unit / integration)
- [ ] DDD / hexagonal boundaries respected (domain has no I/O)
- [x] No secrets; config via environment variables

## 6. Evidence links
- `05-week/hu-status/architectura_monolith.md`
- `05-week/hu-status/backlog_monolith.md`
- _add PR/commit URL_ (monolith repository)
