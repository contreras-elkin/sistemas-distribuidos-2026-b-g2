# CP9 - Incident drill and blameless postmortem

## The drill

Hop marked in CP1/CP2: **workflow-service -> catalog-service, I-03**
(`POST /internal/v1/catalog/products/{id}/reservations`). Injected failure: catalog answers in
**3 s for 25 % of the requests during 40 minutes**, on a Tuesday from 07:00 to 07:40 (morning
market, our busiest hour: about 25 checkouts per hour, `05-slo.md`).

Saga settings (CP2): timeout of **2 s** per call to catalog, **2 retries** with backoff,
`transactionId` as idempotency key.

## Trace of one slow request (span tree with durations)

`trace_id = 3c9e7a1f5b2d4e6f8a0c1b3d5e7f9a2b`

- `POST /api/v1/workflow/checkouts` - api-gateway - **2 412 ms**
  - `POST /api/v1/workflow/checkouts` - workflow-service - 2 398 ms
    - `GET /internal/v1/chat/conversations/{id}` - chat-service - 19 ms
    - `POST .../reservations` attempt 1 - client span - **2 000 ms (timeout)**
      - catalog-service server span - 3 006 ms, of which 2 960 ms waiting for a database connection
        (it ends after the client gave up; it finds the reservation already made by attempt 2 and
        answers `200` replay, nothing subtracted twice)
    - backoff - 110 ms
    - `POST .../reservations` attempt 2 - client span -> catalog server span - 41 ms, `201`
    - `POST /internal/v1/transactions` - transactions-service - 214 ms
      - `stripe.checkout.session.create` - 168 ms

## Four log lines (CP2 format)

```json
{"timestamp":"2026-10-27T12:14:05.310Z","level":"INFO","service":"workflow-service","version":"2.0.0","environment":"main","trace_id":"3c9e7a1f5b2d4e6f8a0c1b3d5e7f9a2b","span_id":"6a1b2c3d4e5f6071","correlation_id":"0b7d9e2a-3c4f-4a5b-8c6d-7e8f9a0b1c2d","event":"saga.checkout.started","message":"checkout saga started","duration_ms":0,"transaction_id":"e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b","quantity":20}
{"timestamp":"2026-10-27T12:14:07.331Z","level":"WARN","service":"workflow-service","version":"2.0.0","environment":"main","trace_id":"3c9e7a1f5b2d4e6f8a0c1b3d5e7f9a2b","span_id":"7b2c3d4e5f607182","correlation_id":"0b7d9e2a-3c4f-4a5b-8c6d-7e8f9a0b1c2d","event":"catalog.reservation.timeout","message":"catalog did not answer in 2000 ms, retry 1 of 2","duration_ms":2000,"transaction_id":"e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b","saga_step":"RESERVE_STOCK"}
{"timestamp":"2026-10-27T12:14:07.482Z","level":"INFO","service":"catalog-service","version":"2.0.0","environment":"main","trace_id":"3c9e7a1f5b2d4e6f8a0c1b3d5e7f9a2b","span_id":"8c3d4e5f60718293","correlation_id":"0b7d9e2a-3c4f-4a5b-8c6d-7e8f9a0b1c2d","event":"catalog.reservation.created","message":"stock reserved","duration_ms":41,"transaction_id":"e1f2a3b4-c5d6-4e7f-8a9b-0c1d2e3f4a5b","http_status":201}
{"timestamp":"2026-10-27T12:14:07.722Z","level":"INFO","service":"api-gateway","version":"2.0.0","environment":"main","trace_id":"3c9e7a1f5b2d4e6f8a0c1b3d5e7f9a2b","span_id":"5f60718293a4b5c6","correlation_id":"0b7d9e2a-3c4f-4a5b-8c6d-7e8f9a0b1c2d","event":"http.request.completed","message":"POST /api/v1/workflow/checkouts 201","duration_ms":2412,"http_status":201}
```

(12:14Z = 07:14 in Colombia.)

## What the SLIs reach

40 minutes at 25 checkouts per hour = **about 17 checkouts**.

| SLI | During the incident | Why |
|---|---|---|
| Availability (SLO 1) | 100 % (0 failed) | A checkout fails only if the 3 attempts are slow: 0.25^3 = 1.6 % -> 0.27 of 17, expected 0 |
| Latency (SLO 2) | 75 % under 2 s (4 of 17 over 2 s) | Every checkout whose first attempt is slow takes 2 s + retry |
| Notifications (SLO 3) | 100 % | Not touched |

## Which alert fires first, and when

**None of our alerts fires.** Fast burn needs more than 7.2 % errors and the error ratio is
about 0 %. We have no latency alert, and even one would not fire: the latency burn rate is
25 % / 5 % = **5x**, under both 14.4 and 6.

The first signal is human: at **07:22 (minute 22)** a producer writes in the chat that "the
payment page takes long", and the on-call member opens the dashboard of `04-metrics.md`:
`catalog_reservation_duration_seconds` p99 = 3 s and `hikaricp_connections_pending{pool="catalog"}`
= 6 (normal: 0).

## Which runbook step stops the damage

`07-runbook.md` §2.b shows the slow step is `RESERVE_STOCK` and the pool of catalog is waiting.
**§3 mitigation 1** (`docker compose up -d --scale catalog-service=2`, minute 30) doubles the
connections available; the p99 is back to 45 ms at minute 33. Rollback (mitigation 2) is not
needed: there was no deploy.

## Postmortem (blameless)

**Timeline (Tuesday 2026-10-27, Colombia time)**

| Time | Minute | Event |
|---|---|---|
| 07:00 | 0 | A producer starts uploading 40 product photos (E-16). Each upload keeps a catalog database connection while the file is written |
| 07:02 | 2 | `hikaricp_connections_pending` > 0; 25 % of reservations wait about 3 s; the saga retries |
| 07:22 | 22 | A producer reports slowness in the chat; the on-call member opens the dashboard |
| 07:26 | 26 | Trace `3c9e...9a2b` read by `trace_id`: the wait is in catalog, not in the network |
| 07:30 | 30 | Runbook §3 mitigation 1: second `catalog-service` replica |
| 07:33 | 33 | p99 of I-03 back to 45 ms |
| 07:40 | 40 | Photo uploads end; the replica is kept until the fix |

**Impact.** About 17 checkouts in 40 minutes; **4 buyers waited more than 2 s**, none failed.
Budget: availability 0 of 30 failed checkouts (0 %); latency 4 of 300 slow checkouts
(**1.3 %**), or about 10 of 2 160 minutes (0.46 %; it is lower in minutes because the incident
happened at our busiest hour, three times the average traffic).

**Root cause.** The photo upload (E-16) and the reservation (I-03) share the same connection
pool of catalog-service, and the upload holds a connection while it writes the file to disk.

**Contributing factors.**
- No alert on latency or on `hikaricp_connections_pending`; detection depended on a user.
- The 2 s timeout hides the slowness from the availability SLI: retries turn waits into successes.
- Retries add load to the pool that is already waiting (attempt 1 still runs after the timeout).

**Actions**

1. *As the on-call member, I want an alert on the latency SLO (multi-window burn rate on
   `workflow_checkout_request_duration_seconds_bucket{le="2.0"}`), so that slowness is found
   without a user report.* Acceptance: the rule passes `promtool check rules`; a
   `promtool test rules` case with 30 % slow checkouts for 1 h fires `CheckoutLatencyBudgetBurn`.
2. *As a buyer, I want my reservation not to wait for photo uploads, so that paying stays fast.*
   Acceptance: in `market-agri-catalog-api` the photo file is written before the transaction
   opens; an integration test (`*IT`) with 10 parallel uploads keeps the p95 of I-03 under 100 ms.
3. *As the team, I want business rejections counted apart, so that they do not burn the error
   budget.* Acceptance: `workflow_checkout_requests_total{outcome="rejected"}` exists; a test
   with catalog answering `409 INSUFFICIENT_STOCK` increments `rejected` and not `error`.
