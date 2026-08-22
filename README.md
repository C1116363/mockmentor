# MockMentor

A learning project: **students and working professionals request mock interviews, senior mentors pick them up and take the call.**

**Stack:** Java 21 · Spring Boot 3.5 · **Spring Security + JWT** · Spring Data JPA · MySQL · **springdoc-openapi (Swagger)** · React 19 (Vite)

Three pieces run side by side:

| Piece | Port | What it is |
| --- | --- | --- |
| **Landing page** | `:3000` | A static, interactive marketing page. **"Launch App →" in the top-right corner** redirects to the React app. |
| **React app** | `:5173` | Login / signup and the three role dashboards. |
| **Spring Boot API** | `:8080` | The REST backend. **Swagger UI at [`/swagger-ui.html`](http://localhost:8080/swagger-ui.html).** |

Start at <http://localhost:3000> and click the corner button.

---

## Three roles

| Role | What they can do |
| --- | --- |
| **STUDENT** | Sign up, raise interview requests, track them, cancel their own |
| **MENTOR** | Sign up with a profile, see the open queue, accept with a slot + link, complete with feedback |
| **ADMIN** | See stats, every user and every request; deactivate or reactivate accounts |

### Demo logins

Every seeded account uses the password `password123`:

| Role | Email |
| --- | --- |
| Student | `rahul@example.com` |
| Mentor | `ananya@example.com` |
| Admin | `admin@example.com` |

There is deliberately **no public "sign up as admin"** endpoint — that would let
anyone grant themselves full access. The first admin comes from the seeder; in a
real system an existing admin would promote others.

---

## Project layout

```
interview-mentor/
├── backend/                          Spring Boot API on :8080
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
├── landing/                          Static landing page on :3000
│   ├── index.html                    one self-contained file — HTML + CSS + JS
│   └── serve.sh                      python3 -m http.server 3000
└── frontend/                         React app on :5173
    └── src/
        ├── api/client.js             every fetch call + the token header
        ├── auth/AuthContext.jsx      "who is logged in", shared via React Context
        ├── pages/
        │   ├── AuthPage.jsx          login + signup (student or mentor)
        │   ├── StudentDashboard.jsx
        │   ├── MentorDashboard.jsx
        │   └── AdminDashboard.jsx
        ├── components/               RequestCard, StatusBadge
        └── App.jsx                   picks the dashboard based on role
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

### 3. Landing page (optional)

```bash
cd landing
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
| `POST` | `/api/requests` | STUDENT — raise a request |
| `GET` | `/api/requests/mine` | STUDENT — your own requests |
| `GET` | `/api/requests/pending` | MENTOR — the open queue |
| `GET` | `/api/requests/assigned` | MENTOR — what you accepted |
| `PATCH` | `/api/requests/{id}/accept` | MENTOR — `{ scheduledAt, meetingLink }` |
| `PATCH` | `/api/requests/{id}/complete` | MENTOR (owner only) — `{ feedback }` |
| `PATCH` | `/api/requests/{id}/cancel` | student, mentor, or admin |

### Public — `/api/public` (no token at all)

| Method | Path | What |
| --- | --- | --- |
| `GET` | `/api/public/stats` | counts only — mentors, students, interviews done, open requests |

The landing page has no login, so this is the only data it can read. It returns
**aggregate numbers only, never names or emails** — anything `permitAll()` is
readable by the whole internet.

### Mentors & admin

| Method | Path | Who |
| --- | --- | --- |
| `GET` | `/api/mentors` | any logged-in user |
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
