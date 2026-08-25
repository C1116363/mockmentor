# ConfirmPlacement

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

| I want to… | Read |
| --- | --- |
| **Get it running on a clean machine** | [**SETUP.md**](SETUP.md) — installs, database, `./start.sh` |
| **Find where something is in the code** | [**ARCHITECTURE.md**](ARCHITECTURE.md) — every folder, which file handles which feature, one request traced end to end |
| **Take card / UPI payments** | [**PAYMENTS.md**](PAYMENTS.md) — the three Razorpay keys and exactly where they go |
| **Send real password-reset emails** | [SETUP.md → Sending real emails](SETUP.md#sending-real-emails-gmail) |
| **Host it on the internet** | [**DEPLOY.md**](DEPLOY.md) — the five things that break, and where to put the backend |

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

## How the code is laid out

```
controller -> facade -> service -> repository -> model
```

Each layer talks only to the one below it. A controller never touches a service or
a repository — `grep -rn "Service " backend/src/main/java/.../controller/` returns
nothing, and that is the property the structure exists to keep.

Requests coming in are `dto/` and end in **Dto**. Responses going out are `vo/`
and end in **Vo**. `service/` and `facade/` hold interfaces; the implementations
live in `service/impl/` and `facade/impl/`.

**The frontend mirrors it**, using the equivalents that are idiomatic in React:

| Backend | Frontend |
| --- | --- |
| `controller/` | `pages/` — the screen, renders and delegates |
| `facade/` | `features/x/useX.js` — a hook: state + orchestration |
| `service/` | `features/x/xRules.js` — pure functions, no React |
| `repository/` | `api/xApi.js` — one module per backend controller |

So `planApi.js` is `PlanController`, and if you know the endpoint you know the
file. Details in the [frontend README](frontend/README.md#structure).

**Every endpoint returns the same envelope:**

```json
{ "success": true,  "status": 200, "data": { ... } }
{ "success": false, "status": 404, "message": "No plan with id 9", "path": "/api/plans/9" }
```

So a client checks one boolean for every call it ever makes. On the frontend the
unwrapping is a single function in `api/http.js` — nothing above that line knows
the envelope exists, and `api.plans()` still resolves to an array of plans.

Full details are in [**ARCHITECTURE.md**](ARCHITECTURE.md) — the folder-by-folder
map, the feature-to-file table, and one request traced through all eleven files it
touches. Layer-specific notes are in the
[backend](backend/README.md#structure) and [frontend](frontend/README.md#structure)
READMEs.

## How a session gets scheduled

**Slots come from mentors, not from a calendar generator.** A mentor declares the
hours they're free; students see only those hours; an admin maps a mentor who
offered that exact hour onto the booking.

```
mentor declares hours  →  admin sees them  →  students see only those hours
                                                      ↓
                                         student books (24h+ ahead)
                                                      ↓
                              admin maps a mentor who offered that exact hour
```

**A day's notice, both ways.** A mentor declares at least 24 hours ahead, a student
books at least 24 hours ahead — one constant, because an admin needs a day in hand
to arrange an interviewer.

The mentor also says *what* they'll take in each hour — mock interviews, mentoring
discussions, or both — so the two grids genuinely differ.

Assigning a mentor who didn't offer that hour is a 409, with an explicit
`override=true` escape hatch for when somebody agrees over the phone. Detail in the
[backend README](backend/README.md#mentor-availability--where-slots-come-from).

## Two kinds of booking

A booked hour is either a **mock interview** or a **mentoring session**, set by
`sessionType` on the booking:

| | Mock interview | Mentoring session |
| --- | --- | --- |
| What it is | A real interview, under pressure | A discussion — career advice, a code review, working through a design |
| Ends in | A scorecard: ratings per skill + a readiness verdict | Written notes. **No ratings** |
| Slot, price, mentor pool | identical | identical |

It is a **field on the booking, not a second entity**: picking a slot, paying,
being assigned a mentor and being completed are the same for both. Only the
mentor's write-up differs, and a parallel table to vary one screen would be two
of everything for nothing.

`sessionType` is optional when booking and absent means `MOCK_INTERVIEW`, so
anything written before mentoring existed keeps working unchanged.

**Ratings are required for an interview and refused for a discussion.** Bean
validation can't express "required, but only for one type" — it never sees the
booking — so `SessionType.isScored()` carries the answer and
`InterviewRequestService.complete` enforces it. `InterviewRequest.complete` also
nulls any ratings that arrive for an unscored session, so a client can't staple a
fake scorecard onto a conversation.

## Live project contribution

Students can pay for contributor access to one of our **private** repositories —
real code that runs, where a senior engineer reviews and merges their pull
requests. That review is the part open source doesn't give a beginner.

An admin adds the project, sets the price and a seat limit; a student requests
access with their GitHub username and pays; the admin verifies the payment and
adds them as a collaborator.

**Two things worth knowing:**

- **The repository path is never sent to somebody without access.** These are
  private repos, so `repoFullName` and `repoUrl` come back null until your access
  is live — and go back to null when it expires or is revoked.
- **Approving the payment and adding them on GitHub are tracked separately.** If
  they were one flag, a failed invite would read as granted access: student told
  they're in, 404 on the repo, nothing in the system aware. Instead there's a
  queue of people who have paid and can't see the code yet.

Detail in the [backend README](backend/README.md#live-project-contribution).

## Plans and study material

Beyond one-off interviews, a student can buy a **plan** — Placement Guide, or a
technology taught by one of our experts. Prices are set by an admin in the admin
panel and stored in the database, so a change is live on the next page load with
no redeploy. An enrollment copies the price when the student starts buying, so
raising a price never rewrites what somebody already paid.

Admins also **send study material** — a file or a link — to every student, to one
named student, or to the members of one plan. Buying a plan unlocks its material
the moment an admin confirms the payment.

Both are documented in detail in the [backend README](backend/README.md#plans-and-study-material).

## Paying for things

Three things cost money — a session, a plan, and contributor access to a private
repo — and all three can be paid **two ways at once**.

### Manual UPI (the default, and always available)

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

```ini
UPI_ID=yourname@okhdfcbank
UPI_PAYEE=Your Name
```

This costs **0%**, the money arrives instantly rather than on T+2, and it needs
no account, no KYC and no approval wait. It is kept working alongside the
gateway rather than replaced by it — partly for the fees, partly because it is
how you take money on a day the gateway is down.

### Razorpay (optional, off until you add keys)

Card, netbanking, UPI and wallets, confirmed automatically with no admin step.
Switched on by three keys in `backend/.env` and nothing else:

```ini
PAYMENT_PROVIDER=razorpay
RAZORPAY_KEY_ID=rzp_test_…
RAZORPAY_KEY_SECRET=…
RAZORPAY_WEBHOOK_SECRET=…
```

With any key missing the app **doesn't break** — it notices, keeps offering
manual UPI, and says so at startup. Full setup, including the Gmail-style
gotchas, is in [PAYMENTS.md](PAYMENTS.md).

Both options appear on the same screen: a "Pay now" button on top, an
`or pay by UPI yourself` divider, then the existing UTR + screenshot steps.

### The rules that keep the money honest

**The amount is never sent by the client.** It is read from the row being paid
for. There is no amount field on any request DTO and there must never be one — a
checkout that accepts a price from the browser is a shop where the customer
writes their own price tag.

**A payment settles exactly once.** The browser callback and Razorpay's webhook
race each other by design, and Razorpay retries a webhook until it gets a 2xx.
Two database guarantees, not application logic:

- `SELECT … FOR UPDATE` on the payment intent, so only one route wins
- a unique constraint on the webhook event id, so a retry cannot re-run one

**The webhook is the authority, not the browser.** A browser callback only
arrives if the tab survives the round trip; closed tabs and dead phones are
ordinary, and those students have paid.

**Signatures are checked against the raw bytes.** Binding the body to an object
and re-serialising changes whitespace and key order, and the HMAC stops
matching.

**A captured amount that disagrees with what we asked for is refused**, not
activated, and left for a person.

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

### What is deliberately not built

No refunds (Razorpay's dashboard does them in two clicks) and no automatic
reconciliation against bank settlements. Neither earns its complexity at this
size.

## Paying mentors — payroll

**Admin → 💰 Payroll.** Set a per-session rate for each mentor, see what they are
owed from sessions they have actually completed, raise a payout, and record the
bank reference once the money has gone.

```
Ananya Rao                                    [On payroll]

   2         +       2                Owed now
 interviews      mentoring             ₹2,600

₹800 / interview · ₹500 / mentoring          [Edit rates]

Paid to date: ₹0 · Bank details       [Raise payout ₹2,600]
```

Two rates, because a mock interview ends in a written scorecard and a mentoring
session does not — different work, usually different money. Bank details and PAN
come from the mentor's existing profile, behind a click so account numbers are
not sitting on screen.

### The one rule

**A completed session is paid exactly once.** Both failures are somebody's
wages: paid twice is awkward to claw back, paid never means a mentor worked for
free and had to notice before anyone else did.

What guarantees it is a column — `interview_requests.payout_id` — not a date
range and not a careful process. Raising a payout stamps every unpaid session in
a single `UPDATE … WHERE payout_id IS NULL`, so two admins pressing the button at
the same moment cannot both claim the same work; the database picks the winner.

Paying "everything completed in August" sounds equivalent and is not: a session
finishing while the run is in progress lands on one side or the other depending
on how somebody rounds, and nobody notices until the money is wrong.

Rates are copied onto the payout, so a raise never rewrites what was already
paid. **A paid payout cannot be cancelled** — that would release its sessions
back into the queue and set up the exact double payment the rest of this
prevents. A payment sent in error needs a correcting entry, which is what every
ledger does.

## Forgotten passwords

A **Forgot your password?** link under the login button, an emailed one-time
link, and a page to choose a new one.

**It works with no email setup.** The default writes the message to the backend
console and you copy the link out of the terminal — so the feature is testable
on a fresh clone without finishing a Google security setup first. Real Gmail is
four lines in `.env`; see
[SETUP.md](SETUP.md#sending-real-emails-gmail).

These are got wrong in four specific places, so each is handled deliberately:

- **Asking for a link answers identically** whether the address has an account or
  not. Otherwise the endpoint is a free membership checker — and here it would
  reveal which of somebody's staff are practising for interviews.
- **The table stores a SHA-256 hash of the token**, never the token. A backup or
  a dump would otherwise hand over a working reset for every open request.
- **256-bit tokens from `SecureRandom`**, single use, 30 minutes, and a new
  request kills the old ones. Unknown, expired, spent and superseded all return
  one identical message.
- **Five requests per account per hour**, refused silently — otherwise this is an
  email flooder aimed at anyone.

**Resetting signs other sessions out.** The JWT is stateless and lives 24 hours,
so without extra work an attacker's token keeps working for a day after the
victim resets. Users carry `passwordChangedAt` and the auth filter refuses any
token issued before it.

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

Room names are long and random (`confirmplacement-h5rye2-rzcuqq-ummniz`) because a
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
│       ├── controller/               HTTP only — calls a facade, returns what it gives back
│       ├── facade/                   one method per use case (interface)
│       │   └── impl/                 …and its implementation
│       ├── service/                  business rules, one thing each (interface)
│       │   └── impl/                 …and its implementation
│       ├── repository/               Spring Data interfaces (no implementation needed)
│       ├── model/                    JPA entities — User, InterviewRequest, Plan, StudyMaterial
│       ├── dto/                      requests coming IN — every class ends in Dto
│       ├── vo/                       responses going OUT — every class ends in Vo
│       ├── common/                   ApiResult, the envelope every endpoint returns
│       ├── security/                 ← the whole auth system, see below
│       ├── storage/                  file uploads — screenshots and study material
│       ├── exception/                custom exceptions + @RestControllerAdvice
│       └── config/                   Swagger metadata, demo data seeder
├── website/                          [README](website/README.md) — marketing site on :3000
│   ├── index.html                    one self-contained file — HTML + CSS + JS
│   └── serve.sh                      starts it on :3000
└── frontend/                         [README](frontend/README.md) — React app on :5173
    └── src/                          mirrors the backend's layering — see below
        ├── pages/                    the screens (controller)
        ├── features/<x>/useX.js      use case: state + orchestration (facade)
        ├── features/<x>/xRules.js    pure business rules (service)
        ├── features/<x>/components/  UI that one feature owns
        ├── api/<x>Api.js             one module per backend controller (repository)
        ├── api/http.js               transport: fetch, auth header, envelope
        ├── components/               shared UI only — StatusBadge, StarRating
        ├── layout/SectionNav.jsx     the section menu in the header
        └── App.jsx                   loading / logged out / logged in
```

---

## Running it

**First time on this machine?** → [**SETUP.md**](SETUP.md) covers installing Java,
Node and MySQL, creating the database, and the errors you are likely to hit.

Once set up, one command brings up all three servers:

```bash
cp backend/.env.example backend/.env     # first time only — add your MySQL password
./start.sh
```

It checks your tools, creates the database if missing, installs frontend
dependencies on first run, waits for each server to actually answer, and prints
the URLs. Ctrl+C stops everything. Logs land in `.logs/`.

```
Checking what you have
  ✓ Java 21
  ✓ Node v22.11.0

Checking the database
  ✓ MySQL is listening on 3306
  ✓ backend/.env found
  ✓ Database 'interview_mentor' ready (user: root)

Starting
  ✓ backend    http://localhost:8080
  ✓ frontend   http://localhost:5173
  ✓ website    http://localhost:3000
```

Or start one part at a time — easier when you are working on a single piece:

```bash
./start.sh backend        # or frontend, or website
```

Or by hand, a terminal each:

```bash
cd backend  && ./run.sh          # :8080
cd frontend && npm install && npm run dev   # :5173
cd website  && ./serve.sh        # :3000
```

### Where things are

| | URL |
| --- | --- |
| The app | <http://localhost:5173> |
| Marketing site | <http://localhost:3000> |
| **API docs — all 78 endpoints** | <http://localhost:8080/swagger-ui.html> |
| Raw OpenAPI spec | <http://localhost:8080/v3/api-docs> |

To call a protected endpoint from Swagger: run `POST /api/auth/login` with a demo
account, copy the `token` out of `data`, click **Authorize** top-right and paste it
in (just the token — Swagger adds `Bearer`).

You do **not** need to create the database by hand. `createDatabaseIfNotExist=true`
in the JDBC URL plus `ddl-auto=update` means Hibernate builds every table on first
boot, and a seeder inserts the demo accounts and four plans.

> **If startup fails on a stale column:** `ddl-auto=update` only *adds* things — it
> never drops or relaxes a column. Locally, throw the schema away and let the
> seeder rebuild it: `mysql -u root -p -e "DROP DATABASE interview_mentor;"`

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
   { token, expiresInMs, user }          ── frontend keeps token in sessionStorage


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
| `POST` | `/api/requests` | STUDENT — book a slot (starts AWAITING_PAYMENT). `sessionType`: `MOCK_INTERVIEW` (default) or `MENTORING` |
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

### Plans — `/api/plans`

| Method | Path | Who |
| --- | --- | --- |
| `GET` | `/api/public/plans` | **no token** — the price list, for the website |
| `GET` | `/api/plans` | any logged-in user — active plans, live prices |
| `GET` | `/api/plans/{id}` | any logged-in user |
| `POST` | `/api/plans/{id}/enroll` | STUDENT — start buying; safe to call twice |
| `GET` | `/api/plans/enrollments/mine` | STUDENT — my purchases |
| `GET` | `/api/plans/enrollments/{id}/instructions` | owner or ADMIN — UPI details + the frozen price |
| `POST` | `/api/plans/enrollments/{id}/proof` | STUDENT — multipart `upiReference` + `screenshot` |
| `PATCH` | `/api/plans/enrollments/{id}/cancel` | STUDENT — back out before paying |
| `GET` | `/api/plans/enrollments/{id}/screenshot` | owner or ADMIN |

### Study material — `/api/materials`

| Method | Path | Who |
| --- | --- | --- |
| `GET` | `/api/materials` | any logged-in user — **only what was addressed to you** |
| `GET` | `/api/materials/{id}/file` | owner / plan member / ADMIN — re-checked, not assumed |

### Admin — plans & material

| Method | Path | Who |
| --- | --- | --- |
| `GET` | `/api/admin/plans` | ADMIN — retired plans included |
| `POST` | `/api/admin/plans` | ADMIN — create |
| `PUT` | `/api/admin/plans/{id}` | ADMIN — replace every field |
| `PATCH` | `/api/admin/plans/{id}/price` | ADMIN — `{ price }`, live immediately |
| `PATCH` | `/api/admin/plans/{id}/active?active=` | ADMIN — retire or revive |
| `GET` | `/api/admin/plan-enrollments/pending` | ADMIN — plan payments to verify |
| `GET` | `/api/admin/plan-enrollments` | ADMIN — all purchases |
| `PATCH` | `/api/admin/plan-enrollments/{id}/activate` | ADMIN — grants access |
| `PATCH` | `/api/admin/plan-enrollments/{id}/reject` | ADMIN — `{ reason }` |
| `GET` | `/api/admin/materials` | ADMIN — everything ever sent |
| `POST` | `/api/admin/materials` | ADMIN — multipart `title`, `file`, optional `targetStudentId` **or** `targetPlanId` |
| `GET` | `/api/projects` | any logged-in user — the catalogue; repo path withheld unless you hold access |
| `POST` | `/api/projects/{id}/request-access` | STUDENT — `{ githubUsername, motivation? }` |
| `GET` | `/api/projects/access/mine` | STUDENT — my access requests |
| `POST` | `/api/projects/access/{id}/proof` | STUDENT — multipart UTR + screenshot |
| `GET` | `/api/admin/projects` | ADMIN — the catalogue, repo always shown |
| `GET` | `/api/admin/projects/access/awaiting-invite` | ADMIN — **paid, not yet on the repo** |
| `GET` | `/api/admin/projects/access/past-expiry` | ADMIN — access that outlived its window |
| `PATCH` | `/api/admin/projects/access/{id}/approve` | ADMIN — confirm payment, start access |
| `PATCH` | `/api/admin/projects/access/{id}/confirm-invite` | ADMIN — "I've added them on GitHub" |
| `PATCH` | `/api/admin/projects/access/{id}/revoke` | ADMIN — `{ reason }` |
| `POST` | `/api/admin/materials/link` | ADMIN — `{ title, description, linkUrl }` + the same optional audience params |
| `PATCH` | `/api/admin/materials/{id}/active?active=` | ADMIN — publish or hide |

### Checkout (gateway) — `/api/checkout`

| Method | Path | Who |
| --- | --- | --- |
| `GET` | `/api/checkout/options` | any logged-in user — which payment methods this server can offer |
| `POST` | `/api/checkout/{purpose}/{targetId}` | STUDENT — open an order. `purpose` is `INTERVIEW` \| `PLAN` \| `PROJECT`. **No amount in the body.** |
| `POST` | `/api/checkout/confirm` | STUDENT — the signed result from the checkout window |
| `POST` | `/api/webhooks/razorpay` | **no token** — Razorpay's servers. Signature-verified against the raw bytes. |

### Forgotten passwords — `/api/auth`

| Method | Path | Who |
| --- | --- | --- |
| `POST` | `/api/auth/forgot-password` | **no token** — always 200, identical message either way |
| `POST` | `/api/auth/reset-password` | **no token** — `{ token, newPassword }`; signs other sessions out |

### Payroll — `/api/admin/payroll`

| Method | Path | Who |
| --- | --- | --- |
| `GET` | `/api/admin/payroll/mentors` | ADMIN — every mentor, rates, what they are owed, bank details |
| `GET` | `/api/admin/payroll/summary` | ADMIN — totals across everyone |
| `PATCH` | `/api/admin/payroll/mentors/{id}/settings` | ADMIN — `{ enabled, interviewRate, mentoringRate }` |
| `POST` | `/api/admin/payroll/mentors/{id}/payouts` | ADMIN — raise a payout. **No amount in the body.** |
| `PATCH` | `/api/admin/payroll/payouts/{id}/mark-paid` | ADMIN — `{ paymentReference, notes? }`; reference required |
| `PATCH` | `/api/admin/payroll/payouts/{id}/cancel?reason=` | ADMIN — releases its sessions; refused once paid |
| `GET` | `/api/admin/payroll/payouts` | ADMIN — every payout, newest first |

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
- **`api/http.js`** — one `request()` wrapper attaches the `Bearer` header and
  clears the token on 401. `requestEnvelope()` is its sibling for the calls
  whose *message* is the answer — a payout total, or "add this person on
  GitHub" with the link.
- **`App.jsx`** — role-based routing with a plain object lookup. Swap for
  react-router once you add more pages.

---

## Known rough edges (deliberate — good things to fix next)

1. **Token in `sessionStorage`** is readable by any JS on the page, so it's still
   exposed to XSS. sessionStorage fixes how long a session lasts, not who can read
   it — an httpOnly cookie is the stronger answer.
2. **No refresh token.** After 24h you just log in again.
3. **No logout on the server.** With JWTs the server keeps no session, so "logout"
   only deletes the client's copy. **Closing the tab is the same act** — the token
   is discarded locally but stays valid server-side until it expires.
   The one exception is a **password reset**, which stamps `passwordChangedAt` and
   makes the auth filter refuse every token issued before it. If you want the same
   for ordinary logout, that column is the hook to build on.
4. **`ddl-auto=update`** instead of Flyway migrations.
5. A harmless startup **warning** about `AuthenticationManager` / `UserDetailsService`
   — it's Spring telling you the explicit `DaoAuthenticationProvider` bean takes
   over from auto-configuration, which is exactly what we want.

---

## Ideas to extend it

1. **Refresh tokens** — short-lived access token + long-lived refresh token.
2. **Stop double-booking** — reject a slot a mentor already has an interview in.
3. **Ratings** — the student rates the mentor after `COMPLETED` (a `@OneToOne` to practise).
4. **Email notifications** — the `Mailer` interface is already there and already
   sends password resets; wiring it to "your interview was accepted" is a method
   call, not a project.
5. **Flyway migrations** — versioned SQL instead of `ddl-auto`.
6. **More tests** — `@WebMvcTest` with `spring-security-test`'s `@WithMockUser`,
   `@DataJpaTest` for repositories. Both dependencies are already in the pom, and
   `RazorpayGatewayTest` is a worked example of the style.
7. **Refunds** — Razorpay's dashboard does them today; in-app would need a
   correcting entry against the original payment, never an edit to it.
8. **A mentor-facing earnings screen** — the payroll data is all there; today
   only admins can see it.
