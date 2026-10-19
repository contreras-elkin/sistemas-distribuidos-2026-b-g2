# CP2 - Structured log contract

Applies to every service of the checkout journey (api-gateway, workflow, chat, catalog,
transactions, worker). One JSON object per line, to stdout (`05-architecture/cross-cutting.md`
§2, NFR-050). Field names are `snake_case` in every service, whatever the stack.

## Mandatory fields

| Field | Type | Required | Example | Why it exists |
|---|---|---|---|---|
| `timestamp` | string ISO-8601 UTC, ms | yes | `2026-10-19T14:40:02.118Z` | Order lines across services; one clock format |
| `level` | enum `ERROR`/`WARN`/`INFO`/`DEBUG` | yes | `INFO` | Filter by severity |
| `service` | string | yes | `workflow-service` | Know who wrote the line |
| `version` | string semver | yes | `2.0.0` | Tie a failure to a release tag (`v2.0.0`) |
| `environment` | enum `dev`/`qa`/`main` | yes | `qa` | Our three environments (ADR-027) |
| `trace_id` | string, 32 hex | yes | `7d3f1c9a0b5e4d2f8a6c1e3b9d0f2a4c` | All lines of one request, from `traceparent` |
| `span_id` | string, 16 hex | yes | `a1b2c3d4e5f60718` | Which hop inside the trace |
| `correlation_id` | string uuid | yes | `5f0c2b1e-...` | Our existing `X-Correlation-Id`; equals the error `traceId` (D-C28) |
| `event` | string `domain.action` | yes | `catalog.reservation.timeout` | Stable key to count and alert; never free text |
| `message` | string | yes | `catalog did not answer` | Human summary, no personal data |
| `duration_ms` | integer | yes (0 if not a timed operation) | `2003` | Latency per hop |

## Domain fields (optional, only when they apply)

| Field | Type | Example | Why |
|---|---|---|---|
| `transaction_id` | uuid | `9a7e...` | The id created by the saga and used as idempotency key in I-03 and I-06 |
| `conversation_id` | uuid | `c41d...` | Ties the purchase to the chat where it was agreed (I-05) |
| `product_id` | uuid | `p88b...` | Which product was reserved (I-03) |
| `saga_step` | enum `READ_CONVERSATION`/`RESERVE_STOCK`/`CREATE_PAYMENT`/`COMPENSATE` | `RESERVE_STOCK` | Where in the saga it failed (Annex E) |
| `quantity` | number | `12.5` | Requested quantity, to explain `INSUFFICIENT_STOCK` |
| `amount_cop` | integer | `85000` | Money in whole pesos (ADR-026), never decimals |
| `error_code` | string | `INSUFFICIENT_STOCK` | The contract error code (`api-contract.md`) |
| `http_status` | integer | `409` | Status answered or received |
| `user_role` | enum `BUYER`/`PRODUCER`/`ADMIN`/`SERVICE` | `BUYER` | Who acted, without saying who the person is |

## Never log (and how it is masked)

| Data | Where it appears in our project | Rule |
|---|---|---|
| Password | E-01 register, E-02 login | Never logged, not even masked |
| JWT access / refresh token, service token (I-01) | `Authorization` header, E-03 | Drop the header; log only `user_role` |
| Stripe secret key, webhook signature (`Stripe-Signature`) | transactions-service, E-43 | Never logged; log `signature_valid: true/false` |
| Card data | Never reaches us (Stripe Checkout) | If a Stripe payload is logged by mistake, the logger drops `payment_method_details` |
| Email of buyer / producer | auth `users` | Masked `j***@gmail.com` |
| Names (`buyerName`, `producerName` snapshots, I-06) | transactions | Not logged; use the ids |
| Phone / WhatsApp / bank account shared in chat | chat messages | Message content is never logged; only `conversation_id` |
| Farm location (`village`, farm profile E-06) | auth | Not logged |

## Seven example lines - one failed checkout (same `trace_id`)

```json
{"timestamp":"2026-10-19T14:40:02.118Z","level":"INFO","service":"api-gateway","version":"2.0.0","environment":"qa","trace_id":"7d3f1c9a0b5e4d2f8a6c1e3b9d0f2a4c","span_id":"a1b2c3d4e5f60718","correlation_id":"5f0c2b1e-8d4a-4c3b-9e2f-1a7b6c5d4e3f","event":"http.request.received","message":"POST /api/v1/workflow/checkouts","duration_ms":0,"user_role":"BUYER"}
{"timestamp":"2026-10-19T14:40:02.131Z","level":"INFO","service":"workflow-service","version":"2.0.0","environment":"qa","trace_id":"7d3f1c9a0b5e4d2f8a6c1e3b9d0f2a4c","span_id":"b2c3d4e5f6071829","correlation_id":"5f0c2b1e-8d4a-4c3b-9e2f-1a7b6c5d4e3f","event":"saga.checkout.started","message":"checkout saga started","duration_ms":0,"transaction_id":"9a7e4b2c-1d3f-4e5a-8b6c-7d8e9f0a1b2c","conversation_id":"c41d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f","quantity":12.5}
{"timestamp":"2026-10-19T14:40:02.164Z","level":"INFO","service":"chat-service","version":"2.0.0","environment":"qa","trace_id":"7d3f1c9a0b5e4d2f8a6c1e3b9d0f2a4c","span_id":"c3d4e5f607182930","correlation_id":"5f0c2b1e-8d4a-4c3b-9e2f-1a7b6c5d4e3f","event":"chat.conversation.read","message":"conversation read for checkout","duration_ms":21,"conversation_id":"c41d2e3f-4a5b-4c6d-8e7f-9a0b1c2d3e4f","saga_step":"READ_CONVERSATION","http_status":200}
{"timestamp":"2026-10-19T14:40:04.181Z","level":"WARN","service":"workflow-service","version":"2.0.0","environment":"qa","trace_id":"7d3f1c9a0b5e4d2f8a6c1e3b9d0f2a4c","span_id":"d4e5f60718293041","correlation_id":"5f0c2b1e-8d4a-4c3b-9e2f-1a7b6c5d4e3f","event":"catalog.reservation.timeout","message":"catalog did not answer in 2000 ms, retry 1 of 2","duration_ms":2003,"transaction_id":"9a7e4b2c-1d3f-4e5a-8b6c-7d8e9f0a1b2c","product_id":"8b8f1a2c-3d4e-4f5a-9b6c-0d1e2f3a4b5c","saga_step":"RESERVE_STOCK"}
{"timestamp":"2026-10-19T14:40:04.402Z","level":"INFO","service":"catalog-service","version":"2.0.0","environment":"qa","trace_id":"7d3f1c9a0b5e4d2f8a6c1e3b9d0f2a4c","span_id":"e5f6071829304152","correlation_id":"5f0c2b1e-8d4a-4c3b-9e2f-1a7b6c5d4e3f","event":"catalog.reservation.rejected","message":"not enough stock for the reservation","duration_ms":38,"transaction_id":"9a7e4b2c-1d3f-4e5a-8b6c-7d8e9f0a1b2c","product_id":"8b8f1a2c-3d4e-4f5a-9b6c-0d1e2f3a4b5c","quantity":12.5,"error_code":"INSUFFICIENT_STOCK","http_status":409}
{"timestamp":"2026-10-19T14:40:04.415Z","level":"INFO","service":"workflow-service","version":"2.0.0","environment":"qa","trace_id":"7d3f1c9a0b5e4d2f8a6c1e3b9d0f2a4c","span_id":"b2c3d4e5f6071829","correlation_id":"5f0c2b1e-8d4a-4c3b-9e2f-1a7b6c5d4e3f","event":"saga.checkout.rejected","message":"saga ended before payment, no transaction created","duration_ms":2284,"transaction_id":"9a7e4b2c-1d3f-4e5a-8b6c-7d8e9f0a1b2c","saga_step":"RESERVE_STOCK","error_code":"INSUFFICIENT_STOCK","http_status":409}
{"timestamp":"2026-10-19T14:40:04.421Z","level":"INFO","service":"api-gateway","version":"2.0.0","environment":"qa","trace_id":"7d3f1c9a0b5e4d2f8a6c1e3b9d0f2a4c","span_id":"a1b2c3d4e5f60718","correlation_id":"5f0c2b1e-8d4a-4c3b-9e2f-1a7b6c5d4e3f","event":"http.request.completed","message":"POST /api/v1/workflow/checkouts 409","duration_ms":2303,"http_status":409}
```

Reading them by `trace_id`: the gateway receives the checkout, the saga starts, chat confirms
the conversation, the first reservation call to catalog times out (2 s), the retry reaches
catalog and is rejected by the business rule `INSUFFICIENT_STOCK`, the saga ends without
creating a payment, and the buyer gets `409`.

## When we use each level

| Level | Rule | Example in our project |
|---|---|---|
| `ERROR` | The request failed for a reason that is ours and someone must act | I-06 to transactions fails after the retries and the saga cannot release the reservation (I-04) |
| `WARN` | Something went wrong but the system recovered or the user can retry | `catalog.reservation.timeout` with a retry left; an event sent to the DLQ |
| `INFO` | Normal business milestones, including expected rejections | `saga.checkout.started`, `catalog.reservation.rejected` with `INSUFFICIENT_STOCK` (a business rule, not a bug) |
| `DEBUG` | Detail for local work only, off in `qa` and `main` | Each outbox poll of the transactions relay with the number of pending rows |
