# Architecture — Modular Monolith (Phase 1 / Strangler Fig)
 
> This document defines the internal structure of the monolith and how its modules communicate.
> Goal: make extracting each module into its own microservice later (per the [PDR](PDR.md)) cheap, without over-engineering phase 1.
 
## 1. Guiding Principle
 
A single Spring Boot deployable, with **strict module boundaries but simple mechanisms**. No full hexagonal architecture, CQRS, event sourcing, or saga in this phase — the PDR itself rules them out for cases that don't need them, and this monolith doesn't need them either. The only hard rule is:
 
> **A module never directly accesses another module's tables/entities/repositories.** All interaction goes through the module's public API (method call) or through an event.
 
That single rule is what makes future extraction cheap.
 
## 2. Package Structure
 
**Repo layout:** `backend/` (the Spring Boot deployable) and `frontend/` (React + Vite) are sibling folders at the repo root — there are not two separate repos, but each has its own `pom.xml`/`package.json` and independent build cycle.
 
```
marketplace-agricultural-huila-monolith/
├── docker-compose.yml          # Postgres + RabbitMQ (local infrastructure)
├── backend/                    # Spring Boot deployable — see package tree below
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/huila/marketplace/...
│       ├── main/resources/{application.yml, db/migration/<module>/}
│       └── test/java/com/huila/marketplace/ArchitectureTests.java
└── frontend/                   # React + Vite + TypeScript
    ├── package.json             # includes react-router-dom since Epic 1
    ├── .env / .env.example     # VITE_API_BASE_URL
    ├── index.html
    └── src/
        ├── main.tsx             # BrowserRouter + AuthProvider wrapping <App/>
        ├── App.tsx              # defines <Routes>; Home shows /health + session status
        ├── api/client.ts        # fetch wrapper (apiGet/apiPost/apiPut, optional token), ApiError
        ├── auth/                # AuthContext (JWT in memory — lost on reload, see §5), api.ts (calls to /api/auth/*), types.ts
        ├── components/          # ProtectedRoute (redirects to /login without a session or without the required role)
        └── pages/                # RegisterPage, LoginPage, FarmProfilePage — each epic adds its own here
```
 
**Per-module frontend convention (since Epic 1):** each epic adds its own `pages/` and, if it exposes reusable data, functions in a `<module>/api.ts` — there is no `features/` folder nor Redux/global state beyond `AuthContext`. Authenticated calls use `apiGet/apiPost/apiPut(path, body?, auth.token)`; `ProtectedRoute` accepts an optional `role` to restrict by role, mirroring `@PreAuthorize` on the backend.
 
Inside `backend/`, a single Maven project (not multi-module — avoids the complexity of multiple `pom.xml` files without adding anything in phase 1). Separation by **package per module**, and within each module, 4 simple layers (typical Spring convention, not hexagonal):
 
```
backend/src/main/java/com/huila/marketplace/
├── MarketplaceApplication.java
├── shared/                     # cross-cutting kernel — NO business logic
│   ├── config/                 # common beans (CorsConfigurationSource, OpenAPI)
│   ├── security/                # SecurityConfig: JWT (encoder/decoder/converter), PasswordEncoder
│   └── web/                    # global exception handler, standard error format
│
├── auth/
│   ├── AuthModuleApi.java      # ← the module's single public entry point
│   ├── Role.java, UserSummary.java   # public types exposed by the API
│   ├── domain/                 # User, FarmProfile (JPA entities)
│   ├── application/            # RegisterUserService, LoginService, FarmProfileService, AuthModuleApiImpl
│   ├── infrastructure/         # UserRepository, FarmProfileRepository — `auth` schema
│   └── web/                    # AuthController, FarmProfileController, DTOs
│
├── catalog/
│   ├── CatalogModuleApi.java
│   ├── domain/                 # Product
│   ├── application/            # CreateProduct, UpdateProduct, ListProducts (filters)
│   ├── infrastructure/         # `catalog` schema
│   └── web/
│
├── chat/
│   ├── ChatModuleApi.java
│   ├── domain/                 # Conversation, Message
│   ├── application/            # OpenConversation, SendMessage, AgreePurchaseMethod
│   ├── infrastructure/         # `chat` schema
│   └── web/                    # WebSocket handler + REST history
│
├── transactions/
│   ├── TransactionsModuleApi.java
│   ├── domain/                 # Transaction, LedgerEntry
│   ├── application/            # InitiatePayment, ConfirmPaymentWebhook
│   ├── infrastructure/         # `transactions` schema
│   └── web/                    # gateway webhook endpoint
│
└── notifications/
    ├── domain/                 # Notification
    ├── application/            # event listeners (does not expose an API to other modules)
    ├── infrastructure/         # `notifications` schema
    └── web/                    # REST to list a user's notifications
```
 
> **Current state (post Epic 2):** `shared/` (`config/{CorsConfig, MediaResourceConfig}`, `security/SecurityConfig`, `web/{GlobalExceptionHandler, ApiError, HealthController}`), `auth/` and `catalog/` (complete, 4-layer structure as designed) have real content. `catalog/` exposes `CatalogModuleApi` + public types (`ProductSummary`, `ProductStatus`, `ProductCategory`, `ProductUnit`) and depends on `auth.AuthModuleApi` for the producer's name in the product detail view — a dependency allowed by Modulith (it goes through the module's API). `chat/`, `transactions/`, `notifications/` are still the target design — they come into existence with their first real class in the epic they belong to.
>
> **Product photos (Epic 2):** limited local upload, no blob store. `catalog/infrastructure/PhotoStorage` writes to `app.uploads.dir` (`./uploads`, gitignored) and `shared/config/MediaResourceConfig` serves them as static files under `/media/**` (a path included in `SecurityConfig`'s `permitAll()`). When `catalog` is extracted into a microservice, `PhotoStorage` becomes a blob-store client and the URL remains the same abstraction.
 
**Visibility rule:** the goal is for only `XModuleApi` (and the types it exposes, e.g. `UserSummary`) to be a module's public contract for the rest of the monolith. In practice, `domain/`, `application/`, `infrastructure/`, and `web/` are 4 distinct Java packages within each module (no `module-info.java`, flat classpath), so their classes must be `public` for Spring to inject them across layers of the same module — Java offers no "public only within my module" visibility level. That's why real isolation between modules is **not enforced by the `public`/package-private modifier**, but by `ArchitectureTests` (`spring-modulith-starter-test`): Modulith treats each module's root package (`auth`, `catalog`, ...) as its API and its subpackages (`domain`, `application`, `infrastructure`, `web`) as internal, and fails the build if another module imports something from there directly. The discipline of "only import `XModuleApi`" remains the convention to follow when writing new code — the test only enforces it.
 
**Automated verification:** `spring-modulith-starter-test` + `backend/src/test/java/com/huila/marketplace/ArchitectureTests.java` with `ApplicationModules.of(MarketplaceApplication.class).verify()`. Since `auth` exists (Epic 1) it already enforces the hard rule between modules with real content; it runs on `mvn test`.
 
## 3. Inter-Module Communication
 
Only two mechanisms, each designed as a direct mirror of how the future microservices will communicate (see PDR §4-5):
 
### a) Synchronous → direct call to the module's API
 
Equivalent to the future synchronous REST. A module injects the public interface (`XModuleApi`) of the module it needs and invokes it like a normal Java method — no HTTP, no serialization.
 
```java
// inside chat/application/OpenConversation.java
class OpenConversation {
    private final CatalogModuleApi catalog;   // injected by Spring
 
    Conversation handle(OpenConversationCommand cmd) {
        ProductSummary product = catalog.getProduct(cmd.productId()); // direct call
        ...
    }
}
```
 
When `catalog` is extracted as a microservice, `CatalogModuleApi` is reimplemented as a REST client — the rest of `chat`'s code does not change.
 
### b) Asynchronous → in-process domain events
 
Equivalent to the future RabbitMQ queue. This uses the events already named in the PDR, published with Spring's `ApplicationEventPublisher` and listened to with `@TransactionalEventListener` (fires only if the transaction that originated it committed):
 
- `TransactionConfirmed` — published by `transactions`, consumed by `notifications`.
- `NewChatMessage` — published by `chat`, consumed by `notifications`.
```java
// transactions publishes
applicationEventPublisher.publishEvent(new TransactionConfirmed(transactionId, buyerId));
 
// notifications listens
@TransactionalEventListener
void on(TransactionConfirmed event) { ... }
```
 
`notifications` is the only module that **does not expose** a `ModuleApi` — no one calls it synchronously, it only reacts to events. This already reflects its actual role in the PDR (asynchronous, fault-tolerant).
 
When it is extracted as a microservice, these in-process events start being published to RabbitMQ instead (Spring Modulith has "event externalization" support for this, but it isn't needed yet).
 
### Hard Rule (repeated on purpose)
 
Never: inject another module's `Repository`, import another module's JPA entity, do a JOIN across schemas. Always: go through `XModuleApi` or an event.
 
## 4. Data
 
A single PostgreSQL instance, **one schema per module** (`auth`, `catalog`, `chat`, `transactions`, `notifications`), migrations managed with Flyway under `backend/src/main/resources/db/migration/<module>/`. No module has permission to read another's schema directly — the logical isolation is the same it will have as separate databases after extraction.
 
**Flyway versioning convention (important for any new migration):** `spring.flyway.locations` points to all 5 folders at once, so Flyway combines them into **a single version history** (`flyway_schema_history`, in the `public` schema) — a `V1` in `auth/` and a `V1` in `catalog/` collide, because Flyway only cares about the version number, not the source folder. That's why each module has a **reserved version range**:
 
| Module | Range |
|--------|-------|
| `auth` | `V1xx` |
| `catalog` | `V2xx` |
| `chat` | `V3xx` |
| `transactions` | `V4xx` |
| `notifications` | `V5xx` |
 
E.g.: the next `auth` migration is `V102__...sql`, not `V2__...sql` (that range belongs to `catalog`). `create-schemas` is set to `false`: each module creates its own schema explicitly in its `V1xx__create_schema.sql` — nothing implicit created ahead of time by Flyway.
 
**Dependency note (Spring Boot 4):** starting with Boot 4, Flyway's autoconfiguration moved to its own artifact, `org.springframework.boot:spring-boot-flyway` — having `flyway-core` on the classpath is no longer enough, as it was on Boot 3.x. It is already declared in `backend/pom.xml`.
 
## 5. Authentication, Authorization, and Error Handling (since Epic 1)
 
**JWT:** the monolith itself issues and validates its tokens — there is no external IdP. It uses Spring Security's **OAuth2 Resource Server** (Nimbus) with a **symmetric HS256** key (`app.jwt.secret`, minimum 256 bits; in `application.yml` with `JWT_SECRET` override). Claims: `sub` (userId, UUID), `role` (`PRODUCER`/`BUYER`), `name`, `iss`, `iat`, `exp` (`app.jwt.expiration-minutes`, currently 60). The decoder/encoder/`JwtAuthenticationConverter` (maps the `role` claim to the `ROLE_<role>` authority) live as beans in `shared/security/SecurityConfig`.
 
**Convention for new modules** (repeatable as-is from `catalog` onward):
- Public endpoint → add it to the `permitAll()` list in `SecurityConfig`; everything else requires a valid JWT by default (`anyRequest().authenticated()`).
- Getting the authenticated user in a controller → `@AuthenticationPrincipal Jwt jwt` and `UUID.fromString(jwt.getSubject())`. There's no need to call `AuthModuleApi` just to know who the logged-in user is — the JWT already carries it, signed.
- Restricting an endpoint by role → `@PreAuthorize("hasRole('PRODUCER')")` (requires `@EnableMethodSecurity`, already enabled in `SecurityConfig`) using the role from the JWT itself, not a query to `auth`. `AuthModuleApi.isProducer(userId)`/`getUserSummary(userId)` are reserved for when another module needs user data beyond what the token already carries (e.g. showing the producer's name on a product).
**CORS:** lives in `shared/config/CorsConfig` as a `CorsConfigurationSource` bean (not a `WebMvcConfigurer`) because the Spring Security chain intercepts the request before Spring MVC — with protected endpoints, the `OPTIONS` preflight and the actual CORS handling must be resolved within that chain (`http.cors(...)`), which looks for that kind of bean.
 
**Errors:** `shared/web/GlobalExceptionHandler` is the single place where exceptions are translated into `ApiError`. For new domain code: throw `org.springframework.web.server.ResponseStatusException` with the appropriate `HttpStatus` (e.g. `CONFLICT` on duplicates, `NOT_FOUND`) — no custom business exception hierarchy was created, to avoid over-engineering. `@Valid` + Bean Validation annotations on the `web/` DTOs already return 400 automatically. `AccessDeniedException` (from `@PreAuthorize`) is already mapped to 403.
 
## 6. What Is NOT Implemented in Phase 1
 
Consistent with the PDR (the "when not to use each pattern" table) and with keeping the codebase simple:
 
- No hexagonal/ports-and-adapters per module — the 4-layer separation is already enough.
- No CQRS or Event Sourcing.
- No Saga — the entire purchase flow lives in a single process, local ACID transactions are sufficient.
- No Circuit Breaker — there are no network calls between modules yet.
- No API Gateway — Spring Boot itself exposes the endpoints; the Gateway appears at extraction time.
- No Outbox Pattern — in-process events are already part of the same database transaction (there's no "saved but didn't publish" risk like there is between separate processes).
## 7. Extraction Path (future reference)
 
When a module is ready to be split out: (1) its `ModuleApi` is converted into a REST client, (2) its events are externalized to RabbitMQ, (3) its schema is moved to its own database, (4) it is deployed separately and the Gateway routes traffic to it (Strangler Fig, see `market-agri-docs/05-architecture/pattern-guide.md`). Suggested extraction order: Chat first (most different stack and data model — Go/Mongo), then Notifications (already asynchronous), then the rest based on observed real load.
 