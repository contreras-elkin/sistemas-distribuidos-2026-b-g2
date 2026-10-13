<!-- HU-STATUS TEMPLATE - do NOT remove the <!-- ... --> markers or the table headers.
     Your weekly grade is read AUTOMATICALLY from this file:
       09-week/hu-status/README.md  (inside YOUR fork). English. -->

# Weekly Status - Week 09

<!-- CONFIG-START - must match your profile repo (username/username) CONFIG -->
- FULL_NAME: Elkin Stiven Contreras Rojas
- GITHUB_USER: contreras-elkin
- TEAM: Elkin Stiven Contreras Rojas, Alejandro Ortiz Vargas
- SPRINT_GOAL: Audit the architecture, data and API documentation against the course norm, align the ADR template and records with norm 4.2.3, and record the repository topology and the delivery strategy (ADR-008 / ADR-009).
<!-- CONFIG-END -->

## 1. User stories worked this week
| HU ID | Title | Status (todo/doing/done) | Evidence (PR or commit URL) |
|---|---|---|---|
| HU-PLAT-003 | Extract auth-service (now "build", ADR-009) | doing | [market-agri-docs#12](https://github.com/code-corhuila/market-agri-docs/pull/12) |
| HU-PLAT-004 | Extract catalog-service (now "build", ADR-009) | doing | [market-agri-docs#11](https://github.com/code-corhuila/market-agri-docs/pull/11), [market-agri-docs#12](https://github.com/code-corhuila/market-agri-docs/pull/12) |
| HU-PLAT-007 | Run notifications-service as a RabbitMQ worker | doing | [market-agri-docs#12](https://github.com/code-corhuila/market-agri-docs/pull/12) (ADR-008: Notifications is the `-worker`) |

## 2. My individual contribution
- Audited `05-architecture`, `06-data` and `07-api` against the course norm and Annexes A-I: the three folders had been written against three different sources (PDR, ADR-003, the monolith + Annex C). Classified the findings (contradicts the norm / two documents contradict each other / editorial) and turned them into an ordered plan of new ADRs and correction PRs.
- Took the four team decisions the audit needed (2026-09-28): Notifications only as a worker (4 domains, 17 repositories), checkout as an orchestrated saga in `-workflow`, React for the front with one portal per domain, Liquibase for every domain.
- Aligned the ADR template and the existing records with norm 4.2.3 (PR #11): mandatory sections Context, Options (at least two real alternatives), Dominant criterion, Accepted cost and Consequences; a status lifecycle; and the superseding rules (an accepted ADR is never edited - it is superseded in whole or in part with a dated note). Retrofitted ADR-001...ADR-007 with a dated format-amendment note.
- Co-authored ADR-008 and ADR-009 with Alejandro (PR #12; he wrote the first draft, I completed and reviewed it):
  - ADR-008 Repository topology: `5 + N x (2 + C)` with 4 domains and the web channel = **17 repositories**; Notifications is the transversal `-worker`.
  - ADR-009 Delivery strategy: every service is built directly in its own repository; the monolith is frozen as a functional reference (supersedes the delivery part of ADR-002, monolith-first / Strangler Fig).
- Restored the superseding rules and the register heading lost in a merge, and fixed the ADR register table (PR #13).
- Attached `adr_alignment.md` with the ADR structure, the topology and the delivery decision.

## 3. Blockers and risks
- Five of the seven accepted ADRs (ADR-003 to ADR-007) are fully or partly invalidated by the norm (one instance and volume per domain, migrations only in the `-db`, outbox for every event...) and must be superseded by new ADRs before the code depends on them.
- ADR-009 accepts a real cost: there is no end-to-end flow until gateway, auth, catalog and infra exist together.
- Annex J (single database, 2026-10-02) arrived at the end of the week and changes the data isolation strategy again.
- 17 repositories, each with `develop` / `qa` / `main` and CI, for two developers.

## 4. Plan for next week
- Record the data decisions (ADR-010 isolation, ADR-011 Liquibase, ADR-012 money, ADR-013 transversal state, ADR-014 outbox), realign `06-data`, and build the first real service end to end at the data layer: `catalog-db`, the PostgreSQL instance in `infra`, and `catalog-api`.

## 5. Compliance self-check
- [x] Conventional Commits - `type(scope): summary`
- [ ] Per-environment HU branch + PR to that environment (hu-xxx-dev -> develop, ...)
- [ ] Testable acceptance criteria
- [ ] Tests added/updated (unit / integration)
- [ ] DDD / hexagonal boundaries respected (domain has no I/O)
- [ ] No secrets; config via environment variables

## 6. Evidence links
- `09-week/hu-status/adr_alignment.md`
- https://github.com/code-corhuila/market-agri-docs/pull/11
- https://github.com/code-corhuila/market-agri-docs/pull/12
- https://github.com/code-corhuila/market-agri-docs/pull/13
