# Water Billing Software

A Spring Boot backend for managing users, water meters, meter assignments, billing plans, meter readings, invoice generation, and historical invoices.

The application is designed around a water-meter billing lifecycle:

`User → Water Meter → Billing Plan → Assignment → Readings → Billing Calculation → Invoice → Invoice Items`

## Features

* User CRUD with soft deletion
* Session-based authentication using Bearer tokens
* Admin and customer roles
* Argon2 password hashing through Password4j
* Water meter CRUD
* Water meter assignment and unassignment
* Fixed-rate and slab-based billing plans
* Billing plan history per water meter
* Water meter reading ingestion
* Idempotent reading ingestion using an ingestion key
* Duplicate reading protection
* Monthly invoice generation
* Bulk invoice generation for administrators
* Customer-specific invoice generation
* Asynchronous billing generation jobs
* Billing-generation job status and skipped-meter tracking
* Historical invoices with invoice-item breakdowns
* Explicit SQL-managed H2 schema
* Centralized application constants and API error messages
* Global exception handling and request validation

## Technology Stack

* Java 17
* Spring Boot 4.1.1
* Spring Web MVC
* Spring Data JPA
* H2 Database
* Maven
* Password4j
* Argon2 password hashing

## Architecture

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
H2 Database
```

Supporting layers include DTOs, entities, mappers, validators, authentication interception, and global exception handling.

## Project Structure

```text
src/main/java/com/ubiqedge/billing_software
├── config
├── constant
├── controller
├── dto
├── entity
├── exception
├── filter
├── mapper
├── repository
├── script
├── service
├── util
└── validation

src/main/resources
├── application.properties
├── psw4j.properties
└── schema.sql
```

## Database

The application uses a file-based H2 database:

```text
./data/billingdb
```

The schema is explicitly managed by:

```text
src/main/resources/schema.sql
```

JPA does not create or update tables automatically.

### Main Tables

| Table                          | Purpose                                        |
| ------------------------------ | ---------------------------------------------- |
| `users`                        | Application users and roles                    |
| `user_sessions`                | Login sessions and token lifecycle             |
| `billing_plans`                | Fixed and slab billing plans                   |
| `billing_plan_slabs`           | Slab pricing ranges                            |
| `water_meters`                 | Water meter master data                        |
| `water_meter_assignments`      | Current and historical meter assignments       |
| `water_meter_billing_plans`    | Billing-plan history for meters                |
| `water_meter_readings`         | Meter readings                                 |
| `invoices`                     | Generated invoice headers                      |
| `invoice_items`                | Billing-plan segments and invoice calculations |
| `billing_generation_jobs`      | Bulk billing job tracking                      |
| `billing_generation_job_skips` | Meters skipped during generation               |

## Billing Plans

Two plan types are supported:

* **FIXED** — price per unit is stored directly on the billing plan.
* **SLAB** — pricing ranges are stored in `billing_plan_slabs`.

Slabs follow:

```text
lower_bound <= consumption < upper_bound
```

A `NULL` upper bound represents an open-ended slab.

## Meter Billing-Plan History

A water meter can have billing-plan history using:

* `effective_from`
* `effective_to`
* `billing_plan_id`

Only one billing plan can be active/open for a meter at a time.

If a billing period crosses a billing-plan change, invoice generation can split the period into invoice-item segments and calculate each segment using the applicable historical plan.

## Meter Assignments

A meter can be assigned to a customer and later unassigned.

Assignment records preserve the historical relationship through:

* `assigned_at`
* `unassigned_at`
* `water_meter_id`
* `user_id`

The database enforces a single active assignment per meter.

## Reading Ingestion

Readings are stored in `water_meter_readings`.

Supported reading types:

* `TOTAL`
* `FLOW`

Reading ingestion is idempotent through:

```text
water_meter_id + ingestion_key
```

The database also prevents duplicate readings for the same meter, reading type, and timestamp.

## Invoice Generation

Monthly invoice generation uses a half-open billing period:

```text
[from, to)
```

Example:

```text
August 2026
from = 2026-08-01
to   = 2026-09-01
```

Only completed billing months are accepted.

Invoice generation is also restricted to the configured two-year historical window.

## API Endpoints

Authenticated endpoints require:

```http
Authorization: Bearer <session-token>
```

### Authentication

| Method | Endpoint           | Description           |
| ------ | ------------------ | --------------------- |
| POST   | `/api/auth/login`  | Login                 |
| POST   | `/api/auth/logout` | Logout/revoke session |

### Users

| Method | Endpoint          | Description      |
| ------ | ----------------- | ---------------- |
| POST   | `/api/users`      | Create user      |
| GET    | `/api/users/{id}` | Get user         |
| PUT    | `/api/users/{id}` | Update user      |
| DELETE | `/api/users/{id}` | Soft-delete user |

### Billing Plans

| Method | Endpoint                  | Description           |
| ------ | ------------------------- | --------------------- |
| POST   | `/api/billing-plans`      | Create billing plan   |
| GET    | `/api/billing-plans`      | Get all billing plans |
| GET    | `/api/billing-plans/{id}` | Get billing plan      |
| PUT    | `/api/billing-plans/{id}` | Update billing plan   |
| DELETE | `/api/billing-plans/{id}` | Delete billing plan   |

### Water Meters

| Method | Endpoint                 | Description                           |
| ------ | ------------------------ | ------------------------------------- |
| POST   | `/api/water-meters`      | Create meter                          |
| GET    | `/api/water-meters`      | Get meters                            |
| GET    | `/api/water-meters/my`   | Get meters assigned to logged-in user |
| GET    | `/api/water-meters/{id}` | Get meter                             |
| PUT    | `/api/water-meters/{id}` | Update meter                          |
| DELETE | `/api/water-meters/{id}` | Delete meter                          |

### Meter Assignments

| Method | Endpoint                       | Description    |
| ------ | ------------------------------ | -------------- |
| POST   | `/api/water-meter-assignments` | Assign meter   |
| DELETE | `/api/water-meter-assignments` | Unassign meter |

### Reading Ingestion

| Method | Endpoint                    | Description            |
| ------ | --------------------------- | ---------------------- |
| POST   | `/api/water-meter-readings` | Ingest a meter reading |

### Invoices

| Method | Endpoint                                      | Description                              |
| ------ | --------------------------------------------- | ---------------------------------------- |
| GET    | `/api/invoices`                               | Get invoices for the logged-in user      |
| POST   | `/api/invoices/generate`                      | Generate invoices for the logged-in user |
| POST   | `/api/admin/invoices/generate`                | Start bulk invoice generation            |
| GET    | `/api/admin/invoices/generation-jobs/{jobId}` | Check bulk generation job status         |

## Invoice History

`GET /api/invoices` returns invoice headers together with their associated invoice items.

An invoice contains:

* user
* water meter
* assignment
* billing period
* total consumption
* total amount
* generation timestamps

An invoice item contains:

* billing plan used for the segment
* segment start/end
* opening reading
* closing reading
* consumption
* calculated amount

Historical invoice retrieval is based on:

```text
invoices.user_id
        ↓
     invoices
        ↓
invoice_items.invoice_id
```

It does not depend on the meter's current assignment.

This allows historical invoices to remain available even if a meter is subsequently reassigned or unassigned.

## Bulk Billing Generation

Administrators can start monthly billing generation through:

```http
POST /api/admin/invoices/generate
```

The operation creates a billing-generation job and returns its job identifier.

Check the job with:

```http
GET /api/admin/invoices/generation-jobs/{jobId}
```

Jobs track:

* billing period
* status
* meters processed
* invoices generated
* meters skipped
* start time
* completion time

Skipped meters and their reasons are stored in:

```text
billing_generation_job_skips
```

## Authentication Flow

1. Login through `POST /api/auth/login`.
2. Receive a session token.
3. Send it on authenticated requests:

```http
Authorization: Bearer <token>
```

4. Logout through `POST /api/auth/logout` to revoke the session.

Passwords are stored as Argon2 hashes rather than plaintext.

## Data Integrity

The database contains constraints and indexes for important business rules, including:

* one active meter assignment
* one active billing plan per meter
* valid assignment date ranges
* valid billing-plan date ranges
* valid billing slabs
* non-negative readings
* non-negative consumption
* non-negative invoice amounts
* duplicate reading prevention
* duplicate ingestion prevention
* one invoice per assignment and billing period

## Running the Application

### Prerequisites

* Java 17+
* Maven 3.9+, or use the included Maven Wrapper

### Linux/macOS

```bash
./mvnw spring-boot:run
```

### Windows

```cmd
mvnw.cmd spring-boot:run
```

The application runs on:

```text
http://localhost:8080
```

### Build

```bash
./mvnw clean package
```

Run the JAR:

```bash
java -jar target/billing-software-0.0.1-SNAPSHOT.jar
```

## H2 Console

The H2 console is enabled at:

```text
http://localhost:8080/h2-console
```

Current datasource configuration:

```text
JDBC URL: jdbc:h2:file:./data/billingdb
Username: db-admin
Password: adminuser
```

For production use, database credentials should be externalized and the H2 console should normally be disabled or protected.



## Repository

[GitHub Repository](https://github.com/karmarankartik/billing-software)

## License

No separate open-source license is currently declared for this project.
