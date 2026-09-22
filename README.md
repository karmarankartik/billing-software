# Water Billing System

A backend water billing system built with **Java 17 and Spring Boot** for managing users, water meters, meter assignments, meter readings, billing plans, and monthly invoice generation.

The application exposes REST APIs for authentication, user management, water meter management, billing-plan management, meter assignment, meter-reading ingestion, and invoice generation.

The database schema is version-controlled and managed using **Flyway** migrations.

---

## 1. Technology Stack

* Java 17
* Spring Boot
* Spring Web MVC
* Spring Data JPA
* Hibernate
* H2 Database
* Flyway
* Maven
* Argon2 password hashing
* Password4j
* OpenAPI / Swagger UI
* Spring Boot Actuator
* Docker

---

## 2. Core Features

### Authentication

* Username/password login
* Token-based session authentication
* Bearer-token authentication for protected APIs
* Session revocation through logout
* Password hashing using Argon2

### User Management

* Create users
* Retrieve users
* Update users
* Delete users
* Support for `ADMIN` and `CUSTOMER` roles

### Water Meter Management

* Create water meters
* Retrieve a meter by ID
* Retrieve all meters
* Update meters
* Delete meters
* Retrieve meters assigned to the logged-in user

### Meter Assignment

* Assign a water meter to a user
* Unassign a water meter
* Maintain assignment history

A meter can have an active assignment while historical assignments remain available for billing-period calculations.

### Meter Reading Ingestion

Supports ingestion of water-meter readings with:

* Meter ID
* Reading type
* Reading value
* Reading timestamp
* Ingestion key

Supported reading types:

```text
TOTAL
FLOW
```

The ingestion key is used to support idempotent reading ingestion.

### Billing Plans

Supports:

* Fixed-rate billing
* Slab-based billing
* Billing-plan CRUD operations
* Billing slabs
* Meter billing-plan history

A meter can have different billing plans over time. This allows historical billing to use the billing plan applicable to the relevant period.

### Monthly Invoice Generation

Invoice generation is based on a **year and month**, rather than accepting arbitrary `from` and `to` dates.

Two invoice-generation modes are available:

1. **ADMIN** — bulk generation for all applicable meters
2. **CUSTOMER** — generation for the logged-in user's assigned meters

---

# 3. Architecture

The application follows a layered architecture:

```text
                    REST API
                       |
                       v
                +--------------+
                | Controllers  |
                +--------------+
                       |
                       v
                +--------------+
                |  Services    |
                +--------------+
                       |
                       v
                +--------------+
                | Repositories |
                +--------------+
                       |
                       v
                +--------------+
                |     H2       |
                |   Database   |
                +--------------+
```

### Controller Layer

Responsible for:

* REST endpoints
* Request handling
* Request validation
* Authentication context
* HTTP responses

### Service Layer

Contains business logic for:

* Users
* Water meters
* Assignments
* Billing plans
* Meter readings
* Invoice generation
* Bulk billing jobs

### Repository Layer

Uses Spring Data JPA for persistence and database access.

### Database

H2 is used for the current implementation.

Flyway manages database schema evolution.

---

# 4. Project Structure

```text
billing-software/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ubiqedge/billing_software/
│   │   │       ├── config/
│   │   │       ├── constant/
│   │   │       ├── controller/
│   │   │       ├── dto/
│   │   │       ├── entity/
│   │   │       ├── exception/
│   │   │       ├── filter/
│   │   │       ├── mapper/
│   │   │       ├── repository/
│   │   │       ├── script/
│   │   │       ├── service/
│   │   │       ├── util/
│   │   │       └── validation/
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/
│   │           └── migration/
│   │               └── V1__initial_schema.sql
│   │
│   └── test/
│
├── pom.xml
├── Dockerfile
└── README.md
```

---

# 5. Database Schema Management

Database schema management is handled by **Flyway**.

Migration files are stored under:

```text
src/main/resources/db/migration/
```

The initial migration is:

```text
V1__initial_schema.sql
```

The application configuration enables Flyway:

```properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=none
```

Hibernate therefore does not create or update the database schema.

Future schema changes should be introduced through additional Flyway migrations, for example:

```text
V2__add_payment_table.sql
V3__add_invoice_status.sql
V4__add_meter_configuration.sql
```

---

# 6. Database Model

The primary database tables include:

```text
users
user_sessions

billing_plans
billing_plan_slabs

water_meters
water_meter_assignments
water_meter_billing_plans

water_meter_readings

invoices
invoice_items

billing_generation_jobs
billing_generation_job_skips
```

### Users

Stores application users, credentials, roles, and audit information.

### User Sessions

Stores authentication tokens, creation time, expiration time, and revocation information.

### Billing Plans

Stores fixed and slab-based billing plans.

### Billing Plan Slabs

Stores individual slabs associated with a slab-based billing plan.

### Water Meters

Stores registered water meters.

### Water Meter Assignments

Stores the relationship between a water meter and a user, including assignment history.

### Water Meter Billing Plans

Stores the billing-plan history associated with each meter.

### Water Meter Readings

Stores meter readings and ingestion information.

### Invoices

Stores generated invoices.

### Invoice Items

Stores individual components of an invoice.

### Billing Generation Jobs

Tracks asynchronous administrator bulk-invoice generation.

### Billing Generation Job Skips

Stores meters that could not be billed during a bulk generation job and the corresponding reason.

---

# 7. Authentication

The application uses token-based authentication.

## Login

```http
POST /api/auth/login
```

Request:

```json
{
  "username": "admin-user",
  "password": "adminuser"
}
```

A successful login returns an authentication token.

For protected APIs, send the token using:

```http
Authorization: Bearer <token>
```

## Logout

```http
POST /api/auth/logout
```

The token is supplied through the `Authorization` header.

Example:

```http
Authorization: Bearer <token>
```

---

# 8. Default Development Admin

The development environment includes the following administrator account:

```text
Username: admin-user
Password: adminuser
Role: ADMIN
```

Use this account to authenticate and access administrator functionality.

### Security Note

These credentials are intended for development/testing.

They should be changed or removed before a production deployment.

---

# 9. API Reference

The application exposes the following REST APIs.

All protected APIs require a valid Bearer token.

---

## 9.1 Authentication APIs

### Login

```http
POST /api/auth/login
```

Request:

```json
{
  "username": "admin-user",
  "password": "adminuser"
}
```

### Logout

```http
POST /api/auth/logout
```

Header:

```http
Authorization: Bearer <token>
```

---

# 10. User APIs

Base path:

```text
/api/users
```

### Create User

```http
POST /api/users
```

Request:

```json
{
  "username": "customer1",
  "password": "password",
  "role": "CUSTOMER"
}
```

### Get User

```http
GET /api/users/{id}
```

### Update User

```http
PUT /api/users/{id}
```

Request:

```json
{
  "username": "customer1",
  "password": "new-password",
  "role": "CUSTOMER"
}
```

### Delete User

```http
DELETE /api/users/{id}
```

---

# 11. Water Meter APIs

Base path:

```text
/api/water-meters
```

### Create Water Meter

```http
POST /api/water-meters
```

Example:

```json
{
  "meterNumber": "WM-001",
  "billingPlanId": "billing-plan-uuid"
}
```

### Get Water Meter

```http
GET /api/water-meters/{id}
```

### Get All Water Meters

```http
GET /api/water-meters
```

### Get My Water Meters

Returns meters assigned to the authenticated user.

```http
GET /api/water-meters/my
```

### Update Water Meter

```http
PUT /api/water-meters/{id}
```

Example:

```json
{
  "meterNumber": "WM-001",
  "billingPlanId": "billing-plan-uuid"
}
```

### Delete Water Meter

```http
DELETE /api/water-meters/{id}
```

---

# 12. Water Meter Assignment APIs

Base path:

```text
/api/water-meter-assignments
```

Assignment operations are administrator-controlled.

### Assign Meter

```http
POST /api/water-meter-assignments
```

Request:

```json
{
  "waterMeterId": "meter-uuid",
  "userId": "user-uuid"
}
```

### Unassign Meter

```http
DELETE /api/water-meter-assignments
```

Request:

```json
{
  "waterMeterId": "meter-uuid",
  "userId": "user-uuid"
}
```

The assignment table maintains historical assignment information using:

```text
assigned_at
unassigned_at
```

This history is important for determining which user was responsible for a meter during a billing period.

---

# 13. Billing Plan APIs

Base path:

```text
/api/billing-plans
```

### Create Billing Plan

```http
POST /api/billing-plans
```

Example fixed-rate plan:

```json
{
  "name": "Residential Fixed",
  "code": "RES_FIXED",
  "description": "Residential fixed-rate plan",
  "pricePerUnit": 25.00,
  "planType": "FIXED",
  "slabs": []
}
```

Example slab plan:

```json
{
  "name": "Residential Slab",
  "code": "RES_SLAB",
  "description": "Residential slab plan",
  "pricePerUnit": null,
  "planType": "SLAB",
  "slabs": [
    {
      "lowerBound": 0,
      "upperBound": 10,
      "pricePerUnit": 10
    },
    {
      "lowerBound": 10,
      "upperBound": 20,
      "pricePerUnit": 15
    },
    {
      "lowerBound": 20,
      "upperBound": null,
      "pricePerUnit": 20
    }
  ]
}
```

### Get Billing Plan

```http
GET /api/billing-plans/{id}
```

### Get All Billing Plans

```http
GET /api/billing-plans
```

### Update Billing Plan

```http
PUT /api/billing-plans/{id}
```

### Delete Billing Plan

```http
DELETE /api/billing-plans/{id}
```

Billing-plan administration is restricted to administrator users.

---

# 14. Water Meter Reading API

Base path:

```text
/api/water-meter-readings
```

### Ingest Reading

```http
POST /api/water-meter-readings
```

Example:

```json
{
  "waterMeterId": "meter-uuid",
  "readingType": "TOTAL",
  "readingValue": 1250.50,
  "readingAt": "2026-08-31T18:30:00Z",
  "ingestionKey": "meter-001-20260831-183000"
}
```

Supported reading types:

```text
TOTAL
FLOW
```

The `ingestionKey` identifies the ingestion event and supports duplicate protection.

---

# 15. Invoice APIs

Invoice endpoints are divided into administrator and customer functionality.

---

## 15.1 Customer - Get My Invoices

```http
GET /api/invoices
```

Returns invoices belonging to the authenticated user.

No user ID is supplied in the request because the user is identified from the authenticated session.

---

## 15.2 Customer - Generate My Monthly Invoices

```http
POST /api/invoices/generate
```

The request specifies the **billing year and month**.

Example:

```json
{
  "year": 2026,
  "month": 8
}
```

The system converts this into the corresponding monthly billing period internally.

For example:

```text
year  = 2026
month = 8

Billing month = August 2026
```

The system then finds the meters assigned to the authenticated user for the relevant billing period and generates the applicable invoices.

The customer does not provide:

```text
from
to
billingPeriodStart
billingPeriodEnd
```

The API accepts only:

```text
year
month
```

---

# 16. Administrator Bulk Invoice Generation

Administrators can initiate invoice generation for the entire meter population.

### Start Bulk Invoice Generation

```http
POST /api/admin/invoices/generate
```

Request:

```json
{
  "year": 2026,
  "month": 8
}
```

This endpoint is **ADMIN-only**.

The system converts the supplied year/month into the monthly billing period internally.

The operation starts an asynchronous billing-generation job rather than keeping the HTTP request open until all meters have been processed.

The response contains information about the created generation job.

---

# 17. Billing Generation Job

After starting an administrator bulk billing operation, the returned job ID can be used to check its status.

### Get Generation Job

```http
GET /api/admin/invoices/generation-jobs/{jobId}
```

This endpoint is **ADMIN-only**.

The job tracks information such as:

```text
Job ID
Billing period
Status
Meters processed
Invoices generated
Meters skipped
Start time
Completion time
```

Skipped meters can include a reason explaining why an invoice could not be generated.

---

# 18. Monthly Billing Flow

The overall monthly billing flow is:

```text
                 year + month
                      |
                      v
             Validate billing month
                      |
                      v
             Determine monthly period
                      |
                      v
        Find applicable meter assignments
                      |
                      v
             Retrieve meter readings
                      |
                      v
            Calculate consumption
                      |
                      v
       Determine applicable billing plan
                      |
                      v
          Calculate billing amount
                      |
                      v
              Create invoice
                      |
                      v
             Create invoice items
```

For administrator bulk generation:

```text
Admin
  |
  | year + month
  v
POST /api/admin/invoices/generate
  |
  v
Create billing generation job
  |
  v
Asynchronous processing
  |
  +---- Meter 1 -> Invoice
  |
  +---- Meter 2 -> Invoice
  |
  +---- Meter 3 -> Skipped
  |
  +---- ...
  |
  v
Job completed
```

---

# 19. Billing Period Validation

The invoice-generation API validates the requested billing month.

The request must contain:

```json
{
  "year": 2026,
  "month": 8
}
```

### Month

The month must be between:

```text
1 - 12
```

### Future/current month

The requested billing month must represent a completed month.

For example, if the current month is:

```text
October 2026
```

then:

```text
September 2026
```

is a completed billing month, while:

```text
October 2026
```

is not yet eligible.

### Billing history limit

The current validation also limits billing generation to the configured historical window of **two years**.

---

# 20. Billing Plan History

Billing plans are maintained historically for meters.

A meter can have:

```text
Plan A
  |
  | effective period
  v
Plan B
  |
  | effective period
  v
Plan C
```

This allows invoice calculation to identify the billing plan applicable to the meter during the relevant billing period.

The database stores:

```text
effective_from
effective_to
```

for meter billing-plan history.

---

# 21. Meter Assignment History

Meter assignments are also historical.

Example:

```text
M1 -> User A
     assigned:   Jan 1
     unassigned: Mar 31

M1 -> User B
     assigned:   Apr 1
     unassigned: Jun 30

M1 -> User C
     assigned:   Jul 1
     active
```

This allows billing logic to determine which user was associated with the meter for a particular billing period.

Only one active assignment is permitted for a meter.

---

# 22. H2 Database

The application currently uses a file-based H2 database.

Configured database:

```text
jdbc:h2:file:./data/billingdb
```

Database username:

```text
db-admin
```

Database password:

```text
adminuser
```

The database is stored under:

```text
./data/
```

This allows data to persist across normal application restarts.

---

# 23. H2 Console

The H2 console is enabled for local development.

URL:

```text
http://localhost:8080/h2-console
```

Use the configured JDBC URL:

```text
jdbc:h2:file:./data/billingdb
```

Username:

```text
db-admin
```

Password:

```text
adminuser
```

The H2 console is intended for development/testing and should not be exposed in a production deployment.

---

# 24. Swagger / OpenAPI

The application includes OpenAPI documentation through Springdoc.

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

OpenAPI YAML:

```text
http://localhost:8080/v3/api-docs.yaml
```

Swagger UI provides an interactive interface for exploring and testing the REST APIs.

For protected APIs, provide the authentication token where required.

---

# 25. Spring Boot Actuator

Spring Boot Actuator is enabled for application monitoring.

Available endpoints include:

```text
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
```

### Health

```http
GET /actuator/health
```

Used to check application health.

### Metrics

```http
GET /actuator/metrics
```

Provides access to application/runtime metrics exposed by the Actuator metrics system.

Example:

```http
GET /actuator/metrics/jvm.memory.used
```

Actuator endpoints should be appropriately secured and restricted in production environments.

---

# 26. Error Handling

The application uses centralized application constants for reusable messages and a common API exception mechanism.

This provides consistent error responses across different APIs.

Examples of validation areas include:

* Invalid billing year
* Invalid billing month
* Invalid user role
* Duplicate users
* Invalid meter assignments
* Invalid billing plans
* Invalid meter readings
* Billing generation already in progress
* Missing billing generation job

---

# 27. Building the Application

### Prerequisites

Install:

* Java 17
* Maven 3.9+
* Docker (optional)

Verify Java:

```bash
java -version
```

Verify Maven:

```bash
mvn -version
```

---

# 28. Run Locally

Build the application:

```bash
mvn clean package
```

Run tests:

```bash
mvn test
```

Run the application:

```bash
mvn spring-boot:run
```

Alternatively, run the generated JAR:

```bash
java -jar target/billing-software-0.0.1-SNAPSHOT.jar
```

The application runs on:

```text
http://localhost:8080
```

---

# 29. Docker

The project includes a Dockerfile for containerized deployment.

### Build Image

```bash
docker build -t water-billing-system .
```

### Run Container

```bash
docker run -p 8080:8080 water-billing-system
```

The application will be available at:

```text
http://localhost:8080
```

---

# 30. Persistent H2 Data With Docker

Because H2 uses a file-based database, the application data can be persisted using a Docker volume.

```bash
docker run \
  -p 8080:8080 \
  -v billing-data:/app/data \
  water-billing-system
```

The volume keeps the H2 database files outside the container filesystem.

---

# 31. Docker Build Architecture

The Dockerfile uses a multi-stage build:

```text
Maven Build Stage
       |
       | mvn clean package
       v
Spring Boot JAR
       |
       v
Java 17 Runtime Stage
       |
       v
Container
```

The final runtime image does not need Maven or the source code.

---

# 32. API Summary

| Area              | Method | Endpoint                                      |
| ----------------- | ------ | --------------------------------------------- |
| Authentication    | POST   | `/api/auth/login`                             |
| Authentication    | POST   | `/api/auth/logout`                            |
| Users             | POST   | `/api/users`                                  |
| Users             | GET    | `/api/users/{id}`                             |
| Users             | PUT    | `/api/users/{id}`                             |
| Users             | DELETE | `/api/users/{id}`                             |
| Water Meters      | POST   | `/api/water-meters`                           |
| Water Meters      | GET    | `/api/water-meters`                           |
| Water Meters      | GET    | `/api/water-meters/{id}`                      |
| Water Meters      | GET    | `/api/water-meters/my`                        |
| Water Meters      | PUT    | `/api/water-meters/{id}`                      |
| Water Meters      | DELETE | `/api/water-meters/{id}`                      |
| Meter Assignment  | POST   | `/api/water-meter-assignments`                |
| Meter Assignment  | DELETE | `/api/water-meter-assignments`                |
| Meter Readings    | POST   | `/api/water-meter-readings`                   |
| Billing Plans     | POST   | `/api/billing-plans`                          |
| Billing Plans     | GET    | `/api/billing-plans`                          |
| Billing Plans     | GET    | `/api/billing-plans/{id}`                     |
| Billing Plans     | PUT    | `/api/billing-plans/{id}`                     |
| Billing Plans     | DELETE | `/api/billing-plans/{id}`                     |
| Customer Invoices | GET    | `/api/invoices`                               |
| Customer Billing  | POST   | `/api/invoices/generate`                      |
| Admin Billing     | POST   | `/api/admin/invoices/generate`                |
| Admin Billing Job | GET    | `/api/admin/invoices/generation-jobs/{jobId}` |
| Monitoring        | GET    | `/actuator/health`                            |
| Monitoring        | GET    | `/actuator/info`                              |
| Monitoring        | GET    | `/actuator/metrics`                           |

---

# 33. Quick Start

```bash
git clone https://github.com/karmarankartik/billing-software.git

cd billing-software

mvn clean package

mvn spring-boot:run
```

Then open Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

Login using:

```json
{
  "username": "admin-user",
  "password": "adminuser"
}
```

Use the returned token as:

```text
Authorization: Bearer <token>
```

For monthly billing, use:

```http
POST /api/admin/invoices/generate
```

with:

```json
{
  "year": 2026,
  "month": 8
}
```

The administrator bulk billing operation runs asynchronously. Use the returned job ID with:

```http
GET /api/admin/invoices/generation-jobs/{jobId}
```

---

# 34. Production Considerations

The current H2 configuration is intended primarily for development, testing, and demonstration.

For production deployment, consider:

* PostgreSQL or another production-grade relational database
* Externalized database credentials
* Secret management
* HTTPS/TLS
* Secure Actuator endpoints
* Removal/change of default admin credentials
* H2 console disabled
* Database backups
* Centralized logging
* Monitoring and alerting
* Container resource limits
* Health checks
* CI/CD
* Database migration management through Flyway

---

# 35. Design Principles

The project follows these principles:

* Layered architecture
* Separation of concerns
* RESTful API design
* Centralized application constants
* Centralized exception handling
* Request validation
* Token-based authentication
* Argon2 password hashing
* Historical meter assignments
* Historical billing-plan assignments
* Idempotent meter-reading ingestion
* Monthly billing-period processing
* Asynchronous administrator bulk billing
* Flyway-based database versioning
* Application observability through Actuator

---

# 36. Repository

GitHub repository:

https://github.com/karmarankartik/billing-software
