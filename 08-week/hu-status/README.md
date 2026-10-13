<!-- HU-STATUS TEMPLATE - do NOT remove the <!-- ... --> markers or the table headers.
     Your weekly grade is read AUTOMATICALLY from this file:
       08-week/hu-status/README.md  (inside YOUR fork). English. -->

# Weekly Status - Week 08

<!-- CONFIG-START - must match your profile repo (username/username) CONFIG -->
- FULL_NAME: Elkin Stiven Contreras Rojas
- GITHUB_USER: contreras-elkin
- TEAM: Elkin Stiven Contreras Rojas, Alejandro Ortiz Vargas
- SPRINT_GOAL: Define a self-sufficient API contract (shared conventions, error body, closed decisions) and the auth service contracts, align the branch and environment policies with the instructor, and scaffold the first hexagonal service (auth-api).
<!-- CONFIG-END -->

## 1. User stories worked this week
| HU ID | Title | Status (todo/doing/done) | Evidence (PR or commit URL) |
|---|---|---|---|
| HU-PLAT-001 | All traffic through the API Gateway | doing | [market-agri-docs#7](https://github.com/code-corhuila/market-agri-docs/pull/7) |
| HU-AUTH-001 | Register as a producer or a buyer | doing | [market-agri-docs#10](https://github.com/code-corhuila/market-agri-docs/pull/10), [market-agri-auth-api#1](https://github.com/code-corhuila/market-agri-auth-api/pull/1) |
| HU-AUTH-002 | Log in and get a session | doing | [market-agri-docs#10](https://github.com/code-corhuila/market-agri-docs/pull/10) |
| HU-PLAT-003 | Extract auth-service | doing | [market-agri-auth-api#1](https://github.com/code-corhuila/market-agri-auth-api/pull/1), [market-agri-docs#9](https://github.com/code-corhuila/market-agri-docs/pull/9) |

## 2. My individual contribution
- Authored `07-api/api-contract.md` (PR #7): the single contract every service is built against.
  - Common conventions (roles, enumerations), one error body always present, and 33 closed decisions (D-C1...D-C33), each one citing its source, plus a register of behavioural differences against the monolith.
  - Key decisions: JWT signed with **RS256** and validated locally with five checks (D-C1); single-use refresh tokens with rotation (D-C2); routes `/api/v1/{service}/...` (D-C4); the gateway forwards only `Authorization` and `X-Correlation-Id` (D-C28); pagination `page/limit` (D-C24); internal API with service tokens (D-C27).
  - Made the contract self-sufficient and moved `_shared.yaml` out of the narrative document.
- Authored the OpenAPI contract of the auth service, `service-auth.yaml` (PR #10): register, login, refresh, logout, JWKS, farm profile, user lookup, internal service token and health/readiness.
- Aligned the branch and environment policies with the instructor (PR #9): `git-conventions.md` (`develop` / `qa` / `main`, child branch naming with the sub-task ID, promotion by `cherry-pick -x`), `10-devops/environments.md` (one environment per permanent branch, assembled by `market-agri-infra`, `.env.<environment>.example` with placeholders only) and the Definition of Done.
- Scaffolded `market-agri-auth-api` (auth-api PR #1, branch `chore/HU-AUTH-001-auth-api-structure`): Maven multi-module hexagonal layout `auth-core` (domain + ports, no I/O) / `auth-adapters` (http in, persistence out) / `auth-app` (Spring Boot wiring), CI workflow, Dockerfile, `deploy/compose.yml`, `.env.example` and a context-load test.
- Reviewed and merged Alejandro's authentication document (`07-api/authentication.md`, PR #8).
- Attached `api_contract_auth.md` with the request path, the JWT validation sequence and the hexagonal layout.

## 3. Blockers and risks
- The course norm and Annexes A-I arrived at the end of the week (2026-09-27): everything already written (ADRs, data, API) must be audited against them.
- The monolith signs JWT with HS256; the contract requires RS256 - the auth logic is rewritten, not copied.
- Pact contract tests are required on `qa` but there is no consumer/provider setup yet.

## 4. Plan for next week
- Align the ADR template and records with norm section 4.2.3, record the repository topology and the delivery strategy (ADR-008 / ADR-009), and audit `05`, `06` and `07` against the norm.

## 5. Compliance self-check
- [x] Conventional Commits - `type(scope): summary`
- [x] Per-environment HU branch + PR to that environment (hu-xxx-dev -> develop, ...)
- [x] Testable acceptance criteria
- [x] Tests added/updated (unit / integration)
- [x] DDD / hexagonal boundaries respected (domain has no I/O)
- [x] No secrets; config via environment variables

## 6. Evidence links
- `08-week/hu-status/api_contract_auth.md`
- https://github.com/code-corhuila/market-agri-docs/pull/7
- https://github.com/code-corhuila/market-agri-docs/pull/9
- https://github.com/code-corhuila/market-agri-docs/pull/10
- https://github.com/code-corhuila/market-agri-auth-api/pull/1
