<!-- HU-STATUS TEMPLATE - do NOT remove the <!-- ... --> markers or the table headers.
     Your weekly grade is read AUTOMATICALLY from this file:
       13-week/hu-status/README.md  (inside YOUR fork). English. -->

# Weekly Status - Week 13

<!-- CONFIG-START - must match your profile repo (username/username) CONFIG -->
- FULL_NAME: Elkin Stiven Contreras Rojas
- GITHUB_USER: contreras-elkin
- TEAM: Elkin Stiven Contreras Rojas, Alejandro Ortiz Vargas
- SPRINT_GOAL: Threat model of trust boundary TB2 (api-gateway -> transactions-api): attack surface map, STRIDE analysis in the team -docs, three security stories in the team backlog and security gates with a negative authorization test (W13 workshop, delivery 1).
<!-- CONFIG-END -->

## 1. User stories worked this week
| HU ID | Title | Status (todo/doing/done) | Evidence (PR or commit URL) |
|---|---|---|---|
| [#88](https://github.com/code-corhuila/market-agri-docs/issues/88) | [security] Only an ADMIN can read the platform transactions and ledger (E, risk 6, P1) | doing | [negative test](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/13-week/workshop/04-security-ci/transactions-authz) |
| [#89](https://github.com/code-corhuila/market-agri-docs/issues/89) | [security] transactions-api rejects tokens not signed by auth (S, risk 6, P1) | todo | https://github.com/code-corhuila/market-agri-docs/pull/87 |
| [#90](https://github.com/code-corhuila/market-agri-docs/issues/90) | [security] The payment webhook E-43 only confirms with a valid Stripe signature, once (T, risk 6, P1) | todo | https://github.com/code-corhuila/market-agri-docs/pull/87 |

## 2. My individual contribution
- CP0 [claim issue #86](https://github.com/code-corhuila/market-agri-docs/issues/86) and [`workshop/README.md`](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/13-week/workshop/README.md): boundary TB2, CONFIG with the library book (Zapata & Gonzales-Calderon, 2014, call number 005.1 G643i, p. 356).
- CP1 [`01-attack-surface.md`](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/13-week/workshop/01-attack-surface.md): Mermaid data flow with every service, datastore, RabbitMQ and Stripe, six trust boundaries (TB1-TB6), seven assets and the entry points E-41..E-45 of TB2.
- CP2 https://github.com/code-corhuila/market-agri-docs/pull/87: `05-architecture/security/stride-contreras-elkin.md` with six STRIDE rows, risk ranking, accepted residual risk and the [B] row E backed by the book.
- CP3 issues [#88](https://github.com/code-corhuila/market-agri-docs/issues/88), [#89](https://github.com/code-corhuila/market-agri-docs/issues/89), [#90](https://github.com/code-corhuila/market-agri-docs/issues/90) with labels `security` and `P1` in the team board.
- CP4 [`04-security-ci/`](https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/13-week/workshop/04-security-ci): `security.yml` (gitleaks, OWASP dependency-check, authorization tests) and `AdminTransactionsAuthzTest` (E-44: BUYER and PRODUCER 403, no token 401, ADMIN 200).

### Peer review (self-review against the ParkFlow example)
My teammate has not opened a STRIDE Pull Request yet, so I reviewed my own table:
- **Row S misses the gateway bypass.** ParkFlow models a caller inside the network that skips the gateway. My S row only covers forged tokens, and the bypass is only accepted in §6 (RR-2). Port 8080 of transactions-api is reachable from any container in the Compose network, so it should be its own line with the JWT check as mitigation.
- **Row D has no timeout.** ParkFlow mitigates pool exhaustion with timeouts; mine only rejects bad signatures and limits size. I should add a statement timeout on the `transactions` datasource.

## 3. Blockers and risks
- `market-agri-transactions-api` only exposes `/health`; the security rule and the negative test live in the workshop module first.
- The negative test was not run yet on my machine (CP8).
- My teammate has not claimed a boundary, so there is no PR to review.

## 4. Plan for next week
- Answer the review on the STRIDE PR and add the two self-review findings (CP6).
- Run `AdminTransactionsAuthzTest` locally and paste the output; validate `security.yml` with actionlint (CP8).
- Move the `/admin/**` role rule into `market-agri-transactions-api` when the transactions domain is built.

## 5. Compliance self-check
- [x] Conventional Commits - `type(scope): summary`
- [ ] Per-environment HU branch + PR to that environment (hu-xxx-dev -> develop, ...)
- [x] Testable acceptance criteria
- [x] Tests added/updated (unit / integration)
- [ ] DDD / hexagonal boundaries respected (domain has no I/O)
- [x] No secrets; config via environment variables

## 6. Evidence links
- https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/tree/main/13-week/workshop
- https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/commits/main/13-week
- STRIDE PR: https://github.com/code-corhuila/market-agri-docs/pull/87
- Review comment: https://github.com/contreras-elkin/sistemas-distribuidos-2026-b-g2/blob/main/13-week/hu-status/README.md#peer-review-self-review-against-the-parkflow-example
- Reflection: https://docs.google.com/document/d/1KHRAEOd73Y3xgmL8pTg3_OcqGXRCWiy_1DisW5Cm3lM/edit?tab=t.0
