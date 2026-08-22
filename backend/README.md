# 2. backend — the Spring Boot API

Java 21 · Spring Boot 3.5 · Spring Security + JWT · Spring Data JPA · MySQL

## Run it

```bash
./run.sh
```

API on <http://localhost:8080>. Interactive docs on
<http://localhost:8080/swagger-ui.html>.

Your database credentials go in `.env` (copy `.env.example`). That file is
gitignored, so your password never reaches GitHub.

## Where to change things

Everything is under `src/main/java/com/learn/interviewmentor/`:

| Folder | What's in it | Change it when you want to... |
| --- | --- | --- |
| `model/` | JPA entities — the database tables | add a column or a new table |
| `repository/` | database queries | add a new lookup, e.g. `findByTopic(...)` |
| `dto/` | the shape of JSON going in and out | change what the API accepts or returns |
| `service/` | the business rules | change what is *allowed* to happen |
| `controller/` | the URLs | add or rename an endpoint |
| `security/` | login, JWT, role rules | change who can call what |
| `exception/` | error handling | change an error message or status code |
| `config/` | Swagger info + demo data | change the seeded accounts |

### Common edits

| I want to... | File |
| --- | --- |
| Change who can call an endpoint | `security/SecurityConfig.java` |
| Change the demo accounts or password | `config/DataSeeder.java` |
| Change how long a login lasts | `application.properties` → `app.jwt.expiration-ms` |
| Change bookable hours (9 AM–9 PM) | `service/SlotService.java` → `DAY_START` / `DAY_END` |
| Change slot length (1 hour) | `model/InterviewRequest.java` → `SLOT_MINUTES` |
| Change how far ahead people can book | `service/SlotService.java` → `MAX_DAYS_AHEAD` |
| Change the DB connection | `.env` |
| Add a field to a request | `model/InterviewRequest.java` **and** the matching DTO |
| Change a validation message | the DTO, e.g. `dto/CreateRequestDto.java` |

> **Note on the database:** `ddl-auto=update` only *adds* columns. If you remove
> or rename a field and startup fails, drop the schema and let it rebuild:
> `mysql -u root -p -e "DROP DATABASE interview_mentor;"`
