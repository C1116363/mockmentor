# MockMentor

A learning project: **students and working professionals request mock interviews, senior mentors pick them up and take the call.**

**Stack:** Java 21 · Spring Boot 3.5 · **Spring Security + JWT** · Spring Data JPA · MySQL · **springdoc-openapi (Swagger)** · React 19 (Vite)

The repo is **three folders**, one per piece. Each has its own README telling you where to change things:

| Folder | Port | What it is |
| --- | --- | --- |
| **`website/`** | `:3000` | The public marketing site. The button in the top-right corner opens the app. |
| **`frontend/`** | `:5173` | The React app — login, signup and the candidate dashboard. |
| **`backend/`** | `:8080` | The Spring Boot API. [Swagger UI](http://localhost:8080/swagger-ui.html) at `/swagger-ui.html`. |

Start at <http://localhost:3000> and click the corner button.

---

## Three roles, three screens

| Role | Sign up? | What they see |
| --- | --- | --- |
| **STUDENT** (candidate) | Yes | Book a 1-hour slot, track requests, read feedback |
| **MENTOR** | Yes → then **verified by an admin** | Profile form → "under verification" → the interview queue |
| **ADMIN** | No — seeded | Assign mentors to requests, the full mentor directory (verified and pending), users |

Admin accounts are **not** created through signup. The login page has an Admin
option, but it only ever shows a login form — a public "make me an admin"
endpoint would let anyone grant themselves full access. The first admin comes
from the seeder and would promote others.

## Payment (v1: manual UPI)

There is **no payment gateway**. A booking is confirmed by a human:

```
student picks a slot  →  AWAITING_PAYMENT   (slot is held, mentors can't see it)
        │
        ├─ pays your UPI ID from their own app
        ├─ uploads the UTR + a screenshot     →  SUBMITTED
        │
   admin checks it against the bank
        │
        ├─ verifies  →  request becomes PENDING and enters the mentor queue
        └─ rejects   →  reason shown; the student can send new proof
```

Set your UPI ID in `backend/.env`:

```
UPI_ID=yourname@okhdfcbank
UPI_PAYEE=Your Name
```

The fee is `app.payment.amount` in `application.properties` (₹499 by default).

**The amount is always read from server config, never from the request body.**
A client that could name its own price is the most obvious hole in any payment
flow, so the API simply doesn't accept one.

An unpaid booking still **holds its slot** — otherwise a student could pay and
find the time gone.

### Screenshot uploads

Uploads are the easiest place to open a hole, so `ScreenshotStorage` is strict:

- **We generate the filename.** A crafted name like `../../application.properties`
  would otherwise write outside the upload directory.
- **The type is detected from the file's own bytes**, not the `Content-Type`
  header, which the caller controls.
- **SVG is rejected** even though it is an image — SVG can contain script, so
  serving one back would be stored XSS.
- 5 MB cap, and files are served as `attachment` with `nosniff`.
- Only the student who uploaded it and admins can fetch it. A screenshot of
  somebody's banking app is private.

Files land in `backend/uploads/` which is gitignored.

### What v1 does not do

No refunds, no automatic reconciliation, and **no payouts to mentors** — you'd
pay them manually. Real payouts mean Razorpay Route or Stripe Connect plus
mentor KYC, which is a much bigger job.

## Interview feedback

Closing an interview means filling in a **scorecard**, not a paragraph:

| Field | Required |
| --- | --- |
| Overall rating (1–5) | yes |
| Verdict — Ready / Almost there / Needs work | yes |
| Summary | yes |
| Technical, Problem solving, Communication (1–5 each) | no |
| What went well / What to work on | no |

The optional ratings are `Integer`, not `int`, so **"not rated" stays distinct
from "scored zero"** — interviews completed before scoring existed have them as
null, and the candidate's scorecard just omits those rows.

The mentor gets clickable stars with hover preview; the candidate gets a
scorecard with the overall score, per-skill stars and a colour-coded verdict.

## Meeting links

**A meeting room is created automatically when a mentor is assigned.** Both the
student and the mentor then see the same link, and an **Upcoming interviews**
banner with a **Join** button appears on both dashboards. Clicking Join from
either side lands both people in the same call.

Rooms are **Jitsi Meet** by default (`app.meeting.provider=jitsi`). That is a
deliberate choice, not a shortcut:

> You cannot create a real `meet.google.com` link without going through the
> **Google Calendar API with OAuth credentials** — there is no public endpoint
> that just hands you one. Generating a Google-Meet-shaped URL ourselves would
> produce a string that *looks* right and fails the moment somebody clicks it.
> A Jitsi room exists the instant the URL is opened: no account, no API key,
> and both people genuinely land in the same call.

To use real Google Meet, set `app.meeting.provider=google` and implement
`GoogleMeetLinkGenerator` — the class comment spells out exactly what is needed
(Calendar API, OAuth or a service account, and a `conferenceData` insert). Until
then it throws loudly rather than handing anyone a dead link.

Room names are long and random (`mockmentor-h5rye2-rzcuqq-ummniz`) because a
Jitsi room is reachable by anyone who knows its name — a guessable one like
`interview-7` would let strangers walk into somebody's interview.

Whoever is scheduling can still paste their own link instead; leaving the field
blank is what triggers generation.

## Two ways a request gets scheduled

1. **A mentor claims it** — a verified mentor sees the open queue and accepts
   one themselves.
2. **An admin assigns it** — the admin sees every unassigned request and hands
   it to a specific mentor. Useful when a request needs someone particular, or
   has been sitting unclaimed.

Either way the request moves `PENDING -> SCHEDULED` and the student sees their
interviewer, the slot and the joining link. An admin cannot assign an
**unverified** mentor — that would sidestep verification entirely.

## Mentor verification

A mentor cannot just sign up and start interviewing. They go through this:

```
sign up  →  INCOMPLETE  →  fill in the profile  →  PENDING
                                                     │
                              admin rejects ─────────┤
                                    │                │
                                REJECTED ────────► APPROVED
                             (reason shown,          │
                              can resubmit)          ▼
                                              can take interviews
```

The profile asks for professional details (expertise, company, designation,
years), education (qualification, university, year), contact, **KYC** (Aadhaar,
PAN) and **bank details** for payouts.

**Only `APPROVED` mentors can do anything.** `MentorProfileService.assertApproved()`
runs before the queue, accept and complete. The waiting screen in the browser is
presentation — the server refuses regardless. Unverified mentors also stay off
the public website and don't count toward slot capacity.

### ⚠️ About the sensitive data

Aadhaar and bank account numbers are **masked to the last 4 digits** in every
response — the full values never leave the server, not even for an admin.

They are, however, stored **in plain text** in MySQL. That is fine for learning
and **not acceptable in production**, where you would need column-level
encryption, access logging, a retention policy, and to check what the law
requires before storing Aadhaar at all. Don't deploy this as-is with real data.

### Demo logins

These are **not** shown on the login page any more — it's a real login screen.
They're listed here for you, the developer. Every seeded account uses the
password `password123`:

| Role | Email | What you'll see |
| --- | --- | --- |
| Candidate | `rahul@example.com` | Booking + tracking |
| Mentor (verified) | `ananya@example.com` | The interview queue |
| Mentor (unverified) | `arjun@example.com` | The profile form — try onboarding |
| Admin | `admin@example.com` | The verification queue |

There is deliberately **no public "sign up as admin"** endpoint — that would let
anyone grant themselves full access. The first admin comes from the seeder; in a
real system an existing admin would promote others.

---

## Project layout

```
interview-mentor/
├── backend/                          [README](backend/README.md) — Spring Boot API on :8080
│   ├── run.sh                        loads .env, then `mvn spring-boot:run`
│   ├── .env                          DB_USER / DB_PASSWORD (gitignored)
│   └── src/main/java/com/learn/interviewmentor/
│       ├── model/                    User, MentorProfile, InterviewRequest, Role, RequestStatus
│       ├── repository/               Spring Data interfaces (no implementation needed)
│       ├── dto/                      request/response records — the API's public shape
│       │   └── auth/                 login, signup, token + user payloads
│       ├── security/                 ← the whole auth system, see below
│       ├── config/OpenApiConfig.java  Swagger metadata + the JWT "Authorize" button
│       ├── service/                  business rules live here
│       ├── controller/               thin REST layer
│       ├── exception/                custom exceptions + @RestControllerAdvice
│       └── config/                   demo data seeder
├── website/                          [README](website/README.md) — marketing site on :3000
│   ├── index.html                    one self-contained file — HTML + CSS + JS
│   └── serve.sh                      starts it on :3000
└── frontend/                         [README](frontend/README.md) — React app on :5173
    └── src/
        ├── api/client.js             every fetch call + the token header
        ├── auth/AuthContext.jsx      "who is logged in", shared via React Context
        ├── pages/
        │   ├── AuthPage.jsx          login + signup
        │   └── StudentDashboard.jsx  book + track interviews
        ├── components/               RequestCard, StatusBadge
        └── App.jsx                   loading / logged out / logged in
```

---

## Running it

You need MySQL running, plus Java 21+, Maven and Node.

### 1. Backend

```bash
cd backend
./run.sh
```

You do **not** need to create the database by hand. The JDBC URL uses
`createDatabaseIfNotExist=true` and `ddl-auto=update` makes Hibernate create the
`users`, `mentor_profiles` and `interview_requests` tables on first boot.
A seeder then inserts the demo accounts.

DB credentials come from `backend/.env`:

```
DB_USER=root
DB_PASSWORD=your-password
```

API is at <http://localhost:8080>.

**Interactive API docs: <http://localhost:8080/swagger-ui.html>**

All 19 endpoints are documented there, with request/response schemas, example
values and every status code they can return. To call a protected endpoint:

1. Run `POST /api/auth/login` with a demo account.
2. Copy the `token` out of the response.
3. Click the green **Authorize** button at the top right and paste it in
   (just the token — Swagger adds the word `Bearer` for you).
4. Every endpoint below now sends the `Authorization` header.

The raw OpenAPI 3 spec is at <http://localhost:8080/v3/api-docs> — you can import
that straight into Postman or Insomnia.

### 2. Frontend

```bash
cd frontend
npm install     # only the first time
npm run dev
```

Open <http://localhost:5173>.

### 3. Website

```bash
cd website
./serve.sh
```

Open <http://localhost:3000>. The **Launch App →** button in the top-right corner
takes you to the React app on `:5173`.

It is one static HTML file with no build step and no dependencies. It works with
the backend switched off — it just shows a "Backend offline" note instead of live
numbers.

> **Note:** if you change the entity classes, `ddl-auto=update` only *adds*
> columns — it never drops or relaxes old ones. If startup fails on a stale
> column, drop the schema and let it rebuild:
> `mysql -u root -p -e "DROP DATABASE interview_mentor;"`

---

## How the authentication works

```
1. POST /api/auth/login  { email, password }
        │
        ▼
   AuthenticationManager  ──► CustomUserDetailsService ──► users table
        │                            (loads the User)
        │                     BCryptPasswordEncoder.matches(raw, hash)
        ▼
   JwtService.generateToken()   signs header.payload.signature with a secret
        │
        ▼
   { token, expiresInMs, user }          ── frontend stores token in localStorage


2. Every later request:  Authorization: Bearer <token>
        │
        ▼
   JwtAuthenticationFilter        validates the signature, loads the user,
        │                         puts an Authentication in the SecurityContext
        ▼
   SecurityConfig rules           hasRole("MENTOR") etc.
        │
        ▼
   Controller                     @CurrentUser User gives you the real account
```

### The two layers of protection

They are **not** the same thing and you need both:

- **URL rules** in `SecurityConfig` answer *"is this person a MENTOR?"*
- **Ownership checks** in `InterviewRequestService` answer *"is this their own interview?"*

Role checks alone would let any mentor complete another mentor's interview, or
any student cancel another student's request. Try it:

```bash
# mentor B tries to complete mentor A's interview
# -> 403 "This interview was accepted by another mentor"
```

### 401 vs 403

- **401 Unauthorized** — "I don't know who you are." No token, expired token, bad signature.
- **403 Forbidden** — "I know exactly who you are, and you still can't."

`RestAuthenticationEntryPoint` and `RestAccessDeniedHandler` produce these.
Without them Spring returns 403 for *both*, and the frontend never learns to
throw away a stale token.

---

## API

### Auth — `/api/auth` (no token needed except `/me`)

| Method | Path | Body |
| --- | --- | --- |
| `POST` | `/api/auth/signup/student` | `{ fullName, email, password }` |
| `POST` | `/api/auth/signup/mentor` | `{ fullName, email, password, expertise, yearsOfExperience, currentCompany, bio }` |
| `POST` | `/api/auth/login` | `{ email, password }` |
| `GET` | `/api/auth/me` | — returns the logged-in user |

### Interview requests — `/api/requests`

| Method | Path | Who |
| --- | --- | --- |
| `GET` | `/api/slots?date=2026-09-20` | any logged-in user — the 1-hour slot grid for that day |
| `POST` | `/api/requests` | STUDENT — book a slot (starts AWAITING_PAYMENT) |
| `GET` | `/api/requests/mine` | STUDENT — your own requests |
| `GET` | `/api/requests/pending` | MENTOR — the open queue |
| `GET` | `/api/requests/assigned` | MENTOR — what you accepted |
| `PATCH` | `/api/requests/{id}/accept` | MENTOR — `{ scheduledAt, meetingLink }` |
| `PATCH` | `/api/requests/{id}/complete` | MENTOR (owner only) — the scorecard |
| `PATCH` | `/api/requests/{id}/cancel` | student, mentor, or admin |

### Public — `/api/public` (no token at all)

| Method | Path | What |
| --- | --- | --- |
| `GET` | `/api/public/stats` | counts only — mentors, students, interviews done, open requests |

The website has no login, so this is the only data it can read. It returns
**aggregate numbers only, never names or emails** — anything `permitAll()` is
readable by the whole internet.

### Payments — `/api/payments`

| Method | Path | Who |
| --- | --- | --- |
| `GET` | `/api/payments/instructions` | any logged-in user — UPI ID, amount, deep link |
| `GET` | `/api/payments/by-request/{id}` | the student who booked, or an admin |
| `POST` | `/api/payments/by-request/{id}/proof` | STUDENT — multipart: `upiReference` + `screenshot` |
| `GET` | `/api/payments/{id}/screenshot` | owner or ADMIN only |
| `GET` | `/api/admin/payments/pending` | ADMIN — the verification queue |
| `PATCH` | `/api/admin/payments/{id}/verify` | ADMIN — releases the booking |
| `PATCH` | `/api/admin/payments/{id}/reject` | ADMIN — `{ reason }` |

### Mentor onboarding — `/api/mentor/profile`

| Method | Path | Who |
| --- | --- | --- |
| `GET` | `/api/mentor/profile` | MENTOR — my profile + verification status |
| `PUT` | `/api/mentor/profile` | MENTOR — submit or resubmit for review |

### Mentors & admin

| Method | Path | Who |
| --- | --- | --- |
| `GET` | `/api/mentors` | any logged-in user (APPROVED mentors only) |
| `GET` | `/api/admin/mentor-profiles/pending` | ADMIN — the review queue |
| `GET` | `/api/admin/mentor-profiles` | ADMIN — every profile |
| `PATCH` | `/api/admin/mentor-profiles/{id}/approve` | ADMIN |
| `PATCH` | `/api/admin/mentor-profiles/{id}/reject` | ADMIN — `{ reason }` |
| `GET` | `/api/admin/requests/pending` | ADMIN — requests with no mentor yet |
| `PATCH` | `/api/admin/requests/{id}/assign` | ADMIN — `{ mentorId, scheduledAt?, meetingLink }` |
| `GET` | `/api/admin/stats` | ADMIN |
| `GET` | `/api/admin/users` | ADMIN |
| `GET` | `/api/admin/requests` | ADMIN |
| `PATCH` | `/api/admin/users/{id}/deactivate` | ADMIN |
| `PATCH` | `/api/admin/users/{id}/activate` | ADMIN |

### Try it from the terminal

```bash
API=http://localhost:8080/api

# log in and keep the token
TOKEN=$(curl -s -X POST $API/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"rahul@example.com","password":"password123"}' \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# raise a request (note: no name/email — the token says who you are)
curl -X POST $API/requests \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"topic":"React interview","experienceLevel":"1-3 years",
       "preferredDate":"2026-09-01","notes":"Nervous about hooks."}'

# without a token -> 401
curl -i $API/requests/mine

# as a student, hitting a mentor-only route -> 403
curl -i $API/requests/pending -H "Authorization: Bearer $TOKEN"
```

---

## Booking slots

Interviews run in **one-hour slots between 9:00 AM and 9:00 PM**, up to 30 days
ahead. `GET /api/slots?date=...` returns the grid for a day and marks each slot
bookable or not:

- **Already passed** — the slot's start time is in the past
- **Fully booked** — as many live bookings as there are mentors, since that is
  how many interviews can run at once. Cancelling a request frees its slot again.

The rules live in `SlotService`. The grid is only a convenience — `createRequest`
calls `assertBookable()` and re-checks everything server-side, so a stale page or
a hand-crafted request still cannot book 3 AM, a half-hour slot, or a full one.

## How a request flows

```
student submits
      │
      ▼
   PENDING ──── mentor accepts (slot + link) ───► SCHEDULED
      │                                              │
      │                                              ├── mentor adds feedback ──► COMPLETED
      └──────────────── cancel ────────────────►  CANCELLED
```

Enforced in `InterviewRequestService`, not the controller: a request can only be
accepted while `PENDING`, only a `SCHEDULED` one can be completed, and a
`COMPLETED` one can't be cancelled.

---

## Things worth reading in the code

**Security**
- **`SecurityConfig`** — the `SecurityFilterChain` bean. This is *the* modern way;
  any tutorial extending `WebSecurityConfigurerAdapter` is years out of date.
  Rule order matters: the first match wins, so `.anyRequest()` stays last.
- **`AppUserDetails`** — the bridge between the JPA `User` entity and Spring
  Security's `UserDetails`. Note `"ROLE_" + role` — `hasRole("ADMIN")` actually
  looks for the authority `ROLE_ADMIN`, and forgetting that prefix is the single
  most common reason `@PreAuthorize` "mysteriously" fails.
- **`JwtAuthenticationFilter`** — extends `OncePerRequestFilter`. It never rejects
  anyone; it only populates the `SecurityContext`. Rejecting is the rules' job.
- **`JwtService`** — a JWT payload is base64, **not encrypted**. Paste one into
  jwt.io and you can read it. The signature only proves nobody changed it, so
  never put anything secret in the claims.
- **`@CurrentUser`** — a custom annotation wrapping
  `@AuthenticationPrincipal(expression = "user")`. This is how the server knows
  who you are without ever trusting a `userId` sent by the browser.
- **`AuthService.signupMentor`** — one `@Transactional` method creating both the
  `User` and the `MentorProfile`, so you can never end up with half of a mentor.

**Swagger / OpenAPI**
- **`OpenApiConfig`** — `addSecurityItem()` applies the bearer scheme to *every*
  endpoint by default; the four genuinely public ones opt out with
  `@SecurityRequirements` (note the plural — the singular one does the opposite).
- **`ApiErrorSchema`** — a documentation-only record. `GlobalExceptionHandler`
  returns a plain `Map`, which Swagger cannot introspect, so without this the
  error responses would render as an empty `{}`. Nothing constructs it at runtime.
- **`@Schema` on the DTO records** — that's where the example values in the
  "Try it out" boxes come from, so you can hit Execute without typing anything.

**JPA**
- **`InterviewRequest.assignTo(...)`** — the entity mutates itself, the service
  never calls `save()`. That's *dirty checking*.
- **`MentorProfile`** — a `@OneToOne` where the FK lives on this table, making it
  the *owning side*.
- **`@EntityGraph`** in the repositories — pulls `student` and `mentor` in the same
  query. Without it, a list of 20 requests fires 40 extra SELECTs (the N+1 problem).

**React**
- **`AuthContext`** — the `loading` flag matters. On refresh you have a token but
  don't yet know if it's valid; without that flag the app flashes the login
  screen every time.
- **`api/client.js`** — one `request()` wrapper attaches the `Bearer` header and
  clears the token on 401.
- **`App.jsx`** — role-based routing with a plain object lookup. Swap for
  react-router once you add more pages.

---

## Known rough edges (deliberate — good things to fix next)

1. **Token in `localStorage`** is readable by any JS on the page, so it's exposed to
   XSS. Production apps often use an httpOnly cookie. Fine for learning.
2. **No refresh token.** After 24h you just log in again.
3. **No logout on the server.** With JWTs the server keeps no session, so "logout"
   only deletes the client's copy. A stolen token stays valid until it expires —
   real systems keep a short expiry plus a denylist.
4. **`ddl-auto=update`** instead of Flyway migrations.
5. A harmless startup **warning** about `AuthenticationManager` / `UserDetailsService`
   — it's Spring telling you the explicit `DaoAuthenticationProvider` bean takes
   over from auto-configuration, which is exactly what we want.

---

## Ideas to extend it

1. **Refresh tokens** — short-lived access token + long-lived refresh token.
2. **Stop double-booking** — reject a slot a mentor already has an interview in.
3. **Ratings** — the student rates the mentor after `COMPLETED` (a `@OneToOne` to practise).
4. **Email notifications** — `spring-boot-starter-mail` when a request is accepted.
5. **Flyway migrations** — versioned SQL instead of `ddl-auto`.
6. **Tests** — `@WebMvcTest` with `spring-security-test`'s `@WithMockUser`,
   `@DataJpaTest` for repositories. Both dependencies are already in the pom.
7. **Password reset** — token by email, expiry, single use.
