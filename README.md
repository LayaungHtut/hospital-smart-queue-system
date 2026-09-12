# Hospital Smart Queue System

A combined Spring Boot application that merges the **Patient Module**, the **Smart Queue Engine**, and the **Doctor / Staff / Admin Module** into one project.

- **Backend:** Spring Boot 3.3 (MVC + Thymeleaf + JdbcTemplate)
- **Cloud database:** Neon (PostgreSQL) — schema created automatically by **Flyway** migrations
- **AI:** OpenRouter free LLM for symptom → department / doctor recommendation
- **Auth:** Session-based with BCrypt password hashing

## Tech stack

| Area | Technology |
|------|-----------|
| Framework | Spring Boot 3.3, Spring MVC, Thymeleaf |
| Data access | Spring `JdbcTemplate` |
| Database | Neon (PostgreSQL 15+) |
| Migrations | Flyway (`src/main/resources/db/migration`) |
| AI | OpenRouter chat completions (free models) |
| Build | Maven, Java 17 |
| IDE | IntelliJ IDEA |

## Project layout

```
src/main/java/com/hospitalqueue
├── ai/           OpenRouter client + AI recommendation service
├── config/       DataSource (Neon), .env loader, auth interceptor, beans
├── controller/   Patient, Queue, Doctor, Staff, Admin controllers
├── model/        Patient, Doctor, Department, Queue, Appointment, ...
├── repository/   JdbcTemplate data access for every entity
├── rule/         Smart Queue Engine business rules
├── service/      QueueService, RecommendationService, AuthService, ...
└── util/         ID/queue-number generators, session & validation helpers

src/main/resources
├── db/migration/ Flyway migrations (V1 schema, V2 seed, V3 functions)
├── static/       CSS + JS
└── templates/    Thymeleaf views (patient / doctor / staff / admin)
```

## 1. Configure the environment (`.env`)

Copy the example and fill in your real credentials:

```bash
cp .env.example .env
```

Fill in:
- **Neon** — create a free database at https://console.neon.tech → copy the connection string and your user/password into `NEON_DATABASE_URL`, `NEON_USER`, `NEON_PASSWORD`.
- **OpenRouter** — get a free API key at https://openrouter.ai/keys → set `OPENROUTER_API_KEY` and choose a free model for `OPENROUTER_MODEL`.

`.env` is git-ignored (secrets). `.env.example` is committed as a template.

## 2. Run in IntelliJ IDEA

1. **Open** the project folder (`hospital-smart-queue-system`) in IntelliJ.
2. Let IntelliJ import it as a **Maven** project (wait for the dependency download).
3. Make sure the project SDK is **Java 17+** (File → Project Structure → Project → SDK).
4. Make sure `.env` is filled in correctly (working directory must be the project root — the default IntelliJ run configuration already uses it).
5. Run the main class:
   ```
   com.hospitalqueue.HospitalSmartQueueSystemApplication
   ```
6. Open the browser at: **http://localhost:8080**

On first startup, Flyway automatically runs the migrations against your Neon database (creating all tables, seed data, and functions). You don't need to run any SQL manually.

## 3. Demo logins (password `123456` for all)

| Role | URL | Login |
|------|-----|-------|
| Patient | `/patient/login` | phone `09123456789` |
| Doctor | `/doctor/login` | doctor ID `D001` |
| Staff | `/staff/login` | phone `0922222222` |
| Admin | `/admin/login` | username `admin` |

## 4. Features by module

**Patient module** (`/patient/**`)
- Register, login, online self-registration, new/old patient detection
- Profile management
- Symptom entry → AI department recommendation
- Department validation, doctor selection (sorted by shortest wait)
- Appointment check-in, queue number generation, queue tracking, cancellation
- Notifications

**Smart queue engine** (`rule/` + `service/QueueService.java`)
- Priority queue (Emergency → Appointment → Normal, then FIFO)
- Queue number generation (e.g. `QCAR1202608160001`)
- Dynamic waiting time, queue capacity control
- Emergency screening + staff emergency confirmation
- Queue reordering, one-active-queue rule, missed-queue expiry
- Doctor availability detection (working hours + capacity + availability flag)
- Automated missed-queue expiry: `QueueExpiryScheduler` expires abandoned `WAITING` and no-show `CALLED` queues every minute

**Doctor / staff / admin** (`/doctor/**`, `/staff/**`, `/admin/**`)
- Doctor: login, view queue, call next (CALLED) → start / pause / resume consultation, complete
- Staff: confirm emergency, reassign doctor, live queue monitor + doctor availability
- Admin: manage doctors (incl. availability toggle), departments, patients, settings; reports

## 5. Database migrations

Migrations live in `src/main/resources/db/migration/` and run automatically:
- `V1__create_schema.sql` — all tables + indexes
- `V2__seed_data.sql` — departments, priorities, doctors, demo patient/staff/admin
- `V3__functions.sql` — PostgreSQL functions for queue numbering / next-patient call

To add a future schema change, add a new `V4__....sql` file and restart — Flyway applies it.
