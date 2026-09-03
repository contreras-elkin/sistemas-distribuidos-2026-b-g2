# Backlog — Modular Monolith (Phase 1)
 
> Ordered by actual dependency between modules (see [architecture.md](architecture.md)), not by isolated business priority — each epic requires the previous one to exist in order to be tested end-to-end. RF-x references the functional requirements from the [PDR](PDR.md).
>
> **Thin frontend per epic:** the final deliverable is the monolith running *together with* the frontend (React), so each backend epic brings its own minimal slice of frontend that consumes that API before moving on to the next. This avoids discovering integration problems (JWT/CORS, real WebSocket, payment gateway SDK) only at the very end. The Angular admin panel is out of this sequence (see "Out of scope for this phase").
 
## Epic 0 — Project Foundation (no direct RF, enables everything else) Completed
 
**Backend** (`backend/`, Java 21 + Spring Boot 4.1.1 + Maven):
1. Spring Boot project initialized (`backend/pom.xml`, per-module package structure described in `architecture.md` — for now only `shared/` has real content, see note in that section).
2. `docker-compose.yml` (repo root): PostgreSQL 16 + RabbitMQ 3-management, backend not yet dockerized (runs locally with `mvn spring-boot:run` against that infrastructure).
3. Flyway configured, one migrations folder per module (`backend/src/main/resources/db/migration/<module>/`), with version ranges reserved per module to avoid collisions in the combined history — see convention in `architecture.md` §4. Required adding `spring-boot-flyway` explicitly (Spring Boot 4 split it out of `spring-boot-autoconfigure`).
4. Global error handling + standard error format (`shared/web/{GlobalExceptionHandler, ApiError}`).
5. Health endpoint (`/health`) — simple REST controller, not Spring Boot Actuator.
6. JWT security skeleton in `shared/security` (`SecurityConfig` + `JwtAuthenticationFilter`, `permitAll()` chain for now since there are no users until Epic 1).
7. CORS configured for `http://localhost:5173` (Vite's dev origin).
8. `spring-modulith-starter-test` + `ArchitectureTests` (`backend/src/test/java/.../ArchitectureTests.java`).
**Frontend** (`frontend/`, React 19 + Vite + TypeScript):
1. Project initialized with Vite (`npm create vite@latest -- --template react-ts`), own HTTP client using `fetch` (`src/api/client.ts`) — no Axios, not yet justified with a single endpoint.
2. Minimal screen (`App.tsx`) that calls `/health` and displays the result — validated in a real browser with no CORS errors.
**Exit criteria:** met — `docker compose up -d` brings up Postgres+RabbitMQ, the backend starts and runs all 5 migrations from scratch (verified with `docker compose down -v` + restart), `/health` responds 200, and the frontend displays it on screen.
 
## Epic 1 — Auth/Users (RF1, RF2) Completed
 
Everything else depends on being able to identify who is a producer and who is a buyer.
 
**Backend** (the `auth/` package is created):
1. Registration: name, email, password (bcrypt via `PasswordEncoder`), role — unique and immutable once created. Unique email (409 if it already exists).
2. Login: validates credentials, issues JWT. Spring Security OAuth2 Resource Server was used (Nimbus, HS256 with a symmetric key in `app.jwt.secret`) instead of a manual JWT library (e.g. jjwt) — the skeleton filter `JwtAuthenticationFilter` from Epic 0 was removed because the Resource Server itself already handles Bearer token parsing/validation.
3. Producer's farm profile: department, municipality, village, farm name (`PUT`/`GET /api/auth/farm-profile`, protected with `@PreAuthorize("hasRole('PRODUCER')")` based on the JWT's `role` claim).
4. `AuthModuleApi`: exposes `getUserSummary(userId)` and `isProducer(userId)`.
**Frontend:**
1. Registration form (with role selection) and login (`pages/RegisterPage.tsx`, `pages/LoginPage.tsx`).
2. JWT stored in memory (React state via `auth/AuthContext.tsx`) and automatically sent on subsequent calls — the session is lost on page reload; persistence (localStorage or refresh token) is left to be evaluated later if needed.
3. Farm profile form for the producer (`pages/FarmProfilePage.tsx`).
4. Protected routing with `react-router-dom` (`components/ProtectedRoute.tsx`) — redirects to `/login` without a session, and away from `/farm-profile` if the role is not producer.
**Exit criteria:** met — verified end-to-end in a real browser: a producer registers, logs in, the JWT travels on subsequent calls (including CORS with credentials between `5173`→`8080`), and completes their farm profile; a buyer registers/logs in and cannot access `/farm-profile` (403 backend, redirect in frontend).
 
## Epic 2 — Catalog (RF3, RF4) Completed
 
Depends on Auth to know who is publishing.
 
**Backend** (the `catalog/` package is created):
1. Producer's product CRUD: name, category, unit, quantity, price, photo, municipality, active/sold-out status. Protected with `@PreAuthorize("hasRole('PRODUCER')")` per method (the controller mixes public and producer routes); `producerId` comes from the JWT; ownership check on edit/delete/photo (403 if it belongs to another producer).
2. Catalog listing and filtering (buyer): `GET /api/catalog/products?category=&municipality=` **public** (`permitAll()`), only returns `ACTIVE`, case-insensitive municipality filter, built with `JpaSpecificationExecutor`. Public detail `GET /api/catalog/products/{id}` enriched with the producer's name via `AuthModuleApi`.
3. `CatalogModuleApi.getProductSummary(productId)` → `ProductSummary(id, name, producerId, status, price, unit)` — minimal, only what chat (Epic 3) and transactions (Epic 4) will need.
**Decisions made (see docs/claude/handoffs/handoff-epica-2.md §"Decisions to resolve"):**
- **Photos:** local upload, **one per product**. `POST /api/catalog/products/{id}/photo` (multipart) → stored in `app.uploads.dir` (`./uploads`, gitignored) with a `UUID.ext` name, JPG/PNG/WebP whitelist, 5 MB limit; served as static content under `/media/**` (`shared/config/MediaResourceConfig` bean, path included in `permitAll()`). Rejected: external URL field (poor UX) and an elaborate pipeline / blob store (overkill for the MVP). When catalog is extracted, `PhotoStorage` is swapped for a blob-store client.
- **Sessionless catalog:** the listing and detail `GET` endpoints are public — consistent with the PDR's "catalog = high availability, many reads." Identity is only required when opening chat (Epic 3).
- **Deletion:** logical (`deleted_at`), filtered in all queries and in `CatalogModuleApi`. Avoids orphaned conversations/transactions in later epics.
- **`category` and `unit`:** closed enum (`ProductCategory`, `ProductUnit` in the module's root package, mapped `EnumType.STRING`). `municipality` remains free text (consistent with `FarmProfile`; the form offers a `<datalist>` of Huila municipalities and pre-fills with the one from the farm profile).
- **Status:** `ProductStatus { ACTIVE, SOLD_OUT }`, default `ACTIVE`; `SOLD_OUT` is visible in the detail view but not in the grid, and does not enable chat.
- Added to `shared`: `apiDelete`/`apiUpload`/`mediaUrl` in the frontend client; 400 (`MethodArgumentTypeMismatchException`) and 413 (`MaxUploadSizeExceededException`) handlers in `GlobalExceptionHandler`. Migration `V202__create_products_table.sql`.
**Frontend:**
1. Producer panel (`pages/MyProductsPage`, `pages/ProductFormPage`): create/edit/delete, active/sold-out toggle, photo upload. Routes `/mis-productos[...]` with `ProtectedRoute role="PRODUCER"`.
2. Buyer catalog (`pages/CatalogPage`): grid with category filter (dropdown) and municipality (text). Public route `/catalogo`.
3. Detail view (`pages/ProductDetailPage`, public route `/productos/:id`) with the producer's name and a disabled "Chat" button (placeholder for Epic 3).
**Exit criteria:** met — verified end-to-end in a real browser and with API tests: a producer creates/edits/marks sold out/deletes their products from the UI; a visitor without an account browses and filters the catalog of all producers by category and municipality and opens a product's detail page. `mvn test` (ArchitectureTests) green: `catalog` respects module boundaries, depending only on `auth.AuthModuleApi`.
 
## Epic 3 — Chat (RF5, RF6) Completed
 
Depends on Auth (identity) and Catalog (which product is being discussed and who the producer is).
 
**Backend** (the `chat/` package is created):
1. Open a conversation tied to a product between buyer and producer. `POST /api/chat/conversations` (`hasRole('BUYER')`, `buyerId` from the JWT), validates the product and gets the `producerId` via `CatalogModuleApi.getProductSummary`. Unique per `(product_id, buyer_id)` (constraint), idempotent (201 on creation / 200 if it already existed).
2. Real-time messaging via **STOMP** WebSocket (`spring-boot-starter-websocket`, simple in-memory broker). Handshake at `/ws`; JWT in the `Authorization` header of the `CONNECT` frame, validated by a `ChannelInterceptor` that reuses `shared`'s `JwtDecoder`; `SUBSCRIBE`/`SEND` to `/…/conversations/{id}` require being a participant. Sending via `SEND /app/conversations/{id}/messages`, fan-out through `/topic/conversations/{id}`.
3. Message history per conversation: `GET /api/chat/conversations/{id}/messages` (read-only, ordered by `sent_at` ascending, no pagination). Detail `GET /api/chat/conversations/{id}` and list `GET /api/chat/conversations` (buyer or producer, with names resolved via `auth`/`catalog`).
4. Purchase method: `AgreedPurchaseMethod {PLATFORM, OFF_PLATFORM}` (null = not agreed), `PUT /api/chat/conversations/{id}/purchase-method`, either party can set it, last-write-wins, no state machine.
5. `ChatModuleApi.getAgreedPurchase(conversationId)` → `AgreedPurchase(conversationId, productId, buyerId, producerId, method)` (404 if it doesn't exist). `NewChatMessage` event published when each message is persisted (no listener until Epic 5).
**Frontend:**
1. "Chat" button on `ProductDetailPage` (active for `BUYER`, `ACTIVE` product, and not the producer's own) → `POST` and navigates to `/chat/:id`.
2. `ConversationPage` (`/chat/:conversationId`) with real WebSocket (`@stomp/stompjs`): live messages, automatic reconnection with REST history reload, ordered by `sent_at`. JWT persistence in `localStorage` resolved in this epic (carried over from Epic 1).
3. Purchase method selector inside the chat. `ConversationsPage` (`/chat`) lists "My conversations" (producer entry point).
**Exit criteria:** met — verified end-to-end in a real browser (2 sessions) and with API tests: the buyer opens the chat from the product detail page, buyer and producer exchange live messages without reloading, the conversation is reused when reopened, a third-party user gets 403, and the chosen purchase method persists and is visible on both sides. `mvn test` (ArchitectureTests) green: `chat` respects boundaries, depending only on `auth.AuthModuleApi` and `catalog.CatalogModuleApi`.
 
**Decisions made (see `docs/claude/epica-3-spec.md` §"Decisions made"):**
- **Transport:** STOMP + simple in-memory broker (not RabbitMQ as a relay — that's post-extraction). Native WebSocket was rejected (manual routing/serialization/sessions).
- **JWT over WebSocket:** `Authorization: Bearer` header in the `CONNECT` frame, validated with a `ChannelInterceptor` that reuses `SecurityConfig`'s `JwtDecoder`. `/ws/**` is in `permitAll()` (the handshake carries no token). Never in a query param.
- **JWT persistence (frontend):** `localStorage`, rehydrated by decoding the token's payload (without verifying the signature; discarded if expired). *Refresh token* was rejected (over-engineering for phase 1). A 401 on an authenticated call triggers `auth:expired` → logout.
- **Uniqueness:** one conversation per `(product_id, buyer_id)` (`UNIQUE` constraint); only the buyer starts it, the producer responds.
- **Data model (`V302`):** `conversations` + `messages`, `*_id` as loose UUIDs (no cross-schema FK), no pagination on the history. `AgreedPurchaseMethod` and `NewChatMessage` live in the `chat` root package.
- **Minimal `ChatModuleApi`:** `AgreedPurchase` only carries ids + `method`; `transactions` re-queries price/quantity from `catalog` (not frozen at the time of agreement).
- **`NewChatMessage` event:** already published, within the `postMessage` transaction, even though Epic 5 doesn't have a listener yet.
- **Authorization:** every operation on a conversation (REST and WS) requires the user to be the `buyerId` or the `producerId`. `SOLD_OUT` does not block chat on the backend (UI-only gating). Sending messages is WS-only.
- Added to `shared`: `/ws/**` in `SecurityConfig`'s `permitAll()`. New dependency in `backend/pom.xml`: `spring-boot-starter-websocket`. Frontend: `@stomp/stompjs`, `wsUrl()` in `api/client.ts`.
## Epic 4 — Transactions (RF7, RF8) Completed
 
Depends on Chat (where the on-platform purchase agreement comes from) and Catalog (price/product).
 
**Backend** (the `transactions/` package is created):
1. Integration with **Stripe (test mode)**, **hosted** Checkout Session (redirect): `POST /api/transactions` (`hasRole('BUYER')`, body `{conversationId}`) validates the agreement via `ChatModuleApi.getAgreedPurchase` (must exist, `method == PLATFORM` → 409, caller == buyer → 403) and the product via `CatalogModuleApi` (`ACTIVE` → 409, `quantity > 0`), freezes `quantity`/`unit_price`/`amount` (= `price × quantity`, "the entire listing is purchased"), creates a `Transaction(PENDING)` and the Stripe session. One active transaction (PENDING|CONFIRMED) per conversation → 409.
2. Internal ledger: upon confirmation, one row in `transactions.ledger_entries` (`gross`/`platform_fee`/`net`, append-only, `UNIQUE (transaction_id)`). **0%** commission in phase 1 (`app.transactions.platform-fee-rate = 0.00`), structure ready to activate it.
3. Webhook `POST /api/transactions/webhook/stripe` (`permitAll`, `Stripe-Signature` verified against the raw body → 400 if invalid): `checkout.session.completed` → idempotent `PENDING→CONFIRMED` keyed on `gateway_session_id`; `checkout.session.expired` → `FAILED`.
4. Publishes `TransactionConfirmed(transactionId, conversationId, productId, buyerId, producerId, amount)` within the webhook's transaction (no listener until Epic 5). Also exposes `TransactionsModuleApi.getTransaction(id)` (no consumer yet, contract ready).
**Frontend:**
1. Payment block on `ConversationPage` (buyer, `method == PLATFORM`) → `startCheckout` → `window.location.href = checkoutUrl`. Hosted checkout ⇒ **no SDK/tokenization in the browser** (no dependency was added).
2. `TransactionStatusPage` (`/transacciones/:id`) — pending/confirmed/failed status; polling on return from Stripe with `?pago=ok` until `CONFIRMED`.
3. `ProducerSalesPage` (`/mis-ventas`, `role="PRODUCER"`) — sales with gross/fee/net breakdown and confirmed net total.
**Exit criteria:** met — verified end-to-end (API + browser with real test-mode Stripe Checkout and `stripe listen`): an on-platform purchase agreed in chat, paid with `4242…`, confirmed automatically by the webhook, the buyer sees "Confirmed" via polling, the producer sees the sale with the disbursement in the ledger. Edge cases tested: event resend (idempotent, ledger still 1 row), 2nd transaction on the same conversation → 409, `OFF_PLATFORM` → 409, unrelated third party → 403, invalid signature → 400, producer `POST` → 403. `mvn test` (ArchitectureTests) green: `transactions` only imports `chat.ChatModuleApi`, `catalog.CatalogModuleApi`, `auth.AuthModuleApi` (+ types) and `shared`, plus the Stripe SDK.
 
**Decisions made (see `docs/claude/epica-4-spec.md` §"Decisions made"):**
- **Gateway:** Stripe test mode + hosted Checkout Session (redirect). MercadoPago sandbox was rejected (more cumbersome setup), as was a simulated gateway in the repo (doesn't exercise the real SDK or real signature verification).
- **No SDK in the frontend:** a consequence of the hosted Checkout — the frontend only redirects. This addresses the backlog's concern about tokenization in the browser.
- **Amount:** "the entire listing is purchased" — `amount = price × quantity` at publish time, with `unit_price`/`quantity`/`amount` frozen on the row. The amount is never accepted from the client. Validated against stock but **not deducted** (no RF requires inventory tracking). Required adding `quantity` to `catalog.ProductSummary`.
- **Ledger:** one row per confirmed transaction (`gross`/`fee`/`net`). Double-entry (2 rows with `entry_type`) was rejected. **0%** commission in phase 1; the `platform_fee_amount` column and `PlatformFee` are ready to activate a rate without touching the schema.
- **`TransactionsModuleApi`:** created with `getTransaction(id)` right away even though it has no consumer yet (user decision, against the spec's YAGNI).
- **Currency:** `COP`. Stripe treats it as a two-decimal currency → `unit_amount` = `amount × 100`; the conversion lives only in `StripePaymentGateway`.
- **Cardinality:** one active transaction per conversation (409 with the existing id). A `FAILED` one does not block.
- **Webhook:** `checkout.session.completed` + `checkout.session.expired`; idempotency across three layers (`gateway_session_id`, `Transaction.confirm()` returns `boolean`, `UNIQUE (transaction_id)` on the ledger). `@Transactional` on `handleWebhook` (not just on `confirm`) due to self-invocation. No saga, no outbox: status + ledger + event in a single local transaction.
- **Ledger = record, not execution:** there is no actual transfer to the producer anywhere; the ledger is the "accounts payable" (the "disbursement/payout" step is out of scope for the MVP), as the PDR §7 anticipated.
- **Stripe secrets kept out of git:** `application.yml` is version-controlled with placeholders; real keys via env vars or `backend/config/application.yml` (gitignored). New dependency in `pom.xml`: `com.stripe:stripe-java`. New route in `permitAll()`: the webhook.
## Epic 5 — Notifications (RF9)
 
Depends on Transactions and Chat (they are the ones publishing the events it consumes).
 
**Backend:**
1. `TransactionConfirmed` listener → creates a notification for the buyer (and optionally the producer).
2. `NewChatMessage` listener → creates a notification for the message's recipient.
3. Simple REST endpoint for the user to list their notifications.
**Frontend:**
1. Unread notifications indicator/badge.
2. List of the user's notifications.
**Exit criteria:** when a transaction is confirmed or a new message arrives, a notification appears in the correct user's UI, even if it was generated a few seconds later (asynchronous).
 
## Out of Scope for This Phase (not to be planned yet)
 
- Actual extraction into microservices (Strangler Fig) — only applies once the monolith already works end-to-end.
- Angular admin panel — a separate internal app (lower priority than the React marketplace); planned once the marketplace is complete, does not block any epic above.
- Visual/UX polish of the frontend beyond what's functional — each epic delivers a functional frontend, not the final design.
- Everything the PDR marks as out of scope for the MVP (§2): reputation, tracking of off-platform purchases, identity verification, logistics, map-based geolocation, final selection of a production payment gateway.
## Order Summary
 
```
Epic 0 (foundation) → Epic 1 (auth) → Epic 2 (catalog) → Epic 3 (chat) → Epic 4 (transactions) → Epic 5 (notifications)
```
 
Each epic is demonstrable end-to-end before starting the next one — this avoids building on untested assumptions about a module that doesn't exist yet.
 