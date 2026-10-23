# Smart Appointment Management System

A Spring Boot web application for registering pets and managing their appointments, built for the YUVA Intern Java Developer Internship.

## Project overview and objectives

A small clinic (or grooming centre, shelter, etc.) needs to know which pets are registered and when they are due to visit. This project provides:

- a **REST API** (Spring Boot + Spring Data JPA + MySQL) and
- a **browser interface** (HTML, CSS, JavaScript) that talks to the API.

Learning objectives: layered architecture (controller, service, repository, entity), REST design with proper HTTP status codes, JPA entity relationships, validation and centralised exception handling, unit testing with Mockito, and keeping secrets out of source code.

## Features

**Pet registration**
- Register a pet: ID (auto-generated), name, type, breed, age, owner name, phone and email
- View all pets, search by ID or name, update, delete

**Appointments**
- Schedule an appointment for a registered pet (date, time, reason, status)
- View all, search by appointment ID or date, update, cancel, mark completed, delete
- Validation: required fields, no dates in the past, no time that already passed today, no booking more than 365 days ahead
- The same pet cannot have two active appointments at the same date and time (a cancelled slot can be re-booked)

**Dashboard**
- Total pets, total appointments, upcoming, completed and cancelled counts, plus a list of the next appointments

**Other**
- Success and error messages in the UI; centralised JSON error responses
- Database password is never stored in the code

## Technologies used

Java 21, Spring Boot 3.3.5 (Web, Data JPA, Validation), MySQL 8, Maven, JUnit 5, Mockito, AssertJ, HTML/CSS/JavaScript.

## Prerequisites

- JDK 21 (JDK 17+ also works if you change `java.version` in `pom.xml`)
- MySQL Server 8.x running locally
- IntelliJ IDEA (Community or Ultimate); it bundles Maven, so no separate install is needed
- Internet access the first time, so Maven can download dependencies

## Database setup

1. Open `database/schema.sql`, change `CHANGE_ME_PASSWORD` to a password of your choice.
2. Run it as a MySQL administrator:
   ```bash
   mysql -u root -p < database/schema.sql
   ```
   This creates the database `smart_appointment_db`, a user `sams_user` and the `pets` and `appointments` tables.
   (The application can also create the tables itself because `spring.jpa.hibernate.ddl-auto=update`, but the database and user must exist.)
3. Copy the sample configuration and fill in your values:
   ```bash
   cp config/database.properties.example config/database.properties
   ```
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/smart_appointment_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   spring.datasource.username=sams_user
   spring.datasource.password=the_password_you_chose
   ```
   `config/database.properties` is in `.gitignore`, so it is never committed.
   Alternative: set the environment variables `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` instead.

### Entity relationship

```
pets (1) ---------< (many) appointments
 id PK                      id PK
 name, type, breed, age     pet_id FK -> pets.id (ON DELETE CASCADE)
 owner_name, owner_phone    appointment_date, appointment_time
 owner_email                reason, status (SCHEDULED | COMPLETED | CANCELLED)
```

## Running the application

### In IntelliJ IDEA
1. **File > Open**, select the project folder (the one containing `pom.xml`). Choose **Trust Project** and wait for Maven to import.
2. **File > Project Structure > Project**: set the SDK to JDK 21.
3. Make sure `config/database.properties` exists (see above).
4. Open `SmartAppointmentApplication.java` and click the green run arrow next to `main`.
   The working directory must be the project root (the default), so the `config/` file is found.
5. Open <http://localhost:8080>.

### From a terminal
```bash
mvn spring-boot:run
```

## API endpoints

All bodies are JSON. Dates are `yyyy-MM-dd`, times are `HH:mm`.

| Method | URL | Purpose | Success |
|---|---|---|---|
| POST | `/api/pets` | Register a pet | 201 |
| GET | `/api/pets` | List pets | 200 |
| GET | `/api/pets?search=bruno` or `?search=3` | Search by name or ID | 200 |
| GET | `/api/pets/{id}` | Get one pet | 200 |
| PUT | `/api/pets/{id}` | Update a pet | 200 |
| DELETE | `/api/pets/{id}` | Delete a pet and its appointments | 204 |
| POST | `/api/appointments` | Schedule an appointment | 201 |
| GET | `/api/appointments` | List all appointments | 200 |
| GET | `/api/appointments?id=5` / `?date=2026-11-02` | Search by ID and/or date | 200 |
| GET | `/api/appointments/{id}` | Get one appointment | 200 |
| PUT | `/api/appointments/{id}` | Update a scheduled appointment | 200 |
| PATCH | `/api/appointments/{id}/cancel` | Cancel | 200 |
| PATCH | `/api/appointments/{id}/complete` | Mark completed | 200 |
| DELETE | `/api/appointments/{id}` | Delete | 204 |
| GET | `/api/dashboard` | Dashboard summary | 200 |

Error statuses: `400` validation / invalid date or body, `404` record not found, `409` duplicate appointment or data conflict, `500` unexpected error. Every error returns:
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request",
  "message": "Validation failed. Please correct the highlighted fields.",
  "fieldErrors": { "ownerPhone": "Owner phone is required" } }
```

Example:
```bash
curl -X POST http://localhost:8080/api/pets -H "Content-Type: application/json" \
  -d '{"name":"Bruno","type":"Dog","breed":"Labrador","age":3,"ownerName":"Asha Verma","ownerPhone":"9876543210","ownerEmail":"asha@example.com"}'

curl -X POST http://localhost:8080/api/appointments -H "Content-Type: application/json" \
  -d '{"petId":1,"appointmentDate":"2026-11-02","appointmentTime":"10:30","reason":"Vaccination"}'
```

## Testing

The unit tests use Mockito, so they need **no database**. Time is frozen with a fixed `Clock`.

```bash
mvn test
```
In IntelliJ: right-click `src/test/java` > **Run 'All Tests'**.

Covered: pet registration, search, update and delete; appointment scheduling, past dates, past time today, far-future dates, duplicates, updating, cancelling, completing, unknown IDs, search; dashboard counts.

## Screenshots

Run the app, take screenshots, save them in `docs/screenshots/` and link them here:

```
![Dashboard](docs/screenshots/dashboard.png)
![Pet registration](docs/screenshots/pets.png)
![Appointments](docs/screenshots/appointments.png)
```

## Project structure

```
src/main/java/com/yuvaintern/sams
  entity/      Pet, Appointment, AppointmentStatus     (database tables)
  repository/  PetRepository, AppointmentRepository    (database access)
  service/     PetService, AppointmentService, DashboardService   (business rules)
  controller/  PetController, AppointmentController, DashboardController   (REST API)
  dto/         AppointmentRequest, AppointmentResponse, DashboardResponse
  exception/   custom exceptions + GlobalExceptionHandler
  config/      AppConfig (Clock bean)
src/main/resources/static   index.html, css/style.css, js/app.js
src/test/java               unit tests
database/schema.sql         MySQL setup script
config/database.properties.example   credential placeholders
```
