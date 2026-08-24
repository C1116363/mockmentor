# 2. backend — the Spring Boot API

> Setting up from scratch? → [SETUP.md](../SETUP.md). To start just this part: `./start.sh backend` from the repo root.

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
| Change the video provider | `application.properties` → `app.meeting.provider`, and `meeting/` |
| Change your UPI ID | `.env` → `UPI_ID` / `UPI_PAYEE` |
| Change the interview fee | `application.properties` → `app.payment.amount` |
| Change upload rules | `storage/ScreenshotStorage.java` |
| Change the DB connection | `.env` |
| Add a field to a request | `model/InterviewRequest.java` **and** the matching DTO |
| Change a validation message | the DTO, e.g. `dto/CreateRequestDto.java` |

> **Note on the database:** `ddl-auto=update` only *adds* columns. If you remove
> or rename a field and startup fails, drop the schema and let it rebuild:
> `mysql -u root -p -e "DROP DATABASE interview_mentor;"`

## Structure

```
controller/   HTTP only. Read the request, call the facade, return what it gives back.
  facade/     One method per use case. Composes services, decides what the caller
  facade/impl is told. This is where a response envelope is built.
    service/      Interface: the contract other layers compile against.
    service/impl  Implementation: business rules for one thing.
      repository/ Spring Data. Queries, and the access rules that belong in SQL.
        model/    JPA entities. The database.

dto/          Requests coming IN.  Every class ends in Dto.
vo/           Responses going OUT. Every class ends in Vo.
common/       ApiResult, the one envelope every endpoint returns.
exception/    Custom exceptions + the @RestControllerAdvice that maps them.
security/     JWT filter, role rules, the 401/403 writers.
storage/      File uploads - screenshots and study material.
```

### Naming

| Kind | Suffix | Package | Example |
| --- | --- | --- | --- |
| Request body | `Dto` | `dto/` | `PlanRequestDto`, `LoginRequestDto` |
| Response body | `Vo` | `vo/` | `PlanVo`, `InterviewRequestVo` |
| Contract | `Service` / `Facade` | `service/`, `facade/` | `PlanService` |
| Implementation | `ServiceImpl` / `FacadeImpl` | `*/impl/` | `PlanServiceImpl` |
| Entity | none | `model/` | `Plan`, `PlanEnrollment` |
| Repository | `Repository` | `repository/` | `PlanRepository` |

Sub-packages mirror across `dto/` and `vo/` (`dto/plan` ↔ `vo/plan`), so the request
and response for a feature sit at the same path in both trees.

### What each layer may talk to

```
controller  ->  facade                    (never a service, never a repository)
facade      ->  service, other facades    (never a repository)
service     ->  repository, other services
repository  ->  model
```

A controller reaching past its facade is the thing this structure exists to stop.
`grep -rn "Service " controller/` should return nothing.

### The one envelope

Every endpoint returns `ApiResult<T>`:

```json
{ "success": true,  "status": 200, "data": { ... } }
{ "success": false, "status": 404, "message": "No plan with id 9", "path": "/api/plans/9" }
```

Null fields are dropped, so a success carries no empty `message`/`fieldErrors` keys.
A client checks one boolean, in one place, for every call it ever makes.

Two details worth knowing:

- **`ApiResultStatusAdvice`** copies `ApiResult.status()` onto the HTTP status line.
  Without it a facade returning `ApiResult.created(...)` would put 201 in the body
  while the wire said 200 — a field contradicting the status line is worse than no
  field. The alternative was every controller restating the status by hand in a
  `ResponseEntity`, which is the same number in two places and exactly how the two
  drift apart.
- **File downloads are not enveloped.** The body has to be the bytes; wrapping them
  would mean base64 inside JSON. Those handlers return `ResponseEntity<Resource>`,
  and their facade methods return a bare `Path`.

Errors use the same envelope, built by `GlobalExceptionHandler` — and by
`exception/ApiErrors` for the 401/403 the security filter chain produces before
Spring MVC ever runs, so a client cannot tell the two apart.

### Why it is not called ApiResponse

`io.swagger.v3.oas.annotations.responses.ApiResponse` already is, and every
controller imports it to document status codes. Java has no import aliases, so
sharing the name would force one of them to be fully-qualified in every signature.

### On the thin facades

Some facades only delegate — `SlotFacade` has one method that calls one service.
That is deliberate. Every controller going through a facade *without exception* is
what makes the layering something you can trust by looking at the package tree
instead of reading each class. The day slots need a second source, or a price
change needs an audit write alongside it, there is already a place for it and
nothing above has to change.

Where the layering already earns its keep: `PlanFacadeImpl.enroll` spans the price
list and the enrollment ledger, and `AdminFacadeImpl` pulls four services together
for one dashboard.

## Errors

Every failure leaves through `exception/GlobalExceptionHandler` and comes back as
the same JSON shape, so the frontend has exactly one thing to parse:

```json
{
  "timestamp": "2026-08-24T16:42:39.572",
  "status": 409,
  "error": "Conflict",
  "message": "An account with rahul@example.com already exists",
  "path": "/api/auth/signup/student",
  "fieldErrors": { "password": "Password must be at least 8 characters" }
}
```

`fieldErrors` appears only on validation failures. `path` is there so a network
tab and a log line can be matched up.

### Which status you get

| Status | When | Thrown by |
| --- | --- | --- |
| 400 | Validation failed, malformed JSON, missing parameter, bad path variable | `BadRequestException`, Spring's binding exceptions |
| 401 | No token, expired token, wrong password | `RestAuthenticationEntryPoint`, `AuthenticationException` |
| 403 | Logged in, but not yours / not your role | `ForbiddenException`, `RestAccessDeniedHandler`, `@PreAuthorize` |
| 404 | Unknown id, unknown URL | `NotFoundException`, `NoResourceFoundException` |
| 405 | Wrong HTTP method (response carries `Allow`) | `HttpRequestMethodNotSupportedException` |
| 409 | Slot filled up, email taken, already paid | `ConflictException`, `DataIntegrityViolationException` |
| 413 | Upload over `spring.servlet.multipart.max-file-size` | `MaxUploadSizeExceededException` |
| 415 | Wrong `Content-Type` — usually JSON sent to the upload endpoint | `HttpMediaTypeNotSupportedException` |
| 500 | Our bug, or the disk failed | `StorageException`, the `Exception` catch-all |
| 503 | MySQL unreachable, pool exhausted, query timed out | `DataAccessException` |

### Two rules the handler follows

1. **4xx messages are written for the user.** They say what to do next and are
   safe to render on screen.
2. **5xx messages tell the user nothing.** Constraint names, SQL and class names
   are reconnaissance for an attacker. The caller gets a short reference code —
   `(reference 3f9a1c02)` — and the stack trace goes to the log under that same
   code. Grep the log for the code a user quotes and their exact request is
   right there.

The last handler catches `Exception` itself. That is deliberate: an uncaught
exception returns Boot's default error body, which omits `message` entirely, and
the frontend — finding nothing to show — tells the user the backend is down. A
500 that lies about the cause is worse than a 500.

### The bit that catches people out

`@RestControllerAdvice` only sees exceptions thrown **inside Spring MVC**.
Requests rejected by the security filter chain never get that far, which is why
401s and filter-level 403s are written by hand in `security/`. All three paths
build their body through `exception/ApiErrors` so they cannot drift apart — if
you add a field, add it there.

The same trap applies to anything thrown **from a filter**: the container turns
it straight into a bare 500 that no handler sees. `JwtAuthenticationFilter`
therefore catches its own failures and simply leaves the request anonymous, so
the entry point answers a clean 401.

And once the first byte of a response is on the wire the status can no longer be
changed — which is why `PaymentService.screenshotPath` checks the file is
readable *before* the controller starts streaming it.

### Checking it by hand

```bash
curl -i localhost:8080/api/public/nope                    # 404
curl -i -X DELETE localhost:8080/api/public/mentors       # 405 + Allow header
curl -i -X POST localhost:8080/api/auth/login \
     -H 'Content-Type: application/json' -d '{bad json'   # 400
```

## Plans and study material

Two features that work together: a price list a student can buy from, and
material an admin sends out — some of it unlocked by having bought a plan.

### Plans

Prices live in the **database**, not in `application.properties`. That is the
whole point: an admin changes a price in the admin panel and students see the new
number on their next page load, with no redeploy and no restart. Contrast the
interview fee (`app.payment.amount`), which is config and needs a deploy —
correct for one fixed fee, wrong for a list somebody wants to tune.

The rule that makes that safe: **a price change only affects what happens next.**
`PlanEnrollment` copies the price when the student starts buying, so nobody's
completed purchase is ever rewritten underneath them.

| Step | Status | Who |
| --- | --- | --- |
| Student picks a plan | `AWAITING_PAYMENT` | student |
| Pays our UPI ID, uploads UTR + screenshot | `SUBMITTED` | student |
| Admin checks the UTR against the bank | `ACTIVE` | admin |
| Couldn't match it | `REJECTED` → student can resend | admin |

Buying is idempotent. A double-tap on "Get this plan" returns the purchase
already in progress rather than creating a second one.

Plans are never deleted, only retired (`active = false`). Enrollments point at
the row, so deleting one would leave a student holding a plan nothing can name.

### Study material

An admin uploads a file or shares a link, and picks one of three audiences:

| Audience | Who sees it |
| --- | --- |
| `ALL_STUDENTS` | every student |
| `SPECIFIC_STUDENT` | one named student, nobody else |
| `PLAN_MEMBERS` | students whose purchase of that plan is currently active |

**All three are enforced in the SQL**, in `StudyMaterialRepository.findVisibleTo`.
Filtering on the client would leave the rows sitting in a JSON response any
student could read out of their own network tab — and for `SPECIFIC_STUDENT` that
is somebody else's private material, which is a real leak whether or not a screen
ever renders it. The download endpoint checks *again*, because the list decides
what a student is shown and cannot decide what they ask for. Ids are sequential
integers.

Files go through `MaterialStorage`, which follows the same rules as
`ScreenshotStorage` — we generate the filename, we sniff the real bytes, the file
lands inside one directory — with a wider allow-list. **HTML and SVG are refused**
even though an admin is uploading: both can carry script, and one served back to
a logged-in student from our own origin would be stored XSS sitting next to their
token in `localStorage`. An admin account can be phished; a trusted role is not a
reason to skip validation.

### Where to change things

| I want to change... | Where |
| --- | --- |
| A plan's price | Admin panel → Plans & prices → Change price |
| The starting plans on a fresh database | `config/DataSeeder.java` → `seedPlans()` |
| Default access length | `model/Plan.java` → `durationDays` |
| Which file types are allowed | `storage/MaterialStorage.java` → `BY_SIGNATURE` / `BY_EXTENSION` |
| Study material size cap | `storage/MaterialStorage.java` → `MAX_BYTES`, and `spring.servlet.multipart.max-file-size` if you raise it above 25 MB |
| Where material files are stored | `.env` / `app.materials.dir` |

> **On upload limits:** `spring.servlet.multipart.max-file-size` is the hard cap
> Tomcat enforces *before* a controller runs, so it has to be the largest any
> feature needs (25 MB, for study material). Each feature then applies its own
> tighter limit in code — screenshots stay at 5 MB in `ScreenshotStorage`. Over
> the Tomcat cap gives 413; over a feature's own limit gives 400.

## Mock interview vs mentoring session

`SessionType` on `InterviewRequest` decides which kind of hour was booked:

| | `MOCK_INTERVIEW` | `MENTORING` |
| --- | --- | --- |
| Purpose | Interviewed under real pressure | A discussion — advice, a code review, a design |
| Completion | Scorecard: ratings + verdict | Written notes only |
| `scored` | `true` | `false` |

A **field, not a second entity.** Slot booking, payment, mentor assignment and
completion are identical; only the write-up differs. A parallel table would be
two repositories, two services and two controllers to change one screen.

### The rule bean validation can't express

`overallRating` and `recommendation` are required for an interview and meaningless
for a discussion. `@NotNull` can't say that — it never sees the booking. So:

1. `SessionType.isScored()` carries the answer, as data on the type rather than
   an `if` in a service somebody has to go find.
2. `InterviewRequestService.complete` enforces it. **This check is now the only
   thing keeping interview scorecards mandatory** — the `@NotNull`s were removed
   from the DTO to let mentoring through. Delete it and interviews silently start
   completing with no score.
3. `InterviewRequest.complete` nulls the scored fields for an unscored session, so
   a client that sends them anyway can't staple a fake scorecard onto a
   discussion.

### The migration this needed

The column was added to a table that already had rows, and with `ddl-auto=update`
there is no backfill step. MySQL fills a `NOT NULL` column with an implicit
default — for an enum stored as a string that is `''`, which then maps to no
constant, so **every pre-existing booking would throw on read.**

So `session_type` is **nullable** in the database even though the code always sets
it, and `getSessionType()` coalesces null to `MOCK_INTERVIEW` — which is what those
rows were. Every read goes through the getter so no caller has to remember.

> If you add another enum column to a table with rows in it, do the same thing.
> This is the failure mode `ddl-auto=update` hands you for free.

### Price

**Both types cost the same** (`app.payment.amount`). If a discussion should be
cheaper, it is not a one-line change: `PaymentService.instructions()` currently
returns one amount from config with no booking context, and `PayModal` calls it
before knowing which booking it is for. Making the fee vary by type means passing
the request id into that endpoint and having the modal fetch per-booking — the
same shape `PlanEnrollmentService.instructionsFor(id, caller)` already uses, so
copy that.
