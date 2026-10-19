<!-- HU-STATUS TEMPLATE - do NOT remove the <!-- ... --> markers or the table headers.
     Your weekly grade is read AUTOMATICALLY from this file:
       11-week/hu-status/README.md  (inside YOUR fork). English. -->

# Weekly Status - Week 11

<!-- CONFIG-START - must match your profile repo (username/username) CONFIG -->
- FULL_NAME: Elkin Stiven Contreras Rojas
- GITHUB_USER: contreras-elkin
- TEAM: Elkin Stiven Contreras Rojas, Alejandro Ortiz Vargas
- SPRINT_GOAL: Make the buyer checkout journey (HU-TXN-001) observable end to end: trace map, structured log contract, trace-context instrumentation in workflow-service and the golden-signals metric catalog (W11 workshop, delivery 1).
<!-- CONFIG-END -->

## 1. User stories worked this week
| HU ID | Title | Status (todo/doing/done) | Evidence (PR or commit URL) |
|---|---|---|---|
| HU-TXN-001 | Pay for a product on the platform (checkout journey made observable) | doing | [workshop folder](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/11-week/workshop), [commits](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commits/main/11-week/workshop) |

## 2. My individual contribution
- CP1 [`01-journey.md`](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/11-week/workshop/01-journey.md): hop table, Mermaid sequence and span tree of the checkout: front -> api-gateway -> workflow saga (E-40) -> chat I-05 / catalog I-03 / transactions I-06 -> Stripe webhook E-43 -> `TransactionConfirmed` on RabbitMQ -> worker. `traceparent` next to our `X-Correlation-Id`.
- CP2 [`02-log-contract.md`](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/11-week/workshop/02-log-contract.md): JSON log fields (mandatory and domain: `transaction_id`, `conversation_id`, `product_id`, `saga_step`, `amount_cop`), never-log list (tokens, Stripe signature, emails, chat content) and seven lines of one failed checkout (catalog timeout + `INSUFFICIENT_STOCK`).
- CP3 [`03-instrumentation/`](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/11-week/workshop/03-instrumentation): Spring Boot `workflow-service` with a trace-context filter, a structured logger, propagation to catalog I-03 through a `RestClient` interceptor, two Micrometer metrics on `/actuator/prometheus` and a test that checks the same `trace_id` goes downstream.
- CP4 [`04-metrics.md`](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/11-week/workshop/04-metrics.md): golden signals per service of the journey, PromQL, label cardinality, forbidden labels and latency buckets.

## 3. Blockers and risks
- The team has no Prometheus / tracing stack yet (`non-functional.md` Appendix A); the catalog is a design and only `workflow-service` in the workshop exposes metrics.
- The real `market-agri-workflow` saga is not built (level 3, ADR-015 pending), so the instrumentation lives in the workshop module first.
- GitHub Actions is still locked for the organization, so tests are run locally.

## 4. Plan for next week
- Delivery 2 of the workshop: SLOs and error budget, burn-rate alerts, runbook and the incident drill (CP6 to CP10).
- Move the trace-context filter and the JSON logger into `market-agri-catalog-api`, next to the existing `X-Correlation-Id` handling.

## 5. Compliance self-check
- [x] Conventional Commits - `type(scope): summary`
- [x] Per-environment HU branch + PR to that environment (hu-xxx-dev -> develop, ...)
- [x] Testable acceptance criteria
- [x] Tests added/updated (unit / integration)
- [x] DDD / hexagonal boundaries respected (domain has no I/O)
- [x] No secrets; config via environment variables

## 6. Evidence links
- https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/11-week/workshop
- https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commits/main/11-week/workshop
