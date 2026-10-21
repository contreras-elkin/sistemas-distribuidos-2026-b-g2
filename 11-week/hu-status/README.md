<!-- HU-STATUS TEMPLATE - do NOT remove the <!-- ... --> markers or the table headers.
     Your weekly grade is read AUTOMATICALLY from this file:
       11-week/hu-status/README.md  (inside YOUR fork). English. -->

# Weekly Status - Week 11

<!-- CONFIG-START - must match your profile repo (username/username) CONFIG -->
- FULL_NAME: Elkin Stiven Contreras Rojas
- GITHUB_USER: contreras-elkin
- TEAM: Elkin Stiven Contreras Rojas, Alejandro Ortiz Vargas
- SPRINT_GOAL: Make the buyer checkout journey (HU-TXN-001) observable end to end: trace map, structured log contract, trace-context instrumentation in workflow-service the golden-signals metric catalog, SLOs with error budget, burn-rate alerts, a runbook and an incident drill with its postmortem (W11 workshop, deliveries 1 and 2).
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
- CP6 [`05-slo.md`](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/11-week/workshop/05-slo.md): three SLOs for the checkout (availability 99.5 %, latency 95 % under 2 s, `TransactionConfirmed` notifications 99.9 %) with the SLI PromQL, a traffic estimate of 200 checkouts per day and the budget in requests and minutes (30 checkouts / 216 min), plus the error budget policy at 50, 75 and 100 %.
- CP7 [`06-alerts.yml`](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/11-week/workshop/06-alerts.yml) and [`06-alert-policy.md`](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/11-week/workshop/06-alert-policy.md): multi-window burn-rate rules (14.4x in 1 h / 5 m, 6x in 6 h / 30 m) checked with `promtool`, the budget each alert spends before firing, a minimum-traffic guard for our low volume and the cause-based alerts we will not create.
- CP8 [`07-runbook.md`](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/11-week/workshop/07-runbook.md): runbook of the fast-burn alert - symptoms, confirmation by PromQL and by `trace_id` in the logs, three mitigations ordered by risk (catalog replica, rollback, checkout off at the gateway), escalation and recovery checks.
- CP9 [`08-incident.md`](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/11-week/workshop/08-incident.md): drill with catalog I-03 answering in 3 s for 25 % of requests during 40 min - span tree, logs, SLI values, why no alert fired, and a blameless postmortem with three actions written as user stories with acceptance criteria.

## 3. Blockers and risks
- The team has no Prometheus / tracing stack yet (`non-functional.md` Appendix A); the catalog is a design and only `workflow-service` in the workshop exposes metrics.
- The drill showed that slowness in catalog does not burn the availability budget (retries hide it) and we have no latency alert yet.
- The real `market-agri-workflow` saga is not built (level 3, ADR-015 pending), so the instrumentation lives in the workshop module first.
- GitHub Actions is still locked for the organization, so tests are run locally.

## 4. Plan for next week
- Turn the three postmortem actions into board issues: latency burn-rate alert, photo upload out of the catalog reservation pool, and `outcome="rejected"` in the workflow checkout counter.
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
- CP0: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/edef0a1
- CP1: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/78cc52b
- CP2: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/dfb9d59
- CP3 push 1: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/180b96c
- CP3 push 1: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/0cdbcaf
- CP3 push 2: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/635c08c
- CP3 push 2: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/621ff18
- CP3 push 2: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/2940b8d
- CP4: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/a06725f
- CP5: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/ddc2c26
- CP6: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/7820f49
- CP7: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/4e557fd
- CP8: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/2b26790
- CP9: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commit/bc1b13a
