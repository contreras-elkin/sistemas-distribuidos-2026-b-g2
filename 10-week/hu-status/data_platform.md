# Data platform: isolation, migrations, events and the first real service (catalog)

Week 10 - Huila Agricultural Marketplace. Sources: `market-agri-docs` ADR-010...ADR-014 and
`06-data`, `market-agri-catalog-db`, `market-agri-infra`, `market-agri-catalog-api`.

## 1. Data isolation (ADR-010)

One instance per engine (per environment), one schema per domain, and the database itself
refuses writes to another domain's schema.

```mermaid
flowchart TB
    subgraph PG["PostgreSQL 16 (market-agri-infra) - database market_agri"]
        direction LR
        SA["schema auth"]; SC["schema catalog"]; ST["schema transactions"]; SW["schema workflow"]; SK["schema worker"]
        PUB["public: only Liquibase control tables"]
    end
    subgraph MG["MongoDB 7 (market-agri-infra-mongo)"]
        DCH["database chat"]
    end
    CAPI["catalog-api"] -- "login catalog_app (search_path = catalog)" --> SC
    TAPI["transactions-api"] -- "transactions_app" --> ST
    RULE["No FK and no SELECT across schemas: data from another domain comes through its API or its events"]
```

| Element | Rule |
|---|---|
| Login user | `<x>_app`, created by the idempotent infra init script |
| Roles | `<x>_reader`, `<x>_writer` (`NOLOGIN`), privileges only on their schema |
| Migrations | run as administrator; services never connect as administrator |
| Cross-domain | no foreign keys, no reads; data comes through the other domain's API or events |

## 2. Migrations with Liquibase (ADR-011)

```mermaid
flowchart LR
    M["changelog/changelog-master.yaml"] --> DDL["01_ddl (schemas, tables, alter, indexes...)"]
    M --> DML["02_dml"]
    M --> DCL["03_dcl (roles, grants)"]
    M --> TCL["04_tcl"]
    DDL -. "every changeset has its mirror" .-> RB["05_rollbacks"]
    INFRA["market-agri-infra: docker compose run --rm catalog-db-migrate"] --> M
```

An applied changeset is never edited: a fix is a new changeset. Each domain has its own control
table (`databasechangelog_catalog`).

## 3. Catalog model (catalog-db, as merged this week)

```mermaid
erDiagram
    PRODUCT {
        uuid id PK
        uuid producer_id "id from auth, never a FK"
        text producer_name "JWT snapshot"
        text name
        text category "closed enum"
        text unit "closed enum"
        numeric quantity "net of reservations"
        bigint price_cents "ADR-012"
        text municipality
        text municipality_key "lower case, no accents"
        text photo_url "one photo"
        text status "ACTIVE or OUT_OF_STOCK"
        timestamptz deleted_at "logical deletion"
    }
    STOCK_RESERVATION {
        uuid transaction_id PK "idempotency key of the reservation"
        uuid product_id FK
        numeric quantity
        bigint unit_price_cents "price at reservation time"
        numeric remaining_quantity
        timestamptz released_at "null = holding stock"
    }
    IDEMPOTENCY_KEY {
        uuid owner_id PK
        text key_value PK "Idempotency-Key header"
        uuid product_id FK
    }
    PRODUCT ||--o{ STOCK_RESERVATION : "reserved by"
    PRODUCT ||--o| IDEMPOTENCY_KEY : "created with"
```

Business rules enforced by the database: `ACTIVE` always has stock
(`status <> 'ACTIVE' OR quantity > 0`), price range, closed enums for category and unit, and
`catalog_writer` has no `DELETE` privilege (deletion is logical).

## 4. Event delivery (ADR-013, ADR-014)

```mermaid
sequenceDiagram
    autonumber
    participant API as transactions-api
    participant DB as schema transactions
    participant R as Outbox relay (every 1 s)
    participant MQ as RabbitMQ market-agri.events
    participant W as worker / workflow
    API->>DB: business change + insert outbox_event (same transaction)
    R->>DB: take up to 50 unpublished rows (FOR UPDATE SKIP LOCKED)
    R->>MQ: publish (publisher confirms)
    R->>DB: set published_at
    MQ->>W: deliver
    W->>W: skip if event_id already in processed_event
    W-->>MQ: ack after the effect is recorded
    Note over W,MQ: network / 429 / 5xx: up to 5 retries with full jitter, then DLX to queue.dlq
```

Why: without the outbox, a crash between "save" and "publish" loses the event or publishes one
for a change that was rolled back.

## 5. catalog-api skeleton

```mermaid
flowchart LR
    REQ["HTTP request"] --> HTTP["catalog-adapters: adapter.in.http"]
    HTTP --> CORE["catalog-core: use cases + domain (no I/O)"]
    CORE --> PERS["catalog-adapters: adapter.out.persistence"]
    PERS --> SC[("schema catalog")]
    HTTP -- "any error" --> EB["contract error body + X-Correlation-Id"]
```

Delivered: module scaffold, image built from whole modules, server limits and graceful shutdown,
contract error body for every error (500 detail only in logs), correlation id in responses and
logs, with tests for the Annex C checks.

## 6. Sub-task progress at the end of the week

```mermaid
flowchart LR
    A["catalog-db: schema, roles, tables, FK, indexes"]:::done --> B["infra: PostgreSQL + catalog_app"]:::done --> C["catalog-api: skeleton + error body"]:::done --> D["catalog-api: persistence adapter"]:::todo --> E["catalog-api: endpoints E-10..."]:::todo
    classDef done fill:#2e7d32,color:#fff
    classDef todo fill:#9e9e9e,color:#fff
```
