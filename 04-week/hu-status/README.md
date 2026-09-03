<!-- HU-STATUS TEMPLATE - do NOT remove the <!-- ... --> markers or the table headers.
     Your weekly grade is read AUTOMATICALLY from this file:
       04-week/hu-status/README.md  (inside YOUR fork). English. -->

# Weekly Status - Week 04

<!-- CONFIG-START - must match your profile repo (username/username) CONFIG -->
- FULL_NAME: Elkin Stiven Contreras Rojas
- GITHUB_USER: contreras-elkin
- TEAM: Elkin Stiven Contreras Rojas, Alejandro Ortiz Vargas
- SPRINT_GOAL: Break the product's discovery into bounded contexts (domains, ubiquitous language, boundaries, persistence, communication, CAP position) and study the REST API / Worker / Workflow / Strangler Fig patterns that guide the monolith-to-distributed evolution.
<!-- CONFIG-END -->

## 1. User stories worked this week
| HU ID | Title | Status (todo/doing/done) | Evidence (PR or commit URL) |
|---|---|---|---|
| HU-02 | Project discovery | done | _add PR/commit URL_ |

## 2. My individual contribution
- Authored `domain_classification.md`: bounded-context analysis of the Huila Agricultural Marketplace (MVP).
  - Identified 5 domains plus the gateway: Authentication & Users, Product Catalog, Chat & Messaging, Transactions & Ledger, Notifications, API Gateway.
  - For each domain: responsibilities and scope, ubiquitous language, in-boundary vs out-of-boundary, backend, database/schema, communication pattern and priority quality attribute (CAP position).
  - Produced the context map: synchronous REST/WebSocket for immediate operations, event-driven via RabbitMQ (`NewChatMessage`, `TransactionConfirmed`) for propagation; Notifications is a consumer worker that no service calls synchronously.
  - Stated the integration rules (single entry through the gateway, cross-cutting JWT validation, Notifications decoupling, independence of off-platform purchases) and the accepted infrastructure risk of a shared PostgreSQL engine with isolated schemas.
- Authored `concepts.md`: study of REST API, Worker, Workflow and the Strangler (Fig) Pattern.
  - Their distinct responsibilities, how a single service can combine all four, orchestration vs choreography (with trade-offs), and the evolution monolith -> modular monolith -> hybrid -> microservices driven by concrete needs (independent scaling, fault isolation, deploy independence), not by default.

## 3. Blockers and risks
- **Divergence from the committed PDR:** `domain_classification.md` assigns Chat to **Go + MongoDB**, while `PDR.pdf` (Week 1) §7 explicitly discards Go and places Messaging on Spring Boot + MongoDB + Redis. The Chat stack must be reconciled and recorded as an ADR.
- `domain_classification.md` describes one shared PostgreSQL instance with isolated schemas, which is weaker than the PDR §6 "database-per-service" statement; the gap is consciously accepted for the academic MVP but should be documented explicitly.
- Bounded-context boundaries are defined on paper only; not yet validated against an implementation.

## 4. Plan for next week
- Turn the bounded contexts into a Phase 1 modular-monolith design (module structure, strict boundaries, in-process communication) and a dependency-ordered backlog toward the MVP.

## 5. Compliance self-check
- [x] Conventional Commits - `type(scope): summary`
- [ ] Per-environment HU branch + PR to that environment (hu-xxx-dev -> develop, ...)
- [ ] Testable acceptance criteria
- [ ] Tests added/updated (unit / integration)
- [ ] DDD / hexagonal boundaries respected (domain has no I/O)
- [ ] No secrets; config via environment variables

## 6. Evidence links
- `04-week/hu-status/domain_classification.md`
- `04-week/hu-status/concepts.md`
- _add PR/commit URL_
