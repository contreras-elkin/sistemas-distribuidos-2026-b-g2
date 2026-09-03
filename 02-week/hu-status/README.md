<!-- HU-STATUS TEMPLATE - do NOT remove the <!-- ... --> markers or the table headers.
     Your weekly grade is read AUTOMATICALLY from this file:
       02-week/hu-status/README.md  (inside YOUR fork). English. -->

# Weekly Status - Week 02

<!-- CONFIG-START - must match your profile repo (username/username) CONFIG -->
- FULL_NAME: Elkin Stiven Contreras Rojas
- GITHUB_USER: contreras-elkin
- TEAM: Elkin Stiven Contreras Rojas, Alejandro Ortiz Vargas
- SPRINT_GOAL: Understand the distributed-architecture styles and the planning path (bounded contexts, ADR) for the product, and connect them to how the team will manage work with Scrum/Kanban, as input for the stack decision.
<!-- CONFIG-END -->

## 1. User stories worked this week
| HU ID | Title | Status (todo/doing/done) | Evidence (PR or commit URL) |
|---|---|---|---|
| HU-01 | Stack selection | done | _add PR/commit URL_ |

## 2. My individual contribution
- Reviewed the Week 2 sessions on distributed-architecture styles and on the planning path (bounded contexts, decision path, ADR).
- Wrote a theory document (ES and EN), `Teoria_Scrum_Kanban_Arquitecturas_Distribuidas.docx`, covering Scrum (roles, events, artifacts) and Kanban (practices and metrics: WIP limits, lead/cycle time, throughput), and mapped both onto the course's ADR / bounded-context planning cycle.
- Connected the theory to HU-01: architecture-style trade-offs (monolith vs modular monolith vs microservices) are the input for choosing the stack, and the team process (Scrum/Kanban) is what governs and records those choices as ADRs.
- Consolidated the stack decision already fixed in the PDR (Week 1, §7): React front-end, Spring Cloud Gateway, Spring Boot + PostgreSQL for Auth/Catalog/Transactions, Spring Boot (WebSocket/STOMP) + MongoDB + Redis for Messaging, RabbitMQ for async, Wompi (sandbox) as gateway, Docker Compose for local orchestration; Go discarded.
- Set the working assumption for the following weeks: a discovery phase documented as ADRs, feeding a Phase 1 modular monolith.

## 3. Blockers and risks
- The stack decision is recorded in the PDR but not yet split into individual ADRs, so its rationale is not independently traceable.
- The team has not formally adopted a process (Scrum vs Kanban), so cadence and decision records are still informal.
- Single-developer capacity: the polyglot, multi-service target may not be fully realistic and could force stack simplifications during the monolith phase.

## 4. Plan for next week
- Define the project governance (Repo Docs as the single source of truth, no-branches model, ADR process) that will host the stack decision records.

## 5. Compliance self-check
- [x] Conventional Commits - `type(scope): summary`
- [ ] Per-environment HU branch + PR to that environment (hu-xxx-dev -> develop, ...)
- [ ] Testable acceptance criteria
- [ ] Tests added/updated (unit / integration)
- [ ] DDD / hexagonal boundaries respected (domain has no I/O)
- [ ] No secrets; config via environment variables

## 6. Evidence links
- `02-week/hu-status/Teoria_Scrum_Kanban_Arquitecturas_Distribuidas.docx`
- _add PR/commit URL_
