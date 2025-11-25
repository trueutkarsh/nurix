# System Design - Outbound Voice Campaign Microservice

## Architecture Overview
The system is designed as a monolithic microservice using Spring Boot, following a layered architecture (Controller -> Service -> Repository). It uses a relational database (H2/Postgres) for persistence and a scheduled task for managing campaign lifecycles.

### Components

1.  **API Layer (`Controller`)**:
    -   Exposes REST endpoints for Campaign Management and Call Triggering.
    -   Handles input validation and response formatting.

2.  **Service Layer (`Service`)**:
    -   **CampaignService**: Manages campaign CRUD, state transitions (Start/Pause), and statistics aggregation.
    -   **CallService (Mock)**: Simulates the telephony provider. Generates call IDs and simulates status changes (IN_PROGRESS -> COMPLETED/FAILED).
    -   **CampaignScheduler**: The core engine. Runs periodically to:
        -   Fetch running campaigns.
        -   Check constraints (Business Hours, Concurrency).
        -   Fetch eligible numbers (Pending or Retryable Failed).
        -   Trigger calls via `CallService`.
        -   Poll for status updates of in-progress calls.

3.  **Data Layer (`Repository`)**:
    -   Uses Spring Data JPA to interact with the database.
    -   **CampaignRepository**: Manages campaign entities.
    -   **PhoneNumberRepository**: Manages phone numbers and their statuses. Optimized queries for fetching eligible numbers and counting stats.

4.  **Database**:
    -   **Campaigns Table**: Stores campaign config (schedule, limits) and status.
    -   **PhoneNumbers Table**: Stores individual numbers, their status, retry counts, and linkage to campaigns.

## Key Design Decisions

### 1. Scheduling & Concurrency
-   **Poller vs Queue**: A scheduled poller (`@Scheduled`) was chosen for simplicity and to easily handle concurrency limits per campaign. A queue (like RabbitMQ) is great for throughput but makes "max concurrent calls per campaign" harder to enforce strictly without complex rate limiting.
-   **Concurrency Control**: The scheduler checks `count(IN_PROGRESS)` before triggering new calls. This is "soft" concurrency. For strict guarantees in a distributed system, we would need distributed locks (Redis) or database row locking (`SELECT FOR UPDATE`).

### 2. Business Hours
-   Timezones are handled using Java's `ZoneId` and `ZonedDateTime`. The scheduler checks the current time in the campaign's timezone against the configured start/end times before processing.

### 3. Fault Tolerance & Retries
-   **Retries**: Failed calls are marked as `FAILED`. The scheduler query includes logic to pick up `FAILED` calls if `retriesAttempted < maxRetries`.
-   **Isolation**: Each campaign is processed independently. An error in one campaign doesn't stop others.
-   **Database**: Using a relational DB ensures ACID properties for status updates.

### 4. Scalability
-   **Horizontal Scaling**: To scale this service, we would need to ensure the scheduler doesn't process the same campaign on multiple instances simultaneously. This can be achieved using:
    -   **ShedLock**: To ensure only one instance runs the scheduler.
    -   **Partitioning**: Assign campaigns to specific instances (sharding).
-   **Async Processing**: The `CallService` trigger is blocking in this mock but should be async in production (returning immediately, with status updates via Webhook).

## External Components (Production Recommendations)
-   **Database**: PostgreSQL for reliability and complex queries.
-   **Message Queue**: RabbitMQ/Kafka for decoupling call triggering if throughput is high.
-   **Cache**: Redis for storing real-time stats and concurrency counters to reduce DB load.
