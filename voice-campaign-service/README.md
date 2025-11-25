# Outbound Voice Campaign Microservice

## Overview
This microservice manages outbound voice campaigns, allowing users to create campaigns, schedule calls within business hours, manage concurrency, and handle retries for failed calls.

## Features
- **Campaign Management**: Create, Start, Pause, and Get Campaigns.
- **Business Hour Scheduling**: Calls are only triggered within specified start and end times (timezone aware).
- **Concurrency Control**: Limits the number of active calls per campaign.
- **Retry Logic**: Automatically retries failed calls up to a configured limit.
- **Status Tracking**: Tracks status of individual calls and overall campaign stats.
- **Mock Telephony**: Simulates call triggering and status updates.

## Tech Stack
- **Java 17**
- **Spring Boot 3.4.0** (Web, Data JPA, Validation)
- **H2 Database** (Default for development/testing)
- **PostgreSQL** (Production profile)
- **Docker & Docker Compose** (For running Postgres)

## Setup Instructions

### Prerequisites
- Java 17+ installed
- Maven (or use included `mvnw`)
- Docker (optional, for Postgres)

### Running Locally (H2)
1.  Clone the repository.
2.  Run the application:
    ```bash
    ./mvnw spring-boot:run
    ```
3.  The application will start on port `8080`.
4.  H2 Console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:voicecampaign`, User: `sa`, Password: `password`).

### Running with Docker Compose (Full Stack)
1.  Build the application:
    ```bash
    ./mvnw clean package -DskipTests
    ```
2.  Start the application and database:
    ```bash
    docker-compose up --build
    ```
3.  The application will be available at `http://localhost:8080`.

### Running Locally with Docker Postgres
1.  Start only the database:
    ```bash
    docker-compose up postgres -d
    ```
2.  Run the application with `prod` profile:
    ```bash
    ./mvnw spring-boot:run -Dspring-profiles.active=prod
    ```

### Running Tests
```bash
./mvnw test
```

## API Usage

### 1. Create a Campaign
**POST** `/campaigns`
```json
{
  "name": "Welcome Offer",
  "phoneNumbers": ["+1234567890", "+1987654321"],
  "startTime": "09:00:00",
  "endTime": "17:00:00",
  "timezone": "America/New_York",
  "concurrencyLimit": 5,
  "retryCount": 3
}
```

### 2. Start a Campaign
**POST** `/campaigns/{id}/start`

### 3. Get Campaign Details
**GET** `/campaigns/{id}`

### 4. Pause a Campaign
**POST** `/campaigns/{id}/pause`

### 5. Trigger Single Call (Existing API)
**POST** `/calls`
```json
{
  "phone_number": "+1234567890"
}
```

## System Design
See [SYSTEM_DESIGN.md](SYSTEM_DESIGN.md) for architecture details.
