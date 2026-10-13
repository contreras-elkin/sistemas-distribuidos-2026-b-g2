<!-- HU-STATUS TEMPLATE - do NOT remove the <!-- ... --> markers or the table headers.
     Your weekly grade is read AUTOMATICALLY from this file:
       06-week/hu-status/README.md  (inside YOUR fork). English. -->

# Weekly Status - Week 06

<!-- CONFIG-START - must match your profile repo (username/username) CONFIG -->
- FULL_NAME: Elkin Stiven Contreras Rojas
- GITHUB_USER: contreras-elkin
- TEAM: Elkin Stiven Contreras Rojas, Alejandro Ortiz Vargas
- SPRINT_GOAL: Close Epic 5 (Notifications) of the Phase 1 modular monolith and publish the complete monolith as the functional reference of the MVP for the move to distributed services.
<!-- CONFIG-END -->

## 1. User stories worked this week
| HU ID | Title | Status (todo/doing/done) | Evidence (PR or commit URL) |
|---|---|---|---|
| HU-NOTIF-001 | Notify me when a transaction is confirmed | done | [market-agri-docs@76fc532](https://github.com/code-corhuila/market-agri-docs/commit/76fc532e6ee8a10259606db07ccbe73e427a67d7) |
| HU-NOTIF-002 | Notify me of a new chat message | done | [market-agri-docs@76fc532](https://github.com/code-corhuila/market-agri-docs/commit/76fc532e6ee8a10259606db07ccbe73e427a67d7) |

## 2. My individual contribution
- Implemented Epic 5 (Notifications) in the monolith, closing the dependency-ordered backlog of Week 5 (Epics 0-5 done).
  - `NotificationEventListener`: reacts to `TransaccionConfirmada` and `NuevoMensajeChat` with `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async("notificationsExecutor")`, so a notification is only created once the business transaction is committed and never slows down or rolls back the publisher.
  - `notifications` stays a pure consumer: no other module calls it; it only listens to domain events (the in-process equivalent of the future RabbitMQ worker).
  - REST API for the signed-in user: `GET /api/notifications`, `PUT /api/notifications/{id}/read`, `PUT /api/notifications/read-all`.
  - Frontend slice: `NotificationsBell` with the unread badge in the header and a `NotificationsPage`.
- Published the complete monolith (backend + frontend + docker-compose) in `market-agri-docs` under `12-ux-ui/MVP/marketplace-agricultural-huila-monolith`, so it becomes the functional and visual reference (design system) for the distributed version.
- Attached `notifications_epic5.md` with the event flow and module diagrams.

## 3. Blockers and risks
- The Stripe (test mode) vs Wompi (PDR) and Chat in PostgreSQL vs MongoDB divergences are still not recorded in an ADR.
- JWT kept in `localStorage` with no refresh token - accepted for Phase 1, to be revisited when auth is extracted.
- In-process `@Async` events are lost if the application stops between commit and handling; the distributed version needs a durable broker (RabbitMQ) and an outbox.

## 4. Plan for next week
- Move from the monolith to the per-service repositories created by the instructor: adopt the course branching policy and prepare the docs repository for the distributed phase.

## 5. Compliance self-check
- [x] Conventional Commits - `type(scope): summary`
- [ ] Per-environment HU branch + PR to that environment (hu-xxx-dev -> develop, ...)
- [x] Testable acceptance criteria
- [x] Tests added/updated (unit / integration)
- [ ] DDD / hexagonal boundaries respected (domain has no I/O)
- [x] No secrets; config via environment variables

## 6. Evidence links
- `06-week/hu-status/notifications_epic5.md`
- https://github.com/code-corhuila/market-agri-docs/commit/76fc532e6ee8a10259606db07ccbe73e427a67d7
- https://github.com/code-corhuila/market-agri-docs/tree/main/12-ux-ui/MVP/marketplace-agricultural-huila-monolith
