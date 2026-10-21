# CP8 - Runbook: `CheckoutErrorBudgetFastBurn`

Alert: `06-alerts.yml`. SLO: checkout availability 99.5 % (`05-slo.md`). Severity **P1** in
`13-operations/incident-management.md` ("payments failing"): answer in **15 minutes**.

## 1. Symptoms

- The page `CheckoutErrorBudgetFastBurn` (`severity: page`, `service: workflow-service`) fired:
  more than 7.2 % of checkouts failed in the last hour and in the last 5 minutes.
- Buyers see `503 CATALOG_UNAVAILABLE`, `503 CHAT_UNAVAILABLE`, `503 TRANSACTIONS_UNAVAILABLE`
  or `502 PAYMENT_GATEWAY_ERROR` after pressing "Pagar" in `transactions-portal`.
- Producers may write in the chat that the buyer "could not pay".

## 2. Confirm (5 minutes)

**a. Is it still happening and how big is it?**

```promql
workflow:checkout_error_ratio:rate5m
sum(increase(workflow_checkout_requests_total{outcome="error"}[1h]))
```

**b. Which downstream step fails?** (`04-metrics.md`)

```promql
sum by (step, error_code) (rate(workflow_saga_step_failures_total[15m]))
histogram_quantile(0.99, sum by (le) (rate(catalog_reservation_duration_seconds_bucket[5m])))
max(hikaricp_connections_pending{pool="catalog"})
```

**c. Take one failed request and read its whole story by `trace_id`** (log contract, CP2):

```bash
# 1. Find a failed checkout and its trace_id
docker compose logs --since 15m workflow-service \
  | grep '"event":"http.request.completed"' | grep '"http_status":503' | tail -1

# 2. All lines of that request, in every service of the journey
docker compose logs --since 15m api-gateway workflow-service chat-service catalog-service transactions-service \
  | grep '"trace_id":"<trace_id>"'
```

The `saga_step` and `event` of the WARN / ERROR line say where it broke, for example
`"event":"catalog.reservation.timeout","saga_step":"RESERVE_STOCK"`.

**d. Health of the suspect service:** `curl -s http://<service>:8080/health/ready` (NFR-012:
`503` with `checks.database: "DOWN"` means its database is the problem).

If the error ratio is already back under 7.2 % and there is no new 503 in the logs: it was
short. Write it on the board as a P2 and stop here.

## 3. Mitigate (from lowest to highest risk)

| # | Action | When | How | Risk |
|---|---|---|---|---|
| 1 | **Add a replica or restart the failing downstream** | The slow / failing step is `catalog` or `chat` (stateless, NFR-080/081) and its `/health/ready` is up but slow, or the container is stuck | `docker compose up -d --scale catalog-service=2` or `docker compose restart catalog-service` | Low. The saga uses `transactionId` as idempotency key in I-03 and I-06, so a retried call does not reserve twice |
| 2 | **Roll back the last release** of the failing service | The failures started right after a deploy (`git log` of the service, tag `v2.Y.Z`) | Set the image tag of that service to the previous tag (for example `v2.0.0`) in `market-agri-infra` and `docker compose up -d <service>` | Medium. Only for services whose last change did not run a database migration; a migration needs its `05_rollbacks` changeset first (ADR-011) |
| 3 | **Turn off the platform checkout** (our "feature flag") | 1 and 2 did not work in 30 minutes, or the cause is Stripe | The gateway answers `503` with the contract body on `location /api/v1/workflow/checkouts` (ADR-017); buyers and producers can still agree an off-platform purchase in the chat | High. No platform sales until it is turned back on |

Not available in our architecture: a **circuit breaker** (deferred in `05-architecture/pattern-guide.md`;
we use timeouts, bounded retries and compensation) and a feature-flag service.

**Do not:** restart `transactions-service` while webhooks are arriving without checking the
outbox (`transactions_outbox_pending_events`); the events are safe in the table, but a restart
loop delays every `TransactionConfirmed`.

## 4. Escalation

| Time since the page | Who | Role (`incident-management.md`) |
|---|---|---|
| 0 min | On-call member of the week (Elkin or Alejandro) | On-Call Engineer, and Incident Commander while alone |
| 15 min without mitigation | The other team member | Tech Lead: diagnosis of the service he owns |
| 30 min, or mitigation 3 applied | The professor (owner of `code-corhuila`) | Informed, as the "Comms Lead" towards the course |
| Cause is Stripe | Stripe status page; nothing to fix on our side | Apply mitigation 3 and wait |

## 5. Verify recovery

- `workflow:checkout_error_ratio:rate5m` under 0.5 % for 15 minutes, and the alert resolves.
- A new test checkout in `qa` returns `201` with `checkoutUrl`, and its `trace_id` shows
  `catalog.reservation.created` in the logs.
- No reservation left without payment: every failed saga of the incident has its I-04
  (`DELETE /internal/v1/catalog/reservations/{transactionId}`) in the logs.
- Note the budget spent (`05-slo.md` policy) and, if it was more than 5 %, write the postmortem
  (`08-incident.md` format).
