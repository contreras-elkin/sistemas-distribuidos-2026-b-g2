<!-- HU-STATUS TEMPLATE - do NOT remove the <!-- ... --> markers or the table headers.
     Your weekly grade is read AUTOMATICALLY from this file:
       03-week/hu-status/README.md  (inside YOUR fork). English. -->

# Weekly Status - Week 03

<!-- CONFIG-START - must match your profile repo (username/username) CONFIG -->
- FULL_NAME: Elkin Stiven Contreras Rojas
- GITHUB_USER: contreras-elkin
- TEAM: Elkin Stiven Contreras Rojas, Alejandro Ortiz Vargas
- SPRINT_GOAL: Establish the project's governance at inception - define the Repo Docs as the single source of truth, the no-branches historical model, and the ADR-based decision process for the discovery phase.
<!-- CONFIG-END -->

## 1. User stories worked this week
| HU ID | Title | Status (todo/doing/done) | Evidence (PR or commit URL) |
|---|---|---|---|
| HU-02 | Project discovery | doing | _add PR/commit URL_ |

## 2. My individual contribution
- Authored `governance.md`: the project's governance model for the inception phase.
- Defined the **Repo Docs** repository as the project's Single Source of Truth (SSOT) and historical record for decisions, definitions, governance rules and documentation standards.
- Specified a deliberately branch-less model for the docs repo: change control comes from the Git history (each commit is a state of project knowledge), not from feature branches; a decision change is a new commit, never a silent overwrite.
- Established the decision lifecycle Proposal -> Discussion -> Formal decision: a decision is not official until formally recorded, and a superseded decision is preserved (e.g. ADR status `Superseded`), not deleted.
- Defined **ADRs** (context, decision, alternatives considered, rationale, consequences, status) as the instrument for architectural decisions, and a traceability chain Requirement -> Problem -> Analysis -> Alternatives -> Decision -> Implementation -> Result.
- Listed the core Repo Docs principles (centralization, authority, traceability, transparency, consistency, change control, evolution, accessibility, historical preservation, single authority) and drafted the final governance statement to be ratified at project start.

## 3. Blockers and risks
- The SSOT only has authority once every participant explicitly recognizes it; that agreement is not yet ratified with the teammate.
- No Repo Docs repository exists yet - governance is defined on paper but not instantiated.
- Without an adopted team process, the "who decides / how it is approved" half of the governance flow remains informal.

## 4. Plan for next week
- Apply this governance to the discovery: classify the product into bounded contexts and study the patterns (REST API, Worker, Workflow, Strangler Fig) that will guide the monolith-to-distributed evolution, recorded in ADR style.

## 5. Compliance self-check
- [x] Conventional Commits - `type(scope): summary`
- [ ] Per-environment HU branch + PR to that environment (hu-xxx-dev -> develop, ...)
- [ ] Testable acceptance criteria
- [ ] Tests added/updated (unit / integration)
- [ ] DDD / hexagonal boundaries respected (domain has no I/O)
- [ ] No secrets; config via environment variables

## 6. Evidence links
- `03-week/hu-status/governance.md`
- _add PR/commit URL_
