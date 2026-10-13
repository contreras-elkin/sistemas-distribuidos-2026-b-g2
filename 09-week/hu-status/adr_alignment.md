# ADR alignment with the course norm, repository topology and delivery strategy

Week 09 - Huila Agricultural Marketplace. Sources: `market-agri-docs/05-architecture/decisions/`
(README, `_template-adr.md`, ADR-008, ADR-009).

## 1. Why an audit

The documentation folders were written at three different moments, each against a different
source of truth. When the course norm arrived it took precedence over all of them.

```mermaid
timeline
    title Source each folder was written against
    30-Aug : 05-architecture (ADR-001...007) - PDR and own design
    31-Aug : 06-data - ADR-003 (one PostgreSQL, schema per service)
    24-27 Sep : 07-api - monolith, team meeting, Annex C
    27-Sep : Course norm + Annexes A-I arrive (they prevail)
```

Result: several accepted ADRs contradict the norm. The fix is **not** to rewrite them, but to
write new ADRs that supersede them, so the history of why the decision changed is kept.

## 2. ADR structure required by norm 4.2.3

```mermaid
flowchart LR
    C["Context + constraints"] --> O["Options (>= 2 real ones)"] --> D["Decision"] --> DC["Dominant criterion"] --> AC["Accepted cost"] --> CO["Consequences"]
    CO --> R["Risks / References"]
```

> An ADR without a dominant criterion and an accepted cost is a statement, not a decision.

### Status lifecycle and superseding

```mermaid
stateDiagram-v2
    [*] --> Proposed
    Proposed --> Accepted
    Proposed --> Rejected
    Accepted --> Superseded: a new ADR replaces it (only Status changes)
    Accepted --> Accepted: partial supersede, dated note under the header
```

## 3. ADR-008 - Repository topology

Norm formula: `Total = 5 + N x (2 + C)`. Real decision: is Notifications a domain or the worker?
It only reacts to events and has no business API, so it is the worker: `N = 4`, `C = 1` (web).

`5 + 4 x (2 + 1) = 17` repositories.

```mermaid
flowchart TB
    subgraph T["Transversal (5)"]
        GW["api-gateway"]; WK["worker (Notifications)"]; WF["workflow (sagas)"]; INF["infra"]; FR["front"]
    end
    subgraph A["auth"]
        A1["auth-db"]; A2["auth-api"]; A3["auth-portal"]
    end
    subgraph CA["catalog"]
        C1["catalog-db"]; C2["catalog-api"]; C3["catalog-portal"]
    end
    subgraph CH["chat"]
        H1["chat-db"]; H2["chat-api"]; H3["chat-portal"]
    end
    subgraph TX["transactions"]
        X1["transactions-db"]; X2["transactions-api"]; X3["transactions-portal"]
    end
```

| Option | Repositories | Why not / why |
|---|---|---|
| **A - 4 domains, Notifications = worker** | 17 | Chosen: matches the worker definition of the norm |
| B - 5 domains, Notifications with db/api/portal | 20 | Three more repos to mirror events, for two developers |
| C - Notifications inside another domain | 17 | Mixes responsibilities, breaks one context per domain |

Accepted cost: notifications cannot be listed (no bell, no "mark as read").
Later amendment (ADR-010): `market-agri-infra-mongo` is added -> 18 repositories.

## 4. ADR-009 - Delivery strategy

```mermaid
flowchart LR
    subgraph Before["ADR-002 (superseded part)"]
        M["Monolith"] -- "Strangler Fig, one by one" --> S1["services"]
    end
    subgraph Now["ADR-009"]
        P["Monolith = frozen prototype (functional reference)"] -. "behaviour re-implemented against api-contract" .-> R["each service built directly in its repository"]
    end
```

- Dominant criterion: norm section 3.2 - after the first cut the prototype is not evaluated and
  receives no more work.
- Accepted cost: no complete end-to-end flow until gateway, auth, catalog and infra exist
  together; the domain logic is rewritten, not moved.
- Mitigation: bring up gateway + auth + catalog first.

## 5. Plan produced by the audit (next weeks)

| Order | Work |
|---|---|
| 1 | ADR template + format retrofit (done, PR #11) |
| 2 | Topology and delivery (done, PR #12) |
| 3 | Data isolation and migrations (ADR-010, ADR-011) |
| 4 | Money, transversal state, outbox (ADR-012...014) |
| 5 | Correction PRs in `06-data`, citing each ADR |
