# Bounded Context Analysis and Domain Structure
## Huila Agricultural Marketplace (MVP)
 
---
 
## 1. Introduction and General Context
 
This document establishes the identification, structure, and boundaries of the **Bounded Contexts** for the **Huila Agricultural Marketplace (MVP)**. The platform's core objective is to eliminate or reduce intermediaries in the agricultural commercial chain, directly connecting **agricultural producers** in the Huila department with their **buyers**.
 
Since the system's different submodules demand heterogeneous profiles of **availability, latency, and consistency**, the system was designed under a distributed architecture oriented toward microservices. This division allows failures to be isolated, ensures independent scalability, and delimits the functional and data responsibilities of each domain.
 
---
 
## 2. Strategic Domain Summary
 
The following table summarizes the structuring of the domains identified in the PDR, detailing their technology, persistence, communication pattern, and consistency model.
 
| Domain / Bounded Context | Main Responsibility | Backend | Database / Schema | Communication | Priority Quality Attribute |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Authentication and Users** | Identity management, unique and immutable roles, and farm profile. | Java + Spring Boot | PostgreSQL (`auth`) | Synchronous REST | Strong Consistency |
| **Product Catalog** | Publishing, editing, querying, and territorial/category filtering of products. | Java + Spring Boot | PostgreSQL (`catalog`) | Synchronous REST | High Availability |
| **Chat and Messaging** | Real-time communication linked to products for purchase negotiation. | Go | MongoDB | WebSocket (Real time) | Low Latency / High Concurrency |
| **Transactions and Ledger** | Payment processing via sandbox gateway, automatic webhook confirmation, and internal disbursement ledger. | Java + Spring Boot | PostgreSQL (`transactions`) | Synchronous REST / Webhook / Event Producer | Strong Consistency |
| **Notifications** | Sending and logging asynchronous alerts triggered by system events. | Java + Spring Boot | PostgreSQL (`notifications`) | Asynchronous (RabbitMQ Queue) | Fault Tolerance / Eventual Consistency |
| **API Gateway** | Single entry point, REST and WebSocket traffic routing. | Infrastructure | N/A | HTTP / WS Proxy | Routing and Security |
 
---
 
## 3. Domain Identification and Definition
 
### 3.1. Domain 1: Authentication and Users (`auth`)
 
#### A. Responsibilities and Scope
This domain manages the lifecycle of user identity within the platform. It ensures that every registered user holds a single role (Producer or Buyer), which remains **immutable** during the MVP. Additionally, it manages the producers' production profile.
 
#### B. Ubiquitous Language
* **User:** Main entity with credentials (email and bcrypt-hashed password).
* **Role:** Type of actor on the platform (`Producer` or `Buyer`).
* **Farm Profile:** Territorial information of the producer's production unit (Department, Municipality, Village, Farm Name).
* **JWT Token:** Authentication mechanism used to back secure requests.
#### C. Boundaries
* **Within the boundary:** User registration, authentication (login), JWT issuance, querying/updating the producer's farm profile.
* **Outside the boundary:** Biometric or ID/face validation via AI (Phase 2 / Out of scope), user reputation or ratings.
#### D. Technical Structure and Persistence
* **Technology:** Java + Spring Boot.
* **Persistence:** PostgreSQL in the isolated `auth` schema.
* **CAP Pattern:** Prioritizes **Strong Consistency**.
---
 
### 3.2. Domain 2: Product Catalog (`catalog`)
 
#### A. Responsibilities and Scope
Manages the system's agricultural goods offerings. It allows producers to perform CRUD operations on their listings and buyers to browse, search, and filter available offerings.
 
#### B. Ubiquitous Language
* **Product:** Agricultural good offered by a producer.
* **Product Attributes:** Name, category, unit of measure, available quantity, unit price, photographs, municipality of origin.
* **Product Status:** Item availability (`Active` or `Sold Out`).
* **Catalog Filter:** Combined search by agricultural category and municipality.
#### C. Boundaries
* **Within the boundary:** Creation, modification, physical/logical deletion of products; updating active/sold-out status; browsing and filtering by category and municipality.
* **Outside the boundary:** Geolocation via interactive maps (only text/municipality is used), automated inventory control deducted by transactions.
#### D. Technical Structure and Persistence
* **Technology:** Java + Spring Boot.
* **Persistence:** PostgreSQL in the isolated `catalog` schema.
* **CAP Pattern:** Prioritizes **High Availability** (tolerates slightly stale reads due to the read-heavy, write-light pattern).
---
 
### 3.3. Domain 3: Chat and Messaging (`chat`)
 
#### A. Responsibilities and Scope
Supports direct real-time interaction between the buyer and the producer, linked to a specific product. Its central purpose is to enable negotiation of the commercial agreement, defining whether the purchase is made through the platform or outside of it.
 
#### B. Ubiquitous Language
* **Conversation / Room:** Messaging thread started by a buyer with the producer of a specific product.
* **Message:** Text content sent in real time between the parties.
* **On-Platform Purchase Agreement:** Decision to pay via the integrated gateway.
* **External Commercial Agreement:** Free negotiation sharing contact details (WhatsApp, bank account number).
#### C. Boundaries
* **Within the boundary:** Creation of chat threads associated with a product, low-latency message exchange, emission of the `NewChatMessage` event.
* **Outside the boundary:** Tracking, confirmation, or guarantee over sales or money negotiated outside the platform (these remain the direct responsibility of the parties).
#### D. Technical Structure and Persistence
* **Technology:** Go (leveraging goroutines and channels for high concurrency of simultaneous connections).
* **Persistence:** MongoDB (dedicated instance with a document-based data model).
* **Protocol:** WebSocket through the API Gateway.
---
 
### 3.4. Domain 4: Transactions and Ledger (`transactions`)
 
#### A. Responsibilities and Scope
Responsible for the internal and external financial processing of purchases made within the platform. It manages sandbox-mode integration with the payment gateway, processes automatic confirmations via webhook, and records the disbursement of funds to the producer in an internal ledger.
 
#### B. Ubiquitous Language
* **Transaction:** Record of a purchase and payment attempt made within the platform.
* **Payment Gateway (Sandbox):** External processing service in a test environment (no real money).
* **Gateway Webhook:** Inbound HTTP notification sent by the gateway to automatically confirm payment.
* **Internal Disbursement Ledger:** Secondary accounting book used to record and manually control the distribution/disbursement of funds to the producer.
* **`TransactionConfirmed` Event:** Internal notification that triggers the asynchronous flow toward the notifications service.
#### C. Boundaries
* **Within the boundary:** Processing on-platform purchases, receiving payment webhooks, recording in the internal ledger, emitting the `TransactionConfirmed` event.
* **Outside the boundary:** Final selection of a production gateway, handling real money in the MVP, recording or intervening in payments agreed outside the platform.
#### D. Technical Structure and Persistence
* **Technology:** Java + Spring Boot.
* **Persistence:** PostgreSQL in the isolated `transactions` schema.
* **CAP Pattern:** Prioritizes **Strong Consistency** (a payment cannot be left in an ambiguous state).
---
 
### 3.5. Domain 5: Notifications (`notifications`)
 
#### A. Responsibilities and Scope
Asynchronously processes relevant events generated by other domains, ensuring timely user alerts without affecting the performance or availability of the originating services.
 
#### B. Ubiquitous Language
* **Asynchronous Event:** Domain event captured from the messaging broker (RabbitMQ).
* **Transaction Notification:** Message generated when an on-platform purchase is confirmed.
* **Chat Notification:** Alert generated upon receiving a new chat message.
#### C. Boundaries
* **Within the boundary:** Consuming queue events (`TransactionConfirmed`, `NewChatMessage`), logging and history of sent notifications.
* **Outside the boundary:** Synchronous alert generation or blocking of transactions/messages if the notifications service fails.
#### D. Technical Structure and Persistence
* **Technology:** Java + Spring Boot.
* **Persistence:** PostgreSQL in the isolated `notifications` schema.
* **Infrastructure:** RabbitMQ message queue consumer.
* **CAP Pattern:** Prioritizes **Fault Tolerance** and **Eventual Consistency**.
---
 
## 4. Context Map and Domain Integration
 
Interaction between domains is organized by combining **synchronous** patterns (REST / WebSocket) for immediate operations with **asynchronous** patterns (Event-Driven via Broker) for event propagation.
 
```
+-----------------------------------------------------------------------------------+
|                                     CLIENTS                                       |
|             [ Marketplace App (React) ]   /   [ Admin Panel (Angular) ]           |
+-----------------------------------------------------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                                  API GATEWAY                                      |
+-----------------------------------------------------------------------------------+
       |                 |                   |                   |
 (REST | Sync)      (REST | Sync)      (WebSocket | RT)     (REST | Sync)
       v                 v                   v                   v
+--------------+  +--------------+    +--------------+    +----------------------+
| Auth / User  |  |   Catalog    |    | Chat / Msg   |    | Transactions/Ledger  |
| (PostgreSQL) |  | (PostgreSQL) |    |  (MongoDB)   |    |     (PostgreSQL)     |
+--------------+  +--------------+    +--------------+    +----------------------+
                                             |                       |
                                     Produces | Event         Produces | Event
                                    `NewChatMessage`        `TransactionConfirmed`
                                             |                       |
                                             v                       v
                                  +--------------------------------------+
                                  |         RabbitMQ Broker              |
                                  +--------------------------------------+
                                                     |
                                             Consumes | Events
                                                     v
                                          +---------------------+
                                          |    Notifications    |
                                          |    (PostgreSQL)      |
                                          +---------------------+
```
 
### Domain Integration Rules:
1. **Entry Point:** All communication from clients passes through the **API Gateway**.
2. **Cross-Cutting Authentication:** The Catalog, Chat, and Transactions services validate the JWT token originally issued by the **Auth** service.
3. **Notifications Decoupling:** No service makes synchronous calls to **Notifications**. Instead, they publish events to **RabbitMQ**. If Notifications goes down, events remain queued without affecting the overall system operation.
4. **Independence in External Purchases:** **Chat** conversations that agree on payments outside the system do not trigger any interaction with the **Transactions** domain.
---
 
## 5. Data Architecture Considerations and Risks
 
1. **Logical Separation in PostgreSQL:**
   With the exception of Chat (MongoDB), the Auth, Catalog, Transactions, and Notifications services share a single physical PostgreSQL instance, but with **independent, isolated schemas** (`auth`, `catalog`, `transactions`, `notifications`).
2. **Accepted Infrastructure Risk:**
   Although logical isolation exists at the application level, a failure of the physical PostgreSQL engine will simultaneously impact the relational domains. This risk is consciously accepted within the definition of the academic MVP.
3. **Handling of External Transactions:**
   The system does not guarantee, audit, or offer coverage over transactions agreed directly between buyer and producer outside the gateway. The Chat domain functions as a neutral channel.
 