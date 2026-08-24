# Architecture — where everything lives

A map of the codebase. If you are looking for where something is, start here.

- 143 Java files across 5 layers · 78 endpoints
- 68 JS/JSX files across the same 5 layers
- Both halves use the **same layering**, so what you learn on one side transfers

**Just want to run it?** → [SETUP.md](SETUP.md)

**Quick links:** [layers](#the-five-layers) · [backend map](#backend-map) ·
[frontend map](#frontend-map) · [one request end to end](#one-request-end-to-end) ·
[adding a feature](#adding-a-feature) · [naming](#naming-rules) ·
[rules you can grep](#rules-you-can-grep)

---

## The five layers

Both halves are the same shape. Each layer talks **only to the one below it**.

| | Backend | Frontend | Holds |
| --- | --- | --- | --- |
| 1 | `controller/` | `pages/` | The entry point. Reads input, delegates, returns. No business logic. |
| 2 | `facade/` | `features/x/useX.js` | One method per use case. Composes layer 3, decides what the caller is told. |
| 3 | `service/` | `features/x/xRules.js` | Business rules for one thing. |
| 4 | `repository/` | `api/xApi.js` | Data access. Queries on one side, URLs on the other. |
| 5 | `model/` | — | The database. |

```
    controller  ──►  facade  ──►  service  ──►  repository  ──►  model
     (pages)         (hooks)      (rules)         (api)        (the DB)
```

A controller reaching past its facade is the thing this structure exists to stop.
See [rules you can grep](#rules-you-can-grep) — every one of them returns empty today.

---

## Backend map

`backend/src/main/java/com/learn/interviewmentor/`

| Package | Files | What is in it |
| --- | --- | --- |
| `controller/` | 13 | HTTP only. Every method returns `ApiResult<T>`. |
| `facade/` + `impl/` | 10 + 10 | Use cases. Interface + implementation. |
| `service/` + `impl/` | 13 + 13 | Business rules. Interface + implementation. |
| `repository/` | 9 | Spring Data interfaces. One per table. |
| `model/` | 20 | JPA entities + their enums. |
| `dto/` | 14 | Requests coming **in**. Every class ends in `Dto`. |
| `vo/` | 14 | Responses going **out**. Every class ends in `Vo`. |
| `common/` | 2 | `ApiResult` (the response envelope) + the advice that syncs its status. |
| `exception/` | 7 | Custom exceptions + the `@RestControllerAdvice` that maps them. |
| `security/` | 8 | JWT filter, role rules, the 401/403 writers. |
| `storage/` | 2 | File uploads — screenshots and study material. |
| `meeting/` | 3 | Meeting-link generators (Jitsi, Google). |
| `config/` | 2 | Swagger metadata, demo-data seeder. |
| `util/` | 1 | `Masking` — last-4-digits for sensitive numbers. |
| `github/` | 3 | Granting collaborator access on a private repo. Interface + manual impl + API stub. |

### Which file for which feature

| Feature | Controller | Facade | Service(s) |
| --- | --- | --- | --- |
| Login / signup | `AuthController` | `AuthFacade` | `AuthService` |
| Booking a session | `InterviewRequestController` | `SessionFacade` | `InterviewRequestService` |
| The hours grid | `SlotController` | `SlotFacade` | `SlotService` |
| Paying for a session | `PaymentController` | `PaymentFacade` | `PaymentService` |
| Mentor directory | `MentorController` | `MentorFacade` | `MentorService` |
| Mentor onboarding | `MentorProfileController` | `MentorFacade` | `MentorProfileService` |
| Plans (student side) | `PlanController` | `PlanFacade` | `PlanService`, `PlanEnrollmentService` |
| Study material | `StudyMaterialController` | `StudyMaterialFacade` | `StudyMaterialService` |
| Marketing site data | `PublicController` | `PublicFacade` | `PublicStatsService`, `PlanService` |
| Admin — core | `AdminController` | `AdminFacade` | Admin, InterviewRequest, MentorProfile, Payment |
| Admin — plans & material | `AdminPlanController` | `PlanFacade`, `StudyMaterialFacade` | Plan, PlanEnrollment, StudyMaterial |
| Live projects (student) | `ProjectController` | `ProjectFacade` | `LiveProjectService`, `ProjectAccessService` |
| Admin — live projects | `AdminProjectController` | `ProjectFacade` | same two |

### The data model

| Entity | Table | Notes |
| --- | --- | --- |
| `User` | `users` | Student, mentor or admin. One `role` column. |
| `MentorProfile` | `mentor_profiles` | Onboarding + the numbers an admin verifies. |
| `InterviewRequest` | `interview_requests` | One booked hour. `sessionType` says interview or mentoring. |
| `Payment` | `payments` | Manual UPI payment for one booking. |
| `Plan` | `plans` | A product. **The price lives here**, not in config. |
| `PlanEnrollment` | `plan_enrollments` | One student buying one plan. Copies the price. |
| `StudyMaterial` | `study_materials` | A file or link, with one of three audiences. |
| `LiveProject` | `live_projects` | A private repo sold as contributor access. Repo stored as owner + name. |
| `ProjectAccessRequest` | `project_access_requests` | One student's paid access to one repo. |

Enums: `Role`, `RequestStatus`, `SessionType`, `PaymentStatus`, `EnrollmentStatus`,
`VerificationStatus`, `MaterialAudience`, `MaterialKind`, `Recommendation`,
`ProjectAccessStatus`, `ProjectDifficulty`.

---

## Frontend map

`frontend/src/`

| Folder | Files | What is in it |
| --- | --- | --- |
| `pages/` | 7 | The screens. Render and delegate — no fetch calls. |
| `features/x/useX.js` | 10 | Hooks: state + orchestration. **The only place that calls `api/`.** |
| `features/x/xRules.js` | 2 | Pure functions. No React, no fetch. |
| `features/x/components/` | 16 | UI that one feature owns. |
| `api/` | 9 | One module per backend controller, plus `http.js`. |
| `components/` | 3 | Shared UI only — `StatusBadge`, `StarRating`, `ThemeToggle`. |
| `hooks/` | 1 | `useBlobUrl` — shared, feature-agnostic. |
| `utils/` | 1 | `format.js` — `formatPrice`. No feature knowledge. |
| `layout/` | 1 | `SectionNav` — the header's section menu. |

### `api/` lines up 1:1 with the backend

| Frontend module | Backend controller |
| --- | --- |
| `authApi.js` | `AuthController` |
| `sessionApi.js` | `InterviewRequestController` |
| `slotApi.js` | `SlotController` |
| `paymentApi.js` | `PaymentController` |
| `mentorApi.js` | `MentorController` + `MentorProfileController` |
| `planApi.js` | `PlanController` |
| `materialApi.js` | `StudyMaterialController` |
| `adminApi.js` | `AdminController` + `AdminPlanController` |
| `projectApi.js` | `ProjectController` |
| `adminProjectApi.js` | `AdminProjectController` |
| `http.js` | — (transport: fetch, auth header, envelope, token) |

**If you know the endpoint, you know the file.**

### The hooks

| Hook | Feature | Used by |
| --- | --- | --- |
| `useSessions` | sessions | `StudentDashboard` |
| `useMentorSessions` | sessions | `MentorDashboard` |
| `useSlots` | sessions | `SlotPicker` |
| `useSessionFeedback` | sessions | `FeedbackModal` |
| `usePlans` | plans | `StudentDashboard` |
| `usePlanPayment` | plans | `PlanPayModal` |
| `useEnrollmentScreenshot` | plans | `EnrollmentReviewCard` |
| `usePayment` | payments | `PayModal` |
| `usePaymentScreenshot` | payments | `PaymentReviewCard` |
| `useMaterials` + `useMaterialDownload` | materials | `StudentDashboard`, `MaterialCard` |
| `useMentorProfile` | mentors | `MentorGate`, `MentorProfileForm` |
| `useAdminDashboard` | admin | `AdminDashboard` |
| `useProjects` | projects | `StudentDashboard` |
| `useProjectAccessPayment` | projects | `ProjectPayModal` |
| `useProjectAccessScreenshot` | projects | `AccessReviewCard` |
| `useAdminProjects` | projects | `AdminDashboard` |

One hook **per feature**, not per screen. `StudentDashboard` calls three, so a
broken plans call cannot blank out the interview list. `useAdminDashboard` is the
exception, for the same reason `AdminFacade` is: one admin screen genuinely needs
six datasets at once, and verifying a payment changes half of them.

---

## One request end to end

Following "an admin changes a plan's price" through every file it touches.

```
1  frontend  pages/AdminDashboard.jsx
              └─ renders <PlanAdminCard onSavePrice={savePlanPrice} />

2  frontend  features/plans/components/PlanAdminCard.jsx
              └─ inline price form, calls onSavePrice(plan, 3499)

3  frontend  features/admin/useAdminDashboard.js          ← facade
              └─ adminApi.updatePlanPrice(id, price), then reload()

4  frontend  api/adminApi.js                              ← repository
              └─ PATCH /api/admin/plans/1/price  { "price": 3499 }

5  frontend  api/http.js
              └─ adds Authorization: Bearer <token>, unwraps the envelope

   ─────────────────────── network ───────────────────────

6  backend   security/JwtAuthenticationFilter
              └─ validates the token, puts the user in the SecurityContext
                 SecurityConfig: /api/admin/** requires ROLE_ADMIN

7  backend   controller/AdminPlanController.changePrice()  ← controller
              └─ @Valid PlanPriceRequestDto, delegates. Nothing else.

8  backend   facade/impl/PlanFacadeImpl.changePrice()      ← facade
              └─ calls the service, wraps in ApiResult.ok(plan, "…students see
                 it on their next load…")

9  backend   service/impl/PlanServiceImpl.changePrice()    ← service
              └─ finds the plan or throws NotFoundException, sets the price,
                 logs old → new

10 backend   repository/PlanRepository                     ← repository
              └─ findById; the write flushes on transaction commit

11 backend   common/ApiResultStatusAdvice
              └─ copies ApiResult.status() onto the HTTP status line
```

**On failure** step 9 throws, `exception/GlobalExceptionHandler` catches it and
returns the same envelope with `success: false` — so step 5 unwraps it into an
`ApiError` and step 3's catch shows it inline under the input.

---

## Adding a feature

Say you want "certificates". Nine files, in this order:

**Backend**

```
1  model/Certificate.java                     @Entity — the table
2  repository/CertificateRepository.java      extends JpaRepository
3  dto/certificate/CertificateRequestDto.java what comes in
4  vo/certificate/CertificateVo.java          what goes out, + static from(entity)
5  service/CertificateService.java            interface — the contract
6  service/impl/CertificateServiceImpl.java   @Service @Transactional — the rules
7  facade/CertificateFacade.java              interface — use cases
8  facade/impl/CertificateFacadeImpl.java     @Component — returns ApiResult
9  controller/CertificateController.java      @RestController — HTTP only
```

Then wire it: a URL rule in `security/SecurityConfig` if the default
`authenticated()` is not right, and a count in `AdminServiceImpl.stats()` if the
dashboard should show one.

**Frontend**

```
1  api/certificateApi.js                      URLs only
2  features/certificates/useCertificates.js   the hook
3  features/certificates/certificateRules.js  pure rules, if there are any
4  features/certificates/components/*.jsx     the UI
5  pages/…                                    add a section + a nav entry
```

Copy `Plan` end to end as the reference — it is the most complete example, with
public + student + admin sides, a payment flow and file uploads.

---

## Naming rules

**Backend**

| Kind | Suffix | Package | Example |
| --- | --- | --- | --- |
| Request body | `Dto` | `dto/` | `PlanRequestDto` |
| Response body | `Vo` | `vo/` | `PlanVo` |
| Contract | `Service` / `Facade` | `service/`, `facade/` | `PlanService` |
| Implementation | `Impl` | `*/impl/` | `PlanServiceImpl` |
| Entity | none | `model/` | `Plan` |
| Repository | `Repository` | `repository/` | `PlanRepository` |

Sub-packages mirror: `dto/plan` ↔ `vo/plan`.

**Frontend**

| Kind | Pattern | Example |
| --- | --- | --- |
| Screen | `*Dashboard.jsx` / `*Page.jsx` | `AdminDashboard.jsx` |
| Use case | `use*.js` | `usePlans.js` |
| Rules | `*Rules.js` | `planRules.js` |
| Data access | `*Api.js` | `planApi.js` |

---

## The response envelope

Every endpoint returns the same shape:

```json
{ "success": true,  "status": 200, "data": { … } }
{ "success": false, "status": 404, "message": "No plan with id 9", "path": "/api/plans/9" }
```

Null fields are dropped, so a success carries no empty `message` key. Errors use
the identical shape — including the 401/403 the security filter chain writes
*before* Spring MVC runs (`exception/ApiErrors`), so a client cannot tell them apart.

The frontend unwraps it in **one function**, `unwrap()` in `api/http.js`. Nothing
above that line knows the envelope exists: `planApi.all()` resolves to an array
of plans.

Two details:

- It is called `ApiResult`, not `ApiResponse`, because
  `io.swagger.v3.oas.annotations.responses.ApiResponse` already is and Java has no
  import aliases.
- **File downloads are not enveloped** — the body has to be the bytes. Those
  handlers return `ResponseEntity<Resource>`, and their facade methods return a
  bare `Path`.

---

## Rules you can grep

Every one of these returns empty today. A non-empty result means a layer was skipped.

```bash
cd backend/src/main/java/com/learn/interviewmentor

# a controller must not reach past its facade
grep -rl "interviewmentor\.service\.\|interviewmentor\.repository\." controller/

# a facade must not touch a repository
grep -rl "interviewmentor\.repository\." facade/

# every contract has exactly one impl
for f in service/*Service.java facade/*Facade.java; do
  b=$(basename "$f" .java); d=$(dirname "$f")
  [ -f "$d/impl/${b}Impl.java" ] || echo "MISSING: ${b}Impl"
done
```

```bash
cd frontend/src

# only hooks may call the api layer
grep -rl 'from "\.\./api/' pages/
grep -rl '/api/' features/*/components/ components/

# rules files stay pure
grep -l 'from "react"' features/*/*Rules.js
grep -l '/api/' features/*/*Rules.js
```

Worth pasting into a pre-commit hook or a CI step if you want them enforced
rather than remembered.

---

## Where the important decisions are written down

Long explanations live next to the code, not here. These are the ones worth
knowing about:

| Topic | File |
| --- | --- |
| Error handling, status codes, the 5xx reference-code rule | [`backend/README.md`](backend/README.md#errors) |
| Why prices live in the DB and never rewrite history | [`backend/README.md`](backend/README.md#plans-and-study-material) |
| Study-material audiences enforced in SQL, not the UI | `service/impl/StudyMaterialServiceImpl.java` |
| Interview vs mentoring, and the rule bean validation can't express | [`backend/README.md`](backend/README.md#mock-interview-vs-mentoring-session) |
| The `session_type` migration trap with `ddl-auto=update` | `model/InterviewRequest.java` |
| Why HTML and SVG uploads are refused | `storage/MaterialStorage.java` |
| Session ends when the tab closes | [`frontend/README.md`](frontend/README.md#session-lifetime) |
| 401 vs 403, and why the filter chain writes its own JSON | `security/RestAuthenticationEntryPoint.java` |
