<!-- HU-STATUS TEMPLATE - do NOT remove the <!-- ... --> markers or the table headers.
     Your weekly grade is read AUTOMATICALLY from this file:
       07-week/hu-status/README.md  (inside YOUR fork). English. -->

# Weekly Status - Week 07

<!-- CONFIG-START - must match your profile repo (username/username) CONFIG -->
- FULL_NAME: Elkin Stiven Contreras Rojas
- GITHUB_USER: contreras-elkin
- TEAM: Elkin Stiven Contreras Rojas, Alejandro Ortiz Vargas
- SPRINT_GOAL: Adopt the course branching policy in the docs repository and prepare the move from the monolith to the per-service repositories seeded by the instructor.
<!-- CONFIG-END -->

## 1. User stories worked this week
| HU ID | Title | Status (todo/doing/done) | Evidence (PR or commit URL) |
|---|---|---|---|
| HU-PLAT-003 | Extract auth-service | todo | [market-agri-docs#4](https://github.com/code-corhuila/market-agri-docs/pull/4) (prerequisite: branching policy and repositories) |
| HU-PLAT-004 | Extract catalog-service | todo | [market-agri-docs#4](https://github.com/code-corhuila/market-agri-docs/pull/4) (prerequisite: branching policy and repositories) |

## 2. My individual contribution
- Reviewed and merged the instructor's course governance into `market-agri-docs` (PR #4):
  - `00-governance/branching-policy.md`: no permanent branch accepts a direct commit; each one is fed only by its own children through a Pull Request (`develop` <- `feat/ fix/ chore/`, `qa` <- `qa/`, `main` <- `release/ hotfix/`).
  - Promotion by re-application (`cherry-pick -x`), never by `merge develop -> qa`; the `-x` trail is the evidence that a story passed through each environment.
  - `.github/CODEOWNERS`: `main` requires the instructor's approval.
- Reviewed the per-service repositories seeded by the instructor (README + CODEOWNERS in each `market-agri-<domain>-db/-api/-portal` and in the transversal `api-gateway`, `front`, `infra`, `workflow`).
- Scope maintenance in `-docs`:
  - `01-context/scope.md`: removed the out-of-scope item about the final selection of a production payment gateway (commit `259d61d`).
  - `06-data/README.md`: updated the folder index.
- Attached `branching_policy.md` with the branch and promotion diagrams adopted by the team.

## 3. Blockers and risks
- Low visible progress this week: the per-repository structure (folders, CI, how a `-db` or an `-api` must look) is not defined yet, so no service code can start without risking rework.
- The team's own `git-conventions.md` still has to be aligned with the course policy (`develop` naming, `qa/` branches, release cadence).
- The monolith work does not transfer automatically: services must be rebuilt in their own repositories.

## 4. Plan for next week
- Write the API contract (shared conventions, error body, closed decisions) and the auth service contracts; align the branch and environment policies with the instructor; scaffold the first hexagonal service (auth-api).

## 5. Compliance self-check
- [x] Conventional Commits - `type(scope): summary`
- [ ] Per-environment HU branch + PR to that environment (hu-xxx-dev -> develop, ...)
- [ ] Testable acceptance criteria
- [ ] Tests added/updated (unit / integration)
- [ ] DDD / hexagonal boundaries respected (domain has no I/O)
- [ ] No secrets; config via environment variables

## 6. Evidence links
- `07-week/hu-status/branching_policy.md`
- https://github.com/code-corhuila/market-agri-docs/pull/4
- https://github.com/code-corhuila/market-agri-docs/commit/259d61de260c948ef2eea4201005fbf23ddcb68f
