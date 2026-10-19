# CP4 - Golden signals and metric catalog

Journey: buyer checkout (`01-journey.md`). Conventions: `snake_case`, a unit suffix
(`_seconds`, `_bytes`), `_total` for counters, the service as prefix. Our team does not run
Prometheus yet (`non-functional.md` Appendix A); this is the catalog we would expose from each
service's `/actuator/prometheus` (Spring Boot + Micrometer). `workflow_checkout_*` is already
implemented in `03-instrumentation/workflow-service`.

## api-gateway (NGINX, via nginx-prometheus-exporter / log-based)

| Signal | Metric | Type | Labels | PromQL (dashboard) |
|---|---|---|---|---|
| Latency | `gateway_request_duration_seconds` | histogram | `route`, `method` | `histogram_quantile(0.95, sum by (le, route) (rate(gateway_request_duration_seconds_bucket[5m])))` |
| Traffic | `gateway_requests_total` | counter | `route`, `method`, `status_class` | `sum by (route) (rate(gateway_requests_total[5m]))` |
| Errors | `gateway_requests_total` | counter | `status_class="5xx"` | `sum(rate(gateway_requests_total{status_class="5xx"}[5m])) / sum(rate(gateway_requests_total[5m]))` |
| Saturation | `nginx_connections_active` | gauge | none | `max(nginx_connections_active)` |

## workflow-service (checkout saga)

| Signal | Metric | Type | Labels | PromQL (dashboard) |
|---|---|---|---|---|
| Latency | `workflow_checkout_request_duration_seconds` | histogram | `route` | `histogram_quantile(0.95, sum by (le) (rate(workflow_checkout_request_duration_seconds_bucket[5m])))` |
| Traffic | `workflow_checkout_requests_total` | counter | `route`, `outcome` | `sum(rate(workflow_checkout_requests_total[5m]))` |
| Errors | `workflow_checkout_requests_total` | counter | `outcome="error"` | `sum(rate(workflow_checkout_requests_total{outcome="error"}[5m])) / sum(rate(workflow_checkout_requests_total[5m]))` |
| Errors (per step) | `workflow_saga_step_failures_total` | counter | `step`, `error_code` | `sum by (step) (rate(workflow_saga_step_failures_total[15m]))` |
| Saturation | `workflow_saga_in_progress` | gauge | none | `max(workflow_saga_in_progress)` (sagas waiting for `TransactionConfirmed`) |

## catalog-service (I-03 reservations)

| Signal | Metric | Type | Labels | PromQL (dashboard) |
|---|---|---|---|---|
| Latency | `catalog_reservation_duration_seconds` | histogram | `outcome` | `histogram_quantile(0.99, sum by (le) (rate(catalog_reservation_duration_seconds_bucket[5m])))` |
| Traffic | `catalog_reservations_total` | counter | `outcome` (`created`/`replayed`/`rejected`/`error`) | `sum by (outcome) (rate(catalog_reservations_total[5m]))` |
| Errors | `catalog_reservations_total` | counter | `outcome="error"` | `sum(rate(catalog_reservations_total{outcome="error"}[5m]))` |
| Saturation | `hikaricp_connections_pending` | gauge | `pool` | `max(hikaricp_connections_pending{pool="catalog"})` |

`rejected` (`INSUFFICIENT_STOCK`, `PRODUCT_NOT_AVAILABLE`) is **not** an error: it is a business
answer and stays out of the error ratio.

## transactions-service (I-06, E-43, outbox)

| Signal | Metric | Type | Labels | PromQL (dashboard) |
|---|---|---|---|---|
| Latency | `transactions_payment_create_duration_seconds` | histogram | `outcome` | `histogram_quantile(0.95, sum by (le) (rate(transactions_payment_create_duration_seconds_bucket[5m])))` |
| Traffic | `transactions_webhooks_total` | counter | `outcome` (`confirmed`/`failed`/`invalid_signature`) | `sum by (outcome) (rate(transactions_webhooks_total[5m]))` |
| Errors | `transactions_payment_create_total` | counter | `outcome="error"`, `error_code` | `sum(rate(transactions_payment_create_total{outcome="error"}[5m]))` |
| Saturation | `transactions_outbox_pending_events` | gauge | none | `max(transactions_outbox_pending_events)` (rows not yet published) |

## worker (notifications)

| Signal | Metric | Type | Labels | PromQL (dashboard) |
|---|---|---|---|---|
| Latency | `worker_event_processing_duration_seconds` | histogram | `event_type` | `histogram_quantile(0.95, sum by (le) (rate(worker_event_processing_duration_seconds_bucket[5m])))` |
| Traffic | `worker_events_consumed_total` | counter | `event_type`, `outcome` | `sum by (event_type) (rate(worker_events_consumed_total[5m]))` |
| Errors | `worker_events_consumed_total` | counter | `outcome="dlq"` | `sum(rate(worker_events_consumed_total{outcome="dlq"}[15m]))` |
| Saturation | `rabbitmq_queue_messages_ready` | gauge | `queue` | `max(rabbitmq_queue_messages_ready{queue="worker.transaction-confirmed"})` |

## Label cardinality

| Label | Values | Estimate |
|---|---|---|
| `route` | route templates of the contract (`/api/v1/workflow/checkouts`, `/internal/v1/catalog/products/{id}/reservations`, ...) | about 40 |
| `method` | GET, POST, PUT, DELETE | 4 |
| `status_class` | 2xx, 3xx, 4xx, 5xx | 4 |
| `outcome` | 3 to 4 per metric | 4 |
| `step` | READ_CONVERSATION, RESERVE_STOCK, CREATE_PAYMENT, COMPENSATE | 4 |
| `error_code` | codes of `api-contract.md` for that endpoint | about 12 |
| `event_type` | TransactionConfirmed, TransactionFailed, ... (`domain-events.md`) | about 10 |
| `queue` / `pool` | one per consumer / one per service | under 10 |

Worst series count: `gateway_request_duration_seconds` = 40 routes x 4 methods x 13 buckets
= about 2 080 series per instance. Acceptable.

**Forbidden labels** (unbounded) and what replaces them:

| Forbidden | Why | Replaced by |
|---|---|---|
| `user_id`, `buyer_id`, `producer_id` | One series per user | `user_role` in logs only |
| `transaction_id`, `product_id`, `conversation_id` | One series per purchase | Log fields (CP2), searched by `trace_id` |
| Raw URL (`/products/8b8f...`) | One series per product | The route template `{id}` |
| `email` | Personal data and unbounded | Nothing; never in metrics |

## Latency buckets

`0.1, 0.25, 0.5, 1, 2, 3, 5` seconds (set in `CheckoutController`).

- A checkout makes three synchronous calls (chat, catalog, transactions) plus Stripe, so a
  normal answer is 300 ms to 1 s; `0.25`, `0.5` and `1` separate a healthy saga from a slow one.
- The buyer is redirected to Stripe after the answer; over 2 s the page feels stuck, which is
  the latency SLO limit we expect to set in CP6, so `2` must be a bucket edge.
- `3` marks a downstream that answers at its timeout (CP9 drill) and `5` catches the retries.
- Catalog reservation alone is one indexed update: `0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1`.
