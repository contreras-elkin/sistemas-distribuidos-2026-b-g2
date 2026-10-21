# CP7 - Alert policy (checkout availability)

Rules: `06-alerts.yml`, checked with `promtool check rules 06-alerts.yml` (Prometheus 2.54.1):

```
Checking 06-alerts.yml
  SUCCESS: 6 rules found
```

SLO: 99.5 % of valid checkouts without a 5xx in 30 days (`05-slo.md`). Budget: 0.5 % of
6 000 = **30 failed checkouts** = **216 minutes** of total outage.

## How much budget each alert has spent before it fires

Budget spent = burn rate x window / 30 days (720 h).

| Alert | Burn rate | Long window / short window | `for` | Error ratio that fires it | Budget spent when it fires | In our numbers |
|---|---|---|---|---|---|---|
| `CheckoutErrorBudgetFastBurn` (page) | 14.4 | 1 h / 5 m | 5 m | > 7.2 % | 14.4 x 1 / 720 = **2 %** | 0.6 of 30 checkouts; 4.3 of 216 min |
| `CheckoutErrorBudgetSlowBurn` (ticket) | 6 | 6 h / 30 m | 30 m | > 3 % | 6 x 6 / 720 = **5 %** | 1.5 of 30 checkouts; 10.8 of 216 min |

- **Why two windows.** The long window (1 h, 6 h) proves the problem is big enough to matter.
  The short window (5 m, 30 m) proves it is still happening, so the alert stops soon after the
  fix instead of staying red for one more hour.
- **If nobody acts:** at 14.4x the whole month is gone in 720 / 14.4 = **50 h** (about two days).
  At 6x it is gone in 720 / 6 = **120 h** (five days). That is why the fast one wakes someone up
  (`severity: page`) and the slow one is a board issue for the next working day
  (`severity: ticket`).

## Low traffic problem (our case)

With 200 checkouts a day we get about **8 per hour**. One failed checkout in a quiet hour is
1 / 8 = 12.5 %, already over the 7.2 % of the fast alert. Without a guard, **one single
failure would page**.

- The rules require at least **5 valid checkouts in 1 h** (fast) and **10 in 6 h** (slow) before
  they can fire, so a night with 2 checkouts and 1 failure does not page.
- We accept that in a busy morning hour (25 checkouts) 2 failures page. Each failed checkout is
  a sale the producer loses, and 2 of them are already 7 % of the month.
- When traffic grows, the guard can go away.

## Labels and routing

| Label | Value | Use |
|---|---|---|
| `severity` | `page` / `ticket` | `page` goes to the phone of the on-call member, `ticket` opens an issue on the backlog board (Project 17) |
| `team` | `market-agri` | Our team (Elkin, Alejandro) |
| `service` | `workflow-service` | Where the SLO is measured; the cause can be downstream (chat, catalog, transactions) |
| `slo` | `checkout-availability` | Links the alert with `05-slo.md` |

Both alerts carry `runbook_url` to `07-runbook.md`.

## Cause-based alerts we will NOT create

| Alert | Why not |
|---|---|
| CPU of the `catalog-service` container > 80 % for 5 m | High CPU does not mean buyers fail. A reservation is one indexed `UPDATE`; catalog can be at 85 % CPU and still answer I-03 in 40 ms. If it gets slow, the latency SLO and the fast burn show it. A CPU alert would wake us up for nothing. |
| Any `5xx` on `POST /api/v1/workflow/checkouts` (one error = one alert) | With retries and idempotency (`Idempotency-Key`, `transactionId`) the buyer often recovers by retrying. One 5xx a day is inside the budget (30 per month). Alerting on each one trains the team to ignore alerts. |
| RabbitMQ container restarted | `NFR-011`: the synchronous checkout keeps working without the broker and the outbox keeps the events. If notifications are lost, SLO 3 (`worker_events_consumed_total`, DLQ) catches it. |
