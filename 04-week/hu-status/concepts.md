    # REST API, Worker, Workflow, and Strangler Pattern

## Context

In a platform that starts as a **monolith**, it is common for business logic, persistence, synchronous processes, and background tasks to live inside the same application.

As the system grows, new requirements may justify evolving toward a distributed architecture: greater scalability, domain isolation, independent deployments, asynchronous processing, or different performance requirements.

This transition should not be understood as an obligation to convert the entire monolith into microservices at once. A common strategy is to **progressively extract specific capabilities from the monolith**, keeping the system operational throughout the migration.

In this context, **REST API, Worker, Workflow, and Strangler Pattern** are different but complementary concepts.

---

# 1. REST API

## What is a REST API?

A **REST API (Representational State Transfer)** is an interface through which different clients or systems communicate using REST principles, typically over HTTP.

A REST API exposes resources and operations through endpoints.

Example:

```text
GET    /products
GET    /products/{id}
POST   /products
PUT    /products/{id}
DELETE /products/{id}
```

In a microservices architecture, a REST API can act as the **synchronous entry point** of a service.

## Responsibilities

A REST API typically handles:

- Receiving HTTP requests.
- Authentication and authorization.
- Input validation.
- Executing or delegating business operations.
- Reading or modifying data.
- Returning a response to the client.
- Publishing events or sending messages to asynchronous processes when necessary.

## Example

A client sends:

```http
POST /orders
```

The Order Service may:

1. Validate the request.
2. Create the order.
3. Persist it.
4. Publish an `OrderCreated` event.
5. Immediately respond to the client.

The REST API does not necessarily need to execute the entire business process before responding.

---

# 2. Worker

## What is a Worker?

A **Worker** is a process that executes tasks outside the main interactive flow of an application.

It normally processes jobs coming from a:

- Message queue.
- Message broker.
- Job table.
- Event system.
- Scheduler.

Conceptually:

```text
API
 |
 | publishes message
 v
Queue
 |
 | consumes message
 v
Worker
 |
 +--> processes task
 +--> updates state
 +--> publishes event
```

## Why use a Worker?

Workers are mainly useful for processes that:

- Do not need to execute immediately.
- May take a long time.
- Require retries.
- Consume significant resources.
- Can execute independently.
- Need to scale horizontally.

## Example

A marketplace receives an order:

```text
POST /orders
```

The API creates the order and publishes:

```text
OrderCreated
```

A Worker can subsequently handle:

- Invoice generation.
- Email delivery.
- Statistics updates.
- Seller notifications.
- Reconciliation tasks.
- Resource-intensive processing.

The API does not need to remain blocked while waiting for all these operations.

## Worker vs API

The fundamental difference is the activation mechanism:

```text
API
Client --> HTTP --> Service

Worker
Queue/Event --> Worker
```

An API normally responds to external requests.

A Worker normally responds to jobs or messages.

A single service can contain both:

```text
Order Service
├── REST API
└── Worker
```

---

# 3. Workflow

## What is a Workflow?

A **Workflow** represents a coordinated sequence of steps that must be executed to complete a business or technical process.

It is not simply a function, nor does it necessarily represent a physical infrastructure component.

It is primarily an **orchestration of activities**.

Example:

```text
Create Order
      |
      v
Reserve Inventory
      |
      v
Process Payment
      |
      v
Confirm Order
      |
      v
Notify Customer
```

Each step may involve a different service.

## Workflow inside a Monolith

Initially, a workflow may exist as code inside the monolith:

```text
OrderController
      |
      v
OrderService
      |
      +--> InventoryService
      +--> PaymentService
      +--> NotificationService
```

Even though everything runs inside the same application, there is already a conceptually defined business process.

## Workflow in Microservices

Once the components are separated:

```text
Order Service
      |
      v
Workflow
      |
      +--> Inventory Service
      |
      +--> Payment Service
      |
      +--> Notification Service
```

The Workflow coordinates interactions between services.

## Orchestration vs Choreography

There are two common ways to implement distributed workflows.

### Orchestration

A central component knows the steps and decides what should execute.

```text
             Workflow
            /    |               v     v     v
      Inventory Payment Notification
```

Advantages:

- Explicit flow.
- Easier to visualize the process.
- Centralized control over errors and retries.

Disadvantages:

- The orchestrator can become a coupling point.
- It may accumulate too much business logic if poorly designed.

### Choreography

Services react to events without a central orchestrator.

```text
OrderCreated
     |
     v
Inventory Service
     |
InventoryReserved
     |
     v
Payment Service
     |
PaymentApproved
     |
     v
Notification Service
```

Advantages:

- Less dependency on a central coordinator.
- Services can evolve more independently.

Disadvantages:

- The global process can be harder to understand.
- Tracing and debugging may become more complex.

---

# 4. Strangler Pattern

> The correct name is **Strangler Pattern** or **Strangler Fig Pattern**.

## What is the Strangler Pattern?

The **Strangler Pattern** is an incremental migration strategy used to progressively replace an existing system, typically a monolith, with new components.

The idea is that the new system gradually absorbs functionality while the existing system continues operating.

Conceptually:

```text
                 Client
                    |
                    v
              Entry Point
             /                        v              v
       New Service      Monolith
```

Over time:

```text
                 Client
                    |
                    v
              Entry Point
             /     |                  v      v       v
        Service A Service B Monolith
```

Eventually:

```text
                 Client
                    |
                    v
              Entry Point
             /     |                  v      v       v
        Service A Service B Service C
```

The monolith gradually becomes smaller until it is no longer required.

## How does it work?

A typical migration follows these steps:

1. Identify a capability or bounded context within the monolith.
2. Define its boundaries.
3. Create the new service.
4. Implement the capability outside the monolith.
5. Change the entry point so requests are routed to the new service.
6. Temporarily keep the remaining functionality inside the monolith.
7. Remove the migrated functionality from the monolith.
8. Repeat the process.

Example:

```text
MONOLITH
├── Auth
├── Catalog
├── Orders
├── Payments
└── Notifications
```

First extraction:

```text
                  Gateway
                 /                       v         v
        Auth Service    Monolith
                         ├── Catalog
                         ├── Orders
                         ├── Payments
                         └── Notifications
```

Then:

```text
                  Gateway
             /      |                   v       v        v
          Auth   Catalog   Monolith
                           ├── Orders
                           ├── Payments
                           └── Notifications
```

The process continues until the capabilities that actually require independent deployment, scaling, or ownership have been extracted.

---

# 5. How They Relate

The four concepts solve different problems.

```text
                     CLIENT
                       |
                       v
                 REST API
                       |
                       v
                 Business Logic
                       |
              +--------+--------+
              |                 |
              v                 v
          Workflow            Queue
              |                 |
              |                 v
              |              Worker
              |
       +------+------+------+
       |      |      |
       v      v      v
    Service Service Service
```

They can be understood as follows:

| Concept | Primary responsibility |
|---|---|
| REST API | Expose an HTTP interface for receiving requests |
| Worker | Execute asynchronous or decoupled jobs |
| Workflow | Coordinate a sequence of steps |
| Strangler Pattern | Progressively migrate an existing system |

They are **not alternatives to one another**.

A single system can use all four.

---

# 6. Example Applied to a Marketplace

Suppose the initial platform is:

```text
Marketplace Monolith

├── Authentication
├── Users
├── Catalog
├── Orders
├── Payments
├── Notifications
└── Reporting
```

An order request might initially execute completely inside the monolith:

```text
Client
  |
  v
REST API
  |
  v
Order Service
  |
  +--> Payment
  +--> Inventory
  +--> Notification
```

As the platform grows, the team identifies that Payments requires greater isolation.

The Payment capability is extracted:

```text
                     Client
                       |
                       v
                      API
                       |
              +--------+--------+
              |                 |
              v                 v
       Payment Service       Monolith
                             ├── Users
                             ├── Catalog
                             ├── Orders
                             └── Notifications
```

This extraction is an application of the **Strangler Pattern**.

---

# 7. REST API + Worker + Workflow Inside a Service

A service can combine these concepts.

For example, a `Payment Service` could have:

```text
Payment Service
│
├── REST API
│     ├── POST /payments
│     └── GET /payments/{id}
│
├── Workflow
│     ├── Validate Payment
│     ├── Authorize Payment
│     ├── Capture Payment
│     └── Update Payment Status
│
└── Worker
      ├── Retry Failed Payment
      ├── Process Webhook
      └── Reconcile Transaction
```

Each component has a different responsibility:

- **REST API** receives requests.
- **Workflow** coordinates the process.
- **Worker** executes asynchronous tasks.
- **Strangler Pattern** defines how this new service can progressively replace the equivalent functionality that previously existed in the monolith.

---

# 8. Relationship with Scalability

Separating these responsibilities allows each component to scale according to its specific workload.

For example:

```text
                 API
                  |
            +-----+-----+
            |           |
            v           v
       Order Service   Queue
                        |
                 +------+------+
                 |             |
                 v             v
              Worker        Worker
```

If there is a large number of pending jobs, additional Workers can be added:

```text
Queue
 |
 +--> Worker 1
 +--> Worker 2
 +--> Worker 3
 +--> Worker 4
```

It is not necessary to proportionally increase the number of API instances.

Likewise, if HTTP traffic increases:

```text
Load Balancer
      |
 +----+----+----+
 v    v    v    v
API  API  API  API
```

The API and Workers can scale independently.

---

# 9. An Important Distinction

These concepts belong to different levels of abstraction:

```text
STRANGLER PATTERN
        |
        | evolution strategy
        v
   Architecture
        |
        +----------------------+
        |                      |
        v                      v
   Microservices          Monolith
        |
        +----------------------+
        |
        v
      Service
        |
   +----+----+
   |         |
   v         v
  API      Worker
   |
   v
Workflow
```

This does **not** mean that a Workflow must be below an API or that a Worker is mandatory in a microservice. It is simply a way to visualize how these concepts can be combined.

---

# 10. Architectural Principle

The central idea should not be:

> "We have a monolith, therefore we must convert it into microservices."

The decision should be based on concrete needs:

- Independent scalability.
- Different workload profiles.
- Fault isolation.
- Independent deployments.
- Team autonomy.
- Different technological requirements.
- Asynchronous processing.
- Clearly defined domain boundaries.

Therefore, a reasonable evolution can be:

```text
MONOLITH
   |
   | identify boundaries
   v
MODULAR MONOLITH
   |
   | extract capabilities as needed
   v
HYBRID ARCHITECTURE
   |
   | continue selective extraction
   v
MICROSERVICES
```

Not every module needs to become a microservice.

---

# 11. Conceptual Summary

```text
REST API
"How does a request enter the system?"

WORKER
"Who executes a task asynchronously?"

WORKFLOW
"How do we coordinate multiple steps to complete a process?"

STRANGLER PATTERN
"How do we progressively replace part of an existing system?"
```

An evolved architecture could look like:

```text
                         CLIENT
                           |
                           v
                      API Gateway
                           |
          +----------------+----------------+
          |                                 |
          v                                 v
    New Services                         Monolith
          |                            (remaining)
          |
     +----+----------------+
     |                     |
     v                     v
 REST API               Workflow
                           |
                     +-----+------+
                     |            |
                     v            v
                  Services      Queue
                                  |
                                  v
                               Workers
```

The key distinction is:

**REST API, Worker, and Workflow are execution and communication mechanisms/components, while the Strangler Pattern is an architectural strategy for evolving the system from a monolith toward a distributed architecture.**
