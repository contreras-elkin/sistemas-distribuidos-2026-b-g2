# CP6 - SLIs, SLOs and error budget

Journey: buyer checkout (`01-journey.md`). Metrics from `04-metrics.md`. Window: **30 days
rolling** for every SLO.

## Traffic estimate

**200 checkouts per day** (`POST /api/v1/workflow/checkouts`).

Why: the first real users are producers and buyers of one pilot area in Huila. Most purchases
are agreed in the chat and many are paid off-platform (`01-context/scope.md`), so only part of
the conversations end in a platform checkout. `NFR-001` sizes the system for about 30
concurrent users, which fits a few hundred purchases a day, not thousands.

- Per 30 days: 200 x 30 = **6 000 checkouts**.
- Per hour on average: 200 / 24 = about **8 checkouts**; at the busy hours (morning market,
  6-10 a.m.) about 25 per hour.

## SLO 1 - Availability of the checkout

| | |
|---|---|
| SLI | good checkouts / valid checkouts. Good = answered without a 5xx. Valid = every checkout except business rejections (`INSUFFICIENT_STOCK`, `PRODUCT_NOT_AVAILABLE`, `PURCHASE_METHOD_NOT_PLATFORM`), which are correct answers |
| Objective | **99.5 %** |
| Window | 30 days |

```promql
1 - (
  sum(rate(workflow_checkout_requests_total{outcome="error"}[30d]))
  /
  sum(rate(workflow_checkout_requests_total{outcome=~"success|error"}[30d]))
)
```

Error budget: 0.5 % of 6 000 = **30 failed checkouts per month**. In time: 0.5 % of
43 200 minutes = **216 minutes** of total outage per month.

> Known gap: today `CheckoutController` counts any `RestClientException` as `error`, and a
> `409` from catalog is one of them. A business rejection would burn budget. It needs
> `outcome="rejected"` (see `08-incident.md`, actions).

## SLO 2 - Latency of the checkout

| | |
|---|---|
| SLI | checkouts answered in 2 s or less / all checkouts answered |
| Objective | **95 %** under 2 s |
| Window | 30 days |

2 s is the limit of `NFR-001` (p95 < 2 s) and a bucket edge of
`workflow_checkout_request_duration_seconds` (`04-metrics.md`), so the SLI is exact, not
interpolated.

```promql
sum(rate(workflow_checkout_request_duration_seconds_bucket{le="2.0"}[30d]))
/
sum(rate(workflow_checkout_request_duration_seconds_count[30d]))
```

Error budget: 5 % of 6 000 = **300 slow checkouts per month**. In time: 5 % of 43 200 =
**2 160 minutes** in which every checkout could be slow.

## SLO 3 - Buyer and producer are notified of the payment

Our product depends on the producer knowing that the buyer paid, so he can deliver the
harvest. The worker consumes `TransactionConfirmed` and sends two notifications.

| | |
|---|---|
| SLI | `TransactionConfirmed` events processed / events consumed (an event that ends in the DLQ is bad) |
| Objective | **99.9 %** |
| Window | 30 days |

```promql
1 - (
  sum(rate(worker_events_consumed_total{event_type="TransactionConfirmed", outcome="dlq"}[30d]))
  /
  sum(rate(worker_events_consumed_total{event_type="TransactionConfirmed"}[30d]))
)
```

Traffic: if about 80 % of checkouts end paid, 4 800 events per month. Error budget: 0.1 % of
4 800 = **about 5 events per month** in the DLQ. In time: 0.1 % of 43 200 = **43 minutes**.
It is stricter than the checkout because a lost notification is a lost sale for the producer,
and the event can be retried without the buyer waiting.

## Error budget policy (checkout availability)

| Budget used | What the team does |
|---|---|
| **50 %** (15 failed checkouts) | Warning in the team chat. Review the failed traces of the month by `trace_id` and open an issue on the board for the main cause. New features continue. |
| **75 %** (about 23) | Only one new feature merged to `qa` per week; the other slot goes to the reliability issue. Every PR to `workflow`, `catalog` or `transactions` needs the test of its error path. |
| **100 %** (30) | Feature freeze for the checkout path: only fixes and reliability work are promoted (`qa-<SUBTASK-ID>` branches with a `fix` type) until the 30-day window is back under 100 %. A postmortem is written (`08-incident.md` format). |

Both team members (Elkin and Alejandro) decide together when to lift the freeze.
