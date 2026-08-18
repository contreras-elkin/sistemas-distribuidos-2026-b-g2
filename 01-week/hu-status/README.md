<!-- HU-STATUS TEMPLATE - do NOT remove the <!-- ... --> markers or the table headers.
     Your weekly grade is read AUTOMATICALLY from this file:
       01-week/hu-status/README.md  (inside YOUR fork). English. -->

# Weekly Status - Week 01

<!-- CONFIG-START - must match your profile repo (username/username) CONFIG -->
- FULL_NAME:Elkin Stiven Contreras Rojas
- GITHUB_USER:contreras-elkin
- TEAM:Elkin Stiven Contreras Rojas, Alejandro Ortiz Vargas
- SPRINT_GOAL:Define the PDR by outlining the architecture and the scope of the different services to be implemented.
<!-- CONFIG-END -->

## 1. User stories worked this week
| HU ID | Title | Status (todo/doing/done) | Evidence (PR or commit URL) |
|---|---|---|---|
| HU-XXX-001 | Define the PDR along with the services that are going to be implemented | doing | _add PR/commit URL_ |

## 2. My individual contribution

- Analyzed the three proposed modules (Payments, Chat, Ratings/Notifications) evaluating advantage, real cost, and architectural value for each.
- Payments: separated two models that were being conflated — direct payment (simple, no architectural value) vs. hold-and-disburse escrow (where the real distributed complexity lives: saga, webhook idempotency, reconciliation, compensation). Flagged a regulatory obstacle in Colombia: holding third-party funds may qualify as unauthorized public fund-raising ("captación"), which requires authorization from the Superintendencia Financiera. Also identified that without a logistics/delivery confirmation step, the escrow cannot close (needs confirmation states, timeout, and dispute handling).
- Chat: built the strongest case of the three — legal (exposing personal phone numbers is a liability under Ley 1581 de 2012), functional (solves fake ratings better than mutual confirmation), and architectural (justifies MongoDB, which had no clear justification in v1.0). Identified the real risk as adoption, not technical: rural users will likely ask for WhatsApp instead. This led to the bilateral contact-reveal flow design.
- Ratings & Notifications: gave each a different verdict. Notifications stays as its own component, but reclassified from a REST service to a consumer worker, justified by fault isolation from SMTP failures (not by domain size). Ratings does not deserve its own service: it's an anemic domain with no independent load/consistency profile and is coupled to the order lifecycle, so it was merged into Transactions.
- Caught an unrequested defect: v1.0 specified a shared Postgres database across services — a distributed-monolith anti-pattern that contradicted the document's own justification. Proposed database-per-service instead.
- Consolidated final decisions: hybrid escrow-gateway / disbursement-ledger model in sandbox; WebSocket + Redis Pub/Sub from the start for chat; Ratings and Payments merged inside Transactions. Resulting design: 4 domain services + gateway + worker.


## 3. Blockers and risks
- Regulatory risk: holding third-party funds (escrow) may require formal authorization from the Superintendencia Financiera de Colombia before implementation — needs legal/compliance confirmation.
- Adoption risk: rural end users may resist the in-app chat and request WhatsApp instead, which could undermine the value of the bilateral contact-reveal flow.
- Consistency risk: the escrow flow depends on delivery/logistics confirmation states (timeout, dispute) that are not yet defined; without them the payment saga cannot fully close.
- Migration risk: moving from a shared Postgres instance to database-per-service requires careful data ownership and migration planning to avoid breaking existing flows.

## 4. Plan for next week
- Define the services that are going to be implemented first

## 5. Compliance self-check
- [ ] Conventional Commits - `type(scope): summary`
- [ ] Per-environment HU branch + PR to that environment (hu-xxx-dev -> develop, ...)
- [ ] Testable acceptance criteria
- [ ] Tests added/updated (unit / integration)
- [ ] DDD / hexagonal boundaries respected (domain has no I/O)
- [ ] No secrets; config via environment variables

## 6. Evidence links
-
